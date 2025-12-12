import { apiService } from '@/services/ApiService';

export interface WorldInfo {
  [key: string]: any;
}

export interface World {
  id: string;
  worldId: string;
  regionId: string;
  name: string;
  description: string;
  publicData: WorldInfo;
  createdAt: string;
  updatedAt: string;
  enabled: boolean;
  parent: string;
  branch: string;
  groundLevel: number;
  waterLevel: number | null;
  groundBlockType: string;
  waterBlockType: string;
  owner: string[];
  editor: string[];
  player: string[];
  publicFlag: boolean;
}

export interface WorldRequest {
  worldId: string;
  name: string;
  description?: string;
  publicData?: WorldInfo;
  enabled?: boolean;
  parent?: string;
  branch?: string;
  groundLevel?: number;
  waterLevel?: number;
  groundBlockType?: string;
  waterBlockType?: string;
}

class WorldServiceFrontend {
  async listWorlds(regionId: string): Promise<World[]> {
    return apiService.get<World[]>(`/api/regions/${regionId}/worlds`);
  }

  async getWorld(regionId: string, worldId: string): Promise<World> {
    return apiService.get<World>(`/api/regions/${regionId}/worlds/${worldId}`);
  }

  async createWorld(regionId: string, request: WorldRequest): Promise<World> {
    return apiService.post<World>(`/api/regions/${regionId}/worlds`, request);
  }

  async updateWorld(regionId: string, worldId: string, request: WorldRequest): Promise<World> {
    return apiService.put<World>(`/api/regions/${regionId}/worlds/${worldId}`, request);
  }

  async deleteWorld(regionId: string, worldId: string): Promise<void> {
    return apiService.delete<void>(`/api/regions/${regionId}/worlds/${worldId}`);
  }
}

export const worldServiceFrontend = new WorldServiceFrontend();
