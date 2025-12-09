package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.WorldInfo;
import de.mhus.nimbus.shared.types.WorldId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WWorldService {

    private final WWorldRepository repository;

    @Transactional(readOnly = true)
    public Optional<WWorld> getByWorldId(WorldId worldId) {
        return repository.findByWorldId(worldId.getId());
    }

    @Transactional(readOnly = true)
    public Optional<WWorld> getByWorldId(String worldId) {
        return repository.findByWorldId(worldId);
    }

    @Transactional(readOnly = true)
    public List<WWorld> findAll() {
        return repository.findAll();
    }

    @Transactional
    public WWorld createWorld(WorldId worldId, WorldInfo info) {
        if (repository.existsByWorldId(worldId.getId())) {
            throw new IllegalStateException("WorldId bereits vorhanden: " + worldId);
        }
        WWorld entity = WWorld.builder()
                .worldId(worldId.getId())
                .publicData(info)
                .build();
        entity.touchForCreate();
        repository.save(entity);
        log.debug("WWorld angelegt: {}", worldId);
        return entity;
    }

    @Transactional
    public WWorld createWorld(WorldId worldId, WorldInfo info, String parent, String branch, Boolean enabled) {
        if (repository.existsByWorldId(worldId.getId())) {
            throw new IllegalStateException("WorldId bereits vorhanden: " + worldId);
        }
        WWorld entity = WWorld.builder()
                .worldId(worldId.getId())
                .publicData(info)
                .parent(parent)
                .branch(branch)
                .enabled(enabled == null ? true : enabled)
                .build();
        entity.touchForCreate();
        repository.save(entity);
        log.debug("WWorld angelegt (extended): {}", worldId);
        return entity;
    }

    @Transactional
    public Optional<WWorld> updateWorld(WorldId worldId, java.util.function.Consumer<WWorld> updater) {
        return repository.findByWorldId(worldId.getId()).map(existing -> {
            updater.accept(existing);
            existing.touchForUpdate();
            repository.save(existing);
            log.debug("WWorld aktualisiert: {}", worldId);
            return existing;
        });
    }

    @Transactional
    public boolean deleteWorld(WorldId worldId) {
        return repository.findByWorldId(worldId.getId()).map(e -> {
            repository.delete(e);
            log.debug("WWorld geloescht: {}", worldId);
            return true;
        }).orElse(false);
    }
}
