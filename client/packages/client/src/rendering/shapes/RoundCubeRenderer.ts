/**
 * Round Cube Renderer
 * Renders ROUND_CUBE blocks (cube with edge offsets applied more aggressively)
 * Uses the same rendering logic as CubeRenderer since edge offsets are applied in the cube logic
 */

import { CubeRenderer } from './CubeRenderer';
import type { BlockRenderContext } from './BlockRenderContext';

export class RoundCubeRenderer extends CubeRenderer {
  render(context: BlockRenderContext): number {
    // Render as cube with edge offsets to create rounded appearance
    return super.render(context);
  }
}
