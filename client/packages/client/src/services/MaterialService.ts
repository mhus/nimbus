/**
 * MaterialService - Manages Babylon.js materials for rendering
 *
 * Creates and caches materials for blocks.
 * Integrates with TextureAtlas for texture management.
 * Supports future shader effects via ShaderService.
 */

import { StandardMaterial, Scene } from '@babylonjs/core';
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { TextureAtlas } from '../rendering/TextureAtlas';

const logger = getLogger('MaterialService');

/**
 * Material type for rendering
 */
export enum MaterialType {
  STANDARD = 'standard',
  // Future material types will be added here based on BlockModifier.visibility.effect:
  // WATER = 'water',
  // LAVA = 'lava',
  // WIND = 'wind',
  // FOG = 'fog',
}

/**
 * MaterialService - Manages materials for rendering
 *
 * Initial implementation provides standard material only.
 * Future versions will support shader-based materials via ShaderService.
 */
export class MaterialService {
  private scene: Scene;
  private appContext: AppContext;
  private textureAtlas?: TextureAtlas;

  // Material cache
  private materials: Map<string, StandardMaterial> = new Map();

  constructor(scene: Scene, appContext: AppContext) {
    this.scene = scene;
    this.appContext = appContext;

    logger.info('MaterialService initialized');
  }

  /**
   * Set the texture atlas
   *
   * Must be called before creating materials
   */
  setTextureAtlas(textureAtlas: TextureAtlas): void {
    this.textureAtlas = textureAtlas;
    logger.debug('Texture atlas set');
  }

  /**
   * Get or create a standard material
   *
   * @param name Material name
   * @returns Standard material
   */
  getStandardMaterial(name: string = 'default'): StandardMaterial {
    const cacheKey = `standard:${name}`;

    // Check cache
    if (this.materials.has(cacheKey)) {
      return this.materials.get(cacheKey)!;
    }

    // Create new material
    try {
      if (!this.textureAtlas) {
        throw new Error('TextureAtlas not set');
      }

      const atlasMaterial = this.textureAtlas.getMaterial();
      if (!atlasMaterial) {
        throw new Error('Atlas material not available');
      }

      // For now, we return the atlas material directly
      // In the future, we might create variants with different properties
      this.materials.set(cacheKey, atlasMaterial);

      logger.debug('Created standard material', { name });

      return atlasMaterial;
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'MaterialService.getStandardMaterial', { name });
    }
  }

  /**
   * Get material for a block based on effect name
   *
   * This is a placeholder for future shader integration.
   * Currently always returns standard material.
   *
   * @param effectName Effect name from BlockModifier.visibility.effect
   * @param effectParameters Effect parameters from BlockModifier.visibility.effectParameters
   * @returns Material for the block
   */
  getMaterialForEffect(
    effectName?: string,
    effectParameters?: Record<string, any>
  ): StandardMaterial {
    // Future implementation will check ShaderService for custom effects
    // For now, always return standard material
    if (effectName) {
      logger.debug('Effect requested but not yet implemented', { effectName });
    }

    return this.getStandardMaterial();
  }

  /**
   * Get the atlas material (shorthand)
   */
  getAtlasMaterial(): StandardMaterial {
    return this.getStandardMaterial();
  }

  /**
   * Check if material service is ready
   */
  isReady(): boolean {
    return this.textureAtlas !== undefined && this.textureAtlas.isReady();
  }

  /**
   * Clear material cache
   */
  clearCache(): void {
    // Don't dispose the atlas material, just clear our references
    this.materials.clear();
    logger.info('Material cache cleared');
  }

  /**
   * Dispose all materials
   *
   * Warning: This will dispose all cached materials including the atlas material.
   * Only call when shutting down or switching worlds.
   */
  dispose(): void {
    for (const material of this.materials.values()) {
      // Only dispose materials we created, not the atlas material
      if (material.name !== 'atlasMaterial') {
        material.dispose();
      }
    }

    this.materials.clear();
    logger.info('Materials disposed');
  }
}
