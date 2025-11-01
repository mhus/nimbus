/**
 * Shape Renderer Base Class
 * Abstract base class for rendering different block shapes
 * Each shape type extends this class and implements the render method
 */

import { Matrix, Vector3 } from '@babylonjs/core';
import { RednerService } from '../services/RenderService';
import type { ClientBlock } from '@nimbus/shared';

/**
 * Abstract base class for shape renderers
 * Provides common functionality for rendering block faces with rotation, UVs, and colors
 */
export abstract class BlockRenderer {
  /**
   * Render a block using the provided context
   * @param context - All data needed to render the block
   * @returns Number of vertices added to the geometry arrays (or Promise for async renderers)
   */
  abstract render(
      renderService : RenderService,
      block: ClientBlock,
      worldX: number,
      worldY: number,
      worldZ: number
): number | Promise<number>;

}
