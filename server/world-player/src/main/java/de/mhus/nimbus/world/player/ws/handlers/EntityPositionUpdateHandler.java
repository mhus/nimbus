package de.mhus.nimbus.world.player.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.session.PlayerSession;
import de.mhus.nimbus.world.player.ws.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

/**
 * Handles entity position update messages from clients.
 * Message type: "e.p.u" (Entity Position Update, Client → Server)
 *
 * Client sends player/entity position updates (can be multiple entities).
 * Server validates and broadcasts to other clients in the same chunk/area.
 *
 * Expected data (array of updates):
 * [
 *   {
 *     "pl": "player",  // local entity id (not unique id)
 *     "p": {"x": 10.5, "y": 64.0, "z": 20.3},  // position (optional)
 *     "r": {"y": 90.0, "p": 0.0},  // rotation: yaw, pitch (optional)
 *     "v": {"x": 0.1, "y": 0, "z": 0.2},  // velocity (optional)
 *     "po": 1,  // pose id (optional)
 *     "ts": 1697045600000,  // timestamp
 *     "ta": {"x": 10.5, "y": 64.0, "z": 20.3, "ts": 1697045800000}  // target position (optional)
 *   }
 * ]
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EntityPositionUpdateHandler implements MessageHandler {

    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;

    @Override
    public String getMessageType() {
        return "e.p.u";
    }

    @Override
    public void handle(PlayerSession session, NetworkMessage message) throws Exception {
        if (!session.isAuthenticated()) {
            log.warn("Entity position update from unauthenticated session: {}",
                    session.getWebSocketSession().getId());
            return;
        }

        JsonNode data = message.getD();

        // Data is an array of entity updates
        if (!data.isArray()) {
            log.warn("Entity position update data is not an array");
            return;
        }

        // Process each entity update
        for (JsonNode entityUpdate : data) {
            processEntityUpdate(session, entityUpdate);
        }
    }

    private void processEntityUpdate(PlayerSession session, JsonNode update) {
        // Extract position data (using compressed field names)
        String playerId = update.has("pl") ? update.get("pl").asText() : null;
        JsonNode position = update.has("p") ? update.get("p") : null;
        JsonNode rotation = update.has("r") ? update.get("r") : null;
        JsonNode velocity = update.has("v") ? update.get("v") : null;
        Integer pose = update.has("po") ? update.get("po").asInt() : null;
        Long timestamp = update.has("ts") ? update.get("ts").asLong() : null;

        if (playerId == null) {
            log.warn("Entity position update without player id (pl)");
            return;
        }

        // TODO: Validate position (check bounds, anti-cheat)
        // TODO: Update entity position in database if needed
        // TODO: Calculate which chunk the entity is in

        // For now, just broadcast to other sessions in the world
        broadcastPositionUpdate(session, update);

        if (log.isTraceEnabled() && position != null) {
            log.trace("Entity position update: pl={}, pos=({}, {}, {}), session={}",
                    playerId,
                    position.has("x") ? position.get("x").asDouble() : 0,
                    position.has("y") ? position.get("y").asDouble() : 0,
                    position.has("z") ? position.get("z").asDouble() : 0,
                    session.getSessionId());
        }
    }

    /**
     * Broadcast position update to other sessions in the same world.
     * Wraps single update in array for consistent message format.
     */
    private void broadcastPositionUpdate(PlayerSession sender, JsonNode singleUpdate) {
        try {
            // Wrap single update in array (message format requires array)
            com.fasterxml.jackson.databind.node.ArrayNode updateArray = objectMapper.createArrayNode();
            updateArray.add(singleUpdate);

            NetworkMessage broadcast = NetworkMessage.builder()
                    .t("e.p.u")
                    .d(updateArray)
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
