/**
 * GeneratorFactory - Creates terrain generators from configuration
 */

import { getLogger } from '@nimbus/shared';
import type { WorldGenerator, GeneratorConfig } from './WorldGenerator';
import type { BlockTypeRegistry } from '../BlockTypeRegistry';
import { FlatGenerator } from './FlatGenerator';
import { NormalGenerator } from './NormalGenerator';

const logger = getLogger('GeneratorFactory');

/**
 * Factory for creating terrain generators
 */
export class GeneratorFactory {
  /**
   * Create a generator from configuration
   *
   * @param config Generator configuration
   * @param registry Block type registry
   * @returns WorldGenerator instance
   */
  static createGenerator(config: GeneratorConfig, registry: BlockTypeRegistry): WorldGenerator {
    logger.info('Creating generator', { type: config.type, seed: config.seed });

    switch (config.type.toLowerCase()) {
      case 'flat':
        return new FlatGenerator(config, registry);

      case 'normal':
        return new NormalGenerator(config, registry);

      default:
        logger.warn('Unknown generator type, falling back to flat', { type: config.type });
        return new FlatGenerator(config, registry);
    }
  }

  /**
   * Load generator configuration from file
   *
   * @param filePath Path to generator.json
   * @returns Generator configuration or null if file doesn't exist
   */
  static async loadConfig(filePath: string): Promise<GeneratorConfig | null> {
    try {
      const fs = await import('fs');
      const path = await import('path');

      if (!fs.existsSync(filePath)) {
        return null;
      }

      const content = fs.readFileSync(filePath, 'utf-8');
      const config = JSON.parse(content) as GeneratorConfig;

      logger.debug('Loaded generator config', { type: config.type, from: filePath });

      return config;
    } catch (error) {
      logger.error('Failed to load generator config', { filePath }, error as Error);
      return null;
    }
  }

  /**
   * Create default generator configuration
   *
   * @param type Generator type
   * @param seed Random seed
   * @returns Default generator configuration
   */
  static createDefaultConfig(type: string = 'normal', seed?: number): GeneratorConfig {
    const config: GeneratorConfig = {
      type,
      seed: seed ?? Date.now(),
      parameters: {},
    };

    // Add default parameters based on type
    switch (type.toLowerCase()) {
      case 'flat':
        config.parameters = {
          groundLevel: 64,
          layers: [
            { blockType: 'bedrock', thickness: 1 },
            { blockType: 'stone', thickness: 50 },
            { blockType: 'dirt', thickness: 3 },
            { blockType: 'grass', thickness: 1 },
          ],
        };
        break;

      case 'normal':
        config.parameters = {
          waterLevel: 62,
          baseHeight: 64,
          heightVariation: 32,
        };
        break;
    }

    return config;
  }
}
