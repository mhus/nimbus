package de.mhus.nimbus.world.life.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.world.life.config.WorldLifeProperties;
import de.mhus.nimbus.world.life.model.ChunkCoordinate;
import de.mhus.nimbus.world.life.service.ChunkAliveService;
import de.mhus.nimbus.world.life.service.ChunkTTLTracker;
import de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Listens for periodic full chunk list updates from world-player pods.
 * Channel: world:{worldId}:c.full
 *
 * Message format:
 * {
 *   "podId": "world-player-xyz",
 *   "timestamp": 1234567890,
 *   "chunks": [{"cx": 6, "cz": -13}, ...]
 * }
 *
 * This replaces the old request/response mechanism with a push-based approach.
 * world-player pods automatically publish their registered chunks every minute.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkListFullUpdateListener {

    private final WorldRedisMessagingService redisMessaging;
    private final ChunkAliveService chunkAliveService;
    private final ChunkTTLTracker ttlTracker;
    private final WorldLifeProperties properties;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void subscribe() {
        String worldId = properties.getWorldId();
        redisMessaging.subscribe(worldId, "c.full", this::handleFullUpdate);
        log.info("Subscribed to chunk list full updates for world: {}", worldId);
    }

    /**
     * Handle full chunk list update from a world-player pod.
     *
     * @param topic Redis topic
     * @param message JSON message
     */
    private void handleFullUpdate(String topic, String message) {
        try {
            JsonNode data = objectMapper.readTree(message);

            String podId = data.has("podId") ? data.get("podId").asText() : "unknown";
            long timestamp = data.has("timestamp") ? data.get("timestamp").asLong() : 0;
            JsonNode chunksNode = data.get("chunks");

            if (chunksNode == null || !chunksNode.isArray()) {
                log.warn("Invalid full chunk update from pod {}: missing chunks array", podId);
                return;
            }

            // Parse chunk coordinates
            List<ChunkCoordinate> chunks = new ArrayList<>();
            for (JsonNode chunkNode : chunksNode) {
                int cx = chunkNode.has("cx") ? chunkNode.get("cx").asInt() : 0;
                int cz = chunkNode.has("cz") ? chunkNode.get("cz").asInt() : 0;
                chunks.add(new ChunkCoordinate(cx, cz));
            }

            // Add chunks to alive service (additive operation)
            chunkAliveService.addChunks(chunks);

            // Update TTL timestamps for all chunks
            chunks.forEach(ttlTracker::touch);

            log.debug("Received full chunk update: podId={}, chunks={}, timestamp={}",
                    podId, chunks.size(), timestamp);

        } catch (Exception e) {
            log.error("Failed to handle full chunk update: {}", message, e);
        }
    }
}
