/**
 * ReloadConfigCommand - Reload configuration from server
 *
 * Reloads all configuration data (WorldInfo, PlayerInfo, PlayerBackpack, Settings)
 * from the REST API in the running client.
 */

import { CommandHandler } from './CommandHandler';
import type { AppContext } from '../AppContext';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('ReloadConfigCommand');

export class ReloadConfigCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'reloadConfig';
  }

  description(): string {
    return 'Reload configuration from server (WorldInfo, PlayerInfo, Settings, Backpack)';
  }

  async execute(_args: string[]): Promise<void> {
    const configService = this.appContext.services.config;

    if (!configService) {
      logger.error('ConfigService not available');
      return;
    }

    try {
      logger.info('Reloading configuration from server...');

      // Determine client type
      const clientType = __EDITOR__ ? 'editor' : 'viewer';

      // Reload configuration
      const config = await configService.reloadConfig(clientType);

      logger.info('Configuration reloaded successfully');
      logger.info('WorldInfo:', {
        worldId: config.worldInfo?.worldId,
        worldName: config.worldInfo?.worldName,
        chunkSize: config.worldInfo?.chunkSize,
      });
      logger.info('PlayerInfo:', {
        displayName: config.playerInfo?.displayName,
        baseWalkSpeed: config.playerInfo?.baseWalkSpeed,
      });
      logger.info('Settings:', {
        name: config.settings?.name,
        inputController: config.settings?.inputController,
      });
      logger.info('PlayerBackpack:', {
        itemCount: Object.keys(config.playerBackpack?.itemIds || {}).length,
        wearingCount: Object.keys(config.playerBackpack?.wearingItemIds || {}).length,
      });
    } catch (error) {
      logger.error('Failed to reload configuration', undefined, error as Error);
    }
  }
}
