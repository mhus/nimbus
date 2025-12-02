package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.Entity;
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
    public Optional<WEntity> findByWorldIdAndEntityId(String worldId, String entityId) {
        return repository.findByWorldIdAndEntityId(worldId, entityId);
    }

    @Transactional(readOnly = true)
    public List<WEntity> findByWorldId(String worldId) {
        return repository.findByWorldId(worldId);
    }

    @Transactional(readOnly = true)
    public List<WEntity> findByWorldIdAndChunk(String worldId, String chunk) {
        return repository.findByWorldIdAndChunk(worldId, chunk);
    }

    @Transactional(readOnly = true)
    public List<WEntity> findByModelId(String modelId) {
        return repository.findByModelId(modelId);
    }

    @Transactional(readOnly = true)
    public List<WEntity> findAllEnabled() {
        return repository.findByEnabled(true);
    }

    @Transactional
    public WEntity save(String worldId, String entityId, Entity publicData, String chunk, String modelId) {
        if (blank(worldId)) {
            throw new IllegalArgumentException("worldId required");
        }
        if (blank(entityId)) {
            throw new IllegalArgumentException("entityId required");
        }
        if (publicData == null) {
            throw new IllegalArgumentException("publicData required");
        }

        WEntity entity = repository.findByWorldIdAndEntityId(worldId, entityId).orElseGet(() -> {
            WEntity neu = WEntity.builder()
                    .worldId(worldId)
                    .entityId(entityId)
                    .chunk(chunk)
                    .modelId(modelId)
                    .enabled(true)
                    .build();
            neu.touchCreate();
            log.debug("Creating new WEntity: world={}, entityId={}", worldId, entityId);
            return neu;
        });

        entity.setPublicData(publicData);
        entity.setChunk(chunk);
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
    public Optional<WEntity> update(String worldId, String entityId, Consumer<WEntity> updater) {
        return repository.findByWorldIdAndEntityId(worldId, entityId).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WEntity saved = repository.save(entity);
            log.debug("Updated WEntity: world={}, entityId={}", worldId, entityId);
            return saved;
        });
    }

    @Transactional
    public boolean delete(String worldId, String entityId) {
        return repository.findByWorldIdAndEntityId(worldId, entityId).map(entity -> {
            repository.delete(entity);
            log.debug("Deleted WEntity: world={}, entityId={}", worldId, entityId);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean disable(String worldId, String entityId) {
        return update(worldId, entityId, entity -> entity.setEnabled(false)).isPresent();
    }

    @Transactional
    public boolean enable(String worldId, String entityId) {
        return update(worldId, entityId, entity -> entity.setEnabled(true)).isPresent();
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
