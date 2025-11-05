/**
 * MaterialService - Manages Babylon.js materials for rendering
 *
 * Creates and caches materials for blocks based on BlockModifier properties.
 * Each unique combination of material properties gets its own cached material.
 * Supports shader effects via ShaderService integration.
 */

import {
  StandardMaterial,
  Scene,
  Texture,
  Color3,
  Material
} from '@babylonjs/core';
import {
  getLogger,
  ExceptionHandler,
  type BlockModifier,
  type TextureDefinition,
  type TextureKey,
  type UVMapping,
  TextureHelper,
  SamplingMode,
  TransparencyMode,
  BlockEffect
} from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { TextureAtlas } from '../rendering/TextureAtlas';
import type { ShaderService } from './ShaderService';

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
 * Creates and caches materials based on BlockModifier properties.
 * Integrates with ShaderService for effect-based materials.
 */
export class MaterialService {
  private scene: Scene;
  private appContext: AppContext;
  private textureAtlas?: TextureAtlas;
  private shaderService?: ShaderService;

  // Material cache: Map<materialKey, Material>
  private materials: Map<string, Material> = new Map();

  // Texture cache: Map<texturePath, Babylon.js Texture>
  private textures: Map<string, Texture> = new Map();

  constructor(scene: Scene, appContext: AppContext) {
    this.scene = scene;
    this.appContext = appContext;

    logger.info('MaterialService initialized');
  }

  /**
   * Set shader service for effect-based materials
   */
  setShaderService(shaderService: ShaderService): void {
    this.shaderService = shaderService;
    logger.debug('ShaderService set');
  }

  /**
   * Set the texture atlas
   *
   * Must be called before creating materials
   * @deprecated TextureAtlas is deprecated in favor of direct texture loading
   */
  setTextureAtlas(textureAtlas: TextureAtlas): void {
    this.textureAtlas = textureAtlas;
    logger.debug('Texture atlas set');
  }

  /**
   * Generate a unique material key from BlockModifier and textureIndex
   *
   * The key consists of all material-relevant properties:
   * - path: texture file path
   * - uvMapping: all UV transformation parameters
   * - samplingMode: texture sampling mode
   * - transparencyMode: transparency handling
   * - opacity: alpha value
   * - shaderParameters: effect-specific parameters
   * - effect: final effect (Texture.effect overrides BlockModifier.effect)
   * - color: tint color
   *
   * @param modifier BlockModifier containing material properties
   * @param textureIndex TextureKey indicating which texture to use
   * @returns Unique material key string for caching
   */
  getMaterialKey(modifier: BlockModifier, textureIndex: TextureKey): string {
    // Extract texture from modifier
    const texture = this.getTextureFromModifier(modifier, textureIndex);

    if (!texture) {
      // No texture defined - return default key
      return 'default';
    }

    // Normalize texture (string → TextureDefinition)
    const textureDef = TextureHelper.normalizeTexture(texture);

    // Build key components
    const parts: string[] = [];

    // 1. Path (required)
    parts.push(textureDef.path || modifier.visibility?.path || 'none');

    parts.push(textureDef.backFaceCulling !== undefined ? `bfc:${textureDef.backFaceCulling}` : 'bfc:true');

    // 2. UV Mapping (if defined)
    if (textureDef.uvMapping) {
      const uv = textureDef.uvMapping;
      parts.push([
        uv.x ?? 0,
        uv.y ?? 0,
        uv.w ?? 0,
        uv.h ?? 0,
        uv.uScale ?? 1,
        uv.vScale ?? 1,
        uv.uOffset ?? 0,
        uv.vOffset ?? 0,
        uv.wrapU ?? 1,
        uv.wrapV ?? 1,
        uv.uRotationCenter ?? 0.5,
        uv.vRotationCenter ?? 0.5,
        uv.wAng ?? 0,
        uv.uAng ?? 0,
        uv.vAng ?? 0,
      ].join(','));
    } else {
      parts.push('uv:default');
    }

    // 3. Sampling mode (default: LINEAR)
    parts.push(`sm:${textureDef.samplingMode ?? SamplingMode.LINEAR}`);

    // 4. Transparency mode (default: NONE)
    parts.push(`tm:${textureDef.transparencyMode ?? TransparencyMode.NONE}`);

    // 5. Opacity (default: 1.0, only include if != 1.0)
    if (textureDef.opacity !== undefined && textureDef.opacity !== 1.0) {
      parts.push(`op:${textureDef.opacity}`);
    }

    // 6. Shader parameters (if defined)
    if (textureDef.shaderParameters) {
      parts.push(`sp:${textureDef.shaderParameters}`);
    }

    // 7. Effect (Texture.effect overrides BlockModifier.effect)
    const finalEffect =
      textureDef.effect ?? modifier.visibility?.effect ?? BlockEffect.NONE;
    if (finalEffect !== BlockEffect.NONE) {
      parts.push(`eff:${finalEffect}`);
    }

    // 8. Color (if defined)
    if (textureDef.color) {
      parts.push(`col:${textureDef.color}`);
    }

    // Join all parts with separator
    return parts.join('|');
  }

