/**
 * Sprite Renderer
 * Renders SPRITE shaped blocks using Babylon.js Sprite system
 * Creates multiple sprite instances per block using SpriteManager
 *
 * Block/Modifier Properties:
 * - spriteCount: Number of sprites per block (default: 5)
 * - scale: Sprite size multiplier (default: 1.0)
 * - texture: Texture path for the sprite
 * - windLeafiness: Wind effect intensity (0-1, default: 0 = no wind)
 * - windStability: Wind resistance (0-1, default: 0 = maximum sway)
 *
 * Positioning: Random on XZ plane (default) or edgeOffset if set
 */

import { Sprite } from '@babylonjs/core';
import { ShapeRenderer } from './ShapeRenderer';
import type { BlockRenderContext } from './BlockRenderContext';

export class SpriteRenderer extends ShapeRenderer {
  async render(context: BlockRenderContext): Promise<number> {
    const {
      x,
      y,
      z,
      block,
      modifier,
      scene,
      spriteManagerRegistry,
    } = context;

    // SPRITE blocks don't add geometry to chunk mesh
    // Instead, they create separate Sprite objects

    if (!scene) {
      console.warn('[SpriteRenderer] No scene provided, cannot render sprites');
      return 0;
    }

    if (!spriteManagerRegistry) {
      console.warn('[SpriteRenderer] No spriteManagerRegistry provided, cannot render sprites');
      return 0;
    }

    // Get sprite count from modifier or block (default: 5)
    const spriteCount = modifier?.spriteCount ?? block.spriteCount ?? 5;

    // Get texture path
    const texturePath = this.getTexturePath(block, modifier);

    // Get scale from modifier (default: 1.0)
    const scale = modifier?.scale?.[0] ?? 1.0; // Use X scale for sprite size

    // Get or create SpriteManager for this texture (await async loading)
    const spriteManager = await spriteManagerRegistry.getManager(texturePath);

    // Get actual texture dimensions from registry (stored during manager creation)
    const textureDimensions = spriteManagerRegistry.getTextureDimensions(texturePath);

    console.log(`[SpriteRenderer] Rendering ${spriteCount} sprites at (${x}, ${y}, ${z}) with scale ${scale}`);
    console.log(`[SpriteRenderer] Texture: ${texturePath}, dimensions: ${textureDimensions.width}x${textureDimensions.height}`);

    // Calculate sprite width and height based on texture aspect ratio
    const aspectRatio = textureDimensions.width / textureDimensions.height;

    // Check if edgeOffsets are set
    const hasEdgeOffsets = this.hasEdgeOffsets(context);

    if (hasEdgeOffsets) {
      // Use edgeOffset for positioning (8 corners)
      this.renderSpritesWithEdgeOffsets(context, spriteCount, spriteManager, scale, aspectRatio);
    } else {
      // Use random positioning
      this.renderSpritesRandom(context, spriteCount, spriteManager, scale, aspectRatio);
    }

    // Return 0 vertices (sprites don't contribute to chunk mesh geometry)
    return 0;
  }

  /**
   * Check if edgeOffsets are available and non-zero
   */
  private hasEdgeOffsets(context: BlockRenderContext): boolean {
    const { edgeOffsets } = context;

    if (!edgeOffsets || edgeOffsets.length < 24) {
      return false;
    }

    // Check if any offset is non-zero
    for (let i = 0; i < 24; i++) {
      if (edgeOffsets[i] !== 0) {
        return true;
      }
    }

    return false;
  }

  /**
   * Render sprites using edgeOffset positions (8 corners)
   */
  private renderSpritesWithEdgeOffsets(
    context: BlockRenderContext,
    spriteCount: number,
    spriteManager: any,
    scale: number,
    aspectRatio: number
  ): void {
    const { x, y, z, edgeOffsets, modifier, spriteManagerRegistry, block } = context;

    // Get wind properties from modifier or block (modifier takes precedence)
    // Default to 0 so wind only applies when explicitly set
    const windLeafiness = modifier?.windLeafiness ?? block.windLeafiness ?? 0;
    const windStability = modifier?.windStability ?? block.windStability ?? 0;

    if (!edgeOffsets || edgeOffsets.length < 24) {
      console.warn('[SpriteRenderer] edgeOffsets invalid, falling back to random positioning');
      this.renderSpritesRandom(context, spriteCount, spriteManager, scale, aspectRatio);
      return;
    }

    // Block center
    const centerX = x + 0.5;
    const centerY = y + 0.5;
    const centerZ = z + 0.5;

    // Use up to 8 positions (corners) for sprites
    const positions: [number, number, number][] = [];

    for (let i = 0; i < Math.min(8, spriteCount); i++) {
      const offsetX = edgeOffsets[i * 3] / 127.0; // Normalize -127..128 to roughly -1..1
      const offsetY = edgeOffsets[i * 3 + 1] / 127.0;
      const offsetZ = edgeOffsets[i * 3 + 2] / 127.0;

      positions.push([
        centerX + offsetX,
        centerY + offsetY,
        centerZ + offsetZ
      ]);
    }

    // If spriteCount > 8, fill remaining with random positions
    const seed = this.hashPosition(x, y, z);
    for (let i = positions.length; i < spriteCount; i++) {
      const randX = this.seededRandom(seed + i * 3) - 0.5;
      const randY = this.seededRandom(seed + i * 3 + 1) - 0.5;
      const randZ = this.seededRandom(seed + i * 3 + 2) - 0.5;

      positions.push([
        centerX + randX * 0.8,
        centerY + randY * 0.8,
        centerZ + randZ * 0.8
      ]);
    }

    // Create actual Sprite objects and add them to the context's sprites array
    for (let i = 0; i < positions.length; i++) {
      const [posX, posY, posZ] = positions[i];
      const sprite = new Sprite(`sprite_${x}_${y}_${z}_${i}`, spriteManager);
      sprite.position.set(posX, posY, posZ);

      // Set width and height based on aspect ratio
      sprite.width = scale * aspectRatio;
      sprite.height = scale;

      // Ensure sprite uses alpha blending (not additive)
      sprite.isPickable = false; // Optimization: sprites don't need picking

      // Register sprite with registry for wind animation only if wind parameters are set and non-zero
      // This optimization skips animation for sprites without wind effects
      if (windLeafiness > 0 || windStability > 0) {
        spriteManagerRegistry.registerSprite(sprite, windLeafiness, windStability);
      }

      // Add sprite to context array for tracking/disposal
      context.sprites.push(sprite);
    }

    const windStatus = (windLeafiness > 0 || windStability > 0) ? `with wind (leafiness=${windLeafiness.toFixed(2)}, stability=${windStability.toFixed(2)})` : 'without wind';
    console.log(`[SpriteRenderer] Created ${spriteCount} sprites using edgeOffset positions ${windStatus}`);
  }

