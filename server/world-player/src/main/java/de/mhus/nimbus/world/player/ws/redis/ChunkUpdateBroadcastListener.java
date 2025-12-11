package de.mhus.nimbus.world.player.ws.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.mhus.nimbus.generated.network.messages.ChunkDataTransferObject;
import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.player.ws.BroadcastService;
import de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService;
import de.mhus.nimbus.world.shared.world.WChunkService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Redis listener for chunk update events.
 * Receives chunk updates from world-control and distributes to connected clients.
 *
 * Redis message format:
 * {
 *   "chunkKey": "0:0",
 *   "cx": 0,
 *   "cz": 0,
 *   "blockCount": 256
 * }
 * OR
 * {
 *   "chunkKey": "0:0",
 *   "deleted": true
 * }
 *
 * Client message type: "c.u" (Chunk Update)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkUpdateBroadcastListener {

    private final WorldRedisMessagingService redisMessaging;
    private final BroadcastService broadcastService;
    private final WChunkService chunkService;
    private final ObjectMapper objectMapper;

    /**
     * Subscribe to all active worlds on startup.
     * TODO: Dynamically subscribe when new worlds become active
     */
    @PostConstruct
    public void subscribeToWorlds() {
        // Subscribe to "main" world for now
        subscribeToWorld("main");
    }

    /**
     * Subscribe to chunk update events for a specific world.
     */
    public void subscribeToWorld(String worldId) {
        redisMessaging.subscribe(worldId, "c.update", (topic, message) -> {
            handleChunkUpdate(worldId, message);
        });
        log.info("Subscribed to chunk update events for world: {}", worldId);
    }

    /**
     * Handle incoming chunk update event from Redis.
     * Load updated chunk and send to all sessions registered for that chunk.
     */
    private void handleChunkUpdate(String worldId, String message) {
        try {
            JsonNode data = objectMapper.readTree(message);

            String chunkKey = data.get("chunkKey").asText();
            int cx = data.has("cx") ? data.get("cx").asInt() : parseChunkX(chunkKey);
            int cz = data.has("cz") ? data.get("cz").asInt() : parseChunkZ(chunkKey);
            boolean deleted = data.has("deleted") && data.get("deleted").asBoolean();

            log.debug("Received chunk update: world={} chunk={} deleted={}",
                    worldId, chunkKey, deleted);

            // Load updated chunk from database
            WorldId wid = WorldId.of(worldId).orElse(null);
            if (wid == null) {
                log.warn("Invalid worldId: {}", worldId);
                return;
            }

            Optional<ChunkData> chunkDataOpt = chunkService.loadChunkData(wid, chunkKey, false);

            if (chunkDataOpt.isEmpty() && !deleted) {
                log.warn("Updated chunk not found in database: world={} chunk={}", worldId, chunkKey);
                return;
            }

            // Build client message - ARRAY format like ChunkRegistrationHandler
            ArrayNode chunksArray = objectMapper.createArrayNode();

            if (deleted || chunkDataOpt.isEmpty()) {
                // Send deleted marker (empty array or special format?)
                // For now, skip sending deleted chunks
                log.debug("Chunk deleted, skipping broadcast: world={} chunk={}", worldId, chunkKey);
                return;
            } else {
                // Include full chunk data
                ChunkData chunkData = chunkDataOpt.get();
                ChunkDataTransferObject transferObject = chunkService.toTransferObject(wid, chunkData);
                chunksArray.add(objectMapper.valueToTree(transferObject));
            }

            // Broadcast to all sessions registered for this chunk
            int sent = broadcastService.broadcastToChunk(
                    worldId, cx, cz, "c.u", chunksArray, null);

            log.info("Broadcast chunk update to {} sessions: world={} chunk={}",
                    sent, worldId, chunkKey);

        } catch (Exception e) {
            log.error("Failed to handle chunk update from Redis: {}", message, e);
        }
    }

    /**
     * Unsubscribe from world (e.g., when shutting down).
     */
    public void unsubscribeFromWorld(String worldId) {
        redisMessaging.unsubscribe(worldId, "c.update");
        log.info("Unsubscribed from chunk update events for world: {}", worldId);
    }

    private int parseChunkX(String chunkKey) {
        return Integer.parseInt(chunkKey.split(":")[0]);
    }

    private int parseChunkZ(String chunkKey) {
        return Integer.parseInt(chunkKey.split(":")[1]);
    }
}
