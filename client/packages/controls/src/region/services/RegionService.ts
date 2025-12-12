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

    return apiService.get<Region[]>('/api/region', params);
  }

  async getRegion(id: string): Promise<Region> {
    return apiService.get<Region>(`/api/region/${id}`);
  }

  async createRegion(request: RegionRequest): Promise<Region> {
    return apiService.post<Region>('/api/region', request);
  }

  async updateRegion(id: string, request: RegionRequest, enabled?: boolean): Promise<Region> {
    const params: any = {};
    if (enabled !== undefined) params.enabled = enabled;

    const url = enabled !== undefined ? `/api/region/${id}?enabled=${enabled}` : `/api/region/${id}`;
    return apiService.put<Region>(url, request);
  }

  async deleteRegion(id: string): Promise<void> {
    return apiService.delete<void>(`/api/region/${id}`);
  }

  async enableRegion(id: string): Promise<Region> {
    return apiService.post<Region>(`/api/region/${id}/enable`);
  }

  async disableRegion(id: string): Promise<Region> {
    return apiService.post<Region>(`/api/region/${id}/disable`);
  }

  async addMaintainer(id: string, userId: string): Promise<Region> {
    return apiService.post<Region>(`/api/region/${id}/maintainers`, { userId });
  }

  async removeMaintainer(id: string, userId: string): Promise<Region> {
    return apiService.delete<Region>(`/api/region/${id}/maintainers/${userId}`);
  }

  async listMaintainers(id: string): Promise<string[]> {
    return apiService.get<string[]>(`/api/region/${id}/maintainers`);
  }
}

export const regionService = new RegionService();
