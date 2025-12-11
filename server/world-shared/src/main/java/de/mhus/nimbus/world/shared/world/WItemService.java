package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.Item;
import de.mhus.nimbus.shared.types.WorldId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing WItem entities (inventory/template items without position).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WItemService {

    private final WItemRepository repository;

    /**
     * Find all items for a specific world.
     */
    @Transactional(readOnly = true)
    public List<WItem> findByWorldId(WorldId worldId) {
        return repository.findByWorldId(worldId.getId());
    }

    /**
     * Find all enabled items for a specific world.
     */
    @Transactional(readOnly = true)
    public List<WItem> findEnabledByWorldId(WorldId worldId) {
        return repository.findByWorldIdAndEnabled(worldId.getId(), true);
    }

    /**
     * Find item by worldId and itemId.
     */
    @Transactional(readOnly = true)
    public Optional<WItem> findByItemId(WorldId worldId, String itemId) {
        return repository.findByWorldIdAndItemId(worldId.getId(), itemId);
    }

    /**
     * Save a new item or update existing.
     */
    @Transactional
    public WItem save(WorldId worldId, String itemId, Item publicData) {
        if (worldId == null) {
            throw new IllegalArgumentException("worldId is required");
        }
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is required");
        }

        Optional<WItem> existing = repository.findByWorldIdAndItemId(worldId.getId(), itemId);
        if (existing.isPresent()) {
            WItem item = existing.get();
            item.setPublicData(publicData);
            item.touchUpdate();
            log.debug("Updated item: worldId={}, itemId={}", worldId, itemId);
            return repository.save(item);
        }

        WItem item = WItem.builder()
                .worldId(worldId.getId())
                .itemId(itemId)
                .publicData(publicData)
                .enabled(true)
                .build();
        item.touchCreate();

        log.debug("Created item: worldId={}, itemId={}", worldId, itemId);
        return repository.save(item);
    }

    /**
     * Update item publicData.
     */
    @Transactional
    public Optional<WItem> update(WorldId worldId, String itemId, Item publicData) {
        return repository.findByWorldIdAndItemId(worldId.getId(), itemId).map(item -> {
            item.setPublicData(publicData);
            item.touchUpdate();
            log.debug("Updated item publicData: worldId={}, itemId={}", worldId, itemId);
            return repository.save(item);
        });
    }

    /**
     * Disable (soft delete) an item.
     */
    @Transactional
    public boolean disable(WorldId worldId, String itemId) {
        return repository.findByWorldIdAndItemId(worldId.getId(), itemId).map(item -> {
            if (!item.isEnabled()) return false;
            item.setEnabled(false);
            item.touchUpdate();
            repository.save(item);
            log.debug("Disabled item: worldId={}, itemId={}", worldId, itemId);
            return true;
        }).orElse(false);
    }

    /**
     * Hard delete an item.
     */
    @Transactional
    public boolean delete(WorldId worldId, String itemId) {
        return repository.findByWorldIdAndItemId(worldId.getId(), itemId).map(item -> {
            repository.delete(item);
            log.debug("Deleted item: worldId={}, itemId={}", worldId, itemId);
            return true;
        }).orElse(false);
    }

    /**
     * Save all items (batch operation for import).
     */
    @Transactional
    public List<WItem> saveAll(WorldId worldId, List<WItem> items) {
        return repository.saveAll(items);
    }
}
