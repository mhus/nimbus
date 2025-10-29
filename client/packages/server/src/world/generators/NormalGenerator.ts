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

    for (const name of blockNames) {
      const id = this.findBlockIdByName(name);
      if (id !== null) {
        this.blockIds.set(name, id);
        logger.debug('Resolved block ID', { name, id });
      } else {
        logger.warn('Could not resolve block ID', { name });
      }
    }
  }

  /**
   * Find block ID by name
   */
  private findBlockIdByName(name: string): number | null {
    // Try to load all block types and find by name
    // This is inefficient but works for initialization
    const allBlockTypes = this.registry.getAllBlockTypes();

    for (const blockType of allBlockTypes) {
      if (blockType.name?.toLowerCase() === name.toLowerCase()) {
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

    // Generate blocks for each XZ position
    for (let localX = 0; localX < chunkSize; localX++) {
      for (let localZ = 0; localZ < chunkSize; localZ++) {
        const worldX = worldBaseX + localX;
        const worldZ = worldBaseZ + localZ;

        // Get terrain height at this position
        const terrainHeight = this.getHeight(worldX, worldZ);

        // Generate column
        for (let worldY = 0; worldY <= terrainHeight; worldY++) {
          let blockTypeId: number;

          // Layer selection
          if (worldY === 0) {
            // Bedrock at bottom
            blockTypeId = this.getBlockId('bedrock');
          } else if (worldY === terrainHeight) {
            // Surface layer
            if (terrainHeight <= this.waterLevel) {
              // Underwater: sand
              blockTypeId = this.getBlockId('sand');
            } else {
              // Above water: grass
              blockTypeId = this.getBlockId('grass');
            }
          } else if (worldY >= terrainHeight - 3) {
            // Subsurface (3 blocks below surface): dirt
            blockTypeId = this.getBlockId('dirt');
          } else {
            // Deep underground: stone
            blockTypeId = this.getBlockId('stone');
          }

          // Skip air blocks
          if (blockTypeId === 0) continue;

          // Add block
          chunk.setBlock({
            position: { x: worldX, y: worldY, z: worldZ },
            blockTypeId,
            status: 0,
          });
        }

        // Add water surface if terrain is below water level
        if (terrainHeight < this.waterLevel) {
          const waterBlockId = this.getBlockId('water');
          if (waterBlockId !== 0) {
            chunk.setBlock({
              position: { x: worldX, y: this.waterLevel, z: worldZ },
              blockTypeId: waterBlockId,
              status: 0,
            });
          }
        }
      }
    }

    logger.debug('Generated normal chunk', { cx, cz, blockCount: chunk.getBlockCount() });

    return chunk;
  }
}
