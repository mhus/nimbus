package de.mhus.nimbus.world.shared.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.shared.asset.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

/**
 * Service für Verwaltung von Welt-Chunks (inline oder extern gespeicherter Datenblock).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WChunkService {

    private final WChunkRepository repository;
    private final Optional<StorageService> storageService; // optional injection

    @Value("${nimbus.chunk.inline-max-size:1048576}")
    @Setter
    private long inlineMaxSize = 1048576L; // 1 MB Default

    private final ObjectMapper objectMapper = new ObjectMapper();

    public long getInlineMaxSize() { return inlineMaxSize; }

    @Transactional(readOnly = true)
    public Optional<WChunk> find(String regionId, String worldId, String chunkKey) {
        return repository.findByRegionIdAndWorldIdAndChunk(regionId, worldId, chunkKey);
    }

    @Transactional
    public WChunk saveChunk(String regionId, String worldId, String chunkKey, ChunkData data) {
        if (blank(regionId) || blank(worldId) || blank(chunkKey)) {
            throw new IllegalArgumentException("regionId, worldId und chunkKey erforderlich");
        }
        if (data == null) throw new IllegalArgumentException("ChunkData erforderlich");
        // Felder 'status' und 'i' vor Serialisierung null setzen
        data.setStatus(null);
        data.setI(null);
        String json;
        try { json = objectMapper.writeValueAsString(data); } catch (Exception e) {
            throw new IllegalStateException("Serialisierung ChunkData fehlgeschlagen", e);
        }
        byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        WChunk entity = repository.findByRegionIdAndWorldIdAndChunk(regionId, worldId, chunkKey)
                .orElseGet(() -> {
                    WChunk neu = WChunk.builder().regionId(regionId).worldId(worldId).chunk(chunkKey).build();
                    neu.touchCreate();
                    return neu;
                });
        if (bytes.length <= inlineMaxSize) {
            if (entity.getStorageId() != null) storageService.ifPresent(s -> safeDeleteExternal(s, entity.getStorageId()));
            entity.setStorageId(null);
            entity.setContent(json);
            log.debug("Chunk inline gespeichert chunkKey={} size={} region={} world={}", chunkKey, bytes.length, regionId, worldId);
        } else {
            StorageService storage = storageService.orElseThrow(() -> new IllegalStateException("Kein StorageService für große Chunks"));
            if (entity.getStorageId() != null) storageService.ifPresent(s -> safeDeleteExternal(s, entity.getStorageId()));
            String storageId = storage.store(bytes);
            entity.setStorageId(storageId);
            entity.setContent(null);
            log.debug("Chunk extern gespeichert chunkKey={} size={} storageId={} region={} world={}", chunkKey, bytes.length, storageId, regionId, worldId);
        }
        entity.touchUpdate();
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public InputStream getStream(String regionId, String worldId, String chunkKey) {
        WChunk chunk = repository.findByRegionIdAndWorldIdAndChunk(regionId, worldId, chunkKey).orElse(null);
        if (chunk == null) return null;
        if (chunk.isInline()) {
            String c = chunk.getContent();
            byte[] d = c == null ? new byte[0] : c.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return new ByteArrayInputStream(d);
        }
        if (chunk.isExternal()) {
            StorageService storage = storageService.orElseThrow(() -> new IllegalStateException("Kein StorageService zum Laden"));
            byte[] d = storage.load(chunk.getStorageId());
            return d == null ? new ByteArrayInputStream(new byte[0]) : new ByteArrayInputStream(d);
        }
        return new ByteArrayInputStream(new byte[0]);
    }

    @Transactional(readOnly = true)
    public Optional<ChunkData> loadChunkData(String regionId, String worldId, String chunkKey) {
        return repository.findByRegionIdAndWorldIdAndChunk(regionId, worldId, chunkKey).map(entity -> {
            byte[] raw;
            if (entity.isInline()) {
                String c = entity.getContent();
                raw = c == null ? new byte[0] : c.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            } else if (entity.isExternal()) {
                StorageService storage = storageService.orElseThrow(() -> new IllegalStateException("Kein StorageService zum Laden"));
                raw = storage.load(entity.getStorageId());
            } else {
                raw = new byte[0];
            }
            if (raw.length == 0) return null;
            try { return objectMapper.readValue(raw, ChunkData.class); } catch (Exception e) {
                log.warn("ChunkData Deserialisierung fehlgeschlagen chunkKey={} region={} world={}", chunkKey, regionId, worldId, e);
                return null;
            }
        });
    }

    @Transactional
    public boolean delete(String regionId, String worldId, String chunkKey) {
        return repository.findByRegionIdAndWorldIdAndChunk(regionId, worldId, chunkKey).map(c -> {
            if (c.isExternal()) {
                storageService.ifPresent(s -> safeDeleteExternal(s, c.getStorageId()));
            }
            repository.delete(c);
            log.debug("Chunk gelöscht chunkKey={} region={} world={}", chunkKey, regionId, worldId);
            return true;
        }).orElse(false);
    }

    private void safeDeleteExternal(StorageService storage, String storageId) {
        try { storage.delete(storageId); } catch (Exception e) { log.warn("Externer Chunk-Speicher konnte nicht gelöscht werden id={}", storageId, e); }
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
