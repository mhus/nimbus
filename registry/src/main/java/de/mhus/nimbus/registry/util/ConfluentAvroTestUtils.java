package de.mhus.nimbus.registry.util;

import de.mhus.nimbus.shared.avro.Environment;
import de.mhus.nimbus.shared.avro.LookupRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ConfluentAvroTestUtils {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ConfluentAvroTestUtils(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sendet eine Test-Lookup-Request an das lookup Topic
     */
    public CompletableFuture<Void> sendTestLookupRequest(String serviceName) {
        return sendTestLookupRequest(serviceName, null, Environment.DEV);
    }

    /**
     * Sendet eine Test-Lookup-Request mit spezifischen Parametern
     */
    public CompletableFuture<Void> sendTestLookupRequest(String serviceName, String version, Environment environment) {
        LookupRequest request = createTestLookupRequest(serviceName, version, environment);

        return kafkaTemplate.send("lookup", request.getRequestId(), request)
                .thenApply(result -> null);
    }

    /**
     * Erstellt eine Test-Lookup-Request
     */
    public LookupRequest createTestLookupRequest(String serviceName, String version, Environment environment) {
        long currentTimestamp = Instant.now().toEpochMilli();

        return LookupRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setService(serviceName)
                .setVersion(version)
                .setEnvironment(environment)
                .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                .setMetadata(new HashMap<>())
                .build();
    }
}
