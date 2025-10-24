/**
 * Normal (terrain) world generator with simplex noise
 */

import { makeNoise2D } from 'open-simplex-noise';
import type { ChunkData, XZ } from '@voxel-02/core';
import { setBlockEdgeOffsets } from '@voxel-02/core';
import type { WorldGenerator } from './WorldGenerator.js';
import type { Registry } from '../../registry/Registry.js';

/**
 * Terrain generator using simplex noise
 */
export class NormalWorldGenerator implements WorldGenerator {
  readonly name = 'normal';

  private seed: number;
  private registry: Registry;
  private noise2D: ReturnType<typeof makeNoise2D>;

  // Block IDs (resolved from registry)
  private grassBlockID: number = 0;
  private dirtBlockID: number = 0;
  private stoneBlockID: number = 0;
  private sandBlockID: number = 0;
  private waterBlockID: number = 0;
  private lavaBlockID: number = 0;

  private waterLevel = 62;
  private baseHeight = 64;
  private heightVariation = 32;

  constructor(seed: number, registry: Registry) {
    this.seed = seed;
    this.registry = registry;
    this.noise2D = makeNoise2D(seed);

    // Resolve block IDs from registry
    this.resolveBlockIDs();
  }

  /**
   * Resolve block names to IDs from registry
   */
  private resolveBlockIDs(): void {
    this.grassBlockID = this.registry.getBlockID('grass') ?? 0;
    this.dirtBlockID = this.registry.getBlockID('dirt') ?? 0;
    this.stoneBlockID = this.registry.getBlockID('stone') ?? 0;
    this.sandBlockID = this.registry.getBlockID('sand') ?? 0;
    this.waterBlockID = this.registry.getBlockID('water') ?? 0;
    this.lavaBlockID = this.registry.getBlockID('lava') ?? 0;

    console.log(`[NormalWorldGenerator] Block IDs: grass=${this.grassBlockID}, dirt=${this.dirtBlockID}, stone=${this.stoneBlockID}, sand=${this.sandBlockID}, water=${this.waterBlockID}, lava=${this.lavaBlockID}`);
  }

  /**
   * Get height at world position
   */
  private getHeight(worldX: number, worldZ: number): number {
    // Multiple octaves of noise for more interesting terrain
    const scale1 = 0.01;
    const scale2 = 0.05;
    const scale3 = 0.1;

    const noise1 = this.noise2D(worldX * scale1, worldZ * scale1);
    const noise2 = this.noise2D(worldX * scale2, worldZ * scale2);
    const noise3 = this.noise2D(worldX * scale3, worldZ * scale3);

    // Combine octaves
    const combined = (noise1 * 0.6 + noise2 * 0.3 + noise3 * 0.1);

    // Convert to height (noise is in range [-1, 1])
    const height = this.baseHeight + combined * this.heightVariation;

    return Math.floor(height);
  }

  async generateChunk(chunkX: number, chunkZ: number, chunkSize: number, height: number): Promise<ChunkData> {
    const size = chunkSize * chunkSize * height;
    const data = new Uint16Array(size);

    // Create height map for chunk and surrounding area
    const heightMap: number[][] = [];
    for (let x = -1; x <= chunkSize; x++) {
      heightMap[x] = [];
      for (let z = -1; z <= chunkSize; z++) {
        const worldX = chunkX * chunkSize + x;
        const worldZ = chunkZ * chunkSize + z;
        heightMap[x][z] = this.getHeight(worldX, worldZ);
      }
    }

    // Generate terrain
    for (let x = 0; x < chunkSize; x++) {
      for (let z = 0; z < chunkSize; z++) {
        const worldX = chunkX * chunkSize + x;
        const worldZ = chunkZ * chunkSize + z;

        const terrainHeight = heightMap[x][z];

        for (let y = 0; y < height; y++) {
          const index = x + y * chunkSize + z * chunkSize * height;

          // Only generate top 2 layers of terrain and surface liquids
          if (y === 0) {
            // Bottom layer: Single layer of lava
            data[index] = this.lavaBlockID;
          } else if (y === terrainHeight - 1 && terrainHeight > 1) {
            // Top surface layer
            if (terrainHeight <= this.waterLevel) {
              // Underwater: sand
              data[index] = this.sandBlockID;
            } else {
              // Above water: grass
              data[index] = this.grassBlockID;
            }
          } else if (y === terrainHeight - 2 && terrainHeight > 2) {
            // Second layer below surface: dirt
            data[index] = this.dirtBlockID;
          } else if (y === this.waterLevel && terrainHeight < this.waterLevel) {
            // Only on water surface level and where terrain is below water
            data[index] = this.waterBlockID;
          } else {
            // Air
            data[index] = 0;
          }
        }
      }
    }

    const chunk: ChunkData = {
      chunkX,
      chunkZ,
      data,
      height,
    };

    // Apply edge offsets for terrain smoothing
    this.applyTerrainSmoothing(chunk, heightMap, chunkSize, height);

    return chunk;
  }

