/**
 * BlockTypeService - Manages block type registry
 *
 * Loads block types from server and provides lookup functionality.
 * BlockTypes are cached after initial load.
 */

import { getLogger, ExceptionHandler, BlockType, normalizeBlockTypeId } from '@nimbus/shared';
import type { AppContext } from '../AppContext';

const logger = getLogger('BlockTypeService');

/**
 * BlockType response from server API (with paging)
 */
interface BlockTypesResponse {
  blockTypes: BlockType[];
  count: number;
  limit: number;
  offset: number;
}

/**
 * BlockTypeService - Manages block type registry with chunk-based lazy loading
 *
 * Features:
 * - Lazy loads block types from server in chunks of 100
 * - Caches loaded chunks
 * - Provides fast lookup by ID
 * - Validates block type data
 *
 * Chunk Strategy:
 * - BlockType ID 15 → loads chunk 0-99
 * - BlockType ID 234 → loads chunk 200-299
 * - Each chunk is loaded once and cached
 */
export class BlockTypeService {
  private blockTypes: Map<string, BlockType> = new Map();
  private loadedChunks: Set<number> = new Set(); // Track which chunks (0, 1, 2, ...) are loaded
  private loadingChunks: Map<number, Promise<void>> = new Map(); // Track chunks currently being loaded
  private readonly CHUNK_SIZE = 100;

  constructor(private appContext: AppContext) {
    // Register static AIR BlockType (id 0)
    // This ensures AIR is always available, even before server block types are loaded
    this.registerAirBlockType();

    logger.debug('BlockTypeService initialized with chunk-based lazy loading');
  }

  /**
   * Register static AIR BlockType (id 0)
   *
   * AIR is a special block type that represents empty space.
   * It must always be available for the SelectService and other systems.
   */
  private registerAirBlockType(): void {
    const airBlockType: BlockType = {
      id: '0',
      initialStatus: 0,
      modifiers: {
        0: {
          visibility: {
            shape: 0, // INVISIBLE shape
          },
          physics: {
            solid: false,
          },
        },
      },
    };

    this.blockTypes.set('0', airBlockType);
    logger.debug('Static AIR BlockType (id 0) registered');
  }

  /**
   * Calculate which chunk a BlockType ID belongs to
   * Example: ID 15 → chunk 0 (0-99), ID 234 → chunk 2 (200-299)
   */
  private getChunkIndex(blockTypeId: string | number): number {
    const id = typeof blockTypeId === 'string' ? parseInt(blockTypeId, 10) : blockTypeId;
    return Math.floor(id / this.CHUNK_SIZE);
  }

  /**
   * Get the range for a chunk
   * Example: chunk 0 → [0, 99], chunk 2 → [200, 299]
   */
  private getChunkRange(chunkIndex: number): { from: number; to: number } {
    return {
      from: chunkIndex * this.CHUNK_SIZE,
      to: (chunkIndex + 1) * this.CHUNK_SIZE - 1,
    };
  }

  /**
   * Load a chunk of BlockTypes from the server
   * @param chunkIndex The chunk index to load (0, 1, 2, ...)
   */
  private async loadChunk(chunkIndex: number): Promise<void> {
    // Check if chunk is already loaded
    if (this.loadedChunks.has(chunkIndex)) {
      logger.debug('Chunk already loaded', { chunkIndex });
      return;
    }

    // Check if chunk is currently being loaded
    const existingPromise = this.loadingChunks.get(chunkIndex);
    if (existingPromise) {
      logger.debug('Chunk loading in progress, waiting...', { chunkIndex });
      return existingPromise;
    }

    // Start loading chunk
    const loadingPromise = this.doLoadChunk(chunkIndex);
    this.loadingChunks.set(chunkIndex, loadingPromise);

    try {
      await loadingPromise;
    } finally {
      this.loadingChunks.delete(chunkIndex);
    }
  }

