/**
 * BlockTypeService - Manages block type registry
 *
 * Loads block types from server and provides lookup functionality.
 * BlockTypes are cached after initial load.
 */

import { getLogger, ExceptionHandler, BlockType } from '@nimbus/shared';
import type { AppContext } from '../AppContext';

const logger = getLogger('BlockTypeService');

/**
 * BlockType response from server API
 */
interface BlockTypesResponse {
  blockTypes: BlockType[];
}

/**
 * BlockTypeService - Manages block type registry
 *
 * Features:
 * - Loads block types from server REST API
 * - Caches block types after initial load
 * - Provides fast lookup by ID
 * - Validates block type data
 */
export class BlockTypeService {
  private blockTypes: Map<number, BlockType> = new Map();
  private loaded: boolean = false;
  private loadingPromise?: Promise<void>;

  constructor(private appContext: AppContext) {
    logger.info('BlockTypeService initialized');
  }

  /**
   * Load block types from server
   *
   * This method is idempotent - multiple calls will reuse the same loading promise
   * and only load once.
   */
  async loadBlockTypes(): Promise<void> {
    // If already loaded, return immediately
    if (this.loaded) {
      logger.debug('Block types already loaded');
      return;
    }

    // If currently loading, return the existing promise
    if (this.loadingPromise) {
      logger.debug('Block types loading in progress, waiting...');
      return this.loadingPromise;
    }

    // Start loading
    this.loadingPromise = this.doLoadBlockTypes();

    try {
      await this.loadingPromise;
    } finally {
      this.loadingPromise = undefined;
    }
  }

  /**
   * Internal method that performs the actual loading
   */
  private async doLoadBlockTypes(): Promise<void> {
    try {
      const networkService = this.appContext.services.network;
      if (!networkService) {
        throw new Error('NetworkService not available');
      }

      const worldId = this.appContext.worldInfo?.worldId || 'main';
      const apiUrl = networkService.getApiUrl();
      const url = `${apiUrl}/api/worlds/${worldId}/blocktypes`;

      logger.info('Loading block types from server', { url });

      const response = await fetch(url);

      if (!response.ok) {
        throw new Error(`Failed to load block types: ${response.status} ${response.statusText}`);
      }

      const data: BlockTypesResponse = await response.json();

      if (!data.blockTypes || !Array.isArray(data.blockTypes)) {
        throw new Error('Invalid block types response: missing or invalid blockTypes array');
      }

      // Clear existing cache
      this.blockTypes.clear();

      // Validate and cache block types
      let validCount = 0;
      for (const blockType of data.blockTypes) {
        if (this.validateBlockType(blockType)) {
          this.blockTypes.set(blockType.id, blockType);
          validCount++;
        } else {
          logger.warn('Invalid block type received', { blockType });
        }
      }

      this.loaded = true;

      logger.info('Block types loaded successfully', {
        total: data.blockTypes.length,
        valid: validCount,
        invalid: data.blockTypes.length - validCount,
      });
    } catch (error) {
      this.loaded = false;
      throw ExceptionHandler.handleAndRethrow(
        error,
        'BlockTypeService.doLoadBlockTypes',
        { worldId: this.appContext.worldInfo?.worldId }
      );
    }
  }

  /**
   * Validate a block type
   */
  private validateBlockType(blockType: any): blockType is BlockType {
    if (typeof blockType !== 'object' || blockType === null) {
      return false;
    }

    if (typeof blockType.id !== 'number') {
      logger.warn('BlockType missing valid id', { blockType });
      return false;
    }

    if (!blockType.modifiers || typeof blockType.modifiers !== 'object') {
      logger.warn('BlockType missing modifiers', { blockType });
      return false;
    }

    return true;
  }

  /**
   * Get a block type by ID
   *
   * @param id Block type ID
   * @returns BlockType or undefined if not found
   */
  getBlockType(id: number): BlockType | undefined {
    if (!this.loaded) {
      logger.warn('Attempting to get block type before loading', { id });
      return undefined;
    }

    return this.blockTypes.get(id);
  }

  /**
   * Get all block types
   *
   * @returns Array of all loaded block types
   */
  getAllBlockTypes(): BlockType[] {
    if (!this.loaded) {
      logger.warn('Attempting to get all block types before loading');
      return [];
    }

    return Array.from(this.blockTypes.values());
  }

  /**
   * Check if block types are loaded
   */
  isLoaded(): boolean {
    return this.loaded;
  }

  /**
   * Get the number of loaded block types
   */
  getBlockTypeCount(): number {
    return this.blockTypes.size;
  }

  /**
   * Clear the cache and reset loaded state
   *
   * Useful for testing or when switching worlds
   */
  clear(): void {
    this.blockTypes.clear();
    this.loaded = false;
    this.loadingPromise = undefined;
    logger.info('Block type cache cleared');
  }
}
