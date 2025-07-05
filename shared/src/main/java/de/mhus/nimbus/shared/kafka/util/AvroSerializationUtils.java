package de.mhus.nimbus.shared.kafka.util;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility-Klasse für Avro-Serialisierung und -Deserialisierung
 */
public class AvroSerializationUtils {

    // Cache für DatumReader und DatumWriter
    private static final ConcurrentHashMap<Class<?>, DatumReader<?>> readerCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, DatumWriter<?>> writerCache = new ConcurrentHashMap<>();

    /**
     * Serialisiert ein Avro-Objekt zu byte[]
     */
    @SuppressWarnings("unchecked")
    public static <T extends SpecificRecord> byte[] serialize(T record) throws IOException {
        Class<T> clazz = (Class<T>) record.getClass();
        DatumWriter<T> writer = (DatumWriter<T>) writerCache.computeIfAbsent(clazz,
            k -> new SpecificDatumWriter<>(clazz));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.write(record, EncoderFactory.get().binaryEncoder(outputStream, null));
        return outputStream.toByteArray();
    }

    /**
     * Deserialisiert byte[] zu einem Avro-Objekt
     */
    @SuppressWarnings("unchecked")
    public static <T extends SpecificRecord> T deserialize(byte[] data, Class<T> clazz) throws IOException {
        DatumReader<T> reader = (DatumReader<T>) readerCache.computeIfAbsent(clazz,
            k -> new SpecificDatumReader<>(clazz));

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        return reader.read(null, DecoderFactory.get().binaryDecoder(inputStream, null));
    }

    /**
     * Räumt den Cache auf
     */
    public static void clearCache() {
        readerCache.clear();
        writerCache.clear();
    }
}
