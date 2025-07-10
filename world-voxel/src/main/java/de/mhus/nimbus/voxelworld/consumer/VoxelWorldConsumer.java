package de.mhus.nimbus.voxelworld.consumer;

import de.mhus.nimbus.common.client.WorldVoxelClient;
import de.mhus.nimbus.shared.avro.VoxelOperationMessage;
import de.mhus.nimbus.shared.avro.VoxelData;
import de.mhus.nimbus.shared.avro.ChunkData;
import de.mhus.nimbus.shared.avro.BatchData;
import de.mhus.nimbus.shared.voxel.Voxel;
import de.mhus.nimbus.shared.voxel.VoxelChunk;
import de.mhus.nimbus.shared.voxel.VoxelInstance;
import de.mhus.nimbus.voxelworld.service.VoxelWorldService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class VoxelWorldConsumer {

    private final VoxelWorldService voxelWorldService;

    @Autowired
    public VoxelWorldConsumer(VoxelWorldService voxelWorldService) {
        this.voxelWorldService = voxelWorldService;
    }

    /**
     * Consumes voxel operation messages from Kafka
     */
    @KafkaListener(topics = WorldVoxelClient.VOXEL_OPERATIONS_TOPIC, groupId = "voxelworld-service")
    public void consumeVoxelOperation(VoxelOperationMessage message, org.springframework.kafka.support.Acknowledgment acknowledgment) {
        try {
            log.debug("Kafka: Received voxel operation {} for world {} with messageId {}",
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
                    log.warn("Unknown operation type: {}", message.getOperation());
            }

            log.debug("Kafka: Successfully processed operation {} for messageId {}",
                        message.getOperation(), message.getMessageId());

        } catch (Exception e) {
            log.error("Kafka: Failed to process voxel operation message with ID {}: {}",
                        message.getMessageId(), e.getMessage(), e);
        } finally {
            // Acknowledge the message after processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.debug("Kafka: Acknowledged message with ID {}", message.getMessageId());
            } else {
                log.warn("Kafka: Acknowledgment is null for message with ID {}", message.getMessageId());
            }
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

                log.debug("Kafka: Saved voxel at ({}, {}, {}) in world {}",
                           voxelData.getX(), voxelData.getY(), voxelData.getZ(), message.getWorldId());
            }
        } catch (Exception e) {
            log.error("Kafka: Failed to save voxel: {}", e.getMessage(), e);
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
                    log.debug("Kafka: Deleted voxel at ({}, {}, {}) in world {}",
                               voxelData.getX(), voxelData.getY(), voxelData.getZ(), message.getWorldId());
                } else {
                    log.debug("Kafka: No voxel found to delete at ({}, {}, {}) in world {}",
                               voxelData.getX(), voxelData.getY(), voxelData.getZ(), message.getWorldId());
                }
            }
        } catch (Exception e) {
            log.error("Kafka: Failed to delete voxel: {}", e.getMessage(), e);
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

                // Parse VoxelInstance-Liste aus BatchData und konvertiere zu Voxel-Liste
                List<VoxelInstance> voxelInstances = parseVoxelsFromBatchData(batchData);

                // Konvertiere VoxelInstances zu Voxels für den Service
                List<Voxel> voxels = voxelInstances.stream()
                    .map(vi -> vi.getVoxelType() != null ? vi.getVoxelType() : createDefaultVoxel())
                    .toList();

                int savedCount = voxelWorldService.saveVoxels(message.getWorldId(), voxels);

                log.info("Kafka: Batch saved {} voxels in world {}", savedCount, message.getWorldId());
            }
        } catch (Exception e) {
            log.error("Kafka: Failed to batch save voxels: {}", e.getMessage(), e);
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

                log.info("Kafka: Cleared chunk ({}, {}, {}) in world {} - {} voxels deleted",
                           chunkData.getChunkX(), chunkData.getChunkY(), chunkData.getChunkZ(),
                           message.getWorldId(), deletedCount);
            }
        } catch (Exception e) {
            log.error("Kafka: Failed to clear chunk: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to clear chunk", e);
        }
    }

    /**
     * Erstellt einen Standard-Voxel für Fallback-Zwecke
     */
    private Voxel createDefaultVoxel() {
        return Voxel.builder()
                .displayName("Default Voxel")
                .hardness(3)
                .build();
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
                // Erstelle ein Standard-Voxel
                return createDefaultVoxel();
            }
        } catch (Exception e) {
            log.warn("Failed to parse voxel data, creating default voxel: {}", e.getMessage());
            return createDefaultVoxel();
        }
    }

    /**
     * Hilfsmethode um VoxelInstance-Liste aus BatchData zu parsen
     */
    private List<VoxelInstance> parseVoxelsFromBatchData(BatchData batchData) {
        try {
            // BatchData enthält jetzt direkt eine Liste von AvroVoxel-Objekten
            return de.mhus.nimbus.shared.util.VoxelConverter.fromAvroVoxelList(batchData.getVoxels());
        } catch (Exception e) {
            log.error("Failed to parse batch voxel data: {}", e.getMessage(), e);
            return List.of(); // Leere Liste als Fallback
        }
    }

    /**
     * Einfache JSON-Parsing-Methode für Voxel
     */
    private Voxel parseVoxelFromJson(String jsonData, int x, int y, int z) {
        try {
            // Einfache Implementierung - erstelle Standard-Voxel
            return createDefaultVoxel();
        } catch (Exception e) {
            log.warn("Failed to parse voxel JSON, using default voxel: {}", e.getMessage());
            return createDefaultVoxel();
        }
    }

    /**
     * Einfache JSON-Parsing-Methode für Voxel-Liste
     */
    private List<Voxel> parseVoxelListFromJson(String jsonData) {
        try {
            // Einfache Implementierung - kann erweitert werden für komplexere Parsing-Logik
            // Hier würde normalerweise ein JSON-Parser verwendet werden
            log.debug("Parsing voxel list from JSON: {}", jsonData);
            return List.of(); // Platzhalter - sollte durch echte JSON-Parsing-Logik ersetzt werden
        } catch (Exception e) {
            log.error("Failed to parse voxel list JSON: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
