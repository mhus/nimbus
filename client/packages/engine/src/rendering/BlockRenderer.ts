/**
 * Shape Renderer Base Class
 * Abstract base class for rendering different block shapes
 * Each shape type extends this class and implements the render method
 */

import { Matrix, Vector3 } from '@babylonjs/core';
import type { RenderService, RenderContext } from '../services/RenderService';
import type { ClientBlock } from '../types';

/**
 * Abstract base class for shape renderers
 * Provides common functionality for rendering block faces with rotation, UVs, and colors
 */
export abstract class BlockRenderer {
  /**
   * Render a block using the provided context
   * @param renderService - The render service instance
   * @param block - The client block to render
   * @param renderContext - The rendering context with transformation and other info
   * @returns Number of vertices added to the geometry arrays (or Promise for async renderers)
   */
  abstract render(
      renderContext: RenderContext,
      block: ClientBlock
): void | Promise<void>;

  /**
   * Determine if this renderer requires a separate mesh for each block
   *
   * Most renderers (CUBE) return false - they batch all blocks into a single chunk mesh.
   * Special renderers (FLIPBOX, BILLBOARD, SPRITE, MODEL) return true - they need
   * individual meshes with original textures or special shader materials.
   *
   * @returns true if this block needs its own mesh, false if it can be batched (default)
   */
  needsSeparateMesh(): boolean {
    return false; // Default: batch into chunk mesh
  }

}
