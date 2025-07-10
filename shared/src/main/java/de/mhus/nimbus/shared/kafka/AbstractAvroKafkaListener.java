package de.mhus.nimbus.shared.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Abstrakte Basis-Klasse für Kafka-Listener mit automatischer Avro-Serialisierung/Deserialisierung
 */
@Slf4j
public abstract class AbstractAvroKafkaListener<REQUEST extends SpecificRecord, RESPONSE extends SpecificRecord> {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final DatumReader<REQUEST> requestReader;
    private final DatumWriter<RESPONSE> responseWriter;

    protected AbstractAvroKafkaListener(KafkaTemplate<String, byte[]> kafkaTemplate,
                                       Class<REQUEST> requestClass,
                                       Class<RESPONSE> responseClass) {
        this.kafkaTemplate = kafkaTemplate;
        this.requestReader = new SpecificDatumReader<>(requestClass);
        this.responseWriter = new SpecificDatumWriter<>(responseClass);
    }

    /**
     * Allgemeine Kafka-Listener-Methode mit automatischer Avro-Deserialisierung
     */
    protected void handleAvroMessage(@Payload byte[] message,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset,
                                   Acknowledgment acknowledgment) {

        log.debug("Received message from topic: '{}', partition: {}, offset: {}",
                    topic, partition, offset);

        try {
            // Deserialisiere die Avro-Nachricht
            REQUEST request = deserialize(message);

            log.debug("Deserialized request: {}", request.getClass().getSimpleName());

            // Verarbeite die Nachricht
            RESPONSE response = processMessage(request, topic, partition, offset);

            // Sende Antwort zurück (falls eine Response erstellt wurde)
            if (response != null) {
                sendResponse(response, getResponseKey(request), getResponseTopic(topic));
            }

            // Manual acknowledgment nach erfolgreicher Verarbeitung
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("Error processing message from topic: {}", topic, e);
            handleError(message, topic, partition, offset, e);
        }
    }

    /**
     * Deserialisiert eine Avro-Nachricht aus byte[]
     */
    protected REQUEST deserialize(byte[] data) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        return requestReader.read(null, DecoderFactory.get().binaryDecoder(inputStream, null));
    }

    /**
     * Serialisiert eine Avro-Nachricht zu byte[]
     */
    protected byte[] serialize(RESPONSE response) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        responseWriter.write(response, EncoderFactory.get().binaryEncoder(outputStream, null));
        return outputStream.toByteArray();
    }

    /**
     * Sendet eine Antwort-Nachricht an ein Kafka-Topic
     */
    protected void sendResponse(RESPONSE response, String key, String responseTopic) {
        try {
            byte[] serializedResponse = serialize(response);
            kafkaTemplate.send(responseTopic, key, serializedResponse);
            log.debug("Sent response to topic: {} with key: {}", responseTopic, key);
        } catch (Exception e) {
            log.error("Error sending response to topic: {}", responseTopic, e);
        }
    }

    /**
     * Abstrakte Methode zur Verarbeitung der deserialisierten Nachricht
     * Muss von abgeleiteten Klassen implementiert werden
     */
    protected abstract RESPONSE processMessage(REQUEST request, String topic, int partition, long offset);

    /**
     * Bestimmt den Response-Key für die Antwort-Nachricht
     * Standard-Implementation gibt null zurück (kann überschrieben werden)
     */
    protected String getResponseKey(REQUEST request) {
        return null;
    }

    /**
     * Bestimmt das Response-Topic für die Antwort-Nachricht
     * Standard-Implementation hängt "-response" an das ursprüngliche Topic an
     */
    protected String getResponseTopic(String originalTopic) {
        return originalTopic + "-response";
    }

    /**
     * Fehlerbehandlung für nicht verarbeitbare Nachrichten
     * Standard-Implementation loggt nur den Fehler (kann überschrieben werden)
     */
    protected void handleError(byte[] message, String topic, int partition, long offset, Exception error) {
        log.error("Failed to process message from topic: {}, partition: {}, offset: {}",
                    topic, partition, offset, error);
        // TODO: Implementierung einer Dead Letter Queue
    }
}
