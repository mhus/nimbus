package de.mhus.nimbus.common.service;

import de.mhus.nimbus.common.constants.NimbusConstants;
import de.mhus.nimbus.common.exception.NimbusException;
import de.mhus.nimbus.common.util.RequestIdUtils;
import de.mhus.nimbus.shared.avro.LoginRequest;
import de.mhus.nimbus.shared.avro.LoginResponse;
import de.mhus.nimbus.shared.avro.PublicKeyRequest;
import de.mhus.nimbus.shared.avro.PublicKeyResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Security Service für JWT Token Management via Kafka
 * Implementiert Login-Funktionalität und Public Key Management mit dem Identity Service
 */
@Service
public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);
    private static final long LOGIN_TIMEOUT_MS = 30000; // 30 Sekunden
    private static final long PUBLIC_KEY_TIMEOUT_MS = 30000; // 30 Sekunden

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RequestIdUtils requestIdUtils;

    // Speichert pending Login-Requests
    private final ConcurrentHashMap<String, CompletableFuture<LoginResponse>> pendingLogins = new ConcurrentHashMap<>();

    // Speichert pending Public Key Requests
    private final ConcurrentHashMap<String, CompletableFuture<PublicKeyResponse>> pendingPublicKeyRequests = new ConcurrentHashMap<>();

    // Public Key Cache
    private volatile PublicKeyInfo cachedPublicKey;
    private volatile Instant publicKeyLastFetched;
    private final AtomicBoolean publicKeyFetchInProgress = new AtomicBoolean(false);
    private static final long PUBLIC_KEY_CACHE_DURATION_MS = 300000; // 5 Minuten Cache

    public SecurityService(KafkaTemplate<String, Object> kafkaTemplate, RequestIdUtils requestIdUtils) {
        this.kafkaTemplate = kafkaTemplate;
        this.requestIdUtils = requestIdUtils;
    }

    /**
     * Login-Funktion die via Kafka ein JWT Token vom Identity Service abruft
     *
     * @param username Benutzername oder E-Mail
     * @param password Passwort
     * @return LoginResult mit Token und User-Informationen
     * @throws NimbusException bei Login-Fehlern
     */
    public LoginResult login(String username, String password) throws ExecutionException, InterruptedException, TimeoutException {
        return login(username, password, null);
    }

    /**
     * Login-Funktion mit Client-Info
     *
     * @param username Benutzername oder E-Mail
     * @param password Passwort
     * @param clientInfo Informationen über den Client (optional)
     * @return LoginResult mit Token und User-Informationen
     * @throws NimbusException bei Login-Fehlern
     */
    public LoginResult login(String username, String password, String clientInfo) throws ExecutionException, InterruptedException, TimeoutException {
        logger.info("Initiating login request for user: {}", username);

        // Validierung
        if (username == null || username.trim().isEmpty()) {
            throw new NimbusException("Username cannot be null or empty", "VALIDATION_ERROR", "nimbus-common");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new NimbusException("Password cannot be null or empty", "VALIDATION_ERROR", "nimbus-common");
        }

        String requestId = requestIdUtils.generateRequestId("login");

        try {
            // Erstelle Login-Request
            LoginRequest loginRequest = LoginRequest.newBuilder()
                    .setRequestId(requestId)
                    .setUsername(username.trim())
                    .setPassword(password)
                    .setTimestamp(Instant.now())
                    .setClientInfo(clientInfo != null ? clientInfo : "nimbus-common")
                    .build();

            // Erstelle Future für Response
            CompletableFuture<LoginResponse> loginFuture = new CompletableFuture<>();
            pendingLogins.put(requestId, loginFuture);

            // Sende Login-Request via Kafka
            kafkaTemplate.send(NimbusConstants.Topics.LOGIN_REQUEST, requestId, loginRequest);
            logger.debug("Sent login request via Kafka: requestId={}", requestId);

            // Warte auf Response mit Timeout
            LoginResponse response = loginFuture.get(LOGIN_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            // Verarbeite Response
            return processLoginResponse(response, username);

        } catch (Exception e) {
            pendingLogins.remove(requestId);
            logger.error("Login failed for user {}: {}", username, e.getMessage(), e);

            if (e instanceof NimbusException) {
                throw e;
            }
            throw new NimbusException("Login request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verarbeitet die Login-Response vom Identity Service
     * Diese Methode wird vom Kafka Consumer aufgerufen
     */
    public void handleLoginResponse(@Payload LoginResponse response,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        String requestId = response.getRequestId();
        logger.debug("Received login response: requestId={}, status={}", requestId, response.getStatus());

        CompletableFuture<LoginResponse> loginFuture = pendingLogins.remove(requestId);
        if (loginFuture != null) {
            loginFuture.complete(response);
        } else {
            logger.warn("Received login response for unknown requestId: {}", requestId);
        }
    }

    /**
     * Verarbeitet die Login-Response und erstellt LoginResult
     */
    private LoginResult processLoginResponse(LoginResponse response, String username) {
        switch (response.getStatus()) {
            case SUCCESS:
                logger.info("Login successful for user: {}", username);
                return new LoginResult(
                        true,
                        response.getToken(),
                        response.getExpiresAt(),
                        response.getUser() != null ? new UserInfo(
                                response.getUser().getId(),
                                response.getUser().getUsername(),
                                response.getUser().getEmail(),
                                response.getUser().getFirstName(),
                                response.getUser().getLastName()
                        ) : null,
                        "Login successful",
                        null
                );

            case INVALID_CREDENTIALS:
                logger.warn("Invalid credentials for user: {}", username);
                throw new NimbusException("Invalid username or password", "INVALID_CREDENTIALS", "nimbus-common");

            case USER_NOT_FOUND:
                logger.warn("User not found: {}", username);
                throw new NimbusException("User not found", "USER_NOT_FOUND", "nimbus-common");

            case USER_INACTIVE:
                logger.warn("User account inactive: {}", username);
                throw new NimbusException("User account is inactive", "USER_INACTIVE", "nimbus-common");

            case ERROR:
            default:
                logger.error("Login error for user {}: {}", username, response.getErrorMessage());
                throw new NimbusException(
                        response.getErrorMessage() != null ? response.getErrorMessage() : "Login failed",
                        "LOGIN_ERROR",
                        "nimbus-common"
                );
        }
    }

    /**
     * Holt den Public Key vom Identity Service via Kafka
     * Cached den Key für bessere Performance
     *
     * @return PublicKeyInfo mit RSA Public Key Informationen
     * @throws NimbusException bei Fehlern beim Abrufen des Keys
     */
    public PublicKeyInfo getPublicKey() throws ExecutionException, InterruptedException, TimeoutException {
        return getPublicKey(false);
    }

    /**
     * Holt den Public Key vom Identity Service via Kafka
     *
     * @param forceRefresh true um den Cache zu umgehen und frischen Key zu holen
     * @return PublicKeyInfo mit RSA Public Key Informationen
     * @throws NimbusException bei Fehlern beim Abrufen des Keys
     */
    public PublicKeyInfo getPublicKey(boolean forceRefresh) throws ExecutionException, InterruptedException, TimeoutException {
        logger.debug("Requesting public key, forceRefresh={}", forceRefresh);

        // Prüfe Cache wenn nicht forced refresh
        if (!forceRefresh && isPublicKeyCacheValid()) {
            logger.debug("Returning cached public key");
            return cachedPublicKey;
        }

        // Verhindere gleichzeitige Requests
        if (!publicKeyFetchInProgress.compareAndSet(false, true)) {
            // Warte auf laufenden Request
            while (publicKeyFetchInProgress.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new NimbusException("Interrupted while waiting for public key", "INTERRUPTED_ERROR", "nimbus-common");
                }
            }
            // Prüfe ob zwischenzeitlich ein Key geholt wurde
            if (isPublicKeyCacheValid()) {
                return cachedPublicKey;
            }
        }

        try {
            String requestId = requestIdUtils.generateRequestId("public-key");

            // Erstelle Public Key Request
            PublicKeyRequest keyRequest = PublicKeyRequest.newBuilder()
                    .setRequestId(requestId)
                    .setTimestamp(Instant.now())
                    .setRequestedBy("nimbus-common-security-service")
                    .build();

            // Erstelle Future für Response
            CompletableFuture<PublicKeyResponse> keyFuture = new CompletableFuture<>();
            pendingPublicKeyRequests.put(requestId, keyFuture);

            // Sende Public Key Request via Kafka
            kafkaTemplate.send(NimbusConstants.Topics.PUBLIC_KEY_REQUEST, requestId, keyRequest);
            logger.debug("Sent public key request via Kafka: requestId={}", requestId);

            // Warte auf Response mit Timeout
            PublicKeyResponse response = keyFuture.get(PUBLIC_KEY_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            // Verarbeite Response
            return processPublicKeyResponse(response);

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            logger.error("Failed to get public key: {}", e.getMessage(), e);

            if (e instanceof NimbusException) {
                throw e;
            }
            throw new NimbusException("Public key request failed: " + e.getMessage(), "PUBLIC_KEY_ERROR", "nimbus-common", e);
        } catch (Exception e) {
            logger.error("Failed to get public key: {}", e.getMessage(), e);

            if (e instanceof NimbusException) {
                throw e;
            }
            throw new NimbusException("Public key request failed: " + e.getMessage(), "PUBLIC_KEY_ERROR", "nimbus-common", e);
        } finally {
            publicKeyFetchInProgress.set(false);
        }
    }

    /**
     * Verarbeitet die Public Key Response vom Identity Service
     * Diese Methode wird vom Kafka Consumer aufgerufen
     */
    public void handlePublicKeyResponse(@Payload PublicKeyResponse response,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        String requestId = response.getRequestId();
        logger.debug("Received public key response: requestId={}, status={}", requestId, response.getStatus());

        CompletableFuture<PublicKeyResponse> keyFuture = pendingPublicKeyRequests.remove(requestId);
        if (keyFuture != null) {
            keyFuture.complete(response);
        } else {
            logger.warn("Received public key response for unknown requestId: {}", requestId);
        }
    }

    /**
     * Verarbeitet die Public Key Response und cached den Key
     */
    private PublicKeyInfo processPublicKeyResponse(PublicKeyResponse response) {
        switch (response.getStatus()) {
            case SUCCESS:
                logger.info("Successfully received public key from Identity Service");

                PublicKeyInfo keyInfo = new PublicKeyInfo(
                        response.getPublicKey(),
                        response.getKeyType(),
                        response.getAlgorithm(),
                        response.getIssuer(),
                        response.getTimestamp()
                );

                // Cache den Key
                cachedPublicKey = keyInfo;
                publicKeyLastFetched = Instant.now();

                return keyInfo;

            case ERROR:
            default:
                logger.error("Public key request failed: {}", response.getErrorMessage());
                throw new NimbusException(
                        response.getErrorMessage() != null ? response.getErrorMessage() : "Public key request failed",
                        "PUBLIC_KEY_ERROR",
                        "nimbus-common"
                );
        }
    }

    /**
     * Prüft ob der gecachte Public Key noch gültig ist
     */
    private boolean isPublicKeyCacheValid() {
        return cachedPublicKey != null &&
                publicKeyLastFetched != null &&
                Instant.now().isBefore(publicKeyLastFetched.plusMillis(PUBLIC_KEY_CACHE_DURATION_MS));
    }

    /**
     * Gibt den gecachten Public Key zurück (kann null sein)
     */
    public PublicKeyInfo getCachedPublicKey() {
        return cachedPublicKey;
    }

    /**
     * Leert den Public Key Cache
     */
    public void clearPublicKeyCache() {
        logger.debug("Clearing public key cache");
        cachedPublicKey = null;
        publicKeyLastFetched = null;
    }

    /**
     * Prüft ob ein Public Key im Cache vorhanden ist
     */
    public boolean hasValidPublicKey() {
        return isPublicKeyCacheValid();
    }

    /**
     * Cleanup-Methode für abgelaufene Requests
     */
    public void cleanupExpiredRequests() {
        // Login requests cleanup
        pendingLogins.entrySet().removeIf(entry -> {
            if (entry.getValue().isDone() || entry.getValue().isCancelled()) {
                return true;
            }
            // Timeout nach 5 Minuten
            entry.getValue().completeExceptionally(
                    new NimbusException("Login request timed out", "TIMEOUT_ERROR", "nimbus-common"));
            return true;
        });

        // Public Key requests cleanup
        pendingPublicKeyRequests.entrySet().removeIf(entry -> {
            if (entry.getValue().isDone() || entry.getValue().isCancelled()) {
                return true;
            }
            // Timeout nach 5 Minuten
            entry.getValue().completeExceptionally(
                    new NimbusException("Public key request timed out", "TIMEOUT_ERROR", "nimbus-common"));
            return true;
        });
    }

    /**
     * Login-Ergebnis DTO
     */
    public static class LoginResult {
        private final boolean success;
        private final String token;
        private final Instant expiresAt;
        private final UserInfo user;
        private final String message;
        private final String errorCode;

        public LoginResult(boolean success, String token, Instant expiresAt, UserInfo user, String message, String errorCode) {
            this.success = success;
            this.token = token;
            this.expiresAt = expiresAt;
            this.user = user;
            this.message = message;
            this.errorCode = errorCode;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getToken() { return token; }
        public Instant getExpiresAt() { return expiresAt; }
        public UserInfo getUser() { return user; }
        public String getMessage() { return message; }
        public String getErrorCode() { return errorCode; }
    }

    /**
     * User-Informationen DTO
     */
    public static class UserInfo {
        private final Long id;
        private final String username;
        private final String email;
        private final String firstName;
        private final String lastName;

        public UserInfo(Long id, String username, String email, String firstName, String lastName) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        // Getters
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
    }

    /**
     * Public Key Informationen DTO
     */
    public static class PublicKeyInfo {
        private final String publicKey;
        private final String keyType;
        private final String algorithm;
        private final String issuer;
        private final Instant fetchedAt;

        public PublicKeyInfo(String publicKey, String keyType, String algorithm, String issuer, Instant fetchedAt) {
            this.publicKey = publicKey;
            this.keyType = keyType;
            this.algorithm = algorithm;
            this.issuer = issuer;
            this.fetchedAt = fetchedAt;
        }

        // Getters
        public String getPublicKey() { return publicKey; }
        public String getKeyType() { return keyType; }
        public String getAlgorithm() { return algorithm; }
        public String getIssuer() { return issuer; }
        public Instant getFetchedAt() { return fetchedAt; }

        @Override
        public String toString() {
            return String.format("PublicKeyInfo{keyType='%s', algorithm='%s', issuer='%s', fetchedAt=%s}",
                    keyType, algorithm, issuer, fetchedAt);
        }
    }

    /**
     * Validiert ein JWT Token und extrahiert die Claims
     *
     * @param token das zu validierende Token
     * @return die extrahierten Claims
     * @throws NimbusException bei Fehlern während der Validierung
     */
    public Claims validateToken(String token) throws NimbusException {
        if (token == null || token.trim().isEmpty()) {
            throw new NimbusException("Token cannot be null or empty", "TOKEN_INVALID", "nimbus-common");
        }

        try {
            logger.debug("Validating JWT token");

            // Hole den Public Key vom Identity Service
            PublicKeyInfo keyInfo = getPublicKey();
            if (keyInfo == null || keyInfo.getPublicKey() == null) {
                throw new NimbusException("Public key not available", "PUBLIC_KEY_UNAVAILABLE", "nimbus-common");
            }

            // Validiere das Token und extrahiere die Claims
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(parsePublicKey(keyInfo.getPublicKey(), keyInfo.getAlgorithm()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            logger.debug("Token validation successful for subject: {}", claims.getSubject());
            return claims;

        } catch (ExpiredJwtException e) {
            logger.warn("Token expired: {}", e.getMessage());
            throw new NimbusException("Token is expired", "TOKEN_EXPIRED", "nimbus-common", e);
        } catch (MalformedJwtException e) {
            logger.warn("Malformed token: {}", e.getMessage());
            throw new NimbusException("Token is malformed", "TOKEN_MALFORMED", "nimbus-common", e);
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported token: {}", e.getMessage());
            throw new NimbusException("Token is unsupported", "TOKEN_UNSUPPORTED", "nimbus-common", e);
        } catch (SignatureException e) {
            logger.warn("Invalid token signature: {}", e.getMessage());
            throw new NimbusException("Token signature is invalid", "TOKEN_INVALID_SIGNATURE", "nimbus-common", e);
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage(), e);
            throw new NimbusException("Token validation failed: " + e.getMessage(), "TOKEN_VALIDATION_ERROR", "nimbus-common", e);
        }
    }

    /**
     * Validiert ein JWT Token und gibt detaillierte Informationen zurück
     *
     * @param token das zu validierende Token
     * @return TokenValidationResult mit Validierungsergebnis und Claims
     */
    public TokenValidationResult validateTokenDetailed(String token) {
        try {
            Claims claims = validateToken(token);
            return new TokenValidationResult(true, claims, null, null);
        } catch (NimbusException e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return new TokenValidationResult(false, null, e.getMessage(), e.getErrorCode());
        }
    }

    /**
     * Konvertiert einen Base64-codierten Public Key String zu einem Key-Objekt
     *
     * @param publicKeyBase64 der Base64-codierte Public Key
     * @param algorithm der verwendete Algorithmus
     * @return das Key-Objekt für die JWT-Validierung
     * @throws Exception bei Konvertierungsfehlern
     */
    private Key parsePublicKey(String publicKeyBase64, String algorithm) throws Exception {
        try {
            // Entferne PEM-Header/Footer falls vorhanden
            String cleanKey = publicKeyBase64
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            // Dekodiere Base64
            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);

            // Erstelle KeySpec
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

            // Bestimme den KeyFactory-Algorithmus basierend auf dem Algorithm-Parameter
            String keyAlgorithm = determineKeyAlgorithm(algorithm);
            KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm);

            return keyFactory.generatePublic(keySpec);

        } catch (Exception e) {
            logger.error("Failed to parse public key: {}", e.getMessage(), e);
            throw new Exception("Failed to parse public key: " + e.getMessage(), e);
        }
    }

    /**
     * Bestimmt den Key-Algorithmus basierend auf dem JWT-Algorithmus
     *
     * @param jwtAlgorithm der JWT-Algorithmus (z.B. "RS256", "RS512")
     * @return der entsprechende Key-Algorithmus
     */
    private String determineKeyAlgorithm(String jwtAlgorithm) {
        if (jwtAlgorithm == null) {
            return "RSA"; // Standard
        }

        if (jwtAlgorithm.startsWith("RS") || jwtAlgorithm.startsWith("PS")) {
            return "RSA";
        } else if (jwtAlgorithm.startsWith("ES")) {
            return "EC";
        } else {
            return "RSA"; // Standard fallback
        }
    }

    /**
     * Extrahiert den Benutzernamen aus einem JWT Token ohne vollständige Validierung
     * Nützlich für Logging oder wenn nur der Username benötigt wird
     *
     * @param token das JWT Token
     * @return der Benutzername oder null bei Fehlern
     */
    public String extractUsernameFromToken(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            logger.debug("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Prüft ob ein Token noch gültig ist (noch nicht abgelaufen)
     *
     * @param token das zu prüfende Token
     * @return true wenn das Token noch gültig ist
     */
    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (NimbusException e) {
            return false;
        }
    }

    /**
     * Token-Validierungsergebnis DTO
     */
    public static class TokenValidationResult {
        private final boolean valid;
        private final Claims claims;
        private final String errorMessage;
        private final String errorCode;

        public TokenValidationResult(boolean valid, Claims claims, String errorMessage, String errorCode) {
            this.valid = valid;
            this.claims = claims;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }

        // Getters
        public boolean isValid() { return valid; }
        public Claims getClaims() { return claims; }
        public String getErrorMessage() { return errorMessage; }
        public String getErrorCode() { return errorCode; }

        /**
         * Extrahiert den Subject (Benutzername) aus den Claims
         * @return der Subject oder null wenn nicht verfügbar
         */
        public String getSubject() {
            return claims != null ? claims.getSubject() : null;
        }

        /**
         * Extrahiert das Ablaufdatum aus den Claims
         * @return das Ablaufdatum oder null wenn nicht verfügbar
         */
        public Instant getExpirationTime() {
            return claims != null && claims.getExpiration() != null
                ? claims.getExpiration().toInstant()
                : null;
        }

        /**
         * Prüft ob das Token abgelaufen ist
         * @return true wenn das Token abgelaufen ist
         */
        public boolean isExpired() {
            Instant expiration = getExpirationTime();
            return expiration != null && Instant.now().isAfter(expiration);
        }
    }
}
