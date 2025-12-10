package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.ItemBlockRef;
import de.mhus.nimbus.generated.types.Vector3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing item positions in the world.
 * Items are stored per chunk for efficient spatial queries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WItemRegistryService {

    private final WItemPositionRepository repository;

    /**
     * Calculate chunk key from world position.
     *
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Chunk key in format "cx:cz" (e.g., "0:0", "-1:2")
     */
    public static String calculateChunkKey(double worldX, double worldZ) {
        int cx = (int) Math.floor(worldX / 16.0);
        int cz = (int) Math.floor(worldZ / 16.0);
        return cx + ":" + cz;
    }

    /**
     * Calculate chunk key from chunk coordinates.
     *
     * @param cx Chunk X coordinate
     * @param cz Chunk Z coordinate
     * @return Chunk key in format "cx:cz"
     */
    public static String toChunkKey(int cx, int cz) {
        return cx + ":" + cz;
    }

    /**
     * Save or update an item position.
     * Automatically calculates chunk key from item position.
     *
     * @param worldId World identifier
     * @param itemBlockRef ItemBlockRef containing position and display data
     * @return Saved item position entity
     */
    @Transactional
    public WItemPosition saveItemPosition(String worldId, ItemBlockRef itemBlockRef) {
        if (worldId == null || worldId.isBlank()) {
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
        String chunk = calculateChunkKey(position.getX(), position.getZ());

        WItemPosition itemPosition = repository.findByWorldIdAndItemId(worldId, itemId)
                .orElseGet(() -> {
                    WItemPosition neu = WItemPosition.builder()
                            .worldId(worldId)
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
     *
     * @param worldId World identifier
     * @param cx Chunk X coordinate
     * @param cz Chunk Z coordinate
     * @return List of ItemBlockRef objects for the chunk
     */
    @Transactional(readOnly = true)
    public List<ItemBlockRef> getItemsInChunk(String worldId, int cx, int cz) {
        String chunk = toChunkKey(cx, cz);
        List<WItemPosition> positions = repository.findByWorldIdAndChunkAndEnabled(
                worldId, chunk, true);

        return positions.stream()
                .map(WItemPosition::getPublicData)
                .filter(data -> data != null)
                .toList();
    }

    /**
     * Get all items in a world.
     * Returns only enabled items.
     *
     * @param worldId World identifier
     * @return List of all item positions
     */
    @Transactional(readOnly = true)
    public List<WItemPosition> getAllItems(String worldId) {
        return repository.findByWorldId(worldId);
    }

    /**
     * Find a specific item by ID.
     *
     * @param worldId World identifier
     * @param itemId Item identifier
     * @return Optional containing the item position if found
     */
    @Transactional(readOnly = true)
    public Optional<WItemPosition> findItem(String worldId, String itemId) {
        return repository.findByWorldIdAndItemId(worldId, itemId);
    }

    /**
     * Delete an item position.
     * Performs soft delete by setting enabled=false.
     *
     * @param worldId World identifier
     * @param itemId Item identifier
     * @return True if item was found and disabled
     */
    @Transactional
    public boolean deleteItemPosition(String worldId, String itemId) {
        Optional<WItemPosition> itemOpt = repository.findByWorldIdAndItemId(worldId, itemId);

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
     *
     * @param worldId World identifier
     * @param itemId Item identifier
     */
    @Transactional
    public void hardDeleteItemPosition(String worldId, String itemId) {
        repository.deleteByWorldIdAndItemId(worldId, itemId);
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
    public List<WItemPosition> saveAll(List<WItemPosition> items) {
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
     *
     * @param worldId World identifier
     * @param cx Chunk X coordinate
     * @param cz Chunk Z coordinate
     * @return Number of items in the chunk
     */
    @Transactional(readOnly = true)
    public long countItemsInChunk(String worldId, int cx, int cz) {
        String chunk = toChunkKey(cx, cz);
        return repository.findByWorldIdAndChunkAndEnabled(
                worldId, chunk, true).size();
    }
}
