/**
 * NormalGenerator - Terrain generator with simplex noise
 *
 * Generates hilly terrain using multiple octaves of simplex noise.
 * Based on client_playground implementation.
 */

import { makeNoise2D } from 'open-simplex-noise';
import { getLogger } from '@nimbus/shared';
import { ServerChunk } from '../../types/ServerChunk';
import type { WorldGenerator, GeneratorConfig } from './WorldGenerator';
import type { BlockTypeRegistry } from '../BlockTypeRegistry';

const logger = getLogger('NormalGenerator');

/**
 * Normal terrain generator with simplex noise
 */
export class NormalGenerator implements WorldGenerator {
  readonly name = 'normal';

  private seed: number;
  private registry: BlockTypeRegistry;
  private noise2D: ReturnType<typeof makeNoise2D>;

  // Configuration
  private waterLevel: number;
  private baseHeight: number;
  private heightVariation: number;

  // Block type IDs (resolved from names)
  private blockIds: Map<string, number> = new Map();

  constructor(config: GeneratorConfig, registry: BlockTypeRegistry) {
    this.seed = config.seed ?? Date.now();
    this.registry = registry;
    this.noise2D = makeNoise2D(this.seed);

    // Load parameters
    const params = config.parameters || {};
    this.waterLevel = params.waterLevel ?? 62;
    this.baseHeight = params.baseHeight ?? 64;
    this.heightVariation = params.heightVariation ?? 32;

    // Resolve block IDs
    this.resolveBlockIds();

    logger.info('NormalGenerator initialized', {
      seed: this.seed,
      waterLevel: this.waterLevel,
      baseHeight: this.baseHeight,
      heightVariation: this.heightVariation,
    });
  }

  /**
   * Resolve block names to IDs
   */
  private resolveBlockIds(): void {
    const blockNames = ['grass', 'dirt', 'stone', 'sand', 'water', 'bedrock'];
    const blockIds = [ 310, 279, 552, 520, 5000, 127 ];

    for (let i = 0; i < blockNames.length; i++)
        this.blockIds.set(blockNames[i], blockIds[i]);
  }

  /**
   * Find block ID by name
   */
  private findBlockIdByName(name: string): number | null {
    // Try to load all block types and find by description
    // This is inefficient but works for initialization
    const allBlockTypes = this.registry.getAllBlockTypes();

    for (const blockType of allBlockTypes) {
      // Search in description (case-insensitive)
      if (blockType.description?.toLowerCase().includes(name.toLowerCase())) {
        return blockType.id;
      }
    }

    return null;
  }

  /**
   * Get block ID by name
   */
  private getBlockId(name: string): number {
    return this.blockIds.get(name) ?? 0;
  }

  /**
   * Get terrain height at world position using simplex noise
   */
  private getHeight(worldX: number, worldZ: number): number {
    // Multiple octaves of noise for more interesting terrain
    const scale1 = 0.01; // Large features (hills)
    const scale2 = 0.05; // Medium features
    const scale3 = 0.1; // Small features (detail)

    const noise1 = this.noise2D(worldX * scale1, worldZ * scale1);
    const noise2 = this.noise2D(worldX * scale2, worldZ * scale2);
    const noise3 = this.noise2D(worldX * scale3, worldZ * scale3);

    // Combine octaves (weighted)
    const combined = noise1 * 0.6 + noise2 * 0.3 + noise3 * 0.1;

    // Convert to height (noise is in range [-1, 1])
    const height = this.baseHeight + combined * this.heightVariation;

    return Math.floor(height);
  }

  /**
   * Generate a chunk with hilly terrain
   */
  generateChunk(cx: number, cz: number, chunkSize: number): ServerChunk {
    const chunk = new ServerChunk(cx, cz, chunkSize);

    // Calculate world coordinates for this chunk
    const worldBaseX = cx * chunkSize;
    const worldBaseZ = cz * chunkSize;

    // Create height map for this chunk and surrounding area (needed for smoothing)
    const heightMap: number[][] = [];
    for (let x = -1; x <= chunkSize; x++) {
      heightMap[x] = [];
      for (let z = -1; z <= chunkSize; z++) {
        const worldX = worldBaseX + x;
        const worldZ = worldBaseZ + z;
        heightMap[x][z] = this.getHeight(worldX, worldZ);
      }
    }

    // Generate blocks for each XZ position
    for (let localX = 0; localX < chunkSize; localX++) {
      for (let localZ = 0; localZ < chunkSize; localZ++) {
        const worldX = worldBaseX + localX;
        const worldZ = worldBaseZ + localZ;

        // Get terrain height at this position
        const terrainHeight = heightMap[localX][localZ];

        // Only generate top 2 layers of terrain and surface liquids
        // This keeps the world sparse and efficient

        // Top surface layer (terrainHeight)
        let surfaceBlockId: number;

        if (terrainHeight <= this.waterLevel) {
          // Underwater: sand
          surfaceBlockId = this.getBlockId('sand');
        } else {
          // Above water: grass
          surfaceBlockId = this.getBlockId('grass');
        }

        if (surfaceBlockId !== 0) {
          chunk.setBlock({
            position: { x: worldX, y: terrainHeight, z: worldZ },
            blockTypeId: surfaceBlockId,
          });
        }

        // Second layer below surface (terrainHeight - 1)
        const dirtId = this.getBlockId('dirt');
        if (dirtId !== 0 && terrainHeight > 0) {
          chunk.setBlock({
            position: { x: worldX, y: terrainHeight - 1, z: worldZ },
            blockTypeId: dirtId,
          });
        }

        // Add water surface if terrain is below water level
        if (terrainHeight < this.waterLevel) {
          const waterBlockId = this.getBlockId('water');
          if (waterBlockId !== 0) {
            chunk.setBlock({
              position: { x: worldX, y: this.waterLevel, z: worldZ },
              blockTypeId: waterBlockId,
            });
          }
        }
      }
    }

    // Apply terrain smoothing (edge offsets)
    this.applyTerrainSmoothing(chunk, heightMap, chunkSize);

    logger.debug('Generated normal chunk', { cx, cz, blockCount: chunk.getBlockCount() });

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
