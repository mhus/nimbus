/**
 * Cube Renderer
 * Renders CUBE and GLASS shaped blocks
 */

import { ShapeRenderer } from './ShapeRenderer';
import type { BlockRenderContext } from './BlockRenderContext';

export class CubeRenderer extends ShapeRenderer {
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

    // Block center for rotation
    const centerX = x + size / 2;
    const centerY = y + size / 2;
    const centerZ = z + size / 2;

    // Define the 8 corners of the cube with optional edge offsets
    // Corner order: bottom face (y=0), top face (y=1)
    // Each corner index: 0-7, each has x,y,z offset at positions [i*3, i*3+1, i*3+2]
    const corners = [
      // Bottom face (y = y)
      [x, y, z], // 0: left-back-bottom
      [x + size, y, z], // 1: right-back-bottom
      [x + size, y, z + size], // 2: right-front-bottom
      [x, y, z + size], // 3: left-front-bottom
      // Top face (y = y + size)
      [x, y + size, z], // 4: left-back-top
      [x + size, y + size, z], // 5: right-back-top
      [x + size, y + size, z + size], // 6: right-front-top
      [x, y + size, z + size], // 7: left-front-top
    ];

    // Apply edge offsets if available
    if (edgeOffsets) {
      for (let i = 0; i < 8; i++) {
        const offsetX = edgeOffsets[i * 3] / 127.0; // Normalize to roughly -1 to 1
        const offsetY = edgeOffsets[i * 3 + 1] / 127.0;
        const offsetZ = edgeOffsets[i * 3 + 2] / 127.0;

        corners[i][0] += offsetX;
        corners[i][1] += offsetY;
        corners[i][2] += offsetZ;
      }
    }

    let currentVertexIndex = vertexIndex;

    // Top face (4, 5, 6, 7)
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

    // Bottom face (3, 2, 1, 0)
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

    // Front face (North) (3, 7, 6, 2)
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
      currentVertexIndex,
      1 // Rotate UV 90° counter-clockwise (left)
    );
    currentVertexIndex += 4;

    // Back face (South) (1, 5, 4, 0)
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
      currentVertexIndex,
      1 // Rotate UV 90° counter-clockwise (left)
    );
    currentVertexIndex += 4;

    // Right face (East) (2, 6, 5, 1)
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
      currentVertexIndex,
      1 // Rotate UV 90° counter-clockwise (left)
    );
    currentVertexIndex += 4;

    // Left face (West) (0, 4, 7, 3)
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
      currentVertexIndex,
      1 // Rotate UV 90° counter-clockwise (left)
    );

    // Add wind properties if rendering transparent blocks
    this.addWindProperties(context, 24);

    // Return 24 vertices (4 per face * 6 faces)
    return 24;
  }
}
