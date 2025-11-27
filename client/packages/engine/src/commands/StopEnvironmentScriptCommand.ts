/**
 * StopEnvironmentScriptCommand - Stop an environment script by group
 */

import { CommandHandler } from './CommandHandler';
import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../AppContext';

const logger = getLogger('StopEnvironmentScriptCommand');

/**
 * Stop environment script command
 *
 * Usage:
 *   stopEnvironmentScript <group>
 *
 * Parameters:
 *   group - Script group to stop (e.g., 'environment', 'weather', 'daytime')
 *
 * Examples:
 *   stopEnvironmentScript("weather")
 *   stopEnvironmentScript("daytime")
 */
export class StopEnvironmentScriptCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'stopEnvironmentScript';
  }

  description(): string {
    return 'Stop an environment script by group';
  }

  async execute(parameters: any[]): Promise<any> {
    const environmentService = this.appContext.services.environment;

    if (!environmentService) {
      logger.error('EnvironmentService not available');
      return { error: 'EnvironmentService not available' };
    }

    // Validate parameters
    if (parameters.length < 1) {
      logger.error('Usage: stopEnvironmentScript <group>');
      return {
        error: 'Missing parameters. Usage: stopEnvironmentScript <group>',
      };
    }

    const group = parameters[0];

    // Validate group
    if (typeof group !== 'string' || group.trim() === '') {
      logger.error('Script group must be a non-empty string');
      return { error: 'Script group must be a non-empty string' };
    }

    // Stop the script
    const stopped = await environmentService.stopEnvironmentScriptByGroup(group);

    if (stopped) {
      const message = `Environment script stopped for group: ${group}`;
      logger.info(message);
      return {
        group,
        stopped: true,
        message,
      };
    } else {
      const message = `No running script found for group: ${group}`;
      logger.info(message);
      return {
        group,
        stopped: false,
        message,
      };
    }
  }
}
