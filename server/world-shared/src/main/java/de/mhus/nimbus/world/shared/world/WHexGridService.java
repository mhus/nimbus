package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.HexGrid;
import de.mhus.nimbus.generated.types.HexVector2;
import de.mhus.nimbus.shared.types.WorldId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for managing WHexGrid entities.
 * Provides business logic for hexagonal grid operations in the world.
 *
 * HexGrids exist separately for each world/zone.
 * Instances CANNOT have their own hex grids - always taken from the defined world.
 * Branches use COW (Copy On Write) - they can have their own hex grids, falling back to parent.
 * HexGrids cannot be deleted in branches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WHexGridService {

    private final WHexGridRepository repository;

    /**
     * Finds a hex grid by world ID and hex position with COW fallback for branches.
     * Instances always look up in their world (without instance suffix).
     * Branches first check their own hex grids, then fall back to parent world.
     *
     * @param worldId The world identifier
     * @param hexPos The hex vector with q and r coordinates
     * @return Optional containing the hex grid if found
     */
    @Transactional(readOnly = true)
    public Optional<WHexGridEntity> findByWorldIdAndPosition(String worldId, HexVector2 hexPos) {
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("worldId required");
        }
        if (hexPos == null) {
            throw new IllegalArgumentException("hexPos required");
        }

        String positionKey = HexMathUtil.positionKey(hexPos);

        // Parse WorldId and filter instances
        WorldId parsedWorldId = WorldId.unchecked(worldId);
        var lookupWorld = parsedWorldId.withoutInstance();

        // Try branch first if this is a branch world
        if (lookupWorld.isBranch()) {
            var hexGrid = repository.findByWorldIdAndPosition(lookupWorld.getId(), positionKey);
            if (hexGrid.isPresent()) {
                return hexGrid;
            }
            // Fallback to parent world (COW)
            var parentWorld = lookupWorld.withoutBranchAndInstance();
            return repository.findByWorldIdAndPosition(parentWorld.getId(), positionKey);
        }

        return repository.findByWorldIdAndPosition(lookupWorld.getId(), positionKey);
    }

    /**
     * Finds all hex grids in a world (no COW fallback for lists).
     * Filters out instances.
     *
     * @param worldId The world identifier
     * @return List of all hex grids in the world
     */
    @Transactional(readOnly = true)
    public List<WHexGridEntity> findByWorldId(String worldId) {
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("worldId required");
        }

        WorldId parsedWorldId = WorldId.unchecked(worldId);
        var lookupWorld = parsedWorldId.withoutInstance();

        return repository.findByWorldId(lookupWorld.getId());
    }

    /**
     * Finds all enabled hex grids in a world (no COW fallback for lists).
     * Filters out instances.
     *
     * @param worldId The world identifier
     * @return List of enabled hex grids in the world
     */
    @Transactional(readOnly = true)
    public List<WHexGridEntity> findAllEnabled(String worldId) {
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("worldId required");
        }

        WorldId parsedWorldId = WorldId.unchecked(worldId);
        var lookupWorld = parsedWorldId.withoutInstance();

        return repository.findByWorldIdAndEnabled(lookupWorld.getId(), true);
    }

    /**
     * Saves a hex grid entity.
     * Automatically synchronizes the position key from publicData.position before saving.
     *
     * @param entity The hex grid entity to save
     * @return The saved entity
     * @throws IllegalStateException if position cannot be synchronized
     */
    @Transactional
    public WHexGridEntity save(WHexGridEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity required");
        }
        if (entity.getPublicData() == null) {
            throw new IllegalArgumentException("publicData required");
        }

        // Ensure position key is synchronized
        entity.syncPositionKey();

        // Set timestamps if new entity
        if (entity.getCreatedAt() == null) {
            entity.touchCreate();
        } else {
            entity.touchUpdate();
        }

        WHexGridEntity saved = repository.save(entity);
        log.debug("Saved WHexGrid: worldId={}, position={}", saved.getWorldId(), saved.getPosition());
        return saved;
    }

    /**
     * Creates a new hex grid.
     * Filters out instances.
     *
     * @param worldId The world identifier
     * @param publicData The hex grid public data with position and metadata
     * @param generatorParams Optional generator parameters (can be null or empty)
     * @return The created hex grid entity
     * @throws IllegalStateException if a hex grid already exists at this position
     */
    @Transactional
    public WHexGridEntity create(String worldId, HexGrid publicData, Map<String, String> generatorParams) {
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("worldId required");
        }
        if (publicData == null || publicData.getPosition() == null) {
            throw new IllegalArgumentException("publicData with position required");
        }

        WorldId parsedWorldId = WorldId.unchecked(worldId);
        var lookupWorld = parsedWorldId.withoutInstance();

        String positionKey = HexMathUtil.positionKey(publicData.getPosition());

        // Check if already exists
        if (repository.existsByWorldIdAndPosition(lookupWorld.getId(), positionKey)) {
            throw new IllegalStateException("Hex grid already exists at worldId=" + lookupWorld.getId() + ", position=" + positionKey);
        }

        WHexGridEntity entity = WHexGridEntity.builder()
                .worldId(lookupWorld.getId())
                .publicData(publicData)
                .position(positionKey)
                .generatorParameters(generatorParams != null ? generatorParams : Map.of())
                .enabled(true)
                .build();

        entity.touchCreate();

        WHexGridEntity saved = repository.save(entity);
        log.info("Created WHexGrid: worldId={}, position={}", lookupWorld.getId(), positionKey);
        return saved;
    }

    /**
     * Updates a hex grid using a consumer function.
     * Filters out instances.
     *
     * @param worldId The world identifier
     * @param hexPos The hex vector with q and r coordinates
     * @param updater Consumer function to modify the entity
     * @return Optional containing the updated entity if found
     */
    @Transactional
    public Optional<WHexGridEntity> update(String worldId, HexVector2 hexPos, Consumer<WHexGridEntity> updater) {
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("worldId required");
        }
        if (hexPos == null) {
            throw new IllegalArgumentException("hexPos required");
        }
        if (updater == null) {
            throw new IllegalArgumentException("updater required");
        }

        WorldId parsedWorldId = WorldId.unchecked(worldId);
        var lookupWorld = parsedWorldId.withoutInstance();

        String positionKey = HexMathUtil.positionKey(hexPos);

        return repository.findByWorldIdAndPosition(lookupWorld.getId(), positionKey).map(entity -> {
            updater.accept(entity);
            entity.syncPositionKey();
            entity.touchUpdate();

            WHexGridEntity saved = repository.save(entity);
            log.debug("Updated WHexGrid: worldId={}, position={}", lookupWorld.getId(), positionKey);
            return saved;
        });
    }

    /**
     * Disables a hex grid (soft delete).
     *
     * @param worldId The world identifier
     * @param hexPos The hex vector with q and r coordinates
     * @return true if the hex grid was found and disabled
     */
    @Transactional
    public boolean disable(String worldId, HexVector2 hexPos) {
        return update(worldId, hexPos, entity -> entity.setEnabled(false)).isPresent();
    }

    /**
     * Enables a hex grid.
     *
     * @param worldId The world identifier
     * @param hexPos The hex vector with q and r coordinates
     * @return true if the hex grid was found and enabled
     */
    @Transactional
    public boolean enable(String worldId, HexVector2 hexPos) {
        return update(worldId, hexPos, entity -> entity.setEnabled(true)).isPresent();
    }

    /**
     * Deletes a hex grid (hard delete).
     * Filters out instances.
     * IMPORTANT: Deletion is NOT allowed in branches - hex grids can only be deleted in main worlds.
     *
     * @param worldId The world identifier
     * @param hexPos The hex vector with q and r coordinates
     * @return true if the hex grid was found and deleted
     */
    @Transactional
    public boolean delete(String worldId, HexVector2 hexPos) {
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("worldId required");
        }
        if (hexPos == null) {
            throw new IllegalArgumentException("hexPos required");
        }

        WorldId parsedWorldId = WorldId.unchecked(worldId);
        var lookupWorld = parsedWorldId.withoutInstance();

        // Prevent deletion in branches
        if (lookupWorld.isBranch()) {
            String positionKey = HexMathUtil.positionKey(hexPos);
            log.warn("Attempted to delete hex grid at position '{}' in branch world '{}' - not allowed", positionKey, lookupWorld.getId());
            throw new IllegalArgumentException("Hex grids cannot be deleted in branches: " + lookupWorld.getId());
        }

        String positionKey = HexMathUtil.positionKey(hexPos);

        return repository.findByWorldIdAndPosition(lookupWorld.getId(), positionKey).map(entity -> {
            repository.delete(entity);
            log.info("Deleted WHexGrid: worldId={}, position={}", lookupWorld.getId(), positionKey);
            return true;
        }).orElse(false);
    }
}
