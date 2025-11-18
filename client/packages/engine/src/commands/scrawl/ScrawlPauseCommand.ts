/**
 * ScrawlPauseCommand - Pause a running scrawl script
 */

import { CommandHandler } from '../CommandHandler';
import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../../AppContext';

const logger = getLogger('ScrawlPauseCommand');

export class ScrawlPauseCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'scrawl.pause';
  }

  description(): string {
    return 'Pause a running scrawl script by executor ID (scrawl.pause <executorId>)';
  }

  async execute(args: string[]): Promise<void> {
    const { appContext } = this;

    if (!appContext.services.scrawl) {
      logger.error('ScrawlService not available');
      return;
    }

    if (args.length === 0) {
      logger.error('Usage: scrawl.pause <executorId>');
      logger.info('Example: scrawl.pause executor_0');
      return;
    }

    const scrawlService = appContext.services.scrawl;
    const executorId = args[0];

    const success = scrawlService.pauseExecutor(executorId);
    if (success) {
      logger.info(`Executor ${executorId} paused`);
    } else {
      logger.error(`Executor ${executorId} not found`);
    }
  }
}
