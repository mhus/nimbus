/**
 * CubeRenderer - Renders cube-shaped blocks
 *
 * Creates mesh geometry for standard cube blocks with proper UV mapping.
 */

import { VertexData, Vector3, Matrix } from '@babylonjs/core';
import { getLogger, FaceFlag, FaceVisibilityHelper } from '@nimbus/shared';
import type { Block, BlockType, TextureDefinition, TextureKey } from '@nimbus/shared';
import type { ClientBlock } from '../types';
import { BlockRenderer} from './BlockRenderer';
import { RenderService } from '../services/RenderService';
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
export class CubeRenderer extends BlockRenderer {
  private textureAtlas: TextureAtlas;

  constructor(textureAtlas: TextureAtlas) {
    super();
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
    * @param renderService - The RenderService instance
    * @param block - The block to render
    * @param worldX - World X position of the block
    * @param worldY - World Y position of the block
    * @param worldZ - World Z position of the block
    * @returns Number of vertices added to the mesh data
   */
  async render(
      renderService : RenderService,
      block: ClientBlock,
      worldX: number,
      worldY: number,
      worldZ: number,
      faceData: FaceData,
      vertexOffset: number
  ): Promise<number> {

    // Get block modifier for current status (from BlockType.initialStatus)
    const modifier = block.currentModifier;

    if (!modifier || !modifier.visibility) {
      logger.debug('Block has no visibility modifier', { blockTypeId: block.blockType.id });
      return vertexOffset;
    }

    // Get textures from modifier
    const textures = modifier.visibility.textures;
    if (!textures) {
      logger.warn('Block has no textures', { blockTypeId: block.blockType.id });
      return vertexOffset;
    }

    logger.debug('Rendering cube block', {
      blockTypeId: block.blockType.id,
      position: { x: worldX, y: worldY, z: worldZ },
      hasTextures: !!textures,
      textureCount: Object.keys(textures).length
    });

    // Determine texture indices for each face
    // Use texture[index] if available, otherwise use default texture[7] or texture[0]
    const topIndex = textures[1] ? 1 : (textures[7] ? 7 : 0);
    const bottomIndex = textures[2] ? 2 : (textures[7] ? 7 : 0);
    const leftIndex = textures[3] ? 3 : (textures[7] ? 7 : 0);
    const rightIndex = textures[4] ? 4 : (textures[7] ? 7 : 0);
    const frontIndex = textures[5] ? 5 : (textures[7] ? 7 : 0);
    const backIndex = textures[6] ? 6 : (textures[7] ? 7 : 0);

    // Check face visibility (default: all faces visible if no faceVisibility)
    const isTopVisible = !block.block.faceVisibility || FaceVisibilityHelper.isVisible(block.block.faceVisibility, FaceFlag.TOP);
    const isBottomVisible = !block.block.faceVisibility || FaceVisibilityHelper.isVisible(block.block.faceVisibility, FaceFlag.BOTTOM);
    const isLeftVisible = !block.block.faceVisibility || FaceVisibilityHelper.isVisible(block.block.faceVisibility, FaceFlag.LEFT);
    const isRightVisible = !block.block.faceVisibility || FaceVisibilityHelper.isVisible(block.block.faceVisibility, FaceFlag.RIGHT);
    const isFrontVisible = !block.block.faceVisibility || FaceVisibilityHelper.isVisible(block.block.faceVisibility, FaceFlag.FRONT);
    const isBackVisible = !block.block.faceVisibility || FaceVisibilityHelper.isVisible(block.block.faceVisibility, FaceFlag.BACK);

    // Get materials for visible faces
    const topMaterial = isTopVisible && topIndex ? await renderService.materialService.getMaterial(modifier, topIndex) : null;
    const bottomMaterial = isBottomVisible && bottomIndex ? await renderService.materialService.getMaterial(modifier, bottomIndex) : null;
    const leftMaterial = isLeftVisible && leftIndex ? await renderService.materialService.getMaterial(modifier, leftIndex) : null;
    const rightMaterial = isRightVisible && rightIndex ? await renderService.materialService.getMaterial(modifier, rightIndex) : null;
    const frontMaterial = isFrontVisible && frontIndex ? await renderService.materialService.getMaterial(modifier, frontIndex) : null;
    const backMaterial = isBackVisible && backIndex ? await renderService.materialService.getMaterial(modifier, backIndex) : null;

    logger.debug('Face visibility and materials', {
      faces: {
        top: { visible: isTopVisible, hasMaterial: !!topMaterial },
        bottom: { visible: isBottomVisible, hasMaterial: !!bottomMaterial },
        left: { visible: isLeftVisible, hasMaterial: !!leftMaterial },
        right: { visible: isRightVisible, hasMaterial: !!rightMaterial },
        front: { visible: isFrontVisible, hasMaterial: !!frontMaterial },
        back: { visible: isBackVisible, hasMaterial: !!backMaterial }
      }
    });

    const size = 1;

    // Block center for rotation
    const centerX = worldX + size / 2;
    const centerY = worldY + size / 2;
    const centerZ = worldZ + size / 2;

    const corners = [
      // Bottom face (y = y)
      [worldX, worldY, worldZ], // 0: left-back-bottom
      [worldX + size, worldY, worldZ], // 1: right-back-bottom
      [worldX + size, worldY, worldZ + size], // 2: right-front-bottom
      [worldX, worldY, worldZ + size], // 3: left-front-bottom
      // Top face (y = y + size)
      [worldX, worldY + size, worldZ], // 4: left-back-top
      [worldX + size, worldY + size, worldZ], // 5: right-back-top
      [worldX + size, worldY + size, worldZ + size], // 6: right-front-top
      [worldX, worldY + size, worldZ + size], // 7: left-front-top
    ];

    // Apply edge offsets if available
    const offsets = block.block.offsets;
    if (offsets) {
      for (let i = 0; i < 8; i++) {
        const offsetX = offsets[i * 3];
        const offsetY = offsets[i * 3 + 1];
        const offsetZ = offsets[i * 3 + 2];

        corners[i][0] += offsetX ?? 0;
        corners[i][1] += offsetY ?? 0;
        corners[i][2] += offsetZ ?? 0;
      }
    }

    // Apply scaling if specified (after offsets, before rotation)
    const scalingX = modifier.visibility?.scalingX ?? 1.0;
    const scalingY = modifier.visibility?.scalingY ?? 1.0;
    const scalingZ = modifier.visibility?.scalingZ ?? 1.0;

    if (scalingX !== 1.0 || scalingY !== 1.0 || scalingZ !== 1.0) {
      for (let i = 0; i < 8; i++) {
        // Translate to origin (relative to center)
        corners[i][0] -= centerX;
        corners[i][1] -= centerY;
        corners[i][2] -= centerZ;

        // Apply scaling
        corners[i][0] *= scalingX;
        corners[i][1] *= scalingY;
        corners[i][2] *= scalingZ;

        // Translate back
        corners[i][0] += centerX;
        corners[i][1] += centerY;
        corners[i][2] += centerZ;
      }
    }

    // Apply rotation if specified (after scaling)
    const rotationX = modifier.visibility?.rotationX ?? 0;
    const rotationY = modifier.visibility?.rotationY ?? 0;

    if (rotationX !== 0 || rotationY !== 0) {
      // Convert degrees to radians
      const radX = rotationX * Math.PI / 180;
      const radY = rotationY * Math.PI / 180;

      // Create rotation matrix using Babylon.js
      // YawPitchRoll corresponds to Y, X, Z rotations (Z is 0)
      const rotationMatrix = Matrix.RotationYawPitchRoll(radY, radX, 0);

      // Apply rotation to each corner around the block center
      for (let i = 0; i < 8; i++) {
        // Translate to origin (relative to center)
        const relativePos = new Vector3(
          corners[i][0] - centerX,
          corners[i][1] - centerY,
          corners[i][2] - centerZ
        );

        // Apply rotation
        const rotatedPos = Vector3.TransformCoordinates(relativePos, rotationMatrix);

        // Translate back and update corner
        corners[i][0] = rotatedPos.x + centerX;
        corners[i][1] = rotatedPos.y + centerY;
        corners[i][2] = rotatedPos.z + centerZ;
      }
    }

    // Render visible faces (with or without material)
    let facesRendered = 0;

    // Top face (y = y + size)
    if (isTopVisible) {
      const texture = textures[topIndex] ? this.normalizeTexture(textures[topIndex]) : null;
      vertexOffset = await this.addFace(
        corners[4], corners[5], corners[6], corners[7],  // left-back, right-back, right-front, left-front
        [0, 1, 0],  // Normal pointing up
        texture,
        faceData,
        vertexOffset
      );
      facesRendered++;
    }

    // Bottom face (y = y)
    if (isBottomVisible) {
      const texture = textures[bottomIndex] ? this.normalizeTexture(textures[bottomIndex]) : null;
      vertexOffset = await this.addFace(
        corners[0], corners[3], corners[2], corners[1],  // left-back, left-front, right-front, right-back
        [0, -1, 0],  // Normal pointing down
        texture,
        faceData,
        vertexOffset
      );
      facesRendered++;
    }

    // Left face (x = x)
    if (isLeftVisible) {
      const texture = textures[leftIndex] ? this.normalizeTexture(textures[leftIndex]) : null;
      vertexOffset = await this.addFace(
        corners[0], corners[4], corners[7], corners[3],  // back-bottom, back-top, front-top, front-bottom
        [-1, 0, 0],  // Normal pointing left
        texture,
        faceData,
        vertexOffset
      );
      facesRendered++;
    }

    // Right face (x = x + size)
    if (isRightVisible) {
      const texture = textures[rightIndex] ? this.normalizeTexture(textures[rightIndex]) : null;
      vertexOffset = await this.addFace(
        corners[1], corners[2], corners[6], corners[5],  // back-bottom, front-bottom, front-top, back-top
        [1, 0, 0],  // Normal pointing right
        texture,
        faceData,
        vertexOffset
      );
      facesRendered++;
    }

    // Front face (z = z + size)
    if (isFrontVisible) {
      const texture = textures[frontIndex] ? this.normalizeTexture(textures[frontIndex]) : null;
      vertexOffset = await this.addFace(
        corners[3], corners[7], corners[6], corners[2],  // left-bottom, left-top, right-top, right-bottom
        [0, 0, 1],  // Normal pointing forward
        texture,
        faceData,
        vertexOffset
      );
      facesRendered++;
    }

    // Back face (z = z)
    if (isBackVisible) {
      const texture = textures[backIndex] ? this.normalizeTexture(textures[backIndex]) : null;
      vertexOffset = await this.addFace(
        corners[0], corners[1], corners[5], corners[4],  // left-bottom, right-bottom, right-top, left-top
        [0, 0, -1],  // Normal pointing backward
        texture,
        faceData,
        vertexOffset
      );
      facesRendered++;
    }

    logger.debug('Cube rendered', {
      blockTypeId: block.blockType.id,
      position: { x: worldX, y: worldY, z: worldZ },
      facesRendered,
      vertexOffset
    });

    return vertexOffset;
  }

  /**
   * Add a face to the mesh data
   * @param corner0 - First corner position [x, y, z]
   * @param corner1 - Second corner position [x, y, z]
   * @param corner2 - Third corner position [x, y, z]
   * @param corner3 - Fourth corner position [x, y, z]
   * @param normal - Face normal vector [x, y, z]
   * @param texture - Texture definition for the face
   * @param faceData - Face data to append to
   * @param vertexOffset - Current vertex offset
   * @returns New vertex offset after adding this face
   */
  private async addFace(
    corner0: number[],
    corner1: number[],
    corner2: number[],
    corner3: number[],
    normal: number[],
    texture: TextureDefinition | null,
    faceData: FaceData,
    vertexOffset: number
  ): Promise<number> {
    // Add 4 vertices (positions)
    faceData.positions.push(
      corner0[0], corner0[1], corner0[2],
      corner1[0], corner1[1], corner1[2],
      corner2[0], corner2[1], corner2[2],
      corner3[0], corner3[1], corner3[2]
    );

    // Add 4 normals (same normal for all vertices of this face)
    for (let i = 0; i < 4; i++) {
      faceData.normals.push(normal[0], normal[1], normal[2]);
    }

    // Add 4 UV coordinates
    if (texture && this.textureAtlas) {
      const atlasUV = await this.textureAtlas.getTextureUV(texture);
      if (atlasUV) {
        // Map standard face UVs to atlas coordinates
        faceData.uvs.push(
          atlasUV.u0, atlasUV.v0,  // corner0: bottom-left
          atlasUV.u1, atlasUV.v0,  // corner1: bottom-right
          atlasUV.u1, atlasUV.v1,  // corner2: top-right
          atlasUV.u0, atlasUV.v1   // corner3: top-left
        );
      } else {
        // Default UVs if texture not found
        faceData.uvs.push(0, 0, 1, 0, 1, 1, 0, 1);
      }
    } else {
      // Default UVs if no texture
      faceData.uvs.push(0, 0, 1, 0, 1, 1, 0, 1);
    }

    // Add 6 indices (2 triangles)
    const i0 = vertexOffset;
    const i1 = vertexOffset + 1;
    const i2 = vertexOffset + 2;
    const i3 = vertexOffset + 3;

    // Triangle 1: 0-1-2 (counter-clockwise for front face)
    faceData.indices.push(i0, i1, i2);
    // Triangle 2: 0-2-3 (counter-clockwise for front face)
    faceData.indices.push(i0, i2, i3);

    return vertexOffset + 4;  // 4 vertices added
  }

}
