/**
 * BlockTypeService - Manages block type registry
 *
 * Loads block types from server and provides lookup functionality.
 * BlockTypes are cached after initial load.
 */

import {
  getLogger,
  ExceptionHandler,
  BlockType,
  normalizeBlockTypeId,
  getBlockTypeGroup,
} from '@nimbus/shared';
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
 * BlockTypeService - Manages block type registry with group-based lazy loading
 *
 * Features:
 * - Lazy loads block types from server in groups
 * - Caches loaded groups
 * - Provides fast lookup by ID
 * - Validates block type data
 *
 * Group Strategy:
 * - BlockType ID format: "{group}:{name}" (e.g., "core:stone", "custom:tree")
 * - If no ":" in ID, default group "w" is used
 * - All IDs are normalized to lowercase
 * - Each group is loaded once and cached
 *
 * Examples:
 * - "core:stone" → loads group "core"
 * - "310" → loads group "w" (default)
 * - "CUSTOM:Tree" → normalized to "custom:tree", loads group "custom"
 */
export class BlockTypeService {
  private blockTypes: Map<string, BlockType> = new Map();
  private loadedGroups: Set<string> = new Set(); // Track which groups are loaded ('core', 'w', 'custom', ...)
  private loadingGroups: Map<string, Promise<void>> = new Map(); // Track groups currently being loaded

  constructor(private appContext: AppContext) {
    // Register static AIR BlockType (id 0)
    // This ensures AIR is always available, even before server block types are loaded
    this.registerAirBlockType();

    logger.debug('BlockTypeService initialized with group-based lazy loading');
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
   * Load a group of BlockTypes from the server
   * @param groupName The group name to load (e.g., 'core', 'w', 'custom')
   */
  private async loadGroup(groupName: string): Promise<void> {
    const normalizedGroup = groupName.toLowerCase();

    // Check if group is already loaded
    if (this.loadedGroups.has(normalizedGroup)) {
      logger.debug('Group already loaded', { groupName: normalizedGroup });
      return;
    }

    // Check if group is currently being loaded
    const existingPromise = this.loadingGroups.get(normalizedGroup);
    if (existingPromise) {
      logger.debug('Group loading in progress, waiting...', { groupName: normalizedGroup });
      return existingPromise;
    }

    // Start loading group
    const loadingPromise = this.doLoadGroup(normalizedGroup);
    this.loadingGroups.set(normalizedGroup, loadingPromise);

    try {
      await loadingPromise;
    } finally {
      this.loadingGroups.delete(normalizedGroup);
    }
  }

  /**
   * Internal method that performs the actual group loading
   */
  private async doLoadGroup(groupName: string): Promise<void> {
    try {
      const networkService = this.appContext.services.network;
      if (!networkService) {
        throw new Error('NetworkService not available');
      }

      const url = networkService.getBlockTypesChunkUrl(groupName);

      logger.debug('Loading BlockType group', { groupName, url });

      const response = await fetch(url);

      if (!response.ok) {
        throw new Error(`Failed to load group: ${response.status} ${response.statusText}`);
      }

      const blockTypes: BlockType[] = await response.json();

      if (!Array.isArray(blockTypes)) {
        throw new Error('Invalid group response: expected array of BlockTypes');
      }

      // Validate and cache block types from this group
      let validCount = 0;
      for (const blockType of blockTypes) {
        if (this.validateBlockType(blockType)) {
          // Ensure BlockType ID has correct group prefix
          const originalId = String(blockType.id);
          let correctedId: string;

          if (originalId.includes(':')) {
            // ID has group, check if it matches the loaded group
            const [existingGroup, name] = originalId.split(':', 2);
            if (existingGroup.toLowerCase() !== groupName.toLowerCase()) {
              // Wrong group in ID, replace with correct group
              correctedId = `${groupName}:${name}`.toLowerCase();
              logger.debug('Fixed BlockType ID group mismatch', {
                originalId,
                correctedId,
                loadedGroup: groupName,
              });
            } else {
              // Correct group, just normalize
              correctedId = originalId.toLowerCase();
            }
          } else {
            // No group in ID, add the loaded group
            correctedId = `${groupName}:${originalId}`.toLowerCase();
            logger.debug('Added group prefix to BlockType ID', {
              originalId,
              correctedId,
              loadedGroup: groupName,
            });
          }

          // Store BlockType with corrected ID
          blockType.id = correctedId;
          this.blockTypes.set(correctedId, blockType);
          validCount++;
        } else {
          logger.warn('Invalid block type received in group', { blockType, groupName });
        }
      }

      // Mark group as loaded
      this.loadedGroups.add(groupName);

      logger.debug('BlockType group loaded successfully', {
        groupName,
        received: blockTypes.length,
        valid: validCount,
        invalid: blockTypes.length - validCount,
      });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'BlockTypeService.doLoadGroup',
        { groupName }
      );
    }
  }

