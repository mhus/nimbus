package de.mhus.nimbus.world.shared.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.network.messages.ChunkDataTransferObject;
import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.generated.types.Vector3;
import de.mhus.nimbus.shared.storage.StorageService;
import de.mhus.nimbus.shared.service.SchemaVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

    public static final String STORAGE_SCHEMA = "WChunkStorage";
    public static final SchemaVersion STORAGE_SCHEMA_VERSION = SchemaVersion.of("1.0.1");

    private final WChunkRepository repository;
    private final StorageService storageService;
    private final WWorldService worldService;
    private final WItemRegistryService itemRegistryService;

    @Value("${nimbus.chunk.size:16}")
    private int chunkSize = 16;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalStateException("Serialisierung ChunkData fehlgeschlagen", e);
        }

        WChunk entity = repository.findByRegionIdAndWorldIdAndChunk(regionId, worldId, chunkKey)
                .orElseGet(() -> {
                    WChunk neu = WChunk.builder().regionId(regionId).worldId(worldId).chunk(chunkKey).build();
                    neu.touchCreate();
                    return neu;
                });

        // Alle Chunks werden jetzt extern über StorageService gespeichert
        try (InputStream stream = new ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            StorageService.StorageInfo storageInfo;
            if (entity.getStorageId() != null) {
                // Update existing chunk
                storageInfo = storageService.update(STORAGE_SCHEMA, STORAGE_SCHEMA_VERSION, entity.getStorageId(), stream);
            } else {
                // Create new chunk
                storageInfo = storageService.store(STORAGE_SCHEMA, STORAGE_SCHEMA_VERSION, worldId, "chunk/" + chunkKey, stream);
            }
            entity.setStorageId(storageInfo.id());
            log.debug("Chunk extern gespeichert chunkKey={} size={} storageId={} region={} world={}",
                    chunkKey, storageInfo.size(), storageInfo.id(), regionId, worldId);
        } catch (Exception e) {
            throw new IllegalStateException("Speichern ChunkData fehlgeschlagen", e);
        }

        entity.touchUpdate();
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public InputStream getStream(String regionId, String worldId, String chunkKey) {
        WChunk chunk = repository.findByRegionIdAndWorldIdAndChunk(regionId, worldId, chunkKey).orElse(null);
        if (chunk == null || chunk.getStorageId() == null) {
            return new ByteArrayInputStream(new byte[0]);
        }

        // Alle Chunks sind jetzt extern gespeichert - Stream direkt vom StorageService zurückgeben
        InputStream stream = storageService.load(chunk.getStorageId());
        return stream != null ? stream : new ByteArrayInputStream(new byte[0]);
    }

    /**
     * Streams chunk content directly to HTTP response without loading into memory.
     * Verhindert Memory-Probleme bei großen Chunks.
     */
    @Transactional(readOnly = true)
    public boolean streamToResponse(String regionId, String worldId, String chunkKey, jakarta.servlet.http.HttpServletResponse response) {
        WChunk chunk = repository.findByRegionIdAndWorldIdAndChunk(regionId, worldId, chunkKey).orElse(null);
        if (chunk == null || chunk.getStorageId() == null) {
            return false;
        }

        try (InputStream inputStream = storageService.load(chunk.getStorageId())) {
            if (inputStream == null) {
                return false;
            }

            // Set content type and headers
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // Stream direkt zum Client ohne Memory-Belastung
            try (OutputStream outputStream = response.getOutputStream()) {
                inputStream.transferTo(outputStream);
                outputStream.flush();
            }

            log.debug("Chunk erfolgreich gestreamt chunkKey={} region={} world={}", chunkKey, regionId, worldId);
            return true;

        } catch (Exception e) {
            log.warn("Fehler beim Streamen des Chunks chunkKey={} region={} world={}", chunkKey, regionId, worldId, e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Optional<ChunkData> loadChunkData(String worldId, String chunkKey, boolean create) {
        Optional<WChunk> chunkOpt = repository.findByWorldIdAndChunk(worldId, chunkKey);

        if (chunkOpt.isPresent()) {
            // Chunk exists in database - load it
            WChunk entity = chunkOpt.get();
            if (entity.getStorageId() == null) {
                log.warn("Chunk ohne StorageId gefunden chunkKey={} world={}", chunkKey, worldId);
                return Optional.empty();
            }

            // Alle Chunks sind jetzt extern gespeichert - Stream-basierte Deserialisierung
            try (InputStream stream = storageService.load(entity.getStorageId())) {
                if (stream == null) {
                    return Optional.empty();
                }

                // Direkte Deserialisierung vom Stream ohne Memory-Verschwendung
                ChunkData chunkData = objectMapper.readValue(stream, ChunkData.class);
                return Optional.ofNullable(chunkData);

            } catch (Exception e) {
                log.warn("ChunkData Deserialisierung fehlgeschlagen chunkKey={} world={}", chunkKey, worldId, e);
                return Optional.empty();
            }
        } else if (create) {
            // Chunk not found - generate default chunk based on world settings
            log.debug("Chunk not found in DB, generating default: chunkKey={} world={}", chunkKey, worldId);
            return Optional.ofNullable(generateDefaultChunk(worldId, chunkKey));
        } else {
            // Chunk not found and create=false - return empty
            return Optional.empty();
        }
    }

    /**
     * Generate default chunk based on world configuration.
     * Creates ground blocks up to groundLevel and water blocks up to waterLevel.
     */
    private ChunkData generateDefaultChunk(String worldId, String chunkKey) {
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
            if (c.getStorageId() != null) {
                safeDeleteExternal(storageService, c.getStorageId());
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
