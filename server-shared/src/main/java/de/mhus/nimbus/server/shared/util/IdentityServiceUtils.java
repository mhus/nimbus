package de.mhus.nimbus.server.shared.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Utility class for JWT token operations and password hashing.
 * Provides methods for validating JWT tokens using RSA public key and password hashing.
 */
@Component
@Slf4j
public class IdentityServiceUtils {

    private static final String ISSUER = "identity-service";
    private RSAPublicKey publicKey;

    public IdentityServiceUtils() {
        loadPublicKey();
    }

    /**
     * Loads the RSA public key from the classpath.
     */
    private void loadPublicKey() {
        try {
            ClassPathResource resource = new ClassPathResource("public.key");
            String keyContent = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);

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
