/**
 * ItemService - Loads and caches items from server
 *
 * Provides access to item data including textures for UI display.
 * Items are loaded from the server REST API and cached locally.
 *
 * Also handles item activation (pose, wait, duration) when shortcuts are triggered.
 */

import type { Block, ItemData } from '@nimbus/shared';
import { getLogger, ExceptionHandler, ENTITY_POSES } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import { StackName } from './ModifierService';

const logger = getLogger('ItemService');

/**
 * ItemService manages item data from server
 */
export class ItemService {
  /** Cache of loaded items: itemId -> Block */
  private itemCache: Map<string, Block> = new Map();

  /** Cache of loaded ItemData: itemId -> ItemData */
  private itemDataCache: Map<string, ItemData> = new Map();

  /** Pending requests to avoid duplicate fetches */
  private pendingRequests: Map<string, Promise<Block | null>> = new Map();

  /** Active pose timers (for duration cleanup) */
  private poseTimers: Map<string, number> = new Map();

  constructor(private appContext: AppContext) {
    logger.info('ItemService initialized');
  }

  /**
   * Initialize event subscriptions
   * Called after PlayerService is available
   */
  initializeEventSubscriptions(): void {
    const playerService = this.appContext.services.player;
    if (!playerService) {
      logger.warn('PlayerService not available for event subscriptions');
      return;
    }

    // Subscribe to shortcut activation events
    playerService.on('shortcut:activated', (data: { shortcutKey: string; itemId?: string }) => {
      this.handleShortcutActivation(data.shortcutKey, data.itemId);
    });

    logger.debug('ItemService event subscriptions initialized');
  }

  /**
   * Get item by ID from cache or server
   *
   * @param itemId Item ID (from metadata.id)
   * @returns Block or null if not found
   */
  async getItem(itemId: string): Promise<Block | null> {
    // Check cache first
    if (this.itemCache.has(itemId)) {
      return this.itemCache.get(itemId)!;
    }

    // Check if already fetching
    const pending = this.pendingRequests.get(itemId);
    if (pending) {
      return pending;
    }

    // Fetch from server
    const promise = this.fetchItemFromServer(itemId);
    this.pendingRequests.set(itemId, promise);

    try {
      const item = await promise;
      if (item) {
        this.itemCache.set(itemId, item);
      }
      return item;
    } finally {
      this.pendingRequests.delete(itemId);
    }
  }

