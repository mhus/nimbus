/**
 * ShadowsDistanceCommand - Set shadow rendering distance
 *
 * Usage: shadowsDistance <blocks>
 * Sets the maximum distance shadows are rendered (reduces performance impact)
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger, toNumber } from '@nimbus/shared';

const logger = getLogger('ShadowsDistanceCommand');

export class ShadowsDistanceCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'shadowsDistance';
  }

  description(): string {
    return 'Set shadow rendering distance in blocks (default: 100)';
  }

  async execute(parameters: any[]): Promise<string> {
    if (parameters.length === 0) {
      return 'Usage: shadowsDistance <blocks>\nExample: shadowsDistance 50';
    }

    const distance = toNumber(parameters[0]);

    if (isNaN(distance) || distance < 10 || distance > 5000) {
      return 'Invalid distance. Must be between 10 and 5000 blocks.';
    }

    const envService = this.appContext.services.environment;
    if (!envService) {
      return 'EnvironmentService not available';
    }

    const shadowGenerator = envService.getShadowGenerator();
    if (!shadowGenerator) {
      return 'Shadow generator not initialized';
    }

    // Set shadowMaxZ (controls how far shadows are rendered)
    const shadowMaxZ = distance * 10; // Convert blocks to units
    shadowGenerator.shadowMaxZ = shadowMaxZ;

    // Also update camera maxZ to match
    const cameraService = this.appContext.services.camera;
    if (cameraService) {
      const camera = cameraService.getCamera();
      if (camera) {
        camera.maxZ = Math.max(shadowMaxZ, 1000);
      }
    }

    // NOTE: DO NOT call splitFrustum() here - it causes duplicates/issues
    // The shadow frustum will auto-adjust

    logger.info('Shadow distance changed', {
      distance,
      shadowMaxZ,
    });

    return `Shadow distance set to ${distance} blocks (shadowMaxZ: ${shadowMaxZ})

Smaller distance = better performance, shorter shadows
Larger distance = worse performance, longer shadows

Current: ${distance} blocks`;
  }
}
