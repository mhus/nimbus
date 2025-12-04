package de.mhus.nimbus.shared.asset;

import de.mhus.nimbus.shared.persistence.AssetMetadata;
import de.mhus.nimbus.shared.persistence.SAsset;
import de.mhus.nimbus.shared.persistence.SAssetRepository;
import de.mhus.nimbus.shared.storage.StorageService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
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

    private final SAssetRepository repository;
    private final StorageService storageService; // optional injected

    /**
     * Speichert ein Asset. Entscheidet ob inline oder extern.
     */
    @Transactional
    public SAsset saveAsset(String regionId, String worldId, String path, InputStream stream, String createdBy) {
        if (regionId == null || regionId.isBlank()) throw new IllegalArgumentException("regionId required");
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path required");
        if (stream == null) return null;

        SAsset asset = new SAsset();
        asset.setRegionId(regionId);
        asset.setWorldId(worldId);
        asset.setPath(path);
        asset.setName(extractName(path));
        asset.setCreatedAt(Instant.now());
        asset.setCreatedBy(createdBy);
        asset.setEnabled(true);

        StorageService.StorageInfo storageInfo = storageService.store("assets/" + worldId + "/" + path, stream);
        asset.setSize(storageInfo.size());
        asset.setStorageId(storageInfo.id());
        log.debug("Storing asset externally path={} size={} storageId={} region={} world={}", path, storageInfo.size(), storageInfo.id(), regionId, worldId);

        return repository.save(asset);
    }

    /**
     * Speichert ein Asset mit Metadaten (publicData).
     * Für Import aus test_server mit .info Dateien.
     */
    @Transactional
    public SAsset saveAsset(String regionId, String worldId, String path, InputStream stream,
                           String createdBy, AssetMetadata publicData) {
        if (regionId == null || regionId.isBlank()) throw new IllegalArgumentException("regionId required");
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path required");
        if (stream == null) return null;

        SAsset asset = SAsset.builder()
                .regionId(regionId)
                .worldId(worldId)
                .path(path)
                .name(extractName(path))
                .createdBy(createdBy)
                .enabled(true)
                .publicData(publicData)
                .build();
        asset.setCreatedAt(Instant.now());

        var storageInfo = storageService.store("assets/" + worldId + "/" + path, stream);
        asset.setStorageId(storageInfo.id());
        asset.setSize(storageInfo.size());
        log.debug("Storing asset externally path={} size={} storageId={} region={} world={}", path, storageInfo.size(), storageInfo.id(), regionId, worldId);

        return repository.save(asset);
    }

    public Optional<SAsset> findById(String id) { return repository.findById(id); }

    public List<SAsset> findByWorldId(String worldId) {
        return repository.findByWorldId(worldId);
    }

    public Optional<SAsset> findByPath(String regionId, String worldId, String path) {
        if (worldId == null) return repository.findByRegionIdAndPath(regionId, path);
        return repository.findByRegionIdAndWorldIdAndPath(regionId, worldId, path);
    }

    /** Lädt den Inhalt des Assets (inline oder extern). */
    public InputStream loadContent(SAsset asset) {
        if (asset == null) return null;
        if (!asset.isEnabled()) throw new IllegalStateException("Asset disabled: " + asset.getId());
        return storageService.load(asset.getStorageId());
    }

    @Transactional
    public void disable(String id) {
        repository.findById(id).ifPresent(a -> {
            if (!a.isEnabled()) return;
            a.setEnabled(false);
            repository.save(a);
            log.debug("Disabled asset id={}", id);
        });
    }

    @Transactional
    public void delete(String id) {
        repository.findById(id).ifPresent(a -> {
            try {
                storageService.delete(a.getStorageId());
            } catch (Exception e) {
                log.warn("Failed to delete external storage {}", a.getStorageId(), e);
            }
            repository.delete(a);
            log.debug("Deleted asset id={} path={}", id, a.getPath());
        });
    }

    @Transactional
    public SAsset updateContent(String id, InputStream stream) {
        if (stream == null) return null;
        return repository.findById(id).map(asset -> {
            if (!asset.isEnabled()) throw new IllegalStateException("Asset disabled: " + asset.getId());
            if (StringUtils.isNotEmpty(asset.getStorageId())) {
                var storageId = storageService.update(asset.getStorageId(), stream);
                asset.setSize(storageId.size());
                asset.setStorageId(storageId.id());
                log.debug("Updated external content id={}", storageId.id());
            } else {
                var worldId = asset.getWorldId();
                var path = asset.getPath();
                var storageId = storageService.store("assets/" + worldId + "/" + path, stream);
                asset.setSize(storageId.size());
                asset.setStorageId(storageId.id());
                log.debug("Updated/Created external content id={}", storageId.id());
            }
            return repository.save(asset);
        }).orElse(null);
    }

    @Transactional
    public Optional<SAsset> updateMetadata(String id, AssetMetadata metadata) {
        return repository.findById(id).map(asset -> {
            if (!asset.isEnabled()) throw new IllegalStateException("Asset disabled: " + asset.getId());
            asset.setPublicData(metadata);
            log.debug("Updated metadata id={}", id);
            return repository.save(asset);
        });
    }

    private String extractName(String path) {
        if (path == null) return null;
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }
}
