package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.ItemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for managing WItemType entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WItemTypeService {

    private final WItemTypeRepository repository;

    @Transactional(readOnly = true)
    public Optional<WItemType> findByItemType(String itemType) {
        return repository.findByItemType(itemType);
    }

    @Transactional(readOnly = true)
    public List<WItemType> findByRegionId(String regionId) {
        return repository.findByRegionId(regionId);
    }

    @Transactional(readOnly = true)
    public List<WItemType> findByWorldId(String worldId) {
        return repository.findByWorldId(worldId);
    }

    @Transactional(readOnly = true)
    public List<WItemType> findAllEnabled() {
        return repository.findByEnabled(true);
    }

    @Transactional
    public WItemType save(String itemType, ItemType publicData, String regionId, String worldId) {
        if (blank(itemType)) {
            throw new IllegalArgumentException("itemType required");
        }
        if (publicData == null) {
            throw new IllegalArgumentException("publicData required");
        }

        WItemType entity = repository.findByItemType(itemType).orElseGet(() -> {
            WItemType neu = WItemType.builder()
                    .itemType(itemType)
                    .regionId(regionId)
                    .worldId(worldId)
                    .enabled(true)
                    .build();
            neu.touchCreate();
            log.debug("Creating new WItemType: {}", itemType);
            return neu;
        });

        entity.setPublicData(publicData);
        entity.setRegionId(regionId);
        entity.setWorldId(worldId);
        entity.touchUpdate();

        WItemType saved = repository.save(entity);
        log.debug("Saved WItemType: {}", itemType);
        return saved;
    }

    @Transactional
    public List<WItemType> saveAll(List<WItemType> entities) {
        entities.forEach(e -> {
            if (e.getCreatedAt() == null) {
                e.touchCreate();
            }
            e.touchUpdate();
        });
        List<WItemType> saved = repository.saveAll(entities);
        log.debug("Saved {} WItemType entities", saved.size());
        return saved;
    }

    @Transactional
    public Optional<WItemType> update(String itemType, Consumer<WItemType> updater) {
        return repository.findByItemType(itemType).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WItemType saved = repository.save(entity);
            log.debug("Updated WItemType: {}", itemType);
            return saved;
        });
    }

    @Transactional
    public boolean delete(String itemType) {
        return repository.findByItemType(itemType).map(entity -> {
            repository.delete(entity);
            log.debug("Deleted WItemType: {}", itemType);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean disable(String itemType) {
        return update(itemType, entity -> entity.setEnabled(false)).isPresent();
    }

    @Transactional
    public boolean enable(String itemType) {
        return update(itemType, entity -> entity.setEnabled(true)).isPresent();
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
