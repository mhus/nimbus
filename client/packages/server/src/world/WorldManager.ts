/**
 * World Manager - Manages world instances
 */

import { getLogger } from '@nimbus/shared';
import type { WorldInstance } from '../types/ServerTypes';
import { BlockTypeRegistry } from './BlockTypeRegistry';

const logger = getLogger('WorldManager');

export class WorldManager {
  private worlds = new Map<string, WorldInstance>();
  private blockTypeRegistry: BlockTypeRegistry;

  constructor() {
    this.blockTypeRegistry = new BlockTypeRegistry();
    this.initializeTestWorlds();
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
}
