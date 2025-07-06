package de.mhus.nimbus.voxelworld.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.VoxelOperationMessage;
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
 */
@Component
public class VoxelWorldConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoxelWorldConsumer.class);

    private final VoxelWorldService voxelWorldService;
    private final ObjectMapper objectMapper;

    @Autowired
    public VoxelWorldConsumer(VoxelWorldService voxelWorldService) {
        this.voxelWorldService = voxelWorldService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Consumes voxel operation messages from Kafka
     */
    @KafkaListener(topics = "voxel-operations", groupId = "voxelworld-service")
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
            // Here you could implement retry logic or dead letter queue handling
        }
    }

    /**
     * Consumes chunk save messages from Kafka
     */
    @KafkaListener(topics = "chunk-operations", groupId = "voxelworld-service")
    public void consumeChunkOperation(VoxelOperationMessage message) {
        try {
            LOGGER.debug("Kafka: Received chunk operation for world {} with messageId {}",
                        message.getWorldId(), message.getMessageId());

            if (message.getChunkData() != null) {
                VoxelChunk chunk = objectMapper.readValue(message.getChunkData().getChunkJson(), VoxelChunk.class);
                int savedCount = voxelWorldService.saveChunk(message.getWorldId(), chunk);

                LOGGER.info("Kafka: Saved chunk ({}, {}, {}) in world {} - {} voxels saved",
                           chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(),
                           message.getWorldId(), savedCount);
            }

        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to process chunk operation message with ID {}: {}",
                        message.getMessageId(), e.getMessage(), e);
        }
    }

    /**
     * Consumes chunk load requests from Kafka
     */
    @KafkaListener(topics = "chunk-load-requests", groupId = "voxelworld-service")
    public void consumeChunkLoadRequest(VoxelOperationMessage message) {
        try {
            LOGGER.info("Kafka: Received chunk load request for world {} with messageId {}",
                       message.getWorldId(), message.getMessageId());

            if (message.getChunkData() != null) {
                VoxelOperationMessage.ChunkData chunkData = message.getChunkData();

                // Load full chunk from database
                VoxelChunk chunk = voxelWorldService.loadFullChunk(
                    message.getWorldId(),
                    chunkData.getChunkX(),
                    chunkData.getChunkY(),
                    chunkData.getChunkZ()
                );

                // Send loaded chunk back via Kafka (optional - for async responses)
                sendChunkLoadResponse(message.getMessageId(), message.getWorldId(), chunk);

                LOGGER.info("Kafka: Successfully loaded chunk ({}, {}, {}) in world {} with {} voxels",
                           chunkData.getChunkX(), chunkData.getChunkY(), chunkData.getChunkZ(),
                           message.getWorldId(), chunk.getVoxelCount());
            }

        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to process chunk load request with ID {}: {}",
                        message.getMessageId(), e.getMessage(), e);
        }
    }

    /**
     * Consumes full chunk load requests (with includeEmpty parameter) from Kafka
     */
    @KafkaListener(topics = "chunk-full-load-requests", groupId = "voxelworld-service")
    public void consumeFullChunkLoadRequest(VoxelOperationMessage message) {
        try {
            LOGGER.info("Kafka: Received full chunk load request for world {} with messageId {}",
                       message.getWorldId(), message.getMessageId());

            if (message.getChunkData() != null) {
                VoxelOperationMessage.ChunkData chunkData = message.getChunkData();

                // Parse includeEmpty flag from chunkJson if present
                boolean includeEmpty = false;
                if (chunkData.getChunkJson() != null) {
                    try {
                        // Simple parsing for includeEmpty flag
                        includeEmpty = chunkData.getChunkJson().contains("\"includeEmpty\":true");
                    } catch (Exception e) {
                        LOGGER.debug("Could not parse includeEmpty flag, defaulting to false");
                    }
                }

                // Load full chunk from database with includeEmpty parameter
                VoxelChunk chunk = voxelWorldService.loadFullChunk(
                    message.getWorldId(),
                    chunkData.getChunkX(),
                    chunkData.getChunkY(),
                    chunkData.getChunkZ(),
                    includeEmpty
                );

                // Send loaded chunk back via Kafka
                sendChunkLoadResponse(message.getMessageId(), message.getWorldId(), chunk);

                LOGGER.info("Kafka: Successfully loaded full chunk ({}, {}, {}) in world {} with {} voxels (includeEmpty: {})",
                           chunkData.getChunkX(), chunkData.getChunkY(), chunkData.getChunkZ(),
                           message.getWorldId(), chunk.getVoxelCount(), includeEmpty);
            }

        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to process full chunk load request with ID {}: {}",
                        message.getMessageId(), e.getMessage(), e);
        }
    }

    /**
     * Handles save voxel operation
     */
    private void handleSaveVoxel(VoxelOperationMessage message) {
        try {
            if (message.getVoxelData() != null) {
                Voxel voxel = objectMapper.readValue(message.getVoxelData().getVoxelJson(), Voxel.class);
                voxelWorldService.saveVoxel(message.getWorldId(), voxel);

                LOGGER.debug("Kafka: Saved voxel at ({}, {}, {}) in world {}",
                           voxel.getX(), voxel.getY(), voxel.getZ(), message.getWorldId());
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
                VoxelOperationMessage.VoxelData voxelData = message.getVoxelData();
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
                Voxel[] voxelArray = objectMapper.readValue(message.getBatchData().getVoxelsJson(), Voxel[].class);
                List<Voxel> voxels = Arrays.asList(voxelArray);

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
                VoxelOperationMessage.ChunkData chunkData = message.getChunkData();
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
     * Sends a chunk load response back to Kafka
     */
    private void sendChunkLoadResponse(String originalMessageId, String worldId, VoxelChunk chunk) {
        try {
            // Create response message
            VoxelOperationMessage response = new VoxelOperationMessage();
            response.setMessageId("response-" + originalMessageId);
            response.setOperation(VoxelOperationMessage.OperationType.BATCH_SAVE); // Reuse existing type
            response.setWorldId(worldId);

            // Serialize chunk to JSON
            String chunkJson = objectMapper.writeValueAsString(chunk);
            VoxelOperationMessage.ChunkData chunkData = new VoxelOperationMessage.ChunkData(
                chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(), chunkJson, false
            );
            response.setChunkData(chunkData);

            // Send to response topic (requires KafkaTemplate bean)
            // kafkaTemplate.send("chunk-load-responses", response);

            LOGGER.debug("Kafka: Sent chunk load response for messageId {}", originalMessageId);

        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to send chunk load response for messageId {}: {}",
                        originalMessageId, e.getMessage(), e);
        }
    }
}
