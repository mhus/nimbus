package de.mhus.nimbus.registry.consumer;

import de.mhus.nimbus.registry.service.PlanetRegistryService;
import de.mhus.nimbus.shared.avro.Environment;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationRequest;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationResponse;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test für UnregisterPlanetConsumer mit H2-Datenbank
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class UnregisterPlanetConsumerTest {

    @Autowired
    private PlanetRegistryService planetRegistryService;

    @Test
    void testUnregistrationOfNonExistentPlanet() {
        // Erstelle eine Unregistrierung-Anfrage für einen nicht existierenden Planeten
        PlanetUnregistrationRequest request = PlanetUnregistrationRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setPlanetName("NonExistentPlanet")
                .setEnvironment(Environment.TEST)
                .setTimestamp(Instant.now())
                .setUnregisteredBy("test-user")
                .setReason("Test unregistration")
                .build();

        // Validiere die Anfrage
        assertDoesNotThrow(() -> planetRegistryService.validateUnregistrationRequest(request));

        // Verarbeite die Anfrage
        PlanetUnregistrationResponse response = planetRegistryService.processUnregistrationRequest(request);

        // Verifikationen
        assertNotNull(response);
        assertEquals(request.getRequestId(), response.getRequestId());
        assertEquals(PlanetUnregistrationStatus.PLANET_NOT_FOUND, response.getStatus());
        assertEquals("NonExistentPlanet", response.getPlanetName());
        assertEquals(Environment.TEST, response.getEnvironment());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("not found"));
    }

    @Test
    void testValidationOfInvalidRequest() {
        // Test mit null Request
        assertThrows(IllegalArgumentException.class,
            () -> planetRegistryService.validateUnregistrationRequest(null));

        // Test mit leerem Planet Namen
        PlanetUnregistrationRequest invalidRequest = PlanetUnregistrationRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setPlanetName("")
                .setEnvironment(Environment.TEST)
                .setTimestamp(Instant.now())
                .build();

        assertThrows(IllegalArgumentException.class,
            () -> planetRegistryService.validateUnregistrationRequest(invalidRequest));
    }
}
