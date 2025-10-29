/**
 * FlatGenerator - Simple flat terrain generator
 *
 * Generates a flat plane at specified height with configurable layers.
 */

import { getLogger } from '@nimbus/shared';
import { ServerChunk } from '../../types/ServerChunk';
import type { WorldGenerator, GeneratorConfig } from './WorldGenerator';
import type { BlockTypeRegistry } from '../BlockTypeRegistry';

const logger = getLogger('FlatGenerator');

/**
 * Flat terrain generator
 *
 * Generates simple flat worlds with customizable layers.
 */
export class FlatGenerator implements WorldGenerator {
  readonly name = 'flat';

  private seed: number;
  private registry: BlockTypeRegistry;

  // Configuration
  private groundLevel: number;
  private layers: Array<{ blockType: string; thickness: number }>;

  constructor(config: GeneratorConfig, registry: BlockTypeRegistry) {
    this.seed = config.seed ?? Date.now();
    this.registry = registry;

    // Load parameters
    const params = config.parameters || {};
    this.groundLevel = params.groundLevel ?? 64;
    this.layers = params.layers || [
      { blockType: 'bedrock', thickness: 1 },
      { blockType: 'stone', thickness: 50 },
      { blockType: 'dirt', thickness: 3 },
      { blockType: 'grass', thickness: 1 },
    ];

    logger.info('FlatGenerator initialized', {
      groundLevel: this.groundLevel,
      layers: this.layers.length,
      seed: this.seed,
    });
  }

  /**
   * Generate a flat chunk
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

        // Place layers from bottom up
        let currentY = 0;

        for (const layer of this.layers) {
          // Get block type ID
          const blockType = this.registry.getBlockType(this.getBlockIdByName(layer.blockType));
          if (!blockType) {
            logger.warn('BlockType not found', { name: layer.blockType });
            continue;
          }

          // Place layer
          for (let i = 0; i < layer.thickness; i++) {
            const worldY = currentY + i;

            // Only generate up to ground level
            if (worldY < this.groundLevel) {
              chunk.setBlock({
                position: { x: worldX, y: worldY, z: worldZ },
                blockTypeId: blockType.id,
                status: 0,
              });
            }
          }

          currentY += layer.thickness;

          // Stop if we've reached ground level
          if (currentY >= this.groundLevel) {
            break;
          }
        }
      }
    }

    logger.debug('Generated flat chunk', { cx, cz, blockCount: chunk.getBlockCount() });

    return chunk;
  }

  /**
   * Get block ID by name (simple lookup)
   * TODO: Improve this with proper registry lookup
   */
  private getBlockIdByName(name: string): number {
    // Simple mapping for common blocks
    const mapping: Record<string, number> = {
      bedrock: 220, // Example IDs - should come from registry
      stone: 527,
      dirt: 258,
      grass: 280,
    };

    return mapping[name] ?? 0;
  }
}
