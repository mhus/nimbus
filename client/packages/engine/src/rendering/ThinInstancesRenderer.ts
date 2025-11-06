/**
 * ThinInstancesRenderer - Renders THIN_INSTANCES blocks
 *
 * Uses ThinInstancesService to create highly performant instance groups.
 * Supports Y-axis billboards and GPU-based wind animation.
 *
 * Features:
 * - Instance count configurable via shaderParameters (default: 100)
 * - Random positioning within block bounds
 * - Y-axis-only billboard (stays vertical) via shader
 * - GPU wind animation via shader
 */

import { getLogger, Shape, TextureHelper } from '@nimbus/shared';
import type { ClientBlock } from '../types';
import { BlockRenderer } from './BlockRenderer';
import type { RenderContext } from '../services/RenderService';

const logger = getLogger('ThinInstancesRenderer');

export class ThinInstancesRenderer extends BlockRenderer {
  /**
   * ThinInstancesRenderer needs separate handling per block
   */
  needsSeparateMesh(): boolean {
    return true;
  }

  /**
   * Render a THIN_INSTANCES block
   */
  async render(renderContext: RenderContext, clientBlock: ClientBlock): Promise<void> {
    const block = clientBlock.block;
    const modifier = clientBlock.currentModifier;

    if (!modifier || !modifier.visibility) {
      logger.warn('ThinInstancesRenderer: No visibility modifier', { block });
      return;
    }

    // Validate shape
    const shape = modifier.visibility.shape ?? Shape.CUBE;
    if (shape !== Shape.THIN_INSTANCES) {
      logger.warn('ThinInstancesRenderer: Not a THIN_INSTANCES shape', { shape, block });
      return;
    }

    const thinInstancesService = renderContext.renderService.appContext.services.thinInstances;
    if (!thinInstancesService) {
      logger.error('ThinInstancesRenderer: ThinInstancesService not available');
      return;
    }

    // Get first texture
    const textures = modifier.visibility.textures;
    if (!textures || Object.keys(textures).length === 0) {
      logger.warn('ThinInstancesRenderer: No textures defined', { block });
      return;
    }

    const firstTexture = textures[0] || textures[1];
    if (!firstTexture) {
      logger.warn('ThinInstancesRenderer: No texture found', { block });
      return;
    }

    const textureDef = TextureHelper.normalizeTexture(firstTexture);

    // Get instance count from shaderParameters (default: 100)
    let instanceCount = 100;
    if (textureDef.shaderParameters) {
      const parsed = parseInt(textureDef.shaderParameters, 10);
      if (!isNaN(parsed) && parsed > 0) {
        instanceCount = parsed;
      }
    }

    // Get offset from visibility.offsets (if available)
    // offsets array: [x0,y0,z0, x1,y1,z1, ..., x7,y7,z7] (8 corners × 3 axes = 24 values)
    // Use first corner's offset (indices 0, 1, 2) for positioning
    let offset: { x: number; y: number; z: number } | undefined;
    if (modifier.visibility.offsets && modifier.visibility.offsets.length >= 3) {
      offset = {
        x: modifier.visibility.offsets[0],
        y: modifier.visibility.offsets[1],
        z: modifier.visibility.offsets[2],
      };
      logger.debug('Using offset from visibility.offsets', { offset });
    }

    // Get scaling from visibility (if available)
    let scaling: { x: number; y: number; z: number } | undefined;
    if (modifier.visibility.scalingX || modifier.visibility.scalingY || modifier.visibility.scalingZ) {
      scaling = {
        x: modifier.visibility.scalingX ?? 1,
        y: modifier.visibility.scalingY ?? 1,
        z: modifier.visibility.scalingZ ?? 1,
      };
      logger.debug('Using scaling from visibility', { scaling });
    }

    // Get chunk key for tracking
    const chunkTransfer = clientBlock.chunk?.data?.transfer;
    const chunkKey = chunkTransfer
      ? `chunk_${chunkTransfer.cx}_${chunkTransfer.cz}`
      : `chunk_${Math.floor(block.position.x / 32)}_${Math.floor(block.position.z / 32)}`;

    // Create thin instances
    try {
      const result = await thinInstancesService.createInstances(
        {
          texturePath: textureDef.path,
          instanceCount,
          blockPosition: block.position,
          offset,
          scaling,
        },
        chunkKey
      );

      // Register mesh and disposable for cleanup
      renderContext.resourcesToDispose.addMesh(result.mesh);
      renderContext.resourcesToDispose.add(result.disposable);

      logger.debug('ThinInstances rendered', {
        position: block.position,
        instanceCount,
      });
    } catch (error) {
      ExceptionHandler.handle(error, 'ThinInstancesRenderer.render', {
        position: block.position,
      });
    }
  }
}
