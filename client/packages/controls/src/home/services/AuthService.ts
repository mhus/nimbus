import { apiService } from '@/services/ApiService';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('AuthService');

export interface AuthStatus {
  authenticated: boolean;
  roles: string[];
  accessUrls: string[];
  loginUrl: string;
}

/**
 * Service for checking authentication status and roles
 */
class AuthService {
  /**
   * Get authentication status and user roles
   */
  async getStatus(): Promise<AuthStatus> {
    try {
      const response = await apiService.post<{
        authenticated: boolean;
        roles?: string[];
        accessUrls?: string[];
        loginUrl?: string;
      }>('/control/aaa/status', {});

      return {
        authenticated: response.authenticated || false,
        roles: response.roles || [],
        accessUrls: response.accessUrls || [],
        loginUrl: response.loginUrl || 'dev-login.html',
      };
    } catch (error) {
      logger.error('Failed to get auth status', {}, error instanceof Error ? error : undefined);

      // If request fails, assume not authenticated
      return {
        authenticated: false,
        roles: [],
        accessUrls: [],
        loginUrl: 'dev-login.html',
      };
    }
  }
}

export const authService = new AuthService();
