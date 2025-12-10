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

    logger.debug('=== Scrawl Scripts ===');
    logger.debug('Scripts in library: (implementation dependent)');
    logger.debug('');

    logger.debug('=== Registered Effects ===');
    if (effectKeys.length === 0) {
      logger.debug('No effects registered');
    } else {
      effectKeys.forEach((key: string) => {
        logger.debug(`  - ${key}`);
      });
    }
    logger.debug('');

    logger.debug('=== Running Executors ===');
    const runningIds = scrawlService.getRunningExecutorIds();
    logger.info(`Total running executors: ${runningIds.length}`);
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
        } else {
          logger.warn(`  - ${id} - EXECUTOR NOT FOUND (already cleaned up?)`);
        }
      });
    }
  }
}
