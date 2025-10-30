/**
 * World Manager - Manages world instances
 */

import { getLogger, type ChunkData, type WorldInfo, ExceptionHandler } from '@nimbus/shared';
import type { WorldInstance } from '../types/ServerTypes';
import { BlockTypeRegistry } from './BlockTypeRegistry';
import { AssetManager } from '../assets/AssetManager';
import { ChunkStorage } from '../storage/ChunkStorage';
import { GeneratorFactory } from './generators/GeneratorFactory';
import type { WorldGenerator } from './generators/WorldGenerator';
import * as fs from 'fs';
import * as path from 'path';

const logger = getLogger('WorldManager');

export class WorldManager {
  private worlds = new Map<string, WorldInstance>();
  private blockTypeRegistry: BlockTypeRegistry;
  private assetManager: AssetManager;
  private chunkStorages = new Map<string, ChunkStorage>();

  constructor() {
    this.blockTypeRegistry = new BlockTypeRegistry();
    this.assetManager = new AssetManager();

    // Load worlds from data directory
    this.loadWorldsFromDirectory();

    // If no worlds loaded, initialize test worlds
    if (this.worlds.size === 0) {
      logger.warn('No worlds found in data directory, initializing test worlds');
      this.initializeTestWorlds();
    }

    // Start auto-save interval (every 30 seconds)
    this.startAutoSave();
  }

  /**
   * Load worlds from data directory
   * Scans ./data/worlds/ for world folders containing info.json
   */
  private loadWorldsFromDirectory(): void {
    const dataDir = path.join(process.cwd(), 'data', 'worlds');

    // Check if data directory exists
    if (!fs.existsSync(dataDir)) {
      logger.info('Data directory does not exist, creating it', { path: dataDir });
      try {
        fs.mkdirSync(dataDir, { recursive: true });
      } catch (error) {
        ExceptionHandler.handle(error, 'WorldManager.loadWorldsFromDirectory.mkdir', { dataDir });
        return;
      }
    }

    try {
      const entries = fs.readdirSync(dataDir, { withFileTypes: true });

      for (const entry of entries) {
        if (!entry.isDirectory()) continue;

        const worldPath = path.join(dataDir, entry.name);
        const infoPath = path.join(worldPath, 'info.json');

        // Check if info.json exists
        if (!fs.existsSync(infoPath)) {
          logger.debug('World directory missing info.json, skipping', { path: worldPath });
          continue;
        }

        // Load and parse info.json
        try {
          const infoContent = fs.readFileSync(infoPath, 'utf-8');
          const worldInfo = JSON.parse(infoContent);

          // Load generator.json if exists
          const generatorPath = path.join(worldPath, 'generator.json');
          let generatorConfig = null;
          if (fs.existsSync(generatorPath)) {
            try {
              const generatorContent = fs.readFileSync(generatorPath, 'utf-8');
              generatorConfig = JSON.parse(generatorContent);
              logger.debug('Loaded generator config', { worldId: entry.name, type: generatorConfig.type });
            } catch (error) {
              logger.warn('Failed to load generator.json, using default', { worldId: entry.name });
            }
          } else {
            logger.debug('No generator.json found, using default', { worldId: entry.name });
          }

          // Create world instance with generator
          this.createWorldFromInfo(entry.name, worldInfo, generatorConfig);

          logger.info('Loaded world from directory', {
            worldId: entry.name,
            name: worldInfo.name || entry.name,
            generator: generatorConfig?.type || 'default'
          });
        } catch (error) {
          ExceptionHandler.handle(error, 'WorldManager.loadWorldsFromDirectory.loadWorld', {
            worldPath,
            infoPath
          });
        }
      }

      logger.info(`Loaded ${this.worlds.size} worlds from data directory`);
    } catch (error) {
      ExceptionHandler.handle(error, 'WorldManager.loadWorldsFromDirectory', { dataDir });
    }
  }

