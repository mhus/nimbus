package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.shared.types.SchemaVersion;
import de.mhus.nimbus.shared.storage.StorageService;
import de.mhus.nimbus.shared.types.WorldId;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public List<SAsset> findByWorldId(WorldId worldId) {
        return repository.findByWorldId(worldId.getId());
    }

    /**
     * World instances never own Assets.
     *
     * @param worldId
     * @param path
     * @return
     */
    public Optional<SAsset> findByPath(WorldId worldId, String path) {

        // world lookup
        var lookupWorld = worldId.withoutInstance();
        int pos = path.indexOf('/');
        if (pos < 0) {
            throw new IllegalArgumentException("path must have a groupId (first element): " + path);
        }
        var group = path.substring(0, pos);
        var relativePath = path.substring(pos + 1);

        if ("w".equals(group)) {
            // world asset
            if (lookupWorld.isBranch()) {
                var item = repository.findByWorldIdAndPath(lookupWorld.getId(),  relativePath);
                if (item.isPresent()) return item;
                // fallback to parent world
                return patchWorldId(lookupWorld, repository.findByWorldIdAndPath(lookupWorld.withoutBranchAndInstance().getId(),  relativePath)); // TODO relativePath !!
            }
            return repository.findByWorldIdAndPath(lookupWorld.getId(), relativePath);
        } else
        if ("r".equals(group)) {
            // region asset
            var regionWorldId = WorldId.of(WorldId.COLLECTION_REGION, lookupWorld.getRegionId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid region worldId: " + lookupWorld.getRegionId()));
            return repository.findByWorldIdAndPath(lookupWorld.getId(), relativePath);
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

    private Optional<SAsset> patchWorldId(WorldId worldId, Optional<SAsset> item) {
        if (item.isEmpty()) return item;
        var asset = item.get();
        // do I need a copy here? do I need to patch worldId?
        asset.setWorldId(worldId.getId());
        return item;
    }

    /** Lädt den Inhalt des Assets (inline oder extern). */
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
}
