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
import { EntityPhysicsSimulator } from './EntityPhysicsSimulator';

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
  private physicsSimulator: EntityPhysicsSimulator;

  private entitiesPath: string;
  private updateFrequency: number;
  private lastUpdateTime: number = Date.now();

  /**
   * @param worldDataPath Path to world data directory
   * @param updateFrequency Update frequency in milliseconds (default: 1000ms = 1 second)
   */
  constructor(worldDataPath: string, updateFrequency: number = 1000) {
    this.entitiesPath = path.join(worldDataPath, 'entities');
    this.updateFrequency = updateFrequency;

    // Initialize physics simulator
    this.physicsSimulator = new EntityPhysicsSimulator();

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

    // Update physics simulator with WorldManager
    this.physicsSimulator.setWorldManager(worldManager);

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
    const deltaTime = (currentTime - this.lastUpdateTime) / 1000; // Convert to seconds
    this.lastUpdateTime = currentTime;

    let pathwaysGenerated = 0;

    // TODO: Get worldId from configuration
    const worldId = 'main';

    for (const spawnDef of this.spawnDefinitions.values()) {
      // Check if entity has physics enabled
      const entity = this.entityManager?.getEntity(spawnDef.entityId);
      const hasPhysics = entity?.physics ?? false;

      // Debug: Log physics status (only log once per entity)
      if (!spawnDef.physicsState && entity) {
        logger.info('Entity physics status', {
          entityId: spawnDef.entityId,
          hasPhysics: hasPhysics,
          entityData: entity,
        });
      }

      if (hasPhysics) {
        // Physics-based update
        await this.updatePhysicsEntity(spawnDef, deltaTime, worldId);
        pathwaysGenerated++;
      } else {
        // Behavior-based update (traditional waypoint system)
        const pathway = await this.updateBehaviorEntity(spawnDef, currentTime, worldId);
        if (pathway) {
          pathwaysGenerated++;
        }
      }
    }

    if (pathwaysGenerated > 0) {
      logger.debug('Update cycle completed', { pathwaysGenerated });
    }
  }

  /**
   * Update entity using behavior system (traditional waypoint-based)
   */
  private async updateBehaviorEntity(
    spawnDef: ServerEntitySpawnDefinition,
    currentTime: number,
    worldId: string
  ): Promise<EntityPathway | null> {
    // Get behavior
    const behavior = this.behaviors.get(spawnDef.behaviorModel);
    if (!behavior) {
      logger.warn('Behavior not found', {
        entityId: spawnDef.entityId,
        behaviorModel: spawnDef.behaviorModel,
      });
      return null;
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

      logger.debug('Pathway generated', {
        entityId: spawnDef.entityId,
        waypoints: pathway.waypoints.length,
        chunks: spawnDef.chunks.length,
      });

      return pathway;
    }

    return null;
  }

  /**
   * Find ground height at position (helper for initialization)
   */
  private async findGroundHeight(
    worldId: string,
    x: number,
    z: number,
    startY: number
  ): Promise<number> {
    if (!this.worldManager) {
      return 64;
    }

    const floorX = Math.floor(x);
    const floorZ = Math.floor(z);

    try {
      // Search downward from start position
      for (let y = Math.floor(startY); y >= 0; y--) {
        const block = await this.worldManager.getBlock(worldId, floorX, y, floorZ);
        if (block && block.blockTypeId !== 'air') {
          return y + 1; // Stand on top of block
        }
      }
    } catch (error) {
      // If chunk loading fails, use default ground height
      logger.warn('Failed to find ground height, using default', {
        x: floorX,
        z: floorZ,
        error: error instanceof Error ? error.message : 'Unknown error'
      });
      return 64;
    }

    return 64; // Default if not found
  }

  /**
   * Update entity using physics system
   */
  private async updatePhysicsEntity(
    spawnDef: ServerEntitySpawnDefinition,
    deltaTime: number,
    worldId: string
  ): Promise<void> {
    // Initialize physics state if not present
    if (!spawnDef.physicsState) {
      // Find actual ground position before initializing
      let initPosition = spawnDef.initialPosition;

      if (this.worldManager) {
        // Search for ground from initial position
        const groundY = await this.findGroundHeight(
          worldId,
          spawnDef.initialPosition.x,
          spawnDef.initialPosition.z,
          spawnDef.initialPosition.y
        );

        initPosition = {
          x: spawnDef.initialPosition.x,
          y: groundY,
          z: spawnDef.initialPosition.z,
        };

        logger.debug('Initialized entity on ground', {
          entityId: spawnDef.entityId,
          groundY,
          originalY: spawnDef.initialPosition.y,
        });
      }

      this.physicsSimulator.initializePhysicsState(
        spawnDef,
        initPosition,
        spawnDef.initialRotation
      );
    }

    // Get entity model for dimensions
    const entityModel = this.entityManager?.getEntityModel(spawnDef.entityModelId);
    if (!entityModel) {
      logger.warn('Entity model not found for physics update', {
        entityId: spawnDef.entityId,
        entityModelId: spawnDef.entityModelId,
      });
      return;
    }

    // Update physics
    await this.physicsSimulator.updatePhysics(spawnDef, entityModel, deltaTime, worldId);

    // Get behavior for movement intention
    const behavior = this.behaviors.get(spawnDef.behaviorModel);
    if (behavior) {
      // Let behavior apply velocity/impulses (e.g., walking direction)
      await behavior.updatePhysics(spawnDef, this.physicsSimulator, worldId);
    }

    // Generate pathway from current physics state (for client synchronization)
    const pathway = this.generatePathwayFromPhysics(spawnDef);
    if (pathway) {
      spawnDef.currentPathway = pathway;
      spawnDef.chunks = calculateAffectedChunks(pathway);
      this.notifyPathwayGenerated(pathway);
    }
  }

  /**
   * Generate pathway from physics state for client synchronization
   */
  private generatePathwayFromPhysics(spawnDef: ServerEntitySpawnDefinition): EntityPathway | null {
    if (!spawnDef.physicsState) {
      return null;
    }

    const state = spawnDef.physicsState;
    const currentTime = Date.now();

    // Calculate velocity magnitude for pose
    const speed = Math.sqrt(
      state.velocity.x * state.velocity.x +
      state.velocity.z * state.velocity.z
    );

    // Determine pose based on movement
    let pose = 0; // IDLE
    if (!state.grounded) {
      pose = 5; // JUMP
    } else if (speed > 0.1) {
      pose = 1; // WALK
    }

    // Create pathway with current position and predicted future position
    // This helps the client interpolate smoothly
    const waypoints = [
      {
        timestamp: currentTime,
        target: { ...state.position },
        rotation: { ...state.rotation },
        pose: pose,
      }
    ];

    // Add a future waypoint based on current velocity (for smooth client interpolation)
    // Predict position 500ms in the future
    const predictionTime = 500; // milliseconds
    const futurePosition = {
      x: state.position.x + state.velocity.x * (predictionTime / 1000),
      y: state.position.y + state.velocity.y * (predictionTime / 1000),
      z: state.position.z + state.velocity.z * (predictionTime / 1000),
    };

    waypoints.push({
      timestamp: currentTime + predictionTime,
      target: futurePosition,
      rotation: { ...state.rotation },
      pose: pose,
    });

    return {
      entityId: spawnDef.entityId,
      startAt: currentTime,
      waypoints: waypoints,
      physicsEnabled: true,
      velocity: { ...state.velocity },
      grounded: state.grounded,
    };
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
