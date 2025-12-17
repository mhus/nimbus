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

export type WorldFilter =
  | 'mainOnly'
  | 'mainAndBranches'
  | 'mainWorldsAndInstances'
  | 'allWithoutInstances'
  | 'regionCollections'
  | 'regionOnly';

export class WorldService {
  /**
   * Get all worlds with optional filter
   * @param filter Optional filter type for world selection
   */
  async getWorlds(filter?: WorldFilter): Promise<WorldInfo[]> {
    const params = filter ? `?filter=${filter}` : '';
    return apiService.get<WorldInfo[]>(`/control/worlds${params}`);
  }

  /**
   * Get single world by ID
   */
  async getWorld(worldId: string): Promise<WorldInfo> {
    return apiService.get<WorldInfo>(`/control/worlds/${worldId}`);
  }
}

export const worldService = new WorldService();
