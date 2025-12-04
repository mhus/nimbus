package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.Item;
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
    public List<WItem> findByWorldId(String worldId) {
        return repository.findByWorldId(worldId);
    }

    /**
     * Find all enabled items for a specific world.
     */
    @Transactional(readOnly = true)
    public List<WItem> findEnabledByWorldId(String worldId) {
        return repository.findByWorldIdAndEnabled(worldId, true);
    }

    /**
     * Find item by ID.
     */
    @Transactional(readOnly = true)
    public Optional<WItem> findById(String id) {
        return repository.findById(id);
    }

    /**
     * Find item by worldId and itemId.
     */
    @Transactional(readOnly = true)
    public Optional<WItem> findByItemId(String worldId, String itemId) {
        return repository.findByWorldIdAndItemId(worldId, itemId);
    }

    /**
     * Save a new item or update existing.
     */
    @Transactional
    public WItem save(String worldId, String itemId, Item publicData, String regionId) {
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("worldId is required");
        }
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is required");
        }

        Optional<WItem> existing = repository.findByWorldIdAndItemId(worldId, itemId);
        if (existing.isPresent()) {
            WItem item = existing.get();
            item.setPublicData(publicData);
            item.setRegionId(regionId);
            item.touchUpdate();
            log.debug("Updated item: worldId={}, itemId={}", worldId, itemId);
            return repository.save(item);
        }

        WItem item = WItem.builder()
                .worldId(worldId)
                .regionId(regionId)
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
    public Optional<WItem> update(String id, Item publicData) {
        return repository.findById(id).map(item -> {
            item.setPublicData(publicData);
            item.touchUpdate();
            log.debug("Updated item publicData: id={}", id);
            return repository.save(item);
        });
    }

    /**
     * Disable (soft delete) an item.
     */
    @Transactional
    public void disable(String id) {
        repository.findById(id).ifPresent(item -> {
            if (!item.isEnabled()) return;
            item.setEnabled(false);
            item.touchUpdate();
            repository.save(item);
            log.debug("Disabled item: id={}", id);
        });
    }

    /**
     * Hard delete an item.
     */
    @Transactional
    public void delete(String id) {
        repository.findById(id).ifPresent(item -> {
            repository.delete(item);
            log.debug("Deleted item: id={}, itemId={}", id, item.getItemId());
        });
    }

    /**
     * Save all items (batch operation for import).
     */
    @Transactional
    public List<WItem> saveAll(List<WItem> items) {
        return repository.saveAll(items);
    }
}
