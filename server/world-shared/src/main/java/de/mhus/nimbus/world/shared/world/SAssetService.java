package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.shared.types.SchemaVersion;
import de.mhus.nimbus.shared.storage.StorageService;
import de.mhus.nimbus.shared.types.WorldId;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service zur Verwaltung von Assets (Inline oder extern gespeichert).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SAssetService {

    public static final String STORAGE_SCHEMA = "SAssetStorage";
    public static final SchemaVersion STORAGE_SCHEMA_VERSION = SchemaVersion.create("1.0.0");

    private final SAssetRepository repository;
    private final StorageService storageService; // optional injected

    /**
     * Speichert ein Asset. Entscheidet ob inline oder extern.
     */
    @Transactional
    public SAsset saveAsset(WorldId worldId, String path, InputStream stream, String createdBy) {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path required");
        if (stream == null) return null;

        // world lookup
        if (worldId.isInstance()) {
            throw new IllegalArgumentException("can't be save to a world instance: " + worldId);
        }

        // action
        SAsset asset = new SAsset();
        asset.setWorldId(worldId.getId());
        asset.setPath(path);
        asset.setName(extractName(path));
        asset.setCreatedAt(Instant.now());
        asset.setCreatedBy(createdBy);
        asset.setEnabled(true);

        StorageService.StorageInfo storageInfo = storageService.store(STORAGE_SCHEMA, STORAGE_SCHEMA_VERSION, worldId.getId(), "assets/" + path, stream);
        asset.setSize(storageInfo.size());
        asset.setStorageId(storageInfo.id());
        log.debug("Storing asset externally path={} size={} storageId={} world={}", path, storageInfo.size(), storageInfo.id(), worldId);

        return repository.save(asset);
    }

    /**
     * Speichert ein Asset mit Metadaten (publicData).
     * Für Import aus test_server mit .info Dateien.
     */
    @Transactional
    public SAsset saveAsset(WorldId worldId, String path, InputStream stream,
                           String createdBy, AssetMetadata publicData) {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path required");
        if (stream == null) return null;

        // world lookup
        if (worldId.isInstance()) {
            throw new IllegalArgumentException("can't be save to a world instance: " + worldId);
        }

        SAsset asset = SAsset.builder()
                .worldId(worldId.getId())
                .path(path)
                .name(extractName(path))
                .createdBy(createdBy)
                .enabled(true)
                .publicData(publicData)
                .build();
        asset.setCreatedAt(Instant.now());

        var storageInfo = storageService.store(STORAGE_SCHEMA, STORAGE_SCHEMA_VERSION, worldId.getId(), "assets/" + path, stream);
        asset.setStorageId(storageInfo.id());
        asset.setSize(storageInfo.size());
        log.debug("Storing asset externally path={} size={} storageId={} world={}", path, storageInfo.size(), storageInfo.id(), worldId);

        return repository.save(asset);
    }

    /**
     * Find all assets by worldId.
     * Assets are only stored in main worlds (no branches, no instances, no zones).
     * WARNING: This loads ALL assets into memory. Use searchAssets() for large result sets.
     */
    public List<SAsset> findByWorldId(WorldId worldId) {
        var lookupWorld = worldId.withoutInstanceAndZone().withoutBranchAndInstance();
        return repository.findByWorldId(lookupWorld.getId());
    }

    /**
     * World instances and branches never own Assets.
     * Assets are only stored in main worlds.
     *
     * @param worldId
     * @param path
     * @return
     */
    public Optional<SAsset> findByPath(WorldId worldId, String path) {

        // world lookup - always use main world (no branches, no instances, no zones)
        var lookupWorld = worldId.withoutInstanceAndZone().withoutBranchAndInstance();
        int pos = path.indexOf(':');
        if (pos < 0) {
            if (path.startsWith("w/")) {
                pos = 1; // legacy support
            } else {
                throw new IllegalArgumentException("path must have a groupId (first element): " + path);
            }
        }
        var group = path.substring(0, pos);
        var relativePath = path.substring(pos + 1);
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        if ("w".equals(group)) {
            // world asset - always from main world
            return repository.findByWorldIdAndPath(lookupWorld.getId(), relativePath);
        } else
        if ("r".equals(group)) {
            // region asset
            var regionWorldId = WorldId.of(WorldId.COLLECTION_REGION, lookupWorld.getRegionId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid region worldId: " + lookupWorld.getRegionId()));
            return repository.findByWorldIdAndPath(regionWorldId.getId(), relativePath);
        } else
        if ("p".equals(group)) {
            // public asset
            var publicWorldId = WorldId.of(WorldId.COLLECTION_PUBLIC, lookupWorld.getRegionId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid public collection worldId:" + lookupWorld.getRegionId()));
            return repository.findByWorldIdAndPath(publicWorldId.getId(), relativePath);
        } else {
            // shared asset group
            var collectionWorldId = WorldId.of( WorldId.COLLECTION_SHARED, group)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid shared collection worldId: " + group));
            return repository.findByWorldIdAndPath(collectionWorldId.getId(), relativePath);
        }
    }

    /** Lädt den Inhalt des Assets. */
    public InputStream loadContent(SAsset asset) {
        if (asset == null) return null;
        if (!asset.isEnabled()) throw new IllegalStateException("Asset disabled: " + asset.getId());
        return storageService.load(asset.getStorageId());
    }

    @Transactional
    public void disable(SAsset asset) {
        repository.findById(asset.getId()).ifPresent(a -> {
            if (!a.isEnabled()) return;
            a.setEnabled(false);
            repository.save(a);
            log.debug("Disabled asset id={}", asset.getId());
        });
    }

    @Transactional
    public void delete(SAsset asset) {
        repository.findById(asset.getId()).ifPresent(a -> {
            try {
                storageService.delete(a.getStorageId());
            } catch (Exception e) {
                log.warn("Failed to delete external storage {}", a.getStorageId(), e);
            }
            repository.delete(a);
            log.debug("Deleted asset id={} path={}", asset.getId(), a.getPath());
        });
    }

    @Transactional
    public SAsset updateContent(SAsset asset, InputStream stream) {
        if (stream == null) return null;
        return repository.findById(asset.getId()).map(a -> {
            if (!a.isEnabled()) throw new IllegalStateException("Asset disabled: " + a.getId());
            if (StringUtils.isNotEmpty(a.getStorageId())) {
                var storageId = storageService.update(STORAGE_SCHEMA, STORAGE_SCHEMA_VERSION, a.getStorageId(), stream);
                a.setSize(storageId.size());
                a.setStorageId(storageId.id());
                log.debug("Updated external content id={}", storageId.id());
            } else {
                var worldId = a.getWorldId();
                var path = a.getPath();
                var storageId = storageService.store(STORAGE_SCHEMA, STORAGE_SCHEMA_VERSION, worldId, "assets/" + path, stream);
                a.setSize(storageId.size());
                a.setStorageId(storageId.id());
                log.debug("Updated/Created external content id={}", storageId.id());
            }
            return repository.save(a);
        }).orElse(null);
    }

    @Transactional
    public Optional<SAsset> updateMetadata(SAsset asset, AssetMetadata metadata) {
        return repository.findById(asset.getId()).map(a -> {
            if (!a.isEnabled()) throw new IllegalStateException("Asset disabled: " + a.getId());
            a.setPublicData(metadata);
            log.debug("Updated metadata id={}", asset.getId());
            return repository.save(a);
        });
    }

    /**
     * Duplicates an asset with a new path.
     * Creates a copy of the content in storage and a new database entry.
     */
    @Transactional
    public SAsset duplicateAsset(SAsset source, String newPath, String createdBy) {
        if (source == null) throw new IllegalArgumentException("source asset required");
        if (newPath == null || newPath.isBlank()) throw new IllegalArgumentException("newPath required");
        if (!source.isEnabled()) throw new IllegalStateException("Source asset disabled: " + source.getId());

        // Load content from source
        InputStream sourceContent = loadContent(source);
        if (sourceContent == null) {
            throw new IllegalStateException("Failed to load source asset content: " + source.getId());
        }

        // Create new asset entity
        SAsset duplicate = SAsset.builder()
                .worldId(source.getWorldId())
                .path(newPath)
                .name(extractName(newPath))
                .createdBy(createdBy)
                .enabled(true)
                .publicData(source.getPublicData()) // Copy metadata
                .build();
        duplicate.setCreatedAt(Instant.now());

        // Store content in new location
        WorldId worldId = WorldId.of(source.getWorldId())
                .orElseThrow(() -> new IllegalStateException("Invalid worldId in source asset"));
        StorageService.StorageInfo storageInfo = storageService.store(
                STORAGE_SCHEMA,
                STORAGE_SCHEMA_VERSION,
                worldId.getId(),
                "assets/" + newPath,
                sourceContent
        );

        duplicate.setStorageId(storageInfo.id());
        duplicate.setSize(storageInfo.size());

        log.debug("Duplicated asset: sourcePath={}, newPath={}, size={}, storageId={}",
                  source.getPath(), newPath, storageInfo.size(), storageInfo.id());

        return repository.save(duplicate);
    }

    private String extractName(String path) {
        if (path == null) return null;
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    /**
     * Search assets with database-level filtering and pagination.
     * Supports prefix-based search (w:, r:, p:, or shared collections).
     * Assets are only stored in main worlds (no branches, no instances, no zones).
     *
     * @param worldId The world identifier
     * @param query Search query (optional, prefix:path format, default prefix is "w:")
     * @param extension Extension filter (optional, e.g., ".png")
     * @param offset Pagination offset
     * @param limit Pagination limit
     * @return Page of assets with total count
     */
    public AssetSearchResult searchAssets(WorldId worldId, String query, String extension, int offset, int limit) {
        // Parse prefix from query
        final String prefix;
        final String searchPath;

        if (query != null && !query.isBlank()) {
            int colonPos = query.indexOf(':');
            if (colonPos > 0) {
                prefix = query.substring(0, colonPos);
                String tempPath = query.substring(colonPos + 1);
                searchPath = tempPath.startsWith("/") ? tempPath.substring(1) : tempPath;
            } else {
                prefix = "w"; // default
                searchPath = query;
            }
        } else {
            prefix = "w"; // default
            searchPath = null; // no filter
        }

        // Determine target worldId based on prefix
        // Always use main world (no branches, no instances, no zones)
        WorldId lookupWorld = worldId.withoutInstanceAndZone().withoutBranchAndInstance();
        final String targetWorldId = switch (prefix.toLowerCase()) {
            case "w" -> {
                // World asset - always use main world
                yield lookupWorld.getId();
            }
            case "r" -> {
                // Region asset
                WorldId regionWorldId = WorldId.of(WorldId.COLLECTION_REGION, lookupWorld.getRegionId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid region worldId: " + lookupWorld.getRegionId()));
                yield regionWorldId.getId();
            }
            case "p" -> {
                // Public asset
                WorldId publicWorldId = WorldId.of(WorldId.COLLECTION_PUBLIC, lookupWorld.getRegionId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid public collection worldId: " + lookupWorld.getRegionId()));
                yield publicWorldId.getId();
            }
            default -> {
                // Shared collection
                WorldId collectionWorldId = WorldId.of(WorldId.COLLECTION_SHARED, prefix)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid shared collection worldId: " + prefix));
                yield collectionWorldId.getId();
            }
        };

        // Build regex pattern for path search and extension filter
        StringBuilder regexBuilder = new StringBuilder();
        if (searchPath != null && !searchPath.isBlank()) {
            regexBuilder.append(".*").append(java.util.regex.Pattern.quote(searchPath)).append(".*");
        }
        if (extension != null && !extension.isBlank()) {
            String normalizedExt = extension.trim().toLowerCase();
            if (!normalizedExt.startsWith(".")) {
                normalizedExt = "." + normalizedExt;
            }
            if (regexBuilder.length() == 0) {
                regexBuilder.append(".*");
            }
            regexBuilder.append("\\").append(java.util.regex.Pattern.quote(normalizedExt)).append("$");
        }

        String pathPattern = regexBuilder.length() > 0 ? regexBuilder.toString() : null;

        // Direct search - assets are only in main worlds
        return searchInWorldId(targetWorldId, pathPattern, offset, limit);
    }

    /**
     * Search assets in a specific worldId with filtering and pagination.
     */
    private AssetSearchResult searchInWorldId(String worldId, String pathPattern, int offset, int limit) {
        // Calculate page number from offset (Spring Data uses 0-based page numbers)
        int pageNumber = offset / limit;
        Pageable pageable = PageRequest.of(pageNumber, limit);
        Page<SAsset> page;

        if (pathPattern != null) {
            page = repository.findByWorldIdAndPathContaining(worldId, pathPattern, pageable);
        } else {
            page = repository.findByWorldId(worldId, pageable);
        }

        return new AssetSearchResult(
                page.getContent(),
                (int) page.getTotalElements(),
                offset,
                limit
        );
    }

    /**
     * Result wrapper for asset search with pagination info.
     */
    public record AssetSearchResult(
            List<SAsset> assets,
            int totalCount,
            int offset,
            int limit
    ) {}
}
