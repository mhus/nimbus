package de.mhus.nimbus.shared.asset;

import de.mhus.nimbus.shared.persistence.AssetMetadata;
import de.mhus.nimbus.shared.persistence.SAsset;
import de.mhus.nimbus.shared.persistence.SAssetRepository;
import de.mhus.nimbus.shared.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Value("${nimbus.asset.inline-max-size:3145728}")
    @Setter
    private long inlineMaxSize = 3145728L; // Default falls kein Spring Context

    public long getInlineMaxSize() { return inlineMaxSize; }

    /**
     * Speichert ein Asset. Entscheidet ob inline oder extern.
     */
    @Transactional
    public SAsset saveAsset(String regionId, String worldId, String path, byte[] data, String createdBy) {
        if (regionId == null || regionId.isBlank()) throw new IllegalArgumentException("regionId required");
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path required");
        if (data == null) data = new byte[0];

        SAsset asset = new SAsset();
        asset.setRegionId(regionId);
        asset.setWorldId(worldId);
        asset.setPath(path);
        asset.setName(extractName(path));
        asset.setCreatedAt(Instant.now());
        asset.setCreatedBy(createdBy);
        asset.setEnabled(true);
        asset.setSize(data.length);

        if (data.length <= inlineMaxSize) {
            asset.setContent(data);
            log.debug("Storing asset inline path={} size={} region={} world={}", path, data.length, regionId, worldId);
        } else {
            String storageId = storageService.store("assets/" + worldId + "/" + path, data);
            asset.setStorageId(storageId);
            log.debug("Storing asset externally path={} size={} storageId={} region={} world={}", path, data.length, storageId, regionId, worldId);
        }
        return repository.save(asset);
    }

    /**
     * Speichert ein Asset mit Metadaten (publicData).
     * Für Import aus test_server mit .info Dateien.
     */
    @Transactional
    public SAsset saveAsset(String regionId, String worldId, String path, byte[] data,
                           String createdBy, AssetMetadata publicData) {
        if (regionId == null || regionId.isBlank()) throw new IllegalArgumentException("regionId required");
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path required");
        if (data == null) data = new byte[0];

        SAsset asset = SAsset.builder()
                .regionId(regionId)
                .worldId(worldId)
                .path(path)
                .name(extractName(path))
                .createdBy(createdBy)
                .enabled(true)
                .size(data.length)
                .publicData(publicData)
                .build();
        asset.setCreatedAt(Instant.now());

        if (data.length <= inlineMaxSize) {
            asset.setContent(data);
            log.debug("Storing asset inline path={} size={} region={} world={}", path, data.length, regionId, worldId);
        } else {
            String storageId = storageService.store("assets/" + worldId + "/" + path, data);
            asset.setStorageId(storageId);
            log.debug("Storing asset externally path={} size={} storageId={} region={} world={}", path, data.length, storageId, regionId, worldId);
        }
        return repository.save(asset);
    }

    public Optional<SAsset> findById(String id) { return repository.findById(id); }

    public Optional<SAsset> findByPath(String regionId, String worldId, String path) {
        if (worldId == null) return repository.findByRegionIdAndPath(regionId, path);
        return repository.findByRegionIdAndWorldIdAndPath(regionId, worldId, path);
    }

    /** Lädt den Inhalt des Assets (inline oder extern). */
    public byte[] loadContent(SAsset asset) {
        if (asset == null) return null;
        if (!asset.isEnabled()) throw new IllegalStateException("Asset disabled: " + asset.getId());
        if (asset.isInline()) return asset.getContent();
        if (asset.isStoredExternal()) {
            return storageService.load(asset.getStorageId());
        }
        return null;
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
            if (a.isStoredExternal()) {
                try {
                    storageService.delete(a.getStorageId());
                } catch (Exception e) {
                    log.warn("Failed to delete external storage {}", a.getStorageId(), e);
                }
            }
            repository.delete(a);
            log.debug("Deleted asset id={} path={}", id, a.getPath());
        });
    }

    @Transactional
    public Optional<SAsset> updateContent(String id, byte[] newData) {
        final byte[] data = newData == null ? new byte[0] : newData; // final für Lambda
        return repository.findById(id).map(asset -> {
            if (!asset.isEnabled()) throw new IllegalStateException("Asset disabled: " + asset.getId());
            asset.setSize(data.length);
            if (data.length <= inlineMaxSize) {
                asset.setStorageId(null);
                asset.setContent(data);
                log.debug("Updated inline content id={} size={}", id, data.length);
            } else {
                if (asset.isStoredExternal()) {
                    String storageId = storageService.update(asset.getStorageId(), data);
                } else {
                    var worldId = asset.getWorldId();
                    var path = asset.getPath();
                    String storageId = storageService.store("assets/" + worldId + "/" + path, data);
                    asset.setStorageId(storageId);
                }
                asset.setContent(null);
                log.debug("Updated external content id={} size={}", id, data.length);
            }
            return repository.save(asset);
        });
    }

    private String extractName(String path) {
        if (path == null) return null;
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }
}
