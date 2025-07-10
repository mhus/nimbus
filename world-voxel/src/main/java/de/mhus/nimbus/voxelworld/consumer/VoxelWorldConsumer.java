package de.mhus.nimbus.voxelworld.consumer;

import de.mhus.nimbus.common.client.WorldVoxelClient;
import de.mhus.nimbus.shared.avro.VoxelOperationMessage;
import de.mhus.nimbus.shared.avro.VoxelData;
import de.mhus.nimbus.shared.avro.ChunkData;
import de.mhus.nimbus.shared.avro.BatchData;
import de.mhus.nimbus.shared.voxel.Voxel;
import de.mhus.nimbus.shared.voxel.VoxelChunk;
import de.mhus.nimbus.voxelworld.service.VoxelWorldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Kafka consumer for voxel world operations
 * Verwendet standardisierte Avro-Objekte aus dem shared-Modul
 */
@Component
public class VoxelWorldConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoxelWorldConsumer.class);

    private final VoxelWorldService voxelWorldService;

    @Autowired
    public VoxelWorldConsumer(VoxelWorldService voxelWorldService) {
        this.voxelWorldService = voxelWorldService;
    }

    /**
     * Consumes voxel operation messages from Kafka
     */
    @KafkaListener(topics = WorldVoxelClient.VOXEL_OPERATIONS_TOPIC, groupId = "voxelworld-service")
    public void consumeVoxelOperation(VoxelOperationMessage message) {
        try {
            LOGGER.debug("Kafka: Received voxel operation {} for world {} with messageId {}",
                        message.getOperation(), message.getWorldId(), message.getMessageId());

            switch (message.getOperation()) {
                case SAVE:
                    handleSaveVoxel(message);
                    break;
                case DELETE:
                    handleDeleteVoxel(message);
                    break;
                case BATCH_SAVE:
                    handleBatchSaveVoxels(message);
                    break;
                case CLEAR_CHUNK:
                    handleClearChunk(message);
                    break;
                default:
                    LOGGER.warn("Unknown operation type: {}", message.getOperation());
            }

            LOGGER.debug("Kafka: Successfully processed operation {} for messageId {}",
                        message.getOperation(), message.getMessageId());

        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to process voxel operation message with ID {}: {}",
                        message.getMessageId(), e.getMessage(), e);
        }
    }

    /**
     * Handles save voxel operation
     */
    private void handleSaveVoxel(VoxelOperationMessage message) {
        try {
            if (message.getVoxelData() != null) {
                VoxelData voxelData = message.getVoxelData();

                // Erstelle Voxel-Objekt aus Avro-Daten
                Voxel voxel = createVoxelFromData(voxelData);
                voxelWorldService.saveVoxel(message.getWorldId(), voxel);

                LOGGER.debug("Kafka: Saved voxel at ({}, {}, {}) in world {}",
                           voxelData.getX(), voxelData.getY(), voxelData.getZ(), message.getWorldId());
            }
        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to save voxel: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save voxel", e);
        }
    }

    /**
     * Handles delete voxel operation
     */
    private void handleDeleteVoxel(VoxelOperationMessage message) {
        try {
            if (message.getVoxelData() != null) {
                VoxelData voxelData = message.getVoxelData();
                boolean deleted = voxelWorldService.deleteVoxel(
                    message.getWorldId(),
                    voxelData.getX(),
                    voxelData.getY(),
                    voxelData.getZ()
                );

                if (deleted) {
                    LOGGER.debug("Kafka: Deleted voxel at ({}, {}, {}) in world {}",
                               voxelData.getX(), voxelData.getY(), voxelData.getZ(), message.getWorldId());
                } else {
                    LOGGER.debug("Kafka: No voxel found to delete at ({}, {}, {}) in world {}",
                               voxelData.getX(), voxelData.getY(), voxelData.getZ(), message.getWorldId());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to delete voxel: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete voxel", e);
        }
    }

    /**
     * Handles batch save voxels operation
     */
    private void handleBatchSaveVoxels(VoxelOperationMessage message) {
        try {
            if (message.getBatchData() != null) {
                BatchData batchData = message.getBatchData();

                // Parse Voxel-Array aus JSON-String in BatchData
                List<Voxel> voxels = parseVoxelsFromBatchData(batchData);
                int savedCount = voxelWorldService.saveVoxels(message.getWorldId(), voxels);

                LOGGER.info("Kafka: Batch saved {} voxels in world {}", savedCount, message.getWorldId());
            }
        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to batch save voxels: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to batch save voxels", e);
        }
    }

    /**
     * Handles clear chunk operation
     */
    private void handleClearChunk(VoxelOperationMessage message) {
        try {
            if (message.getChunkData() != null) {
                ChunkData chunkData = message.getChunkData();
                long deletedCount = voxelWorldService.clearChunk(
                    message.getWorldId(),
                    chunkData.getChunkX(),
                    chunkData.getChunkY(),
                    chunkData.getChunkZ()
                );

                LOGGER.info("Kafka: Cleared chunk ({}, {}, {}) in world {} - {} voxels deleted",
                           chunkData.getChunkX(), chunkData.getChunkY(), chunkData.getChunkZ(),
                           message.getWorldId(), deletedCount);
            }
        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to clear chunk: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to clear chunk", e);
        }
    }

    /**
     * Hilfsmethode um Voxel-Objekt aus Avro VoxelData zu erstellen
     */
    private Voxel createVoxelFromData(VoxelData voxelData) {
        // Erstelle Voxel-Objekt basierend auf den Avro-Daten
        try {
            if (voxelData.getData() != null) {
                // Parse JSON oder verwende die Daten direkt
                return parseVoxelFromJson(voxelData.getData(), voxelData.getX(), voxelData.getY(), voxelData.getZ());
            } else {
                // Erstelle ein einfaches Voxel ohne zusätzliche Daten
                return new Voxel(voxelData.getX(), voxelData.getY(), voxelData.getZ(), null);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse voxel data, creating simple voxel: {}", e.getMessage());
            return new Voxel(voxelData.getX(), voxelData.getY(), voxelData.getZ(), null);
        }
    }

    /**
     * Hilfsmethode um Voxel-Liste aus BatchData zu parsen
     */
    private List<Voxel> parseVoxelsFromBatchData(BatchData batchData) {
        try {
            // Das 'data' Feld in BatchData enthält die JSON-Repräsentation der Voxel-Liste
            return parseVoxelListFromJson(batchData.getData());
        } catch (Exception e) {
            LOGGER.error("Failed to parse batch voxel data: {}", e.getMessage(), e);
            return List.of(); // Leere Liste als Fallback
        }
    }

    /**
     * Einfache JSON-Parsing-Methode für Voxel
     */
    private Voxel parseVoxelFromJson(String jsonData, int x, int y, int z) {
        try {
            // Einfache Implementierung - erstelle Voxel mit null VoxelType als Platzhalter
            return new Voxel(x, y, z, null);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse voxel JSON, using coordinates only: {}", e.getMessage());
            return new Voxel(x, y, z, null);
        }
    }

    /**
     * Einfache JSON-Parsing-Methode für Voxel-Liste
     */
    private List<Voxel> parseVoxelListFromJson(String jsonData) {
        try {
            // Einfache Implementierung - kann erweitert werden für komplexere Parsing-Logik
            // Hier würde normalerweise ein JSON-Parser verwendet werden
            LOGGER.debug("Parsing voxel list from JSON: {}", jsonData);
            return List.of(); // Platzhalter - sollte durch echte JSON-Parsing-Logik ersetzt werden
        } catch (Exception e) {
            LOGGER.error("Failed to parse voxel list JSON: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
