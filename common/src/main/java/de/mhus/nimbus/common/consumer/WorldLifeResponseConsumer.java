package de.mhus.nimbus.common.consumer;

import de.mhus.nimbus.common.client.WorldLifeClient;
import de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer für WorldLife-Responses
 * Verarbeitet Character-Lookup-Responses vom world-life Modul und leitet sie an den WorldLifeClient weiter
 */
@Component
@ConditionalOnProperty(name = "nimbus.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class WorldLifeResponseConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldLifeResponseConsumer.class);

    private final WorldLifeClient worldLifeClient;

    @Autowired
    public WorldLifeResponseConsumer(WorldLifeClient worldLifeClient) {
        this.worldLifeClient = worldLifeClient;
    }

    /**
     * Verarbeitet Character-Lookup-Responses vom WorldLife-Modul
     */
    @KafkaListener(
        topics = "character-lookup-response",
        groupId = "worldlife-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCharacterLookupResponse(@Payload PlayerCharacterLookupResponse response,
                                             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                             @Header(KafkaHeaders.OFFSET) long offset,
                                             Acknowledgment acknowledgment) {

        LOGGER.debug("Received character lookup response: requestId={}, status={}, characters count={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(),
                    response.getCharacters() != null ? response.getCharacters().size() : 0, topic, partition, offset);

        try {
            // Delegiere an WorldLifeClient
            boolean handled = worldLifeClient.handleCharacterLookupResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                LOGGER.debug("Successfully processed character lookup response: requestId={}", response.getRequestId());
            } else {
                LOGGER.warn("Could not process character lookup response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            LOGGER.error("Error handling character lookup response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet Character-Operation-Bestätigungen vom WorldLife-Modul
     */
    @KafkaListener(
        topics = "character-operation-confirmation",
        groupId = "worldlife-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCharacterOperationConfirmation(@Payload String messageId,
                                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                                     @Header(KafkaHeaders.OFFSET) long offset,
                                                     Acknowledgment acknowledgment) {

        LOGGER.debug("Received character operation confirmation: messageId={}, topic={}, partition={}, offset={}",
                    messageId, topic, partition, offset);

        try {
            // Delegiere an WorldLifeClient
            boolean handled = worldLifeClient.handleCharacterOperationConfirmation(messageId);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                LOGGER.debug("Successfully processed character operation confirmation: messageId={}", messageId);
            } else {
                LOGGER.warn("Could not process character operation confirmation - message will not be acknowledged: messageId={}", messageId);
            }

        } catch (Exception e) {
            LOGGER.error("Error handling character operation confirmation: messageId={}", messageId, e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet Character-Operation-Fehler vom WorldLife-Modul
     */
    @KafkaListener(
        topics = "character-operation-error",
        groupId = "worldlife-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCharacterOperationError(@Payload String errorMessage,
                                             @Header("messageId") String messageId,
                                             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                             @Header(KafkaHeaders.OFFSET) long offset,
                                             Acknowledgment acknowledgment) {

        LOGGER.debug("Received character operation error: messageId={}, error={}, topic={}, partition={}, offset={}",
                    messageId, errorMessage, topic, partition, offset);

        try {
            // Delegiere an WorldLifeClient
            boolean handled = worldLifeClient.handleCharacterOperationError(messageId, errorMessage);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                LOGGER.debug("Successfully processed character operation error: messageId={}", messageId);
            } else {
                LOGGER.warn("Could not process character operation error - message will not be acknowledged: messageId={}", messageId);
            }

        } catch (Exception e) {
            LOGGER.error("Error handling character operation error: messageId={}", messageId, e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }
}
