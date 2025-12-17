package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.ItemBlockRef;
import de.mhus.nimbus.generated.types.Vector3;
import de.mhus.nimbus.shared.types.WorldId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing item positions in the world.
 * Items are stored per chunk for efficient spatial queries.
 *
 * Item positions exist separately for each world/zone/branch/instance.
 * Each world context is treated as a separate instance.
 * COW (Copy On Write) on save for branches - items saved in a branch stay in that branch.
 * No storage functionality supported (always world-instance-specific).
 * List loading does NOT fall back to main world.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WItemPositionService {

    private final WItemPositionRepository repository;
    private final WWorldService worldService;

    /**
     * Save or update an item position.
     * Automatically calculates chunk key from item position.
     * COW for branches: Items are saved directly to the branch (no parent modification).
     *
     * @param worldId World identifier (can be main world, branch, instance, or zone)
     * @param itemBlockRef ItemBlockRef containing position and display data
     * @return Saved item position entity
     */
    @Transactional
    public WItemPosition saveItemPosition(WorldId worldId, ItemBlockRef itemBlockRef) {
        if (worldId == null) {
            throw new IllegalArgumentException("worldId required");
        }
        if (itemBlockRef == null) {
            throw new IllegalArgumentException("itemBlockRef required");
        }
        if (itemBlockRef.getId() == null || itemBlockRef.getId().isBlank()) {
            throw new IllegalArgumentException("itemBlockRef.id required");
        }
        if (itemBlockRef.getPosition() == null) {
            throw new IllegalArgumentException("itemBlockRef.position required");
        }

        String itemId = itemBlockRef.getId();
        Vector3 position = itemBlockRef.getPosition();
        WWorld world = worldService.getByWorldId(worldId.getId()).get();
        String chunk = world.getChunkKey((int)position.getX(), (int)position.getZ());

        WItemPosition itemPosition = repository.findByWorldIdAndItemId(worldId.getId(), itemId)
                .orElseGet(() -> {
                    WItemPosition neu = WItemPosition.builder()
                            .worldId(worldId.getId())
                            .itemId(itemId)
                            .chunk(chunk)
                            .enabled(true)
                            .build();
                    neu.touchCreate();
                    log.debug("Creating new item position: world={}, itemId={}, chunk={}",
                            worldId, itemId, chunk);
                    return neu;
                });

        itemPosition.setPublicData(itemBlockRef);
        itemPosition.setChunk(chunk);
        itemPosition.touchUpdate();

        WItemPosition saved = repository.save(itemPosition);
        log.debug("Saved item position: world={}, itemId={}, chunk={}",
                worldId, itemId, chunk);
        return saved;
    }

    /**
     * Get all items in a specific chunk.
     * Returns only enabled items.
     * No fallback to parent world - returns only items in this specific world context.
     *
     * @param worldId World identifier (can be main world, branch, instance, or zone)
     * @param cx Chunk X coordinate
     * @param cz Chunk Z coordinate
     * @return List of ItemBlockRef objects for the chunk
     */
    @Transactional(readOnly = true)
    public List<ItemBlockRef> getItemsInChunk(WorldId worldId, int cx, int cz) {
        WWorld world = worldService.getByWorldId(worldId.getId()).get();
        String chunk = BlockUtil.toChunkKey(cx, cz);
        List<WItemPosition> positions = repository.findByWorldIdAndChunkAndEnabled(
                worldId.getId(), chunk, true);

        return positions.stream()
                .map(WItemPosition::getPublicData)
                .filter(data -> data != null)
                .toList();
    }

    /**
     * Get all items in a world.
     * Returns only enabled items.
     * No fallback to parent world - returns only items in this specific world context.
     *
     * @param worldId World identifier (can be main world, branch, instance, or zone)
     * @return List of all item positions
     */
    @Transactional(readOnly = true)
    public List<WItemPosition> getAllItems(WorldId worldId) {
        return repository.findByWorldId(worldId.getId());
    }

    /**
     * Find a specific item by ID with COW fallback for branches.
     * If this is a branch world and item is not found, falls back to parent world (COW).
     * For instances/zones, no fallback is performed.
     *
     * @param worldId World identifier (can be main world, branch, instance, or zone)
     * @param itemId Item identifier
     * @return Optional containing the item position if found
     */
    @Transactional(readOnly = true)
    public Optional<WItemPosition> findItem(WorldId worldId, String itemId) {
        // Try branch first if this is a branch world
        if (worldId.isBranch()) {
            var itemPosition = repository.findByWorldIdAndItemId(worldId.getId(), itemId);
            if (itemPosition.isPresent()) {
                return itemPosition;
            }
            // Fallback to parent world (COW)
            var parentWorld = worldId.withoutBranchAndInstance();
            return repository.findByWorldIdAndItemId(parentWorld.getId(), itemId);
        }

        return repository.findByWorldIdAndItemId(worldId.getId(), itemId);
    }

    /**
     * Delete an item position.
     * Performs soft delete by setting enabled=false.
     * IMPORTANT: Deletion is NOT allowed in branches - item positions can only be deleted in main worlds.
     *
     * @param worldId World identifier (can be main world, branch, instance, or zone)
     * @param itemId Item identifier
     * @return True if item was found and disabled
     * @throws IllegalArgumentException if attempting to delete in a branch world
     */
    @Transactional
    public boolean deleteItemPosition(WorldId worldId, String itemId) {
        // Prevent deletion in branches
        if (worldId.isBranch()) {
            log.warn("Attempted to delete item position '{}' in branch world '{}' - not allowed", itemId, worldId.getId());
            throw new IllegalArgumentException("Item positions cannot be deleted in branches: " + worldId.getId());
        }

        Optional<WItemPosition> itemOpt = repository.findByWorldIdAndItemId(worldId.getId(), itemId);

        if (itemOpt.isEmpty()) {
            log.debug("Item not found for deletion: world={}, itemId={}",
                    worldId, itemId);
            return false;
        }

        WItemPosition item = itemOpt.get();
        item.setEnabled(false);
        item.touchUpdate();
        repository.save(item);

        log.info("Soft deleted item: world={}, itemId={}",
                worldId, itemId);
        return true;
    }

    /**
     * Permanently delete an item position.
     * IMPORTANT: Deletion is NOT allowed in branches - item positions can only be deleted in main worlds.
     *
     * @param worldId World identifier (can be main world, branch, instance, or zone)
     * @param itemId Item identifier
     * @throws IllegalArgumentException if attempting to delete in a branch world
     */
    @Transactional
    public void hardDeleteItemPosition(WorldId worldId, String itemId) {
        // Prevent deletion in branches
        if (worldId.isBranch()) {
            log.warn("Attempted to hard delete item position '{}' in branch world '{}' - not allowed", itemId, worldId.getId());
            throw new IllegalArgumentException("Item positions cannot be deleted in branches: " + worldId.getId());
        }

        repository.deleteByWorldIdAndItemId(worldId.getId(), itemId);
        log.info("Hard deleted item: world={}, itemId={}",
                worldId, itemId);
    }

    /**
     * Save multiple item positions in batch.
     *
     * @param items List of item positions to save
     * @return List of saved item positions
     */
    @Transactional
    public List<WItemPosition> saveAll(WorldId worldId, List<WItemPosition> items) {
        items.forEach(item -> {
            if (item.getCreatedAt() == null) {
                item.touchCreate();
            }
            item.touchUpdate();
        });

        List<WItemPosition> saved = repository.saveAll(items);
        log.debug("Saved {} item positions", saved.size());
        return saved;
    }

    /**
     * Count items in a chunk.
     * Counts only items in this specific world context (no parent fallback).
     *
     * @param worldId World identifier (can be main world, branch, instance, or zone)
     * @param cx Chunk X coordinate
     * @param cz Chunk Z coordinate
     * @return Number of items in the chunk
     */
    @Transactional(readOnly = true)
    public long countItemsInChunk(WorldId worldId, int cx, int cz) {
        String chunk = BlockUtil.toChunkKey(cx, cz);
        return repository.findByWorldIdAndChunkAndEnabled(
                worldId.getId(), chunk, true).size();
    }
}
