/**
 * ConfigService - Manages configuration loading and caching
 *
 * Loads EngineConfiguration from REST API and provides cached access.
 * Configuration includes: WorldInfo, PlayerInfo, PlayerBackpack, Settings
 */

import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type {
  EngineConfiguration,
  PlayerInfo,
  WorldInfo,
  PlayerBackpack,
  Settings,
} from '@nimbus/shared';
import type { AppContext } from '../AppContext';

const logger = getLogger('ConfigService');

export class ConfigService {
  private appContext: AppContext;
  private config: EngineConfiguration | null = null;
  private loading: boolean = false;

  constructor(appContext: AppContext) {
    this.appContext = appContext;
  }

  /**
   * Load complete configuration from REST API
   *
   * @param clientType - 'viewer' or 'editor'
   * @param forceReload - Force reload even if already cached
   * @param worldId - Optional world ID (defaults to config.worldId or 'main')
   */
  async loadConfig(
    clientType: 'viewer' | 'editor' = 'viewer',
    forceReload: boolean = false,
    worldId?: string
  ): Promise<EngineConfiguration> {
    // Return cached config if available and not forcing reload
    if (this.config && !forceReload) {
      logger.debug('Returning cached config');
      return this.config;
    }

    // Prevent concurrent loading
    if (this.loading) {
      logger.debug('Config already loading, waiting...');
      // Wait for current load to complete
      while (this.loading) {
        await new Promise(resolve => setTimeout(resolve, 100));
      }
      if (this.config) {
        return this.config;
      }
    }

    this.loading = true;

    try {
      // Get worldId from parameter, config, or default to 'main'
      const targetWorldId = worldId || this.appContext.config?.worldId || 'main';
      const apiUrl = this.appContext.config?.apiUrl || 'http://localhost:3000';
      const url = `${apiUrl}/api/worlds/${targetWorldId}/config?client=${clientType}`;

      logger.info('Loading configuration from REST API', { url, clientType });

      const response = await fetch(url);

      if (!response.ok) {
        throw new Error(`Failed to load config: ${response.statusText}`);
      }

      const oldWorldInfo = this.appContext.worldInfo;
      this.config = await response.json();

      // Update AppContext with loaded config
      if (this.config.worldInfo) {
        this.appContext.worldInfo = this.config.worldInfo;
      }
      if (this.config.playerInfo) {
        this.appContext.playerInfo = this.config.playerInfo;
      }

      logger.info('Configuration loaded successfully', {
        hasWorldInfo: !!this.config.worldInfo,
        hasPlayerInfo: !!this.config.playerInfo,
        hasBackpack: !!this.config.playerBackpack,
        hasSettings: !!this.config.settings,
      });

      // If reloading and status/season changed, recalculate modifiers
      if (forceReload && oldWorldInfo && this.config.worldInfo) {
        const statusChanged = oldWorldInfo.status !== this.config.worldInfo.status;
        const seasonStatusChanged = oldWorldInfo.seasonStatus !== this.config.worldInfo.seasonStatus;
        const seasonProgressChanged = oldWorldInfo.seasonProgress !== this.config.worldInfo.seasonProgress;

        if (statusChanged || seasonStatusChanged || seasonProgressChanged) {
          logger.info('WorldInfo status/season changed during reload, recalculating modifiers', {
            statusChanged,
            seasonStatusChanged,
            seasonProgressChanged,
          });

          const chunkService = this.appContext.services.chunk;
          if (chunkService) {
            const result = chunkService.recalculateAndRedrawAll();
            logger.info('Modifiers recalculated and chunks redrawn', result);
          }
        }
      }

      return this.config;
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'ConfigService.loadConfig',
        { clientType, forceReload }
      );
    } finally {
      this.loading = false;
    }
  }

  /**
   * Reload configuration (force refresh from server)
   */
  async reloadConfig(
    clientType: 'viewer' | 'editor' = 'viewer',
    worldId?: string
  ): Promise<EngineConfiguration> {
    logger.info('Reloading configuration');
    return this.loadConfig(clientType, true, worldId);
  }

  /**
   * Get cached configuration (returns null if not loaded yet)
   */
  getConfig(): EngineConfiguration | null {
    return this.config;
  }

  /**
   * Get WorldInfo from cached config
   */
  getWorldInfo(): WorldInfo | null {
    return this.config?.worldInfo || null;
  }

  /**
   * Get PlayerInfo from cached config
   */
  getPlayerInfo(): PlayerInfo | null {
    return this.config?.playerInfo || null;
  }

  /**
   * Get PlayerBackpack from cached config
   */
  getPlayerBackpack(): PlayerBackpack | null {
    return this.config?.playerBackpack || null;
  }

  /**
   * Get Settings from cached config
   */
  getSettings(): Settings | null {
    return this.config?.settings || null;
  }

  /**
   * Load specific config section
   */
  async loadWorldInfo(worldId?: string): Promise<WorldInfo> {
    const targetWorldId = worldId || this.appContext.config?.worldId || 'main';
    const apiUrl = this.appContext.config?.apiUrl || 'http://localhost:3000';
    const url = `${apiUrl}/api/worlds/${targetWorldId}/config/worldinfo`;

    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to load WorldInfo: ${response.statusText}`);
    }

    const oldWorldInfo = this.appContext.worldInfo;
    const newWorldInfo = await response.json();

    // Check if status or season changed
    const statusChanged = oldWorldInfo?.status !== newWorldInfo.status;
    const seasonStatusChanged = oldWorldInfo?.seasonStatus !== newWorldInfo.seasonStatus;
    const seasonProgressChanged = oldWorldInfo?.seasonProgress !== newWorldInfo.seasonProgress;

    if (this.config) {
      this.config.worldInfo = newWorldInfo;
    }
    this.appContext.worldInfo = newWorldInfo;

    // If status or season changed, recalculate all modifiers
    if (statusChanged || seasonStatusChanged || seasonProgressChanged) {
      logger.info('WorldInfo status/season changed, recalculating modifiers', {
        statusChanged,
        seasonStatusChanged,
        seasonProgressChanged,
        oldStatus: oldWorldInfo?.status,
        newStatus: newWorldInfo.status,
        oldSeasonStatus: oldWorldInfo?.seasonStatus,
        newSeasonStatus: newWorldInfo.seasonStatus,
        oldSeasonProgress: oldWorldInfo?.seasonProgress,
        newSeasonProgress: newWorldInfo.seasonProgress,
      });

      const chunkService = this.appContext.services.chunk;
      if (chunkService) {
        const result = chunkService.recalculateAndRedrawAll();
        logger.info('Modifiers recalculated and chunks redrawn', result);
      }
    }

    return newWorldInfo;
  }

  /**
   * Load specific config section
   */
  async loadPlayerInfo(worldId?: string): Promise<PlayerInfo> {
    const targetWorldId = worldId || this.appContext.config?.worldId || 'main';
    const apiUrl = this.appContext.config?.apiUrl || 'http://localhost:3000';
    const url = `${apiUrl}/api/worlds/${targetWorldId}/config/playerinfo`;

    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to load PlayerInfo: ${response.statusText}`);
    }

    const playerInfo = await response.json();
    if (this.config) {
      this.config.playerInfo = playerInfo;
    }
    this.appContext.playerInfo = playerInfo;

    return playerInfo;
  }

  /**
   * Load specific config section
   */
  async loadPlayerBackpack(worldId?: string): Promise<PlayerBackpack> {
    const targetWorldId = worldId || this.appContext.config?.worldId || 'main';
    const apiUrl = this.appContext.config?.apiUrl || 'http://localhost:3000';
    const url = `${apiUrl}/api/worlds/${targetWorldId}/config/playerbackpack`;

    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to load PlayerBackpack: ${response.statusText}`);
    }

    const backpack = await response.json();
    if (this.config) {
      this.config.playerBackpack = backpack;
    }

    return backpack;
  }

  /**
   * Load specific config section
   */
  async loadSettings(clientType: 'viewer' | 'editor' = 'viewer', worldId?: string): Promise<Settings> {
    const targetWorldId = worldId || this.appContext.config?.worldId || 'main';
    const apiUrl = this.appContext.config?.apiUrl || 'http://localhost:3000';
    const url = `${apiUrl}/api/worlds/${targetWorldId}/config/settings?client=${clientType}`;

    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to load Settings: ${response.statusText}`);
    }

    const settings = await response.json();
    if (this.config) {
      this.config.settings = settings;
    }

    return settings;
  }

  /**
   * Clear cached configuration
   */
  clearCache(): void {
    logger.info('Clearing config cache');
    this.config = null;
  }
}
