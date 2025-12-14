import { apiService } from '@/services/ApiService';

// ===== Interfaces =====

export interface World {
  worldId: string;
  name: string;
  description?: string;
  regionId: string;
  enabled: boolean;
  publicFlag: boolean;
}

export interface Character {
  id: string;
  userId: string;
  name: string;
  display?: string;
  regionId: string;
}

export interface User {
  id: string;
  username: string;
  email?: string;
  enabled: boolean;
}

export type ActorType = 'PLAYER' | 'EDITOR' | 'SUPPORT';

export interface SessionLoginRequest {
  worldId: string;
  agent: false;
  userId: string;
  characterId: string;
  actor: ActorType;
}

export interface AgentLoginRequest {
  worldId: string;
  agent: true;
  userId: string;
}

export type LoginRequest = SessionLoginRequest | AgentLoginRequest;

export interface LoginResponse {
  accessToken: string;
  cookieUrls: string[];
  jumpUrl: string;
  sessionId?: string;
  playerId?: string;
}

// ===== Service Class =====

class DevLoginService {
  /**
   * Get list of available worlds with optional search filter
   */
  async getWorlds(searchQuery?: string, limit: number = 100): Promise<World[]> {
    const params: any = { limit };
    if (searchQuery) {
      params.search = searchQuery;
    }
    return apiService.get<World[]>('/api/aaa/devlogin', params);
  }

  /**
   * Get list of users with search filter
   */
  async getUsers(searchQuery?: string, limit: number = 100): Promise<User[]> {
    const params: any = { limit };
    if (searchQuery) {
      params.search = searchQuery;
    }
    return apiService.get<User[]>('/api/aaa/devlogin/users', params);
  }

  /**
   * Get characters for a user in a world
   */
  async getCharacters(userId: string, worldId: string): Promise<Character[]> {
    const params = { userId, worldId };
    return apiService.get<Character[]>('/api/aaa/devlogin/characters', params);
  }

  /**
   * Perform login (session or agent)
   */
  async login(request: LoginRequest): Promise<LoginResponse> {
    return apiService.post<LoginResponse>('/api/aaa/devlogin', request);
  }

  /**
   * Authorize with cookie URLs
   * Makes requests to each URL to set authentication cookies
   */
  async authorize(cookieUrls: string[], accessToken: string): Promise<void> {
    const authPromises = cookieUrls.map(url =>
      fetch(`${url}?token=${accessToken}`, {
        method: 'GET',
        credentials: 'include', // Important: allows setting cookies cross-origin
        mode: 'cors',
      })
    );

    await Promise.all(authPromises);
  }
}

export const devLoginService = new DevLoginService();
