package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.EntityModel;
import de.mhus.nimbus.shared.types.WorldId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for managing WEntityModel entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WEntityModelService {

    private final WEntityModelRepository repository;

    @Transactional(readOnly = true)
    public Optional<WEntityModel> findByModelId(WorldId worldId, String modelId) {
        return repository.findByWorldIdAndModelId(worldId.getId(), modelId);
    }

    @Transactional(readOnly = true)
    public List<WEntityModel> findByWorldId(WorldId worldId) {
        return repository.findByWorldId(worldId.getId());
    }

    @Transactional(readOnly = true)
    public List<WEntityModel> findAllEnabled(WorldId worldId) {
        return repository.findByWorldIdAndEnabled(worldId.getId(), true);
    }

    @Transactional
    public WEntityModel save(WorldId worldId, String modelId, EntityModel publicData) {
        if (modelId == null) {
            throw new IllegalArgumentException("modelId required");
        }
        if (publicData == null) {
            throw new IllegalArgumentException("publicData required");
        }

        WEntityModel entity = repository.findByWorldIdAndModelId(worldId.getId(), modelId).orElseGet(() -> {
            WEntityModel neu = WEntityModel.builder()
                    .modelId(modelId)
                    .worldId(worldId.getId())
                    .enabled(true)
                    .build();
            neu.touchCreate();
            log.debug("Creating new WEntityModel: {}", modelId);
            return neu;
        });

        entity.setPublicData(publicData);
        entity.touchUpdate();

        WEntityModel saved = repository.save(entity);
        log.debug("Saved WEntityModel: {}", modelId);
        return saved;
    }

    @Transactional
    public List<WEntityModel> saveAll(WorldId worldId, List<WEntityModel> entities) {
        entities.forEach(e -> {
            if (e.getCreatedAt() == null) {
                e.touchCreate();
            }
            e.touchUpdate();
        });
        List<WEntityModel> saved = repository.saveAll(entities);
        log.debug("Saved {} WEntityModel entities", saved.size());
        return saved;
    }

    @Transactional
    public Optional<WEntityModel> update(WorldId worldId, String modelId, Consumer<WEntityModel> updater) {
        return repository.findByWorldIdAndModelId(worldId.getId(), modelId).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WEntityModel saved = repository.save(entity);
            log.debug("Updated WEntityModel: {}", modelId);
            return saved;
        });
    }

    @Transactional
    public boolean delete(WorldId worldId, String modelId) {
        return repository.findByWorldIdAndModelId(worldId.getId(), modelId).map(entity -> {
            repository.delete(entity);
            log.debug("Deleted WEntityModel: {}", modelId);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean disable(WorldId worldId, String modelId) {
        return update(worldId, modelId, entity -> entity.setEnabled(false)).isPresent();
    }

    @Transactional
    public boolean enable(WorldId worldId, String modelId) {
        return update(worldId, modelId, entity -> entity.setEnabled(true)).isPresent();
    }

}
