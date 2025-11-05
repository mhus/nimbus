/**
 * Asset Service
 * Manages asset CRUD operations (textures, models, etc.)
 */

import { apiService } from './ApiService';

export interface Asset {
  path: string;
  size: number;
  mimeType: string;
  lastModified: string;
  extension: string;
  category: string;
}

export interface AssetListResponse {
  assets: Asset[];
  count: number;
  limit: number;
  offset: number;
}

export interface AssetPagingParams {
  query?: string;
  limit?: number;
  offset?: number;
}

export class AssetService {
  /**
   * Get all assets or search with pagination
   */
  async getAssets(worldId: string, params?: AssetPagingParams): Promise<AssetListResponse> {
    const response = await apiService.get<AssetListResponse>(
      `/api/worlds/${worldId}/assets`,
      params
    );
    return response;
  }

  /**
   * Upload new asset
   */
  async uploadAsset(worldId: string, assetPath: string, file: File): Promise<Asset> {
    const arrayBuffer = await file.arrayBuffer();
    return apiService.uploadBinary<Asset>(
      `/api/worlds/${worldId}/assets/${assetPath}`,
      arrayBuffer,
      file.type
    );
  }

  /**
   * Update existing asset
   */
  async updateAsset(worldId: string, assetPath: string, file: File): Promise<Asset> {
    const arrayBuffer = await file.arrayBuffer();
    return apiService.updateBinary<Asset>(
      `/api/worlds/${worldId}/assets/${assetPath}`,
      arrayBuffer,
      file.type
    );
  }

  /**
   * Delete asset
   */
  async deleteAsset(worldId: string, assetPath: string): Promise<void> {
    return apiService.delete<void>(`/api/worlds/${worldId}/assets/${assetPath}`);
  }

  /**
   * Get asset download URL
   */
  getAssetUrl(worldId: string, assetPath: string): string {
    return `${apiService.getBaseUrl()}/api/worlds/${worldId}/assets/${assetPath}`;
  }

  /**
   * Check if asset is an image (for preview)
   */
  isImageAsset(asset: Asset): boolean {
    const imageExtensions = ['.png', '.jpg', '.jpeg', '.gif', '.webp', '.svg'];
    return imageExtensions.includes(asset.extension.toLowerCase());
  }

  /**
   * Get icon for asset type
   */
  getAssetIcon(asset: Asset): string {
    const ext = asset.extension.toLowerCase();

    // Image files
    if (this.isImageAsset(asset)) {
      return '🖼️';
    }

    // 3D models
    if (['.obj', '.mtl', '.fbx', '.gltf', '.glb'].includes(ext)) {
      return '🎨';
    }

    // Audio files
    if (['.mp3', '.wav', '.ogg'].includes(ext)) {
      return '🔊';
    }

    // JSON files
    if (ext === '.json') {
      return '📄';
    }

    // Default
    return '📦';
  }
}

export const assetService = new AssetService();
