package de.mhus.nimbus.world.player.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.ws.PlayerSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles chunk registration messages from clients.
 * Message type: "c.r"
 *
 * Clients register chunks they want to receive updates for.
 * Registration is based on player position and view distance.
 * Only registered chunks receive updates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChunkRegistrationHandler implements MessageHandler {

    @Override
    public String getMessageType() {
        return "c.r";
    }

    @Override
    public void handle(PlayerSession session, NetworkMessage message) throws Exception {
        JsonNode data = message.getD();

        if (!data.has("c") || !data.get("c").isArray()) {
            log.warn("Invalid chunk registration: missing 'c' array");
            return;
        }

        // Clear current registrations
        session.clearChunks();

        // Register new chunks
        JsonNode chunksArray = data.get("c");
        int registeredCount = 0;

        for (JsonNode chunkNode : chunksArray) {
            int cx = chunkNode.has("x") ? chunkNode.get("x").asInt() : 0;
            int cz = chunkNode.has("z") ? chunkNode.get("z").asInt() : 0;

            session.registerChunk(cx, cz);
            registeredCount++;
        }

        log.debug("Chunk registration: session={}, chunks={}, worldId={}",
                session.getWebSocketSession().getId(), registeredCount, session.getWorldId());

        // TODO: Automatically send chunk data for newly registered chunks
        // This will be handled by ChunkQueryHandler or a separate service
    }
}
