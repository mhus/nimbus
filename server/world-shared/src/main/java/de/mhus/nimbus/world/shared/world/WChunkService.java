package de.mhus.nimbus.world.shared.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.network.messages.ChunkDataTransferObject;
import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.generated.types.Vector3;
import de.mhus.nimbus.shared.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
    private final WWorldService worldService;
    private final WItemRegistryService itemRegistryService;

    @Value("${nimbus.chunk.inline-max-size:1048576}")
    @Setter
    private long inlineMaxSize = 1048576L; // 1 MB Default

    @Value("${nimbus.chunk.size:16}")
    private int chunkSize = 16;

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
            String storageId = storage.store("chunk/" + worldId + "/" + chunkKey, bytes);
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
    public Optional<ChunkData> loadChunkData(String regionId, String worldId, String chunkKey, boolean create) {
        Optional<WChunk> chunkOpt = repository.findByRegionIdAndWorldIdAndChunk(regionId, worldId, chunkKey);

        if (chunkOpt.isPresent()) {
            // Chunk exists in database - load it
            WChunk entity = chunkOpt.get();
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
            if (raw.length == 0) return Optional.empty();
            try {
                return Optional.ofNullable(objectMapper.readValue(raw, ChunkData.class));
            } catch (Exception e) {
                log.warn("ChunkData Deserialisierung fehlgeschlagen chunkKey={} region={} world={}", chunkKey, regionId, worldId, e);
                return Optional.empty();
            }
        } else if (create) {
            // Chunk not found - generate default chunk based on world settings
            log.debug("Chunk not found in DB, generating default: chunkKey={} world={}", chunkKey, worldId);
            return Optional.ofNullable(generateDefaultChunk(regionId, worldId, chunkKey));
        } else {
            // Chunk not found and create=false - return empty
            return Optional.empty();
        }
    }

    /**
     * Generate default chunk based on world configuration.
     * Creates ground blocks up to groundLevel and water blocks up to waterLevel.
     */
    private ChunkData generateDefaultChunk(String regionId, String worldId, String chunkKey) {
        try {
            // Parse chunk coordinates from key (format: "cx:cz")
            String[] parts = chunkKey.split(":");
            if (parts.length != 2) {
                log.warn("Invalid chunk key format: {}", chunkKey);
                return null;
            }

            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);

            // Load world configuration
            WWorld world = worldService.getByWorldId(worldId).orElse(null);
            if (world == null) {
                log.warn("World not found for default chunk generation: {}", worldId);
                return null;
            }

            int groundLevel = world.getGroundLevel();
            Integer waterLevel = world.getWaterLevel();
            String groundBlockType = world.getGroundBlockType();
            String waterBlockType = world.getWaterBlockType();

            // Create chunk data
            ChunkData chunkData = new ChunkData();
            chunkData.setCx(cx);
            chunkData.setCz(cz);
            chunkData.setSize((byte) chunkSize);

            List<Block> blocks = new ArrayList<>();

            // Generate blocks for the chunk (16x16 xz area)
            for (int localX = 0; localX < chunkSize; localX++) {
                for (int localZ = 0; localZ < chunkSize; localZ++) {
                    int worldX = cx * chunkSize + localX;
                    int worldZ = cz * chunkSize + localZ;

                    // Create ground block at groundLevel
                    if (groundLevel >= 0 && groundBlockType != null) {
                        Block groundBlock = createBlock(worldX, groundLevel, worldZ, groundBlockType);
                        blocks.add(groundBlock);
                    }

                    // Create water blocks from groundLevel+1 to waterLevel
                    if (waterLevel != null && waterLevel > groundLevel && waterBlockType != null) {
                        for (int y = groundLevel + 1; y <= waterLevel; y++) {
                            Block waterBlock = createBlock(worldX, y, worldZ, waterBlockType);
                            blocks.add(waterBlock);
                        }
                    }
                }
            }

            chunkData.setBlocks(blocks);

            log.debug("Generated default chunk: cx={}, cz={}, blocks={}, groundLevel={}, waterLevel={}",
                    cx, cz, blocks.size(), groundLevel, waterLevel);

            return chunkData;

        } catch (Exception e) {
            log.error("Failed to generate default chunk: chunkKey={}", chunkKey, e);
            return null;
        }
    }

    /**
     * Create a simple block at the given position.
     */
    private Block createBlock(int x, int y, int z, String blockTypeId) {
        Block block = new Block();

        Vector3 position = new Vector3();
        position.setX(x);
        position.setY(y);
        position.setZ(z);
        block.setPosition(position);

        block.setBlockTypeId(blockTypeId);

        return block;
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

    /**
     * Convert ChunkData to ChunkDataTransferObject for network transmission.
     * Komprimiert field names für optimierte Netzwerk-Übertragung.
     * Loads and includes items from item registry.
     *
     * @param worldId World identifier
     * @param universeId Universe identifier
     * @param chunkData Internal chunk data
     * @return Transfer object with compressed field names (blocks → b, heightData → h, items → i)
     */
    public ChunkDataTransferObject toTransferObject(String worldId, String universeId, ChunkData chunkData) {
        if (chunkData == null) return null;

        // Load items for this chunk from registry
        var items = itemRegistryService.getItemsInChunk(worldId, universeId, chunkData.getCx(), chunkData.getCz());

        log.trace("Converting chunk to transfer object: cx={}, cz={}, blocks={}, items={}",
                chunkData.getCx(), chunkData.getCz(),
                chunkData.getBlocks() != null ? chunkData.getBlocks().size() : 0,
                items.size());

        return ChunkDataTransferObject.builder()
                .cx(chunkData.getCx())
                .cz(chunkData.getCz())
                .b(chunkData.getBlocks())        // blocks → b
                .i(items.isEmpty() ? null : items)  // items from registry → i
                .h(chunkData.getHeightData())     // heightData → h
                // Note: AreaData (a) currently not in ChunkData
                .build();
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
