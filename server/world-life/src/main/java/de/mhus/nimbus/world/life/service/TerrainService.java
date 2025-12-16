package de.mhus.nimbus.world.life.service;

import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.shared.world.BlockUtil;
import de.mhus.nimbus.world.shared.world.WChunkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for accessing terrain data (chunk blocks) for entity positioning.
 * Provides ground height lookup and block queries for terrain-aware movement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TerrainService {

    private final WChunkService chunkService;

    /**
     * Get ground height at world position (x, z).
     * Searches downward from startY to find the highest solid block.
     *
     * @param worldId World identifier
     * @param x X coordinate (world space)
     * @param z Z coordinate (world space)
     * @param startY Starting Y coordinate for downward search
     * @return Y coordinate of ground surface (top of highest solid block), or 64 if not found
     */
    public int getGroundHeight(WorldId worldId, int x, int z, int startY) {
        try {
            // Calculate chunk coordinates
            int chunkX = Math.floorDiv(x, 16);
            int chunkZ = Math.floorDiv(z, 16);
            String chunkKey = BlockUtil.toChunkKey(chunkX, chunkZ);

            // Load chunk from database (regionId = worldId for main world, create=false)
            Optional<ChunkData> chunkDataOpt = chunkService.loadChunkData(worldId, chunkKey, false);

            if (chunkDataOpt.isEmpty()) {
                log.trace("Chunk not found for ground height lookup: world={}, chunk={}", worldId, chunkKey);
                return 64; // Default ground level
            }

            ChunkData chunkData = chunkDataOpt.get();

            // Search downward from startY to find highest solid block
            for (int y = startY; y >= 0; y--) {
                Optional<Block> blockOpt = getBlockAt(chunkData, x, y, z);

                if (blockOpt.isPresent()) {
                    Block block = blockOpt.get();
                    String blockTypeId = block.getBlockTypeId();

                    if (isSolidBlock(blockTypeId)) {
                        // Found solid block, return Y + 1 (stand on top)
                        log.trace("Ground height found at ({}, {}, {}): y={}", x, y, z, y + 1);
                        return y + 1;
                    }
                }
            }

            // No solid block found, use default ground level
            log.trace("No solid block found at ({}, {}), using default ground level", x, z);
            return 64;

        } catch (Exception e) {
            log.error("Error getting ground height at ({}, {})", x, z, e);
            return 64; // Fallback to default
        }
    }

    /**
     * Get block at specific world coordinates.
     *
     * @param chunkData Chunk data
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @return Optional containing the block if found
     */
    private Optional<Block> getBlockAt(ChunkData chunkData, int worldX, int worldY, int worldZ) {
        if (chunkData.getBlocks() == null) {
            return Optional.empty();
        }

        // Search for block with matching position
        for (Block block : chunkData.getBlocks()) {
            if (block.getPosition() == null) continue;

            var pos = block.getPosition();
            if (pos.getX() == worldX && pos.getY() == worldY && pos.getZ() == worldZ) {
                return Optional.of(block);
            }
        }

        return Optional.empty();
    }

    /**
     * Check if block type ID represents a solid block.
     *
     * @param blockTypeId Block type identifier
     * @return True if block is solid (not air or null)
     */
    public boolean isSolidBlock(String blockTypeId) {
        // "0" is air block, null is also air
        return blockTypeId != null && !blockTypeId.equals("0") && !blockTypeId.isBlank();
    }

    /**
     * Check if position is valid (within world bounds).
     *
     * @param y Y coordinate
     * @return True if within valid range
     */
    public boolean isValidHeight(int y) {
        return y >= 0 && y <= 255;
    }
}
