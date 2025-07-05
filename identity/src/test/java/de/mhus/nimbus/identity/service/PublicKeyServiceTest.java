package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.config.JwtProperties;
import de.mhus.nimbus.shared.avro.PublicKeyRequest;
import de.mhus.nimbus.shared.avro.PublicKeyResponse;
import de.mhus.nimbus.shared.avro.PublicKeyStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test für PublicKeyService
 */
@SpringBootTest
@ActiveProfiles("test")
class PublicKeyServiceTest {

    @Autowired
    private PublicKeyService publicKeyService;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void testProcessPublicKeyRequest() {
        // Given
        PublicKeyRequest request = PublicKeyRequest.newBuilder()
                .setRequestId("test-public-key-1")
                .setTimestamp(Instant.now().toEpochMilli())
                .setRequestedBy("test-service")
                .build();

        // When
        PublicKeyResponse response = publicKeyService.processPublicKeyRequest(request);

        // Then
        assertNotNull(response);
        assertEquals("test-public-key-1", response.getRequestId());
        assertEquals(PublicKeyStatus.SUCCESS, response.getStatus());
        assertNotNull(response.getPublicKey());
        assertFalse(response.getPublicKey().isEmpty());
        assertEquals("RSA", response.getKeyType());
        assertEquals("RS256", response.getAlgorithm());
        assertEquals(jwtProperties.getIssuer(), response.getIssuer());
        assertNull(response.getErrorMessage());
        assertTrue(response.getTimestamp() > 0);
    }

    @Test
    void testValidatePublicKeyRequest() {
        // Valid request
        PublicKeyRequest validRequest = PublicKeyRequest.newBuilder()
                .setRequestId("valid-request")
                .setTimestamp(Instant.now().toEpochMilli())
                .setRequestedBy("test-service")
                .build();

        assertDoesNotThrow(() -> publicKeyService.validatePublicKeyRequest(validRequest));

        // Invalid request - null
        assertThrows(IllegalArgumentException.class,
                    () -> publicKeyService.validatePublicKeyRequest(null));

        // Invalid request - empty requestId
        PublicKeyRequest invalidRequest = PublicKeyRequest.newBuilder()
                .setRequestId("")
                .setTimestamp(Instant.now().toEpochMilli())
                .build();

        assertThrows(IllegalArgumentException.class,
                    () -> publicKeyService.validatePublicKeyRequest(invalidRequest));
    }

    @Test
    void testCreatePublicKeyErrorResponse() {
        // Given
        PublicKeyRequest request = PublicKeyRequest.newBuilder()
                .setRequestId("error-test")
                .setTimestamp(Instant.now().toEpochMilli())
                .build();

        String errorMessage = "Test error message";

        // When
        PublicKeyResponse response = publicKeyService.createPublicKeyErrorResponse(request, errorMessage);

        // Then
        assertNotNull(response);
        assertEquals("error-test", response.getRequestId());
        assertEquals(PublicKeyStatus.ERROR, response.getStatus());
        assertNull(response.getPublicKey());
        assertNull(response.getKeyType());
        assertNull(response.getAlgorithm());
        assertNull(response.getIssuer());
        assertEquals(errorMessage, response.getErrorMessage());
        assertTrue(response.getTimestamp() > 0);
    }

    @Test
    void testPublicKeyConsistency() {
        // Given - Mehrere Anfragen
        PublicKeyRequest request1 = PublicKeyRequest.newBuilder()
                .setRequestId("consistency-test-1")
                .setTimestamp(Instant.now().toEpochMilli())
                .build();

        PublicKeyRequest request2 = PublicKeyRequest.newBuilder()
                .setRequestId("consistency-test-2")
                .setTimestamp(Instant.now().toEpochMilli())
                .build();

        // When
        PublicKeyResponse response1 = publicKeyService.processPublicKeyRequest(request1);
        PublicKeyResponse response2 = publicKeyService.processPublicKeyRequest(request2);

        // Then - Public Key sollte immer derselbe sein
        assertEquals(response1.getPublicKey(), response2.getPublicKey());
        assertEquals(response1.getKeyType(), response2.getKeyType());
        assertEquals(response1.getAlgorithm(), response2.getAlgorithm());
        assertEquals(response1.getIssuer(), response2.getIssuer());
    }
}