  /**
   * Internal method that performs the actual chunk loading
   */
  private async doLoadChunk(chunkIndex: number): Promise<void> {
    try {
      const networkService = this.appContext.services.network;
      if (!networkService) {
        throw new Error('NetworkService not available');
      }

      const range = this.getChunkRange(chunkIndex);
      const url = networkService.getBlockTypesRangeUrl(range.from, range.to);

      logger.debug('Loading BlockType chunk', { chunkIndex, range, url });

      const response = await fetch(url);

      if (!response.ok) {
        throw new Error(`Failed to load chunk: ${response.status} ${response.statusText}`);
      }

      const blockTypes: BlockType[] = await response.json();

      if (!Array.isArray(blockTypes)) {
        throw new Error('Invalid chunk response: expected array of BlockTypes');
      }

      // Validate and cache block types from this chunk
      let validCount = 0;
      for (const blockType of blockTypes) {
        if (this.validateBlockType(blockType)) {
          this.blockTypes.set(blockType.id, blockType);
          validCount++;
        } else {
          logger.warn('Invalid block type received in chunk', { blockType, chunkIndex });
        }
      }

      // Mark chunk as loaded
      this.loadedChunks.add(chunkIndex);

      logger.debug('BlockType chunk loaded successfully', {
        chunkIndex,
        range,
        received: blockTypes.length,
        valid: validCount,
        invalid: blockTypes.length - validCount,
      });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'BlockTypeService.doLoadChunk',
        { chunkIndex }
      );
    }
  }

  /**
   * Ensure a BlockType is loaded (loads its chunk if needed)
   * @param blockTypeId The BlockType ID to ensure is loaded
   */
  private async ensureBlockTypeLoaded(blockTypeId: string | number): Promise<void> {
    const chunkIndex = this.getChunkIndex(blockTypeId);

    // If chunk not loaded, load it
    if (!this.loadedChunks.has(chunkIndex)) {
      await this.loadChunk(chunkIndex);
    }
  }

  /**
   * Clear the BlockType cache
   *
   * Clears all cached BlockTypes and loaded chunk flags.
   * Next BlockType access will reload from server.
   */
  clearCache(): void {
    const beforeCount = this.blockTypes.size;
    const beforeChunks = this.loadedChunks.size;

    this.blockTypes.clear();
    this.loadedChunks.clear();

    // Re-register AIR BlockType (id 0)
    this.registerAirBlockType();

    logger.debug('BlockType cache cleared', {
      clearedBlockTypes: beforeCount - 1, // -1 for AIR
      clearedChunks: beforeChunks,
    });
  }

  /**
   * Validate a block type
   */
  private validateBlockType(blockType: any): blockType is BlockType {
    if (typeof blockType !== 'object' || blockType === null) {
      return false;
    }

    if (typeof blockType.id !== 'string' && typeof blockType.id !== 'number') {
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
   * Get a block type by ID (synchronous - returns cached value)
   *
   * Use this when you need immediate access to a BlockType.
   * Returns undefined if the chunk containing this BlockType is not loaded yet.
   *
   * @param id Block type ID (string or legacy number)
   * @returns BlockType or undefined if not loaded
   */
  getBlockType(id: string | number): BlockType | undefined {
    const normalizedId = normalizeBlockTypeId(id);
    return this.blockTypes.get(normalizedId);
  }

  /**
   * Get a block type by ID (asynchronous - loads chunk if needed)
   *
   * Use this when you can await and want to ensure the BlockType is available.
   * This will automatically load the chunk containing the BlockType if needed.
   *
   * @param id Block type ID (string or legacy number)
   * @returns BlockType or undefined if not found on server
   */
  async getBlockTypeAsync(id: string | number): Promise<BlockType | undefined> {
    const normalizedId = normalizeBlockTypeId(id);
    
    // Check if already in cache
    const cached = this.blockTypes.get(normalizedId);
    if (cached) {
      return cached;
    }

    // Load the chunk containing this BlockType
    await this.ensureBlockTypeLoaded(normalizedId);

    // Return from cache (may still be undefined if server doesn't have it)
    return this.blockTypes.get(normalizedId);
  }

  /**
   * Get all currently loaded block types
   *
   * Note: This only returns BlockTypes that have been loaded so far.
   * Use loadAllChunks() first if you need all BlockTypes.
   *
   * @returns Array of loaded block types
   */
  getAllBlockTypes(): BlockType[] {
    return Array.from(this.blockTypes.values());
  }

  /**
   * Check if a specific chunk is loaded
   */
  isChunkLoaded(chunkIndex: number): boolean {
    return this.loadedChunks.has(chunkIndex);
  }

  /**
   * Get statistics about loaded chunks
   */
  getLoadedChunksStats(): { loadedChunks: number; totalBlockTypes: number } {
    return {
      loadedChunks: this.loadedChunks.size,
      totalBlockTypes: this.blockTypes.size,
    };
  }

  /**
   * Get the number of loaded block types
   */
  getBlockTypeCount(): number {
    return this.blockTypes.size;
  }

  /**
   * Preload multiple chunks at once
   * Useful for preloading common BlockType ranges
   *
   * @param chunkIndices Array of chunk indices to load
   */
  async preloadChunks(chunkIndices: number[]): Promise<void> {
    const promises = chunkIndices.map(index => this.loadChunk(index));
    await Promise.all(promises);
  }

  /**
   * Preload BlockTypes for a list of IDs
   * Efficiently loads all required chunks
   *
   * @param blockTypeIds Array of BlockType IDs to preload (string or legacy numbers)
   */
  async preloadBlockTypes(blockTypeIds: (string | number)[]): Promise<void> {
    // Normalize all IDs first
    const normalizedIds = blockTypeIds.map(id => normalizeBlockTypeId(id));
    
    // Calculate unique chunks needed
    const chunksNeeded = new Set(normalizedIds.map(id => this.getChunkIndex(id)));

    // Load all chunks in parallel
    await this.preloadChunks(Array.from(chunksNeeded));
  }

  /**
   * Clear the cache and reset loaded state
   *
   * Useful for testing or when switching worlds
   */
  clear(): void {
    this.blockTypes.clear();
    this.loadedChunks.clear();
    this.loadingChunks.clear();

    // Re-register static AIR BlockType
    this.registerAirBlockType();

    logger.debug('Block type cache cleared');
  }
}
