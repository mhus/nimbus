package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.Backdrop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for managing WBackdrop entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WBackdropService {

    private final WBackdropRepository repository;

    @Transactional(readOnly = true)
    public Optional<WBackdrop> findByBackdropId(String backdropId) {
        return repository.findByBackdropId(backdropId);
    }

    @Transactional(readOnly = true)
    public List<WBackdrop> findByRegionId(String regionId) {
        return repository.findByRegionId(regionId);
    }

    @Transactional(readOnly = true)
    public List<WBackdrop> findByWorldId(String worldId) {
        return repository.findByWorldId(worldId);
    }

    @Transactional(readOnly = true)
    public List<WBackdrop> findAllEnabled() {
        return repository.findByEnabled(true);
    }

    @Transactional
    public WBackdrop save(String backdropId, Backdrop publicData, String regionId, String worldId) {
        if (blank(backdropId)) {
            throw new IllegalArgumentException("backdropId required");
        }
        if (publicData == null) {
            throw new IllegalArgumentException("publicData required");
        }

        WBackdrop entity = repository.findByBackdropId(backdropId).orElseGet(() -> {
            WBackdrop neu = WBackdrop.builder()
                    .backdropId(backdropId)
                    .regionId(regionId)
                    .worldId(worldId)
                    .enabled(true)
                    .build();
            neu.touchCreate();
            log.debug("Creating new WBackdrop: {}", backdropId);
            return neu;
        });

        entity.setPublicData(publicData);
        entity.setRegionId(regionId);
        entity.setWorldId(worldId);
        entity.touchUpdate();

        WBackdrop saved = repository.save(entity);
        log.debug("Saved WBackdrop: {}", backdropId);
        return saved;
    }

    @Transactional
    public List<WBackdrop> saveAll(List<WBackdrop> entities) {
        entities.forEach(e -> {
            if (e.getCreatedAt() == null) {
                e.touchCreate();
            }
            e.touchUpdate();
        });
        List<WBackdrop> saved = repository.saveAll(entities);
        log.debug("Saved {} WBackdrop entities", saved.size());
        return saved;
    }

    @Transactional
    public Optional<WBackdrop> update(String backdropId, Consumer<WBackdrop> updater) {
        return repository.findByBackdropId(backdropId).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WBackdrop saved = repository.save(entity);
            log.debug("Updated WBackdrop: {}", backdropId);
            return saved;
        });
    }

    @Transactional
    public boolean delete(String backdropId) {
        return repository.findByBackdropId(backdropId).map(entity -> {
            repository.delete(entity);
            log.debug("Deleted WBackdrop: {}", backdropId);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean disable(String backdropId) {
        return update(backdropId, entity -> entity.setEnabled(false)).isPresent();
    }

    @Transactional
    public boolean enable(String backdropId) {
        return update(backdropId, entity -> entity.setEnabled(true)).isPresent();
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
