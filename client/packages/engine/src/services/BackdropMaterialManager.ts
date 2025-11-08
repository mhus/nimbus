/**
 * BackdropMaterialManager - Manages materials for backdrop rendering
 *
 * Separate material cache from block materials for backdrop-specific properties:
 * - No lighting (unlit materials)
 * - Render on far plane
 * - Custom alpha blending
 * - Distance-based fade effects
 */

import { StandardMaterial, Texture, Color3, type Scene, type Material } from '@babylonjs/core';
import { getLogger, ExceptionHandler, type Backdrop } from '@nimbus/shared';
import type { AppContext } from '../AppContext';

const logger = getLogger('BackdropMaterialManager');

/**
 * BackdropMaterialManager
 *
 * Features:
 * - Separate cache for backdrop materials (doesn't pollute block material cache)
 * - Lazy loading of backdrop textures
 * - Applies backdrop-specific rendering properties
 * - Uses NetworkService for texture URL resolution
 */
export class BackdropMaterialManager {
  /** Material cache: key = backdrop config ID */
  private materials = new Map<string, Material>();

  constructor(
    private scene: Scene,
    private appContext: AppContext
  ) {
    logger.info('BackdropMaterialManager initialized');
  }

  /**
   * Get or create backdrop material from configuration
   *
   * @param config Backdrop configuration loaded from server
   * @returns Babylon.js material configured for backdrop rendering
   */
  async getBackdropMaterial(config: Backdrop): Promise<Material> {
    try {
      // Create cache key from texture path for proper invalidation
      const cacheKey = `backdrop:${config.texture ?? config.typeId ?? config.id ?? 'default'}`;

      // Check cache
      if (this.materials.has(cacheKey)) {
        logger.debug('Backdrop material from cache', { cacheKey });
        return this.materials.get(cacheKey)!;
      }

      logger.info('Creating new backdrop material (not in cache)', { cacheKey, config });

      // Create material
      const material = await this.createMaterial(config, cacheKey);

      // Cache and return
      this.materials.set(cacheKey, material);

      return material;
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'BackdropMaterialManager.getBackdropMaterial',
        { configTypeId: config.typeId }
      );
    }
  }

  /**
   * Create Babylon.js material from backdrop configuration
   */
  private async createMaterial(
    config: Backdrop,
    materialName: string
  ): Promise<StandardMaterial> {
    const material = new StandardMaterial(materialName, this.scene);

    logger.info('Creating backdrop material', {
      materialName,
      config
    });

    // Load texture if specified
    if (config.texture) {
      const networkService = this.appContext.services.network;
      if (!networkService) {
        throw new Error('NetworkService not available');
      }

      const textureUrl = networkService.getAssetUrl(config.texture);

      logger.info('Loading backdrop texture', {
        texturePath: config.texture,
        textureUrl
      });

      const texture = new Texture(textureUrl, this.scene);
      texture.hasAlpha = true; // Enable alpha channel from PNG
      material.diffuseTexture = texture;
      material.useAlphaFromDiffuseTexture = true; // Use alpha from texture

      // Don't override color or alpha when using texture - let PNG control it
      material.diffuseColor = Color3.White(); // Neutral color to not tint the texture
      // Don't set material.alpha - let texture alpha control it
      logger.info('Using texture alpha (not setting material.alpha)');
    } else {
      // No texture - use color and alpha from config
      if (config.color) {
        material.diffuseColor = this.parseColor(config.color);
        logger.info('Applied color to material', { color: config.color });
      } else {
        material.diffuseColor = Color3.White();
      }

      // Apply alpha only when no texture
      material.alpha = config.alpha ?? 1.0;
      logger.info('Applied alpha to material', { alpha: material.alpha });
    }

    // Backdrop-specific material properties
    this.applyBackdropProperties(material);

    logger.info('Backdrop material creation complete', {
      materialName,
      hasTexture: !!material.diffuseTexture,
      alpha: material.alpha,
      diffuseColor: material.diffuseColor
    });

    return material;
  }

  /**
   * Apply backdrop-specific rendering properties
   */
  private applyBackdropProperties(material: StandardMaterial): void {
    material.backFaceCulling = false;

    if (!material.diffuseTexture) {
      // Solid color backdrop: use emissive for glow
      material.disableLighting = true;
      material.emissiveColor = material.diffuseColor;
      material.transparencyMode = StandardMaterial.MATERIAL_OPAQUE;
    } else {
      // Textured backdrop: use emissive texture for unlit + alpha support
      material.disableLighting = true;
      material.emissiveTexture = material.diffuseTexture;
      material.transparencyMode = StandardMaterial.MATERIAL_ALPHABLEND;
    }

    // Enable depth testing
    material.disableDepthWrite = false;
    material.depthFunction = 0;

    logger.debug('Backdrop material properties applied', {
      hasDiffuseTexture: !!material.diffuseTexture,
      hasEmissiveTexture: !!material.emissiveTexture,
      transparencyMode: material.transparencyMode
    });
  }

  /**
   * Parse color string (hex format) to Color3
   *
   * @param colorString Hex color like "#808080" or "#ff0000"
   * @returns Babylon.js Color3
   */
  private parseColor(colorString: string): Color3 {
    try {
      // Remove # if present
      const hex = colorString.replace('#', '');

      // Parse RGB components
      const r = parseInt(hex.substring(0, 2), 16) / 255;
      const g = parseInt(hex.substring(2, 4), 16) / 255;
      const b = parseInt(hex.substring(4, 6), 16) / 255;

      return new Color3(r, g, b);
    } catch (error) {
      logger.warn('Failed to parse color, using white', { colorString });
      return Color3.White();
    }
  }

  /**
   * Check if material exists in cache
   */
  hasMaterial(backdropId: string): boolean {
    return this.materials.has(`backdrop:${backdropId}`);
  }

  /**
   * Get statistics
   */
  getStats(): { cachedMaterials: number } {
    return {
      cachedMaterials: this.materials.size,
    };
  }

  /**
   * Dispose all backdrop materials
   */
  dispose(): void {
    logger.info('Disposing all backdrop materials', {
      count: this.materials.size,
    });

    for (const material of this.materials.values()) {
      material.dispose();
    }

    this.materials.clear();
  }
}
