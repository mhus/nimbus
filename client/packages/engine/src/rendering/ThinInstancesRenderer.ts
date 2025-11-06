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

    // Get chunk key for tracking
    const chunkTransfer = clientBlock.chunk?.data?.transfer;
    const chunkKey = chunkTransfer
      ? `chunk_${chunkTransfer.cx}_${chunkTransfer.cz}`
      : `chunk_${Math.floor(block.position.x / 32)}_${Math.floor(block.position.z / 32)}`;

    // Create thin instances
    try {
      const mesh = await thinInstancesService.createInstances(
        {
          texturePath: textureDef.path,
          instanceCount,
          blockPosition: block.position,
        },
        chunkKey
      );

      // Register mesh for disposal
      renderContext.resourcesToDispose.addMesh(mesh);

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
