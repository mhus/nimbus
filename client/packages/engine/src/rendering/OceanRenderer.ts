/**
 * OceanRenderer - Renders ocean water surfaces
 *
 * Creates flat water surfaces on TOP of blocks using Babylon.js WaterMaterial.
 * Each ocean block gets its own separate mesh with animated water effects.
 *
 * Features:
 * - Flat horizontal water surface at block top
 * - Babylon.js WaterMaterial with waves and reflections
 * - Configurable via wind parameters (windForce, waveHeight, etc.)
 * - Supports offset, scaling transformations
 */

import { Mesh, MeshBuilder, Texture, Vector2, Color3 } from '@babylonjs/core';
import { WaterMaterial } from '@babylonjs/materials';
import { getLogger, TextureHelper, Shape } from '@nimbus/shared';
import type { ClientBlock } from '../types';
import { BlockRenderer } from './BlockRenderer';
import type { RenderContext } from '../services/RenderService';

const logger = getLogger('OceanRenderer');

/**
 * OceanRenderer - Renders flat ocean water surfaces
 *
 * Creates horizontal planes with WaterMaterial for realistic water animation.
 * Default size: 1x1 unit, positioned at TOP of block.
 */
export class OceanRenderer extends BlockRenderer {
  /**
   * OceanRenderer needs separate mesh per block
   * (WaterMaterial needs separate mesh for reflections/refractions)
   */
  needsSeparateMesh(): boolean {
    return true;
  }

  /**
   * Render an OCEAN block
   *
   * Creates a flat horizontal water surface at the top of the block.
   * Uses WaterMaterial for animated waves.
   *
   * @param renderContext Render context
   * @param clientBlock Block to render
   */
  async render(renderContext: RenderContext, clientBlock: ClientBlock): Promise<void> {
    const block = clientBlock.block;
    const modifier = clientBlock.currentModifier;

    if (!modifier || !modifier.visibility) {
      logger.warn('OceanRenderer: No visibility modifier', { block });
      return;
    }

    // Validate shape
    const shape = modifier.visibility.shape ?? Shape.CUBE;
    if (shape !== Shape.OCEAN) {
      logger.warn('OceanRenderer: Not an OCEAN shape', { shape, block });
      return;
    }

    // Get textures
    const textures = modifier.visibility.textures;
    if (!textures || Object.keys(textures).length === 0) {
      logger.warn('OceanRenderer: No textures defined (need bump texture)', { block });
      return;
    }

    // Get bump texture (required for WaterMaterial)
    // Can be defined as TextureKey.ALL (0) or TextureKey.TOP (1)
    const bumpTexture = textures[0] || textures[1];
    if (!bumpTexture) {
      logger.warn('OceanRenderer: No bump texture defined', { block });
      return;
    }

    const bumpTextureDef = TextureHelper.normalizeTexture(bumpTexture);

    // Get transformations
    const scalingX = modifier.visibility.scalingX ?? 1.0;
    const scalingZ = modifier.visibility.scalingZ ?? 1.0;

    // Get offset (shifts the water plane position)
    let offsetX = 0;
    let offsetY = 0;
    let offsetZ = 0;

    if (modifier.visibility.offsets && modifier.visibility.offsets.length >= 3) {
      offsetX = modifier.visibility.offsets[0] || 0;
      offsetY = modifier.visibility.offsets[1] || 0;
      offsetZ = modifier.visibility.offsets[2] || 0;
    }

    // Get water properties from wind modifier (reused for water animation)
    const windForce = modifier.wind?.stability ?? 10.0;
    const waveHeight = modifier.wind?.leafiness ?? 0.4;
    const bumpHeight = modifier.wind?.leverUp ?? 0.1;
    const waveLength = modifier.wind?.leverDown ?? 0.1;

    // Get water color from texture color field (default: blue)
    let waterColor = new Color3(0.1, 0.4, 0.8); // Blue
    if (bumpTextureDef.color) {
      waterColor = this.parseColor(bumpTextureDef.color);
    }

    // Block position
    const pos = block.position;

    // Calculate water surface center
    // IMPORTANT: Water surface is at TOP of block (Y + 0.5)
    const centerX = pos.x + 0.5 + offsetX;
    const centerY = pos.y + 0.5 + offsetY;
    const centerZ = pos.z + 0.5 + offsetZ;

    // Create ocean mesh
    await this.createOceanMesh(
      clientBlock,
      centerX,
      centerY,
      centerZ,
      scalingX,
      scalingZ,
      bumpTextureDef.path,
      waterColor,
      windForce,
      waveHeight,
      bumpHeight,
      waveLength,
      renderContext
    );
  }

