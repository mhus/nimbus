package de.mhus.nimbus.world.shared.auth;

import de.mhus.nimbus.world.shared.auth.WorldAuthContext;
import de.mhus.nimbus.world.shared.auth.WorldAuthorizationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorldAuthorizationIntegrationTest {

    private WorldAuthorizationUtils authUtils;

    @BeforeEach
    void setUp() {
        authUtils = new WorldAuthorizationUtils();
        ReflectionTestUtils.setField(authUtils, "sharedSecret", "test-integration-secret");
    }

    @Test
    void testCompleteAuthenticationFlow() {
        // Test the complete authentication flow
        String validHash = authUtils.generateSecretHash();

        // Validate authentication
        assertTrue(authUtils.validateAuthHeader(validHash));

        // Extract user information
        String username = authUtils.extractUsername("testuser");
        assertEquals("testuser", username);

        List<String> roles = authUtils.extractRoles("ADMIN,USER");
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ADMIN"));
        assertTrue(roles.contains("USER"));

        // Create authenticated context
        WorldAuthContext context = authUtils.createAuthContext(username, roles);
        assertTrue(context.isAuthenticated());
        assertEquals("testuser", context.getUsername());
        assertEquals(roles, context.getRoles());

        // Test role checking
        assertTrue(context.hasRole("ADMIN"));
        assertTrue(context.hasRole("USER"));
        assertFalse(context.hasRole("MODERATOR"));

        assertTrue(context.hasAnyRole("ADMIN", "MODERATOR"));
        assertTrue(context.hasAnyRole("USER"));
        assertFalse(context.hasAnyRole("MODERATOR", "GUEST"));
    }

    @Test
    void testInvalidAuthenticationFlow() {
        // Test invalid authentication
        assertFalse(authUtils.validateAuthHeader("invalid-hash"));
        assertFalse(authUtils.validateAuthHeader(""));
        assertFalse(authUtils.validateAuthHeader(null));

        // Test invalid username
        assertNull(authUtils.extractUsername("invalid@user"));
        assertNull(authUtils.extractUsername(""));
        assertNull(authUtils.extractUsername(null));

        // Test unauthenticated context
        WorldAuthContext context = authUtils.createUnauthenticatedContext();
        assertFalse(context.isAuthenticated());
        assertNull(context.getUsername());
        assertNull(context.getRoles());

        // Test role checking on unauthenticated context
        assertFalse(context.hasRole("ADMIN"));
        assertFalse(context.hasAnyRole("ADMIN", "USER"));
    }

    @Test
    void testSecretHashConsistency() {
        // Hash should be consistent across multiple calls
        String hash1 = authUtils.generateSecretHash();
        String hash2 = authUtils.generateSecretHash();
        assertEquals(hash1, hash2);

        // Different secrets should produce different hashes
        WorldAuthorizationUtils authUtils2 = new WorldAuthorizationUtils();
        ReflectionTestUtils.setField(authUtils2, "sharedSecret", "different-secret");
        String differentHash = authUtils2.generateSecretHash();
        assertNotEquals(hash1, differentHash);
    }
}