  /**
   * Create world from info.json data
   */
  private createWorldFromInfo(worldId: string, info: any, generatorConfig?: any): void {
    const now = new Date().toISOString();

    // Create generator if config provided
    let generator: WorldGenerator | undefined;
    if (generatorConfig) {
      try {
        generator = GeneratorFactory.createGenerator(generatorConfig, this.blockTypeRegistry);
      } catch (error) {
        logger.error('Failed to create generator', { worldId, generatorConfig }, error as Error);
      }
    }

    // If no generator created, use default normal generator
    if (!generator) {
      const defaultConfig = GeneratorFactory.createDefaultConfig('normal', info.seed);
      generator = GeneratorFactory.createGenerator(defaultConfig, this.blockTypeRegistry);
      logger.info('Using default normal generator', { worldId });
    }

    // Get dimensions with defaults
    const dimensions = info.dimensions || {
      minX: -128,
      maxX: 128,
      minY: -64,
      maxY: 192,
      minZ: -128,
      maxZ: 128,
    };

    // Build WorldInfo for client transmission (shared type)
    const worldInfo: WorldInfo = {
      worldId,
      name: info.name || worldId,
      description: info.description || '',
      chunkSize: info.chunkSize || 32,
      status: info.status ?? 0,
      assetPath: `/api/worlds/${worldId}/assets`,
      editorUrl: info.editorUrl, // Optional from info.json
      start: {
        x: dimensions.minX,
        y: dimensions.minY,
        z: dimensions.minZ,
      },
      stop: {
        x: dimensions.maxX,
        y: dimensions.maxY,
        z: dimensions.maxZ,
      },
      createdAt: info.createdAt || now,
      updatedAt: now,
      // Optional fields from info.json
      owner: info.owner,
      settings: info.settings,
      license: info.license,
      startArea: info.startArea,
      worldGroupId: info.worldGroupId,
      assetPort: info.assetPort,
    };

    const world: WorldInstance = {
      worldId,
      name: info.name || worldId,
      description: info.description || '',
      chunkSize: info.chunkSize || 32,
      dimensions,
      seaLevel: info.seaLevel ?? 0,
      groundLevel: info.groundLevel ?? 64,
      status: info.status ?? 0,
      chunks: new Map(),
      generator,
      createdAt: info.createdAt || now,
      updatedAt: now,
      worldInfo, // Shared WorldInfo for client
    };

    this.worlds.set(worldId, world);

    // Initialize chunk storage for this world
    const storage = new ChunkStorage(worldId);
    storage.initialize().catch((error) => {
      logger.error('Failed to initialize chunk storage', { worldId }, error);
    });
    this.chunkStorages.set(worldId, storage);
  }

  private initializeTestWorlds(): void {
    // Test World 1
    this.createWorld({
      worldId: 'test-world-1',
      name: 'Test World 1',
      description: 'A simple test world',
      chunkSize: 16,
      dimensions: {
        minX: -64,
        maxX: 64,
        minY: -64,
        maxY: 64,
        minZ: -64,
        maxZ: 64,
      },
      seaLevel: 0,
      groundLevel: 1,
      status: 0,
    });

    // Test World 2
    this.createWorld({
      worldId: 'test-world-2',
      name: 'Test World 2',
      description: 'Another test world',
      chunkSize: 16,
      dimensions: {
        minX: -128,
        maxX: 128,
        minY: -64,
        maxY: 128,
        minZ: -128,
        maxZ: 128,
      },
      seaLevel: 0,
      groundLevel: 1,
      status: 0,
    });

    logger.info(`Initialized ${this.worlds.size} test worlds`);
  }

  private createWorld(params: Omit<WorldInstance, 'chunks' | 'createdAt' | 'updatedAt' | 'worldInfo'>): void {
    const now = new Date().toISOString();

    // Build WorldInfo for client transmission
    const worldInfo: WorldInfo = {
      worldId: params.worldId,
      name: params.name,
      description: params.description,
      chunkSize: params.chunkSize,
      status: params.status,
      assetPath: `/api/worlds/${params.worldId}/assets`,
      start: {
        x: params.dimensions.minX,
        y: params.dimensions.minY,
        z: params.dimensions.minZ,
      },
      stop: {
        x: params.dimensions.maxX,
        y: params.dimensions.maxY,
        z: params.dimensions.maxZ,
      },
      createdAt: now,
      updatedAt: now,
    };

    const world: WorldInstance = {
      ...params,
      chunks: new Map(),
      createdAt: now,
      updatedAt: now,
      worldInfo,
    };
    this.worlds.set(world.worldId, world);

    // Initialize chunk storage for this world
    const storage = new ChunkStorage(world.worldId);
    storage.initialize().catch((error) => {
      logger.error('Failed to initialize chunk storage', { worldId: world.worldId }, error);
    });
    this.chunkStorages.set(world.worldId, storage);
  }

