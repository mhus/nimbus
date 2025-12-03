package de.mhus.nimbus.world.player.ws.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.world.player.ws.PlayerSession;
import de.mhus.nimbus.world.player.ws.SessionManager;
import de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Listens for chunk list requests from world-life pods.
 * Channel: world:{worldId}:c.l.req
 *
 * When a request is received, responds with all currently registered chunks
 * from all authenticated sessions on this pod.
 *
 * Response channel: world:{worldId}:c.l.resp
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkListRequestListener {

    private final WorldRedisMessagingService redisMessaging;
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void subscribeToChunkListRequests() {
        // Subscribe to "main" world (hardcoded for now, TODO: dynamic world subscription)
        redisMessaging.subscribe("main", "c.l.req", this::handleChunkListRequest);
        log.info("Subscribed to chunk list requests for world: main");
    }

    /**
     * Handle chunk list request from world-life.
     *
     * Request format:
     * {
     *   "requestId": "uuid-12345",
     *   "timestamp": 1234567890
     * }
     */
    private void handleChunkListRequest(String topic, String message) {
        try {
            JsonNode data = objectMapper.readTree(message);
            String requestId = data.has("requestId") ? data.get("requestId").asText() : null;

            if (requestId == null) {
                log.warn("Chunk list request missing requestId: {}", message);
                return;
            }

            // Collect all registered chunks from all authenticated sessions
            Set<ChunkCoordinate> allChunks = new HashSet<>();

            for (PlayerSession session : sessionManager.getAllSessions().values()) {
                // Only include authenticated sessions
                if (!session.isAuthenticated()) {
                    continue;
                }

                // Parse registered chunks from session
                for (String chunkKey : session.getRegisteredChunks()) {
                    try {
                        ChunkCoordinate coord = parseChunkKey(chunkKey);
                        allChunks.add(coord);
                    } catch (Exception e) {
                        log.warn("Invalid chunk key format in session: {}", chunkKey);
                    }
                }
            }

            // Publish response
            publishChunkListResponse(requestId, allChunks);

            log.debug("Handled chunk list request: requestId={}, unique chunks={}",
                    requestId, allChunks.size());

        } catch (Exception e) {
            log.error("Failed to handle chunk list request: {}", message, e);
        }
    }

    /**
     * Publish chunk list response to Redis.
     *
     * Response format:
     * {
     *   "requestId": "uuid-12345",
     *   "podId": "world-player-pod-1",
     *   "chunks": [{"cx": 6, "cz": -13}, ...]
     * }
     */
    private void publishChunkListResponse(String requestId, Set<ChunkCoordinate> chunks) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("requestId", requestId);
            response.put("podId", getPodId());

            ArrayNode chunksArray = response.putArray("chunks");
            for (ChunkCoordinate coord : chunks) {
                ObjectNode chunkObj = chunksArray.addObject();
                chunkObj.put("cx", coord.cx);
                chunkObj.put("cz", coord.cz);
            }

            String json = objectMapper.writeValueAsString(response);
            redisMessaging.publish("main", "c.l.resp", json);

            log.trace("Published chunk list response: requestId={}, chunks={}", requestId, chunks.size());

        } catch (Exception e) {
            log.error("Failed to publish chunk list response: requestId={}", requestId, e);
        }
    }

    /**
     * Parse chunk key from "cx:cz" format.
     */
    private ChunkCoordinate parseChunkKey(String chunkKey) {
        String[] parts = chunkKey.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid chunk key: " + chunkKey);
        }
        return new ChunkCoordinate(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    /**
     * Get pod identifier.
     */
    private String getPodId() {
        return System.getenv().getOrDefault("HOSTNAME", "world-player-local");
    }

    private record ChunkCoordinate(int cx, int cz) {}
}
