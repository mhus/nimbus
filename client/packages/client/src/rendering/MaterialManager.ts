/**
 * Material Manager
 * Manages and caches material variants based on block properties
 *
 * Creates appropriate materials based on:
 * - Block transparency (sets backFaceCulling = false)
 * - Wind properties (uses WindShader)
 * - Fluid type (uses FluidWaveShader)
 * - Modifier properties (custom material variants)
 */

import { Material, StandardMaterial, ShaderMaterial, Scene, Texture, Color3 } from '@babylonjs/core';
import type { BlockType } from '@voxel-02/core';
import type { BlockModifier } from '@voxel-02/core';
import type { TextureAtlas } from './TextureAtlas';
import type { WindShader } from './WindShader';
import type { FluidWaveShader } from './FluidWaveShader';

/**
 * Material type classification
 */
enum MaterialType {
  SOLID = 'solid',
  SOLID_WIND = 'solid_wind',
  TRANSPARENT = 'transparent',
  TRANSPARENT_WIND = 'transparent_wind',
  WATER = 'water',
  LAVA = 'lava',
}

/**
 * Material configuration for rendering
 */
export interface MaterialConfig {
  key: string;
  needsWindAttributes: boolean;
}

export class MaterialManager {
  private scene: Scene;
  private textureAtlas: TextureAtlas;
  private windShader: WindShader | undefined;
  private fluidWaveShader: FluidWaveShader | undefined;

  // Material cache: Map<cacheKey, Material>
  private materialCache: Map<string, Material> = new Map();

  constructor(
    scene: Scene,
    textureAtlas: TextureAtlas,
    windShader?: WindShader,
    fluidWaveShader?: FluidWaveShader
  ) {
    this.scene = scene;
    this.textureAtlas = textureAtlas;
    this.windShader = windShader;
    this.fluidWaveShader = fluidWaveShader;

    console.log('[MaterialManager] Initialized with texture atlas and shaders');
    console.log('[MaterialManager] WindShader:', windShader ? 'available' : 'NOT AVAILABLE');
    console.log('[MaterialManager] FluidWaveShader:', fluidWaveShader ? 'available' : 'NOT AVAILABLE');
  }

  /**
   * Get or create material for a block
   * Returns cached material if available, otherwise creates new one
   */
  getMaterial(block: BlockType, modifier?: BlockModifier): Material {
    const cacheKey = this.createMaterialKey(block, modifier);

    // Return cached material if available
    if (this.materialCache.has(cacheKey)) {
      return this.materialCache.get(cacheKey)!;
    }

    // Create new material based on block properties
    const material = this.createMaterial(block, modifier, cacheKey);

    // Cache and return
    this.materialCache.set(cacheKey, material);
    console.log(`[MaterialManager] Created and cached material: ${cacheKey}`);

    return material;
  }

  /**
   * Get material configuration for a block
   * Returns the material key and configuration needed for rendering
   */
  getMaterialConfig(block: BlockType, modifier?: BlockModifier): MaterialConfig {
    const materialKey = this.createMaterialKey(block, modifier);

    return {
      key: materialKey,
      needsWindAttributes:
        materialKey.startsWith(MaterialType.TRANSPARENT_WIND) ||
        materialKey.startsWith(MaterialType.SOLID_WIND),
    };
  }

  /**
   * Get or create material by type (direct access without block)
   * @param type Material type: 'solid', 'solid_wind', 'transparent', 'transparent_wind', 'water', 'lava'
   */
  getMaterialByType(type: 'solid' | 'solid_wind' | 'transparent' | 'transparent_wind' | 'water' | 'lava'): Material {
    // Return cached material if available
    if (this.materialCache.has(type)) {
      return this.materialCache.get(type)!;
    }

    // Create new material based on type
    const atlasMaterial = this.textureAtlas.getMaterial();
    const atlasTexture = atlasMaterial?.diffuseTexture;

    // Log texture status for debugging
    if (!atlasTexture) {
      console.warn(`[MaterialManager] Texture atlas not loaded for material type: ${type}`);
    } else {
      console.log(`[MaterialManager] Creating material ${type} with texture:`, atlasTexture.name);
    }

    let material: Material;

    switch (type) {
      case 'solid':
        material = this.createSolidMaterial(type, atlasTexture);
        break;
      case 'solid_wind':
        material = this.createSolidWindMaterial(type, atlasTexture);
        break;
      case 'transparent':
        material = this.createTransparentMaterial(type, atlasTexture);
        break;
      case 'transparent_wind':
        material = this.createTransparentWindMaterial(type, atlasTexture);
        break;
      case 'water':
        material = this.createWaterMaterial(type, atlasTexture);
        break;
      case 'lava':
        material = this.createLavaMaterial(type, atlasTexture);
        break;
      default:
        console.warn(`[MaterialManager] Unknown material type: ${type}, falling back to solid`);
        material = this.createSolidMaterial(type, atlasTexture);
    }

    // Cache and return
    this.materialCache.set(type, material);
    console.log(`[MaterialManager] Created and cached material by type: ${type}`);

    return material;
  }

