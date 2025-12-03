package de.mhus.nimbus.world.player.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.ws.PlayerSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles entity interaction messages from clients.
 * Message type: "e.int.r" (Entity Interaction Request, Client → Server)
 *
 * Client sends entity interactions when player interacts with NPCs or entities.
 * Server processes the interaction (currently just logging).
 *
 * Expected data:
 * {
 *   "entityId": "npc_farmer_001",
 *   "ts": 1697045600000,  // timestamp
 *   "ac": "click",  // action: 'click', 'fireShortcut', 'use', 'talk', 'attack', 'touch', etc.
 *   "pa": {  // params
 *     "clickType": "left",  // for 'click' action
 *     "shortcutNr": 2,      // for 'fireShortcut' action
 *     ...
 *   }
 * }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EntityInteractionHandler implements MessageHandler {

    @Override
    public String getMessageType() {
        return "e.int.r";
    }

    @Override
    public void handle(PlayerSession session, NetworkMessage message) throws Exception {
        if (!session.isAuthenticated()) {
            log.warn("Entity interaction from unauthenticated session: {}",
                    session.getWebSocketSession().getId());
            return;
        }

        JsonNode data = message.getD();

        // Extract interaction data
        String entityId = data.has("entityId") ? data.get("entityId").asText() : null;
        Long timestamp = data.has("ts") ? data.get("ts").asLong() : null;
        String action = data.has("ac") ? data.get("ac").asText() : null;
        JsonNode params = data.has("pa") ? data.get("pa") : null;

        if (entityId == null || action == null) {
            log.warn("Entity interaction without entityId or action");
            return;
        }

        // TODO: Load entity from database
        // TODO: Check if entity is interactive
        // TODO: Validate player has permission to interact
        // TODO: Execute interaction logic based on action type
        // TODO: Send response to client if needed

        // For now, just log the interaction
        log.info("Entity interaction: entityId={}, action={}, params={}, user={}, session={}",
                entityId, action, params, session.getDisplayName(), session.getSessionId());

        // Example actions to handle in future:
        // - "click" → Open dialog, trigger action
        // - "fireShortcut" → Execute shortcut action
        // - "use" → Use item on entity
        // - "talk" → Start conversation
        // - "attack" → Combat action
        // - "touch" → Proximity trigger
        // - "entityCollision" → Collision event
        // - "entityProximity" → Attention range event
    }
}
