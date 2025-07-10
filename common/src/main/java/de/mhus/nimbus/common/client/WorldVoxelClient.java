package de.mhus.nimbus.common.client;

import de.mhus.nimbus.shared.avro.VoxelOperationMessage;
import de.mhus.nimbus.shared.avro.OperationType;
import de.mhus.nimbus.shared.avro.VoxelData;
import de.mhus.nimbus.shared.avro.ChunkData;
import de.mhus.nimbus.shared.voxel.Voxel;
import de.mhus.nimbus.shared.voxel.VoxelChunk;
import de.mhus.nimbus.shared.voxel.VoxelInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Client for communicating with world-voxel module via Kafka
 */
@Component
public class WorldVoxelClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldVoxelClient.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Maps für pending Requests
    private final ConcurrentHashMap<String, CompletableFuture<VoxelOperationMessage>> pendingVoxelOperations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<VoxelChunk>> pendingChunkLoads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<List<Voxel>>> pendingVoxelLoads = new ConcurrentHashMap<>();

    // Kafka Topics
    public static final String VOXEL_OPERATIONS_TOPIC = "voxel-operations";
    public static final String CHUNK_OPERATIONS_TOPIC = "chunk-operations";
    public static final String CHUNK_LOAD_REQUESTS_TOPIC = "chunk-load-requests";
    public static final String CHUNK_FULL_LOAD_REQUESTS_TOPIC = "chunk-full-load-requests";

    // Default Timeout für Responses in Sekunden
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    @Autowired
    public WorldVoxelClient(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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
            String voxelJson = voxel.toString(); // Directly use Voxel's toString method

            VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                    .setMessageId(messageId)
                    .setOperation(OperationType.SAVE)
                    .setWorldId(worldId)
                    .setVoxelData(VoxelData.newBuilder()
                            .setX(voxel.getX())
                            .setY(voxel.getY())
                            .setZ(voxel.getZ())
                            .setData(voxelJson)
                            .build())
                    .build();

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

        VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                .setMessageId(messageId)
                .setOperation(OperationType.DELETE)
                .setWorldId(worldId)
                .setVoxelData(VoxelData.newBuilder()
                        .setX(x)
                        .setY(y)
                        .setZ(z)
                        .build())
                .build();

        return sendMessage(VOXEL_OPERATIONS_TOPIC, messageId, message);
    }

    /**
     * Speichert mehrere Voxel in einem Batch
     *
     * @param worldId Die Welt-ID
     * @param voxelInstances  Liste der zu speichernden VoxelInstances
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> batchSaveVoxels(String worldId, List<VoxelInstance> voxelInstances) {
        try {
            String messageId = UUID.randomUUID().toString();

            VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                    .setMessageId(messageId)
                    .setOperation(OperationType.BATCH_SAVE)
                    .setWorldId(worldId)
                    .setBatchData(de.mhus.nimbus.shared.avro.BatchData.newBuilder()
                            .setVoxels(de.mhus.nimbus.shared.util.VoxelConverter.toAvroVoxelList(voxelInstances))
                            .build())
                    .build();

            return sendMessage(VOXEL_OPERATIONS_TOPIC, messageId, message);

        } catch (Exception e) {
            LOGGER.error("Failed to convert voxels for batch save operation", e);
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

        VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                .setMessageId(messageId)
                .setOperation(OperationType.CLEAR_CHUNK)
                .setWorldId(worldId)
                .setChunkData(ChunkData.newBuilder()
                        .setChunkX(chunkX)
                        .setChunkY(chunkY)
                        .setChunkZ(chunkZ)
                        .setIncludeEmpty(false)
                        .build())
                .build();

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
            String chunkJson = chunk.toString(); // Directly use VoxelChunk's toString method

            VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                    .setMessageId(messageId)
                    .setOperation(OperationType.SAVE)
                    .setWorldId(worldId)
                    .setChunkData(ChunkData.newBuilder()
                            .setChunkX(chunk.getChunkX())
                            .setChunkY(chunk.getChunkY())
                            .setChunkZ(chunk.getChunkZ())
                            .setData(chunkJson)
                            .setIncludeEmpty(false)
                            .build())
                    .build();

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

        VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                .setMessageId(messageId)
                .setOperation(OperationType.SAVE)
                .setWorldId(worldId)
                .setChunkData(ChunkData.newBuilder()
                        .setChunkX(chunkX)
                        .setChunkY(chunkY)
                        .setChunkZ(chunkZ)
                        .setIncludeEmpty(false)
                        .build())
                .build();

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

            VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                    .setMessageId(messageId)
                    .setOperation(OperationType.SAVE)
                    .setWorldId(worldId)
                    .setChunkData(ChunkData.newBuilder()
                            .setChunkX(chunkX)
                            .setChunkY(chunkY)
                            .setChunkZ(chunkZ)
                            .setData(configJson)
                            .setIncludeEmpty(includeEmpty)
                            .build())
                    .build();

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

    /**
     * Registriert einen Pending Request für Voxel-Operationen
     *
     * @param messageId Die Nachrichten-ID
     * @return CompletableFuture für die Antwortnachricht
     */
    public CompletableFuture<VoxelOperationMessage> registerPendingVoxelOperation(String messageId) {
        CompletableFuture<VoxelOperationMessage> future = new CompletableFuture<>();
        pendingVoxelOperations.put(messageId, future);

        // Timeout für die Antwort
        CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();
        timeoutFuture.orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                     .thenRun(() -> {
                         CompletableFuture<VoxelOperationMessage> pendingFuture = pendingVoxelOperations.remove(messageId);
                         if (pendingFuture != null) {
                             pendingFuture.completeExceptionally(new RuntimeException("Timeout waiting for voxel operation response"));
                         }
                     });

        return future;
    }

    /**
     * Registriert einen Pending Request für Chunk-Loads
     *
     * @param messageId Die Nachrichten-ID
     * @return CompletableFuture für die Antwortnachricht
     */
    public CompletableFuture<VoxelChunk> registerPendingChunkLoad(String messageId) {
        CompletableFuture<VoxelChunk> future = new CompletableFuture<>();
        pendingChunkLoads.put(messageId, future);

        // Timeout für die Antwort
        CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();
        timeoutFuture.orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                     .thenRun(() -> {
                         CompletableFuture<VoxelChunk> pendingFuture = pendingChunkLoads.remove(messageId);
                         if (pendingFuture != null) {
                             pendingFuture.completeExceptionally(new RuntimeException("Timeout waiting for chunk load response"));
                         }
                     });

        return future;
    }

    /**
     * Registriert einen Pending Request für Voxel-Loads
     *
     * @param messageId Die Nachrichten-ID
     * @return CompletableFuture für die Antwortnachricht
     */
    public CompletableFuture<List<Voxel>> registerPendingVoxelLoad(String messageId) {
        CompletableFuture<List<Voxel>> future = new CompletableFuture<>();
        pendingVoxelLoads.put(messageId, future);

        // Timeout für die Antwort
        CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();
        timeoutFuture.orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                     .thenRun(() -> {
                         CompletableFuture<List<Voxel>> pendingFuture = pendingVoxelLoads.remove(messageId);
                         if (pendingFuture != null) {
                             pendingFuture.completeExceptionally(new RuntimeException("Timeout waiting for voxel load response"));
                         }
                     });

        return future;
    }

    /**
     * Handhabt eingehende Nachrichten für Voxel-Operationen
     *
     * @param message Die empfangene Nachricht
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleVoxelOperationResponse(VoxelOperationMessage message) {
        CompletableFuture<VoxelOperationMessage> future = pendingVoxelOperations.remove(message.getMessageId());
        if (future != null) {
            future.complete(message);
            return true;
        } else {
            LOGGER.warn("Received response for unknown voxel operation message ID {}", message.getMessageId());
            return false;
        }
    }

    /**
     * Handhabt eingehende Nachrichten für Chunk-Loads
     *
     * @param message Die empfangene Nachricht
     * @param messageId Die Message-ID für den Request
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleChunkLoadResponse(VoxelChunk message, String messageId) {
        CompletableFuture<VoxelChunk> future = pendingChunkLoads.remove(messageId);
        if (future != null) {
            future.complete(message);
            return true;
        } else {
            LOGGER.warn("Received response for unknown chunk load message ID {}", messageId);
            return false;
        }
    }

    /**
     * Handhabt eingehende Nachrichten für Voxel-Loads
     *
     * @param message Die empfangene Nachricht
     * @param messageId Die Message-ID für den Request
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleVoxelLoadResponse(List<Voxel> message, String messageId) {
        if (message == null || message.isEmpty()) {
            LOGGER.warn("Received empty voxel load response for messageId: {}", messageId);
            return false;
        }

        CompletableFuture<List<Voxel>> future = pendingVoxelLoads.remove(messageId);
        if (future != null) {
            future.complete(message);
            return true;
        } else {
            LOGGER.warn("Received response for unknown voxel load message ID {}", messageId);
            return false;
        }
    }

    /**
     * Speichert einen einzelnen Voxel mit Response-Handling
     *
     * @param worldId Die Welt-ID
     * @param voxel   Der zu speichernde Voxel
     * @return CompletableFuture mit VoxelOperationMessage Response
     */
    public CompletableFuture<VoxelOperationMessage> saveVoxelWithResponse(String worldId, Voxel voxel) {
        try {
            String messageId = UUID.randomUUID().toString();
            String voxelJson = voxel.toString();

            VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                    .setMessageId(messageId)
                    .setOperation(OperationType.SAVE)
                    .setWorldId(worldId)
                    .setVoxelData(VoxelData.newBuilder()
                            .setX(voxel.getX())
                            .setY(voxel.getY())
                            .setZ(voxel.getZ())
                            .setData(voxelJson)
                            .build())
                    .build();

            CompletableFuture<VoxelOperationMessage> future = new CompletableFuture<>();
            pendingVoxelOperations.put(messageId, future);

            sendMessage(VOXEL_OPERATIONS_TOPIC, messageId, message)
                .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    }
                });

            LOGGER.info("Saving voxel at ({},{},{}) in world {} with messageId {} (with response)",
                       voxel.getX(), voxel.getY(), voxel.getZ(), worldId, messageId);

            return future;

        } catch (Exception e) {
            LOGGER.error("Failed to serialize voxel for save operation with response", e);
            CompletableFuture<VoxelOperationMessage> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Löscht einen Voxel mit Response-Handling
     *
     * @param worldId Die Welt-ID
     * @param x       X-Koordinate
     * @param y       Y-Koordinate
     * @param z       Z-Koordinate
     * @return CompletableFuture mit VoxelOperationMessage Response
     */
    public CompletableFuture<VoxelOperationMessage> deleteVoxelWithResponse(String worldId, int x, int y, int z) {
        String messageId = UUID.randomUUID().toString();

        VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                .setMessageId(messageId)
                .setOperation(OperationType.DELETE)
                .setWorldId(worldId)
                .setVoxelData(VoxelData.newBuilder()
                        .setX(x)
                        .setY(y)
                        .setZ(z)
                        .build())
                .build();

        CompletableFuture<VoxelOperationMessage> future = new CompletableFuture<>();
        pendingVoxelOperations.put(messageId, future);

        sendMessage(VOXEL_OPERATIONS_TOPIC, messageId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        LOGGER.info("Deleting voxel at ({},{},{}) in world {} with messageId {} (with response)",
                   x, y, z, worldId, messageId);

        return future;
    }

    /**
     * Batch-Speichert mehrere Voxel mit Response-Handling
     *
     * @param worldId Die Welt-ID
     * @param voxelInstances  Liste der zu speichernden VoxelInstances
     * @return CompletableFuture mit VoxelOperationMessage Response
     */
    public CompletableFuture<VoxelOperationMessage> batchSaveVoxelsWithResponse(String worldId, List<VoxelInstance> voxelInstances) {
        try {
            String messageId = UUID.randomUUID().toString();

            VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                    .setMessageId(messageId)
                    .setOperation(OperationType.BATCH_SAVE)
                    .setWorldId(worldId)
                    .setBatchData(de.mhus.nimbus.shared.avro.BatchData.newBuilder()
                            .setVoxels(de.mhus.nimbus.shared.util.VoxelConverter.toAvroVoxelList(voxelInstances))
                            .build())
                    .build();

            CompletableFuture<VoxelOperationMessage> future = new CompletableFuture<>();
            pendingVoxelOperations.put(messageId, future);

            sendMessage(VOXEL_OPERATIONS_TOPIC, messageId, message)
                .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    }
                });

            LOGGER.info("Batch saving {} voxels in world {} with messageId {} (with response)",
                       voxelInstances.size(), worldId, messageId);

            return future;

        } catch (Exception e) {
            LOGGER.error("Failed to convert voxels for batch save operation with response", e);
            CompletableFuture<VoxelOperationMessage> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Lädt einen Chunk mit Response-Handling
     *
     * @param worldId Die Welt-ID
     * @param chunkX  Chunk X-Koordinate
     * @param chunkY  Chunk Y-Koordinate
     * @param chunkZ  Chunk Z-Koordinate
     * @return CompletableFuture mit VoxelChunk Response
     */
    public CompletableFuture<VoxelChunk> loadChunkWithResponse(String worldId, int chunkX, int chunkY, int chunkZ) {
        String messageId = UUID.randomUUID().toString();

        VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                .setMessageId(messageId)
                .setOperation(OperationType.SAVE)
                .setWorldId(worldId)
                .setChunkData(ChunkData.newBuilder()
                        .setChunkX(chunkX)
                        .setChunkY(chunkY)
                        .setChunkZ(chunkZ)
                        .setIncludeEmpty(false)
                        .build())
                .build();

        CompletableFuture<VoxelChunk> future = new CompletableFuture<>();
        pendingChunkLoads.put(messageId, future);

        sendMessage(CHUNK_LOAD_REQUESTS_TOPIC, messageId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        LOGGER.info("Loading chunk ({},{},{}) in world {} with messageId {} (with response)",
                   chunkX, chunkY, chunkZ, worldId, messageId);

        return future;
    }

    /**
     * Lädt einen kompletten Chunk mit Response-Handling
     *
     * @param worldId      Die Welt-ID
     * @param chunkX       Chunk X-Koordinate
     * @param chunkY       Chunk Y-Koordinate
     * @param chunkZ       Chunk Z-Koordinate
     * @param includeEmpty Ob leere Voxel eingeschlossen werden sollen
     * @return CompletableFuture mit VoxelChunk Response
     */
    public CompletableFuture<VoxelChunk> loadFullChunkWithResponse(String worldId, int chunkX, int chunkY, int chunkZ, boolean includeEmpty) {
        try {
            String messageId = UUID.randomUUID().toString();
            String configJson = String.format("{\"includeEmpty\":%s}", includeEmpty);

            VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                    .setMessageId(messageId)
                    .setOperation(OperationType.SAVE)
                    .setWorldId(worldId)
                    .setChunkData(ChunkData.newBuilder()
                            .setChunkX(chunkX)
                            .setChunkY(chunkY)
                            .setChunkZ(chunkZ)
                            .setData(configJson)
                            .setIncludeEmpty(includeEmpty)
                            .build())
                    .build();

            CompletableFuture<VoxelChunk> future = new CompletableFuture<>();
            pendingChunkLoads.put(messageId, future);

            sendMessage(CHUNK_FULL_LOAD_REQUESTS_TOPIC, messageId, message)
                .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    }
                });

            LOGGER.info("Loading full chunk ({},{},{}) in world {} with includeEmpty={} with messageId {} (with response)",
                       chunkX, chunkY, chunkZ, worldId, includeEmpty, messageId);

            return future;

        } catch (Exception e) {
            LOGGER.error("Failed to create full chunk load request with response", e);
            CompletableFuture<VoxelChunk> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Lädt spezifische Voxel mit Response-Handling
     *
     * @param worldId Die Welt-ID
     * @param x       X-Koordinate
     * @param y       Y-Koordinate
     * @param z       Z-Koordinate
     * @param radius  Radius um die Koordinaten
     * @return CompletableFuture mit List<Voxel> Response
     */
    public CompletableFuture<List<Voxel>> loadVoxelsWithResponse(String worldId, int x, int y, int z, int radius) {
        String messageId = UUID.randomUUID().toString();
        String requestJson = String.format("{\"x\":%d,\"y\":%d,\"z\":%d,\"radius\":%d}", x, y, z, radius);

        VoxelOperationMessage message = VoxelOperationMessage.newBuilder()
                .setMessageId(messageId)
                .setOperation(OperationType.SAVE)
                .setWorldId(worldId)
                .setVoxelData(VoxelData.newBuilder()
                        .setX(x)
                        .setY(y)
                        .setZ(z)
                        .setData(requestJson)
                        .build())
                .build();

        CompletableFuture<List<Voxel>> future = new CompletableFuture<>();
        pendingVoxelLoads.put(messageId, future);

        sendMessage(VOXEL_OPERATIONS_TOPIC, messageId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        LOGGER.info("Loading voxels around ({},{},{}) with radius {} in world {} with messageId {} (with response)",
                   x, y, z, radius, worldId, messageId);

        return future;
    }

    /**
     * Bereinigt abgelaufene Requests aus den pending Maps
     * Diese Methode sollte periodisch aufgerufen werden
     */
    public void cleanupExpiredRequests() {
        long expiredCount = 0;

        // Cleanup für Voxel Operation Requests
        expiredCount += cleanupExpiredRequests(pendingVoxelOperations, "voxel operation");

        // Cleanup für Chunk Load Requests
        expiredCount += cleanupExpiredRequests(pendingChunkLoads, "chunk load");

        // Cleanup für Voxel Load Requests
        expiredCount += cleanupExpiredRequests(pendingVoxelLoads, "voxel load");

        if (expiredCount > 0) {
            LOGGER.info("Cleaned up {} expired request(s)", expiredCount);
        }
    }

    /**
     * Hilfsmethode zum Bereinigen von abgelaufenen Requests
     */
    private <T> long cleanupExpiredRequests(ConcurrentHashMap<String, CompletableFuture<T>> pendingRequests, String requestType) {
        long removedCount = 0;
        var iterator = pendingRequests.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            CompletableFuture<T> future = entry.getValue();
            if (future.isDone() || future.isCancelled()) {
                LOGGER.debug("Removing completed/cancelled {} request: {}", requestType, entry.getKey());
                iterator.remove();
                removedCount++;
            }
        }

        return removedCount;
    }

    /**
     * Gibt die Anzahl der wartenden Requests zurück
     */
    public int getPendingRequestCount() {
        return pendingVoxelOperations.size() +
               pendingChunkLoads.size() +
               pendingVoxelLoads.size();
    }

    /**
     * Gibt Statistiken über wartende Requests zurück
     */
    public String getPendingRequestStats() {
        return String.format("Pending requests - VoxelOps: %d, ChunkLoads: %d, VoxelLoads: %d",
                pendingVoxelOperations.size(),
                pendingChunkLoads.size(),
                pendingVoxelLoads.size());
    }

    /**
     * Handler für Voxel-Operation-Bestätigungen
     * Diese Methode wird aufgerufen, wenn eine Voxel-Operation erfolgreich verarbeitet wurde
     *
     * @param messageId Die Message-ID der ursprünglichen Operation
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleVoxelOperationConfirmation(String messageId) {
        CompletableFuture<VoxelOperationMessage> future = pendingVoxelOperations.remove(messageId);

        if (future != null) {
            LOGGER.debug("Completing voxel operation with messageId {}", messageId);
            // Erstelle eine Bestätigungs-Nachricht
            VoxelOperationMessage confirmation = VoxelOperationMessage.newBuilder()
                    .setMessageId(messageId)
                    .setOperation(OperationType.SAVE) // oder entsprechende Operation
                    .build();
            future.complete(confirmation);
            return true;
        } else {
            LOGGER.warn("Received voxel operation confirmation for unknown message ID: {}", messageId);
            return false;
        }
    }

    /**
     * Handler für Voxel-Operation-Fehler
     * Diese Methode wird aufgerufen, wenn eine Voxel-Operation fehlgeschlagen ist
     *
     * @param messageId Die Message-ID der ursprünglichen Operation
     * @param error Die Fehlermeldung
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleVoxelOperationError(String messageId, String error) {
        CompletableFuture<VoxelOperationMessage> future = pendingVoxelOperations.remove(messageId);

        if (future != null) {
            LOGGER.debug("Completing voxel operation with error for messageId {}: {}", messageId, error);
            future.completeExceptionally(new RuntimeException("Voxel operation failed: " + error));
            return true;
        } else {
            LOGGER.warn("Received voxel operation error for unknown message ID: {}", messageId);
            return false;
        }
    }

    /**
     * Handler für Chunk-Load-Fehler
     * Diese Methode wird aufgerufen, wenn ein Chunk-Load fehlgeschlagen ist
     *
     * @param messageId Die Message-ID der ursprünglichen Operation
     * @param error Die Fehlermeldung
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleChunkLoadError(String messageId, String error) {
        CompletableFuture<VoxelChunk> future = pendingChunkLoads.remove(messageId);

        if (future != null) {
            LOGGER.debug("Completing chunk load with error for messageId {}: {}", messageId, error);
            future.completeExceptionally(new RuntimeException("Chunk load failed: " + error));
            return true;
        } else {
            LOGGER.warn("Received chunk load error for unknown message ID: {}", messageId);
            return false;
        }
    }

    /**
     * Handler für Voxel-Load-Fehler
     * Diese Methode wird aufgerufen, wenn ein Voxel-Load fehlgeschlagen ist
     *
     * @param messageId Die Message-ID der ursprünglichen Operation
     * @param error Die Fehlermeldung
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleVoxelLoadError(String messageId, String error) {
        CompletableFuture<List<Voxel>> future = pendingVoxelLoads.remove(messageId);

        if (future != null) {
            LOGGER.debug("Completing voxel load with error for messageId {}: {}", messageId, error);
            future.completeExceptionally(new RuntimeException("Voxel load failed: " + error));
            return true;
        } else {
            LOGGER.warn("Received voxel load error for unknown message ID: {}", messageId);
            return false;
        }
    }
}
