package de.mhus.nimbus.world.life.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.world.life.config.WorldLifeProperties;
import de.mhus.nimbus.world.life.model.ChunkCoordinate;
import de.mhus.nimbus.world.life.service.ChunkAliveService;
import de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes chunk list requests to world-player pods and listens for responses.
 *
 * Request channel: world:{worldId}:c.l.req
 * Response channel: world:{worldId}:c.l.resp
 *
 * Requests are broadcast to all world-player pods, each pod responds with its current chunk list.
 * Responses are aggregated and used to refresh the active chunk set.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkListRequestPublisher {

    private final WorldRedisMessagingService redisMessaging;
    private final ChunkAliveService chunkAliveService;
    private final WorldLifeProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Pending requests awaiting responses.
     * Maps requestId to aggregated chunk set.
     */
    private final Map<String, Set<ChunkCoordinate>> pendingRequests = new ConcurrentHashMap<>();

    @PostConstruct
    public void subscribeToResponses() {
        String worldId = properties.getWorldId();
        redisMessaging.subscribe(worldId, "c.l.resp", this::handleChunkListResponse);
        log.info("Subscribed to chunk list responses for world: {}", worldId);
    }

    /**
     * Request chunk lists from all world-player pods.
     * Publishes request to Redis, all pods will respond.
     *
     * @return Request ID for tracking responses
     */
    public String requestChunkLists() {
        try {
            String requestId = UUID.randomUUID().toString();
            String worldId = properties.getWorldId();

            // Initialize pending request
            pendingRequests.put(requestId, new HashSet<>());

            // Publish request
            ObjectNode request = objectMapper.createObjectNode();
            request.put("requestId", requestId);
            request.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(request);
            redisMessaging.publish(worldId, "c.l.req", json);

            log.debug("Published chunk list request: requestId={}", requestId);

            // Schedule response timeout cleanup (after 5 seconds)
            scheduleRequestCleanup(requestId);

            return requestId;

        } catch (Exception e) {
            log.error("Failed to publish chunk list request", e);
            return null;
        }
    }

    /**
     * Handle chunk list response from a world-player pod.
     *
     * Message format:
     * {
     *   "requestId": "uuid",
     *   "podId": "world-player-1",
     *   "chunks": [{"cx": 6, "cz": -13}, ...]
     * }
     */
    private void handleChunkListResponse(String topic, String message) {
        try {
            JsonNode data = objectMapper.readTree(message);

            String requestId = data.has("requestId") ? data.get("requestId").asText() : null;
            String podId = data.has("podId") ? data.get("podId").asText() : null;
            JsonNode chunksNode = data.get("chunks");

            if (requestId == null || !pendingRequests.containsKey(requestId)) {
                log.trace("Received chunk list response for unknown/expired request: {}", requestId);
                return;
            }

            if (chunksNode == null || !chunksNode.isArray()) {
                log.warn("Invalid chunk list response from pod {}: missing chunks array", podId);
                return;
            }

            // Parse chunks
            Set<ChunkCoordinate> chunks = pendingRequests.get(requestId);
            for (JsonNode chunkNode : chunksNode) {
                int cx = chunkNode.has("cx") ? chunkNode.get("cx").asInt() : 0;
                int cz = chunkNode.has("cz") ? chunkNode.get("cz").asInt() : 0;
                chunks.add(new ChunkCoordinate(cx, cz));
            }

            log.debug("Received chunk list response: requestId={}, podId={}, chunks={}",
                    requestId, podId, chunksNode.size());

        } catch (Exception e) {
            log.error("Failed to handle chunk list response: {}", message, e);
        }
    }

    /**
     * Finalize chunk list request and update active chunks.
     * Should be called after waiting for all responses (e.g., 2-3 seconds).
     *
     * @param requestId Request identifier
     */
    public void finalizeRequest(String requestId) {
        Set<ChunkCoordinate> aggregatedChunks = pendingRequests.remove(requestId);

        if (aggregatedChunks != null) {
            chunkAliveService.replaceChunks(aggregatedChunks);
            log.info("Finalized chunk list request: requestId={}, total chunks={}",
                    requestId, aggregatedChunks.size());
        } else {
            log.warn("No pending request found for finalization: requestId={}", requestId);
        }
    }

    /**
     * Schedule cleanup of request after timeout.
     * Prevents memory leak if responses are slow or missing.
     */
    private void scheduleRequestCleanup(String requestId) {
        // Schedule finalization after 5 seconds
        // This gives world-player pods time to respond
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                finalizeRequest(requestId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("Request cleanup interrupted: {}", requestId);
            }
        }).start();
    }
}
