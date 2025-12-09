package de.mhus.nimbus.world.player.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.mhus.nimbus.world.player.ws.ChunkSenderService;
import de.mhus.nimbus.world.player.ws.ChunkSenderService.ChunkCoord;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.session.PlayerSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles chunk registration messages from clients.
 * Message type: "c.r"
 *
 * Clients register chunks they want to receive updates for.
 * Registration is based on player position and view distance.
 * Only registered chunks receive updates.
 *
 * Delta-based: Only newly registered chunks are sent to client.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChunkRegistrationHandler implements MessageHandler {

    private final ChunkSenderService chunkSenderService;
    private final ObjectMapper objectMapper;
    private final de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService redisMessaging;

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

        // Parse requested chunks
        JsonNode chunksArray = data.get("c");
        List<ChunkCoord> requestedChunks = new ArrayList<>();

        for (JsonNode chunkNode : chunksArray) {
            int cx = chunkNode.has("cx") ? chunkNode.get("cx").asInt() : 0;
            int cz = chunkNode.has("cz") ? chunkNode.get("cz").asInt() : 0;
            requestedChunks.add(new ChunkCoord(cx, cz));
        }

        // Calculate delta (new chunks = requested - already registered)
        List<ChunkCoord> newChunks = new ArrayList<>();
        for (ChunkCoord coord : requestedChunks) {
            if (!session.isChunkRegistered(coord.cx(), coord.cz())) {
                newChunks.add(coord);
            }
        }

        // Update registration (replace with new list)
        session.clearChunks();
        for (ChunkCoord coord : requestedChunks) {
            session.registerChunk(coord.cx(), coord.cz());
        }

        log.debug("Chunk registration: session={}, total={}, new={}, worldId={}",
                session.getWebSocketSession().getId(), requestedChunks.size(),
                newChunks.size(), session.getWorldId());

        // Publish chunk registration to Redis for world-life
        if (!newChunks.isEmpty()) {
            publishChunkRegistrationUpdate(session.getWorldId(), "add", newChunks);
        }

        // Asynchronously send new chunks to client
        if (!newChunks.isEmpty()) {
            chunkSenderService.sendChunksAsync(session, newChunks);
        }
    }

    /**
     * Publish chunk registration update to Redis for world-life.
     * Channel: world:{worldId}:c.r
     *
     * @param worldId World identifier
     * @param action "add" or "remove"
     * @param chunks List of chunk coordinates
     */
    private void publishChunkRegistrationUpdate(String worldId, String action, List<ChunkCoord> chunks) {
        try {
            com.fasterxml.jackson.databind.node.ObjectNode message = objectMapper.createObjectNode();
            message.put("action", action);

            ArrayNode chunksArray = message.putArray("chunks");
            for (ChunkCoord chunk : chunks) {
                com.fasterxml.jackson.databind.node.ObjectNode chunkObj = chunksArray.addObject();
                chunkObj.put("cx", chunk.cx());
                chunkObj.put("cz", chunk.cz());
            }

            String json = objectMapper.writeValueAsString(message);
            redisMessaging.publish(worldId, "c.r", json);

            log.trace("Published chunk registration to Redis: action={}, chunks={}", action, chunks.size());

        } catch (Exception e) {
            log.error("Failed to publish chunk registration to Redis: action={}", action, e);
        }
    }
}

