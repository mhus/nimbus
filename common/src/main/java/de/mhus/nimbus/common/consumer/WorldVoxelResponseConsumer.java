package de.mhus.nimbus.common.consumer;

import de.mhus.nimbus.common.client.WorldVoxelClient;
import de.mhus.nimbus.shared.avro.VoxelOperationMessage;
import de.mhus.nimbus.shared.voxel.VoxelChunk;
import de.mhus.nimbus.shared.voxel.Voxel;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;

/**
 * Kafka Consumer für WorldVoxel-Responses
 * Verarbeitet Voxel- und Chunk-Responses vom world-voxel Modul und leitet sie an den WorldVoxelClient weiter
 */
@Component
@ConditionalOnProperty(name = "nimbus.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class WorldVoxelResponseConsumer {

    private final WorldVoxelClient worldVoxelClient;

    @Autowired
    public WorldVoxelResponseConsumer(WorldVoxelClient worldVoxelClient) {
        this.worldVoxelClient = worldVoxelClient;
    }

    /**
     * Verarbeitet Voxel-Operation-Responses vom WorldVoxel-Modul
     */
    @KafkaListener(
        topics = "voxel-operation-response",
        groupId = "worldvoxel-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleVoxelOperationResponse(@Payload VoxelOperationMessage response,
                                            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                            @Header(KafkaHeaders.OFFSET) long offset,
                                            Acknowledgment acknowledgment) {

        log.debug("Received voxel operation response: messageId={}, operation={}, worldId={}, topic={}, partition={}, offset={}",
                    response.getMessageId(), response.getOperation(), response.getWorldId(), topic, partition, offset);

        try {
            // Delegiere an WorldVoxelClient
            boolean handled = worldVoxelClient.handleVoxelOperationResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed voxel operation response: messageId={}", response.getMessageId());
            } else {
                log.warn("Could not process voxel operation response - message will not be acknowledged: messageId={}", response.getMessageId());
            }

        } catch (Exception e) {
            log.error("Error handling voxel operation response: messageId={}", response.getMessageId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet Chunk-Load-Responses vom WorldVoxel-Modul
     */
    @KafkaListener(
        topics = "chunk-load-response",
        groupId = "worldvoxel-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleChunkLoadResponse(@Payload VoxelChunk response,
                                       @Header("messageId") String messageId,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset,
                                       Acknowledgment acknowledgment) {

        log.debug("Received chunk load response: messageId={}, chunk=({},{},{}), topic={}, partition={}, offset={}",
                    messageId, response.getChunkX(), response.getChunkY(), response.getChunkZ(),
                    topic, partition, offset);

        try {
            // Delegiere an WorldVoxelClient
            boolean handled = worldVoxelClient.handleChunkLoadResponse(response, messageId);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed chunk load response: messageId={}", messageId);
            } else {
                log.warn("Could not process chunk load response - message will not be acknowledged: messageId={}", messageId);
            }

        } catch (Exception e) {
            log.error("Error handling chunk load response: messageId={}", messageId, e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet Voxel-Load-Responses vom WorldVoxel-Modul
     */
    @KafkaListener(
        topics = "voxel-load-response",
        groupId = "worldvoxel-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleVoxelLoadResponse(@Payload List<Voxel> response,
                                       @Header("messageId") String messageId,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset,
                                       Acknowledgment acknowledgment) {

        log.debug("Received voxel load response: messageId={}, voxels count={}, topic={}, partition={}, offset={}",
                    messageId, response != null ? response.size() : 0, topic, partition, offset);

        try {
            // Delegiere an WorldVoxelClient
            boolean handled = worldVoxelClient.handleVoxelLoadResponse(response, messageId);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed voxel load response: messageId={}", messageId);
            } else {
                log.warn("Could not process voxel load response - message will not be acknowledged: messageId={}", messageId);
            }

        } catch (Exception e) {
            log.error("Error handling voxel load response: messageId={}", messageId, e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet Voxel-Operation-Bestätigungen vom WorldVoxel-Modul
     */
    @KafkaListener(
        topics = "voxel-operation-confirmation",
        groupId = "worldvoxel-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleVoxelOperationConfirmation(@Payload String messageId,
                                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                                 @Header(KafkaHeaders.OFFSET) long offset,
                                                 Acknowledgment acknowledgment) {

        log.debug("Received voxel operation confirmation: messageId={}, topic={}, partition={}, offset={}",
                    messageId, topic, partition, offset);

        try {
            // Delegiere an WorldVoxelClient
            boolean handled = worldVoxelClient.handleVoxelOperationConfirmation(messageId);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed voxel operation confirmation: messageId={}", messageId);
            } else {
                log.warn("Could not process voxel operation confirmation - message will not be acknowledged: messageId={}", messageId);
            }

        } catch (Exception e) {
            log.error("Error handling voxel operation confirmation: messageId={}", messageId, e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet Voxel-Operation-Fehler vom WorldVoxel-Modul
     */
    @KafkaListener(
        topics = "voxel-operation-error",
        groupId = "worldvoxel-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleVoxelOperationError(@Payload String errorMessage,
                                         @Header("messageId") String messageId,
                                         @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                         @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                         @Header(KafkaHeaders.OFFSET) long offset,
                                         Acknowledgment acknowledgment) {

        log.debug("Received voxel operation error: messageId={}, error={}, topic={}, partition={}, offset={}",
                    messageId, errorMessage, topic, partition, offset);

        try {
            // Delegiere an WorldVoxelClient
            boolean handled = worldVoxelClient.handleVoxelOperationError(messageId, errorMessage);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed voxel operation error: messageId={}", messageId);
            } else {
                log.warn("Could not process voxel operation error - message will not be acknowledged: messageId={}", messageId);
            }

        } catch (Exception e) {
            log.error("Error handling voxel operation error: messageId={}", messageId, e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet Chunk-Load-Fehler vom WorldVoxel-Modul
     */
    @KafkaListener(
        topics = "chunk-load-error",
        groupId = "worldvoxel-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleChunkLoadError(@Payload String errorMessage,
                                    @Header("messageId") String messageId,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                    @Header(KafkaHeaders.OFFSET) long offset,
                                    Acknowledgment acknowledgment) {

        log.debug("Received chunk load error: messageId={}, error={}, topic={}, partition={}, offset={}",
                    messageId, errorMessage, topic, partition, offset);

        try {
            // Delegiere an WorldVoxelClient
            boolean handled = worldVoxelClient.handleChunkLoadError(messageId, errorMessage);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed chunk load error: messageId={}", messageId);
            } else {
                log.warn("Could not process chunk load error - message will not be acknowledged: messageId={}", messageId);
            }

        } catch (Exception e) {
            log.error("Error handling chunk load error: messageId={}", messageId, e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet Voxel-Load-Fehler vom WorldVoxel-Modul
     */
    @KafkaListener(
        topics = "voxel-load-error",
        groupId = "worldvoxel-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleVoxelLoadError(@Payload String errorMessage,
                                    @Header("messageId") String messageId,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                    @Header(KafkaHeaders.OFFSET) long offset,
                                    Acknowledgment acknowledgment) {

        log.debug("Received voxel load error: messageId={}, error={}, topic={}, partition={}, offset={}",
                    messageId, errorMessage, topic, partition, offset);

        try {
            // Delegiere an WorldVoxelClient
            boolean handled = worldVoxelClient.handleVoxelLoadError(messageId, errorMessage);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed voxel load error: messageId={}", messageId);
            } else {
                log.warn("Could not process voxel load error - message will not be acknowledged: messageId={}", messageId);
            }

        } catch (Exception e) {
            log.error("Error handling voxel load error: messageId={}", messageId, e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }
}
