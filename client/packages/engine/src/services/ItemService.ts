/**
 * ItemService - Loads and caches items from server
 *
 * Provides access to item data including textures for UI display.
 * Items are loaded from the server REST API and cached locally.
 */

import type { Block } from '@nimbus/shared';
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';

const logger = getLogger('ItemService');

/**
 * ItemService manages item data from server
 */
export class ItemService {
  /** Cache of loaded items: itemId -> Block */
  private itemCache: Map<string, Block> = new Map();

  /** Pending requests to avoid duplicate fetches */
  private pendingRequests: Map<string, Promise<Block | null>> = new Map();

  constructor(private appContext: AppContext) {
    logger.info('ItemService initialized');
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

      const apiUrl = this.appContext.config.apiUrl;
      const url = `${apiUrl}/worlds/${worldId}/item/${itemId}`;

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

      // Build asset URL
      const apiUrl = this.appContext.config.apiUrl;
      return `${apiUrl}/assets/${texturePath}`;
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
   * Dispose service
   */
  dispose(): void {
    this.itemCache.clear();
    this.pendingRequests.clear();
    logger.info('ItemService disposed');
  }
}
