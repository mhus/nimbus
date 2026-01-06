package de.mhus.nimbus.world.generator.flat;

import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.Vector3Int;
import de.mhus.nimbus.shared.types.BlockDef;
import de.mhus.nimbus.world.shared.generator.WFlat;
import de.mhus.nimbus.world.shared.generator.WFlatService;
import de.mhus.nimbus.world.shared.layer.LayerBlock;
import de.mhus.nimbus.world.shared.layer.LayerChunkData;
import de.mhus.nimbus.world.shared.layer.LayerType;
import de.mhus.nimbus.world.shared.layer.WDirtyChunkService;
import de.mhus.nimbus.world.shared.layer.WLayer;
import de.mhus.nimbus.world.shared.layer.WLayerService;
import de.mhus.nimbus.world.shared.world.BlockUtil;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for exporting WFlat to WLayer GROUND type.
 * Handles conversion from flat terrain data to layer chunks.
 * Marks modified chunks as dirty for regeneration.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FlatExportService {

    private final WFlatService flatService;
    private final WLayerService layerService;
    private final WWorldService worldService;
    private final WDirtyChunkService dirtyChunkService;

    /**
     * Export WFlat to a WLayer of type GROUND.
     * Only exports columns that are set (not 0/NOT_SET).
     * Fills columns down to lowest sibling level to avoid holes.
     *
     * @param flatId Flat identifier (database ID)
     * @param worldId World identifier
     * @param layerName Name of the target GROUND layer
     * @return Number of exported columns
     * @throws IllegalArgumentException if flat, world, or layer not found, or layer is not GROUND type
     */
    public int exportToLayer(String flatId, String worldId, String layerName) {
        log.info("Exporting flat to layer: flatId={}, worldId={}, layerName={}", flatId, worldId, layerName);

        // Load flat
        WFlat flat = flatService.findById(flatId)
                .orElseThrow(() -> new IllegalArgumentException("Flat not found: " + flatId));

        // Load world
        WWorld world = worldService.getByWorldId(worldId)
                .orElseThrow(() -> new IllegalArgumentException("World not found: " + worldId));

        // Load layer
        WLayer layer = layerService.findByWorldIdAndName(worldId, layerName)
                .orElseThrow(() -> new IllegalArgumentException("Layer not found: " + layerName));

        // Validate layer type
        if (layer.getLayerType() != LayerType.GROUND) {
            throw new IllegalArgumentException("Layer must be of type GROUND, but is: " + layer.getLayerType());
        }

        int chunkSize = world.getPublicData().getChunkSize();
        String layerDataId = layer.getLayerDataId();

        // Map to store modified chunks: chunkKey -> LayerChunkData
        Map<String, LayerChunkData> modifiedChunks = new HashMap<>();

        int exportedColumns = 0;
        int skippedColumns = 0;

        // Iterate over all columns in the flat
        for (int localX = 0; localX < flat.getSizeX(); localX++) {
            for (int localZ = 0; localZ < flat.getSizeZ(); localZ++) {

                // Calculate world coordinates
                int worldX = flat.getMountX() + localX;
                int worldZ = flat.getMountZ() + localZ;

                // Calculate chunk coordinates
                int chunkX = world.getChunkX(worldX);
                int chunkZ = world.getChunkZ(worldZ);
                String chunkKey = BlockUtil.toChunkKey(chunkX, chunkZ);

                // Get or create chunk data
                LayerChunkData chunkData = modifiedChunks.computeIfAbsent(chunkKey, key -> {
                    // Try to load existing chunk
                    Optional<LayerChunkData> existing = layerService.loadTerrainChunk(layerDataId, key);
                    if (existing.isPresent()) {
                        return existing.get();
                    } else {
                        // Create new chunk
                        return LayerChunkData.builder()
                                .cx(chunkX)
                                .cz(chunkZ)
                                .blocks(new ArrayList<>())
                                .build();
                    }
                });

                // Check if column is set
                if (!flat.isColumnSet(localX, localZ)) {
                    // NOT_SET: Keep existing blocks, but fill down if neighbors are lower
                    handleNotSetColumn(chunkData, worldX, worldZ, flat, localX, localZ, world);
                    skippedColumns++;
                    continue;
                }

                // Column is set - process normally
                // Get level from flat
                int level = flat.getLevel(localX, localZ);

                // Get block type from column definition
                WFlat.MaterialDefinition columnDef = flat.getColumnMaterial(localX, localZ);
                if (columnDef == null) {
                    log.warn("Column definition not found for column at ({}, {}), skipping", localX, localZ);
                    skippedColumns++;
                    continue;
                }

                // Delete all existing blocks at this column
                deleteColumnBlocks(chunkData, worldX, worldZ);

                // Find lowest sibling level to avoid holes
                int lowestSiblingLevel = findLowestSiblingLevel(flat, localX, localZ, chunkData, world);

                // Fill column from level down to lowestSiblingLevel
                fillColumn(chunkData, worldX, worldZ, level, lowestSiblingLevel, columnDef, flat);

                exportedColumns++;
            }
        }

        // Save all modified chunks
        for (Map.Entry<String, LayerChunkData> entry : modifiedChunks.entrySet()) {
            String chunkKey = entry.getKey();
            LayerChunkData chunkData = entry.getValue();
            layerService.saveTerrainChunk(worldId, layerDataId, chunkKey, chunkData);
        }

        // Mark all modified chunks as dirty for regeneration
        if (!modifiedChunks.isEmpty()) {
            List<String> chunkKeys = new ArrayList<>(modifiedChunks.keySet());
            dirtyChunkService.markChunksDirty(worldId, chunkKeys, "Flat export: " + flatId);
            log.info("Marked {} chunks as dirty for regeneration", chunkKeys.size());
        }

        log.info("Export complete: flatId={}, exported={} columns, skipped={} columns, modified={} chunks",
                flatId, exportedColumns, skippedColumns, modifiedChunks.size());

        return exportedColumns;
    }

    /**
     * Handle NOT_SET column: Keep top block, delete all blocks below, fill down if neighbors are lower.
     * This prevents gaps between old high blocks and new lower blocks.
     */
    private void handleNotSetColumn(LayerChunkData chunkData, int worldX, int worldZ,
                                    WFlat flat, int localX, int localZ, WWorld world) {
        // Find highest existing block at this position
        int existingLevel = findHighestBlockAtPosition(chunkData, worldX, worldZ);
        if (existingLevel == -1) {
            // No existing blocks, nothing to do
            return;
        }

        // Get the block type from the highest existing block BEFORE deleting
        String topBlockDefString = getBlockDefAtPosition(chunkData, worldX, worldZ, existingLevel);
        if (topBlockDefString == null || topBlockDefString.isBlank()) {
            log.warn("Could not find block definition at ({},{},{})", worldX, existingLevel, worldZ);
            return;
        }

        // Parse block definition
        Optional<BlockDef> topBlockDefOpt = BlockDef.of(topBlockDefString);
        if (topBlockDefOpt.isEmpty()) {
            log.warn("Invalid block definition for NOT_SET column: {}", topBlockDefString);
            return;
        }

        // Delete all blocks BELOW the top block (keep only the top block)
        deleteBlocksBelowLevel(chunkData, worldX, worldZ, existingLevel);

        // Find lowest sibling level (from neighbors)
        int lowestSiblingLevel = findLowestSiblingLevel(flat, localX, localZ, chunkData, world);

        // If neighbors are lower, fill down to avoid gaps
        if (lowestSiblingLevel < existingLevel) {
            // Fill down from existingLevel-1 to lowestSiblingLevel
            for (int y = existingLevel - 1; y >= lowestSiblingLevel; y--) {
                // Create new block with same type as top block
                Block block = Block.builder()
                        .position(Vector3Int.builder()
                                .x(worldX)
                                .y(y)
                                .z(worldZ)
                                .build())
                        .build();

                topBlockDefOpt.get().fillBlock(block);

                // Add to chunk
                LayerBlock layerBlock = LayerBlock.builder()
                        .block(block)
                        .build();
                chunkData.getBlocks().add(layerBlock);
            }

            log.debug("Filled NOT_SET column at ({},{}) from {} down to {}",
                     worldX, worldZ, existingLevel - 1, lowestSiblingLevel);
        }
    }

    /**
     * Get block definition string at a specific position.
     */
    private String getBlockDefAtPosition(LayerChunkData chunkData, int worldX, int worldZ, int y) {
        for (LayerBlock layerBlock : chunkData.getBlocks()) {
            Block block = layerBlock.getBlock();
            if (block == null || block.getPosition() == null) {
                continue;
            }
            Vector3Int pos = block.getPosition();
            if (pos.getX() == worldX && pos.getY() == y && pos.getZ() == worldZ) {
                // Reconstruct blockDef string from block
                return reconstructBlockDef(block);
            }
        }
        return null;
    }

    /**
     * Reconstruct a blockDef string from a Block.
     * Format: blockTypeId@s:state@r:rx,ry@l:level@f:faceVisibility
     */
    private String reconstructBlockDef(Block block) {
        StringBuilder sb = new StringBuilder();
        sb.append(block.getBlockTypeId());

        Integer status = block.getStatus();
        if (status != null && status != 0) {
            sb.append("@s:").append(status);
        }

        if (block.getRotation() != null) {
            sb.append("@r:").append(block.getRotation().getX()).append(",").append(block.getRotation().getY());
        }

        if (block.getLevel() != null) {
            sb.append("@l:").append(block.getLevel());
        }

        if (block.getFaceVisibility() != null) {
            sb.append("@f:").append(block.getFaceVisibility());
        }

        return sb.toString();
    }

    /**
     * Check if a block exists at a specific position.
     */
    private boolean hasBlockAtPosition(LayerChunkData chunkData, int worldX, int y, int worldZ) {
        for (LayerBlock layerBlock : chunkData.getBlocks()) {
            Block block = layerBlock.getBlock();
            if (block == null || block.getPosition() == null) {
                continue;
            }
            Vector3Int pos = block.getPosition();
            if (pos.getX() == worldX && pos.getY() == y && pos.getZ() == worldZ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Delete all blocks below a specific level in a column.
     * Keeps blocks at and above the specified level.
     */
    private void deleteBlocksBelowLevel(LayerChunkData chunkData, int worldX, int worldZ, int keepLevel) {
        chunkData.getBlocks().removeIf(layerBlock -> {
            Block block = layerBlock.getBlock();
            if (block == null || block.getPosition() == null) {
                return false;
            }
            Vector3Int pos = block.getPosition();
            // Remove if same X,Z and Y < keepLevel
            return pos.getX() == worldX && pos.getZ() == worldZ && pos.getY() < keepLevel;
        });
    }

    /**
     * Delete all blocks in a column (at specific X,Z for all Y levels).
     */
    private void deleteColumnBlocks(LayerChunkData chunkData, int worldX, int worldZ) {
        chunkData.getBlocks().removeIf(layerBlock -> {
            Block block = layerBlock.getBlock();
            if (block == null || block.getPosition() == null) {
                return false;
            }
            Vector3Int pos = block.getPosition();
            return pos.getX() == worldX && pos.getZ() == worldZ;
        });
    }

    /**
     * Find lowest level of sibling (neighboring) columns to avoid creating holes.
     * Checks all 8 neighboring positions in the flat.
     *
     * @return Lowest sibling level, or 0 if no neighbors found
     */
    private int findLowestSiblingLevel(WFlat flat, int localX, int localZ,
                                       LayerChunkData chunkData, WWorld world) {
        int lowestLevel = Integer.MAX_VALUE;
        boolean foundSibling = false;

        // Check all 8 neighbors
        int[][] offsets = {
                {-1, -1}, {0, -1}, {1, -1},
                {-1, 0},           {1, 0},
                {-1, 1},  {0, 1},  {1, 1}
        };

        for (int[] offset : offsets) {
            int neighborX = localX + offset[0];
            int neighborZ = localZ + offset[1];

            // Check if neighbor is within flat bounds
            if (neighborX >= 0 && neighborX < flat.getSizeX() &&
                neighborZ >= 0 && neighborZ < flat.getSizeZ()) {

                // Check if neighbor column is defined
                if (flat.isColumnSet(neighborX, neighborZ)) {
                    int neighborLevel = flat.getLevel(neighborX, neighborZ);
                    if (neighborLevel < lowestLevel) {
                        lowestLevel = neighborLevel;
                        foundSibling = true;
                    }
                }
            } else {
                // Check in existing chunk data (outside flat bounds)
                int worldX = flat.getMountX() + neighborX;
                int worldZ = flat.getMountZ() + neighborZ;
                int existingLevel = findHighestBlockAtPosition(chunkData, worldX, worldZ);
                if (existingLevel != -1 && existingLevel < lowestLevel) {
                    lowestLevel = existingLevel;
                    foundSibling = true;
                }
            }
        }

        return foundSibling ? lowestLevel : 0;
    }

    /**
     * Find highest block Y at a specific X,Z position in chunk data.
     *
     * @return Highest Y coordinate, or -1 if no block found
     */
    private int findHighestBlockAtPosition(LayerChunkData chunkData, int worldX, int worldZ) {
        int highestY = -1;

        for (LayerBlock layerBlock : chunkData.getBlocks()) {
            Block block = layerBlock.getBlock();
            if (block == null || block.getPosition() == null) {
                continue;
            }

            Vector3Int pos = block.getPosition();
            if (pos.getX() == worldX && pos.getZ() == worldZ) {
                if (pos.getY() > highestY) {
                    highestY = pos.getY();
                }
            }
        }

        return highestY;
    }

    /**
     * Fill a column with blocks from level down to lowestSiblingLevel.
     * Uses column definition to determine block types.
     */
    private void fillColumn(LayerChunkData chunkData, int worldX, int worldZ,
                            int level, int lowestSiblingLevel,
                            WFlat.MaterialDefinition columnDef, WFlat flat) {
        // Get extra blocks for this column
        String[] extraBlocks = flat.getExtraBlocksForColumn(
                worldX - flat.getMountX(),
                worldZ - flat.getMountZ()
        );

        // Fill from level down to lowestSiblingLevel
        for (int y = level; y >= lowestSiblingLevel; y--) {
            // Get block definition for this Y level
            String blockDefString = columnDef.getBlockAt(flat, level, y, extraBlocks);

            if (blockDefString == null || blockDefString.equals("0") || blockDefString.isBlank()) {
                // Air block, skip
                continue;
            }

            // Create block with position
            Block block = Block.builder()
                    .position(Vector3Int.builder()
                            .x(worldX)
                            .y(y)
                            .z(worldZ)
                            .build())
                    .build();

            // Parse and apply block definition
            Optional<BlockDef> blockDefOpt = BlockDef.of(blockDefString);
            if (blockDefOpt.isEmpty()) {
                log.warn("Invalid block definition: {} at ({},{},{})", blockDefString, worldX, y, worldZ);
                continue;
            }
            blockDefOpt.get().fillBlock(block);

            // Wrap in LayerBlock and add to chunk
            LayerBlock layerBlock = LayerBlock.builder()
                    .block(block)
                    .build();

            chunkData.getBlocks().add(layerBlock);
        }
    }
}
