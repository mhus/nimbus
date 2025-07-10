package de.mhus.nimbus.registry.consumer;

import de.mhus.nimbus.registry.service.PlanetRegistryService;
import de.mhus.nimbus.shared.avro.Environment;
import de.mhus.nimbus.shared.avro.WorldUnregistrationRequest;
import de.mhus.nimbus.shared.avro.WorldUnregistrationResponse;
import de.mhus.nimbus.shared.avro.WorldUnregistrationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test für UnregisterWorldConsumer mit H2-Datenbank
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class UnregisterWorldConsumerTest {

    @Autowired
    private PlanetRegistryService planetRegistryService;

    @Test
    void testWorldUnregistrationOfNonExistentWorld() {
        // Erstelle eine World-Deregistrierung-Anfrage für eine nicht existierende Welt
        WorldUnregistrationRequest request = WorldUnregistrationRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setWorldId("non-existent-world")
                .setPlanetName("SomePlanet")
                .setEnvironment(Environment.TEST)
                .setTimestamp(Instant.now())
                .setUnregisteredBy("test-user")
                .setReason("Test unregistration")
                .setMetadata(new HashMap<>())
                .build();

        // Validiere die Anfrage
        assertDoesNotThrow(() -> planetRegistryService.validateWorldUnregistrationRequest(request));

        // Verarbeite die Anfrage
        WorldUnregistrationResponse response = planetRegistryService.processWorldUnregistrationRequest(request);

        // Verifikationen
        assertNotNull(response);
        assertEquals(request.getRequestId(), response.getRequestId());
        assertEquals(WorldUnregistrationStatus.WORLD_NOT_FOUND, response.getStatus());
        assertEquals("non-existent-world", response.getWorldId());
        assertEquals(Environment.TEST, response.getEnvironment());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("not found"));
    }

    @Test
    void testValidationOfInvalidWorldUnregistrationRequest() {
        // Test mit null Request
        assertThrows(IllegalArgumentException.class,
            () -> planetRegistryService.validateWorldUnregistrationRequest(null));

        // Test mit leerem World ID
        WorldUnregistrationRequest invalidRequest = WorldUnregistrationRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setWorldId("")
                .setEnvironment(Environment.TEST)
                .setTimestamp(Instant.now())
                .build();

        assertThrows(IllegalArgumentException.class,
            () -> planetRegistryService.validateWorldUnregistrationRequest(invalidRequest));

        // Test mit fehlender Request ID
        WorldUnregistrationRequest invalidRequest2 = WorldUnregistrationRequest.newBuilder()
                .setRequestId("")
                .setWorldId("world-001")
                .setEnvironment(Environment.TEST)
                .setTimestamp(Instant.now())
                .build();

        assertThrows(IllegalArgumentException.class,
            () -> planetRegistryService.validateWorldUnregistrationRequest(invalidRequest2));
    }
}
