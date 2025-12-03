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

import java.util.HashSet;
import java.util.Set;

/**
 * Redis listener for effect trigger events.
 * Receives effect trigger events from Redis and distributes to relevant sessions.
 *
 * Distribution: Broadcasts to all chunks listed in the "chunks" array.
 *
 * Redis message format:
 * {
 *   "sessionId": "abc123",
 *   "userId": "user123",
 *   "displayName": "Player",
 *   "entityId": "@player_1234",
 *   "effectId": "effect_123",
 *   "chunks": [{"x":1,"z":4}, {"x":2,"z":4}],
 *   "effect": { ... }
 * }
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EffectTriggerBroadcastListener {

    private final WorldRedisMessagingService redisMessaging;
    private final BroadcastService broadcastService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void subscribeToWorlds() {
        // Subscribe to "main" world for now
        subscribeToWorld("main");
    }

    public void subscribeToWorld(String worldId) {
        redisMessaging.subscribe(worldId, "e.t", (topic, message) -> {
            handleEffectTrigger(worldId, message);
        });
        log.info("Subscribed to effect trigger events for world: {}", worldId);
    }

    private void handleEffectTrigger(String worldId, String message) {
        try {
            JsonNode data = objectMapper.readTree(message);

            // Extract metadata
            String originatingSessionId = data.has("sessionId") ? data.get("sessionId").asText() : null;
            ArrayNode chunks = data.has("chunks") && data.get("chunks").isArray()
                    ? (ArrayNode) data.get("chunks") : null;

            if (originatingSessionId == null) {
                log.warn("Effect trigger without sessionId, ignoring");
                return;
            }

            // Build client message (without internal metadata)
            ObjectNode clientData = objectMapper.createObjectNode();
            if (data.has("entityId")) clientData.put("entityId", data.get("entityId").asText());
            if (data.has("effectId")) clientData.put("effectId", data.get("effectId").asText());
            if (data.has("chunks")) clientData.set("chunks", data.get("chunks"));
            if (data.has("effect")) clientData.set("effect", data.get("effect"));

            // Broadcast to all affected chunks
            if (chunks != null && chunks.size() > 0) {
                Set<String> broadcastedSessions = new HashSet<>();
                int totalSent = 0;

                for (JsonNode chunkNode : chunks) {
                    if (!chunkNode.has("x") || !chunkNode.has("z")) continue;

                    int cx = chunkNode.get("x").asInt();
                    int cz = chunkNode.get("z").asInt();

                    // Broadcast to this chunk (BroadcastService handles deduplication per chunk)
                    int sent = broadcastService.broadcastToWorld(
                            worldId, "e.t", clientData, originatingSessionId, cx, cz);
                    totalSent += sent;
                }

                log.trace("Distributed effect trigger to {} sessions across {} chunks",
                        totalSent, chunks.size());
            } else {
                // No chunks specified, broadcast to entire world
                int sent = broadcastService.broadcastToWorld(worldId, "e.t", clientData, originatingSessionId, null, null);
                log.trace("Distributed effect trigger to {} sessions (world-wide)", sent);
            }

        } catch (Exception e) {
            log.error("Failed to handle effect trigger from Redis: {}", message, e);
        }
    }

    public void unsubscribeFromWorld(String worldId) {
        redisMessaging.unsubscribe(worldId, "e.t");
        log.info("Unsubscribed from effect trigger events for world: {}", worldId);
    }
}
