/**
 * CloudsAnimationStartCommand - Start automated cloud animation
 *
 * Usage: cloudsAnimationStart <jobName> <emitCountPerMinute> <emitProbability> <minX> <maxX> <minZ> <maxZ> <minY> <maxY> <minWidth> <maxWidth> <minHeight> <maxHeight> <minDirection> <maxDirection> <speed> <texture1,texture2,...>
 *
 * Example: cloudsAnimationStart myJob 10 0.5 -100 100 -100 100 80 120 20 40 10 20 0 360 2.0 textures/cloud1.png,textures/cloud2.png
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import type { Area } from '../../services/CloudsService';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('CloudsAnimationStartCommand');

export class CloudsAnimationStartCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'cloudsAnimationStart';
  }

  description(): string {
    return 'Start automated cloud animation that creates clouds over time';
  }

  async execute(parameters: string[]): Promise<string> {
    const cloudsService = this.appContext.services.clouds;

    if (!cloudsService) {
      return 'CloudsService not available';
    }

    if (parameters.length < 17) {
      return 'Usage: cloudsAnimationStart <jobName> <emitCountPerMinute> <emitProbability> <minX> <maxX> <minZ> <maxZ> <minY> <maxY> <minWidth> <maxWidth> <minHeight> <maxHeight> <minDirection> <maxDirection> <speed> <texture1,texture2,...>';
    }

    try {
      const jobName = parameters[0];
      const emitCountPerMinute = parseFloat(parameters[1]);
      const emitProbability = parseFloat(parameters[2]);

      // Parse area
      const area: Area = {
        minX: parseFloat(parameters[3]),
        maxX: parseFloat(parameters[4]),
        minZ: parseFloat(parameters[5]),
        maxZ: parseFloat(parameters[6]),
        minY: parseFloat(parameters[7]),
        maxY: parseFloat(parameters[8]),
        minWidth: parseFloat(parameters[9]),
        maxWidth: parseFloat(parameters[10]),
        minHeight: parseFloat(parameters[11]),
        maxHeight: parseFloat(parameters[12]),
        minDirection: parseFloat(parameters[13]),
        maxDirection: parseFloat(parameters[14]),
      };

      const speed = parseFloat(parameters[15]);
      const textures = parameters[16].split(',').map(t => t.trim());

      // Validate numeric values
      if (isNaN(emitCountPerMinute) || isNaN(emitProbability) || isNaN(speed)) {
        return 'Invalid numeric parameters';
      }

      if (Object.values(area).some(isNaN)) {
        return 'Invalid area parameters';
      }

      cloudsService.startCloudsAnimation(
        jobName,
        emitCountPerMinute,
        emitProbability,
        area,
        speed,
        textures
      );

      logger.info('Cloud animation started via command', { jobName, emitCountPerMinute, emitProbability, speed });
      return `Cloud animation '${jobName}' started (${emitCountPerMinute} emits/min, ${textures.length} textures)`;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      logger.error('Failed to start cloud animation', { error: errorMessage });
      return `Failed to start cloud animation: ${errorMessage}`;
    }
  }
}