  /**
   * Ensure a BlockType is loaded (loads its group if needed)
   * @param blockTypeId The BlockType ID to ensure is loaded
   */
  private async ensureBlockTypeLoaded(blockTypeId: string | number): Promise<void> {
    const groupName = getBlockTypeGroup(blockTypeId);

    // If group not loaded, load it
    if (!this.loadedGroups.has(groupName)) {
      await this.loadGroup(groupName);
    }
  }

  /**
   * Ensure a BlockType group is loaded (public version)
   * @param groupName The group name to ensure is loaded
   */
  async ensureGroupLoaded(groupName: string): Promise<void> {
    const normalizedGroup = groupName.toLowerCase();

    // If group not loaded, load it
    if (!this.loadedGroups.has(normalizedGroup)) {
      await this.loadGroup(normalizedGroup);
    }
  }

  /**
   * Clear the BlockType cache
   *
   * Clears all cached BlockTypes and loaded group flags.
   * Next BlockType access will reload from server.
   */
  clearCache(): void {
    const beforeCount = this.blockTypes.size;
    const beforeGroups = this.loadedGroups.size;

    this.blockTypes.clear();
    this.loadedGroups.clear();

    // Re-register AIR BlockType (id 0)
    this.registerAirBlockType();

    logger.debug('BlockType cache cleared', {
      clearedBlockTypes: beforeCount - 1, // -1 for AIR
      clearedGroups: beforeGroups,
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
   * Normalize BlockType ID and ensure it has a group prefix
   * @param id Block type ID (string or legacy number)
   * @returns Normalized ID with group prefix (e.g., "w:310")
   */
  private normalizeIdWithGroup(id: string | number): string {
    const normalized = normalizeBlockTypeId(id);

    // Check if ID already has a group prefix
    if (normalized.includes(':')) {
      return normalized;
    }

    // No prefix, add default group 'w'
    return `w:${normalized}`;
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
    const normalizedId = this.normalizeIdWithGroup(id);
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
    const normalizedId = this.normalizeIdWithGroup(id);

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
   * Use preloadGroups() first if you need specific groups.
   *
   * @returns Array of loaded block types
   */
  getAllBlockTypes(): BlockType[] {
    return Array.from(this.blockTypes.values());
  }

  /**
   * Check if a specific group is loaded
   */
  isGroupLoaded(groupName: string): boolean {
    return this.loadedGroups.has(groupName.toLowerCase());
  }

  /**
   * Get statistics about loaded groups
   */
  getLoadedGroupsStats(): { loadedGroups: number; totalBlockTypes: number } {
    return {
      loadedGroups: this.loadedGroups.size,
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
   * Get all loaded groups
   * @returns Array of loaded group names
   */
  getLoadedGroups(): string[] {
    return Array.from(this.loadedGroups);
  }

  /**
   * Get all known BlockType IDs
   * @returns Array of all BlockType IDs in cache
   */
  getAllBlockTypeIds(): string[] {
    return Array.from(this.blockTypes.keys());
  }

  /**
   * Preload multiple groups at once
   * Useful for preloading common BlockType groups
   *
   * @param groupNames Array of group names to load (e.g., ['core', 'custom', 'w'])
   */
  async preloadGroups(groupNames: string[]): Promise<void> {
    const promises = groupNames.map(name => this.loadGroup(name));
    await Promise.all(promises);
  }

  /**
   * Preload BlockTypes for a list of IDs
   * Efficiently loads all required groups
   *
   * @param blockTypeIds Array of BlockType IDs to preload (string or legacy numbers)
   */
  async preloadBlockTypes(blockTypeIds: (string | number)[]): Promise<void> {
    // Extract unique groups needed
    const groupsNeeded = new Set(blockTypeIds.map(id => getBlockTypeGroup(id)));

    // Load all groups in parallel
    await this.preloadGroups(Array.from(groupsNeeded));
  }

  /**
   * Clear the cache and reset loaded state
   *
   * Useful for testing or when switching worlds
   */
  clear(): void {
    this.blockTypes.clear();
    this.loadedGroups.clear();
    this.loadingGroups.clear();

    // Re-register static AIR BlockType
    this.registerAirBlockType();

    logger.debug('Block type cache cleared');
  }
}
