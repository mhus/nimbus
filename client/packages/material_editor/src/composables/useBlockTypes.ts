/**
 * useBlockTypes Composable
 * Manages block type list and operations
 */

import { ref } from 'vue';
import type { BlockType } from '@nimbus/shared';
import { blockTypeService } from '../services/BlockTypeService';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('useBlockTypes');

export function useBlockTypes(worldId: string) {
  const blockTypes = ref<BlockType[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const searchQuery = ref('');

  /**
   * Load all block types
   */
  const loadBlockTypes = async () => {
    loading.value = true;
    error.value = null;

    try {
      blockTypes.value = await blockTypeService.getBlockTypes(worldId);
      logger.info('Loaded block types', { count: blockTypes.value.length, worldId });
    } catch (err) {
      error.value = 'Failed to load block types';
      logger.error('Failed to load block types', { worldId }, err as Error);
    } finally {
      loading.value = false;
    }
  };

  /**
   * Search block types
   */
  const searchBlockTypes = async (query: string) => {
    searchQuery.value = query;
    loading.value = true;
    error.value = null;

    try {
      blockTypes.value = await blockTypeService.getBlockTypes(worldId, query || undefined);
      logger.info('Searched block types', { query, count: blockTypes.value.length, worldId });
    } catch (err) {
      error.value = 'Failed to search block types';
      logger.error('Failed to search block types', { worldId, query }, err as Error);
    } finally {
      loading.value = false;
    }
  };

  /**
   * Get single block type
   */
  const getBlockType = async (id: number): Promise<BlockType | null> => {
    try {
      return await blockTypeService.getBlockType(worldId, id);
    } catch (err) {
      error.value = 'Failed to load block type';
      logger.error('Failed to load block type', { worldId, id }, err as Error);
      return null;
    }
  };

  /**
   * Create block type
   */
  const createBlockType = async (blockType: Partial<BlockType>): Promise<number | null> => {
    try {
      const id = await blockTypeService.createBlockType(worldId, blockType);
      logger.info('Created block type', { worldId, id });
      await loadBlockTypes(); // Refresh list
      return id;
    } catch (err) {
      error.value = 'Failed to create block type';
      logger.error('Failed to create block type', { worldId }, err as Error);
      return null;
    }
  };

  /**
   * Update block type
   */
  const updateBlockType = async (id: number, blockType: Partial<BlockType>): Promise<boolean> => {
    try {
      await blockTypeService.updateBlockType(worldId, id, blockType);
      logger.info('Updated block type', { worldId, id });
      await loadBlockTypes(); // Refresh list
      return true;
    } catch (err) {
      error.value = 'Failed to update block type';
      logger.error('Failed to update block type', { worldId, id }, err as Error);
      return false;
    }
  };

  /**
   * Delete block type
   */
  const deleteBlockType = async (id: number): Promise<boolean> => {
    try {
      await blockTypeService.deleteBlockType(worldId, id);
      logger.info('Deleted block type', { worldId, id });
      await loadBlockTypes(); // Refresh list
      return true;
    } catch (err) {
      error.value = 'Failed to delete block type';
      logger.error('Failed to delete block type', { worldId, id }, err as Error);
      return false;
    }
  };

  /**
   * Get next available ID
   */
  const getNextAvailableId = async (): Promise<number> => {
    try {
      return await blockTypeService.getNextAvailableId(worldId);
    } catch (err) {
      logger.error('Failed to get next available ID', { worldId }, err as Error);
      return 100; // Fallback
    }
  };

  return {
    blockTypes,
    loading,
    error,
    searchQuery,
    loadBlockTypes,
    searchBlockTypes,
    getBlockType,
    createBlockType,
    updateBlockType,
    deleteBlockType,
    getNextAvailableId,
  };
}
