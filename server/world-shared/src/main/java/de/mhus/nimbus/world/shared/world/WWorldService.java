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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WWorldService {

    private final WWorldRepository repository;
    private final WAnythingService anythingService;

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
     * Find all world collections from WAnything entries with collection="worldCollection".
     * World collections are identified by WorldId entries starting with '@'.
     * Note: Region field in WAnything is ignored for world collections, as they are global.
     *
     * @return List of distinct WorldIds representing collections
     */
    @Transactional(readOnly = true)
    public List<WorldId> findWorldCollections() {
        // Use findByCollection without region filter, as world collections are not region-specific
        List<WAnything> entries = anythingService.findByCollection("worldCollection");
        return entries.stream()
                .map(WAnything::getName)
                .filter(name -> name != null && name.startsWith("@"))
                .distinct()
                .map(WorldId::unchecked)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Find a specific world collection by WorldId.
     * Checks if the given WorldId is a collection and if corresponding WAnything entries exist.
     * Note: Region field in WAnything is ignored for world collections, as they are global.
     *
     * @param worldId The WorldId to search for (must be a collection starting with '@')
     * @return Optional containing the WorldId if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<WorldId> findWorldCollection(WorldId worldId) {
        if (!worldId.isCollection()) {
            log.debug("WorldId is not a collection: {}", worldId);
            return Optional.empty();
        }

        // Use findByCollection without region filter, then filter by worldId
        // This ignores the regionId field in WAnything, which may be incorrectly set for collections
        boolean exists = anythingService.findByCollection("worldCollection")
                .stream()
                .anyMatch(e -> worldId.getId().equals(e.getWorldId()));

        return exists ? Optional.of(worldId) : Optional.empty();
    }

    /**
     * Check if a world collection exists.
     * Verifies that the WorldId is a collection and that corresponding WAnything entries exist.
     * Note: Region field in WAnything is ignored for world collections, as they are global.
     *
     * @param worldId The WorldId to check (must be a collection starting with '@')
     * @return true if the collection exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsWorldCollection(WorldId worldId) {
        if (!worldId.isCollection()) {
            return false;
        }

        // Use findByCollection without region filter, then filter by worldId
        // This ignores the regionId field in WAnything, which may be incorrectly set for collections
        return anythingService.findByCollection("worldCollection")
                .stream()
                .anyMatch(e -> worldId.getId().equals(e.getWorldId()));
    }

    /**
     * Get the title for a world collection from its WAnything data.
     * Returns the title from WorldCollectionDto if available, otherwise null.
     *
     * @param worldId The WorldId of the collection
     * @return The title if found and valid, null otherwise
     */
    @Transactional(readOnly = true)
    public String getWorldCollectionTitle(WorldId worldId) {
        if (!worldId.isCollection()) {
            return null;
        }

        // Find the WAnything entry for this collection
        return anythingService.findByCollection("worldCollection")
                .stream()
                .filter(e -> worldId.getId().equals(e.getName()))
                .findFirst()
                .flatMap(e -> e.getDataAs(WorldCollectionDto.class))
                .map(WorldCollectionDto::getTitle)
                .orElse(null);
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
