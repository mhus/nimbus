package de.mhus.nimbus.world.shared.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Utility class for world service authentication using a shared secret.
 * Provides methods to validate authentication headers and extract user information.
 */
@Component
@Slf4j
public class WorldAuthorizationUtils {

    public static final String AUTH_HEADER = "X-World-Auth";
    public static final String USERNAME_HEADER = "X-World-Username";
    public static final String ROLES_HEADER = "X-World-Roles";

    @Value("${nimbus.world.shared.secret:default-secret}")
    private String sharedSecret;

    /**
     * Validates the authentication header against the shared secret.
     *
     * @param authHeader the authentication header value
     * @return true if the header is valid, false otherwise
     */
    public boolean validateAuthHeader(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            log.debug("Missing authentication header");
            return false;
        }

        try {
            String expectedHash = generateSecretHash();
            return MessageDigest.isEqual(
                authHeader.getBytes(),
                expectedHash.getBytes()
            );
        } catch (Exception e) {
            log.error("Error validating auth header", e);
            return false;
        }
    }

    /**
     * Generates the expected hash for the shared secret.
     *
     * @return the base64 encoded hash of the shared secret
     */
    public String generateSecretHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sharedSecret.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Extracts and validates the username from the request headers.
     *
     * @param username the username header value
     * @return the username if valid, null otherwise
     */
    public String extractUsername(String username) {
        if (!StringUtils.hasText(username)) {
            log.debug("Missing username header");
            return null;
        }

        // Basic validation - username should be alphanumeric and reasonable length
        if (username.matches("^[a-zA-Z0-9_-]{1,50}$")) {
            return username;
        }

        log.debug("Invalid username format: {}", username);
        return null;
    }

    /**
     * Extracts and validates user roles from the request headers.
     *
     * @param rolesHeader the roles header value (comma-separated)
     * @return list of valid roles, empty list if no valid roles found
     */
    public List<String> extractRoles(String rolesHeader) {
        if (!StringUtils.hasText(rolesHeader)) {
            log.debug("Missing roles header");
            return List.of();
        }

        return Arrays.stream(rolesHeader.split(","))
            .map(String::trim)
            .filter(role -> StringUtils.hasText(role))
            .filter(role -> role.matches("^[A-Z_]{1,30}$")) // Roles should be uppercase with underscores
            .toList();
    }

    /**
     * Creates an authentication context for the current request.
     *
     * @param username the authenticated username
     * @param roles the user's roles
     * @return the authentication context
     */
    public WorldAuthContext createAuthContext(String username, List<String> roles) {
        return WorldAuthContext.builder()
            .username(username)
            .roles(roles)
            .authenticated(true)
            .build();
    }

    /**
     * Creates an unauthenticated context.
     *
     * @return the unauthenticated context
     */
    public WorldAuthContext createUnauthenticatedContext() {
        return WorldAuthContext.builder()
            .authenticated(false)
            .build();
    }
}
