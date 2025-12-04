package de.mhus.nimbus.world.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.generated.types.Vector3;
import de.mhus.nimbus.world.shared.layer.WLayer;
import de.mhus.nimbus.world.shared.layer.WLayerService;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import de.mhus.nimbus.world.shared.world.WChunkService;
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
        String chunkKey = calculateChunkKey(x, z);
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
        // TODO: Implement layer-specific block loading
        // This requires:
        // 1. Load WLayer entity
        // 2. For TERRAIN: Load LayerChunkData, find block
        // 3. For MODEL: Calculate relative position, find in model content
        // 4. Convert LayerBlock to Block

        // For now: Return null (falls back to WChunk)
        return null;
    }

    /**
     * Load block from merged WChunk (final rendered result).
     */
    private Block loadBlockFromChunk(String worldId, int x, int y, int z) {
        String chunkKey = calculateChunkKey(x, z);

        Optional<ChunkData> chunkDataOpt = chunkService.loadChunkData(null, worldId, chunkKey, false);
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

    /**
     * Calculate chunk key from world coordinates.
     */
    private String calculateChunkKey(int x, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        return cx + ":" + cz;
    }
}
