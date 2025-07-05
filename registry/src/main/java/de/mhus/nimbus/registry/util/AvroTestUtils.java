package de.mhus.nimbus.registry.util;

import de.mhus.nimbus.shared.avro.Environment;
import de.mhus.nimbus.shared.avro.LookupRequest;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

@Component
public class AvroTestUtils {

    private final DatumWriter<LookupRequest> requestWriter;

    public AvroTestUtils() {
        this.requestWriter = new SpecificDatumWriter<>(LookupRequest.class);
    }

    public byte[] createTestLookupRequest(String serviceName) throws IOException {
        return createTestLookupRequest(serviceName, null, Environment.DEV);
    }

    public byte[] createTestLookupRequest(String serviceName, String version, Environment environment) throws IOException {
        long currentTimestamp = Instant.now().toEpochMilli();

        LookupRequest request = LookupRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setService(serviceName)
                .setVersion(version)
                .setEnvironment(environment)
                .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                .setMetadata(new HashMap<>())
                .build();

        return serializeLookupRequest(request);
    }

    private byte[] serializeLookupRequest(LookupRequest request) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        requestWriter.write(request, EncoderFactory.get().binaryEncoder(outputStream, null));
        return outputStream.toByteArray();
    }
}
