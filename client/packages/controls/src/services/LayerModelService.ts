/**
 * Layer Model Service
 * Manages layer model CRUD operations
 */

import type { LayerModelDto, CreateLayerModelRequest, UpdateLayerModelRequest } from '@nimbus/shared';
import { apiService } from './ApiService';

export interface LayerModelListResponse {
  models: LayerModelDto[];
  count: number;
}

export interface LayerModelCreateResponse {
  id: string;
}

export class LayerModelService {
  /**
   * Get all models for a layer
   */
  async getModels(
    worldId: string,
    layerId: string
  ): Promise<LayerModelListResponse> {
    return apiService.get<LayerModelListResponse>(
      `/control/worlds/${worldId}/layers/${layerId}/models`
    );
  }

  /**
   * Get single model by ID
   */
  async getModel(worldId: string, layerId: string, id: string): Promise<LayerModelDto> {
    return apiService.get<LayerModelDto>(
      `/control/worlds/${worldId}/layers/${layerId}/models/${id}`
    );
  }

  /**
   * Create new model
   */
  async createModel(
    worldId: string,
    layerId: string,
    model: CreateLayerModelRequest
  ): Promise<string> {
    const response = await apiService.post<LayerModelCreateResponse>(
      `/control/worlds/${worldId}/layers/${layerId}/models`,
      model
    );
    return response.id;
  }

  /**
   * Update existing model
   */
  async updateModel(
    worldId: string,
    layerId: string,
    id: string,
    model: UpdateLayerModelRequest
  ): Promise<LayerModelDto> {
    return apiService.put<LayerModelDto>(
      `/control/worlds/${worldId}/layers/${layerId}/models/${id}`,
      model
    );
  }

  /**
   * Delete model
   */
  async deleteModel(worldId: string, layerId: string, id: string): Promise<void> {
    return apiService.delete<void>(
      `/control/worlds/${worldId}/layers/${layerId}/models/${id}`
    );
  }

  /**
   * Sync model to terrain
   * Manually triggers transfer of model data to terrain layer and marks chunks as dirty
   */
  async syncToTerrain(worldId: string, layerId: string, id: string): Promise<void> {
    return apiService.post<void>(
      `/control/worlds/${worldId}/layers/${layerId}/models/${id}/sync`,
      {}
    );
  }
}

export const layerModelService = new LayerModelService();
