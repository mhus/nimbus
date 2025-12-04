package de.mhus.nimbus.world.shared.layer;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.generated.types.Vector3;
import de.mhus.nimbus.shared.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;

/**
 * Service for layer overlay algorithm.
 * Merges multiple layers into a single chunk.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WLayerOverlayService {

    private final WLayerService layerService;
    private final WLayerTerrainRepository terrainRepository;
    private final WLayerModelRepository modelRepository;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    @Value("${nimbus.chunk.size:16}")
    private int chunkSize;

    /**
     * Generate final chunk by overlaying all enabled layers.
     *
     * @param worldId  World identifier
     * @param chunkKey Chunk key (format: "cx:cz")
     * @return Merged ChunkData or empty Optional if no layers
     */
    @Transactional(readOnly = true)
    public Optional<ChunkData> generateChunk(String worldId, String chunkKey) {
        // Parse chunk coordinates
        String[] parts = chunkKey.split(":");
        if (parts.length != 2) {
            log.warn("Invalid chunk key format: {}", chunkKey);
            return Optional.empty();
        }

        int cx;
        int cz;
        try {
            cx = Integer.parseInt(parts[0]);
            cz = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            log.warn("Invalid chunk coordinates: {}", chunkKey, e);
            return Optional.empty();
        }

        // Get all layers affecting this chunk (sorted by order)
        List<WLayer> layers = layerService.getLayersAffectingChunk(worldId, chunkKey);

        if (layers.isEmpty()) {
            log.debug("No layers affecting chunk {}, returning empty", chunkKey);
            return Optional.empty();
        }

        // Initialize canvas: Map<"x:y:z", Block>
        Map<String, Block> blockMap = new HashMap<>();

        // Overlay each layer (bottom to top)
        for (WLayer layer : layers) {
            if (!layer.isEnabled()) {
                continue;
            }

            try {
                if (layer.getLayerType() == LayerType.TERRAIN) {
                    overlayTerrainLayer(layer, chunkKey, blockMap);
                } else if (layer.getLayerType() == LayerType.MODEL) {
                    overlayModelLayer(layer, cx, cz, blockMap);
                }
            } catch (Exception e) {
                log.error("Failed to overlay layer {} on chunk {}", layer.getName(), chunkKey, e);
                // Continue with other layers
            }
        }

        // Convert map to ChunkData
        ChunkData result = new ChunkData();
        result.setCx(cx);
        result.setCz(cz);
        result.setSize((byte) chunkSize);
        result.setBlocks(new ArrayList<>(blockMap.values()));

        log.debug("Generated chunk {} from {} layers, {} blocks",
                chunkKey, layers.size(), blockMap.size());

        return Optional.of(result);
    }

    /**
     * Overlay terrain layer onto block map.
     */
    private void overlayTerrainLayer(WLayer layer, String chunkKey, Map<String, Block> blockMap) {
        // Load terrain chunk from storage
        Optional<WLayerTerrain> terrainOpt = terrainRepository
                .findByLayerDataIdAndChunkKey(layer.getLayerDataId(), chunkKey);

        if (terrainOpt.isEmpty()) {
            log.trace("No terrain data for layer {} chunk {}", layer.getName(), chunkKey);
            return;
        }

        WLayerTerrain terrain = terrainOpt.get();
        if (terrain.getStorageId() == null) {
            log.warn("Terrain chunk has no storageId: layer={} chunk={}", layer.getName(), chunkKey);
            return;
        }

        // Load chunk data from storage
        try (InputStream stream = storageService.load(terrain.getStorageId())) {
            if (stream == null) {
                log.warn("Failed to load terrain storage: {}", terrain.getStorageId());
                return;
            }

            LayerChunkData chunkData = objectMapper.readValue(stream, LayerChunkData.class);

            // Overlay blocks (later blocks overwrite earlier ones)
            if (chunkData.getBlocks() != null) {
                for (LayerBlock layerBlock : chunkData.getBlocks()) {
                    if (layerBlock.getBlock() == null || layerBlock.getBlock().getPosition() == null) {
                        continue;
                    }

                    Block block = layerBlock.getBlock();
                    String key = blockKey(block.getPosition());

                    // Check override flag
                    if (!layerBlock.isOverride() && blockMap.containsKey(key)) {
                        // Skip if block exists and override is false
                        continue;
                    }

                    blockMap.put(key, block); // Overwrite previous layer
                }
            }

            log.trace("Overlaid terrain layer {}: {} blocks",
                    layer.getName(),
                    chunkData.getBlocks() != null ? chunkData.getBlocks().size() : 0);

        } catch (Exception e) {
            log.error("Failed to load terrain layer {} chunk {}", layer.getName(), chunkKey, e);
        }
    }

    /**
     * Overlay model layer onto block map.
     */
    private void overlayModelLayer(WLayer layer, int cx, int cz, Map<String, Block> blockMap) {
        // Load model
        Optional<WLayerModel> modelOpt = modelRepository
                .findByLayerDataId(layer.getLayerDataId());

        if (modelOpt.isEmpty()) {
            log.trace("No model data for layer {}", layer.getName());
            return;
        }

        WLayerModel model = modelOpt.get();
        if (model.getContent() == null || model.getContent().isEmpty()) {
            return;
        }

        // Get mount point
        int mountX = layer.getMountX() != null ? layer.getMountX() : 0;
        int mountY = layer.getMountY() != null ? layer.getMountY() : 0;
        int mountZ = layer.getMountZ() != null ? layer.getMountZ() : 0;

        // Calculate chunk bounds
        int chunkMinX = cx * chunkSize;
        int chunkMaxX = chunkMinX + chunkSize - 1;
        int chunkMinZ = cz * chunkSize;
        int chunkMaxZ = chunkMinZ + chunkSize - 1;

        // Overlay blocks that fall within chunk bounds
        int overlaidCount = 0;
        for (LayerBlock layerBlock : model.getContent()) {
            if (layerBlock.getBlock() == null || layerBlock.getBlock().getPosition() == null) {
                continue;
            }

            Block relativeBlock = layerBlock.getBlock();

            // Calculate world position
            int worldX = mountX + (int) relativeBlock.getPosition().getX();
            int worldY = mountY + (int) relativeBlock.getPosition().getY();
            int worldZ = mountZ + (int) relativeBlock.getPosition().getZ();

            // Check if within chunk bounds
            if (worldX >= chunkMinX && worldX <= chunkMaxX &&
                    worldZ >= chunkMinZ && worldZ <= chunkMaxZ) {

                String key = blockKey(relativeBlock.getPosition());

                // Check override flag
                if (!layerBlock.isOverride() && blockMap.containsKey(key)) {
                    // Skip if block exists and override is false
                    continue;
                }

                // Create block with world coordinates
                Block worldBlock = cloneBlock(relativeBlock);
                worldBlock.getPosition().setX(worldX);
                worldBlock.getPosition().setY(worldY);
                worldBlock.getPosition().setZ(worldZ);

                blockMap.put(key, worldBlock);
                overlaidCount++;
            }
        }

        log.trace("Overlaid model layer {}: {} blocks", layer.getName(), overlaidCount);
    }

    /**
     * Generate block key from position.
     */
    private String blockKey(Vector3 pos) {
        return (int) pos.getX() + ":" + (int) pos.getY() + ":" + (int) pos.getZ();
    }

    /**
     * Deep copy block using Jackson.
     */
    private Block cloneBlock(Block source) {
        try {
            return objectMapper.readValue(
                    objectMapper.writeValueAsString(source), Block.class);
        } catch (Exception e) {
            log.error("Failed to clone block", e);
            return source; // Fallback
        }
    }
}
