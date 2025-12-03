package de.mhus.nimbus.world.life.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.world.life.config.WorldLifeProperties;
import de.mhus.nimbus.world.life.model.EntityOwnership;
import de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing entity ownership across multiple world-life pods.
 *
 * Uses Redis heartbeat-based ownership coordination:
 * - Each pod claims entities and sends periodic heartbeats (every 5s)
 * - Other pods see the heartbeats and skip simulation for those entities
 * - If heartbeats stop (pod crashed), entities become orphaned
 * - Orphaned entities can be claimed by other pods
 *
 * Channel: world:{worldId}:e.o
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntityOwnershipService {

    private final WorldRedisMessagingService redisMessaging;
    private final WorldLifeProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Registry of all known entity ownerships (from all pods).
     * Maps entityId → EntityOwnership
     */
    private final Map<String, EntityOwnership> ownershipRegistry = new ConcurrentHashMap<>();

    /**
     * Set of entities owned by this pod.
     */
    private final Set<String> ownedEntities = ConcurrentHashMap.newKeySet();

    /**
     * Pod identifier (Kubernetes hostname or configured ID).
     */
    private String podId;

    @PostConstruct
    public void initialize() {
        // Get pod ID from environment or default
        podId = System.getenv().getOrDefault("HOSTNAME", "world-life-local");

        // Subscribe to ownership announcements
        String worldId = properties.getWorldId();
        redisMessaging.subscribe(worldId, "e.o", this::handleOwnershipAnnouncement);

        log.info("EntityOwnershipService initialized: podId={}, world={}", podId, worldId);
    }

    /**
     * Claim ownership of an entity.
     * Checks if entity is already owned by another pod (non-stale).
     * Publishes claim announcement to Redis if successful.
     *
     * @param entityId Entity identifier
     * @param chunk Current chunk where entity is located
     * @return True if claim was successful, false if entity is owned by another pod
     */
    public boolean claimEntity(String entityId, String chunk) {
        long timestamp = System.currentTimeMillis();

        // Check if already owned by another pod (non-stale)
        EntityOwnership existing = ownershipRegistry.get(entityId);
        if (existing != null &&
                !existing.getPodId().equals(podId) &&
                !existing.isStale(timestamp, properties.getOwnershipStaleThresholdMs())) {

            log.trace("Entity {} already owned by pod {}", entityId, existing.getPodId());
            return false;
        }

        // Claim entity
        EntityOwnership ownership = new EntityOwnership(entityId, podId, timestamp, timestamp, chunk);
        ownershipRegistry.put(entityId, ownership);
        ownedEntities.add(entityId);

        // Publish claim announcement
        publishOwnershipAnnouncement("claim", entityId, chunk);

        log.debug("Claimed entity {} in chunk {}", entityId, chunk);
        return true;
    }

    /**
     * Release ownership of an entity.
     * Publishes release announcement to Redis.
     *
     * @param entityId Entity identifier
     */
    public void releaseEntity(String entityId) {
        EntityOwnership ownership = ownershipRegistry.remove(entityId);
        ownedEntities.remove(entityId);

        if (ownership != null) {
            publishOwnershipAnnouncement("release", entityId, ownership.getCurrentChunk());
            log.debug("Released entity {}", entityId);
        }
    }

    /**
     * Send heartbeats for all owned entities.
     * Scheduled task runs every 5 seconds (configurable).
     */
    @Scheduled(fixedDelayString = "#{${world.life.ownership-heartbeat-interval-ms:5000}}")
    public void sendHeartbeats() {
        if (ownedEntities.isEmpty()) {
            return;
        }

        long timestamp = System.currentTimeMillis();

        for (String entityId : ownedEntities) {
            EntityOwnership ownership = ownershipRegistry.get(entityId);
            if (ownership != null) {
                ownership.setLastHeartbeat(timestamp);
                publishOwnershipAnnouncement("claim", entityId, ownership.getCurrentChunk());
            }
        }

        log.trace("Sent heartbeats for {} entities", ownedEntities.size());
    }

    /**
     * Check if entity is owned by this pod.
     *
     * @param entityId Entity identifier
     * @return True if owned by this pod
     */
    public boolean isOwnedByThisPod(String entityId) {
        return ownedEntities.contains(entityId);
    }

    /**
     * Check if entity is orphaned (no owner or stale ownership).
     *
     * @param entityId Entity identifier
     * @return True if entity is orphaned
     */
    public boolean isOrphaned(String entityId) {
        EntityOwnership ownership = ownershipRegistry.get(entityId);

        if (ownership == null) {
            return true; // No ownership record
        }

        long currentTime = System.currentTimeMillis();
        return ownership.isStale(currentTime, properties.getOwnershipStaleThresholdMs());
    }

    /**
     * Get all orphaned entities.
     *
     * @return List of entity IDs with stale ownership
     */
    public List<String> getOrphanedEntities() {
        long currentTime = System.currentTimeMillis();
        long staleThreshold = properties.getOwnershipStaleThresholdMs();

        List<String> orphans = new ArrayList<>();

        for (Map.Entry<String, EntityOwnership> entry : ownershipRegistry.entrySet()) {
            if (entry.getValue().isStale(currentTime, staleThreshold)) {
                orphans.add(entry.getKey());
            }
        }

        return orphans;
    }

    /**
     * Get number of entities owned by this pod.
     *
     * @return Count of owned entities
     */
    public int getOwnedEntityCount() {
        return ownedEntities.size();
    }

    /**
     * Handle ownership announcement from Redis.
     *
     * Message format:
     * {
     *   "action": "claim" | "release",
     *   "entityId": "entity_001",
     *   "podId": "world-life-1",
     *   "timestamp": 1234567890,
     *   "chunk": "6:-13"
     * }
     */
    private void handleOwnershipAnnouncement(String topic, String message) {
        try {
            JsonNode data = objectMapper.readTree(message);

            String action = data.has("action") ? data.get("action").asText() : null;
            String entityId = data.has("entityId") ? data.get("entityId").asText() : null;
            String podIdFromMessage = data.has("podId") ? data.get("podId").asText() : null;
            long timestamp = data.has("timestamp") ? data.get("timestamp").asLong() : System.currentTimeMillis();
            String chunk = data.has("chunk") ? data.get("chunk").asText() : null;

            if (action == null || entityId == null || podIdFromMessage == null) {
                log.warn("Invalid ownership announcement: {}", message);
                return;
            }

            if ("claim".equals(action)) {
                EntityOwnership ownership = new EntityOwnership(entityId, podIdFromMessage, timestamp, timestamp, chunk);
                ownershipRegistry.put(entityId, ownership);

                log.trace("Entity {} claimed by pod {} in chunk {}", entityId, podIdFromMessage, chunk);

            } else if ("release".equals(action)) {
                ownershipRegistry.remove(entityId);
                log.trace("Entity {} released by pod {}", entityId, podIdFromMessage);

            } else {
                log.warn("Unknown ownership action: {}", action);
            }

        } catch (Exception e) {
            log.error("Failed to handle ownership announcement: {}", message, e);
        }
    }

    /**
     * Publish ownership announcement to Redis.
     *
     * @param action "claim" or "release"
     * @param entityId Entity identifier
     * @param chunk Current chunk
     */
    private void publishOwnershipAnnouncement(String action, String entityId, String chunk) {
        try {
            ObjectNode message = objectMapper.createObjectNode();
            message.put("action", action);
            message.put("entityId", entityId);
            message.put("podId", podId);
            message.put("timestamp", System.currentTimeMillis());
            message.put("chunk", chunk);

            String worldId = properties.getWorldId();
            String json = objectMapper.writeValueAsString(message);
            redisMessaging.publish(worldId, "e.o", json);

            log.trace("Published ownership announcement: action={}, entityId={}, chunk={}",
                    action, entityId, chunk);

        } catch (Exception e) {
            log.error("Failed to publish ownership announcement: entityId={}", entityId, e);
        }
    }
}
