package de.mhus.nimbus.world.player.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.ws.PlayerSession;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles block interaction messages from clients.
 * Message type: "b.int" (Block Interaction, Client → Server)
 *
 * Client sends block interactions (break, place, modify).
 * Server validates, updates world state, and broadcasts to other clients.
 *
 * Expected data:
 * {
 *   "action": "break" | "place" | "modify",
 *   "position": {"x": 10, "y": 64, "z": 20},
 *   "blockTypeId": "core:stone",  // for place/modify
 *   "metadata": {}  // optional
 * }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BlockInteractionHandler implements MessageHandler {

    private final ObjectMapper objectMapper;
    private final BlockUpdateSender blockUpdateSender;
    private final WWorldService worldService;

    @Override
    public String getMessageType() {
        return "b.int";
    }

    @Override
    public void handle(PlayerSession session, NetworkMessage message) throws Exception {
        if (!session.isAuthenticated()) {
            log.warn("Block interaction from unauthenticated session: {}",
                    session.getWebSocketSession().getId());
            return;
        }

        JsonNode data = message.getD();

        // Extract interaction data
        String action = data.has("action") ? data.get("action").asText() : null;
        JsonNode position = data.has("position") ? data.get("position") : null;
        String blockTypeId = data.has("blockTypeId") ? data.get("blockTypeId").asText() : null;

        if (action == null || position == null) {
            log.warn("Invalid block interaction: missing action or position");
            return;
        }

        int x = position.has("x") ? position.get("x").asInt() : 0;
        int y = position.has("y") ? position.get("y").asInt() : 0;
        int z = position.has("z") ? position.get("z").asInt() : 0;

        // TODO: Validate player has permission to modify this block
        // TODO: Validate block position is within world bounds
        // TODO: Update block in database (via WChunkService)

        // Get world and calculate chunk coordinates using configured chunkSize
        WWorld world = worldService.getByWorldId(session.getWorldId()).orElse(null);
        if (world == null || world.getPublicData() == null) {
            log.warn("World not found: {}", session.getWorldId());
            return;
        }

        int cx = world.getChunkX(x);
        int cz = world.getChunkZ(z);

        // Create block update for broadcast
        List<JsonNode> blockUpdates = new ArrayList<>();
        blockUpdates.add(createBlockUpdate(x, y, z, blockTypeId, action));

        // Broadcast to all clients in the chunk
        blockUpdateSender.broadcastToChunk(session.getWorldId(), cx, cz, blockUpdates);

        log.debug("Block interaction: action={}, pos=({}, {}, {}), blockType={}, user={}",
                action, x, y, z, blockTypeId, session.getDisplayName());
    }

    private JsonNode createBlockUpdate(int x, int y, int z, String blockTypeId, String action) {
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode blockData = mapper.createObjectNode();

        // Position
        com.fasterxml.jackson.databind.node.ObjectNode pos = mapper.createObjectNode();
        pos.put("x", x);
        pos.put("y", y);
        pos.put("z", z);
        blockData.set("position", pos);

        // BlockTypeId (or 0 for AIR if breaking)
        if ("break".equals(action)) {
            blockData.put("blockTypeId", "core:air");
        } else {
            blockData.put("blockTypeId", blockTypeId != null ? blockTypeId : "core:air");
        }

        return blockData;
    }
}
