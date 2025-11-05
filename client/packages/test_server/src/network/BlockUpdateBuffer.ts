/**
 * BlockUpdateBuffer - Batches block updates before broadcasting to clients
 *
 * Collects block changes and broadcasts them in batches to reduce network traffic.
 * Uses two flush strategies:
 * 1. Time-based: Flush after FLUSH_INTERVAL (default 1 second)
 * 2. Size-based: Flush immediately when MAX_BATCH_SIZE is reached
 */

import { getLogger, type Block, ExceptionHandler } from '@nimbus/shared';

const logger = getLogger('BlockUpdateBuffer');

/**
 * Configuration for BlockUpdateBuffer
 */
export interface BlockUpdateBufferConfig {
  /** Time to wait before flushing (milliseconds) */
  flushInterval: number;

  /** Maximum blocks to batch before immediate flush */
  maxBatchSize: number;
}

/**
 * Default configuration
 */
const DEFAULT_CONFIG: BlockUpdateBufferConfig = {
  flushInterval: 1000, // 1 second
  maxBatchSize: 100, // 100 blocks
};

/**
 * BlockUpdateBuffer - Batches block updates for efficient broadcasting
 */
export class BlockUpdateBuffer {
  private pendingUpdates: Map<string, Block[]>; // worldId -> blocks[]
  private flushTimer: NodeJS.Timeout | null = null;
  private config: BlockUpdateBufferConfig;
  private flushCallback: (worldId: string, blocks: Block[]) => void;

  constructor(
    flushCallback: (worldId: string, blocks: Block[]) => void,
    config?: Partial<BlockUpdateBufferConfig>
  ) {
    this.pendingUpdates = new Map();
    this.config = { ...DEFAULT_CONFIG, ...config };
    this.flushCallback = flushCallback;

    logger.info('BlockUpdateBuffer initialized', {
      flushInterval: this.config.flushInterval,
      maxBatchSize: this.config.maxBatchSize,
    });
  }

  /**
   * Add a block update to the buffer
   *
   * @param worldId World ID
   * @param block Block to update
   */
  addUpdate(worldId: string, block: Block): void {
    try {
      // Get or create blocks array for this world
      let blocks = this.pendingUpdates.get(worldId);
      if (!blocks) {
        blocks = [];
        this.pendingUpdates.set(worldId, blocks);
      }

      // Add block to buffer
      blocks.push(block);

      logger.info('🔵 SERVER: Block update added to buffer', {
        worldId,
        position: block.position,
        blockTypeId: block.blockTypeId,
        bufferedCount: blocks.length,
        maxBatchSize: this.config.maxBatchSize,
      });

      // Check if we should flush immediately (size-based)
      if (blocks.length >= this.config.maxBatchSize) {
        logger.info('🔵 SERVER: Max batch size reached, flushing immediately', {
          worldId,
          count: blocks.length,
        });
        this.flushWorld(worldId);
      } else {
        // Schedule timer-based flush
        logger.info('🔵 SERVER: Scheduling timer-based flush', {
          worldId,
          bufferedCount: blocks.length,
          flushInterval: this.config.flushInterval,
        });
        this.scheduleFlush();
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'BlockUpdateBuffer.addUpdate', { worldId, block });
    }
  }

  /**
   * Schedule a flush after FLUSH_INTERVAL
   * Only schedules if not already scheduled
   */
  private scheduleFlush(): void {
    if (this.flushTimer !== null) {
      // Timer already running
      return;
    }

    this.flushTimer = setTimeout(() => {
      this.flush();
    }, this.config.flushInterval);

    logger.debug('Flush timer scheduled', { interval: this.config.flushInterval });
  }

  /**
   * Flush all pending updates for a specific world
   *
   * @param worldId World ID to flush
   */
  private flushWorld(worldId: string): void {
    const blocks = this.pendingUpdates.get(worldId);
    if (!blocks || blocks.length === 0) {
      return;
    }

    try {
      logger.info('🔵 SERVER: BlockUpdateBuffer flushing', {
        worldId,
        count: blocks.length,
        blocks: blocks.map(b => ({
          position: b.position,
          blockTypeId: b.blockTypeId,
        })),
      });

      // Call the flush callback
      this.flushCallback(worldId, blocks);

      logger.info('✅ SERVER: Flush callback completed');

      // Clear blocks for this world
      this.pendingUpdates.delete(worldId);
    } catch (error) {
      ExceptionHandler.handle(error, 'BlockUpdateBuffer.flushWorld', { worldId });
    }
  }

  /**
   * Flush all pending updates for all worlds
   */
  flush(): void {
    try {
      // Clear timer
      if (this.flushTimer !== null) {
        clearTimeout(this.flushTimer);
        this.flushTimer = null;
      }

      // Flush all worlds
      const worldIds = Array.from(this.pendingUpdates.keys());
      if (worldIds.length === 0) {
        logger.debug('No pending updates to flush');
        return;
      }

      logger.debug('Flushing all pending updates', {
        worldCount: worldIds.length,
        totalBlocks: Array.from(this.pendingUpdates.values()).reduce(
          (sum, blocks) => sum + blocks.length,
          0
        ),
      });

      for (const worldId of worldIds) {
        this.flushWorld(worldId);
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'BlockUpdateBuffer.flush');
    }
  }

  /**
   * Get count of pending updates
   */
  getPendingCount(): number {
    return Array.from(this.pendingUpdates.values()).reduce(
      (sum, blocks) => sum + blocks.length,
      0
    );
  }

  /**
   * Get count of pending updates for a specific world
   */
  getPendingCountForWorld(worldId: string): number {
    return this.pendingUpdates.get(worldId)?.length ?? 0;
  }

  /**
   * Dispose buffer and flush all pending updates
   */
  dispose(): void {
    try {
      logger.info('Disposing BlockUpdateBuffer');

      // Flush any remaining updates
      this.flush();

      // Clear timer
      if (this.flushTimer !== null) {
        clearTimeout(this.flushTimer);
        this.flushTimer = null;
      }

      // Clear all data
      this.pendingUpdates.clear();
    } catch (error) {
      ExceptionHandler.handle(error, 'BlockUpdateBuffer.dispose');
    }
  }
}
