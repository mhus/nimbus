/**
 * useAssets Composable
 * Manages asset list and operations
 */

import { ref } from 'vue';
import { assetService, type Asset } from '../services/AssetService';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('useAssets');

export function useAssets(worldId: string) {
  const assets = ref<Asset[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const searchQuery = ref('');
  const uploadProgress = ref(0);

  /**
   * Load all assets
   */
  const loadAssets = async () => {
    loading.value = true;
    error.value = null;

    try {
      assets.value = await assetService.getAssets(worldId);
      logger.info('Loaded assets', { count: assets.value.length, worldId });
    } catch (err) {
      error.value = 'Failed to load assets';
      logger.error('Failed to load assets', { worldId }, err as Error);
    } finally {
      loading.value = false;
    }
  };

  /**
   * Search assets
   */
  const searchAssets = async (query: string) => {
    searchQuery.value = query;
    loading.value = true;
    error.value = null;

    try {
      assets.value = await assetService.getAssets(worldId, query || undefined);
      logger.info('Searched assets', { query, count: assets.value.length, worldId });
    } catch (err) {
      error.value = 'Failed to search assets';
      logger.error('Failed to search assets', { worldId, query }, err as Error);
    } finally {
      loading.value = false;
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
    loadAssets,
    searchAssets,
    uploadAsset,
    updateAsset,
    deleteAsset,
    getAssetUrl,
    isImage,
    getIcon,
  };
}
