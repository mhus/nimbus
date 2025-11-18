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
    return 'scrawl.start';
  }

  description(): string {
    return 'Start a scrawl script by ID or inline JSON definition (scrawl.start <scriptId|json>)';
  }

  async execute(args: string[]): Promise<void> {
    const { appContext } = this;

    if (!appContext.services.scrawl) {
      logger.error('ScrawlService not available');
      return;
    }

    if (args.length === 0) {
      logger.error('Usage: scrawl.start <scriptId|json>');
      logger.info('Examples:');
      logger.info('  scrawl.start my-script');
      logger.info('  scrawl.start \'{"id":"test","root":{"kind":"Play","effectId":"test"}}\'');
      return;
    }

    const scrawlService = appContext.services.scrawl;
    const input = args.join(' ');

    try {
      let executorId: string;

      // Check if input is JSON
      if (input.trim().startsWith('{')) {
        // Parse as inline script
        const script: ScrawlScript = JSON.parse(input);
        logger.info(`Starting inline script: ${script.id}`);
        executorId = await scrawlService.executeScript(script);
      } else {
        // Treat as script ID
        logger.info(`Starting script: ${input}`);
        executorId = await scrawlService.executeScript(input);
      }

      logger.info(`Script started with executor ID: ${executorId}`);
    } catch (error: any) {
      logger.error('Failed to start script', { error: error.message });
    }
  }
}