  /**
   * Apply edge offsets to smooth terrain transitions
   */
  private applyTerrainSmoothing(chunk: ChunkData, heightMap: number[][], chunkSize: number, height: number): void {
    for (let x = 0; x < chunkSize; x++) {
      for (let z = 0; z < chunkSize; z++) {
        const terrainHeight = heightMap[x][z];

        // Only apply smoothing to top surface blocks
        for (let y = Math.max(0, terrainHeight - 2); y <= Math.min(height - 1, terrainHeight); y++) {
          const index = x + y * chunkSize + z * chunkSize * height;
          const blockId = chunk.data[index];

          // Skip air blocks
          if (blockId === 0) continue;

          // Calculate edge offsets based on neighboring heights
          const offsets = this.calculateEdgeOffsets(x, y, z, heightMap, terrainHeight);

          if (offsets) {
            setBlockEdgeOffsets(chunk, x, y, z, offsets, chunkSize);
          }
        }
      }
    }
  }

  /**
   * Calculate edge offsets for a block based on neighboring terrain heights
   */
  private calculateEdgeOffsets(
    x: number,
    y: number,
    z: number,
    heightMap: number[][],
    centerHeight: number
  ): number[] | null {
    // Get heights of 8 neighboring blocks (and center)
    const heights = [
      heightMap[x - 1]?.[z - 1] ?? centerHeight,  // NW
      heightMap[x]?.[z - 1] ?? centerHeight,      // N
      heightMap[x + 1]?.[z - 1] ?? centerHeight,  // NE
      heightMap[x - 1]?.[z] ?? centerHeight,      // W
      centerHeight,                               // Center
      heightMap[x + 1]?.[z] ?? centerHeight,      // E
      heightMap[x - 1]?.[z + 1] ?? centerHeight,  // SW
      heightMap[x]?.[z + 1] ?? centerHeight,      // S
      heightMap[x + 1]?.[z + 1] ?? centerHeight,  // SE
    ];

    // Calculate offsets for 8 corners
    // Corner order matches ChunkRenderer: bottom 4, then top 4
    const offsets: number[] = new Array(24).fill(0);

    // Bottom corners (y offset only, based on height difference)
    const yDiff = y - centerHeight;

    // Bottom corners: pull down if below terrain, push up if above
    const bottomOffset = Math.max(-127, Math.min(127, yDiff * 40));

    // 0: left-back-bottom (influenced by W, NW, N)
    offsets[1] = bottomOffset;

    // 1: right-back-bottom (influenced by E, NE, N)
    offsets[4] = bottomOffset;

    // 2: right-front-bottom (influenced by E, SE, S)
    offsets[7] = bottomOffset;

    // 3: left-front-bottom (influenced by W, SW, S)
    offsets[10] = bottomOffset;

    // Top corners: adjust based on neighboring terrain
    const topYDiff = (y + 1) - centerHeight;
    const topOffset = Math.max(-127, Math.min(127, topYDiff * 40));

    // 4: left-back-top (influenced by W, NW, N)
    const h4 = (heights[3] + heights[0] + heights[1]) / 3;
    offsets[13] = Math.max(-127, Math.min(127, (h4 - centerHeight) * 30 + topOffset));

    // 5: right-back-top (influenced by E, NE, N)
    const h5 = (heights[5] + heights[2] + heights[1]) / 3;
    offsets[16] = Math.max(-127, Math.min(127, (h5 - centerHeight) * 30 + topOffset));

    // 6: right-front-top (influenced by E, SE, S)
    const h6 = (heights[5] + heights[8] + heights[7]) / 3;
    offsets[19] = Math.max(-127, Math.min(127, (h6 - centerHeight) * 30 + topOffset));

    // 7: left-front-top (influenced by W, SW, S)
    const h7 = (heights[3] + heights[6] + heights[7]) / 3;
    offsets[22] = Math.max(-127, Math.min(127, (h7 - centerHeight) * 30 + topOffset));

    // Check if any offset is non-zero
    const hasOffset = offsets.some(o => o !== 0);
    return hasOffset ? offsets : null;
  }
}
