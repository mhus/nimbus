package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.BlockType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for managing WBlockType entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WBlockTypeService {

    private final WBlockTypeRepository repository;

    @Transactional(readOnly = true)
    public Optional<WBlockType> findByBlockId(String blockId) {
        return repository.findByBlockId(blockId);
    }

    @Transactional(readOnly = true)
    public List<WBlockType> findByRegionId(String regionId) {
        return repository.findByRegionId(regionId);
    }

    @Transactional(readOnly = true)
    public List<WBlockType> findByWorldId(String worldId) {
        return repository.findByWorldId(worldId);
    }

    @Transactional(readOnly = true)
    public List<WBlockType> findAllEnabled() {
        return repository.findByEnabled(true);
    }

    @Transactional
    public WBlockType save(String blockId, BlockType publicData, String regionId, String worldId) {
        if (blank(blockId)) {
            throw new IllegalArgumentException("blockId required");
        }
        if (publicData == null) {
            throw new IllegalArgumentException("publicData required");
        }

        WBlockType entity = repository.findByBlockId(blockId).orElseGet(() -> {
            WBlockType neu = WBlockType.builder()
                    .blockId(blockId)
                    .regionId(regionId)
                    .worldId(worldId)
                    .enabled(true)
                    .build();
            neu.touchCreate();
            log.debug("Creating new WBlockType: {}", blockId);
            return neu;
        });

        entity.setPublicData(publicData);
        entity.setRegionId(regionId);
        entity.setWorldId(worldId);
        entity.touchUpdate();

        WBlockType saved = repository.save(entity);
        log.debug("Saved WBlockType: {}", blockId);
        return saved;
    }

    @Transactional
    public List<WBlockType> saveAll(List<WBlockType> entities) {
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

    @Transactional
    public Optional<WBlockType> update(String blockId, Consumer<WBlockType> updater) {
        return repository.findByBlockId(blockId).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WBlockType saved = repository.save(entity);
            log.debug("Updated WBlockType: {}", blockId);
            return saved;
        });
    }

    @Transactional
    public boolean delete(String blockId) {
        return repository.findByBlockId(blockId).map(entity -> {
            repository.delete(entity);
            log.debug("Deleted WBlockType: {}", blockId);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean disable(String blockId) {
        return update(blockId, entity -> entity.setEnabled(false)).isPresent();
    }

    @Transactional
    public boolean enable(String blockId) {
        return update(blockId, entity -> entity.setEnabled(true)).isPresent();
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
