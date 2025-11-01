/**
 * CubeRenderer - Renders cube-shaped blocks
 *
 * Creates mesh geometry for standard cube blocks with proper UV mapping.
 */

import { VertexData, Vector3 } from '@babylonjs/core';
import { getLogger } from '@nimbus/shared';
import type { Block, BlockType, TextureDefinition, TextureKey } from '@nimbus/shared';
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
      worldZ: number
  ): Promise<number> {
    // Get block modifier for current status (from BlockType.initialStatus)

    const modifier = block.currentModifier;

    if (!modifier || !modifier.visibility) {
      logger.debug('Block has no visibility modifier', { blockTypeId: block.blockTypeId, status });
      return vertexOffset;
    }

    // Get textures from modifier
    const textures = modifier.visibility.textures;
    if (!textures) {
      logger.warn('Block has no textures', { blockTypeId: block.blockTypeId, status });
      return vertexOffset;
    }

    const topIndex = texture[1] ? 1 : 0;
    const bottomIndex = texture[2] ? 2 : 0;
    const leftIndex = texture[3] ? 3 : (texture[7] ? 7 : 0);
    const rightIndex = texture[4] ? 4 : (texture[7] ? 7 : 0);
    const frontIndex = texture[5] ? 5 : (texture[7] ? 7 : 0);
    const backIndex = texture[6] ? 6 : (texture[7] ? 7 : 0);

    const topMaterial = topIndex && (!block.faceVisibility || Block.isVisible(block.faceVisibility, FaceFlag.TOP)) ? renderService.materialService.getMaterial(modifier, topIndex) : null;
    const bottomMaterial = bottomTexture && (!block.faceVisibility || Block.isVisible(block.faceVisibility, FaceFlag.BOTTOM)) ? renderService.materialService.getMaterialForTexture(bottomTexture, modifier): null;
    const leftMaterial = leftTexture && (!block.faceVisibility || Block.isVisible(block.faceVisibility, FaceFlag.LEFT)) ? renderService.materialService.getMaterialForTexture(sideTexture, modifier) : null;
    const rightMaterial = rightTexture && (!block.faceVisibility || Block.isVisible(block.faceVisibility, FaceFlag.RIGHT)) ? renderService.materialService.getMaterialForTexture(sideTexture, modifier) : null;
    const frontMaterial = frontTexture && (!block.faceVisibility || Block.isVisible(block.faceVisibility, FaceFlag.FRONT)) ? renderService.materialService.getMaterialForTexture(sideTexture, modifier) : null;
    const backMaterial = backTexture && (!block.faceVisibility || Block.isVisible(block.faceVisibility, FaceFlag.BACK)) ? renderService.materialService.getMaterialForTexture(sideTexture, modifier) : null;

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
    const offsets = block.offsets;
    if (offsets) {
      for (let i = 0; i < 8; i++) {
        const offsetX = offsets[i * 3];
        const offsetY = offsets[i * 3 + 1];
        const offsetZ = offsets[i * 3 + 2];

        corners[i][0] += offsetX ? offsetX : 0;
        corners[i][1] += offsetY ? offsetY : 0;
        corners[i][2] += offsetZ ? offsetZ : 0;
      }
    }

    // Calculate rotation matrix from modifier
    let rotationMatrix: Matrix | null = null;
    // TODO apply rotation if specified in modifier.visibility

    // TODO rotate corners if rotationMatrix exists

    let vertexOffset = 0;
    // TODO Render all 6 faces if material exists, implement addFace method
//     if (topMaterial) {
//         vertexOffset = this.addFace(
//                 corners[?],
//                 corners[?],
//                 corners[?],
//                 corners[?],
//                 topMaterial
//             );
//     }

    return vertexOffset;
  }

}