  /**
   * Fetch item from server REST API
   *
   * @param itemId Item ID
   * @returns Block or null if not found
   */
  private async fetchItemFromServer(itemId: string): Promise<Block | null> {
    try {
      const worldId = this.appContext.worldInfo?.worldId;
      if (!worldId) {
        logger.warn('Cannot fetch item: no worldId', { itemId });
        return null;
      }

      const networkService = this.appContext.services.network;
      if (!networkService) {
        logger.warn('NetworkService not available', { itemId });
        return null;
      }

      const url = networkService.getItemUrl(itemId);

      logger.debug('Fetching item from server', { itemId, url });

      const response = await fetch(url);

      if (!response.ok) {
        if (response.status === 404) {
          logger.debug('Item not found', { itemId });
          return null;
        }
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const block: Block = await response.json();
      logger.debug('Item loaded from server', { itemId });

      return block;
    } catch (error) {
      ExceptionHandler.handle(error, 'ItemService.fetchItemFromServer', { itemId });
      return null;
    }
  }

  /**
   * Get texture URL for item
   *
   * Returns the asset URL for the item's texture that can be used in <img> tags.
   *
   * @param item Item block
   * @returns Texture URL or null if no texture
   */
  getTextureUrl(item: Block): string | null {
    try {
      // Get texture from modifiers
      const modifier = item.modifiers?.['0'];
      if (!modifier?.visibility?.textures) {
        return null;
      }

      const texture = modifier.visibility.textures['0'];
      if (!texture) {
        return null;
      }

      // Get texture path
      const texturePath = typeof texture === 'string' ? texture : texture.path;
      if (!texturePath) {
        return null;
      }

      // Build asset URL via NetworkService
      const networkService = this.appContext.services.network;
      if (!networkService) {
        logger.warn('NetworkService not available for texture URL');
        return null;
      }

      return networkService.getAssetUrl(`assets/${texturePath}`);
    } catch (error) {
      ExceptionHandler.handle(error, 'ItemService.getTextureUrl', {
        itemId: item.metadata?.id,
      });
      return null;
    }
  }

  /**
   * Preload multiple items
   *
   * Useful for preloading items that will be displayed in UI.
   *
   * @param itemIds Array of item IDs to preload
   */
  async preloadItems(itemIds: string[]): Promise<void> {
    const promises = itemIds.map((id) => this.getItem(id));
    await Promise.all(promises);
    logger.debug('Items preloaded', { count: itemIds.length });
  }

  /**
   * Clear item cache
   */
  clearCache(): void {
    this.itemCache.clear();
    logger.debug('Item cache cleared');
  }

  /**
   * Get cache size
   *
   * @returns Number of cached items
   */
  getCacheSize(): number {
    return this.itemCache.size;
  }

  /**
   * Get ItemData by ID (includes pose, wait, duration, description, parameters)
   *
   * @param itemId Item ID
   * @returns ItemData or null if not found
   */
  async getItemData(itemId: string): Promise<ItemData | null> {
    try {
      // Check cache first
      if (this.itemDataCache.has(itemId)) {
        return this.itemDataCache.get(itemId)!;
      }

      // Fetch ItemData from server (full data endpoint)
      const worldId = this.appContext.worldInfo?.worldId;
      if (!worldId) {
        logger.warn('Cannot fetch ItemData: no worldId', { itemId });
        return null;
      }

      const networkService = this.appContext.services.network;
      if (!networkService) {
        logger.warn('NetworkService not available', { itemId });
        return null;
      }

      const url = networkService.getItemDataUrl(itemId);

      const response = await fetch(url);
      if (!response.ok) {
        if (response.status === 404) {
          logger.debug('ItemData not found', { itemId });
          return null;
        }
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const itemData: ItemData = await response.json();
      this.itemDataCache.set(itemId, itemData);

      // Also cache the block
      this.itemCache.set(itemId, itemData.block);

      return itemData;
    } catch (error) {
      ExceptionHandler.handle(error, 'ItemService.getItemData', { itemId });
      return null;
    }
  }

  /**
   * Handle shortcut activation
   *
   * Loads item data and activates pose with wait/duration timing.
   *
   * @param shortcutKey Shortcut key that was activated
   * @param itemId Item ID from shortcut
   */
  private async handleShortcutActivation(shortcutKey: string, itemId?: string): Promise<void> {
    try {
      if (!itemId) {
        logger.debug('No itemId for shortcut activation', { shortcutKey });
        return;
      }

      // Load ItemData (includes pose, wait, duration, onUseEffect)
      const itemData = await this.getItemData(itemId);
      if (!itemData) {
        logger.warn('ItemData not found for shortcut', { shortcutKey, itemId });
        return;
      }

      const { pose, wait, duration, onUseEffect, exclusive } = itemData;

      // Execute scrawl script if defined
      let executorId: string | undefined;
      if (onUseEffect) {
        const scrawlService = this.appContext.services.scrawl;
        if (scrawlService) {
          try {
            logger.debug('Executing onUseEffect script', { itemId, shortcutKey });
            executorId = await scrawlService.executeAction(onUseEffect, {
              // Provide context for the script
              itemId,
              shortcutKey,
            });
          } catch (error) {
            ExceptionHandler.handle(error, 'ItemService.handleShortcutActivation.onUseEffect', {
              shortcutKey,
              itemId,
            });
            logger.warn('Failed to execute onUseEffect script', {
              itemId,
              error: (error as Error).message,
            });
          }
        } else {
          logger.debug('ScrawlService not available, skipping onUseEffect', { itemId });
        }
      }

      // Register in ShortcutService if executorId exists
      if (executorId) {
        const shortcutService = this.appContext.services.shortcut;
        const playerService = this.appContext.services.player;

        if (shortcutService && playerService) {
          const shortcutNr = this.extractShortcutNumber(shortcutKey);
          const isExclusive = exclusive ?? false;

          shortcutService.startShortcut(shortcutNr, shortcutKey, executorId, isExclusive, itemId);

          // Emit started event
          playerService.emitShortcutStarted({
            shortcutNr,
            shortcutKey,
            executorId,
            itemId,
            exclusive: isExclusive,
          });

          logger.debug('Shortcut registered in ShortcutService', {
            shortcutNr,
            executorId,
            exclusive: isExclusive,
          });
        }
      }

      // Handle pose animation if defined
      if (pose) {
        // Get pose ID from ENTITY_POSES
        const poseId = (ENTITY_POSES as any)[pose.toUpperCase()];
        if (poseId === undefined) {
          logger.warn('Unknown pose', { pose, itemId });
          return;
        }

        // Apply wait delay if specified
        const waitMs = wait || 0;
        if (waitMs > 0) {
          logger.debug('Waiting before pose activation', { waitMs, pose, itemId });
          await new Promise(resolve => setTimeout(resolve, waitMs));
        }

        // Activate pose with priority 10 (overrides idle=100, but not calculated movement poses)
        this.activatePose(poseId, duration || 1000, itemId);

        logger.debug('Shortcut pose activated', { shortcutKey, itemId, pose, poseId, duration });
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'ItemService.handleShortcutActivation', { shortcutKey, itemId });
    }
  }

  /**
   * Activate a pose for a specific duration
   *
   * @param poseId Pose ID from ENTITY_POSES
   * @param durationMs Duration in milliseconds
   * @param modifierId Unique ID for this modifier
   */
  private activatePose(poseId: number, durationMs: number, modifierId: string): void {
    const poseStack = this.appContext.services.modifier?.getModifierStack<number>(StackName.PLAYER_POSE);
    if (!poseStack) {
      logger.warn('PLAYER_POSE stack not available');
      return;
    }

    // Clear existing timer for this modifier if any
    const existingTimer = this.poseTimers.get(modifierId);
    if (existingTimer) {
      clearTimeout(existingTimer);
      this.poseTimers.delete(modifierId);
    }

    // Add modifier with priority 10 (higher than default, overrides idle)
    const modifier = poseStack.addModifier(poseId, 10);

    // Set timer to remove modifier after duration
    const timer = window.setTimeout(() => {
      modifier.close();
      this.poseTimers.delete(modifierId);
      logger.debug('Pose duration expired', { modifierId, durationMs });
    }, durationMs);

    this.poseTimers.set(modifierId, timer);
  }

  /**
   * Extracts the shortcut number from a shortcut key.
   *
   * @param shortcutKey Shortcut key (e.g., 'key1', 'key10', 'click2', 'slot5')
   * @returns Shortcut number
   */
  private extractShortcutNumber(shortcutKey: string): number {
    if (shortcutKey.startsWith('key')) {
      // key0-key9 -> 0-9, key10 -> 10
      return parseInt(shortcutKey.replace('key', ''), 10);
    } else if (shortcutKey.startsWith('click')) {
      // click0-9 -> 0-9
      return parseInt(shortcutKey.replace('click', ''), 10);
    } else if (shortcutKey.startsWith('slot')) {
      // slot0-N -> 0-N
      return parseInt(shortcutKey.replace('slot', ''), 10);
    }

    // Fallback: try to extract any number from the key
    const match = shortcutKey.match(/\d+/);
    return match ? parseInt(match[0], 10) : 0;
  }

  /**
   * Dispose service
   */
  dispose(): void {
    // Clear all pose timers
    this.poseTimers.forEach((timer) => clearTimeout(timer));
    this.poseTimers.clear();

    this.itemCache.clear();
    this.itemDataCache.clear();
    this.pendingRequests.clear();
    logger.info('ItemService disposed');
  }
}