  /**
   * Extract texture from BlockModifier for a given textureIndex
   */
  private getTextureFromModifier(
    modifier: BlockModifier,
    textureIndex: TextureKey
  ): TextureDefinition | string | undefined {
    if (!modifier.visibility?.textures) {
      return undefined;
    }

    return TextureHelper.getTexture(modifier.visibility, textureIndex);
  }

  /**
   * Check if BlockModifier has wind properties
   */
  private hasWindProperties(modifier: BlockModifier): boolean {
    return !!(
      (modifier.wind?.leafiness && modifier.wind.leafiness > 0) ||
      (modifier.wind?.stability && modifier.wind.stability > 0) ||
      (modifier.wind?.leverUp && modifier.wind.leverUp > 0) ||
      (modifier.wind?.leverDown && modifier.wind.leverDown > 0)
    );
  }

  /**
   * Get or create a material for a block based on BlockModifier and textureIndex
   *
   * This method:
   * 1. Generates a unique material key using getMaterialKey()
   * 2. Returns cached material if available
   * 3. Otherwise creates a new material:
   *    - Loads texture from server
   *    - Applies UV mapping and texture properties
   *    - Integrates with ShaderService for effects
   *    - Caches the result
   *
   * @param modifier BlockModifier containing material properties
   * @param textureIndex TextureKey indicating which texture to use
   * @returns Material (cached or newly created)
   */
  async getMaterial(
    modifier: BlockModifier,
    textureIndex: TextureKey
  ): Promise<Material> {
    try {
      // Generate cache key
      const cacheKey = this.getMaterialKey(modifier, textureIndex);

      // Check cache
      if (this.materials.has(cacheKey)) {
        return this.materials.get(cacheKey)!;
      }

      // Extract texture definition
      const texture = this.getTextureFromModifier(modifier, textureIndex);
      if (!texture) {
        // No texture - return default material
        return this.getOrCreateDefaultMaterial();
      }

      // Normalize texture
      const textureDef = TextureHelper.normalizeTexture(texture);

      // Determine final effect (Texture.effect overrides BlockModifier.effect)
      let finalEffect =
        textureDef.effect ?? modifier.visibility?.effect ?? BlockEffect.NONE;

      // Auto-detect wind effect if wind properties are present
      if (finalEffect === BlockEffect.NONE && this.hasWindProperties(modifier)) {
        finalEffect = BlockEffect.WIND;
        logger.debug('Auto-detected wind properties, using WIND effect', { cacheKey });
      }

      // Try to create shader material if effect is specified
      let material: Material | null = null;
      if (finalEffect !== BlockEffect.NONE && this.shaderService) {
        const effectName = BlockEffect[finalEffect].toLowerCase();
        if (this.shaderService.hasEffect(effectName)) {
          // Load texture first for shader
          const bTexture = await this.loadTexture(textureDef);
          material = this.shaderService.createMaterial(
            effectName,
            { texture: bTexture, name: cacheKey, ...modifier.visibility?.effectParameters }
          );
          logger.debug('Created shader material', { effectName, cacheKey });
        }
      }

      // Fallback to StandardMaterial if no shader material
      if (!material) {
        material = await this.createStandardMaterial(textureDef, cacheKey);
      }

      // Apply common material properties
      this.applyMaterialProperties(material, textureDef);

      // Cache material
      this.materials.set(cacheKey, material);

      logger.debug('Material created and cached', { cacheKey });

      return material;
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'MaterialService.getMaterial',
        { textureIndex }
      );
    }
  }

  /**
   * Create a StandardMaterial with texture
   */
  private async createStandardMaterial(
    textureDef: TextureDefinition,
    name: string
  ): Promise<StandardMaterial> {
    const material = new StandardMaterial(name, this.scene);

    // Load and apply texture
    const bTexture = await this.loadTexture(textureDef);
    if (bTexture) {
      material.diffuseTexture = bTexture;
    }

    // Disable specular highlights for blocks
    material.specularColor = new Color3(0, 0, 0);
    material.backFaceCulling = true;

    return material;
  }

  /**
   * Load a Babylon.js Texture from TextureDefinition
   */
  private async loadTexture(textureDef: TextureDefinition): Promise<Texture | null> {
    try {
      // Get full URL from NetworkService
      const networkService = this.appContext.services.network;
      if (!networkService) {
        throw new Error('NetworkService not available');
      }

      const url = networkService.getAssetUrl(textureDef.path);

      // Check texture cache
      const cacheKey = this.getTextureCacheKey(textureDef);
      if (this.textures.has(cacheKey)) {
        logger.debug('Texture loaded from cache', { path: textureDef.path });
        return this.textures.get(cacheKey)!;
      }

      // Create Babylon.js Texture
      const texture = new Texture(url, this.scene);

      // Apply UV mapping transformations if defined
      if (textureDef.uvMapping) {
        this.applyUVMapping(texture, textureDef.uvMapping);
      }

      // Apply sampling mode
      if (textureDef.samplingMode !== undefined) {
        texture.updateSamplingMode(this.toBabylonSamplingMode(textureDef.samplingMode));
      }

      // Cache texture
      this.textures.set(cacheKey, texture);

      logger.debug('Texture loaded', { path: textureDef.path, url });

      return texture;
    } catch (error) {
      ExceptionHandler.handle(error, 'MaterialService.loadTexture', {
        path: textureDef.path,
      });
      return null;
    }
  }

  /**
   * Apply UV mapping properties to Babylon.js Texture
   */
  private applyUVMapping(texture: Texture, uvMapping: UVMapping): void {
    // Apply UV transformations
    if (uvMapping.uScale !== undefined) texture.uScale = uvMapping.uScale;
    if (uvMapping.vScale !== undefined) texture.vScale = uvMapping.vScale;
    if (uvMapping.uOffset !== undefined) texture.uOffset = uvMapping.uOffset;
    if (uvMapping.vOffset !== undefined) texture.vOffset = uvMapping.vOffset;

    // Apply wrap modes
    if (uvMapping.wrapU !== undefined) {
      texture.wrapU = this.toBabylonWrapMode(uvMapping.wrapU);
    }
    if (uvMapping.wrapV !== undefined) {
      texture.wrapV = this.toBabylonWrapMode(uvMapping.wrapV);
    }

    // Apply rotation centers
    if (uvMapping.uRotationCenter !== undefined) {
      texture.uRotationCenter = uvMapping.uRotationCenter;
    }
    if (uvMapping.vRotationCenter !== undefined) {
      texture.vRotationCenter = uvMapping.vRotationCenter;
    }

    // Apply rotation angles
    if (uvMapping.wAng !== undefined) texture.wAng = uvMapping.wAng;
    if (uvMapping.uAng !== undefined) texture.uAng = uvMapping.uAng;
    if (uvMapping.vAng !== undefined) texture.vAng = uvMapping.vAng;
  }

  /**
   * Apply material properties (opacity, transparency, color)
   */
  private applyMaterialProperties(material: Material, textureDef: TextureDefinition): void {
    // Apply opacity
    if (textureDef.opacity !== undefined && textureDef.opacity !== 1.0) {
      material.alpha = textureDef.opacity;
    }

    // Apply transparency mode
    if (textureDef.transparencyMode !== undefined) {
      // Babylon.js will handle alpha automatically based on material.alpha and texture
      if (textureDef.transparencyMode !== TransparencyMode.NONE) {
        material.transparencyMode = textureDef.transparencyMode;
      }
    }

    // Apply color tint (if StandardMaterial)
    if (material instanceof StandardMaterial && textureDef.color) {
      const color = this.parseColor(textureDef.color);
      if (color) {
        material.diffuseColor = color;
      }
    }

    if (textureDef.backFaceCulling) {
        material.backFaceCulling = textureDef.backFaceCulling;
    }

  }

  /**
   * Parse color string to Color3
   */
  private parseColor(colorStr: string): Color3 | null {
    try {
      // Support hex colors (#RRGGBB or #RGB)
      if (colorStr.startsWith('#')) {
        return Color3.FromHexString(colorStr);
      }
      // Support named colors or other formats
      // For now, just return null for unsupported formats
      return null;
    } catch (error) {
      logger.warn('Failed to parse color', { color: colorStr });
      return null;
    }
  }

  /**
   * Convert SamplingMode enum to Babylon.js constant
   */
  private toBabylonSamplingMode(mode: SamplingMode): number {
    switch (mode) {
      case SamplingMode.NEAREST:
        return Texture.NEAREST_SAMPLINGMODE;
      case SamplingMode.LINEAR:
        return Texture.BILINEAR_SAMPLINGMODE;
      case SamplingMode.MIPMAP:
        return Texture.TRILINEAR_SAMPLINGMODE;
      default:
        return Texture.BILINEAR_SAMPLINGMODE;
    }
  }

  /**
   * Convert WrapMode to Babylon.js constant
   */
  private toBabylonWrapMode(mode: number): number {
    // WrapMode enum: CLAMP=0, REPEAT=1, MIRROR=2
    switch (mode) {
      case 0: // CLAMP
        return Texture.CLAMP_ADDRESSMODE;
      case 1: // REPEAT
        return Texture.WRAP_ADDRESSMODE;
      case 2: // MIRROR
        return Texture.MIRROR_ADDRESSMODE;
      default:
        return Texture.WRAP_ADDRESSMODE;
    }
  }

  /**
   * Get texture cache key
   */
  private getTextureCacheKey(textureDef: TextureDefinition): string {
    if (textureDef.uvMapping) {
      return `${textureDef.path}:${textureDef.uvMapping.x},${textureDef.uvMapping.y},${textureDef.uvMapping.w},${textureDef.uvMapping.h}`;
    }
    return textureDef.path;
  }

  /**
   * Get or create default material
   */
  private getOrCreateDefaultMaterial(): StandardMaterial {
    const cacheKey = 'default';
    if (this.materials.has(cacheKey)) {
      return this.materials.get(cacheKey) as StandardMaterial;
    }

    const material = new StandardMaterial(cacheKey, this.scene);
    material.diffuseColor = new Color3(1, 0, 1); // Magenta for missing texture
    material.specularColor = new Color3(0, 0, 0);
    this.materials.set(cacheKey, material);

    return material;
  }

  /**
   * Get or create a standard material
   *
   * @param name Material name
   * @returns Standard material
   * @deprecated Use getMaterial() with BlockModifier instead
   */
  getStandardMaterial(name: string = 'default'): StandardMaterial {
    const cacheKey = `standard:${name}`;

    // Check cache
    if (this.materials.has(cacheKey)) {
      const cached = this.materials.get(cacheKey)!;
      if (cached instanceof StandardMaterial) {
        return cached;
      }
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
   *
   * Clears all cached materials and textures without disposing them.
   * Use this when you want to reload materials.
   */
  clearCache(): void {
    this.materials.clear();
    this.textures.clear();
    logger.info('Material and texture caches cleared');
  }

  /**
   * Dispose all materials and textures
   *
   * Warning: This will dispose all cached materials and textures.
   * Only call when shutting down or switching worlds.
   */
  dispose(): void {
    // Dispose all materials
    for (const material of this.materials.values()) {
      // Skip atlas material if still using TextureAtlas
      if (material.name !== 'atlasMaterial') {
        material.dispose();
      }
    }

    // Dispose all textures
    for (const texture of this.textures.values()) {
      texture.dispose();
    }

    this.materials.clear();
    this.textures.clear();

    logger.info('Materials and textures disposed');
  }

  /**
   * Get cache statistics
   *
   * @returns Object with cache statistics
   */
  getCacheStats(): { materials: number; textures: number } {
    return {
      materials: this.materials.size,
      textures: this.textures.size,
    };
  }
}
