/**
 * ScrawlStatusCommand - Show status of running executors
 */

import { CommandHandler } from '../CommandHandler';
import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../../AppContext';

const logger = getLogger('ScrawlStatusCommand');

export class ScrawlStatusCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'scrawlStatus';
  }

  description(): string {
    return 'Show status of running scrawl executors (scrawlStatus [executorId])';
  }

  async execute(args: string[]): Promise<void> {
    const { appContext } = this;

    if (!appContext.services.scrawl) {
      logger.error('ScrawlService not available');
      return;
    }

    const scrawlService = appContext.services.scrawl;

    if (args.length === 0) {
      // Show all running executors
      const runningIds = scrawlService.getRunningExecutorIds();

      logger.info('=== Running Scrawl Executors ===');
      if (runningIds.length === 0) {
        logger.info('No scripts currently running');
      } else {
        logger.info(`Total running: ${runningIds.length}`);
        logger.info('');
        runningIds.forEach((id: string) => {
          const executor = scrawlService.getExecutor(id);
          if (executor) {
            const status = executor.isCancelled()
              ? 'cancelled'
              : executor.isPaused()
              ? 'paused'
              : 'running';
            logger.info(`  ${id}:`);
            logger.info(`    Script: ${executor.getScriptId()}`);
            logger.info(`    Status: ${status}`);
          }
        });
      }
    } else {
      // Show specific executor
      const executorId = args[0];
      const executor = scrawlService.getExecutor(executorId);

      if (!executor) {
        logger.error(`Executor ${executorId} not found`);
        return;
      }

      logger.info(`=== Executor ${executorId} ===`);
      logger.info(`Script ID: ${executor.getScriptId()}`);
      logger.info(`Cancelled: ${executor.isCancelled()}`);
      logger.info(`Paused: ${executor.isPaused()}`);
    }
  }
}