  /**
   * Create material key from block properties
   * Key format: "type_properties"
   * Examples: "solid", "transparent", "transparent_wind", "water", "lava"
   * With custom texture: "solid_stone.png", "transparent_glass.png;dirt.png"
   */
  private createMaterialKey(block: BlockType, modifier?: BlockModifier): string {
    const materialType = this.classifyMaterialType(block, modifier);

    // Add additional properties that affect material
    const parts: string[] = [materialType];

    // Add texture override to key if present (ensures different textures get different materials)
    if (modifier?.texture) {
      const textureKey = typeof modifier.texture === 'string'
        ? modifier.texture
        : modifier.texture.join(';');
      parts.push(textureKey);
    }

    return parts.join('_');
  }

  /**
   * Classify material type based on block properties
   */
  private classifyMaterialType(block: BlockType, modifier?: BlockModifier): MaterialType {
    // Check for fluid first
    const isFluid = block.options?.fluid || false;
    const fluidMaterial = block.options?.material;

    if (isFluid && fluidMaterial === 'water') {
      return MaterialType.WATER;
    }

    if (isFluid && fluidMaterial === 'lava') {
      return MaterialType.LAVA;
    }

    // Check for transparency
    const isTransparent = block.transparent || block.options?.transparent || false;

    if (isTransparent) {
      // Check if block has wind properties (from block or modifier)
      // Check for ANY wind parameter: windLeafiness, windStability, windLeverUp, or windLeverDown
      const hasWindProperties =
        (block.windLeafiness && block.windLeafiness > 0) ||
        (block.windStability && block.windStability > 0) ||
        (block.windLeverUp && block.windLeverUp > 0) ||
        (block.windLeverDown && block.windLeverDown > 0) ||
        (modifier?.windLeafiness && modifier.windLeafiness > 0) ||
        (modifier?.windStability && modifier.windStability > 0) ||
        (modifier?.windLeverUp && modifier.windLeverUp > 0) ||
        (modifier?.windLeverDown && modifier.windLeverDown > 0);

      if (hasWindProperties && this.windShader) {
        console.log(`[MaterialManager] Block ${block.name} classified as TRANSPARENT_WIND (transparent=${isTransparent}, hasWind=${hasWindProperties})`);
        return MaterialType.TRANSPARENT_WIND;
      }

      console.log(`[MaterialManager] Block ${block.name} classified as TRANSPARENT (transparent=${isTransparent})`);
      return MaterialType.TRANSPARENT;
    }

    // Check if block has wind properties but is NOT transparent
    const hasWindProperties =
      (block.windLeafiness && block.windLeafiness > 0) ||
      (block.windStability && block.windStability > 0) ||
      (block.windLeverUp && block.windLeverUp > 0) ||
      (block.windLeverDown && block.windLeverDown > 0) ||
      (modifier?.windLeafiness && modifier.windLeafiness > 0) ||
      (modifier?.windStability && modifier.windStability > 0) ||
      (modifier?.windLeverUp && modifier.windLeverUp > 0) ||
      (modifier?.windLeverDown && modifier.windLeverDown > 0);

    if (hasWindProperties && this.windShader) {
      console.log(`[MaterialManager] Block ${block.name} classified as SOLID_WIND (transparent=false, hasWind=${hasWindProperties})`);
      return MaterialType.SOLID_WIND;
    }

    // Default: solid material
    return MaterialType.SOLID;
  }

  /**
   * Create material based on type and configuration
   */
  private createMaterial(block: BlockType, modifier: BlockModifier | undefined, cacheKey: string): Material {
    const materialType = this.classifyMaterialType(block, modifier);
    const atlasTexture = this.textureAtlas.getMaterial()?.diffuseTexture;

    switch (materialType) {
      case MaterialType.SOLID:
        return this.createSolidMaterial(cacheKey, atlasTexture);

      case MaterialType.SOLID_WIND:
        return this.createSolidWindMaterial(cacheKey, atlasTexture);

      case MaterialType.TRANSPARENT:
        return this.createTransparentMaterial(cacheKey, atlasTexture);

      case MaterialType.TRANSPARENT_WIND:
        return this.createTransparentWindMaterial(cacheKey, atlasTexture);

      case MaterialType.WATER:
        return this.createWaterMaterial(cacheKey, atlasTexture);

      case MaterialType.LAVA:
        return this.createLavaMaterial(cacheKey, atlasTexture);

      default:
        console.warn(`[MaterialManager] Unknown material type, falling back to solid: ${materialType}`);
        return this.createSolidMaterial(cacheKey, atlasTexture);
    }
  }

