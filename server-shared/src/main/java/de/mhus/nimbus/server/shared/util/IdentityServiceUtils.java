package de.mhus.nimbus.server.shared.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

/**
 * Utility class for JWT token operations and password hashing.
 * Provides methods for creating, validating and extracting information from JWT tokens.
 */
@Component
@Slf4j
public class IdentityServiceUtils {

    private static final String ISSUER = "identity-service";
    private static final long TOKEN_VALIDITY_HOURS = 2;

    @Value("${nimbus.jwt.secret:default-secret-change-in-production}")
    private String jwtSecret;

    /**
     * Creates a JWT token for the given user ID and roles.
     * @param userId the user identifier
     * @param roles list of user roles
     * @return JWT token string
     */
    public String createToken(String userId, List<String> roles) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + TOKEN_VALIDITY_HOURS * 60 * 60 * 1000);

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(userId)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withClaim("roles", roles)
                .sign(algorithm);
    }

    /**
     * Validates a JWT token and returns the decoded token if valid.
     * @param token the JWT token to validate
     * @return decoded JWT if valid, null if invalid
     */
    public DecodedJWT validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
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
     * @param token the JWT token
     * @return user ID if token is valid, null otherwise
     */
    public String extractUserId(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT != null ? decodedJWT.getSubject() : null;
    }

    /**
     * Extracts the roles from a JWT token.
     * @param token the JWT token
     * @return list of roles if token is valid, null otherwise
     */
    public List<String> extractRoles(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT != null ? decodedJWT.getClaim("roles").asList(String.class) : null;
    }

    /**
     * Extracts the expiration time from a JWT token.
     * @param token the JWT token
     * @return expiration time as Unix timestamp, null if token invalid
     */
    public Long extractExpirationTime(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT != null ? decodedJWT.getExpiresAt().getTime() / 1000 : null;
    }

    /**
     * Extracts the issued time from a JWT token.
     * @param token the JWT token
     * @return issued time as Unix timestamp, null if token invalid
     */
    public Long extractIssuedTime(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT != null ? decodedJWT.getIssuedAt().getTime() / 1000 : null;
    }

    /**
     * Creates a SHA256 hash of a password using the user ID as salt.
     * @param password the plain text password
     * @param userId the user ID to use as salt
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
     * @param password the plain text password to verify
     * @param userId the user ID used as salt
     * @param storedHash the stored password hash
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(String password, String userId, String storedHash) {
        String computedHash = hashPassword(password, userId);
        return computedHash.equals(storedHash);
    }

    /**
     * Converts byte array to hexadecimal string.
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
