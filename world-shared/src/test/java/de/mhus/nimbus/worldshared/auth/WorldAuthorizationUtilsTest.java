package de.mhus.nimbus.worldshared.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorldAuthorizationUtilsTest {

    private WorldAuthorizationUtils authUtils;

    @BeforeEach
    void setUp() {
        authUtils = new WorldAuthorizationUtils();
        ReflectionTestUtils.setField(authUtils, "sharedSecret", "test-secret");
    }

    @Test
    void testGenerateSecretHash() {
        String hash = authUtils.generateSecretHash();
        assertNotNull(hash);
        assertFalse(hash.isEmpty());

        // Should be consistent
        assertEquals(hash, authUtils.generateSecretHash());
    }

    @Test
    void testValidateAuthHeader_ValidHeader() {
        String validHash = authUtils.generateSecretHash();
        assertTrue(authUtils.validateAuthHeader(validHash));
    }

    @Test
    void testValidateAuthHeader_InvalidHeader() {
        assertFalse(authUtils.validateAuthHeader("invalid-hash"));
        assertFalse(authUtils.validateAuthHeader(""));
        assertFalse(authUtils.validateAuthHeader(null));
    }

    @Test
    void testExtractUsername_ValidUsername() {
        assertEquals("testuser", authUtils.extractUsername("testuser"));
        assertEquals("user_123", authUtils.extractUsername("user_123"));
        assertEquals("user-name", authUtils.extractUsername("user-name"));
    }

    @Test
    void testExtractUsername_InvalidUsername() {
        assertNull(authUtils.extractUsername(""));
        assertNull(authUtils.extractUsername(null));
        assertNull(authUtils.extractUsername("user@domain.com")); // Contains @
        assertNull(authUtils.extractUsername("a".repeat(51))); // Too long
        assertNull(authUtils.extractUsername("user space")); // Contains space
    }

    @Test
    void testExtractRoles_ValidRoles() {
        List<String> roles = authUtils.extractRoles("ADMIN,USER");
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ADMIN"));
        assertTrue(roles.contains("USER"));
    }

    @Test
    void testExtractRoles_SingleRole() {
        List<String> roles = authUtils.extractRoles("ADMIN");
        assertEquals(1, roles.size());
        assertEquals("ADMIN", roles.get(0));
    }

    @Test
    void testExtractRoles_WithSpaces() {
        List<String> roles = authUtils.extractRoles("ADMIN, USER , MODERATOR");
        assertEquals(3, roles.size());
        assertTrue(roles.contains("ADMIN"));
        assertTrue(roles.contains("USER"));
        assertTrue(roles.contains("MODERATOR"));
    }

    @Test
    void testExtractRoles_InvalidRoles() {
        List<String> roles = authUtils.extractRoles("admin,user"); // lowercase
        assertTrue(roles.isEmpty());

        roles = authUtils.extractRoles("ADMIN,user@domain"); // invalid characters
        assertEquals(1, roles.size());
        assertEquals("ADMIN", roles.get(0));
    }

    @Test
    void testExtractRoles_EmptyOrNull() {
        assertTrue(authUtils.extractRoles("").isEmpty());
        assertTrue(authUtils.extractRoles(null).isEmpty());
        assertTrue(authUtils.extractRoles("   ").isEmpty());
    }

    @Test
    void testCreateAuthContext_Authenticated() {
        List<String> roles = List.of("ADMIN", "USER");
        WorldAuthContext context = authUtils.createAuthContext("testuser", roles);

        assertTrue(context.isAuthenticated());
        assertEquals("testuser", context.getUsername());
        assertEquals(roles, context.getRoles());
    }

    @Test
    void testCreateUnauthenticatedContext() {
        WorldAuthContext context = authUtils.createUnauthenticatedContext();

        assertFalse(context.isAuthenticated());
        assertNull(context.getUsername());
        assertNull(context.getRoles());
    }
}
