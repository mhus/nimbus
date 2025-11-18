/**
 * ScrawlListCommand - List available scrawl scripts
 */

import { CommandHandler } from '../CommandHandler';
import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../../AppContext';

const logger = getLogger('ScrawlListCommand');

export class ScrawlListCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'scrawlList';
  }

  description(): string {
    return 'List available scrawl scripts and effects';
  }

  async execute(args: string[]): Promise<void> {
    const { appContext } = this;

    if (!appContext.services.scrawl) {
      logger.error('ScrawlService not available');
      return;
    }

    const scrawlService = appContext.services.scrawl;
    const library = scrawlService.getScriptLibrary();

    // Get all registered effects
    const effectKeys = scrawlService.getEffectFactory().getEffectKeys();

    logger.info('=== Scrawl Scripts ===');
    logger.info('Scripts in library: (implementation dependent)');
    logger.info('');

    logger.info('=== Registered Effects ===');
    if (effectKeys.length === 0) {
      logger.info('No effects registered');
    } else {
      effectKeys.forEach((key: string) => {
        logger.info(`  - ${key}`);
      });
    }
    logger.info('');

    logger.info('=== Running Executors ===');
    const runningIds = scrawlService.getRunningExecutorIds();
    if (runningIds.length === 0) {
      logger.info('No scripts currently running');
    } else {
      runningIds.forEach((id: string) => {
        const executor = scrawlService.getExecutor(id);
        if (executor) {
          const status = executor.isCancelled()
            ? 'cancelled'
            : executor.isPaused()
            ? 'paused'
            : 'running';
          logger.info(`  - ${id} (${executor.getScriptId()}) - ${status}`);
        }
      });
    }
  }
}
