/**
 * FogRenderer - Renders fog blocks
 *
 * Creates a cube-shaped fog volume that fills the entire block space.
 * Uses a special fog shader for volumetric fog effect.
 *
 * Fog parameters can be specified in visibility.effectParameters:
 * Format: "density" (e.g., "0.5")
 * - density: Fog thickness (0.0 = transparent, 1.0 = opaque), default: 0.3
 */

import { getLogger } from '@nimbus/shared';
import type { ClientBlock } from '../types';
import type { BlockModifier, TextureDefinition } from '@nimbus/shared';
import { BlockRenderer } from './BlockRenderer';
import type { RenderContext } from '../services/RenderService';
import type { TextureAtlas } from './TextureAtlas';

const logger = getLogger('FogRenderer');

/**
 * FogRenderer - Renders fog blocks as semi-transparent cubes
 *
 * Creates cube geometry similar to CubeRenderer but uses fog shader material.
 * Supports all transformations: offsets, scaling, rotation.
 */
export class FogRenderer extends BlockRenderer {
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

    if (typeof texture === 'object' && texture.path) {
      return texture as TextureDefinition;
    }

    if (typeof texture === 'string') {
      return { path: texture };
    }

    return null;
  }

  /**
   * Render a fog block
   */
  async render(
    renderContext: RenderContext,
    block: ClientBlock,
  ): Promise<void> {
    const worldX = block.block.position.x;
    const worldY = block.block.position.y;
    const worldZ = block.block.position.z;

    const modifier = block.currentModifier;

    if (!modifier || !modifier.visibility) {
      logger.debug('Fog block has no visibility modifier', { blockTypeId: block.blockType.id });
      return;
    }

    const textures = modifier.visibility.textures;
    if (!textures) {
      logger.warn('Fog block has no textures', { blockTypeId: block.blockType.id });
      return;
    }

    const size = 1;

    // Block center for rotation
    const centerX = worldX + size / 2;
    const centerY = worldY + size / 2;
    const centerZ = worldZ + size / 2;

    // Define 8 corners of the cube
    const corners = [
      [worldX, worldY, worldZ], // 0: left-back-bottom
      [worldX + size, worldY, worldZ], // 1: right-back-bottom
      [worldX + size, worldY, worldZ + size], // 2: right-front-bottom
      [worldX, worldY, worldZ + size], // 3: left-front-bottom
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

    // Apply scaling
    const scalingX = modifier.visibility?.scalingX ?? 1.0;
    const scalingY = modifier.visibility?.scalingY ?? 1.0;
    const scalingZ = modifier.visibility?.scalingZ ?? 1.0;

    if (scalingX !== 1.0 || scalingY !== 1.0 || scalingZ !== 1.0) {
      for (let i = 0; i < 8; i++) {
        corners[i][0] -= centerX;
        corners[i][1] -= centerY;
        corners[i][2] -= centerZ;

        corners[i][0] *= scalingX;
        corners[i][1] *= scalingY;
        corners[i][2] *= scalingZ;

        corners[i][0] += centerX;
        corners[i][1] += centerY;
        corners[i][2] += centerZ;
      }
    }

    // Apply rotation
    const rotationX = modifier.visibility?.rotationX ?? 0;
    const rotationY = modifier.visibility?.rotationY ?? 0;

    if (rotationX !== 0 || rotationY !== 0) {
      const { Matrix, Vector3 } = await import('@babylonjs/core');
      const radX = rotationX * Math.PI / 180;
      const radY = rotationY * Math.PI / 180;

      const rotationMatrix = Matrix.RotationYawPitchRoll(radY, radX, 0);

      for (let i = 0; i < 8; i++) {
        const relativePos = new Vector3(
          corners[i][0] - centerX,
          corners[i][1] - centerY,
          corners[i][2] - centerZ
        );

        const rotatedPos = Vector3.TransformCoordinates(relativePos, rotationMatrix);

        corners[i][0] = rotatedPos.x + centerX;
        corners[i][1] = rotatedPos.y + centerY;
        corners[i][2] = rotatedPos.z + centerZ;
      }
    }

    // Determine texture (use texture 0 or ALL)
    const textureIndex = textures[0] ? 0 : (textures[7] ? 7 : 0);
    const texture = textures[textureIndex] ? this.normalizeTexture(textures[textureIndex]) : null;

    // Render all 6 faces (fog fills entire cube)
    // Top face
    await this.addFace(
      corners[4], corners[5], corners[6], corners[7],
      [0, 1, 0],
      texture,
      modifier,
      renderContext
    );

    // Bottom face
    await this.addFace(
      corners[0], corners[3], corners[2], corners[1],
      [0, -1, 0],
      texture,
      modifier,
      renderContext
    );

    // Left face
    await this.addFace(
      corners[0], corners[3], corners[7], corners[4],
      [-1, 0, 0],
      texture,
      modifier,
      renderContext,
      true
    );

    // Right face
    await this.addFace(
      corners[2], corners[1], corners[5], corners[6],
      [1, 0, 0],
      texture,
      modifier,
      renderContext,
      true
    );

    // Front face
    await this.addFace(
      corners[3], corners[2], corners[6], corners[7],
      [0, 0, 1],
      texture,
      modifier,
      renderContext,
      true
    );

    // Back face
    await this.addFace(
      corners[1], corners[0], corners[4], corners[5],
      [0, 0, -1],
      texture,
      modifier,
      renderContext,
      true
    );

    logger.debug('Fog cube rendered', {
      blockTypeId: block.blockType.id,
      position: { x: worldX, y: worldY, z: worldZ },
    });
  }

  /**
   * Add a face to the mesh data
   */
  private async addFace(
    corner0: number[],
    corner1: number[],
    corner2: number[],
    corner3: number[],
    normal: number[],
    texture: TextureDefinition | null,
    modifier: BlockModifier,
    renderContext: RenderContext,
    reverseWinding: boolean = false
  ): Promise<void> {
    const faceData = renderContext.faceData;

    // Add 4 vertices (positions)
    faceData.positions.push(
      corner0[0], corner0[1], corner0[2],
      corner1[0], corner1[1], corner1[2],
      corner2[0], corner2[1], corner2[2],
      corner3[0], corner3[1], corner3[2]
    );

    // Add 4 normals
    for (let i = 0; i < 4; i++) {
      faceData.normals.push(normal[0], normal[1], normal[2]);
    }

    // Add 4 UV coordinates
    if (texture && this.textureAtlas) {
      const atlasUV = await this.textureAtlas.getTextureUV(texture);
      if (atlasUV) {
        const isHorizontalFace = normal[1] !== 0;

        if (isHorizontalFace) {
          faceData.uvs.push(
            atlasUV.u0, atlasUV.v0,
            atlasUV.u1, atlasUV.v0,
            atlasUV.u1, atlasUV.v1,
            atlasUV.u0, atlasUV.v1
          );
        } else {
          faceData.uvs.push(
            atlasUV.u0, atlasUV.v1,
            atlasUV.u1, atlasUV.v1,
            atlasUV.u1, atlasUV.v0,
            atlasUV.u0, atlasUV.v0
          );
        }
      } else {
        faceData.uvs.push(0, 0, 1, 0, 1, 1, 0, 1);
      }
    } else {
      faceData.uvs.push(0, 0, 1, 0, 1, 1, 0, 1);
    }

    // Add 6 indices (2 triangles)
    const i0 = renderContext.vertexOffset;
    const i1 = renderContext.vertexOffset + 1;
    const i2 = renderContext.vertexOffset + 2;
    const i3 = renderContext.vertexOffset + 3;

    if (reverseWinding) {
      faceData.indices.push(i0, i2, i1);
      faceData.indices.push(i0, i3, i2);
    } else {
      faceData.indices.push(i0, i1, i2);
      faceData.indices.push(i0, i2, i3);
    }

    // Add wind attributes and colors (uses helper from base class)
    this.addWindAttributesAndColors(faceData, modifier, 4);

    renderContext.vertexOffset += 4;
  }
}
