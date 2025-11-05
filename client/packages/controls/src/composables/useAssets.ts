/**
 * useAssets Composable
 * Manages asset list and operations
 */

import { ref, computed } from 'vue';
import { assetService, type Asset } from '../services/AssetService';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('useAssets');

export function useAssets(worldId: string) {
  const assets = ref<Asset[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const searchQuery = ref('');
  const uploadProgress = ref(0);

  // Paging state
  const totalCount = ref(0);
  const currentPage = ref(1);
  const pageSize = ref(50);

  // Computed
  const totalPages = computed(() => Math.ceil(totalCount.value / pageSize.value));
  const hasNextPage = computed(() => currentPage.value < totalPages.value);
  const hasPreviousPage = computed(() => currentPage.value > 1);

  /**
   * Load assets with pagination
   */
  const loadAssets = async (page: number = 1) => {
    loading.value = true;
    error.value = null;
    currentPage.value = page;

    try {
      const offset = (page - 1) * pageSize.value;
      const response = await assetService.getAssets(worldId, {
        query: searchQuery.value || undefined,
        limit: pageSize.value,
        offset,
      });

      assets.value = response.assets;
      totalCount.value = response.count;

      logger.info('Loaded assets', {
        count: assets.value.length,
        totalCount: totalCount.value,
        page,
        worldId,
      });
    } catch (err) {
      error.value = 'Failed to load assets';
      logger.error('Failed to load assets', { worldId, page }, err as Error);
    } finally {
      loading.value = false;
    }
  };

  /**
   * Search assets
   */
  const searchAssets = async (query: string) => {
    searchQuery.value = query;
    currentPage.value = 1; // Reset to first page on search
    await loadAssets(1);
  };

  /**
   * Go to next page
   */
  const nextPage = async () => {
    if (hasNextPage.value) {
      await loadAssets(currentPage.value + 1);
    }
  };

  /**
   * Go to previous page
   */
  const previousPage = async () => {
    if (hasPreviousPage.value) {
      await loadAssets(currentPage.value - 1);
    }
  };

  /**
   * Go to specific page
   */
  const goToPage = async (page: number) => {
    if (page >= 1 && page <= totalPages.value) {
      await loadAssets(page);
    }
  };

  /**
   * Upload new asset
   */
  const uploadAsset = async (assetPath: string, file: File): Promise<boolean> => {
    uploadProgress.value = 0;
    error.value = null;

    try {
      // Simulate progress (axios upload progress could be added)
      uploadProgress.value = 50;

      await assetService.uploadAsset(worldId, assetPath, file);
      uploadProgress.value = 100;

      logger.info('Uploaded asset', { worldId, assetPath, size: file.size });
      await loadAssets(); // Refresh list
      return true;
    } catch (err) {
      error.value = 'Failed to upload asset';
      logger.error('Failed to upload asset', { worldId, assetPath }, err as Error);
      uploadProgress.value = 0;
      return false;
    }
  };

  /**
   * Update existing asset
   */
  const updateAsset = async (assetPath: string, file: File): Promise<boolean> => {
    try {
      await assetService.updateAsset(worldId, assetPath, file);
      logger.info('Updated asset', { worldId, assetPath, size: file.size });
      await loadAssets(); // Refresh list
      return true;
    } catch (err) {
      error.value = 'Failed to update asset';
      logger.error('Failed to update asset', { worldId, assetPath }, err as Error);
      return false;
    }
  };

  /**
   * Delete asset
   */
  const deleteAsset = async (assetPath: string): Promise<boolean> => {
    try {
      await assetService.deleteAsset(worldId, assetPath);
      logger.info('Deleted asset', { worldId, assetPath });
      await loadAssets(); // Refresh list
      return true;
    } catch (err) {
      error.value = 'Failed to delete asset';
      logger.error('Failed to delete asset', { worldId, assetPath }, err as Error);
      return false;
    }
  };

  /**
   * Get asset URL for display
   */
  const getAssetUrl = (assetPath: string): string => {
    return assetService.getAssetUrl(worldId, assetPath);
  };

  /**
   * Check if asset is an image
   */
  const isImage = (asset: Asset): boolean => {
    return assetService.isImageAsset(asset);
  };

  /**
   * Get icon for asset
   */
  const getIcon = (asset: Asset): string => {
    return assetService.getAssetIcon(asset);
  };

  return {
    assets,
    loading,
    error,
    searchQuery,
    uploadProgress,
    // Paging
    totalCount,
    currentPage,
    pageSize,
    totalPages,
    hasNextPage,
    hasPreviousPage,
    // Methods
    loadAssets,
    searchAssets,
    nextPage,
    previousPage,
    goToPage,
    uploadAsset,
    updateAsset,
    deleteAsset,
    getAssetUrl,
    isImage,
    getIcon,
  };
}
