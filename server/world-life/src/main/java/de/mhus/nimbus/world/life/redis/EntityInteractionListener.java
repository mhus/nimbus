package de.mhus.nimbus.world.life.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.world.life.config.WorldLifeProperties;
import de.mhus.nimbus.world.life.service.EntityInteractionService;
import de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Listens for entity interaction events from world-player pods.
 * Channel: world:{worldId}:e.int
 *
 * When a player interacts with an entity (click, talk, attack, etc.),
 * world-player forwards the interaction to world-life via Redis.
 *
 * world-life processes the interaction and may:
 * - Update entity behavior
 * - Generate new pathways
 * - Trigger scripts/effects
 * - Update entity state
 *
 * Message format:
 * {
 *   "entityId": "cow2",
 *   "action": "click",
 *   "timestamp": 1234567890,
 *   "params": {...},
 *   "userId": "user123",
 *   "sessionId": "session-abc",
 *   "displayName": "Player"
 * }
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntityInteractionListener {

    private final WorldRedisMessagingService redisMessaging;
    private final EntityInteractionService interactionService;
    private final WorldLifeProperties properties;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void subscribeToEntityInteractions() {
        String worldId = properties.getWorldId();
        redisMessaging.subscribe(worldId, "e.int", this::handleEntityInteraction);
        log.info("Subscribed to entity interactions for world: {}", worldId);
    }

    /**
     * Handle entity interaction from Redis.
     *
     * @param topic Redis topic
     * @param message JSON message
     */
    private void handleEntityInteraction(String topic, String message) {
        try {
            JsonNode data = objectMapper.readTree(message);

            String entityId = data.has("entityId") ? data.get("entityId").asText() : null;
            String action = data.has("action") ? data.get("action").asText() : null;
            Long timestamp = data.has("timestamp") ? data.get("timestamp").asLong() : null;
            JsonNode params = data.has("params") ? data.get("params") : null;

            // Player/session context
            String userId = data.has("userId") ? data.get("userId").asText() : null;
            String sessionId = data.has("sessionId") ? data.get("sessionId").asText() : null;
            String displayName = data.has("displayName") ? data.get("displayName").asText() : null;

            if (entityId == null || action == null) {
                log.warn("Invalid entity interaction message: missing entityId or action");
                return;
            }

            // Process interaction via service
            interactionService.handleInteraction(
                    entityId,
                    action,
                    timestamp,
                    params,
                    userId,
                    sessionId,
                    displayName
            );

            log.debug("Handled entity interaction: entityId={}, action={}, user={}",
                    entityId, action, displayName);

        } catch (Exception e) {
            log.error("Failed to handle entity interaction: {}", message, e);
        }
    }
}
