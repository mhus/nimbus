package de.mhus.nimbus.world.player.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.ws.PlayerSession;
import de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles effect update messages from clients.
 * Message type: "e.u" (Effect Update, Client → Server)
 *
 * Client sends effect updates when effect variables change.
 * Server publishes to Redis for multi-pod broadcasting.
 *
 * Expected data:
 * {
 *   "effectId": "effect_123",
 *   "chunks": [{"x":1,"z":4}, {"x":2,"z":4}],  // optional, affected chunks
 *   "variables": {
 *     "intensity": 0.8,
 *     "duration": 5000,
 *     ...
 *   }
 * }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EffectUpdateHandler implements MessageHandler {

    private final ObjectMapper objectMapper;
    private final WorldRedisMessagingService redisMessaging;

    @Override
    public String getMessageType() {
        return "e.u";
    }

    @Override
    public void handle(PlayerSession session, NetworkMessage message) throws Exception {
        if (!session.isAuthenticated()) {
            log.warn("Effect update from unauthenticated session: {}",
                    session.getWebSocketSession().getId());
            return;
        }

        JsonNode data = message.getD();

        // Extract effect data
        String effectId = data.has("effectId") ? data.get("effectId").asText() : null;
        JsonNode chunks = data.has("chunks") ? data.get("chunks") : null;
        JsonNode variables = data.has("variables") ? data.get("variables") : null;

        if (effectId == null || variables == null) {
            log.warn("Effect update without effectId or variables");
            return;
        }

        // TODO: Validate variables
        // TODO: Check permissions (player allowed to update this effect?)

        // Publish to Redis for multi-pod broadcasting
        publishToRedis(session, data);

        log.debug("Effect update: effectId={}, session={}, variables={}",
                effectId, session.getSessionId(), variables);
    }

    /**
     * Publish effect update to Redis for broadcasting to all pods.
     */
    private void publishToRedis(PlayerSession session, JsonNode originalData) {
        try {
            // Build enriched message with session info
            ObjectNode enriched = objectMapper.createObjectNode();
            enriched.put("sessionId", session.getSessionId());
            enriched.put("userId", session.getUserId());
            enriched.put("displayName", session.getDisplayName());

            // Copy original data
            if (originalData.has("effectId")) enriched.put("effectId", originalData.get("effectId").asText());
            if (originalData.has("chunks")) enriched.set("chunks", originalData.get("chunks"));
            if (originalData.has("variables")) enriched.set("variables", originalData.get("variables"));

            String json = objectMapper.writeValueAsString(enriched);
            redisMessaging.publish(session.getWorldId(), "e.u", json);

            log.trace("Published effect update to Redis: worldId={}, sessionId={}",
                    session.getWorldId(), session.getSessionId());

        } catch (Exception e) {
            log.error("Failed to publish effect update to Redis", e);
        }
    }
}
