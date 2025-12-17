package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.Entity;
import de.mhus.nimbus.shared.types.WorldId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for managing WEntity instances in the world.
 * Entities exist separately for each world/zone/instance.
 * Branches use COW (Copy On Write) - they can have their own entities, falling back to parent.
 * Entities cannot be deleted in branches.
 * No storage functionality supported (always world-instance-specific).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WEntityService {

    private final WEntityRepository repository;

    /**
     * Find entity by entityId with COW fallback for branches.
     * Instances and zones always look up in their main world.
     * Branches first check their own entities, then fall back to parent world.
     */
    @Transactional(readOnly = true)
    public Optional<WEntity> findByWorldIdAndEntityId(WorldId worldId, String entityId) {
        var lookupWorld = worldId.withoutInstanceAndZone();

        // Try branch first if this is a branch world
        if (lookupWorld.isBranch()) {
            var entity = repository.findByWorldIdAndEntityId(lookupWorld.getId(), entityId);
            if (entity.isPresent()) {
                return entity;
            }
            // Fallback to parent world (COW)
            var parentWorld = lookupWorld.withoutBranchAndInstance();
            return repository.findByWorldIdAndEntityId(parentWorld.getId(), entityId);
        }

        return repository.findByWorldIdAndEntityId(lookupWorld.getId(), entityId);
    }

    /**
     * Find all entities for specific world (no COW fallback for lists).
     * Filters out instances and zones.
     */
    @Transactional(readOnly = true)
    public List<WEntity> findByWorldId(WorldId worldId) {
        var lookupWorld = worldId.withoutInstanceAndZone();
        return repository.findByWorldId(lookupWorld.getId());
    }

    /**
     * Find entities by modelId for specific world (no COW fallback for lists).
     * Filters out instances and zones.
     */
    @Transactional(readOnly = true)
    public List<WEntity> findByModelId(WorldId worldId, String modelId) {
        var lookupWorld = worldId.withoutInstanceAndZone();
        return repository.findByWorldIdAndModelId(lookupWorld.getId(), modelId);
    }

    /**
     * Find all enabled entities for specific world (no COW fallback for lists).
     * Filters out instances and zones.
     */
    @Transactional(readOnly = true)
    public List<WEntity> findAllEnabled(WorldId worldId) {
        var lookupWorld = worldId.withoutInstanceAndZone();
        return repository.findByWorldIdAndEnabled(lookupWorld.getId(), true);
    }

    /**
     * Save or update an entity.
     * Filters out instances and zones - entities are stored per world.
     */
    @Transactional
    public WEntity save(WorldId worldId, String entityId, Entity publicData, String modelId) {
        if (worldId == null) {
            throw new IllegalArgumentException("worldId required");
        }
        if (blank(entityId)) {
            throw new IllegalArgumentException("entityId required");
        }
        if (publicData == null) {
            throw new IllegalArgumentException("publicData required");
        }

        var lookupWorld = worldId.withoutInstanceAndZone();

        WEntity entity = repository.findByWorldIdAndEntityId(lookupWorld.getId(), entityId).orElseGet(() -> {
            WEntity neu = WEntity.builder()
                    .worldId(lookupWorld.getId())
                    .entityId(entityId)
                    .modelId(modelId)
                    .enabled(true)
                    .build();
            neu.touchCreate();
            log.debug("Creating new WEntity: world={}, entityId={}", lookupWorld, entityId);
            return neu;
        });

        entity.setPublicData(publicData);
        entity.setModelId(modelId);
        entity.touchUpdate();

        WEntity saved = repository.save(entity);
        log.debug("Saved WEntity: world={}, entityId={}", lookupWorld, entityId);
        return saved;
    }

    @Transactional
    public List<WEntity> saveAll(List<WEntity> entities) {
        entities.forEach(e -> {
            if (e.getCreatedAt() == null) {
                e.touchCreate();
            }
            e.touchUpdate();
        });
        List<WEntity> saved = repository.saveAll(entities);
        log.debug("Saved {} WEntity entities", saved.size());
        return saved;
    }

    /**
     * Update an entity.
     * Filters out instances and zones.
     */
    @Transactional
    public Optional<WEntity> update(WorldId worldId, String entityId, Consumer<WEntity> updater) {
        var lookupWorld = worldId.withoutInstanceAndZone();
        return repository.findByWorldIdAndEntityId(lookupWorld.getId(), entityId).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WEntity saved = repository.save(entity);
            log.debug("Updated WEntity: world={}, entityId={}", lookupWorld, entityId);
            return saved;
        });
    }

    /**
     * Delete an entity.
     * Filters out instances and zones.
     * IMPORTANT: Deletion is NOT allowed in branches - entities can only be deleted in main worlds.
     */
    @Transactional
    public boolean delete(WorldId worldId, String entityId) {
        var lookupWorld = worldId.withoutInstanceAndZone();

        // Prevent deletion in branches
        if (lookupWorld.isBranch()) {
            log.warn("Attempted to delete entity '{}' in branch world '{}' - not allowed", entityId, lookupWorld.getId());
            throw new IllegalArgumentException("Entities cannot be deleted in branches: " + lookupWorld.getId());
        }

        return repository.findByWorldIdAndEntityId(lookupWorld.getId(), entityId).map(entity -> {
            repository.delete(entity);
            log.debug("Deleted WEntity: world={}, entityId={}", lookupWorld, entityId);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean enable(WorldId worldId, String entityId) {
        return update(worldId, entityId, entity -> entity.setEnabled(true)).isPresent();
    }

    @Transactional
    public boolean disable(WorldId worldId, String entityId) {
        return update(worldId, entityId, entity -> entity.setEnabled(false)).isPresent();
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
