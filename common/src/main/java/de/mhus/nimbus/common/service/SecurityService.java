package de.mhus.nimbus.common.service;

import de.mhus.nimbus.common.constants.NimbusConstants;
import de.mhus.nimbus.common.dto.NimbusResponse;
import de.mhus.nimbus.common.exception.NimbusException;
import de.mhus.nimbus.common.util.RequestIdUtils;
import de.mhus.nimbus.shared.avro.LoginRequest;
import de.mhus.nimbus.shared.avro.LoginResponse;
import de.mhus.nimbus.shared.avro.LoginStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Security Service für JWT Token Management via Kafka
 * Implementiert Login-Funktionalität mit dem Identity Service
 */
@Service
public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);
    private static final long LOGIN_TIMEOUT_MS = 30000; // 30 Sekunden

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RequestIdUtils requestIdUtils;

    // Speichert pending Login-Requests
    private final ConcurrentHashMap<String, CompletableFuture<LoginResponse>> pendingLogins = new ConcurrentHashMap<>();

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
     * Cleanup-Methode für abgelaufene Login-Requests
     */
    public void cleanupExpiredRequests() {
        pendingLogins.entrySet().removeIf(entry -> {
            if (entry.getValue().isDone() || entry.getValue().isCancelled()) {
                return true;
            }
            // Timeout nach 5 Minuten
            entry.getValue().completeExceptionally(
                    new NimbusException("Login request timed out", "TIMEOUT_ERROR", "nimbus-common"));
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
}
