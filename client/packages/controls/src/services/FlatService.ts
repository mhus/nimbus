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
  title: string | null;
  description: string | null;
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
  title: string | null;
  description: string | null;
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

  /**
   * Get export URL for downloading flat data
   */
  getExportUrl(id: string): string {
    return `${apiService.getBaseUrl()}/control/flats/${encodeURIComponent(id)}/export`;
  }

  /**
   * Import flat data from uploaded file
   */
  async importFlat(id: string, file: File): Promise<FlatDetail> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(
      `${apiService.getBaseUrl()}/control/flats/${encodeURIComponent(id)}/import`,
      {
        method: 'POST',
        body: formData,
      }
    );

    if (!response.ok) {
      throw new Error(`Import failed: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Update flat metadata (title and description)
   */
  async updateMetadata(id: string, title: string | null, description: string | null): Promise<FlatDetail> {
    return apiService.patch<FlatDetail>(
      `/control/flats/${encodeURIComponent(id)}/metadata`,
      { title, description }
    );
  }
}

export const flatService = new FlatService();
