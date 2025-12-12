import { apiService } from '@/services/ApiService';

export interface Region {
  id: string;
  name: string;
  enabled: boolean;
  maintainers: string[];
}

export interface RegionRequest {
  name: string;
  maintainers: string;
}

export interface MaintainerRequest {
  userId: string;
}

class RegionService {
  async listRegions(name?: string, enabled?: boolean): Promise<Region[]> {
    const params: any = {};
    if (name) params.name = name;
    if (enabled !== undefined) params.enabled = enabled;

    return apiService.get<Region[]>('/api/regions', params);
  }

  async getRegion(id: string): Promise<Region> {
    return apiService.get<Region>(`/api/regions/${id}`);
  }

  async createRegion(request: RegionRequest): Promise<Region> {
    return apiService.post<Region>('/api/regions', request);
  }

  async updateRegion(id: string, request: RegionRequest, enabled?: boolean): Promise<Region> {
    const params: any = {};
    if (enabled !== undefined) params.enabled = enabled;

    const url = enabled !== undefined ? `/api/regions/${id}?enabled=${enabled}` : `/api/regions/${id}`;
    return apiService.put<Region>(url, request);
  }

  async deleteRegion(id: string): Promise<void> {
    return apiService.delete<void>(`/api/regions/${id}`);
  }

  async enableRegion(id: string): Promise<Region> {
    return apiService.post<Region>(`/api/regions/${id}/enable`);
  }

  async disableRegion(id: string): Promise<Region> {
    return apiService.post<Region>(`/api/regions/${id}/disable`);
  }

  async addMaintainer(id: string, userId: string): Promise<Region> {
    return apiService.post<Region>(`/api/regions/${id}/maintainers`, { userId });
  }

  async removeMaintainer(id: string, userId: string): Promise<Region> {
    return apiService.delete<Region>(`/api/regions/${id}/maintainers/${userId}`);
  }

  async listMaintainers(id: string): Promise<string[]> {
    return apiService.get<string[]>(`/api/regions/${id}/maintainers`);
  }
}

export const regionService = new RegionService();
