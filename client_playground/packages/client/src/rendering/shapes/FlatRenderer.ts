/**
 * Flat Renderer
 * Renders FLAT blocks (only top face, like carpet or water surface)
 */

import { ShapeRenderer } from './ShapeRenderer';
import type { BlockRenderContext } from './BlockRenderContext';

export class FlatRenderer extends ShapeRenderer {
  render(context: BlockRenderContext): number {
    const {
      x,
      y,
      z,
      blockUVs,
      rotationMatrix,
      edgeOffsets,
      blockColor,
      positions,
      indices,
      normals,
      uvs,
      colors,
      vertexIndex,
    } = context;

    const size = 1;
    const height = 0.1; // Flat blocks are thin

    // Block center for rotation
    const centerX = x + size / 2;
    const centerY = y + height;
    const centerZ = z + size / 2;

    // Only top face
    const corners = [
      [x, y + height, z], // left-back
      [x + size, y + height, z], // right-back
      [x + size, y + height, z + size], // right-front
      [x, y + height, z + size], // left-front
    ];

    // Apply edge offsets if available (only top 4 corners)
    if (edgeOffsets) {
      for (let i = 0; i < 4; i++) {
        const offsetX = edgeOffsets[(i + 4) * 3] / 127.0;
        const offsetY = edgeOffsets[(i + 4) * 3 + 1] / 127.0;
        const offsetZ = edgeOffsets[(i + 4) * 3 + 2] / 127.0;

        corners[i][0] += offsetX;
        corners[i][1] += offsetY;
        corners[i][2] += offsetZ;
      }
    }

    this.addFace(
      corners[0],
      corners[1],
      corners[2],
      corners[3],
      [0, 1, 0],
      blockUVs.top,
      rotationMatrix,
      centerX,
      centerY,
      centerZ,
      blockColor,
      positions,
      indices,
      normals,
      uvs,
      colors,
      vertexIndex
    );

    // Add wind properties if rendering transparent blocks
    this.addWindProperties(context, 4);

    // Return 4 vertices (only top face)
    return 4;
  }
}
