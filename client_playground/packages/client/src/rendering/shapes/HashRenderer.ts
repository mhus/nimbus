/**
 * Hash Renderer
 * Renders HASH blocks - 6 independent faces that can be shifted with edgeOffsets
 * Unlike CUBE where faces share corners, each HASH face has its own 4 corners
 * Each face is rendered from both sides (double-sided) to remain visible when shifted
 * Total: up to 12 faces (6 × 2 sides) = up to 48 vertices
 *
 * EdgeOffset mapping (uses first 6 corners for the 6 faces):
 * - Corner 0 (indices 0-2): Top face offset (x,y,z)
 * - Corner 1 (indices 3-5): Bottom face offset (x,y,z)
 * - Corner 2 (indices 6-8): North face offset (x,y,z)
 * - Corner 3 (indices 9-11): South face offset (x,y,z)
 * - Corner 4 (indices 12-14): East face offset (x,y,z)
 * - Corner 5 (indices 15-17): West face offset (x,y,z)
 *
 * Special feature: If the X offset value is 127, the face is hidden (not rendered)
 * When all offsets are 0, the faces are positioned to form a normal cube.
 */

import { ShapeRenderer } from './ShapeRenderer';
import type { BlockRenderContext } from './BlockRenderContext';

export class HashRenderer extends ShapeRenderer {
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
    let verticesAdded = 0;

    // Block center for rotation
    const centerX = x + size / 2;
    const centerY = y + size / 2;
    const centerZ = z + size / 2;

    // Helper to get offset for a face
    const getFaceOffset = (cornerIndex: number): [number, number, number] | null => {
      if (!edgeOffsets || cornerIndex * 3 + 2 >= edgeOffsets.length) {
        return [0, 0, 0];
      }

      const rawX = edgeOffsets[cornerIndex * 3];

      // Special case: X offset of 127 means hide this face
      if (rawX === 127) {
        return null;
      }

      return [
        rawX / 127.0,
        edgeOffsets[cornerIndex * 3 + 1] / 127.0,
        edgeOffsets[cornerIndex * 3 + 2] / 127.0,
      ];
    };

    // Get offsets for each face (null if face should be hidden)
    const topOffset = getFaceOffset(0);
    const bottomOffset = getFaceOffset(1);
    const northOffset = getFaceOffset(2);
    const southOffset = getFaceOffset(3);
    const eastOffset = getFaceOffset(4);
    const westOffset = getFaceOffset(5);

    let currentVertexIndex = vertexIndex;

