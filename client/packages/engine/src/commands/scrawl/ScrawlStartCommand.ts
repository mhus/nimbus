/**
 * ScrawlStartCommand - Start a scrawl script
 */

import { CommandHandler } from '../CommandHandler';
import { getLogger } from '@nimbus/shared';
import type { ScrawlScript } from '@nimbus/shared';
import type { AppContext } from '../../AppContext';

const logger = getLogger('ScrawlStartCommand');

export class ScrawlStartCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'scrawlStart';
  }

  description(): string {
    return 'Start a scrawl script by ID or inline JSON definition (scrawlStart <scriptId|json>)';
  }

  async execute(args: any[]): Promise<void> {
    const { appContext } = this;

    if (!appContext.services.scrawl) {
      logger.error('ScrawlService not available');
      return;
    }

    if (args.length === 0) {
      logger.error('Usage: scrawlStart <scriptId|json|object>');
      logger.info('Examples:');
      logger.info('  scrawlStart my-script');
      logger.info('  scrawlStart \'{"id":"test","root":{"kind":"Play","effectId":"test"}}\'');
      logger.info('  scrawlStart({id:"test",root:{kind:"Play",effectId:"log",ctx:{message:"Hi"}}})');
      return;
    }

    const scrawlService = appContext.services.scrawl;

    try {
      let executorId: string;
      let script: ScrawlScript | string;

      // Check if first argument is already an object (from doScrawlStart)
      if (typeof args[0] === 'object' && args[0] !== null) {
        // Direct object passed
        script = args[0] as ScrawlScript;
        logger.info(`Starting inline script (object): ${script.id}`);
        executorId = await scrawlService.executeScript(script);
      } else {
        // String argument - could be JSON or script ID
        const input = args.join(' ');

        if (input.trim().startsWith('{')) {
          // Parse as JSON string
          script = JSON.parse(input);
          logger.info(`Starting inline script (JSON): ${(script as ScrawlScript).id}`);
          executorId = await scrawlService.executeScript(script as ScrawlScript);
        } else {
          // Treat as script ID
          logger.info(`Starting script: ${input}`);
          executorId = await scrawlService.executeScript(input);
        }
      }

      logger.info(`Script started with executor ID: ${executorId}`);
    } catch (error: any) {
      logger.error('Failed to start script', { error: error.message });
    }
  }
}
