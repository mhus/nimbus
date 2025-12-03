package de.mhus.nimbus.world.life.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.world.life.config.WorldLifeProperties;
import de.mhus.nimbus.world.life.model.ChunkCoordinate;
import de.mhus.nimbus.world.life.service.ChunkAliveService;
import de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Listens for chunk registration updates from world-player pods.
 * Channel: world:{worldId}:c.r
 *
 * Message format:
 * {
 *   "action": "add" | "remove",
 *   "chunks": [{"cx": 6, "cz": -13}, ...],
 *   "sessionId": "..."
 * }
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkRegistrationListener {

    private final WorldRedisMessagingService redisMessaging;
    private final ChunkAliveService chunkAliveService;
    private final WorldLifeProperties properties;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void subscribeToChunkRegistrations() {
        String worldId = properties.getWorldId();
        redisMessaging.subscribe(worldId, "c.r", this::handleChunkRegistration);
        log.info("Subscribed to chunk registrations for world: {}", worldId);
    }

    /**
     * Handle chunk registration update from Redis.
     *
     * @param topic Redis topic
     * @param message JSON message
     */
    private void handleChunkRegistration(String topic, String message) {
        try {
            JsonNode data = objectMapper.readTree(message);

            String action = data.has("action") ? data.get("action").asText() : null;
            JsonNode chunksNode = data.get("chunks");

            if (action == null || chunksNode == null || !chunksNode.isArray()) {
                log.warn("Invalid chunk registration message: {}", message);
                return;
            }

            // Parse chunk coordinates
            List<ChunkCoordinate> chunks = new ArrayList<>();
            for (JsonNode chunkNode : chunksNode) {
                int cx = chunkNode.has("cx") ? chunkNode.get("cx").asInt() : 0;
                int cz = chunkNode.has("cz") ? chunkNode.get("cz").asInt() : 0;
                chunks.add(new ChunkCoordinate(cx, cz));
            }

            // Update chunk alive service based on action
            switch (action) {
                case "add" -> {
                    chunkAliveService.addChunks(chunks);
                    log.trace("Added {} chunks from registration update", chunks.size());
                }
                case "remove" -> {
                    chunkAliveService.removeChunks(chunks);
                    log.trace("Removed {} chunks from registration update", chunks.size());
                }
                default -> log.warn("Unknown chunk registration action: {}", action);
            }

        } catch (Exception e) {
            log.error("Failed to handle chunk registration update: {}", message, e);
        }
    }
}
