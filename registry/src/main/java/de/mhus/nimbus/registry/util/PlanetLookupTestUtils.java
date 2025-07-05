package de.mhus.nimbus.registry.util;

import de.mhus.nimbus.shared.avro.Environment;
import de.mhus.nimbus.shared.avro.PlanetLookupRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class PlanetLookupTestUtils {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PlanetLookupTestUtils(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sendet eine Test-Planet-Lookup-Request an das planet-lookup Topic
     */
    public CompletableFuture<Void> sendTestPlanetLookupRequest(String planetName) {
        return sendTestPlanetLookupRequest(planetName, null, Environment.DEV, null);
    }

    /**
     * Sendet eine Test-Planet-Lookup-Request für eine spezifische Welt
     */
    public CompletableFuture<Void> sendTestPlanetLookupRequest(String planetName, String worldName) {
        return sendTestPlanetLookupRequest(planetName, worldName, Environment.DEV, null);
    }

    /**
     * Sendet eine Test-Planet-Lookup-Request mit allen Parametern
     */
    public CompletableFuture<Void> sendTestPlanetLookupRequest(String planetName, String worldName,
                                                             Environment environment, String requestedBy) {
        PlanetLookupRequest request = createTestPlanetLookupRequest(planetName, worldName, environment, requestedBy);

        return kafkaTemplate.send("planet-lookup", request.getRequestId(), request)
                .thenApply(result -> null);
    }

    /**
     * Erstellt eine Test-Planet-Lookup-Request
     */
    public PlanetLookupRequest createTestPlanetLookupRequest(String planetName, String worldName,
                                                           Environment environment, String requestedBy) {
        long currentTimestamp = Instant.now().toEpochMilli();

        return PlanetLookupRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setPlanetName(planetName)
                .setWorldName(worldName)
                .setEnvironment(environment)
                .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                .setRequestedBy(requestedBy)
                .setMetadata(new HashMap<>())
                .build();
    }
}