  /**
   * Parse color from string
   *
   * Supports: "#FF6600", "rgb(255,102,0)", etc.
   * Fallback: Blue
   */
  private parseColor(colorString: string): Color3 {
    try {
      colorString = colorString.trim();

      // Hex format: "#0066CC"
      if (colorString.startsWith('#')) {
        const hex = colorString.substring(1);
        const r = parseInt(hex.substring(0, 2), 16) / 255;
        const g = parseInt(hex.substring(2, 4), 16) / 255;
        const b = parseInt(hex.substring(4, 6), 16) / 255;
        return new Color3(r, g, b);
      }

      // RGB format: "rgb(10,102,204)"
      if (colorString.startsWith('rgb')) {
        const match = colorString.match(/\d+/g);
        if (match && match.length >= 3) {
          const r = parseInt(match[0]) / 255;
          const g = parseInt(match[1]) / 255;
          const b = parseInt(match[2]) / 255;
          return new Color3(r, g, b);
        }
      }

      logger.warn('Could not parse color, using default blue', { colorString });
      return new Color3(0.1, 0.4, 0.8);
    } catch (error) {
      logger.warn('Error parsing color, using default blue', { colorString, error });
      return new Color3(0.1, 0.4, 0.8);
    }
  }

  /**
   * Create ocean mesh with WaterMaterial
   *
   * Creates a flat horizontal plane with animated water effects.
   *
   * @param clientBlock Block to create ocean for
   * @param centerX Center X position
   * @param centerY Center Y position (top of block)
   * @param centerZ Center Z position
   * @param scalingX X-axis scaling (width)
   * @param scalingZ Z-axis scaling (depth)
   * @param bumpTexturePath Bump texture for water waves
   * @param waterColor Water color tint
   * @param windForce Wind strength
   * @param waveHeight Wave amplitude
   * @param bumpHeight Bump map intensity
   * @param waveLength Wave frequency
   * @param renderContext Render context with resourcesToDispose
   */
  private async createOceanMesh(
    clientBlock: ClientBlock,
    centerX: number,
    centerY: number,
    centerZ: number,
    scalingX: number,
    scalingZ: number,
    bumpTexturePath: string,
    waterColor: Color3,
    windForce: number,
    waveHeight: number,
    bumpHeight: number,
    waveLength: number,
    renderContext: RenderContext
  ): Promise<void> {
    const block = clientBlock.block;
    const scene = renderContext.renderService.materialService.scene;

    // Create mesh name
    const meshName = `ocean_${block.position.x}_${block.position.y}_${block.position.z}`;

    // Create flat horizontal ground mesh (subdivisions for wave animation)
    const plane = MeshBuilder.CreateGround(
      meshName,
      {
        width: scalingX,
        height: scalingZ, // Ground uses 'height' for Z-axis depth
        subdivisions: 32, // More subdivisions = smoother waves
      },
      scene
    );

    // Position at top of block
    plane.position.set(centerX, centerY, centerZ);

    // Create WaterMaterial
    const waterMaterial = new WaterMaterial(meshName + '_mat', scene, new Vector2(512, 512));

    // Load bump texture
    const bumpTexture = (await renderContext.renderService.materialService.loadTexture(
      bumpTexturePath
    )) as Texture;

    if (bumpTexture) {
      waterMaterial.bumpTexture = bumpTexture;
    } else {
      logger.warn('Failed to load bump texture for ocean', { bumpTexturePath });
    }

    // Configure water properties
    waterMaterial.windForce = windForce;
    waterMaterial.waveHeight = waveHeight;
    waterMaterial.bumpHeight = bumpHeight;
    waterMaterial.waveLength = waveLength;
    waterMaterial.waterColor = waterColor;
    waterMaterial.colorBlendFactor = 0.5;

    // Set wind direction (default: northeast)
    waterMaterial.windDirection = new Vector2(1, 1);

    // Make water semi-transparent
    waterMaterial.backFaceCulling = false;

    // Apply material to mesh
    plane.material = waterMaterial;

    // Make water not pickable (optimization)
    plane.isPickable = false;

    // Register mesh for automatic disposal when chunk is unloaded
    renderContext.resourcesToDispose.addMesh(plane);

    logger.debug('OCEAN mesh created', {
      meshName,
      position: `${centerX},${centerY},${centerZ}`,
      size: `${scalingX}x${scalingZ}`,
      windForce,
      waveHeight,
    });
  }
}
