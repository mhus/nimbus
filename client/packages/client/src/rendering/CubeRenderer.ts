/**
 * CubeRenderer - Renders cube-shaped blocks
 *
 * Creates mesh geometry for standard cube blocks with proper UV mapping.
 */

import { VertexData, Vector3 } from '@babylonjs/core';
import { getLogger } from '@nimbus/shared';
import type { Block, BlockType, TextureDefinition, TextureKey } from '@nimbus/shared';
import type { TextureAtlas, AtlasUV } from './TextureAtlas';

const logger = getLogger('CubeRenderer');

/**
 * Face data for cube rendering
 */
interface FaceData {
  positions: number[];
  indices: number[];
  uvs: number[];
  normals: number[];
}

/**
 * CubeRenderer - Renders cube blocks
 *
 * Creates optimized mesh data for cube-shaped blocks.
 * Supports face culling and UV mapping from texture atlas.
 */
export class CubeRenderer {
  private textureAtlas: TextureAtlas;

  constructor(textureAtlas: TextureAtlas) {
    this.textureAtlas = textureAtlas;
  }

  /**
   * Normalize texture - convert string or TextureDefinition to TextureDefinition
   */
  private normalizeTexture(texture: any): TextureDefinition | null {
    if (!texture) return null;

    // If it's already a TextureDefinition object with 'path'
    if (typeof texture === 'object' && texture.path) {
      return texture as TextureDefinition;
    }

    // If it's a string, convert to TextureDefinition
    if (typeof texture === 'string') {
      return { path: texture };
    }

    return null;
  }

  /**
   * Render a cube block into the mesh data
   *
   * @param block Block instance
   * @param blockType Block type definition
   * @param worldX World X coordinate
   * @param worldY World Y coordinate
   * @param worldZ World Z coordinate
   * @param faceData Face data arrays to append to
   * @param vertexOffset Current vertex offset
   */
  async renderCube(
    block: Block,
    blockType: BlockType,
    worldX: number,
    worldY: number,
    worldZ: number,
    faceData: FaceData,
    vertexOffset: number
  ): Promise<number> {
    // Get block modifier for current status (from BlockType.initialStatus)
    const status = blockType.initialStatus ?? 0;
    const modifier = blockType.modifiers[status];

    if (!modifier || !modifier.visibility) {
      logger.warn('Block has no visibility modifier', { blockTypeId: block.blockTypeId, status });
      return vertexOffset;
    }

    // Get textures from modifier
    const textures = modifier.visibility.textures;
    if (!textures) {
      logger.warn('Block has no textures', { blockTypeId: block.blockTypeId, status });
      return vertexOffset;
    }

    // Get texture definitions for each face
    // TextureKey: ALL=0, TOP=1, BOTTOM=2, LEFT=3, RIGHT=4, FRONT=5, BACK=6, SIDE=7
    const topTexture = this.normalizeTexture(textures[1] || textures[0] || textures[7]); // TOP, ALL, or SIDE
    const bottomTexture = this.normalizeTexture(textures[2] || textures[0] || textures[7]); // BOTTOM, ALL, or SIDE
    const sideTexture = this.normalizeTexture(textures[7] || textures[0]); // SIDE or ALL

    if (!topTexture || !bottomTexture || !sideTexture) {
      logger.warn('Missing required textures for cube', { blockTypeId: block.blockTypeId });
      return vertexOffset;
    }

    // Load textures and get UVs
    const topUV = await this.textureAtlas.getTextureUV(topTexture);
    const bottomUV = await this.textureAtlas.getTextureUV(bottomTexture);
    const sideUV = await this.textureAtlas.getTextureUV(sideTexture);

    // Render all 6 faces
    // TODO: Implement face culling based on block.faceVisibility
    vertexOffset = this.addFace(faceData, vertexOffset, worldX, worldY, worldZ, 'top', topUV);
    vertexOffset = this.addFace(faceData, vertexOffset, worldX, worldY, worldZ, 'bottom', bottomUV);
    vertexOffset = this.addFace(faceData, vertexOffset, worldX, worldY, worldZ, 'north', sideUV);
    vertexOffset = this.addFace(faceData, vertexOffset, worldX, worldY, worldZ, 'south', sideUV);
    vertexOffset = this.addFace(faceData, vertexOffset, worldX, worldY, worldZ, 'east', sideUV);
    vertexOffset = this.addFace(faceData, vertexOffset, worldX, worldY, worldZ, 'west', sideUV);

    return vertexOffset;
  }

  /**
   * Add a cube face to the mesh data
   */
  private addFace(
    faceData: FaceData,
    vertexOffset: number,
    x: number,
    y: number,
    z: number,
    face: string,
    uv: AtlasUV
  ): number {
    const { positions, normals } = this.getFaceGeometry(face, x, y, z);

    // Add positions
    faceData.positions.push(...positions);

    // Add normals
    faceData.normals.push(...normals);

    // Add UVs (4 vertices per face)
    faceData.uvs.push(uv.u0, uv.v1); // Bottom-left
    faceData.uvs.push(uv.u1, uv.v1); // Bottom-right
    faceData.uvs.push(uv.u1, uv.v0); // Top-right
    faceData.uvs.push(uv.u0, uv.v0); // Top-left

    // Add indices (2 triangles per face)
    faceData.indices.push(
      vertexOffset + 0,
      vertexOffset + 1,
      vertexOffset + 2, // Triangle 1
      vertexOffset + 0,
      vertexOffset + 2,
      vertexOffset + 3 // Triangle 2
    );

    return vertexOffset + 4;
  }

  /**
   * Get geometry data for a cube face
   */
  private getFaceGeometry(
    face: string,
    x: number,
    y: number,
    z: number
  ): { positions: number[]; normals: number[] } {
    const positions: number[] = [];
    const normals: number[] = [];

    switch (face) {
      case 'top':
        // Top face (Y+)
        positions.push(
          x, y + 1, z,     // 0
          x + 1, y + 1, z, // 1
          x + 1, y + 1, z + 1, // 2
          x, y + 1, z + 1  // 3
        );
        normals.push(0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0);
        break;

      case 'bottom':
        // Bottom face (Y-)
        positions.push(
          x, y, z + 1,     // 0
          x + 1, y, z + 1, // 1
          x + 1, y, z,     // 2
          x, y, z          // 3
        );
        normals.push(0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0);
        break;

      case 'north':
        // North face (Z-)
        positions.push(
          x + 1, y, z,     // 0
          x, y, z,         // 1
          x, y + 1, z,     // 2
          x + 1, y + 1, z  // 3
        );
        normals.push(0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1);
        break;

      case 'south':
        // South face (Z+)
        positions.push(
          x, y, z + 1,     // 0
          x + 1, y, z + 1, // 1
          x + 1, y + 1, z + 1, // 2
          x, y + 1, z + 1  // 3
        );
        normals.push(0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1);
        break;

      case 'west':
        // West face (X-)
        positions.push(
          x, y, z,         // 0
          x, y, z + 1,     // 1
          x, y + 1, z + 1, // 2
          x, y + 1, z      // 3
        );
        normals.push(-1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0);
        break;

      case 'east':
        // East face (X+)
        positions.push(
          x + 1, y, z + 1,     // 0
          x + 1, y, z,         // 1
          x + 1, y + 1, z,     // 2
          x + 1, y + 1, z + 1  // 3
        );
        normals.push(1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0);
        break;
    }

    return { positions, normals };
  }
}