  /**
   * Create solid (opaque) material
   */
  private createSolidMaterial(name: string, texture: Texture | undefined): StandardMaterial {
    const material = new StandardMaterial(`solid_${name}`, this.scene);

    if (texture) {
      material.diffuseTexture = texture;
    }

    material.backFaceCulling = true; // Opaque blocks only need front faces
    material.useVertexColors = true; // Enable vertex color tinting

    return material;
  }

  /**
   * Create solid material with wind shader (opaque blocks with wind)
   */
  private createSolidWindMaterial(name: string, texture: Texture | undefined): ShaderMaterial {
    if (!this.windShader) {
      console.warn('[MaterialManager] WindShader not available, falling back to standard solid material');
      return this.createSolidMaterial(name, texture);
    }

    const material = this.windShader.createWindMaterial(texture, `wind_solid_${name}`);

    // Wind shader already handles rendering
    // For solid blocks, we could enable backFaceCulling for better performance
    // but the shader already has it set to false by default
    // material.backFaceCulling = true; // Uncomment if needed for performance

    return material;
  }

  /**
   * Create transparent material (glass, leaves without wind)
   */
  private createTransparentMaterial(name: string, texture: Texture | undefined): StandardMaterial {
    const material = new StandardMaterial(`transparent_${name}`, this.scene);

    if (texture) {
      material.diffuseTexture = texture;
      material.diffuseTexture.hasAlpha = true;
    }

    // Enable texture-based transparency
    material.useAlphaFromDiffuseTexture = true;
    material.transparencyMode = StandardMaterial.MATERIAL_ALPHATEST;

    // Transparent blocks need to be visible from both sides
    material.backFaceCulling = false;

    // Enable vertex colors for tinting
    material.useVertexColors = true;

    return material;
  }

  /**
   * Create transparent material with wind shader (leaves with wind)
   */
  private createTransparentWindMaterial(name: string, texture: Texture | undefined): ShaderMaterial {
    if (!this.windShader) {
      console.warn('[MaterialManager] WindShader not available, falling back to standard transparent material');
      return this.createTransparentMaterial(name, texture);
    }

    const material = this.windShader.createWindMaterial(texture, `wind_${name}`);

    // Wind shader already has backFaceCulling = false set in WindShader.ts
    // And alpha testing is handled in the fragment shader

    return material;
  }

  /**
   * Create water material with fluid shader
   */
  private createWaterMaterial(name: string, texture: Texture | undefined): Material {
    if (!this.fluidWaveShader) {
      console.warn('[MaterialManager] FluidWaveShader not available, falling back to standard material');
      return this.createFallbackFluidMaterial(name, texture, 'water');
    }

    const material = this.fluidWaveShader.createWaterMaterial(texture);
    return material;
  }

  /**
   * Create lava material with fluid shader
   */
  private createLavaMaterial(name: string, texture: Texture | undefined): Material {
    if (!this.fluidWaveShader) {
      console.warn('[MaterialManager] FluidWaveShader not available, falling back to standard material');
      return this.createFallbackFluidMaterial(name, texture, 'lava');
    }

    const material = this.fluidWaveShader.createLavaMaterial(texture);
    return material;
  }

  /**
   * Create fallback fluid material when shader is not available
   */
  private createFallbackFluidMaterial(
    name: string,
    texture: Texture | undefined,
    fluidType: 'water' | 'lava'
  ): StandardMaterial {
    const material = new StandardMaterial(`${fluidType}_${name}`, this.scene);

    if (texture) {
      material.diffuseTexture = texture;
    }

    material.backFaceCulling = false;
    material.transparencyMode = StandardMaterial.MATERIAL_ALPHABLEND;
    material.useVertexColors = true;

    if (fluidType === 'water') {
      material.alpha = 0.7;
      material.diffuseColor = new Color3(0.8, 0.9, 1.0);
    } else {
      material.alpha = 0.9;
      material.diffuseColor = new Color3(1.0, 0.3, 0.0);
      material.emissiveColor = new Color3(0.3, 0.1, 0.0);
    }

    return material;
  }

  /**
   * Clear material cache (useful for resource cleanup)
   */
  clearCache(): void {
    for (const [key, material] of this.materialCache) {
      material.dispose();
    }
    this.materialCache.clear();
    console.log('[MaterialManager] Cache cleared');
  }

  /**
   * Get cache statistics
   */
  getCacheStats(): { size: number; keys: string[] } {
    return {
      size: this.materialCache.size,
      keys: Array.from(this.materialCache.keys()),
    };
  }
}
