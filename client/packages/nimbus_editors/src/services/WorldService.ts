/**
 * World Service
 * Manages world-related API calls
 */

import { apiService } from './ApiService';
import type { WorldInfo } from '@nimbus/shared';

/**
 * @deprecated Use WorldInfo from @nimbus/shared instead
 */
export type WorldListResponse = WorldInfo;

export class WorldService {
  /**
   * Get all worlds
   */
  async getWorlds(): Promise<WorldInfo[]> {
    return apiService.get<WorldInfo[]>('/api/worlds');
  }

  /**
   * Get single world by ID
   */
  async getWorld(worldId: string): Promise<WorldInfo> {
    return apiService.get<WorldInfo>(`/api/worlds/${worldId}`);
  }
}

export const worldService = new WorldService();