  /**
   * Render sprites using random positioning within block bounds
   * Sprites are placed on the bottom XZ plane (like grass growing from ground)
   */
  private renderSpritesRandom(
    context: BlockRenderContext,
    spriteCount: number,
    spriteManager: any,
    scale: number,
    aspectRatio: number
  ): void {
    const { x, y, z, spriteManagerRegistry, block, modifier } = context;

    // Get wind properties from modifier or block (modifier takes precedence)
    // Default to 0 so wind only applies when explicitly set
    const windLeafiness = modifier?.windLeafiness ?? block.windLeafiness ?? 0;
    const windStability = modifier?.windStability ?? block.windStability ?? 0;

    // Block center for X and Z
    const centerX = x + 0.5;
    const centerZ = z + 0.5;

    // Bottom of block for Y (grass grows from ground)
    const baseY = y;

    // Use deterministic random based on block position for consistency
    const seed = this.hashPosition(x, y, z);

    // Create sprites at random positions on XZ plane
    for (let i = 0; i < spriteCount; i++) {
      // Generate pseudo-random offset based on seed and index
      const randX = this.seededRandom(seed + i * 2) - 0.5;
      const randZ = this.seededRandom(seed + i * 2 + 1) - 0.5;

      // Position within 80% of block bounds on XZ plane (keep some margin)
      const posX = centerX + randX * 0.8;
      const posZ = centerZ + randZ * 0.8;

      // Y position at bottom of block
      const posY = baseY;

      // Create actual Sprite object
      const sprite = new Sprite(`sprite_${x}_${y}_${z}_${i}`, spriteManager);
      sprite.position.set(posX, posY, posZ);

      // Set width and height based on aspect ratio
      sprite.width = scale * aspectRatio;
      sprite.height = scale;

      // Ensure sprite uses alpha blending (not additive)
      sprite.isPickable = false; // Optimization: sprites don't need picking

      // Register sprite with registry for wind animation only if wind parameters are set and non-zero
      // This optimization skips animation for sprites without wind effects
      if (windLeafiness > 0 || windStability > 0) {
        spriteManagerRegistry.registerSprite(sprite, windLeafiness, windStability);
      }

      // Add sprite to context array for tracking/disposal
      context.sprites.push(sprite);
    }

    const windStatus = (windLeafiness > 0 || windStability > 0) ? `with wind (leafiness=${windLeafiness.toFixed(2)}, stability=${windStability.toFixed(2)})` : 'without wind';
    console.log(`[SpriteRenderer] Created ${spriteCount} sprites on XZ plane with aspect ratio ${aspectRatio.toFixed(2)} ${windStatus}`);
  }

  /**
   * Get texture path from block or modifier
   */
  private getTexturePath(block: any, modifier: any): string {
    // Check modifier first
    if (modifier?.texture) {
      return typeof modifier.texture === 'string'
        ? modifier.texture
        : modifier.texture[0]; // Use first texture if array
    }

    // Use block texture
    if (typeof block.texture === 'string') {
      return block.texture;
    }

    if (Array.isArray(block.texture)) {
      return block.texture[0]; // Use first texture
    }

    return 'stone'; // Fallback
  }

  /**
   * Hash position to seed for deterministic random
   */
  private hashPosition(x: number, y: number, z: number): number {
    // Simple hash function for deterministic randomness
    let hash = 0;
    hash = ((hash << 5) - hash) + x;
    hash = ((hash << 5) - hash) + y;
    hash = ((hash << 5) - hash) + z;
    return hash >>> 0; // Unsigned 32-bit integer
  }

  /**
   * Seeded pseudo-random number generator (0-1)
   */
  private seededRandom(seed: number): number {
    const x = Math.sin(seed) * 10000;
    return x - Math.floor(x);
  }
}
