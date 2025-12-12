import { apiService } from '@/services/ApiService';

export interface User {
  id: string;
  username: string;
  email: string;
  enabled: boolean;
  sectorRoles: string[];
  userSettings: Record<string, any>;
}

export interface UserRequest {
  username: string;
  email: string;
  sectorRolesRaw: string;
}

export interface Settings {
  [key: string]: any;
}

class UserService {
  async listUsers(): Promise<User[]> {
    return apiService.get<User[]>('/api/user');
  }

  async getUser(username: string): Promise<User> {
    return apiService.get<User>(`/api/user/${username}`);
  }

  async updateUser(username: string, request: UserRequest): Promise<User> {
    return apiService.put<User>(`/api/user/${username}`, request);
  }

  async deleteUser(username: string): Promise<void> {
    return apiService.delete<void>(`/api/user/${username}`);
  }

  async getUserSettings(username: string): Promise<Record<string, Settings>> {
    return apiService.get<Record<string, Settings>>(`/api/user/${username}/settings`);
  }

  async getSettingsForClientType(username: string, clientType: string): Promise<Settings> {
    return apiService.get<Settings>(`/api/user/${username}/settings/${clientType}`);
  }

  async updateSettingsForClientType(username: string, clientType: string, settings: Settings): Promise<any> {
    return apiService.put<any>(`/api/user/${username}/settings/${clientType}`, settings);
  }

  async deleteSettingsForClientType(username: string, clientType: string): Promise<void> {
    return apiService.delete<void>(`/api/user/${username}/settings/${clientType}`);
  }

  async updateAllSettings(username: string, settings: Record<string, Settings>): Promise<any> {
    return apiService.put<any>(`/api/user/${username}/settings`, settings);
  }

  async addSectorRole(username: string, role: string): Promise<User> {
    return apiService.post<User>(`/api/user/${username}/roles/${role}`);
  }

  async removeSectorRole(username: string, role: string): Promise<User> {
    return apiService.delete<User>(`/api/user/${username}/roles/${role}`);
  }
}

export const userService = new UserService();
