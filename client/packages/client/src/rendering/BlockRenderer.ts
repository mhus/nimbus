/**
 * Shape Renderer Base Class
 * Abstract base class for rendering different block shapes
 * Each shape type extends this class and implements the render method
 */

import { Matrix, Vector3 } from '@babylonjs/core';
import type { RenderService } from '../services/RenderService';
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
   * @param worldX - World X position of the block
   * @param worldY - World Y position of the block
   * @param worldZ - World Z position of the block
   * @param faceData - Face data to append geometry to
   * @param vertexOffset - Current vertex offset for indices
   * @returns Number of vertices added to the geometry arrays (or Promise for async renderers)
   */
  abstract render(
      renderService : RenderService,
      block: ClientBlock,
      worldX: number,
      worldY: number,
      worldZ: number,
      faceData: any,
      vertexOffset: number
): number | Promise<number>;

}
