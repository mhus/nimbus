/**
 * EntitySimulator - Spawns entities and manages their pathways
 *
 * Features:
 * - Load entity spawn definitions from data/worlds/{worldId}/entities/
 * - Update entity behaviors and generate new pathways
 * - Calculate affected chunks from pathways
 * - Distribute pathways to SessionHandlers
 */

import * as fs from 'fs';
import * as path from 'path';
import type { EntityPathway, ServerEntitySpawnDefinition, Vector2 } from '@nimbus/shared';
import { calculateAffectedChunks, getLogger, ExceptionHandler } from '@nimbus/shared';
import { EntityBehavior } from './behaviors/EntityBehavior';
import { PreyAnimalBehavior } from './behaviors/PreyAnimalBehavior';
import type { WorldManager } from '../world/WorldManager';
import type { EntityManager } from './EntityManager';

const logger = getLogger('EntitySimulator');

/**
 * EntitySimulator - Server-side entity simulation
 */
export class EntitySimulator {
  private spawnDefinitions: Map<string, ServerEntitySpawnDefinition> = new Map();
  private behaviors: Map<string, EntityBehavior> = new Map();
  private updateInterval: NodeJS.Timeout | null = null;
  private pathwayCallbacks: Set<(pathway: EntityPathway) => void> = new Set();
  private worldManager: WorldManager | null = null;
  private entityManager: EntityManager | null = null;

  private entitiesPath: string;
  private updateFrequency: number;

  /**
   * @param worldDataPath Path to world data directory
   * @param updateFrequency Update frequency in milliseconds (default: 1000ms = 1 second)
   */
  constructor(worldDataPath: string, updateFrequency: number = 1000) {
    this.entitiesPath = path.join(worldDataPath, 'entities');
    this.updateFrequency = updateFrequency;

    logger.info('EntitySimulator initialized', {
      entitiesPath: this.entitiesPath,
      updateFrequency: this.updateFrequency,
    });

    // Register behaviors
    this.registerBehavior('PreyAnimalBehavior', new PreyAnimalBehavior());

    // Load spawn definitions
    this.loadSpawnDefinitions();
  }

  /**
   * Set WorldManager for ground height calculation
   */
  setWorldManager(worldManager: WorldManager): void {
    this.worldManager = worldManager;

    // Update behaviors with WorldManager
    for (const behavior of this.behaviors.values()) {
      behavior.setWorldManager(worldManager);
    }

    logger.debug('WorldManager set in EntitySimulator');
  }

  /**
   * Set EntityManager for model access
   */
  setEntityManager(entityManager: EntityManager): void {
    this.entityManager = entityManager;

    // Update behaviors with EntityManager
    for (const behavior of this.behaviors.values()) {
      behavior.setEntityManager(entityManager);
    }

    logger.debug('EntityManager set in EntitySimulator');
  }

  /**
   * Register a behavior
   */
  registerBehavior(name: string, behavior: EntityBehavior): void {
    this.behaviors.set(name, behavior);

    // Set WorldManager if available
    if (this.worldManager) {
      behavior.setWorldManager(this.worldManager);
    }

    // Set EntityManager if available
    if (this.entityManager) {
      behavior.setEntityManager(this.entityManager);
    }

    logger.debug('Behavior registered', { name });
  }

  /**
   * Load spawn definitions from entities directory
   */
  private loadSpawnDefinitions(): void {
    try {
      if (!fs.existsSync(this.entitiesPath)) {
        logger.warn('Entities directory does not exist', { path: this.entitiesPath });
        fs.mkdirSync(this.entitiesPath, { recursive: true });
        logger.info('Created entities directory', { path: this.entitiesPath });
        return;
      }

      const files = fs.readdirSync(this.entitiesPath);
      let loadedCount = 0;

      for (const file of files) {
        if (!file.endsWith('.json')) {
          continue;
        }

        const filePath = path.join(this.entitiesPath, file);

        try {
          const data = fs.readFileSync(filePath, 'utf-8');
          const spawnDef: ServerEntitySpawnDefinition = JSON.parse(data);

          this.spawnDefinitions.set(spawnDef.entityId, spawnDef);
          loadedCount++;
          logger.debug('Loaded spawn definition', { entityId: spawnDef.entityId, file });
        } catch (error) {
          ExceptionHandler.handle(error, 'EntitySimulator.loadSpawnDefinitions.file', { file });
        }
      }

      logger.info('Spawn definitions loaded', { count: loadedCount });
    } catch (error) {
      ExceptionHandler.handle(error, 'EntitySimulator.loadSpawnDefinitions');
    }
  }

