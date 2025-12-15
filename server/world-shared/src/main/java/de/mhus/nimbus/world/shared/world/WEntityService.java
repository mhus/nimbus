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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WEntityService {

    private final WEntityRepository repository;

    @Transactional(readOnly = true)
    public Optional<WEntity> findByWorldIdAndEntityId(WorldId worldId, String entityId) {
        return repository.findByWorldIdAndEntityId(worldId.getId(), entityId);
    }

    @Transactional(readOnly = true)
    public List<WEntity> findByWorldId(WorldId worldId) {
        return repository.findByWorldId(worldId.getId());
    }

    @Transactional(readOnly = true)
    public List<WEntity> findByModelId(WorldId worldId, String modelId) {
        return repository.findByWorldIdAndModelId(worldId.getId(), modelId);
    }

    @Transactional(readOnly = true)
    public List<WEntity> findAllEnabled(WorldId worldId) {
        return repository.findByWorldIdAndEnabled(worldId.getId(), true);
    }

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

        WEntity entity = repository.findByWorldIdAndEntityId(worldId.getId(), entityId).orElseGet(() -> {
            WEntity neu = WEntity.builder()
                    .worldId(worldId.getId())
                    .entityId(entityId)
                    .modelId(modelId)
                    .enabled(true)
                    .build();
            neu.touchCreate();
            log.debug("Creating new WEntity: world={}, entityId={}", worldId, entityId);
            return neu;
        });

        entity.setPublicData(publicData);
        entity.setModelId(modelId);
        entity.touchUpdate();

        WEntity saved = repository.save(entity);
        log.debug("Saved WEntity: world={}, entityId={}", worldId, entityId);
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

    @Transactional
    public Optional<WEntity> update(WorldId worldId, String entityId, Consumer<WEntity> updater) {
        return repository.findByWorldIdAndEntityId(worldId.getId(), entityId).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WEntity saved = repository.save(entity);
            log.debug("Updated WEntity: world={}, entityId={}", worldId, entityId);
            return saved;
        });
    }

    @Transactional
    public boolean delete(WorldId worldId, String entityId) {
        return repository.findByWorldIdAndEntityId(worldId.getId(), entityId).map(entity -> {
            repository.delete(entity);
            log.debug("Deleted WEntity: world={}, entityId={}", worldId, entityId);
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
