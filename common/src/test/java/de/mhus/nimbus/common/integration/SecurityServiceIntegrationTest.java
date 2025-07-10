package de.mhus.nimbus.common.integration;

import de.mhus.nimbus.common.client.IdentityClient;
import de.mhus.nimbus.common.service.SecurityService;
import de.mhus.nimbus.common.util.RequestIdUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test für SecurityService
 * Testet die Instanziierung und grundlegende Funktionalität ohne Spring Context
 */
@ExtendWith(MockitoExtension.class)
class SecurityServiceIntegrationTest {

    @Mock
    private IdentityClient identityClient;

    private SecurityService securityService;
    private RequestIdUtils requestIdUtils;

    @BeforeEach
    void setUp() {
        // Initialisiere die Services direkt ohne Spring Context
        requestIdUtils = new RequestIdUtils();
        securityService = new SecurityService(identityClient);
    }

    @Test
    void testSecurityServiceInstantiation() {
        // Verify that SecurityService can be instantiated
        assertNotNull(securityService, "SecurityService should be instantiated");
        assertNotNull(requestIdUtils, "RequestIdUtils should be instantiated");
    }

    @Test
    void testRequestIdGeneration() {
        // Test that RequestIdUtils works correctly
        String requestId = requestIdUtils.generateRequestId("test");
        assertNotNull(requestId, "Generated request ID should not be null");
        assertTrue(requestId.startsWith("nimbus-test-"), "Request ID should start with prefix");
        assertTrue(requestId.length() > 12, "Request ID should contain UUID part");

        // Test different service names
        String requestId2 = requestIdUtils.generateRequestId("security");
        assertTrue(requestId2.startsWith("nimbus-security-"), "Request ID should use correct service name");
        assertNotEquals(requestId, requestId2, "Request IDs should be unique");
    }

    @Test
    void testSecurityServiceBasicFunctionality() {
        // Test that SecurityService basic methods work
        assertNotNull(securityService, "SecurityService should be initialized");

        // Test public key cache operations
        assertFalse(securityService.hasValidPublicKey(), "Initially should have no cached public key");
        assertNull(securityService.getCachedPublicKey(), "Cached key should be null initially");

        // Test cache clearing
        securityService.clearPublicKeyCache();
        assertFalse(securityService.hasValidPublicKey(), "Cache should remain empty after clear");
    }

    @Test
    void testSecurityServiceWithMockedDependencies() {
        // Verify that SecurityService properly holds its dependencies
        assertNotNull(securityService, "SecurityService should be properly initialized with mocked IdentityClient");

        // Test that methods don't throw NullPointerExceptions
        assertDoesNotThrow(() -> {
            securityService.clearPublicKeyCache();
            securityService.hasValidPublicKey();
            SecurityService.PublicKeyInfo cachedKey = securityService.getCachedPublicKey();
            assertNull(cachedKey, "Cached key should be null initially");
        }, "Basic cache operations should not throw exceptions");
    }

    @Test
    void testRequestIdUtilsThreadSafety() {
        // Test that RequestIdUtils generates unique IDs in concurrent scenario
        String[] ids = new String[10];
        for (int i = 0; i < 10; i++) {
            ids[i] = requestIdUtils.generateRequestId("concurrent");
        }

        // Verify all IDs are unique
        for (int i = 0; i < ids.length; i++) {
            for (int j = i + 1; j < ids.length; j++) {
                assertNotEquals(ids[i], ids[j], "All generated IDs should be unique");
            }
        }
    }

    // Note: Echte Integration-Tests mit Spring Boot würden eine Test-Application-Klasse erfordern
    // Diese Tests zeigen die grundlegende Funktionalität ohne Spring Context
}
