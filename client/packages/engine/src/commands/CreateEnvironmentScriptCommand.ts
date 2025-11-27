/**
 * CreateEnvironmentScriptCommand - Create/register an environment script
 */

import { CommandHandler } from './CommandHandler';
import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { ScriptActionDefinition } from '@nimbus/shared';

const logger = getLogger('CreateEnvironmentScriptCommand');

/**
 * Create environment script command
 *
 * Usage:
 *   createEnvironmentScript <name> <group> <scriptDefinition>
 *
 * Parameters:
 *   name             - Script name (unique identifier)
 *   group            - Script group (e.g., 'environment', 'weather', 'daytime')
 *   scriptDefinition - Script action definition (JSON object or inline object)
 *
 * Examples:
 *   createEnvironmentScript("rain_storm", "weather", {script:{id:"rain",root:{kind:"Play",effectId:"rain"}}})
 *   createEnvironmentScript("day_cycle", "daytime", {scriptId:"daytime_day"})
 */
export class CreateEnvironmentScriptCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'createEnvironmentScript';
  }

  description(): string {
    return 'Create/register an environment script (name group scriptDefinition)';
  }

  execute(parameters: any[]): any {
    const environmentService = this.appContext.services.environment;

    if (!environmentService) {
      logger.error('EnvironmentService not available');
      return { error: 'EnvironmentService not available' };
    }

    // Validate parameters
    if (parameters.length < 3) {
      logger.error('Usage: createEnvironmentScript <name> <group> <scriptDefinition>');
      return {
        error: 'Missing parameters. Usage: createEnvironmentScript <name> <group> <scriptDefinition>',
      };
    }

    const name = parameters[0];
    const group = parameters[1];
    let scriptDefinition: ScriptActionDefinition;

    // Validate name
    if (typeof name !== 'string' || name.trim() === '') {
      logger.error('Script name must be a non-empty string');
      return { error: 'Script name must be a non-empty string' };
    }

    // Validate group
    if (typeof group !== 'string' || group.trim() === '') {
      logger.error('Script group must be a non-empty string');
      return { error: 'Script group must be a non-empty string' };
    }

    // Parse script definition
    try {
      if (typeof parameters[2] === 'object' && parameters[2] !== null) {
        scriptDefinition = parameters[2] as ScriptActionDefinition;
      } else if (typeof parameters[2] === 'string') {
        scriptDefinition = JSON.parse(parameters[2]);
      } else {
        throw new Error('Invalid script definition format');
      }
    } catch (error: any) {
      logger.error('Failed to parse script definition', { error: error.message });
      return { error: `Failed to parse script definition: ${error.message}` };
    }

    // Create the script
    environmentService.createEnvironmentScript(name, group, scriptDefinition);

    const message = `Environment script created: ${name} (group: ${group})`;
    logger.info(message);

    return {
      name,
      group,
      message,
    };
  }
}
