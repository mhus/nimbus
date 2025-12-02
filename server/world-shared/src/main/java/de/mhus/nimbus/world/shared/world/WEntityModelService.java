package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.EntityModel;
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
    public Optional<WEntityModel> findByModelId(String modelId) {
        return repository.findByModelId(modelId);
    }

    @Transactional(readOnly = true)
    public List<WEntityModel> findByRegionId(String regionId) {
        return repository.findByRegionId(regionId);
    }

    @Transactional(readOnly = true)
    public List<WEntityModel> findByWorldId(String worldId) {
        return repository.findByWorldId(worldId);
    }

    @Transactional(readOnly = true)
    public List<WEntityModel> findAllEnabled() {
        return repository.findByEnabled(true);
    }

    @Transactional
    public WEntityModel save(String modelId, EntityModel publicData, String regionId, String worldId) {
        if (blank(modelId)) {
            throw new IllegalArgumentException("modelId required");
        }
        if (publicData == null) {
            throw new IllegalArgumentException("publicData required");
        }

        WEntityModel entity = repository.findByModelId(modelId).orElseGet(() -> {
            WEntityModel neu = WEntityModel.builder()
                    .modelId(modelId)
                    .regionId(regionId)
                    .worldId(worldId)
                    .enabled(true)
                    .build();
            neu.touchCreate();
            log.debug("Creating new WEntityModel: {}", modelId);
            return neu;
        });

        entity.setPublicData(publicData);
        entity.setRegionId(regionId);
        entity.setWorldId(worldId);
        entity.touchUpdate();

        WEntityModel saved = repository.save(entity);
        log.debug("Saved WEntityModel: {}", modelId);
        return saved;
    }

    @Transactional
    public List<WEntityModel> saveAll(List<WEntityModel> entities) {
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
    public Optional<WEntityModel> update(String modelId, Consumer<WEntityModel> updater) {
        return repository.findByModelId(modelId).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WEntityModel saved = repository.save(entity);
            log.debug("Updated WEntityModel: {}", modelId);
            return saved;
        });
    }

    @Transactional
    public boolean delete(String modelId) {
        return repository.findByModelId(modelId).map(entity -> {
            repository.delete(entity);
            log.debug("Deleted WEntityModel: {}", modelId);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean disable(String modelId) {
        return update(modelId, entity -> entity.setEnabled(false)).isPresent();
    }

    @Transactional
    public boolean enable(String modelId) {
        return update(modelId, entity -> entity.setEnabled(true)).isPresent();
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
