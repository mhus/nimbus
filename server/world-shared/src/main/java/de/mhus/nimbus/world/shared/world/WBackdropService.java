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
 * Backdrops are only stored in main worlds (no branches, no instances, no zones).
 * Similar to assets, backdrops must be created in the main world to be used in branches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WBackdropService {

    private final WBackdropRepository repository;

    /**
     * Find backdrop by ID.
     * Always looks up in main world only (no branches, no instances, no zones).
     */
    @Transactional(readOnly = true)
    public Optional<WBackdrop> findByBackdropId(WorldId worldId, String backdropId) {
        var lookupWorld = worldId.withoutInstanceAndZone().withoutBranchAndInstance();
        return repository.findByWorldIdAndBackdropId(lookupWorld.getId(), backdropId);
    }

    /**
     * Find all backdrops for a world.
     * Always looks up in main world only (no branches, no instances, no zones).
     */
    @Transactional(readOnly = true)
    public List<WBackdrop> findByWorldId(WorldId worldId) {
        var lookupWorld = worldId.withoutInstanceAndZone().withoutBranchAndInstance();
        return repository.findByWorldId(lookupWorld.getId());
    }

    /**
     * Find all enabled backdrops for a world.
     * Always looks up in main world only (no branches, no instances, no zones).
     */
    @Transactional(readOnly = true)
    public List<WBackdrop> findAllEnabled(WorldId worldId) {
        var lookupWorld = worldId.withoutInstanceAndZone().withoutBranchAndInstance();
        return repository.findByWorldIdAndEnabled(lookupWorld.getId(), true);
    }

    /**
     * Save or update a backdrop.
     * Always saves to main world only (no branches, no instances, no zones).
     */
    @Transactional
    public WBackdrop save(WorldId worldId, String backdropId, Backdrop publicData) {
        if (blank(backdropId)) {
            throw new IllegalArgumentException("backdropId required");
        }
        if (publicData == null) {
            throw new IllegalArgumentException("publicData required");
        }

        var lookupWorld = worldId.withoutInstanceAndZone().withoutBranchAndInstance();

        WBackdrop entity = repository.findByWorldIdAndBackdropId(lookupWorld.getId(), backdropId).orElseGet(() -> {
            WBackdrop neu = WBackdrop.builder()
                    .backdropId(backdropId)
                    .worldId(lookupWorld.getId())
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

    /**
     * Update a backdrop.
     * Always updates in main world only (no branches, no instances, no zones).
     */
    @Transactional
    public Optional<WBackdrop> update(WorldId worldId, String backdropId, Consumer<WBackdrop> updater) {
        var lookupWorld = worldId.withoutInstanceAndZone().withoutBranchAndInstance();
        return repository.findByWorldIdAndBackdropId(lookupWorld.getId(), backdropId).map(entity -> {
            updater.accept(entity);
            entity.touchUpdate();
            WBackdrop saved = repository.save(entity);
            log.debug("Updated WBackdrop: {}", backdropId);
            return saved;
        });
    }

    /**
     * Delete a backdrop.
     * Always deletes from main world only (no branches, no instances, no zones).
     */
    @Transactional
    public boolean delete(WorldId worldId, String backdropId) {
        var lookupWorld = worldId.withoutInstanceAndZone().withoutBranchAndInstance();
        return repository.findByWorldIdAndBackdropId(lookupWorld.getId(), backdropId).map(entity -> {
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
