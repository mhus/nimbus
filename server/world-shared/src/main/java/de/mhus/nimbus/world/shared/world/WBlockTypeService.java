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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WBlockTypeService {

    private final WBlockTypeRepository repository;

    @Transactional(readOnly = true)
    public Optional<WBlockType> findByBlockId(WorldId worldId, String blockId) {
        return repository.findByWorldIdAndBlockId(worldId.getId(), blockId);
    }

    @Transactional(readOnly = true)
    public List<WBlockType> findByBlockTypeGroup(WorldId worldId, String blockTypeGroup) {
        return repository.findByWorldIdAndBlockTypeGroup(worldId.getId(), blockTypeGroup);
    }

    @Transactional(readOnly = true)
    public List<WBlockType> findByWorldId(WorldId worldId) {
        return repository.findByWorldId(worldId.getId());
    }

    @Transactional(readOnly = true)
    public List<WBlockType> findAllEnabled(WorldId worldId) {
        return repository.findByWorldIdAndEnabled(worldId.getId(), true);
    }

    @Transactional
    public WBlockType save(WorldId worldId, String blockId, BlockType publicData) {
        if (blank(blockId)) {
            throw new IllegalArgumentException("blockId required");
        }
        if (publicData == null) {
            throw new IllegalArgumentException("publicData required");
        }

        WBlockType entity = repository.findByWorldIdAndBlockId(worldId.getId(), blockId).orElseGet(() -> {
            WBlockType neu = WBlockType.builder()
                    .blockId(blockId)
                    .worldId(worldId.getId())
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

    @Transactional
    public Optional<WBlockType> update(WorldId worldId, String blockId, Consumer<WBlockType> updater) {
        return repository.findByWorldIdAndBlockId(worldId.getId(), blockId).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WBlockType saved = repository.save(entity);
            log.debug("Updated WBlockType: {}", blockId);
            return saved;
        });
    }

    @Transactional
    public boolean delete(WorldId worldId, String blockId) {
        return repository.findByWorldIdAndBlockId(worldId.getId(), blockId).map(entity -> {
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
