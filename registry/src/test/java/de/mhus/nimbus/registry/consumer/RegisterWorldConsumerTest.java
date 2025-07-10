package de.mhus.nimbus.registry.consumer;

import de.mhus.nimbus.registry.service.PlanetRegistryService;
import de.mhus.nimbus.shared.avro.Environment;
import de.mhus.nimbus.shared.avro.WorldRegistrationRequest;
import de.mhus.nimbus.shared.avro.WorldRegistrationResponse;
import de.mhus.nimbus.shared.avro.WorldRegistrationStatus;
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
 * Test für RegisterWorldConsumer mit H2-Datenbank
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class RegisterWorldConsumerTest {

    @Autowired
    private PlanetRegistryService planetRegistryService;

    @Test
    void testWorldRegistrationOnNonExistentPlanet() {
        // Erstelle eine World-Registrierung-Anfrage für einen nicht existierenden Planeten
        WorldRegistrationRequest request = WorldRegistrationRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setPlanetName("NonExistentPlanet")
                .setWorldId("world-001")
                .setWorldName("Test World")
                .setEnvironment(Environment.TEST)
                .setManagementUrl("http://localhost:8080/management")
                .setApiUrl("http://localhost:8080/api")
                .setWebUrl("http://localhost:8080")
                .setDescription("Test world description")
                .setWorldType("settlement")
                .setAccessLevel("public")
                .setTimestamp(Instant.now())
                .setRegisteredBy("test-user")
                .setMetadata(new HashMap<>())
                .build();

        // Validiere die Anfrage
        assertDoesNotThrow(() -> planetRegistryService.validateWorldRegistrationRequest(request));

        // Verarbeite die Anfrage
        WorldRegistrationResponse response = planetRegistryService.processWorldRegistrationRequest(request);

        // Verifikationen
        assertNotNull(response);
        assertEquals(request.getRequestId(), response.getRequestId());
        assertEquals(WorldRegistrationStatus.PLANET_NOT_FOUND, response.getStatus());
        assertEquals("NonExistentPlanet", response.getPlanetName());
        assertEquals("world-001", response.getWorldId());
        assertEquals(Environment.TEST, response.getEnvironment());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("not found"));
    }

    @Test
    void testValidationOfInvalidWorldRequest() {
        // Test mit null Request
        assertThrows(IllegalArgumentException.class,
            () -> planetRegistryService.validateWorldRegistrationRequest(null));

        // Test mit leerem World Namen
        WorldRegistrationRequest invalidRequest = WorldRegistrationRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setPlanetName("TestPlanet")
                .setWorldId("world-001")
                .setWorldName("")
                .setEnvironment(Environment.TEST)
                .setManagementUrl("http://localhost:8080/management")
                .setTimestamp(Instant.now())
                .build();

        assertThrows(IllegalArgumentException.class,
            () -> planetRegistryService.validateWorldRegistrationRequest(invalidRequest));

        // Test mit fehlender Management-URL
        WorldRegistrationRequest invalidRequest2 = WorldRegistrationRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setPlanetName("TestPlanet")
                .setWorldId("world-001")
                .setWorldName("Test World")
                .setEnvironment(Environment.TEST)
                .setManagementUrl("")
                .setTimestamp(Instant.now())
                .build();

        assertThrows(IllegalArgumentException.class,
            () -> planetRegistryService.validateWorldRegistrationRequest(invalidRequest2));
    }
}
