/**
 * Config REST API Routes
 *
 * Provides endpoints for loading configuration data:
 * - Complete EngineConfiguration
 * - Individual config sections (PlayerInfo, WorldInfo, PlayerBackpack, Settings)
 */

import express from 'express';
import path from 'path';
import fs from 'fs/promises';
import type {
  EngineConfiguration,
  PlayerInfo,
  WorldInfo,
  PlayerBackpack,
  Settings,
} from '@nimbus/shared';
import type { WorldManager } from '../../world/WorldManager';

/**
 * Create config routes
 * @returns Express router
 */
export function createConfigRoutes(worldManager: WorldManager): express.Router {
  const router = express.Router();

  /**
   * Helper: Load settings from file
   */
  async function loadSettingsFromFile(worldId: string, clientType: string): Promise<Settings> {
    try {
      const settingsPath = path.join(
        process.cwd(),
        'data',
        'worlds',
        worldId,
        `settings_${clientType}.json`
      );

      const data = await fs.readFile(settingsPath, 'utf-8');
      return JSON.parse(data);
    } catch (error: any) {
      if (error.code === 'ENOENT') {
        // Return default settings if file doesn't exist
        console.warn(`Settings file not found for world ${worldId}, returning defaults`);
        return {
          name: 'Player',
          inputController: 'keyboard',
          inputMappings: {},
        };
      }
      throw error;
    }
  }

  /**
   * Helper: Load player backpack from file
   */
  async function loadPlayerBackpackFromFile(worldId: string): Promise<PlayerBackpack> {
    try {
      const backpackPath = path.join(
        process.cwd(),
        'data',
        'worlds',
        worldId,
        'playerbackpack.json'
      );

      const data = await fs.readFile(backpackPath, 'utf-8');
      return JSON.parse(data);
    } catch (error: any) {
      if (error.code === 'ENOENT') {
        // Return default backpack if file doesn't exist
        console.warn(`PlayerBackpack file not found for world ${worldId}, returning defaults`);
        return {
          itemIds: {},
          wearingItemIds: {} as any,
        };
      }
      throw error;
    }
  }

  /**
   * GET /api/worlds/:worldId/config?client={clientType}
   * Get complete EngineConfiguration
   */
  router.get('/:worldId/config', async (req, res) => {
    try {
      const worldId = req.params.worldId;
      const clientType = (req.query.client as string) || 'viewer';

      const world = worldManager.getWorld(worldId);
      if (!world) {
        res.status(404).json({ error: 'World not found' });
        return;
      }

      // Load settings from file
      const settings = await loadSettingsFromFile(worldId, clientType);

      // Load player backpack from file
      const playerBackpack = await loadPlayerBackpackFromFile(worldId);

      // Build complete configuration
      const config: EngineConfiguration = {
        worldInfo: world.worldInfo,
        playerInfo: world.playerInfo,
        playerBackpack,
        settings,
      };

      res.json(config);
    } catch (error: any) {
      console.error('Failed to load config:', error);
      res.status(500).json({ error: 'Failed to load configuration' });
    }
  });

  /**
   * GET /api/worlds/:worldId/config/playerinfo
   * Get PlayerInfo only
   */
  router.get('/:worldId/config/playerinfo', async (req, res) => {
    try {
      const worldId = req.params.worldId;

      const world = worldManager.getWorld(worldId);
      if (!world) {
        res.status(404).json({ error: 'World not found' });
        return;
      }

      res.json(world.playerInfo);
    } catch (error: any) {
      console.error('Failed to load PlayerInfo:', error);
      res.status(500).json({ error: 'Failed to load PlayerInfo' });
    }
  });

  /**
   * GET /api/worlds/:worldId/config/worldinfo
   * Get WorldInfo only
   */
  router.get('/:worldId/config/worldinfo', async (req, res) => {
    try {
      const worldId = req.params.worldId;

      const world = worldManager.getWorld(worldId);
      if (!world) {
        res.status(404).json({ error: 'World not found' });
        return;
      }

      res.json(world.worldInfo);
    } catch (error: any) {
      console.error('Failed to load WorldInfo:', error);
      res.status(500).json({ error: 'Failed to load WorldInfo' });
    }
  });

  /**
   * GET /api/worlds/:worldId/config/playerbackpack
   * Get PlayerBackpack only
   */
  router.get('/:worldId/config/playerbackpack', async (req, res) => {
    try {
      const worldId = req.params.worldId;

      const world = worldManager.getWorld(worldId);
      if (!world) {
        res.status(404).json({ error: 'World not found' });
        return;
      }

      // Load player backpack from file
      const playerBackpack = await loadPlayerBackpackFromFile(worldId);

      res.json(playerBackpack);
    } catch (error: any) {
      console.error('Failed to load PlayerBackpack:', error);
      res.status(500).json({ error: 'Failed to load PlayerBackpack' });
    }
  });

  /**
   * GET /api/worlds/:worldId/config/settings?client={clientType}
   * Get Settings only
   */
  router.get('/:worldId/config/settings', async (req, res) => {
    try {
      const worldId = req.params.worldId;
      const clientType = (req.query.client as string) || 'viewer';

      const world = worldManager.getWorld(worldId);
      if (!world) {
        res.status(404).json({ error: 'World not found' });
        return;
      }

      // Load settings from file
      const settings = await loadSettingsFromFile(worldId, clientType);

      res.json(settings);
    } catch (error: any) {
      console.error('Failed to load Settings:', error);
      res.status(500).json({ error: 'Failed to load Settings' });
    }
  });

  return router;
}
