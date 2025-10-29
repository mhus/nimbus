/**
 * World Manager - Manages world instances
 */

import { getLogger, type ChunkData } from '@nimbus/shared';
import type { WorldInstance } from '../types/ServerTypes';
import { BlockTypeRegistry } from './BlockTypeRegistry';
import { ChunkStorage } from '../storage/ChunkStorage';

const logger = getLogger('WorldManager');

export class WorldManager {
  private worlds = new Map<string, WorldInstance>();
  private blockTypeRegistry: BlockTypeRegistry;
  private chunkStorages = new Map<string, ChunkStorage>();

  constructor() {
    this.blockTypeRegistry = new BlockTypeRegistry();
    this.initializeTestWorlds();

    // Start auto-save interval (every 30 seconds)
    this.startAutoSave();
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

  private createWorld(params: Omit<WorldInstance, 'chunks' | 'createdAt' | 'updatedAt'>): void {
    const now = new Date().toISOString();
    const world: WorldInstance = {
      ...params,
      chunks: new Map(),
      createdAt: now,
      updatedAt: now,
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
    cz: number,
    terrainGenerator: any
  ): Promise<ChunkData | null> {
    const world = this.worlds.get(worldId);
    if (!world) {
      logger.warn('World not found', { worldId });
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

    // Step 3: Generate new chunk
    logger.debug('Generating new chunk', { cx, cz });
    const newServerChunk = terrainGenerator.generateChunk(cx, cz, world.chunkSize);
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
