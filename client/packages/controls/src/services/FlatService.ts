/**
 * Flat Service
 * Manages flat terrain data viewing and operations
 */

import { apiService } from './ApiService';

export interface FlatListItem {
  id: string;
  worldId: string;
  layerDataId: string;
  flatId: string;
  sizeX: number;
  sizeZ: number;
  mountX: number;
  mountZ: number;
  oceanLevel: number;
  createdAt: string;
  updatedAt: string;
}

export interface FlatDetail {
  id: string;
  worldId: string;
  layerDataId: string;
  flatId: string;
  sizeX: number;
  sizeZ: number;
  mountX: number;
  mountZ: number;
  oceanLevel: number;
  oceanBlockId: string;
  unknownProtected: boolean;
  levels: number[];
  columns: number[];
  createdAt: string;
  updatedAt: string;
}

export class FlatService {
  /**
   * Get all flats for a world
   */
  async getFlats(worldId: string): Promise<FlatListItem[]> {
    return apiService.get<FlatListItem[]>(
      '/control/flats',
      { worldId }
    );
  }

  /**
   * Get single flat detail
   */
  async getFlat(id: string): Promise<FlatDetail> {
    return apiService.get<FlatDetail>(
      `/control/flats/${encodeURIComponent(id)}`
    );
  }

  /**
   * Delete flat by ID
   */
  async deleteFlat(id: string): Promise<void> {
    return apiService.delete<void>(
      `/control/flats/${encodeURIComponent(id)}`
    );
  }
}

export const flatService = new FlatService();
