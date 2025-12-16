package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.WorldInfo;
import de.mhus.nimbus.shared.types.WorldId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    /**
     * Find all worlds (no filtering, no pagination).
     * WARNING: This loads ALL worlds into memory. Use searchWorlds() for large result sets.
     */
    @Transactional(readOnly = true)
    public List<WWorld> findAll() {
        return repository.findAll();
    }

    /**
     * Search worlds with database-level filtering and pagination.
     * Searches in worldId, name, and description fields (case-insensitive).
     *
     * @param searchQuery Optional search query (can be null/empty for no filter)
     * @param offset Pagination offset (0-based)
     * @param limit Pagination limit
     * @return WorldSearchResult with paginated worlds and total count
     */
    @Transactional(readOnly = true)
    public WorldSearchResult searchWorlds(String searchQuery, int offset, int limit) {
        log.debug("Searching worlds: query='{}', offset={}, limit={}", searchQuery, offset, limit);

        // Calculate page number from offset
        int pageNumber = offset / limit;
        Pageable pageable = PageRequest.of(pageNumber, limit);

        Page<WWorld> page;
        if (searchQuery != null && !searchQuery.isBlank()) {
            // Search with filter (MongoDB regex search across multiple fields)
            String searchPattern = searchQuery; // MongoDB regex, no need to add .*
            page = repository.findBySearchQuery(searchPattern, pageable);
        } else {
            // No filter, just pagination
            page = repository.findAllBy(pageable);
        }

        log.debug("Found {} worlds (total: {})", page.getNumberOfElements(), page.getTotalElements());

        return new WorldSearchResult(
                page.getContent(),
                (int) page.getTotalElements(),
                offset,
                limit
        );
    }

    @Transactional(readOnly = true)
    public List<WWorld> findByRegionId(String regionId) {
        return repository.findByRegionId(regionId);
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
    public WWorld save(WWorld world) {
        world.touchForUpdate();
        WWorld saved = repository.save(world);
        log.debug("WWorld gespeichert: {}", world.getWorldId());
        return saved;
    }

    @Transactional
    public boolean deleteWorld(WorldId worldId) {
        return repository.findByWorldId(worldId.getId()).map(e -> {
            repository.delete(e);
            log.debug("WWorld geloescht: {}", worldId);
            return true;
        }).orElse(false);
    }

    /**
     * Result wrapper for world search with pagination info.
     */
    public record WorldSearchResult(
            List<WWorld> worlds,
            int totalCount,
            int offset,
            int limit
    ) {}
}
