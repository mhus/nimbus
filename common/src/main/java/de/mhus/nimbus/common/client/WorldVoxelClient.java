package de.mhus.nimbus.common.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.VoxelOperationMessage;
import de.mhus.nimbus.shared.voxel.Voxel;
import de.mhus.nimbus.shared.voxel.VoxelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with world-voxel module via Kafka
 */
@Component
public class WorldVoxelClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldVoxelClient.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Kafka Topics
    private static final String VOXEL_OPERATIONS_TOPIC = "voxel-operations";
    private static final String CHUNK_OPERATIONS_TOPIC = "chunk-operations";
    private static final String CHUNK_LOAD_REQUESTS_TOPIC = "chunk-load-requests";
    private static final String CHUNK_FULL_LOAD_REQUESTS_TOPIC = "chunk-full-load-requests";

    @Autowired
    public WorldVoxelClient(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Speichert einen einzelnen Voxel
     *
     * @param worldId Die Welt-ID
     * @param voxel   Der zu speichernde Voxel
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> saveVoxel(String worldId, Voxel voxel) {
        try {
            String messageId = UUID.randomUUID().toString();
            String voxelJson = objectMapper.writeValueAsString(voxel);

            VoxelOperationMessage message = new VoxelOperationMessage();
            message.setMessageId(messageId);
            message.setOperation(VoxelOperationMessage.OperationType.SAVE);
            message.setWorldId(worldId);
            message.setVoxelData(new VoxelOperationMessage.VoxelData(
                    voxel.getX(), voxel.getY(), voxel.getZ(), voxelJson));

            return sendMessage(VOXEL_OPERATIONS_TOPIC, messageId, message);

        } catch (Exception e) {
            LOGGER.error("Failed to serialize voxel for save operation", e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Löscht einen Voxel
     *
     * @param worldId Die Welt-ID
     * @param x       X-Koordinate
     * @param y       Y-Koordinate
     * @param z       Z-Koordinate
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> deleteVoxel(String worldId, int x, int y, int z) {
        String messageId = UUID.randomUUID().toString();

        VoxelOperationMessage message = new VoxelOperationMessage();
        message.setMessageId(messageId);
        message.setOperation(VoxelOperationMessage.OperationType.DELETE);
        message.setWorldId(worldId);
        message.setVoxelData(new VoxelOperationMessage.VoxelData(x, y, z, null));

        return sendMessage(VOXEL_OPERATIONS_TOPIC, messageId, message);
    }

    /**
     * Speichert mehrere Voxel in einem Batch
     *
     * @param worldId Die Welt-ID
     * @param voxels  Liste der zu speichernden Voxel
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> batchSaveVoxels(String worldId, List<Voxel> voxels) {
        try {
            String messageId = UUID.randomUUID().toString();
            String voxelsJson = objectMapper.writeValueAsString(voxels);

            VoxelOperationMessage message = new VoxelOperationMessage();
            message.setMessageId(messageId);
            message.setOperation(VoxelOperationMessage.OperationType.BATCH_SAVE);
            message.setWorldId(worldId);
            message.setBatchData(new VoxelOperationMessage.BatchData(voxelsJson));

            return sendMessage(VOXEL_OPERATIONS_TOPIC, messageId, message);

        } catch (Exception e) {
            LOGGER.error("Failed to serialize voxels for batch save operation", e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Löscht alle Voxel in einem Chunk
     *
     * @param worldId Die Welt-ID
     * @param chunkX  Chunk X-Koordinate
     * @param chunkY  Chunk Y-Koordinate
     * @param chunkZ  Chunk Z-Koordinate
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> clearChunk(String worldId, int chunkX, int chunkY, int chunkZ) {
        String messageId = UUID.randomUUID().toString();

        VoxelOperationMessage message = new VoxelOperationMessage();
        message.setMessageId(messageId);
        message.setOperation(VoxelOperationMessage.OperationType.CLEAR_CHUNK);
        message.setWorldId(worldId);
        message.setChunkData(new VoxelOperationMessage.ChunkData(chunkX, chunkY, chunkZ, null, false));

        return sendMessage(VOXEL_OPERATIONS_TOPIC, messageId, message);
    }

    /**
     * Speichert einen kompletten Chunk
     *
     * @param worldId Die Welt-ID
     * @param chunk   Der zu speichernde Chunk
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> saveChunk(String worldId, VoxelChunk chunk) {
        try {
            String messageId = UUID.randomUUID().toString();
            String chunkJson = objectMapper.writeValueAsString(chunk);

            VoxelOperationMessage message = new VoxelOperationMessage();
            message.setMessageId(messageId);
            message.setOperation(VoxelOperationMessage.OperationType.SAVE);
            message.setWorldId(worldId);
            message.setChunkData(new VoxelOperationMessage.ChunkData(
                    chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(), chunkJson, false));

            return sendMessage(CHUNK_OPERATIONS_TOPIC, messageId, message);

        } catch (Exception e) {
            LOGGER.error("Failed to serialize chunk for save operation", e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Lädt einen Chunk
     *
     * @param worldId Die Welt-ID
     * @param chunkX  Chunk X-Koordinate
     * @param chunkY  Chunk Y-Koordinate
     * @param chunkZ  Chunk Z-Koordinate
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> loadChunk(String worldId, int chunkX, int chunkY, int chunkZ) {
        String messageId = UUID.randomUUID().toString();

        VoxelOperationMessage message = new VoxelOperationMessage();
        message.setMessageId(messageId);
        message.setOperation(VoxelOperationMessage.OperationType.SAVE);
        message.setWorldId(worldId);
        message.setChunkData(new VoxelOperationMessage.ChunkData(chunkX, chunkY, chunkZ, null, false));

        return sendMessage(CHUNK_LOAD_REQUESTS_TOPIC, messageId, message);
    }

    /**
     * Lädt einen kompletten Chunk mit Option für leere Voxel
     *
     * @param worldId      Die Welt-ID
     * @param chunkX       Chunk X-Koordinate
     * @param chunkY       Chunk Y-Koordinate
     * @param chunkZ       Chunk Z-Koordinate
     * @param includeEmpty Ob leere Voxel eingeschlossen werden sollen
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> loadFullChunk(String worldId, int chunkX, int chunkY, int chunkZ, boolean includeEmpty) {
        try {
            String messageId = UUID.randomUUID().toString();

            // Create a simple JSON object for the includeEmpty flag
            String configJson = String.format("{\"includeEmpty\":%s}", includeEmpty);

            VoxelOperationMessage message = new VoxelOperationMessage();
            message.setMessageId(messageId);
            message.setOperation(VoxelOperationMessage.OperationType.SAVE);
            message.setWorldId(worldId);
            message.setChunkData(new VoxelOperationMessage.ChunkData(chunkX, chunkY, chunkZ, configJson, includeEmpty));

            return sendMessage(CHUNK_FULL_LOAD_REQUESTS_TOPIC, messageId, message);

        } catch (Exception e) {
            LOGGER.error("Failed to create full chunk load request", e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Sendet eine Nachricht an ein Kafka-Topic
     *
     * @param topic     Das Kafka-Topic
     * @param messageId Die Nachrichten-ID (wird als Key verwendet)
     * @param message   Die zu sendende Nachricht
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    private CompletableFuture<Void> sendMessage(String topic, String messageId, VoxelOperationMessage message) {
        try {
            return kafkaTemplate.send(topic, messageId, message)
                .thenRun(() -> LOGGER.debug("Successfully sent message with ID {} to topic {}", messageId, topic))
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error("Failed to send message with ID {} to topic {}: {}",
                                   messageId, topic, throwable.getMessage(), throwable);
                        throw new RuntimeException(throwable);
                    }
                    return null;
                });

        } catch (Exception e) {
            LOGGER.error("Failed to send message with ID {} to topic {}: {}",
                       messageId, topic, e.getMessage(), e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
