package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.BlockType;
import de.mhus.nimbus.shared.types.WorldId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for managing WBlockType entities.
 * Block types are stored per main world (no instances, no zones).
 * Branches use COW (Copy On Write) - they can have their own block types, falling back to parent.
 * Block types cannot be deleted in branches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WBlockTypeService {

    private final WBlockTypeRepository repository;

    /**
     * Find block type by blockId with COW fallback for branches.
     * Instances and zones always look up in their main world.
     * Branches first check their own block types, then fall back to parent world.
     */
    @Transactional(readOnly = true)
    public Optional<WBlockType> findByBlockId(WorldId worldId, String blockId) {
        var lookupWorld = worldId.withoutInstanceAndZone();

        // Try branch first if this is a branch world
        if (lookupWorld.isBranch()) {
            var blockType = repository.findByWorldIdAndBlockId(lookupWorld.getId(), blockId);
            if (blockType.isPresent()) {
                return blockType;
            }
            // Fallback to parent world (COW)
            var parentWorld = lookupWorld.withoutBranchAndInstance();
            return repository.findByWorldIdAndBlockId(parentWorld.getId(), blockId);
        }

        return repository.findByWorldIdAndBlockId(lookupWorld.getId(), blockId);
    }

    /**
     * Find block types by group for specific world (no COW fallback for lists).
     * Filters out instances and zones.
     */
    @Transactional(readOnly = true)
    public List<WBlockType> findByBlockTypeGroup(WorldId worldId, String blockTypeGroup) {
        var lookupWorld = worldId.withoutInstanceAndZone();
        return repository.findByWorldIdAndBlockTypeGroup(lookupWorld.getId(), blockTypeGroup);
    }

    /**
     * Find all block types for specific world (no COW fallback for lists).
     * Filters out instances and zones.
     */
    @Transactional(readOnly = true)
    public List<WBlockType> findByWorldId(WorldId worldId) {
        var lookupWorld = worldId.withoutInstanceAndZone();
        return repository.findByWorldId(lookupWorld.getId());
    }

    /**
     * Find all enabled block types for specific world (no COW fallback for lists).
     * Filters out instances and zones.
     */
    @Transactional(readOnly = true)
    public List<WBlockType> findAllEnabled(WorldId worldId) {
        var lookupWorld = worldId.withoutInstanceAndZone();
        return repository.findByWorldIdAndEnabled(lookupWorld.getId(), true);
    }

    /**
     * Save or update a block type.
     * Filters out instances and zones - block types are stored per world.
     * Default blockTypeGroup is 'w' if not already set in publicData.
     */
    @Transactional
    public WBlockType save(WorldId worldId, String blockId, BlockType publicData) {
        if (blank(blockId)) {
            throw new IllegalArgumentException("blockId required");
        }
        if (publicData == null) {
            throw new IllegalArgumentException("publicData required");
        }

        var lookupWorld = worldId.withoutInstanceAndZone();

        WBlockType entity = repository.findByWorldIdAndBlockId(lookupWorld.getId(), blockId).orElseGet(() -> {
            WBlockType neu = WBlockType.builder()
                    .blockId(blockId)
                    .worldId(lookupWorld.getId())
                    .blockTypeGroup("w") // Default storage group
                    .enabled(true)
                    .build();
            neu.touchCreate();
            log.debug("Creating new WBlockType: {}", blockId);
            return neu;
        });

        entity.setPublicData(publicData);
        entity.touchUpdate();

        WBlockType saved = repository.save(entity);
        log.debug("Saved WBlockType: {}", blockId);
        return saved;
    }

    @Transactional
    public List<WBlockType> saveAll(WorldId worldId, List<WBlockType> entities) {
        entities.forEach(e -> {
            if (e.getCreatedAt() == null) {
                e.touchCreate();
            }
            e.touchUpdate();
        });
        List<WBlockType> saved = repository.saveAll(entities);
        log.debug("Saved {} WBlockType entities", saved.size());
        return saved;
    }

    /**
     * Update a block type.
     * Filters out instances and zones.
     */
    @Transactional
    public Optional<WBlockType> update(WorldId worldId, String blockId, Consumer<WBlockType> updater) {
        var lookupWorld = worldId.withoutInstanceAndZone();
        return repository.findByWorldIdAndBlockId(lookupWorld.getId(), blockId).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WBlockType saved = repository.save(entity);
            log.debug("Updated WBlockType: {}", blockId);
            return saved;
        });
    }

    /**
     * Delete a block type.
     * Filters out instances and zones.
     * IMPORTANT: Deletion is NOT allowed in branches - block types can only be deleted in main worlds.
     */
    @Transactional
    public boolean delete(WorldId worldId, String blockId) {
        var lookupWorld = worldId.withoutInstanceAndZone();

        // Prevent deletion in branches
        if (lookupWorld.isBranch()) {
            log.warn("Attempted to delete block type '{}' in branch world '{}' - not allowed", blockId, lookupWorld.getId());
            throw new IllegalArgumentException("Block types cannot be deleted in branches: " + lookupWorld.getId());
        }

        return repository.findByWorldIdAndBlockId(lookupWorld.getId(), blockId).map(entity -> {
            repository.delete(entity);
            log.debug("Deleted WBlockType: {}", blockId);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean disable(WorldId worldId, String blockId) {
        return update(worldId, blockId, entity -> entity.setEnabled(false)).isPresent();
    }

    @Transactional
    public boolean enable(WorldId worldId, String blockId) {
        return update(worldId, blockId, entity -> entity.setEnabled(true)).isPresent();
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
