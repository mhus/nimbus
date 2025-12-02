package de.mhus.nimbus.world.player.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.ws.PlayerSession;
import de.mhus.nimbus.world.player.ws.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

/**
 * Handles entity position update messages from clients.
 * Message type: "e.pu" (Entity Position Update, Client → Server)
 *
 * Client sends player/entity position updates.
 * Server validates and broadcasts to other clients in the same chunk/area.
 *
 * Expected data:
 * {
 *   "entityId": "player123",
 *   "position": {"x": 10.5, "y": 64.0, "z": 20.3},
 *   "rotation": {"yaw": 90, "pitch": 0},
 *   "pose": 1,  // WALK
 *   "velocity": {"x": 0.1, "y": 0, "z": 0.2}
 * }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EntityPositionUpdateHandler implements MessageHandler {

    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;

    @Override
    public String getMessageType() {
        return "e.pu";
    }

    @Override
    public void handle(PlayerSession session, NetworkMessage message) throws Exception {
        if (!session.isAuthenticated()) {
            log.warn("Entity position update from unauthenticated session: {}",
                    session.getWebSocketSession().getId());
            return;
        }

        JsonNode data = message.getD();

        // Extract position data
        String entityId = data.has("entityId") ? data.get("entityId").asText() : null;
        JsonNode position = data.has("position") ? data.get("position") : null;
        JsonNode rotation = data.has("rotation") ? data.get("rotation") : null;
        Integer pose = data.has("pose") ? data.get("pose").asInt() : null;

        if (entityId == null || position == null) {
            log.warn("Invalid entity position update: missing entityId or position");
            return;
        }

        // TODO: Validate position (check bounds, anti-cheat)
        // TODO: Update entity position in database if needed
        // TODO: Calculate which chunk the entity is in

        // For now, just broadcast to other sessions in the world
        broadcastPositionUpdate(session, data);

        log.trace("Entity position update: entity={}, pos=({}, {}, {})",
                entityId,
                position.has("x") ? position.get("x").asDouble() : 0,
                position.has("y") ? position.get("y").asDouble() : 0,
                position.has("z") ? position.get("z").asDouble() : 0);
    }

    /**
     * Broadcast position update to other sessions in the same world.
     */
    private void broadcastPositionUpdate(PlayerSession sender, JsonNode data) {
        try {
            NetworkMessage broadcast = NetworkMessage.builder()
                    .t("e.pu")
                    .d(data)
                    .build();

            String json = objectMapper.writeValueAsString(broadcast);
            TextMessage textMessage = new TextMessage(json);

            int sentCount = 0;
            for (PlayerSession session : sessionManager.getAllSessions().values()) {
                // Don't send back to sender
                if (session == sender) continue;

                // Only send to authenticated sessions in same world
                if (session.isAuthenticated() &&
                    sender.getWorldId().equals(session.getWorldId())) {

                    session.getWebSocketSession().sendMessage(textMessage);
                    sentCount++;
                }
            }

            log.trace("Broadcast entity position to {} sessions", sentCount);

        } catch (Exception e) {
            log.error("Failed to broadcast entity position update", e);
        }
    }
}
