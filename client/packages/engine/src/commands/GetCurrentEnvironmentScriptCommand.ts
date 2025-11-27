/**
 * GetCurrentEnvironmentScriptCommand - Get current running script name for a group
 */

import { CommandHandler } from './CommandHandler';
import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../AppContext';

const logger = getLogger('GetCurrentEnvironmentScriptCommand');

/**
 * Get current environment script command
 *
 * Usage:
 *   getCurrentEnvironmentScript <group>
 *
 * Parameters:
 *   group - Script group (e.g., 'environment', 'weather', 'daytime')
 *
 * Examples:
 *   getCurrentEnvironmentScript("weather")
 *   getCurrentEnvironmentScript("daytime")
 *
 * Returns: Current running script name or null if no script is running in the group
 */
export class GetCurrentEnvironmentScriptCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'getCurrentEnvironmentScript';
  }

  description(): string {
    return 'Get current running script name for a group';
  }

  execute(parameters: any[]): any {
    const environmentService = this.appContext.services.environment;

    if (!environmentService) {
      logger.error('EnvironmentService not available');
      return { error: 'EnvironmentService not available' };
    }

    // Validate parameters
    if (parameters.length < 1) {
      logger.error('Usage: getCurrentEnvironmentScript <group>');
      return {
        error: 'Missing parameters. Usage: getCurrentEnvironmentScript <group>',
      };
    }

    const group = parameters[0];

    // Validate group
    if (typeof group !== 'string' || group.trim() === '') {
      logger.error('Script group must be a non-empty string');
      return { error: 'Script group must be a non-empty string' };
    }

    // Get current script name
    const scriptName = environmentService.getCurrentEnvironmentScriptName(group);

    if (scriptName) {
      const message = `Current script for group ${group}: ${scriptName}`;
      logger.debug(message);
      return {
        group,
        scriptName,
        message,
      };
    } else {
      const message = `No running script for group: ${group}`;
      logger.debug(message);
      return {
        group,
        scriptName: null,
        message,
      };
    }
  }
}
