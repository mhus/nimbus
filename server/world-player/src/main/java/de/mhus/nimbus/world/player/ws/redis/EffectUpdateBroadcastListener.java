package de.mhus.nimbus.world.player.ws.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.world.player.ws.BroadcastService;
import de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Redis listener for effect update events.
 * Receives effect update events from Redis and distributes to relevant sessions.
 *
 * Distribution: Broadcasts to all chunks listed in the "chunks" array.
 *
 * Redis message format:
 * {
 *   "sessionId": "abc123",
 *   "userId": "user123",
 *   "displayName": "Player",
 *   "effectId": "effect_123",
 *   "chunks": [{"x":1,"z":4}, {"x":2,"z":4}],
 *   "variables": { "intensity": 0.8, ... }
 * }
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EffectUpdateBroadcastListener {

    private final WorldRedisMessagingService redisMessaging;
    private final BroadcastService broadcastService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void subscribeToWorlds() {
        // Subscribe to "main" world for now
        subscribeToWorld("main");
    }

    public void subscribeToWorld(String worldId) {
        redisMessaging.subscribe(worldId, "e.u", (topic, message) -> {
            handleEffectUpdate(worldId, message);
        });
        log.info("Subscribed to effect update events for world: {}", worldId);
    }

    private void handleEffectUpdate(String worldId, String message) {
        try {
            JsonNode data = objectMapper.readTree(message);

            // Extract metadata
            String originatingSessionId = data.has("sessionId") ? data.get("sessionId").asText() : null;
            ArrayNode chunks = data.has("chunks") && data.get("chunks").isArray()
                    ? (ArrayNode) data.get("chunks") : null;

            if (originatingSessionId == null) {
                log.warn("Effect update without sessionId, ignoring");
                return;
            }

            // Build client message (without internal metadata)
            ObjectNode clientData = objectMapper.createObjectNode();
            if (data.has("effectId")) clientData.put("effectId", data.get("effectId").asText());
            if (data.has("chunks")) clientData.set("chunks", data.get("chunks"));
            if (data.has("variables")) clientData.set("variables", data.get("variables"));

            // Broadcast to all affected chunks
            if (chunks != null && chunks.size() > 0) {
                int totalSent = 0;

                for (JsonNode chunkNode : chunks) {
                    if (!chunkNode.has("x") || !chunkNode.has("z")) continue;

                    int cx = chunkNode.get("x").asInt();
                    int cz = chunkNode.get("z").asInt();

                    // Broadcast to this chunk
                    int sent = broadcastService.broadcastToWorld(
                            worldId, "e.u", clientData, originatingSessionId, cx, cz);
                    totalSent += sent;
                }

                log.trace("Distributed effect update to {} sessions across {} chunks",
                        totalSent, chunks.size());
            } else {
                // No chunks specified, broadcast to entire world
                int sent = broadcastService.broadcastToWorld(worldId, "e.u", clientData, originatingSessionId, null, null);
                log.trace("Distributed effect update to {} sessions (world-wide)", sent);
            }

        } catch (Exception e) {
            log.error("Failed to handle effect update from Redis: {}", message, e);
        }
    }

    public void unsubscribeFromWorld(String worldId) {
        redisMessaging.unsubscribe(worldId, "e.u");
        log.info("Unsubscribed from effect update events for world: {}", worldId);
    }
}
