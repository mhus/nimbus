package de.mhus.nimbus.shared.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * Utility class for JWT token operations and password hashing.
 * Provides methods for validating JWT tokens using RSA public key and password hashing.
 */
@Slf4j
public class IdentityServiceUtils {

    private static final String ISSUER = "identity-service";
    private RSAPublicKey publicKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public IdentityServiceUtils() {
        loadPublicKey();
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * DTO for login request.
     */
    @Data
    public static class LoginRequest {
        @JsonProperty("userId")
        private String userId;

        @JsonProperty("password")
        private String password;

        public LoginRequest(String userId, String password) {
            this.userId = userId;
            this.password = password;
        }
    }

    /**
     * DTO for login response.
     */
    @Data
    public static class LoginResponse {
        @JsonProperty("token")
        private String token;

        @JsonProperty("expires_at")
        private Long expiresAt;
    }

    /**
     * Loads the RSA public key from the classpath.
     */
    private void loadPublicKey() {
        try {
            ClassPathResource resource = new ClassPathResource("public.key");

            // Use InputStream to read from JAR or file system
            String keyContent;
            try (InputStream inputStream = resource.getInputStream()) {
                keyContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Remove header, footer and whitespace
            keyContent = keyContent
                    .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyContent);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey = (RSAPublicKey) keyFactory.generatePublic(spec);

            log.info("Public key loaded successfully for JWT validation");
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Failed to load public key: {}", e.getMessage());
            throw new RuntimeException("Could not load public key for JWT validation", e);
        }
    }

    /**
     * Validates a JWT token and returns the decoded token if valid.
     *
     * @param token the JWT token to validate
     * @return decoded JWT if valid, null if invalid
     */
    public DecodedJWT validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            log.warn("JWT verification failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the user ID from a JWT token.
     *
     * @param token the JWT token
     * @return user ID if token is valid, null otherwise
     */
    public String extractUserId(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT != null ? decodedJWT.getSubject() : null;
    }

    /**
     * Extracts the roles from a JWT token.
     *
     * @param token the JWT token
     * @return list of roles if token is valid, null otherwise
     */
    public List<String> extractRoles(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT != null ? decodedJWT.getClaim("roles").asList(String.class) : null;
    }

    /**
     * Extracts the expiration time from a JWT token.
     *
     * @param token the JWT token
     * @return expiration time as Unix timestamp, null if token invalid
     */
    public Long extractExpirationTime(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT != null ? decodedJWT.getExpiresAt().getTime() / 1000 : null;
    }

    /**
     * Extracts the issued time from a JWT token.
     *
     * @param token the JWT token
     * @return issued time as Unix timestamp, null if token invalid
     */
    public Long extractIssuedTime(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT != null ? decodedJWT.getIssuedAt().getTime() / 1000 : null;
    }

    /**
     * Performs a login against the Identity Service via REST request.
     *
     * @param url      the base URL of the Identity Service
     * @param username the username for login
     * @param password the password for login
     * @return JWT token if login successful
     * @throws RuntimeException if login fails or communication error occurs
     */
    public String login(String url, String username, String password) {
        try {
            // Prepare login request
            LoginRequest loginRequest = new LoginRequest(username, password);

            // Set up HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create HTTP entity with request body
            HttpEntity<LoginRequest> entity = new HttpEntity<>(loginRequest, headers);

            // Construct login endpoint URL
            String loginUrl = url.endsWith("/") ? url + "login" : url + "/login";

            log.debug("Attempting login for user '{}' at URL: {}", username, loginUrl);

            // Perform POST request to login endpoint
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                    loginUrl,
                    entity,
                    LoginResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String token = response.getBody().getToken();
                log.info("Login successful for user '{}'", username);
                return token;
            } else {
                log.warn("Login failed for user '{}': Invalid response", username);
                throw new RuntimeException("Login failed: Invalid response from Identity Service");
            }

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("Login failed for user '{}': Invalid credentials", username);
                throw new RuntimeException("Login failed: Invalid username or password");
            } else {
                log.error("Login failed for user '{}': HTTP error {}", username, e.getStatusCode());
                throw new RuntimeException("Login failed: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Login failed for user '{}': {}", username, e.getMessage());
            throw new RuntimeException("Login failed: Communication error with Identity Service", e);
        }
    }

    /**
     * Creates a SHA256 hash of a password using the user ID as salt.
     *
     * @param password the plain text password
     * @param userId   the user ID to use as salt
     * @return SHA256 hash as hex string
     */
    public String hashPassword(String password, String userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedPassword = password + userId;
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies a password against a stored hash.
     *
     * @param password   the plain text password to verify
     * @param userId     the user ID used as salt
     * @param storedHash the stored password hash
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(String password, String userId, String storedHash) {
        String computedHash = hashPassword(password, userId);
        return computedHash.equals(storedHash);
    }

    /**
     * Converts byte array to hexadecimal string.
     *
     * @param bytes the byte array
     * @return hexadecimal string representation
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
