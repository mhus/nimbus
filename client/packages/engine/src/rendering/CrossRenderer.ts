/**
 * CrossRenderer - Renders cross-shaped blocks
 *
 * Creates mesh geometry for cross blocks (two diagonal faces intersecting).
 * Cross blocks consist of two vertical planes arranged diagonally.
 */

import { Vector3, Matrix } from '@babylonjs/core';
import { getLogger } from '@nimbus/shared';
import type { TextureDefinition } from '@nimbus/shared';
import type { ClientBlock } from '../types';
import { BlockRenderer } from './BlockRenderer';
import type { RenderContext } from '../services/RenderService';
import type { TextureAtlas } from './TextureAtlas';

const logger = getLogger('CrossRenderer');

/**
 * CrossRenderer - Renders cross blocks
 *
 * Creates optimized mesh data for cross-shaped blocks.
 * Supports offset, scaling, rotation, and UV mapping from texture atlas.
 */
export class CrossRenderer extends BlockRenderer {
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
   * Render a cross block into the mesh data
   *
   * Cross consists of two diagonal vertical planes:
   * - Plane 1: From corner (0,0,0)-(1,0,1) to (0,1,0)-(1,1,1)
   * - Plane 2: From corner (0,0,1)-(1,0,0) to (0,1,1)-(1,1,0)
   *
   * @param renderContext - Render context
   * @param block - The block to render
   */
  async render(renderContext: RenderContext, block: ClientBlock): Promise<void> {
    const renderService = renderContext.renderService;
    const worldX = block.block.position.x;
    const worldY = block.block.position.y;
    const worldZ = block.block.position.z;

    // Get block modifier for current status
    const modifier = block.currentModifier;

    if (!modifier || !modifier.visibility) {
      logger.debug('Block has no visibility modifier', { blockTypeId: block.blockType.id });
      return;
    }

    // Get textures from modifier
    const textures = modifier.visibility.textures;
    if (!textures) {
      logger.warn('Block has no textures', { blockTypeId: block.blockType.id });
      return;
    }

    logger.debug('Rendering cross block', {
      blockTypeId: block.blockType.id,
      position: { x: worldX, y: worldY, z: worldZ },
      hasTextures: !!textures,
      textureCount: Object.keys(textures).length
    });

    const size = 1;

    // Block center for transformations
    const centerX = worldX + size / 2;
    const centerY = worldY + size / 2;
    const centerZ = worldZ + size / 2;

    // Define 8 corners for cross geometry (same as cube, but we'll use different faces)
    // We use 8 corners to support offset deformation like cube
    const corners = [
      // Bottom corners
      [worldX, worldY, worldZ],              // 0: left-back-bottom
      [worldX + size, worldY, worldZ],       // 1: right-back-bottom
      [worldX + size, worldY, worldZ + size], // 2: right-front-bottom
      [worldX, worldY, worldZ + size],       // 3: left-front-bottom
      // Top corners
      [worldX, worldY + size, worldZ],       // 4: left-back-top
      [worldX + size, worldY + size, worldZ], // 5: right-back-top
      [worldX + size, worldY + size, worldZ + size], // 6: right-front-top
      [worldX, worldY + size, worldZ + size], // 7: left-front-top
    ];

    // Apply edge offsets if available (supports all 8 corners like cube)
    const offsets = modifier.visibility.offsets;
    if (offsets) {
      for (let i = 0; i < 8 && i * 3 + 2 < offsets.length; i++) {
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

      // Create rotation matrix
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

    // Get textures for the two diagonal planes
    // Use texture 0 for first diagonal, texture 1 for second diagonal
    // Fall back to texture 7 (SIDE) or texture 0 (ALL) if not defined
    const diag1Index = textures[0] ? 0 : (textures[7] ? 7 : 0);
    const diag2Index = textures[1] ? 1 : (textures[7] ? 7 : (textures[0] ? 0 : 1));

    const texture1 = textures[diag1Index] ? this.normalizeTexture(textures[diag1Index]) : null;
    const texture2 = textures[diag2Index] ? this.normalizeTexture(textures[diag2Index]) : null;

    // Render diagonal plane 1: (left-back-bottom, right-front-bottom, right-front-top, left-back-top)
    // This creates a diagonal from corner 0-2 to corner 4-6
    if (texture1) {
      // Front side
      await this.addFace(
        corners[0], corners[2], corners[6], corners[4],  // left-back-bottom, right-front-bottom, right-front-top, left-back-top
        [0.707, 0, -0.707],  // Normal pointing diagonally (normalized vector)
        texture1,
        renderContext
      );

      // Back side (reverse winding for backface)
      await this.addFace(
        corners[4], corners[6], corners[2], corners[0],  // Reversed order
        [-0.707, 0, 0.707],  // Opposite normal
        texture1,
        renderContext
      );
    }

    // Render diagonal plane 2: (left-front-bottom, right-back-bottom, right-back-top, left-front-top)
    // This creates a diagonal from corner 3-1 to corner 7-5
    if (texture2) {
      // Front side
      await this.addFace(
        corners[3], corners[1], corners[5], corners[7],  // left-front-bottom, right-back-bottom, right-back-top, left-front-top
        [-0.707, 0, -0.707],  // Normal pointing diagonally
        texture2,
        renderContext
      );

      // Back side (reverse winding for backface)
      await this.addFace(
        corners[7], corners[5], corners[1], corners[3],  // Reversed order
        [0.707, 0, 0.707],  // Opposite normal
        texture2,
        renderContext
      );
    }

    logger.debug('Cross rendered', {
      blockTypeId: block.blockType.id,
      position: { x: worldX, y: worldY, z: worldZ },
      planesRendered: (texture1 ? 2 : 0) + (texture2 ? 2 : 0)
    });
  }

  /**
   * Add a face to the mesh data
   *
   * @param corner0 - First corner position [x, y, z]
   * @param corner1 - Second corner position [x, y, z]
   * @param corner2 - Third corner position [x, y, z]
   * @param corner3 - Fourth corner position [x, y, z]
   * @param normal - Face normal vector [x, y, z]
   * @param texture - Texture definition for the face
   * @param renderContext - Render context
   */
  private async addFace(
    corner0: number[],
    corner1: number[],
    corner2: number[],
    corner3: number[],
    normal: number[],
    texture: TextureDefinition | null,
    renderContext: RenderContext
  ): Promise<void> {
    const faceData = renderContext.faceData;

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
    const i0 = renderContext.vertexOffset;
    const i1 = renderContext.vertexOffset + 1;
    const i2 = renderContext.vertexOffset + 2;
    const i3 = renderContext.vertexOffset + 3;

    // Triangle 1: 0-1-2 (counter-clockwise for front face)
    faceData.indices.push(i0, i1, i2);
    // Triangle 2: 0-2-3 (counter-clockwise for front face)
    faceData.indices.push(i0, i2, i3);

    renderContext.vertexOffset += 4;  // 4 vertices added
  }
}
