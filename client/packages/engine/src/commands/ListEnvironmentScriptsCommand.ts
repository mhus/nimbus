/**
 * ListEnvironmentScriptsCommand - List all registered environment scripts
 */

import { CommandHandler } from './CommandHandler';
import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../AppContext';

const logger = getLogger('ListEnvironmentScriptsCommand');

/**
 * List environment scripts command
 *
 * Usage:
 *   listEnvironmentScripts
 *
 * Returns: List of all registered environment scripts with their names, groups, and running status
 *
 * Examples:
 *   listEnvironmentScripts
 */
export class ListEnvironmentScriptsCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'listEnvironmentScripts';
  }

  description(): string {
    return 'List all registered environment scripts';
  }

  execute(parameters: any[]): any {
    const environmentService = this.appContext.services.environment;

    if (!environmentService) {
      logger.error('EnvironmentService not available');
      return { error: 'EnvironmentService not available' };
    }

    // Get all scripts
    const allScripts = environmentService.getAllEnvironmentScripts();
    const runningScripts = environmentService.getRunningEnvironmentScripts();

    // Create a map of running scripts by group
    const runningByGroup = new Map<string, string>();
    for (const running of runningScripts) {
      runningByGroup.set(running.group, running.name);
    }

    // Build result list
    const scriptList = allScripts.map((script) => {
      const isRunning = runningByGroup.get(script.group) === script.name;
      return {
        name: script.name,
        group: script.group,
        running: isRunning,
        scriptId: script.script.scriptId || '(inline)',
      };
    });

    // Sort by group, then by name
    scriptList.sort((a, b) => {
      if (a.group !== b.group) {
        return a.group.localeCompare(b.group);
      }
      return a.name.localeCompare(b.name);
    });

    // Log summary
    logger.info(`Total environment scripts: ${scriptList.length}`);
    logger.info(`Running scripts: ${runningScripts.length}`);

    // Log each script
    if (scriptList.length === 0) {
      logger.info('No environment scripts registered');
    } else {
      logger.info('Environment scripts:');
      for (const script of scriptList) {
        const status = script.running ? '[RUNNING]' : '[STOPPED]';
        logger.info(`  ${status} ${script.name} (group: ${script.group}, scriptId: ${script.scriptId})`);
      }
    }

    return {
      total: scriptList.length,
      running: runningScripts.length,
      scripts: scriptList,
    };
  }
}
