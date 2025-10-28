/**
 * Cross Renderer
 * Renders CROSS shaped blocks (2 intersecting planes for plants)
 * Renders both sides of each plane for full visibility
 * Supports edgeOffsets to manipulate the 8 corners and rotation
 */

import { ShapeRenderer } from './ShapeRenderer';
import type { BlockRenderContext } from './BlockRenderContext';
import type { AtlasUV } from '../TextureAtlas';
import { Matrix, Vector3 } from '@babylonjs/core';

export class CrossRenderer extends ShapeRenderer {
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

    // Define the 8 corners of the cross (same as cube corners)
    // Corner order: bottom face (y=0), top face (y=1)
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

    // First plane (diagonal from corner 0 to corner 6)
    // Vertices ordered: bottom-left, bottom-right, top-right, top-left
    // UV mapping for correct orientation: bottom gets v1, top gets v0 (v is inverted in texture space)
    const plane1 = [
      corners[0], // left-back-bottom (UV: u0,v1)
      corners[2], // right-front-bottom (UV: u1,v1)
      corners[6], // right-front-top (UV: u1,v0)
      corners[4], // left-back-top (UV: u0,v0)
    ];

    // Second plane (diagonal from corner 1 to corner 7)
    // Vertices ordered: bottom-left, bottom-right, top-right, top-left
    const plane2 = [
      corners[3], // left-front-bottom (UV: u0,v1)
      corners[1], // right-back-bottom (UV: u1,v1)
      corners[5], // right-back-top (UV: u1,v0)
      corners[7], // left-front-top (UV: u0,v0)
    ];

    // Texture to use for cross
    const texture = blockUVs.sides || blockUVs.top;

    // Flip V coordinates to correct upside-down texture
    // In texture space: v=0 is top, v=1 is bottom
    // In vertex space: bottom vertices should get v=1, top vertices should get v=0
    const flippedTexture: AtlasUV = {
      u0: texture.u0,
      u1: texture.u1,
      v0: texture.v1, // Swap v0 and v1
      v1: texture.v0,
    };

    let currentVertexIndex = vertexIndex;

    // Add first plane - front side
    this.addFace(
      plane1[0],
      plane1[1],
      plane1[2],
      plane1[3],
      [0.707, 0, 0.707], // Normal for diagonal plane
      flippedTexture,
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

    // Add first plane - back side (with mirrored UVs)
    this.addBackFace(
      plane1[0],
      plane1[1],
      plane1[2],
      plane1[3],
      [-0.707, 0, -0.707], // Reversed normal
      flippedTexture,
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

    // Add second plane - front side
    this.addFace(
      plane2[0],
      plane2[1],
      plane2[2],
      plane2[3],
      [0.707, 0, -0.707], // Normal for other diagonal
      flippedTexture,
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

    // Add second plane - back side (with mirrored UVs)
    this.addBackFace(
      plane2[0],
      plane2[1],
      plane2[2],
      plane2[3],
      [-0.707, 0, 0.707], // Reversed normal
      flippedTexture,
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
    this.addWindProperties(context, 16);

    // Return 16 vertices (4 faces * 4 vertices each)
    return 16;
  }

  /**
   * Add a face with reversed winding order (for back faces)
   * This ensures back faces are visible when backFaceCulling is false
   */
  private addBackFace(
    v1: number[],
    v2: number[],
    v3: number[],
    v4: number[],
    normal: number[],
    atlasUV: AtlasUV,
    rotationMatrix: Matrix | null,
    centerX: number,
    centerY: number,
    centerZ: number,
    blockColor: [number, number, number, number],
    positions: number[],
    indices: number[],
    normals: number[],
    uvs: number[],
    colors: number[],
    vertexIndex: number
  ): void {
    // For back face, reverse the winding order: v1, v4, v3, v2 (instead of v1, v2, v3, v4)
    // This ensures the face is rendered correctly from behind
    const vertices = [v1, v4, v3, v2];

    // Apply rotation if matrix exists
    const rotatedVertices = vertices.map((v) => {
      if (rotationMatrix) {
        const vec = new Vector3(v[0] - centerX, v[1] - centerY, v[2] - centerZ);
        const rotated = Vector3.TransformCoordinates(vec, rotationMatrix);
        return [rotated.x + centerX, rotated.y + centerY, rotated.z + centerZ];
      }
      return v;
    });

    // Positions
    positions.push(
      ...rotatedVertices[0],
      ...rotatedVertices[1],
      ...rotatedVertices[2],
      ...rotatedVertices[3]
    );

    // Rotate normals if needed
    let finalNormal = normal;
    if (rotationMatrix) {
      const normalVec = new Vector3(...normal);
      const rotatedNormal = Vector3.TransformNormal(normalVec, rotationMatrix);
      finalNormal = [rotatedNormal.x, rotatedNormal.y, rotatedNormal.z];
    }

    // Normals (same for all 4 vertices)
    for (let i = 0; i < 4; i++) {
      normals.push(...finalNormal);
    }

    // UVs - same as front face (no mirroring)
    // Because we reversed the winding order, the UVs are automatically in the correct position
    uvs.push(
      atlasUV.u0,
      atlasUV.v0, // Bottom-left
      atlasUV.u1,
      atlasUV.v0, // Bottom-right
      atlasUV.u1,
      atlasUV.v1, // Top-right
      atlasUV.u0,
      atlasUV.v1 // Top-left
    );

    // Colors (RGBA for all 4 vertices)
    for (let i = 0; i < 4; i++) {
      colors.push(...blockColor);
    }

    // Indices (2 triangles)
    indices.push(
      vertexIndex,
      vertexIndex + 1,
      vertexIndex + 2,
      vertexIndex,
      vertexIndex + 2,
      vertexIndex + 3
    );
  }
}
