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
import { DEFAULT_STATE_VALUES } from '@nimbus/shared';
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
   * Helper: Load player info from file
   */
  async function loadPlayerInfoFromFile(worldId: string): Promise<PlayerInfo> {
    try {
      const playerInfoPath = path.join(
        process.cwd(),
        'data',
        'worlds',
        worldId,
        'playerinfo.json'
      );

      const data = await fs.readFile(playerInfoPath, 'utf-8');
      return JSON.parse(data);
    } catch (error: any) {
      if (error.code === 'ENOENT') {
        // Return default player info if file doesn't exist
        console.warn(`PlayerInfo file not found for world ${worldId}, returning defaults`);
        return {
          playerId: 'default-player',
          displayName: 'Player',
          thirdPersonModelId: 'wizard1',
          stateValues: DEFAULT_STATE_VALUES,
          baseWalkSpeed: 5.0,
          baseRunSpeed: 7.0,
          baseUnderwaterSpeed: 3.0,
          baseCrawlSpeed: 1.5,
          baseRidingSpeed: 8.0,
          baseJumpSpeed: 8.0,
          effectiveWalkSpeed: 5.0,
          effectiveRunSpeed: 7.0,
          effectiveUnderwaterSpeed: 3.0,
          effectiveCrawlSpeed: 1.5,
          effectiveRidingSpeed: 8.0,
          effectiveJumpSpeed: 8.0,
          eyeHeight: 1.6,
          dimensions: {
            walk: { height: 2.0, width: 0.6, footprint: 0.3 },
            sprint: { height: 2.0, width: 0.6, footprint: 0.3 },
            crouch: { height: 1.0, width: 0.6, footprint: 0.3 },
            swim: { height: 1.8, width: 0.6, footprint: 0.3 },
            climb: { height: 1.8, width: 0.6, footprint: 0.3 },
            fly: { height: 1.8, width: 0.6, footprint: 0.3 },
            teleport: { height: 1.8, width: 0.6, footprint: 0.3 },
          },
          stealthRange: 8.0,
          distanceNotifyReductionWalk: 0.0,
          distanceNotifyReductionCrouch: 0.5,
          selectionRadius: 5.0,
          baseTurnSpeed: 0.003,
          effectiveTurnSpeed: 0.003,
          baseUnderwaterTurnSpeed: 0.002,
          effectiveUnderwaterTurnSpeed: 0.002,
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

      // Load player info from file
      const playerInfo = await loadPlayerInfoFromFile(worldId);

      // Load player backpack from file
      const playerBackpack = await loadPlayerBackpackFromFile(worldId);

      // Build complete configuration
      const config: EngineConfiguration = {
        worldInfo: world.worldInfo,
        playerInfo,
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

      const playerInfo = await loadPlayerInfoFromFile(worldId);
      res.json(playerInfo);
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
