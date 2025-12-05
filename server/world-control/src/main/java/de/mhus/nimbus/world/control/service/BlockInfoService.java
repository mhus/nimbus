package de.mhus.nimbus.world.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.generated.types.Vector3;
import de.mhus.nimbus.world.shared.layer.WLayer;
import de.mhus.nimbus.world.shared.layer.WLayerService;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import de.mhus.nimbus.world.shared.world.WChunkService;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for loading block information with layer metadata.
 * Handles edit mode, layer selection, and Redis overlays.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockInfoService {

    private final EditService editService;
    private final WLayerService layerService;
    private final WChunkService chunkService;
    private final WorldRedisService redisService;
    private final ObjectMapper objectMapper;
    private final WWorldService worldService;

    /**
     * Load block info with layer metadata.
     *
     * @param worldId   World identifier
     * @param sessionId Session identifier (optional - if null, loads from WChunk only)
     * @param x         Block X coordinate
     * @param y         Block Y coordinate
     * @param z         Block Z coordinate
     * @return BlockInfo as Map (can be serialized to JSON directly)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> loadBlockInfo(String worldId, String sessionId, int x, int y, int z) {
        Map<String, Object> blockInfo = new HashMap<>();

        // Position
        blockInfo.put("position", Map.of("x", x, "y", y, "z", z));

        // Check edit mode and layer selection
        boolean editMode = false;
        String layerName = null;
        Integer group = null;

        if (sessionId != null && !sessionId.isBlank()) {
            EditState state = editService.getEditState(worldId, sessionId);
            editMode = state.isEditMode();
            layerName = state.getSelectedLayer();
            group = state.getSelectedGroup();
        }

        Block block = null;
        boolean readOnly = true;
        String groupName = null;

        // Load block based on edit mode and layer selection
        if (editMode && layerName != null && !layerName.isBlank()) {
            // EDIT MODE with selected layer

            // 1. Check Redis overlay first
            block = loadBlockFromRedisOverlay(worldId, sessionId, x, y, z);

            if (block != null) {
                log.debug("Loaded block from Redis overlay: pos=({},{},{})", x, y, z);
                readOnly = false;
            } else {
                // 2. Load from selected layer
                block = loadBlockFromLayer(worldId, layerName, x, y, z);
                if (block != null) {
                    log.debug("Loaded block from layer: layer={} pos=({},{},{})", layerName, x, y, z);
                    readOnly = false;

                    // Get group name from layer
                    Optional<WLayer> layerOpt = layerService.findLayer(worldId, layerName);
                    if (layerOpt.isPresent() && group != null && group > 0) {
                        WLayer layer = layerOpt.get();
                        if (layer.getGroups() != null && layer.getGroups().size() > group) {
                            groupName = layer.getGroups().get(group);
                        }
                    }
                }
            }
        }

        // Fallback: Load from WChunk (merged result)
        if (block == null) {
            block = loadBlockFromChunk(worldId, x, y, z);
            readOnly = true;
            log.debug("Loaded block from WChunk: pos=({},{},{}) readOnly=true", x, y, z);
        }

        // Default to air if still not found
        if (block == null) {
            block = createAirBlock(x, y, z);
            log.debug("Block not found, using air: pos=({},{},{})", x, y, z);
        }

        // Build BlockInfo response
        blockInfo.put("layer", layerName);
        blockInfo.put("group", group);
        blockInfo.put("groupName", groupName);
        blockInfo.put("block", block);
        blockInfo.put("readOnly", readOnly);

        return blockInfo;
    }

    /**
     * Load block from Redis overlay (edited but not committed).
     */
    private Block loadBlockFromRedisOverlay(String worldId, String sessionId, int x, int y, int z) {
        // Get world for chunk calculation
        WWorld world = worldService.getByWorldId(worldId).orElse(null);
        if (world == null) {
            log.warn("World not found for overlay lookup: {}", worldId);
            return null;
        }

        String chunkKey = world.getChunkKey(x, z);
        String key = "world:" + worldId + ":overlay:" + sessionId + ":" + chunkKey;

        Optional<String> overlayJson = redisService.getValue(worldId, key);
        if (overlayJson.isEmpty()) {
            return null;
        }

        try {
            // Parse overlay chunk data
            ChunkData chunkData = objectMapper.readValue(overlayJson.get(), ChunkData.class);

            // Find block at position (Vector3 uses doubles, need int comparison)
            if (chunkData.getBlocks() != null) {
                for (Block block : chunkData.getBlocks()) {
                    Vector3 pos = block.getPosition();
                    if (pos != null &&
                        (int)pos.getX() == x &&
                        (int)pos.getY() == y &&
                        (int)pos.getZ() == z) {
                        log.debug("Found block in Redis overlay: pos=({},{},{}) blockTypeId={}",
                                x, y, z, block.getBlockTypeId());
                        return block;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Redis overlay: key={}", key, e);
        }

        return null;
    }

    /**
     * Load block from selected layer.
     */
    private Block loadBlockFromLayer(String worldId, String layerName, int x, int y, int z) {
        try {
            // 1. Load WLayer entity
            Optional<WLayer> layerOpt = layerService.findLayer(worldId, layerName);
            if (layerOpt.isEmpty()) {
                log.debug("Layer not found: {}", layerName);
                return null;
            }

            WLayer layer = layerOpt.get();
            String layerDataId = layer.getLayerDataId();

            // 2. Load block based on layer type
            switch (layer.getLayerType()) {
                case TERRAIN:
                    return loadBlockFromTerrainLayer(worldId, layerDataId, x, y, z);

                case MODEL:
                    return loadBlockFromModelLayer(worldId, layerDataId, layer, x, y, z);

                default:
                    log.warn("Unknown layer type: {}", layer.getLayerType());
                    return null;
            }

        } catch (Exception e) {
            log.warn("Failed to load block from layer {}: {}", layerName, e.getMessage());
            return null;
        }
    }

    /**
     * Load block from TERRAIN layer.
     */
    private Block loadBlockFromTerrainLayer(String worldId, String layerDataId, int x, int y, int z) {
        // Get world and calculate chunk key
        WWorld world = worldService.getByWorldId(worldId).orElse(null);
        if (world == null) {
            log.warn("World not found: {}", worldId);
            return null;
        }

        String chunkKey = world.getChunkKey(x, z);

        // Load LayerChunkData
        Optional<de.mhus.nimbus.world.shared.layer.LayerChunkData> chunkDataOpt =
                layerService.loadTerrainChunk(layerDataId, chunkKey);

        if (chunkDataOpt.isEmpty()) {
            return null;
        }

        de.mhus.nimbus.world.shared.layer.LayerChunkData chunkData = chunkDataOpt.get();

        // Find block at position
        if (chunkData.getBlocks() != null) {
            for (de.mhus.nimbus.world.shared.layer.LayerBlock layerBlock : chunkData.getBlocks()) {
                Block block = layerBlock.getBlock();
                if (block != null && block.getPosition() != null) {
                    Vector3 pos = block.getPosition();
                    if ((int) pos.getX() == x && (int) pos.getY() == y && (int) pos.getZ() == z) {
                        return block;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Load block from MODEL layer.
     */
    private Block loadBlockFromModelLayer(String worldId, String layerDataId, WLayer layer, int x, int y, int z) {
        // Load model content
        Optional<de.mhus.nimbus.world.shared.layer.WLayerModel> modelOpt =
                layerService.loadModel(layerDataId);

        if (modelOpt.isEmpty()) {
            return null;
        }

        de.mhus.nimbus.world.shared.layer.WLayerModel model = modelOpt.get();

        // Calculate relative position from mount point
        int mountX = layer.getMountX() != null ? layer.getMountX() : 0;
        int mountY = layer.getMountY() != null ? layer.getMountY() : 0;
        int mountZ = layer.getMountZ() != null ? layer.getMountZ() : 0;

        int relativeX = x - mountX;
        int relativeY = y - mountY;
        int relativeZ = z - mountZ;

        // Find block in model content
        if (model.getContent() != null) {
            for (de.mhus.nimbus.world.shared.layer.LayerBlock layerBlock : model.getContent()) {
                Block block = layerBlock.getBlock();
                if (block != null && block.getPosition() != null) {
                    Vector3 pos = block.getPosition();
                    if ((int) pos.getX() == relativeX && (int) pos.getY() == relativeY && (int) pos.getZ() == relativeZ) {
                        // Create new block with absolute position
                        Block absoluteBlock = Block.builder()
                                .position(Vector3.builder().x((double) x).y((double) y).z((double) z).build())
                                .blockTypeId(block.getBlockTypeId())
                                .offsets(block.getOffsets())
                                .cornerHeights(block.getCornerHeights())
                                .status(block.getStatus())
                                .modifiers(block.getModifiers())
                                .metadata(block.getMetadata())
                                .build();
                        return absoluteBlock;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Load block from merged WChunk (final rendered result).
     */
    private Block loadBlockFromChunk(String worldId, int x, int y, int z) {
        // Get world and calculate chunk key
        WWorld world = worldService.getByWorldId(worldId).orElse(null);
        if (world == null) {
            log.warn("World not found: {}", worldId);
            return null;
        }

        String chunkKey = world.getChunkKey(x, z);

        Optional<ChunkData> chunkDataOpt = chunkService.loadChunkData(worldId, chunkKey, false);
        if (chunkDataOpt.isEmpty()) {
            log.debug("Chunk not found: {}", chunkKey);
            return null;
        }

        ChunkData chunkData = chunkDataOpt.get();

        // Find block at position (Vector3 uses doubles, need int comparison)
        if (chunkData.getBlocks() != null) {
            for (Block block : chunkData.getBlocks()) {
                Vector3 pos = block.getPosition();
                if (pos != null &&
                    (int)pos.getX() == x &&
                    (int)pos.getY() == y &&
                    (int)pos.getZ() == z) {
                    log.debug("Found block in chunk: pos=({},{},{}) blockTypeId={}",
                            x, y, z, block.getBlockTypeId());
                    return block;
                }
            }
        }

        log.debug("Block not found in chunk: pos=({},{},{}) totalBlocks={}",
                x, y, z, chunkData.getBlocks() != null ? chunkData.getBlocks().size() : 0);
        return null;
    }

    /**
     * Create air block at position.
     */
    private Block createAirBlock(int x, int y, int z) {
        Vector3 position = Vector3.builder()
                .x((double) x)
                .y((double) y)
                .z((double) z)
                .build();

        Block block = Block.builder()
                .blockTypeId("air")
                .position(position)
                .build();

        return block;
    }
}
