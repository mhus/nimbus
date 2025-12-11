package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.Backdrop;
import de.mhus.nimbus.shared.types.WorldId;
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
    public Optional<WBackdrop> findByBackdropId(WorldId worldId, String backdropId) {
        return repository.findByWorldIdAndBackdropId(worldId.getId(), backdropId);
    }

    @Transactional(readOnly = true)
    public List<WBackdrop> findByWorldId(WorldId worldId) {
        return repository.findByWorldId(worldId.getId());
    }

    @Transactional(readOnly = true)
    public List<WBackdrop> findAllEnabled(WorldId worldId) {
        return repository.findByWorldIdAndEnabled(worldId.getId(), true);
    }

    @Transactional
    public WBackdrop save(WorldId worldId, String backdropId, Backdrop publicData) {
        if (blank(backdropId)) {
            throw new IllegalArgumentException("backdropId required");
        }
        if (publicData == null) {
            throw new IllegalArgumentException("publicData required");
        }

        WBackdrop entity = repository.findByWorldIdAndBackdropId(worldId.getId(), backdropId).orElseGet(() -> {
            WBackdrop neu = WBackdrop.builder()
                    .backdropId(backdropId)
                    .worldId(worldId.getId())
                    .enabled(true)
                    .build();
            neu.touchCreate();
            log.debug("Creating new WBackdrop: {}", backdropId);
            return neu;
        });

        entity.setPublicData(publicData);
        entity.touchUpdate();

        WBackdrop saved = repository.save(entity);
        log.debug("Saved WBackdrop: {}", backdropId);
        return saved;
    }

    @Transactional
    public List<WBackdrop> saveAll(WorldId worldId, List<WBackdrop> entities) {
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
    public Optional<WBackdrop> update(WorldId worldId, String backdropId, Consumer<WBackdrop> updater) {
        return repository.findByWorldIdAndBackdropId(worldId.getId(), backdropId).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WBackdrop saved = repository.save(entity);
            log.debug("Updated WBackdrop: {}", backdropId);
            return saved;
        });
    }

    @Transactional
    public boolean delete(WorldId worldId, String backdropId) {
        return repository.findByWorldIdAndBackdropId(worldId.getId(), backdropId).map(entity -> {
            repository.delete(entity);
            log.debug("Deleted WBackdrop: {}", backdropId);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean disable(WorldId worldId, String backdropId) {
        return update(worldId, backdropId, entity -> entity.setEnabled(false)).isPresent();
    }

    @Transactional
    public boolean enable(WorldId worldId, String backdropId) {
        return update(worldId, backdropId, entity -> entity.setEnabled(true)).isPresent();
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
