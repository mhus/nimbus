package de.mhus.nimbus.world.player.ws.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.world.player.ws.BroadcastService;
import de.mhus.nimbus.world.player.ws.SessionManager;
import de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Listens for entity pathway updates from all world-player and world-life pods.
 * Channel pattern: world:*:e.p (subscribes to ALL worlds)
 *
 * Architecture: Each world-player pod can have sessions from any world.
 * Therefore, this listener subscribes to all worlds and filters by worldId
 * in the BroadcastService.
 *
 * Message format:
 * {
 *   "pathways": [{EntityPathway}, ...],
 *   "affectedChunks": [{"cx": 6, "cz": -13}, ...],
 *   "entityToSession": {"@user:char": "sessionId", ...}
 * }
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PathwayBroadcastListener {

    private final WorldRedisMessagingService redisMessaging;
    private final BroadcastService broadcastService;
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void subscribeToPathwayUpdates() {
        // Subscribe to ALL worlds using pattern: world:*:e.p
        redisMessaging.subscribeToAllWorlds("e.p", this::handlePathwayUpdate);
        log.info("Subscribed to entity pathway updates for all worlds (pattern: world:*:e.p)");
    }

    /**
     * Handle pathway update from Redis (world-life or world-player pods).
     *
     * @param topic Redis topic (format: "world:{worldId}:e.p")
     * @param message JSON message
     */
    private void handlePathwayUpdate(String topic, String message) {
        try {
            // Extract worldId from topic: "world:main:e.p" -> "main"
            String worldId = extractWorldIdFromTopic(topic);
            if (worldId == null) {
                log.warn("Could not extract worldId from topic: {}", topic);
                return;
            }

            JsonNode data = objectMapper.readTree(message);

            JsonNode pathways = data.get("pathways");
            JsonNode affectedChunks = data.get("affectedChunks");
            JsonNode entityToSession = data.get("entityToSession");

            if (pathways == null || !pathways.isArray()) {
                log.warn("Invalid pathway update: missing pathways array");
                return;
            }

            if (affectedChunks == null || !affectedChunks.isArray()) {
                log.warn("Invalid pathway update: missing affectedChunks array");
                return;
            }

            // Filter pathways to exclude those from local sessions on this pod
            // This prevents echoing back pathways to the sessions that generated them
            com.fasterxml.jackson.databind.node.ArrayNode filteredPathways = objectMapper.createArrayNode();
            if (entityToSession != null && entityToSession.isObject()) {
                // Build set of entityIds to exclude (those that belong to local sessions)
                java.util.Set<String> localEntityIds = new java.util.HashSet<>();
                java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = entityToSession.fields();
                while (fields.hasNext()) {
                    java.util.Map.Entry<String, JsonNode> entry = fields.next();
                    String entityId = entry.getKey();
                    String sessionId = entry.getValue().asText();
                    // Check if this session exists on this pod
                    if (sessionManager.getAllSessions().containsKey(sessionId)) {
                        localEntityIds.add(entityId);
                    }
                }

                // Filter out local pathways
                for (JsonNode pathway : pathways) {
                    String entityId = pathway.has("entityId") ? pathway.get("entityId").asText() : null;
                    if (entityId == null || !localEntityIds.contains(entityId)) {
                        filteredPathways.add(pathway);
                    }
                }
            } else {
                // No entityToSession mapping, include all pathways (e.g., from world-life)
                for (JsonNode pathway : pathways) {
                    filteredPathways.add(pathway);
                }
            }

            // Skip if no pathways to broadcast after filtering
            if (filteredPathways.isEmpty()) {
                log.trace("No pathways to broadcast after filtering local sessions");
                return;
            }

            // Broadcast pathways to sessions for each affected chunk
            // BroadcastService will filter sessions by chunk registration
            for (JsonNode chunkNode : affectedChunks) {
                int cx = chunkNode.has("cx") ? chunkNode.get("cx").asInt() : 0;
                int cz = chunkNode.has("cz") ? chunkNode.get("cz").asInt() : 0;

                // Broadcast to all sessions that have this chunk registered
                int sentCount = broadcastService.broadcastToWorld(
                        worldId,         // worldId from topic
                        "e.p",           // messageType
                        filteredPathways, // data (filtered pathways array)
                        null,            // originatingSessionId (filtering done above)
                        cx,              // chunk X
                        cz               // chunk Z
                );

                log.trace("Broadcasted {} pathways to {} sessions for chunk ({}, {})",
                        filteredPathways.size(), sentCount, cx, cz);
            }

            log.debug("Handled pathway update: {} pathways, {} filtered, {} chunks",
                    pathways.size(), filteredPathways.size(), affectedChunks.size());

        } catch (Exception e) {
            log.error("Failed to handle pathway update from topic {}: {}", topic, message, e);
        }
    }

    /**
     * Extract worldId from Redis topic.
     * Topic format: "world:{worldId}:e.p"
     *
     * Handles worldIds with special characters (including ':').
     * Extracts everything between "world:" and the last ":e.p".
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

        // Find last occurrence of ":e.p" and extract everything before it
        int lastIndex = withoutPrefix.lastIndexOf(":e.p");
        if (lastIndex > 0) {
            return withoutPrefix.substring(0, lastIndex);
        }

        return null;
    }
}