    // Top face (only render if not hidden)
    if (topOffset !== null) {
      // Top face - front side (y = y + size) - all 4 corners shifted by topOffset
      this.addFace(
        [x + topOffset[0], y + size + topOffset[1], z + topOffset[2]],
        [x + size + topOffset[0], y + size + topOffset[1], z + topOffset[2]],
        [x + size + topOffset[0], y + size + topOffset[1], z + size + topOffset[2]],
        [x + topOffset[0], y + size + topOffset[1], z + size + topOffset[2]],
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
      verticesAdded += 4;

      // Top face - back side (reversed winding order)
      this.addFace(
        [x + topOffset[0], y + size + topOffset[1], z + size + topOffset[2]],
        [x + size + topOffset[0], y + size + topOffset[1], z + size + topOffset[2]],
        [x + size + topOffset[0], y + size + topOffset[1], z + topOffset[2]],
        [x + topOffset[0], y + size + topOffset[1], z + topOffset[2]],
        [0, -1, 0],
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
      verticesAdded += 4;
    }

    // Bottom face (only render if not hidden)
    if (bottomOffset !== null) {
      // Bottom face - front side (y = y) - all 4 corners shifted by bottomOffset
      this.addFace(
        [x + bottomOffset[0], y + bottomOffset[1], z + size + bottomOffset[2]],
        [x + size + bottomOffset[0], y + bottomOffset[1], z + size + bottomOffset[2]],
        [x + size + bottomOffset[0], y + bottomOffset[1], z + bottomOffset[2]],
        [x + bottomOffset[0], y + bottomOffset[1], z + bottomOffset[2]],
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
      verticesAdded += 4;

      // Bottom face - back side (reversed winding order)
      this.addFace(
        [x + bottomOffset[0], y + bottomOffset[1], z + bottomOffset[2]],
        [x + size + bottomOffset[0], y + bottomOffset[1], z + bottomOffset[2]],
        [x + size + bottomOffset[0], y + bottomOffset[1], z + size + bottomOffset[2]],
        [x + bottomOffset[0], y + bottomOffset[1], z + size + bottomOffset[2]],
        [0, 1, 0],
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
      verticesAdded += 4;
    }

    // North face (only render if not hidden)
    if (northOffset !== null) {
      // North face - front side (z = z + size) - all 4 corners shifted by northOffset
      this.addFace(
        [x + northOffset[0], y + northOffset[1], z + size + northOffset[2]],
        [x + size + northOffset[0], y + northOffset[1], z + size + northOffset[2]],
        [x + size + northOffset[0], y + size + northOffset[1], z + size + northOffset[2]],
        [x + northOffset[0], y + size + northOffset[1], z + size + northOffset[2]],
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
      verticesAdded += 4;

      // North face - back side (reversed winding order)
      this.addFace(
        [x + northOffset[0], y + size + northOffset[1], z + size + northOffset[2]],
        [x + size + northOffset[0], y + size + northOffset[1], z + size + northOffset[2]],
        [x + size + northOffset[0], y + northOffset[1], z + size + northOffset[2]],
        [x + northOffset[0], y + northOffset[1], z + size + northOffset[2]],
        [0, 0, -1],
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
      verticesAdded += 4;
    }

    // South face (only render if not hidden)
    if (southOffset !== null) {
      // South face - front side (z = z) - all 4 corners shifted by southOffset
      this.addFace(
        [x + size + southOffset[0], y + southOffset[1], z + southOffset[2]],
        [x + southOffset[0], y + southOffset[1], z + southOffset[2]],
        [x + southOffset[0], y + size + southOffset[1], z + southOffset[2]],
        [x + size + southOffset[0], y + size + southOffset[1], z + southOffset[2]],
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
      verticesAdded += 4;

      // South face - back side (reversed winding order)
      this.addFace(
        [x + size + southOffset[0], y + size + southOffset[1], z + southOffset[2]],
        [x + southOffset[0], y + size + southOffset[1], z + southOffset[2]],
        [x + southOffset[0], y + southOffset[1], z + southOffset[2]],
        [x + size + southOffset[0], y + southOffset[1], z + southOffset[2]],
        [0, 0, 1],
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
      verticesAdded += 4;
    }

    // East face (only render if not hidden)
    if (eastOffset !== null) {
      // East face - front side (x = x + size) - all 4 corners shifted by eastOffset
      this.addFace(
        [x + size + eastOffset[0], y + eastOffset[1], z + eastOffset[2]],
        [x + size + eastOffset[0], y + eastOffset[1], z + size + eastOffset[2]],
        [x + size + eastOffset[0], y + size + eastOffset[1], z + size + eastOffset[2]],
        [x + size + eastOffset[0], y + size + eastOffset[1], z + eastOffset[2]],
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
      verticesAdded += 4;

      // East face - back side (reversed winding order)
      this.addFace(
        [x + size + eastOffset[0], y + size + eastOffset[1], z + eastOffset[2]],
        [x + size + eastOffset[0], y + size + eastOffset[1], z + size + eastOffset[2]],
        [x + size + eastOffset[0], y + eastOffset[1], z + size + eastOffset[2]],
        [x + size + eastOffset[0], y + eastOffset[1], z + eastOffset[2]],
        [-1, 0, 0],
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
      verticesAdded += 4;
    }

    // West face (only render if not hidden)
    if (westOffset !== null) {
      // West face - front side (x = x) - all 4 corners shifted by westOffset
      this.addFace(
        [x + westOffset[0], y + westOffset[1], z + size + westOffset[2]],
        [x + westOffset[0], y + westOffset[1], z + westOffset[2]],
        [x + westOffset[0], y + size + westOffset[1], z + westOffset[2]],
        [x + westOffset[0], y + size + westOffset[1], z + size + westOffset[2]],
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
      verticesAdded += 4;

      // West face - back side (reversed winding order)
      this.addFace(
        [x + westOffset[0], y + size + westOffset[1], z + size + westOffset[2]],
        [x + westOffset[0], y + size + westOffset[1], z + westOffset[2]],
        [x + westOffset[0], y + westOffset[1], z + westOffset[2]],
        [x + westOffset[0], y + westOffset[1], z + size + westOffset[2]],
        [1, 0, 0],
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
      verticesAdded += 4;
    }

    // Add wind properties if rendering transparent blocks
    this.addWindProperties(context, verticesAdded);

    return verticesAdded;
  }
}