  getWorld(worldId: string): WorldInstance | undefined {
    return this.worlds.get(worldId);
  }

  getAllWorlds(): WorldInstance[] {
    return Array.from(this.worlds.values());
  }

  getBlockTypeRegistry(): BlockTypeRegistry {
    return this.blockTypeRegistry;
  }

  getAssetManager(): AssetManager {
    return this.assetManager;
  }

  getChunkStorage(worldId: string): ChunkStorage | undefined {
    return this.chunkStorages.get(worldId);
  }

  /**
   * Get chunk with 3-step algorithm:
   * 1. Check if ServerChunk is loaded in world.chunks → convert to ChunkData
   * 2. Check if ChunkData exists in storage → return directly
   * 3. Generate new ServerChunk → store in world.chunks → convert to ChunkData
   */
  async getChunkData(
    worldId: string,
    cx: number,
    cz: number
  ): Promise<ChunkData | null> {
    const world = this.worlds.get(worldId);
    if (!world) {
      logger.warn('World not found', { worldId });
      return null;
    }

    if (!world.generator) {
      logger.error('World has no generator', { worldId });
      return null;
    }

    const storage = this.chunkStorages.get(worldId);
    if (!storage) {
      logger.warn('ChunkStorage not found', { worldId });
      return null;
    }

    const key = `${cx},${cz}`;

    // Step 1: Check if ServerChunk is loaded in memory
    const serverChunk = world.chunks.get(key);
    if (serverChunk) {
      logger.debug('Returning ServerChunk from memory', { cx, cz });
      return serverChunk.toChunkData();
    }

    // Step 2: Check if ChunkData exists in storage
    const storedChunk = await storage.load(cx, cz);
    if (storedChunk) {
      logger.debug('Returning ChunkData from storage', { cx, cz });
      return storedChunk;
    }

    // Step 3: Generate new chunk using world's generator
    logger.debug('Generating new chunk', { cx, cz, generator: world.generator.name });
    const newServerChunk = world.generator.generateChunk(cx, cz, world.chunkSize);
    world.chunks.set(key, newServerChunk);

    return newServerChunk.toChunkData();
  }

  /**
   * Start auto-save interval for dirty chunks
   */
  private startAutoSave(): void {
    setInterval(() => {
      this.saveAllDirtyChunks().catch((error) => {
        logger.error('Auto-save failed', {}, error);
      });
    }, 30000); // Every 30 seconds

    logger.info('Auto-save started (interval: 30s)');
  }

  /**
   * Save all dirty chunks across all worlds
   */
  async saveAllDirtyChunks(): Promise<void> {
    let savedCount = 0;

    for (const [worldId, world] of this.worlds) {
      const storage = this.chunkStorages.get(worldId);
      if (!storage) continue;

      for (const [key, serverChunk] of world.chunks) {
        if (serverChunk.isDirty) {
          try {
            const chunkData = serverChunk.toChunkData();
            await storage.save(chunkData);
            serverChunk.isDirty = false;
            savedCount++;
          } catch (error) {
            logger.error('Failed to save dirty chunk', { worldId, key }, error as Error);
          }
        }
      }
    }

    if (savedCount > 0) {
      logger.debug('Auto-saved dirty chunks', { count: savedCount });
    }
  }

  /**
   * Save all chunks (shutdown/backup)
   */
  async saveAll(): Promise<void> {
    logger.info('Saving all chunks...');
    let savedCount = 0;

    for (const [worldId, world] of this.worlds) {
      const storage = this.chunkStorages.get(worldId);
      if (!storage) continue;

      for (const serverChunk of world.chunks.values()) {
        try {
          const chunkData = serverChunk.toChunkData();
          await storage.save(chunkData);
          serverChunk.isDirty = false;
          savedCount++;
        } catch (error) {
          logger.error('Failed to save chunk', { worldId, cx: serverChunk.cx, cz: serverChunk.cz }, error as Error);
        }
      }
    }

    logger.info('Saved all chunks', { count: savedCount });
  }
}
