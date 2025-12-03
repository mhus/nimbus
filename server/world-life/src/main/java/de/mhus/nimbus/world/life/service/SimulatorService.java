package de.mhus.nimbus.world.life.service;

import de.mhus.nimbus.generated.types.EntityPathway;
import de.mhus.nimbus.generated.types.Vector3;
import de.mhus.nimbus.generated.types.Waypoint;
import de.mhus.nimbus.world.life.behavior.BehaviorRegistry;
import de.mhus.nimbus.world.life.behavior.EntityBehavior;
import de.mhus.nimbus.world.life.config.WorldLifeProperties;
import de.mhus.nimbus.world.life.model.ChunkCoordinate;
import de.mhus.nimbus.world.life.model.SimulationState;
import de.mhus.nimbus.world.life.redis.PathwayPublisher;
import de.mhus.nimbus.world.shared.world.WEntity;
import de.mhus.nimbus.world.shared.world.WEntityRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main entity simulation service.
 *
 * Responsibilities:
 * - Load entities from database
 * - Run simulation loop (every 1 second)
 * - Manage entity simulation states
 * - Coordinate entity ownership across pods
 * - Generate pathways via behavior strategies
 * - Publish pathways to world-player pods
 *
 * Only simulates entities in active chunks (performance optimization).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulatorService {

    private final WEntityRepository entityRepository;
    private final BehaviorRegistry behaviorRegistry;
    private final ChunkAliveService chunkAliveService;
    private final PathwayPublisher pathwayPublisher;
    private final EntityOwnershipService ownershipService;
    private final WorldLifeProperties properties;

    /**
     * Simulation states for all entities in the world.
     * Maps entityId → SimulationState
     */
    private final Map<String, SimulationState> simulationStates = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        log.info("Initializing SimulatorService for world: {}", properties.getWorldId());

        // Load all entities from database
        List<WEntity> entities = entityRepository.findByWorldId(properties.getWorldId());
        log.info("Loaded {} entities from database", entities.size());

        // Initialize simulation state for each entity
        int initializedCount = 0;
        for (WEntity entity : entities) {
            if (!entity.isEnabled()) {
                continue;
            }

            SimulationState state = new SimulationState(entity);
            simulationStates.put(entity.getEntityId(), state);
            initializedCount++;
        }

        // Register for chunk change notifications (optional - not critical for now)
        chunkAliveService.addChangeListener(this::onChunksChanged);

        log.info("SimulatorService initialized: {} entities ready for simulation", initializedCount);
    }

    /**
     * Main simulation loop.
     * Runs every second (configurable via world.life.simulation-interval-ms).
     *
     * For each entity in active chunks:
     * 1. Check chunk is active
     * 2. Claim ownership if needed
     * 3. Simulate entity (generate pathway)
     * 4. Publish pathways to Redis
     */
    @Scheduled(fixedDelayString = "#{${world.life.simulation-interval-ms:1000}}")
    public void simulationLoop() {
        long currentTime = System.currentTimeMillis();
        Set<ChunkCoordinate> activeChunks = chunkAliveService.getActiveChunks();

        if (activeChunks.isEmpty()) {
            log.trace("No active chunks, skipping simulation");
            return;
        }

        List<EntityPathway> newPathways = new ArrayList<>();

        for (Map.Entry<String, SimulationState> entry : simulationStates.entrySet()) {
            String entityId = entry.getKey();
            SimulationState state = entry.getValue();
            WEntity entity = state.getEntity();

            try {
                // 1. Check if entity is in an active chunk
                String entityChunk = entity.getChunk();
                if (entityChunk == null || !chunkAliveService.isChunkActive(entityChunk)) {
                    // Entity chunk is not active, release ownership if we own it
                    if (ownershipService.isOwnedByThisPod(entityId)) {
                        ownershipService.releaseEntity(entityId);
                        log.trace("Released entity {} (chunk {} no longer active)", entityId, entityChunk);
                    }
                    continue;
                }

                // 2. Try to claim ownership if not already owned
                if (!ownershipService.isOwnedByThisPod(entityId)) {
                    boolean claimed = ownershipService.claimEntity(entityId, entityChunk);
                    if (!claimed) {
                        // Another pod owns this entity, skip simulation
                        continue;
                    }
                    log.debug("Claimed entity {} in chunk {}", entityId, entityChunk);
                }

                // 3. Simulate entity
                Optional<EntityPathway> pathway = simulateEntity(entity, state, currentTime);
                pathway.ifPresent(newPathways::add);

            } catch (Exception e) {
                log.error("Error simulating entity: {}", entityId, e);
            }
        }

        // 4. Publish pathways to Redis
        if (!newPathways.isEmpty()) {
            Set<ChunkCoordinate> affectedChunks = calculateAffectedChunks(newPathways);
            pathwayPublisher.publishPathways(newPathways, affectedChunks);

            log.debug("Simulation loop: generated {} pathways, affecting {} chunks",
                    newPathways.size(), affectedChunks.size());
        }
    }

    /**
     * Simulate a single entity and generate pathway if needed.
     *
     * @param entity Entity to simulate
     * @param state Simulation state
     * @param currentTime Current time
     * @return Optional pathway if generated
     */
    private Optional<EntityPathway> simulateEntity(WEntity entity, SimulationState state, long currentTime) {
        // Get behavior for entity
        String behaviorType = getBehaviorType(entity);
        EntityBehavior behavior = behaviorRegistry.getBehavior(behaviorType);

        if (behavior == null) {
            log.warn("Behavior not found: {}, entity: {}", behaviorType, entity.getEntityId());
            return Optional.empty();
        }

        // Generate pathway
        EntityPathway pathway = behavior.update(entity, state, currentTime, properties.getWorldId());

        if (pathway != null) {
            // Update simulation state
            state.setLastPathwayTime(currentTime);
            state.setCurrentPathway(pathway);
            state.updatePathwayEndTime();

            log.trace("Generated pathway for entity {}: {} waypoints",
                    entity.getEntityId(),
                    pathway.getWaypoints() != null ? pathway.getWaypoints().size() : 0);

            return Optional.of(pathway);
        }

        return Optional.empty();
    }

    /**
     * Get behavior type for entity.
     * Reads from entity.behaviorModel field, defaults to PreyAnimalBehavior.
     *
     * @param entity Entity
     * @return Behavior type identifier
     */
    private String getBehaviorType(WEntity entity) {
        String behaviorModel = entity.getBehaviorModel();
        return (behaviorModel != null && !behaviorModel.isBlank()) ? behaviorModel : "PreyAnimalBehavior";
    }

    /**
     * Calculate which chunks are affected by pathways.
     * Used to filter which sessions receive pathway updates.
     *
     * @param pathways List of pathways
     * @return Set of chunk coordinates
     */
    private Set<ChunkCoordinate> calculateAffectedChunks(List<EntityPathway> pathways) {
        Set<ChunkCoordinate> chunks = new HashSet<>();

        for (EntityPathway pathway : pathways) {
            if (pathway.getWaypoints() == null) {
                continue;
            }

            for (Waypoint waypoint : pathway.getWaypoints()) {
                Vector3 target = waypoint.getTarget();
                if (target == null) {
                    continue;
                }

                int cx = (int) Math.floor(target.getX() / 16);
                int cz = (int) Math.floor(target.getZ() / 16);
                chunks.add(new ChunkCoordinate(cx, cz));
            }
        }

        return chunks;
    }

    /**
     * Handle chunk changes notification from ChunkAliveService.
     *
     * @param activeChunks Updated set of active chunks
     */
    private void onChunksChanged(Set<ChunkCoordinate> activeChunks) {
        log.debug("Active chunks changed: {} chunks now active", activeChunks.size());
        // Entities in newly active chunks will be claimed in next simulation loop
        // Entities in deactivated chunks will have ownership released automatically
    }

    /**
     * Try to claim an orphaned entity if it's in an active chunk.
     * Called by OrphanDetectionTask.
     *
     * @param entityId Entity identifier
     */
    public void tryClaimOrphanedEntity(String entityId) {
        SimulationState state = simulationStates.get(entityId);
        if (state == null) {
            log.debug("Entity not found in simulation states: {}", entityId);
            return;
        }

        WEntity entity = state.getEntity();
        String entityChunk = entity.getChunk();

        if (entityChunk == null) {
            log.debug("Entity has no chunk information: {}", entityId);
            return;
        }

        if (!chunkAliveService.isChunkActive(entityChunk)) {
            log.trace("Entity chunk not active, not claiming: {} in chunk {}", entityId, entityChunk);
            return;
        }

        // Try to claim entity
        boolean claimed = ownershipService.claimEntity(entityId, entityChunk);
        if (claimed) {
            log.info("Claimed orphaned entity: {} in chunk {}", entityId, entityChunk);
        }
    }

    /**
     * Get number of entities being simulated.
     *
     * @return Total entity count
     */
    public int getEntityCount() {
        return simulationStates.size();
    }

    /**
     * Get number of entities actively owned by this pod.
     *
     * @return Owned entity count
     */
    public int getOwnedEntityCount() {
        return ownershipService.getOwnedEntityCount();
    }
}
