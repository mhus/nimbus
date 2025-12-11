package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.ItemType;
import de.mhus.nimbus.shared.types.WorldId;
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
    public Optional<WItemType> findByItemType(WorldId worldId, String itemType) {
        return repository.findByWorldIdAndItemType(worldId.getId(), itemType);
    }

    @Transactional(readOnly = true)
    public List<WItemType> findByWorldId(WorldId worldId) {
        return repository.findByWorldId(worldId.getId());
    }

    @Transactional(readOnly = true)
    public List<WItemType> findAllEnabled(WorldId worldId) {
        return repository.findByWorldIdAndEnabled(worldId.getId(), true);
    }

    @Transactional
    public WItemType save(WorldId worldId, String itemType, ItemType publicData) {
        if (blank(itemType)) {
            throw new IllegalArgumentException("itemType required");
        }
        if (publicData == null) {
            throw new IllegalArgumentException("publicData required");
        }

        WItemType entity = repository.findByWorldIdAndItemType(worldId.getId(), itemType).orElseGet(() -> {
            WItemType neu = WItemType.builder()
                    .itemType(itemType)
                    .worldId(worldId.getId())
                    .enabled(true)
                    .build();
            neu.touchCreate();
            log.debug("Creating new WItemType: {}", itemType);
            return neu;
        });

        entity.setPublicData(publicData);
        entity.touchUpdate();

        WItemType saved = repository.save(entity);
        log.debug("Saved WItemType: {}", itemType);
        return saved;
    }

    @Transactional
    public List<WItemType> saveAll(WorldId worldId, List<WItemType> entities) {
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
    public Optional<WItemType> update(WorldId worldId, String itemType, Consumer<WItemType> updater) {
        return repository.findByWorldIdAndItemType(worldId.getId(), itemType).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WItemType saved = repository.save(entity);
            log.debug("Updated WItemType: {}", itemType);
            return saved;
        });
    }

    @Transactional
    public boolean delete(WorldId worldId, String itemType) {
        return repository.findByWorldIdAndItemType(worldId.getId(), itemType).map(entity -> {
            repository.delete(entity);
            log.debug("Deleted WItemType: {}", itemType);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean disable(WorldId worldId, String itemType) {
        return update(worldId, itemType, entity -> entity.setEnabled(false)).isPresent();
    }

    @Transactional
    public boolean enable(WorldId worldId, String itemType) {
        return update(worldId, itemType, entity -> entity.setEnabled(true)).isPresent();
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
