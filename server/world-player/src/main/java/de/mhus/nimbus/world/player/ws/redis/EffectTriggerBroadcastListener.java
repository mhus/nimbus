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
        // Subscribe to ALL worlds using pattern: world:*:e.t
        redisMessaging.subscribeToAllWorlds("e.t", this::handleEffectTrigger);
        log.info("Subscribed to effect trigger events for all worlds (pattern: world:*:e.t)");
    }

    private void handleEffectTrigger(String topic, String message) {
        try {
            // Extract worldId from topic: "world:main:e.t" -> "main"
            String worldId = extractWorldIdFromTopic(topic);
            if (worldId == null) {
                log.warn("Could not extract worldId from topic: {}", topic);
                return;
            }

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
                    // Support both "cx"/"cz" and "x"/"z" formats
                    int cx = chunkNode.has("cx") ? chunkNode.get("cx").asInt() :
                             chunkNode.has("x") ? chunkNode.get("x").asInt() : 0;
                    int cz = chunkNode.has("cz") ? chunkNode.get("cz").asInt() :
                             chunkNode.has("z") ? chunkNode.get("z").asInt() : 0;

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

    /**
     * Extract worldId from Redis topic.
     * Topic format: "world:{worldId}:e.t"
     *
     * @param topic Redis topic
     * @return worldId or null if invalid format
     */
    private String extractWorldIdFromTopic(String topic) {
        if (topic == null || !topic.startsWith("world:")) {
            return null;
        }
        // Remove "world:" prefix
        String withoutPrefix = topic.substring(6);

        // Find last occurrence of ":e.t" and extract everything before it
        int lastIndex = withoutPrefix.lastIndexOf(":e.t");
        if (lastIndex > 0) {
            return withoutPrefix.substring(0, lastIndex);
        }

        return null;
    }
}
