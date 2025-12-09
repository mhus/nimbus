package de.mhus.nimbus.world.player.ws;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.generated.types.EntityPathway;
import de.mhus.nimbus.generated.types.Waypoint;
import de.mhus.nimbus.generated.types.Vector3;
import de.mhus.nimbus.generated.types.Rotation;
import de.mhus.nimbus.generated.types.ENTITY_POSES;
import de.mhus.nimbus.shared.engine.EngineMapper;
import de.mhus.nimbus.world.player.config.PathwayBroadcastProperties;
import de.mhus.nimbus.world.player.session.PlayerSession;
import de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for generating and broadcasting entity pathways to Redis.
 *
 * Runs a scheduled task every 100ms (configurable) that:
 * 1. Iterates all active sessions
 * 2. Generates EntityPathway from position/rotation/velocity state
 * 3. Filters inactive and unchanged sessions
 * 4. Batches pathways by world
 * 5. Publishes to Redis for distribution to all world-player pods
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PathwayBroadcastService {

    private final SessionManager sessionManager;
    private final WorldRedisMessagingService redisMessaging;
    private final EngineMapper engineMapper;
    private final PathwayBroadcastProperties properties;

    /**
     * Scheduled task: Generate and broadcast entity pathways every 100ms.
     *
     * Spring @Scheduled with fixedRateString from properties.
     */
    @Scheduled(fixedRateString = "${world.player.pathway-broadcast-interval-ms:100}")
    public void broadcastPathways() {
        try {
            // Group pathways and session mappings by worldId
            Map<String, List<EntityPathway>> pathwaysByWorld = new HashMap<>();
            Map<String, Map<String, String>> entityToSessionByWorld = new HashMap<>();

            // Iterate all active sessions
            for (PlayerSession session : sessionManager.getAllSessions().values()) {
                if (!session.isAuthenticated()) continue;

                // Skip if position hasn't been updated recently
                if (session.isUpdateStale(properties.getEntityUpdateTimeoutMs())) {
                    continue;
                }

                // Skip if position hasn't changed
                if (!session.isPositionChanged()) {
                    continue;
                }

                // Generate pathway for this session
                EntityPathway pathway = generatePathway(session);
                if (pathway != null) {
                    String worldId = session.getWorldId().getId();
                    pathwaysByWorld
                        .computeIfAbsent(worldId, k -> new ArrayList<>())
                        .add(pathway);

                    // Store entityId → sessionId mapping for filtering
                    entityToSessionByWorld
                        .computeIfAbsent(worldId, k -> new HashMap<>())
                        .put(pathway.getEntityId(), session.getSessionId());
                }
            }

            // Publish pathways to Redis (grouped by world)
            for (Map.Entry<String, List<EntityPathway>> entry : pathwaysByWorld.entrySet()) {
                String worldId = entry.getKey();
                Map<String, String> entityToSession = entityToSessionByWorld.get(worldId);
                publishPathways(worldId, entry.getValue(), entityToSession);
            }

            if (!pathwaysByWorld.isEmpty()) {
                log.trace("Broadcasted {} pathways across {} worlds",
                    pathwaysByWorld.values().stream().mapToInt(List::size).sum(),
                    pathwaysByWorld.size());
            }

        } catch (Exception e) {
            log.error("Error in pathway broadcast task", e);
        }
    }

    /**
     * Generate EntityPathway from PlayerSession state.
     *
     * Creates a predicted pathway with:
     * - Current position as start waypoint
     * - Predicted target position (current + velocity * predictionTime)
     */
    private EntityPathway generatePathway(PlayerSession session) {
        try {
            Vector3 position = session.getLastPosition();
            Rotation rotation = session.getLastRotation();
            Vector3 velocity = session.getLastVelocity();
            ENTITY_POSES pose = session.getLastPose();

            if (position == null) return null;

            long now = System.currentTimeMillis();
            long predictionMs = properties.getPathwayPredictionTimeMs();

            // Calculate predicted target position
            Vector3 targetPosition = position;
            if (velocity != null &&
                (Math.abs(velocity.getX()) > 0.001 ||
                 Math.abs(velocity.getY()) > 0.001 ||
                 Math.abs(velocity.getZ()) > 0.001)) {

                double predictionSec = predictionMs / 1000.0;
                targetPosition = Vector3.builder()
                    .x(position.getX() + velocity.getX() * predictionSec)
                    .y(position.getY() + velocity.getY() * predictionSec)
                    .z(position.getZ() + velocity.getZ() * predictionSec)
                    .build();
            }

            // Create waypoints: start (now) → target (now + 100ms)
            List<Waypoint> waypoints = new ArrayList<>();

            // Start waypoint (current position)
            waypoints.add(Waypoint.builder()
                .timestamp(now)
                .target(position)
                .rotation(rotation != null ? rotation : Rotation.builder().y(0.0).p(0.0).build())
                .pose(pose != null ? pose : ENTITY_POSES.IDLE)
                .build());

            // Target waypoint (predicted position)
            waypoints.add(Waypoint.builder()
                .timestamp(now + predictionMs)
                .target(targetPosition)
                .rotation(rotation != null ? rotation : Rotation.builder().y(0.0).p(0.0).build())
                .pose(pose != null ? pose : ENTITY_POSES.IDLE)
                .build());

            // Build EntityPathway
            return EntityPathway.builder()
                .entityId(session.getEntityId())  // Format: "@userId:characterId"
                .startAt(now)
                .waypoints(waypoints)
                .isLooping(false)
                .queryAt(now)
                .idlePose(pose)
                .physicsEnabled(true)
                .velocity(velocity)
                .grounded(null)  // Later implementation: proper grounded detection
                .build();

        } catch (Exception e) {
            log.error("Failed to generate pathway for session {}",
                session.getSessionId(), e);
            return null;
        }
    }

    /**
     * Publish pathways to Redis for broadcasting to all pods.
     *
     * Message format:
     * {
     *   "pathways": [EntityPathway, ...],
     *   "affectedChunks": [{"cx": 6, "cz": -13}, ...],
     *   "entityToSession": {"@user1:char1": "session123", ...}  // EntityId → SessionId mapping
     * }
     */
    private void publishPathways(String worldId, List<EntityPathway> pathways, Map<String, String> entityToSession) {
        try {
            // Determine affected chunks from pathways
            Set<ChunkCoordinate> affectedChunks = new HashSet<>();

            for (EntityPathway pathway : pathways) {
                for (Waypoint wp : pathway.getWaypoints()) {
                    Vector3 pos = wp.getTarget();
                    int cx = (int) Math.floor(pos.getX() / 16);
                    int cz = (int) Math.floor(pos.getZ() / 16);
                    affectedChunks.add(new ChunkCoordinate(cx, cz));
                }
            }

            // Build Redis message
            ObjectNode message = engineMapper.createObjectNode();
            message.set("pathways", engineMapper.valueToTree(pathways));

            ArrayNode chunksArray = engineMapper.createArrayNode();
            for (ChunkCoordinate chunk : affectedChunks) {
                ObjectNode chunkNode = engineMapper.createObjectNode();
                chunkNode.put("cx", chunk.cx);
                chunkNode.put("cz", chunk.cz);
                chunksArray.add(chunkNode);
            }
            message.set("affectedChunks", chunksArray);

            // Add entityId → sessionId mapping for origin filtering
            if (entityToSession != null && !entityToSession.isEmpty()) {
                ObjectNode mappingNode = engineMapper.createObjectNode();
                for (Map.Entry<String, String> entry : entityToSession.entrySet()) {
                    mappingNode.put(entry.getKey(), entry.getValue());
                }
                message.set("entityToSession", mappingNode);
            }

            String json = engineMapper.writeValueAsString(message);
            redisMessaging.publish(worldId, "e.p", json);

            log.debug("Publishing {} pathways to Redis for world {} ({} chunks)",
                pathways.size(), worldId, affectedChunks.size()); // XXX

            log.trace("Published {} pathways to Redis for world {} ({} chunks)",
                pathways.size(), worldId, affectedChunks.size());

        } catch (Exception e) {
            log.error("Failed to publish pathways to Redis", e);
        }
    }

    /**
     * Helper class for chunk coordinate deduplication.
     */
    @Data
    @AllArgsConstructor
    private static class ChunkCoordinate {
        int cx, cz;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkCoordinate)) return false;
            ChunkCoordinate that = (ChunkCoordinate) o;
            return cx == that.cx && cz == that.cz;
        }

        @Override
        public int hashCode() {
            return Objects.hash(cx, cz);
        }
    }
}
