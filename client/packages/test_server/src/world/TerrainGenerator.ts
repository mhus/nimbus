/**
 * Terrain Generator - Simple flat plane for testing
 */

import { ServerChunk, ChunkStatus } from '../types/ServerChunk';

export class TerrainGenerator {
  /**
   * Generate a flat plane at Y=0 with stone (blockTypeId: 220)
   */
  generateChunk(cx: number, cz: number, chunkSize: number): ServerChunk {
    const chunk = new ServerChunk(cx, cz, chunkSize);

    // Fill Y=0 with stone (blockTypeId: 220)
    const worldX = cx * chunkSize;
    const worldZ = cz * chunkSize;

    // Create height map for this chunk and surrounding area (needed for smoothing)
    const heightMap: number[][] = [];
    for (let x = -1; x <= chunkSize; x++) {
      heightMap[x] = [];
      for (let z = -1; z <= chunkSize; z++) {
        // For now, flat terrain at Y=0
        heightMap[x][z] = 0;
      }
    }

    for (let localX = 0; localX < chunkSize; localX++) {
      for (let localZ = 0; localZ < chunkSize; localZ++) {
        chunk.setBlock({
          position: {
            x: worldX + localX,
            y: 0,
            z: worldZ + localZ,
          },
          blockTypeId: '220', // Stone
        });
      }
    }

    // Apply terrain smoothing (edge offsets)
    this.applyTerrainSmoothing(chunk, heightMap, chunkSize);

    chunk.status = ChunkStatus.READY;
    return chunk;
  }

  /**
   * Apply edge offsets to smooth terrain transitions
   *
   * Iterates through terrain surface blocks and calculates edge offsets based on
   * neighboring terrain heights to create smooth transitions.
   *
   * @param chunk The chunk to apply smoothing to
   * @param heightMap Height map including surrounding blocks (indices from -1 to chunkSize)
   * @param chunkSize Size of the chunk
   */
  private applyTerrainSmoothing(
    chunk: ServerChunk,
    heightMap: number[][],
    chunkSize: number
  ): void {
    const worldX = chunk.cx * chunkSize;
    const worldZ = chunk.cz * chunkSize;

    for (let localX = 0; localX < chunkSize; localX++) {
      for (let localZ = 0; localZ < chunkSize; localZ++) {
        const terrainHeight = heightMap[localX][localZ];

        // Only apply smoothing to top surface blocks (terrain height and 1-2 blocks below)
        for (
          let y = Math.max(0, terrainHeight - 2);
          y <= terrainHeight;
          y++
        ) {
          const block = chunk.getBlock(worldX + localX, y, worldZ + localZ);

          // Skip if no block exists (air)
          if (!block) continue;

          // Calculate edge offsets based on neighboring heights
          const offsets = this.calculateOffsets(
            localX,
            y,
            localZ,
            heightMap,
            terrainHeight
          );

          if (offsets) {
            // Update block with offsets
            chunk.setBlock({
              ...block,
              offsets,
            });
          }
        }
      }
    }
  }

  /**
   * Calculate edge offsets for a block based on neighboring terrain heights
   *
   * Uses the 8 neighboring blocks to determine how to adjust each corner of the block
   * to create smooth terrain transitions.
   *
   * @param x Local X coordinate within chunk
   * @param y World Y coordinate
   * @param z Local Z coordinate within chunk
   * @param heightMap Height map with indices from -1 to chunkSize
   * @param centerHeight Terrain height at this position
   * @returns Array of 24 offset values (8 corners × 3 axes) in block units (-1.0 to 1.0) or null if no offsets needed
   */
  private calculateOffsets(
    x: number,
    y: number,
    z: number,
    heightMap: number[][],
    centerHeight: number
  ): number[] | null {
    // Get heights of 8 neighboring blocks (and center)
    const heights = [
      heightMap[x - 1]?.[z - 1] ?? centerHeight, // NW
      heightMap[x]?.[z - 1] ?? centerHeight, // N
      heightMap[x + 1]?.[z - 1] ?? centerHeight, // NE
      heightMap[x - 1]?.[z] ?? centerHeight, // W
      centerHeight, // Center
      heightMap[x + 1]?.[z] ?? centerHeight, // E
      heightMap[x - 1]?.[z + 1] ?? centerHeight, // SW
      heightMap[x]?.[z + 1] ?? centerHeight, // S
      heightMap[x + 1]?.[z + 1] ?? centerHeight, // SE
    ];

    // Calculate offsets for 8 corners
    // Corner order: bottom 4, then top 4
    // Offsets are in block units (float): -1.0 to 1.0
    const offsets: number[] = new Array(24).fill(0);

    // Bottom corners (y offset only, based on height difference)
    const yDiff = y - centerHeight;

    // Bottom corners: pull down if below terrain, push up if above
    // Scale factor controls smoothness (0.3 = moderate smoothing)
    const bottomOffset = Math.max(-1.0, Math.min(1.0, yDiff * 0.3));

    // 0: left-back-bottom (influenced by W, NW, N)
    offsets[1] = bottomOffset;

    // 1: right-back-bottom (influenced by E, NE, N)
    offsets[4] = bottomOffset;

    // 2: right-front-bottom (influenced by E, SE, S)
    offsets[7] = bottomOffset;

    // 3: left-front-bottom (influenced by W, SW, S)
    offsets[10] = bottomOffset;

    // Top corners: adjust based on neighboring terrain
    const topYDiff = y + 1 - centerHeight;
    const topOffset = Math.max(-1.0, Math.min(1.0, topYDiff * 0.3));

    // 4: left-back-top (influenced by W, NW, N)
    const h4 = (heights[3] + heights[0] + heights[1]) / 3;
    offsets[13] = Math.max(
      -1.0,
      Math.min(1.0, (h4 - centerHeight) * 0.24 + topOffset)
    );

    // 5: right-back-top (influenced by E, NE, N)
    const h5 = (heights[5] + heights[2] + heights[1]) / 3;
    offsets[16] = Math.max(
      -1.0,
      Math.min(1.0, (h5 - centerHeight) * 0.24 + topOffset)
    );

    // 6: right-front-top (influenced by E, SE, S)
    const h6 = (heights[5] + heights[8] + heights[7]) / 3;
    offsets[19] = Math.max(
      -1.0,
      Math.min(1.0, (h6 - centerHeight) * 0.24 + topOffset)
    );

    // 7: left-front-top (influenced by W, SW, S)
    const h7 = (heights[3] + heights[6] + heights[7]) / 3;
    offsets[22] = Math.max(
      -1.0,
      Math.min(1.0, (h7 - centerHeight) * 0.24 + topOffset)
    );

    // Check if any offset is non-zero
    const hasOffset = offsets.some((o) => o !== 0);
    return hasOffset ? offsets : null;
  }
}
