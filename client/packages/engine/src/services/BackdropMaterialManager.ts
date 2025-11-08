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
      const cacheKey = `backdrop:${config.typeId ?? config.texture ?? 'default'}`;

      // Check cache
      if (this.materials.has(cacheKey)) {
        logger.debug('Backdrop material found in cache', { typeId: config.typeId });
        return this.materials.get(cacheKey)!;
      }

      logger.debug('Creating new backdrop material', { typeId: config.typeId });

      // Create material
      const material = await this.createMaterial(config, cacheKey);

      // Cache
      this.materials.set(cacheKey, material);

      logger.debug('Backdrop material created and cached', { typeId: config.typeId });

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

    // Load texture if specified
    if (config.texture) {
      const networkService = this.appContext.services.network;
      if (!networkService) {
        throw new Error('NetworkService not available');
      }

      const textureUrl = networkService.getAssetUrl(config.texture);
      const texture = new Texture(textureUrl, this.scene);

      material.diffuseTexture = texture;
    }

    // Apply color tint if specified
    if (config.color) {
      const color = this.parseColor(config.color);
      material.diffuseColor = color;
    }

    // Apply alpha
    if (config.alpha !== undefined) {
      material.alpha = config.alpha;
    }

    // Backdrop-specific material properties
    this.applyBackdropProperties(material);

    return material;
  }

  /**
   * Apply backdrop-specific rendering properties
   */
  private applyBackdropProperties(material: StandardMaterial): void {
    // Disable lighting (unlit material - always bright regardless of scene lighting)
    material.disableLighting = true;
    material.emissiveColor = Color3.White();

    // Enable alpha blending
    material.transparencyMode = StandardMaterial.MATERIAL_ALPHABLEND;

    // Render both sides (no backface culling)
    material.backFaceCulling = false;

    // Render on far plane (behind everything else)
    // Note: This should be combined with mesh.renderingGroupId = 0 in BackdropService
    // alphaMode is automatically set by transparencyMode

    // Disable depth write (don't block other objects)
    material.disableDepthWrite = false; // Can be configured per use case
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
