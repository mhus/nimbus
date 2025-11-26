/**
 * PrecipitationStartCommand - Start precipitation with full control
 *
 * Usage: precipitationStart [intensity] [r] [g] [b] [size] [speed] [gravity]
 * - intensity: 0-100
 * - r, g, b: Color (0.0-1.0)
 * - size: Particle size
 * - speed: Emit power (fall speed)
 * - gravity: Gravity strength
 *
 * Example: precipitationStart 50 0.4 0.4 0.6 0.3 25 15
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { Color4 } from '@babylonjs/core';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('PrecipitationStartCommand');

export class PrecipitationStartCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'precipitationStart';
  }

  description(): string {
    return 'Start precipitation with custom parameters (intensity r g b size speed gravity)';
  }

  async execute(parameters: string[]): Promise<string> {
    const precipitationService = this.appContext.services.precipitation;
    if (!precipitationService) {
      return 'PrecipitationService not available';
    }

    // Parse parameters
    if (parameters.length < 7) {
      return 'Usage: precipitationStart [intensity] [r] [g] [b] [size] [speed] [gravity]\nExample: precipitationStart 50 0.4 0.4 0.6 0.3 25 15';
    }

    const intensity = parseFloat(parameters[0]);
    const r = parseFloat(parameters[1]);
    const g = parseFloat(parameters[2]);
    const b = parseFloat(parameters[3]);
    const size = parseFloat(parameters[4]);
    const speed = parseFloat(parameters[5]);
    const gravity = parseFloat(parameters[6]);

    // Validate
    if (isNaN(intensity) || intensity < 0 || intensity > 100) {
      return 'Intensity must be 0-100';
    }
    if (isNaN(r) || isNaN(g) || isNaN(b) || r < 0 || g < 0 || b < 0 || r > 1 || g > 1 || b > 1) {
      return 'RGB values must be 0.0-1.0';
    }
    if (isNaN(size) || size <= 0) {
      return 'Size must be positive';
    }
    if (isNaN(speed) || speed < 0) {
      return 'Speed must be non-negative';
    }
    if (isNaN(gravity)) {
      return 'Gravity must be a number';
    }

    try {
      // Stop any existing precipitation
      if (precipitationService.isEnabled()) {
        precipitationService.setEnabled(false);
      }

      // Set all parameters
      precipitationService.setIntensity(intensity);
      precipitationService.setParticleSize(size);
      precipitationService.setParticleColor(new Color4(r, g, b, 1.0));
      precipitationService.setParticleSpeed(speed);
      precipitationService.setParticleGravity(gravity);

      // Start
      precipitationService.setEnabled(true);

      logger.info('Precipitation started', { intensity, color: { r, g, b }, size, speed, gravity });
      return `Precipitation started: intensity=${intensity}, color=(${r},${g},${b}), size=${size}, speed=${speed}, gravity=${gravity}`;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      logger.error('Failed to start precipitation', { error: errorMessage });
      return `Failed to start precipitation: ${errorMessage}`;
    }
  }
}
