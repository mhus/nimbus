/**
 * BlockType Service
 * Manages block type CRUD operations
 */

import type { BlockType } from '@nimbus/shared';
import { apiService } from './ApiService';

export interface BlockTypeListResponse {
  blockTypes: BlockType[];
  count: number;
  limit: number;
  offset: number;
}

export interface BlockTypeCreateResponse {
  id: number;
}

export interface BlockTypePagingParams {
  query?: string;
  limit?: number;
  offset?: number;
}

export class BlockTypeService {
  /**
   * Get all block types or search with pagination
   */
  async getBlockTypes(
    worldId: string,
    params?: BlockTypePagingParams
  ): Promise<BlockTypeListResponse> {
    console.log('[BlockTypeService] getBlockTypes called', { worldId, params });
    console.log('[BlockTypeService] Request URL:', `/api/worlds/${worldId}/blocktypes`);

    const response = await apiService.get<BlockTypeListResponse>(
      `/api/worlds/${worldId}/blocktypes`,
      params
    );
    console.log('[BlockTypeService] API response:', response);
    console.log('[BlockTypeService] Returning:', {
      count: response.count,
      blockTypes: response.blockTypes?.length ?? 0,
      limit: response.limit,
      offset: response.offset
    });
    return response;
  }

  /**
   * Get single block type by ID
   */
  async getBlockType(worldId: string, id: number): Promise<BlockType> {
    return apiService.get<BlockType>(`/api/worlds/${worldId}/blocktypes/type/${id}`);
  }

  /**
   * Create new block type
   */
  async createBlockType(worldId: string, blockType: Partial<BlockType>): Promise<number> {
    const response = await apiService.post<BlockTypeCreateResponse>(
      `/api/worlds/${worldId}/blocktypes/type`,
      blockType
    );
    return response.id;
  }

  /**
   * Update existing block type
   */
  async updateBlockType(worldId: string, id: number, blockType: Partial<BlockType>): Promise<BlockType> {
    return apiService.put<BlockType>(
      `/api/worlds/${worldId}/blocktypes/type/${id}`,
      blockType
    );
  }

  /**
   * Delete block type
   */
  async deleteBlockType(worldId: string, id: number): Promise<void> {
    return apiService.delete<void>(`/api/worlds/${worldId}/blocktypes/type/${id}`);
  }

  /**
   * Get next available block type ID
   */
  async getNextAvailableId(worldId: string): Promise<number> {
    // The server automatically assigns IDs if not provided
    // This is a helper to get next ID from existing block types
    const response = await this.getBlockTypes(worldId);
    if (response.blockTypes.length === 0) {
      return 100; // Start at 100
    }
    const maxId = Math.max(...response.blockTypes.map(bt => Number(bt.id)));
    return maxId + 1;
  }
}

export const blockTypeService = new BlockTypeService();