  /**
   * Save spawn definition to file
   */
  saveSpawnDefinition(spawnDef: ServerEntitySpawnDefinition): void {
    try {
      const filePath = path.join(this.entitiesPath, `${spawnDef.entityId}.json`);
      fs.writeFileSync(filePath, JSON.stringify(spawnDef, null, 2), 'utf-8');

      this.spawnDefinitions.set(spawnDef.entityId, spawnDef);
      logger.info('Spawn definition saved', { entityId: spawnDef.entityId });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'EntitySimulator.saveSpawnDefinition', {
        entityId: spawnDef.entityId,
      });
    }
  }

  /**
   * Get spawn definition by entity ID
   */
  getSpawnDefinition(entityId: string): ServerEntitySpawnDefinition | null {
    return this.spawnDefinitions.get(entityId) || null;
  }

  /**
   * Get all spawn definitions
   */
  getAllSpawnDefinitions(): ServerEntitySpawnDefinition[] {
    return Array.from(this.spawnDefinitions.values());
  }

  /**
   * Add spawn definition
   */
  addSpawnDefinition(spawnDef: ServerEntitySpawnDefinition): void {
    this.spawnDefinitions.set(spawnDef.entityId, spawnDef);
    logger.debug('Spawn definition added', { entityId: spawnDef.entityId });
  }

  /**
   * Remove spawn definition
   */
  removeSpawnDefinition(entityId: string): void {
    this.spawnDefinitions.delete(entityId);
    logger.debug('Spawn definition removed', { entityId });
  }

  /**
   * Register callback for new pathways
   * Callbacks are invoked when a new pathway is generated
   */
  onPathwayGenerated(callback: (pathway: EntityPathway) => void): void {
    this.pathwayCallbacks.add(callback);
  }

  /**
   * Unregister pathway callback
   */
  offPathwayGenerated(callback: (pathway: EntityPathway) => void): void {
    this.pathwayCallbacks.delete(callback);
  }

  /**
   * Start simulation loop
   */
  start(): void {
    if (this.updateInterval) {
      logger.warn('Simulator already running');
      return;
    }

    logger.info('Starting entity simulator', { updateFrequency: this.updateFrequency });

    this.updateInterval = setInterval(() => {
      this.update();
    }, this.updateFrequency);
  }

  /**
   * Stop simulation loop
   */
  stop(): void {
    if (this.updateInterval) {
      clearInterval(this.updateInterval);
      this.updateInterval = null;
      logger.info('Entity simulator stopped');
    }
  }

  /**
   * Update all entities and generate new pathways
   */
  private async update(): Promise<void> {
    const currentTime = Date.now();
    let pathwaysGenerated = 0;

    // TODO: Get worldId from configuration
    const worldId = 'main';

    for (const spawnDef of this.spawnDefinitions.values()) {
      // Get behavior
      const behavior = this.behaviors.get(spawnDef.behaviorModel);
      if (!behavior) {
        logger.warn('Behavior not found', {
          entityId: spawnDef.entityId,
          behaviorModel: spawnDef.behaviorModel,
        });
        continue;
      }

      // Update behavior and generate pathway if needed
      const pathway = await behavior.update(spawnDef, currentTime, worldId);
      if (pathway) {
        // Update spawn definition with new pathway
        spawnDef.currentPathway = pathway;

        // Calculate affected chunks
        spawnDef.chunks = calculateAffectedChunks(pathway);

        // Notify callbacks
        this.notifyPathwayGenerated(pathway);

        pathwaysGenerated++;
        logger.debug('Pathway generated', {
          entityId: spawnDef.entityId,
          waypoints: pathway.waypoints.length,
          chunks: spawnDef.chunks.length,
        });
      }
    }

    if (pathwaysGenerated > 0) {
      logger.debug('Update cycle completed', { pathwaysGenerated });
    }
  }

  /**
   * Notify all callbacks about new pathway
   */
  private notifyPathwayGenerated(pathway: EntityPathway): void {
    for (const callback of this.pathwayCallbacks) {
      try {
        callback(pathway);
      } catch (error) {
        ExceptionHandler.handle(error, 'EntitySimulator.notifyPathwayGenerated', {
          entityId: pathway.entityId,
        });
      }
    }
  }

  /**
   * Get pathways for specific chunk
   */
  getPathwaysForChunk(chunkX: number, chunkZ: number): EntityPathway[] {
    const pathways: EntityPathway[] = [];

    for (const spawnDef of this.spawnDefinitions.values()) {
      if (!spawnDef.currentPathway) {
        continue;
      }

      // Check if chunk is in affected chunks list
      const hasChunk = spawnDef.chunks.some((chunk: Vector2) => chunk.x === chunkX && chunk.z === chunkZ);

      if (hasChunk) {
        pathways.push(spawnDef.currentPathway);
      }
    }

    return pathways;
  }

  /**
   * Get all current pathways
   */
  getAllPathways(): EntityPathway[] {
    const pathways: EntityPathway[] = [];

    for (const spawnDef of this.spawnDefinitions.values()) {
      if (spawnDef.currentPathway) {
        pathways.push(spawnDef.currentPathway);
      }
    }

    return pathways;
  }
}
