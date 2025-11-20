/**
 * ItemUpdateBuffer - Batches item updates before broadcasting to clients
 *
 * Collects item changes and broadcasts them in batches to reduce network traffic.
 * Uses two flush strategies:
 * 1. Time-based: Flush after FLUSH_INTERVAL (default 1 second)
 * 2. Size-based: Flush immediately when MAX_BATCH_SIZE is reached
 */

import {getLogger, type Item, ExceptionHandler, ItemBlockRef} from '@nimbus/shared';

const logger = getLogger('ItemUpdateBuffer');

/**
 * Configuration for ItemUpdateBuffer
 */
export interface ItemUpdateBufferConfig {
  /** Time to wait before flushing (milliseconds) */
  flushInterval: number;

  /** Maximum items to batch before immediate flush */
  maxBatchSize: number;
}

/**
 * Default configuration
 */
const DEFAULT_CONFIG: ItemUpdateBufferConfig = {
  flushInterval: 1000, // 1 second
  maxBatchSize: 50, // 50 items (lower than blocks since items are less frequent)
};

/**
 * ItemUpdateBuffer - Batches item updates for efficient broadcasting
 */
export class ItemUpdateBuffer {
  private pendingUpdates: Map<string, ItemBlockRef[]>; // worldId -> items[]
  private flushTimer: NodeJS.Timeout | null = null;
  private config: ItemUpdateBufferConfig;
  private flushCallback: (worldId: string, items: ItemBlockRef[]) => void;

  constructor(
    flushCallback: (worldId: string, items: ItemBlockRef[]) => void,
    config?: Partial<ItemUpdateBufferConfig>
  ) {
    this.pendingUpdates = new Map();
    this.config = { ...DEFAULT_CONFIG, ...config };
    this.flushCallback = flushCallback;

    logger.info('ItemUpdateBuffer initialized', {
      flushInterval: this.config.flushInterval,
      maxBatchSize: this.config.maxBatchSize,
    });
  }

  /**
   * Add an item update to the buffer
   *
   * @param worldId World ID
   * @param item Item to update
   */
  addUpdate(worldId: string, item: ItemBlockRef): void {
    try {
      // Get or create items array for this world
      let items = this.pendingUpdates.get(worldId);
      if (!items) {
        items = [];
        this.pendingUpdates.set(worldId, items);
      }

      // Add item to buffer
      items.push(item);

      // logger.info('🔵 SERVER: Item update added to buffer', {
      //   worldId,
      //   position: item.position,
      //   itemId: item.id,
      //   bufferedCount: items.length,
      //   maxBatchSize: this.config.maxBatchSize,
      // });

      // Check if we should flush immediately (size-based)
      if (items.length >= this.config.maxBatchSize) {
        // logger.info('🔵 SERVER: Max batch size reached, flushing immediately', {
        //   worldId,
        //   count: items.length,
        // });
        this.flushWorld(worldId);
      } else {
        // Schedule timer-based flush
        // logger.info('🔵 SERVER: Scheduling timer-based flush', {
        //   worldId,
        //   bufferedCount: items.length,
        //   flushInterval: this.config.flushInterval,
        // });
        this.scheduleFlush();
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'ItemUpdateBuffer.addUpdate', { worldId, item });
    }
  }

  /**
   * Schedule a timer-based flush
   */
  private scheduleFlush(): void {
    // Clear existing timer
    if (this.flushTimer) {
      return; // Timer already running
    }

    // Create new timer
    this.flushTimer = setTimeout(() => {
      logger.info('🔵 SERVER: Timer-based flush triggered');
      this.flushAll();
    }, this.config.flushInterval);

    logger.info('🔵 SERVER: Flush timer scheduled', {
      interval: this.config.flushInterval,
    });
  }

  /**
   * Flush all pending updates for a specific world
   *
   * @param worldId World ID to flush
   */
  private flushWorld(worldId: string): void {
    const items = this.pendingUpdates.get(worldId);
    if (!items || items.length === 0) {
      return;
    }

    // logger.info('🔵 SERVER: Flushing item updates for world', {
    //   worldId,
    //   count: items.length,
    //   items: items.map((i) => ({
    //     position: i.position,
    //     itemId: i.id,
    //   })),
    // });

    // Call flush callback
    this.flushCallback(worldId, items);

    // Clear pending updates for this world
    this.pendingUpdates.delete(worldId);
  }

  /**
   * Flush all pending updates for all worlds
   */
  private flushAll(): void {
    // Clear timer
    if (this.flushTimer) {
      clearTimeout(this.flushTimer);
      this.flushTimer = null;
    }

    // Flush each world
    for (const worldId of Array.from(this.pendingUpdates.keys())) {
      this.flushWorld(worldId);
    }

    logger.info('🔵 SERVER: All item updates flushed');
  }

  /**
   * Force immediate flush of all pending updates
   */
  forceFlush(): void {
    logger.info('🔵 SERVER: Force flush requested');
    this.flushAll();
  }

  /**
   * Get pending update count for a world
   *
   * @param worldId World ID
   * @returns Number of pending items
   */
  getPendingCount(worldId: string): number {
    return this.pendingUpdates.get(worldId)?.length ?? 0;
  }

  /**
   * Get total pending update count across all worlds
   *
   * @returns Total number of pending items
   */
  getTotalPendingCount(): number {
    let total = 0;
    for (const items of this.pendingUpdates.values()) {
      total += items.length;
    }
    return total;
  }

  /**
   * Shutdown the buffer (cleanup timers)
   */
  shutdown(): void {
    if (this.flushTimer) {
      clearTimeout(this.flushTimer);
      this.flushTimer = null;
    }
    logger.info('ItemUpdateBuffer shutdown');
  }
}
