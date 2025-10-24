/**
 * Stair Renderer
 * Renders STAIR shaped blocks (filled stairs)
 * Creates a stair with full-width base and half-width top step
 * Supports rotation via rotationX/rotationY
 */

import { ShapeRenderer } from './ShapeRenderer';
import type { BlockRenderContext } from './BlockRenderContext';

export class StairRenderer extends ShapeRenderer {
  render(context: BlockRenderContext): number {
    console.log('[StairRenderer] Rendering STAIR block at', context.x, context.y, context.z, 'with block:', context.block?.name);

    const {
      x,
      y,
      z,
      blockUVs,
      rotationMatrix,
      blockColor,
      positions,
      indices,
      normals,
      uvs,
      colors,
      vertexIndex,
    } = context;

    const size = 1;

    // Block center for rotation
    const centerX = x + size / 2;
    const centerY = y + size / 2;
    const centerZ = z + size / 2;

    // Define corners for a filled stair
    // Lower base: full width (x=0 to x=1, z=0 to z=1), from y=0 to y=0.5
    // Upper step: half width (x=0 to x=1, z=0.5 to z=1.0), from y=0.5 to y=1.0

    const corners = [
      // Lower base - bottom 4 corners (y=0)
      [x, y, z],                      // 0: left-back-bottom
      [x + size, y, z],               // 1: right-back-bottom
      [x + size, y, z + size],        // 2: right-front-bottom
      [x, y, z + size],               // 3: left-front-bottom

      // Lower base - top 4 corners (y=0.5)
      [x, y + size / 2, z],           // 4: left-back-mid
      [x + size, y + size / 2, z],    // 5: right-back-mid
      [x + size, y + size / 2, z + size],  // 6: right-front-mid
      [x, y + size / 2, z + size],         // 7: left-front-mid

      // Upper step - bottom 4 corners (y=0.5, z=0.5 to z=1.0)
      [x, y + size / 2, z + size / 2],         // 8: left-mid-mid
      [x + size, y + size / 2, z + size / 2],  // 9: right-mid-mid
      [x + size, y + size / 2, z + size],      // 10: right-front-mid (same as 6)
      [x, y + size / 2, z + size],             // 11: left-front-mid (same as 7)

      // Upper step - top 4 corners (y=1.0, z=0.5 to z=1.0)
      [x, y + size, z + size / 2],             // 12: left-mid-top
      [x + size, y + size, z + size / 2],      // 13: right-mid-top
      [x + size, y + size, z + size],          // 14: right-front-top
      [x, y + size, z + size],                 // 15: left-front-top
    ];

    let currentVertexIndex = vertexIndex;

    // LOWER BASE (full width, 6 faces)

    // Top face of lower base (4, 5, 6, 7)
    this.addFace(
      corners[4],
      corners[5],
      corners[6],
      corners[7],
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
      currentVertexIndex
    );
    currentVertexIndex += 4;

    // Bottom face of lower base (3, 2, 1, 0)
    this.addFace(
      corners[3],
      corners[2],
      corners[1],
      corners[0],
      [0, -1, 0],
      blockUVs.bottom,
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
      currentVertexIndex
    );
    currentVertexIndex += 4;

    // Front face of lower base (3, 7, 6, 2)
    this.addFace(
      corners[3],
      corners[7],
      corners[6],
      corners[2],
      [0, 0, 1],
      blockUVs.north || blockUVs.sides,
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
      currentVertexIndex
    );
    currentVertexIndex += 4;

    // Back face of lower base (1, 5, 4, 0)
    this.addFace(
      corners[1],
      corners[5],
      corners[4],
      corners[0],
      [0, 0, -1],
      blockUVs.south || blockUVs.sides,
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
      currentVertexIndex
    );
    currentVertexIndex += 4;

    // Right face of lower base (2, 6, 5, 1)
    this.addFace(
      corners[2],
      corners[6],
      corners[5],
      corners[1],
      [1, 0, 0],
      blockUVs.east || blockUVs.sides,
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
      currentVertexIndex
    );
    currentVertexIndex += 4;

    // Left face of lower base (0, 4, 7, 3)
    this.addFace(
      corners[0],
      corners[4],
      corners[7],
      corners[3],
      [-1, 0, 0],
      blockUVs.west || blockUVs.sides,
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
      currentVertexIndex
    );
    currentVertexIndex += 4;

    // UPPER STEP (half width, 6 faces)

    // Top face of upper step (12, 13, 14, 15)
    this.addFace(
      corners[12],
      corners[13],
      corners[14],
      corners[15],
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
      currentVertexIndex
    );
    currentVertexIndex += 4;

    // Bottom face of upper step (11, 10, 9, 8)
    this.addFace(
      corners[11],
      corners[10],
      corners[9],
      corners[8],
      [0, -1, 0],
      blockUVs.bottom,
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
      currentVertexIndex
    );
    currentVertexIndex += 4;

    // Front face of upper step (11, 15, 14, 10)
    this.addFace(
      corners[11],
      corners[15],
      corners[14],
      corners[10],
      [0, 0, 1],
      blockUVs.north || blockUVs.sides,
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
      currentVertexIndex
    );
    currentVertexIndex += 4;

    // Back face of upper step (9, 13, 12, 8)
    this.addFace(
      corners[9],
      corners[13],
      corners[12],
      corners[8],
      [0, 0, -1],
      blockUVs.south || blockUVs.sides,
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
      currentVertexIndex
    );
    currentVertexIndex += 4;

    // Right face of upper step (10, 14, 13, 9)
    this.addFace(
      corners[10],
      corners[14],
      corners[13],
      corners[9],
      [1, 0, 0],
      blockUVs.east || blockUVs.sides,
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
      currentVertexIndex
    );
    currentVertexIndex += 4;

    // Left face of upper step (8, 12, 15, 11)
    this.addFace(
      corners[8],
      corners[12],
      corners[15],
      corners[11],
      [-1, 0, 0],
      blockUVs.west || blockUVs.sides,
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
      currentVertexIndex
    );

    // Add wind properties if rendering transparent blocks
    this.addWindProperties(context, 48);

    // Return 48 vertices (4 per face * 12 faces: 6 for lower base + 6 for upper step)
    return 48;
  }
}
