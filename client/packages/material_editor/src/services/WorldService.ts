/**
 * World Service
 * Manages world-related API calls
 */

import { apiService } from './ApiService';

export interface WorldInfo {
  worldId: string;
  name: string;
  description: string;
  chunkSize: number;
  status: number;
}

export interface WorldListResponse {
  worldId: string;
  name: string;
  description: string;
  chunkSize: number;
  status: number;
}

export class WorldService {
  /**
   * Get all worlds
   */
  async getWorlds(): Promise<WorldListResponse[]> {
    return apiService.get<WorldListResponse[]>('/api/worlds');
  }

  /**
   * Get single world by ID
   */
  async getWorld(worldId: string): Promise<WorldInfo> {
    return apiService.get<WorldInfo>(`/api/worlds/${worldId}`);
  }
}

export const worldService = new WorldService();
