/**
 * EntityService - Entity and EntityModel management
 *
 * Manages entity models and entity instances:
 * - Lazy loading from REST API
 * - Caching with LRU eviction
 * - ClientEntity management (rendering state)
 * - Waypoint interpolation
 */

import {
  EntityModel,
  Entity,
  EntityPathway,
  ClientEntity,
  createClientEntity,
  getLogger,
  ExceptionHandler,
} from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { NetworkService } from './NetworkService';

const logger = getLogger('EntityService');

/**
 * Entity event types
 */
export type EntityEventType = 'pathway' | 'visibility' | 'transform' | 'pose' | 'removed';

/**
 * Entity event listener
 */
export type EntityEventListener = (data: any) => void;

/**
 * EntityService configuration
 */
export interface EntityServiceConfig {
  /** Maximum number of entity models in cache */
  maxModelCacheSize?: number;

  /** Maximum number of entities in cache */
  maxEntityCacheSize?: number;

  /** Cache eviction timeout in milliseconds (entities not accessed for this time are evicted) */
  cacheEvictionTimeout?: number;

  /** Interval for cache cleanup in milliseconds */
  cacheCleanupInterval?: number;

  /** Update interval in milliseconds (entity position/state updates) */
  updateInterval?: number;

  /** Visibility radius in blocks (entities beyond this distance from player are hidden) */
  visibilityRadius?: number;
}

/**
 * EntityService - Manages entities and entity models
 *
 * Features:
 * - Lazy loading of entity models from REST API
 * - Lazy loading of entities from REST API
 * - LRU cache with automatic eviction
 * - ClientEntity management with rendering state
 * - Waypoint interpolation
 */
export class EntityService {
  private appContext: AppContext;
  private networkService: NetworkService;
  private config: Required<EntityServiceConfig>;

  // Caches
  private entityModelCache: Map<string, EntityModel> = new Map();
  private entityCache: Map<string, ClientEntity> = new Map();
  private entityPathwayCache: Map<string, EntityPathway> = new Map(); // entityId -> pathway

  // Event system
  private eventListeners: Map<EntityEventType, Set<EntityEventListener>> = new Map();

  // Update loop
  private updateInterval?: NodeJS.Timeout;

  // Cache cleanup
  private cleanupInterval?: NodeJS.Timeout;

  // Visibility radius
  private _visibilityRadius: number;

  constructor(appContext: AppContext, config?: EntityServiceConfig) {
    if (!appContext.services.network) {
      throw new Error('NetworkService is required for EntityService');
    }

    this.appContext = appContext;
    this.networkService = appContext.services.network;

    // Default configuration
    this.config = {
      maxModelCacheSize: config?.maxModelCacheSize ?? 100,
      maxEntityCacheSize: config?.maxEntityCacheSize ?? 1000,
      cacheEvictionTimeout: config?.cacheEvictionTimeout ?? 300000, // 5 minutes
      cacheCleanupInterval: config?.cacheCleanupInterval ?? 60000, // 1 minute
      updateInterval: config?.updateInterval ?? 100, // 100ms update loop
      visibilityRadius: config?.visibilityRadius ?? 50, // 50 blocks
    };

    this._visibilityRadius = this.config.visibilityRadius;

    logger.info('EntityService initialized', { config: this.config });

    // Start cache cleanup
    this.startCacheCleanup();

    // Start update loop
    this.startUpdateLoop();

    // Register chunk events
    this.registerChunkEvents();
  }

  /**
   * Register chunk event listeners
   */
  private registerChunkEvents(): void {
    const chunkService = this.appContext.services.chunk;
    if (!chunkService) {
      logger.warn('ChunkService not available, chunk events will not be registered');
      return;
    }

    // Listen for chunk unload events
    chunkService.on('chunk:unloaded', (data: { cx: number; cz: number }) => {
      // Get chunk size from world info
      const worldInfo = this.appContext.worldInfo;
      if (worldInfo && worldInfo.chunkSize) {
        this.onChunkUnloaded(data.cx, data.cz, worldInfo.chunkSize);
      }
    });

    logger.debug('Chunk event listeners registered');
  }

  /**
   * Get entity model by ID (lazy loading with cache)
   */
  async getEntityModel(modelId: string): Promise<EntityModel | null> {
    try {
      // Check cache
      const cached = this.entityModelCache.get(modelId);
      if (cached) {
        logger.debug('Entity model cache hit', { modelId });
        return cached;
      }

      // Load from REST API
      const worldId = this.appContext.worldInfo?.worldId || 'main';
      const timestamp = Date.now();
      const url = `${this.networkService.getApiUrl()}/api/worlds/${worldId}/entitymodel/${modelId}?t=${timestamp}`;
      logger.debug('Loading entity model from API', { modelId, url });
      const response = await fetch(url);

      if (!response.ok) {
        if (response.status === 404) {
          logger.warn('Entity model not found', { modelId });
          return null;
        }
        throw new Error(`Failed to load entity model: ${response.statusText}`);
      }

      const data = await response.json();
      const model = this.deserializeEntityModel(data);

      if (!model) {
        logger.warn('Failed to deserialize entity model', { modelId, data });
        return null;
      }

      // Add to cache
      this.entityModelCache.set(modelId, model);
      this.evictModelCache();

      logger.debug('Entity model loaded', { modelId });
      return model;
    } catch (error) {
      ExceptionHandler.handle(error, 'EntityService.getEntityModel', { modelId });
      return null;
    }
  }

  /**
   * Get entity by ID (lazy loading with cache)
   */
  async getEntity(entityId: string): Promise<ClientEntity | null> {
    try {
      // Check cache
      const cached = this.entityCache.get(entityId);
      if (cached) {
        cached.lastAccess = Date.now();
        logger.debug('Entity cache hit', { entityId });
        return cached;
      }

      // Load from REST API
      const worldId = this.appContext.worldInfo?.worldId || 'main';
      const timestamp = Date.now();
      const url = `${this.networkService.getApiUrl()}/api/worlds/${worldId}/entity/${entityId}?t=${timestamp}`;
      logger.debug('Loading entity from API', { entityId, url });
      const response = await fetch(url);

      if (!response.ok) {
        if (response.status === 404) {
          logger.warn('Entity not found', { entityId });
          return null;
        }
        throw new Error(`Failed to load entity: ${response.statusText}`);
      }

      const data = await response.json();
      const entity = data as Entity;

      // Load entity model
      const model = await this.getEntityModel(entity.model);
      if (!model) {
        logger.warn('Entity model not found for entity', { entityId, modelId: entity.model });
        return null;
      }

      // Create ClientEntity
      const clientEntity = createClientEntity(entity, model);

      // Add to cache
      this.entityCache.set(entityId, clientEntity);
      this.evictEntityCache();

      logger.debug('Entity loaded', { entityId });
      return clientEntity;
    } catch (error) {
      ExceptionHandler.handle(error, 'EntityService.getEntity', { entityId });
      return null;
    }
  }

  /**
   * Update entity in cache
   */
  updateEntity(entity: Entity): void {
    const clientEntity = this.entityCache.get(entity.id);
    if (clientEntity) {
      clientEntity.entity = entity;
      clientEntity.lastAccess = Date.now();
      logger.debug('Entity updated in cache', { entityId: entity.id });
    }
  }

  /**
   * Set entity pathway
   * If entity doesn't exist, it will be loaded automatically
   */
  async setEntityPathway(pathway: EntityPathway): Promise<void> {
    this.entityPathwayCache.set(pathway.entityId, pathway);

    // Check if entity exists
    let clientEntity = this.entityCache.get(pathway.entityId);

    // If entity doesn't exist, load it (unknown entity from network)
    if (!clientEntity) {
      logger.debug('Unknown entity pathway received, loading entity', { entityId: pathway.entityId });

      try {
        const loadedEntity = await this.getEntity(pathway.entityId);

        if (!loadedEntity) {
          logger.warn('Failed to load entity for pathway', { entityId: pathway.entityId });
          return;
        }

        clientEntity = loadedEntity;
      } catch (error) {
        ExceptionHandler.handle(error, 'EntityService.setEntityPathway.loadEntity', {
          entityId: pathway.entityId,
        });
        return;
      }
    }

    // Update ClientEntity with waypoints
    clientEntity.currentWaypoints = pathway.waypoints;
    clientEntity.currentWaypointIndex = 0;
    clientEntity.lastAccess = Date.now();

    // Initialize position from first waypoint if available
    if (pathway.waypoints.length > 0) {
      const firstWaypoint = pathway.waypoints[0];
      clientEntity.currentPosition = {
        x: firstWaypoint.target.x,
        y: firstWaypoint.target.y,
        z: firstWaypoint.target.z,
      };
      clientEntity.currentRotation = {
        y: firstWaypoint.rotation.y,
        p: firstWaypoint.rotation.p ?? 0,
      };
      clientEntity.currentPose = firstWaypoint.pose ?? 0;
    }

    logger.info('🟢 ENTITY PATHWAY SET IN ENTITYSERVICE', {
      entityId: pathway.entityId,
      waypointCount: pathway.waypoints.length,
      initialPosition: clientEntity.currentPosition,
      visible: clientEntity.visible,
      firstWaypoint: pathway.waypoints[0],
    });

    // Emit pathway event
    logger.info('🟢 EMITTING PATHWAY EVENT', { entityId: pathway.entityId });
    this.emit('pathway', pathway);
  }

  /**
   * Get entity pathway
   */
  getEntityPathway(entityId: string): EntityPathway | null {
    return this.entityPathwayCache.get(entityId) || null;
  }

  /**
   * Update entity position/rotation from interpolation
   */
  updateEntityTransform(
    entityId: string,
    position: { x: number; y: number; z: number },
    rotation: { y: number; p: number },
    pose: number
  ): void {
    const clientEntity = this.entityCache.get(entityId);
    if (clientEntity) {
      clientEntity.currentPosition = position;
      clientEntity.currentRotation = rotation;
      clientEntity.currentPose = pose;
      clientEntity.lastAccess = Date.now();

      // Emit transform event
      this.emit('transform', { entityId, position, rotation, pose });
    }
  }

  /**
   * Set entity visibility
   */
  setEntityVisibility(entityId: string, visible: boolean): void {
    const clientEntity = this.entityCache.get(entityId);
    if (clientEntity) {
      clientEntity.visible = visible;
      clientEntity.lastAccess = Date.now();
      logger.debug('Entity visibility changed', { entityId, visible });

      // Emit visibility event
      this.emit('visibility', { entityId, visible });
    }
  }

  /**
   * Get all cached entities
   */
  getAllEntities(): ClientEntity[] {
    return Array.from(this.entityCache.values());
  }

  /**
   * Get all visible entities
   */
  getVisibleEntities(): ClientEntity[] {
    return Array.from(this.entityCache.values()).filter(e => e.visible);
  }

  /**
   * Remove entity from cache
   */
  removeEntity(entityId: string): void {
    const removed = this.entityCache.delete(entityId);
    this.entityPathwayCache.delete(entityId);

    if (removed) {
      logger.debug('Entity removed from cache', { entityId });

      // Emit removed event
      this.emit('removed', entityId);
    }
  }

  /**
   * Clear all caches
   */
  clearCache(): void {
    this.entityModelCache.clear();
    this.entityCache.clear();
    this.entityPathwayCache.clear();
    logger.info('Entity caches cleared');
  }

  /**
   * Get cache statistics
   */
  getCacheStats(): {
    modelCacheSize: number;
    entityCacheSize: number;
    pathwayCacheSize: number;
    maxModelCacheSize: number;
    maxEntityCacheSize: number;
  } {
    return {
      modelCacheSize: this.entityModelCache.size,
      entityCacheSize: this.entityCache.size,
      pathwayCacheSize: this.entityPathwayCache.size,
      maxModelCacheSize: this.config.maxModelCacheSize,
      maxEntityCacheSize: this.config.maxEntityCacheSize,
    };
  }

  /**
   * Get visibility radius
   */
  get visibilityRadius(): number {
    return this._visibilityRadius;
  }

  /**
   * Set visibility radius
   */
  set visibilityRadius(value: number) {
    this._visibilityRadius = value;
    logger.debug('Visibility radius changed', { radius: value });
  }

  /**
   * Dispose service (stop cleanup interval and update loop)
   */
  dispose(): void {
    if (this.updateInterval) {
      clearInterval(this.updateInterval);
      this.updateInterval = undefined;
      logger.debug('Update loop stopped');
    }

    if (this.cleanupInterval) {
      clearInterval(this.cleanupInterval);
      this.cleanupInterval = undefined;
      logger.debug('Cache cleanup stopped');
    }
  }

  /**
   * Start cache cleanup interval
   */
  private startCacheCleanup(): void {
    this.cleanupInterval = setInterval(() => {
      this.cleanupCache();
    }, this.config.cacheCleanupInterval);

    logger.debug('Cache cleanup started', { interval: this.config.cacheCleanupInterval });
  }

  /**
   * Cleanup expired entities from cache
   */
  private cleanupCache(): void {
    const now = Date.now();
    const evictionThreshold = now - this.config.cacheEvictionTimeout;

    let evictedCount = 0;

    // Evict old entities
    for (const [entityId, clientEntity] of this.entityCache.entries()) {
      if (clientEntity.lastAccess < evictionThreshold) {
        this.entityCache.delete(entityId);
        this.entityPathwayCache.delete(entityId);
        evictedCount++;
      }
    }

    if (evictedCount > 0) {
      logger.debug('Cache cleanup evicted entities', { count: evictedCount });
    }
  }

  /**
   * Evict oldest entity models if cache is full
   */
  private evictModelCache(): void {
    if (this.entityModelCache.size <= this.config.maxModelCacheSize) {
      return;
    }

    // Simple eviction: remove first entry (oldest in insertion order)
    const firstKey = this.entityModelCache.keys().next().value;
    if (firstKey) {
      this.entityModelCache.delete(firstKey);
      logger.debug('Entity model evicted from cache', { modelId: firstKey });
    }
  }

  /**
   * Evict oldest entities if cache is full
   */
  private evictEntityCache(): void {
    if (this.entityCache.size <= this.config.maxEntityCacheSize) {
      return;
    }

    // Find entity with oldest lastAccess
    let oldestEntityId: string | null = null;
    let oldestAccess = Date.now();

    for (const [entityId, clientEntity] of this.entityCache.entries()) {
      if (clientEntity.lastAccess < oldestAccess) {
        oldestAccess = clientEntity.lastAccess;
        oldestEntityId = entityId;
      }
    }

    if (oldestEntityId) {
      this.entityCache.delete(oldestEntityId);
      this.entityPathwayCache.delete(oldestEntityId);
      logger.debug('Entity evicted from cache', { entityId: oldestEntityId });
    }
  }

  /**
   * Deserialize entity model from JSON (convert plain objects to Maps)
   */
  private deserializeEntityModel(data: any): EntityModel | null {
    if (!data || typeof data !== 'object') {
      return null;
    }

    return {
      ...data,
      poseMapping: new Map(Object.entries(data.poseMapping || {})),
      modelModifierMapping: new Map(Object.entries(data.modelModifierMapping || {})),
    } as EntityModel;
  }

  /**
   * Start update loop for entity position/state updates
   */
  private startUpdateLoop(): void {
    this.updateInterval = setInterval(() => {
      this.update();
    }, this.config.updateInterval);

    logger.debug('Update loop started', { interval: this.config.updateInterval });
  }

  /**
   * Update all entities (position interpolation, visibility management)
   */
  private update(): void {
    try {
      // Get current time with network lag compensation
      // TODO: Get server lag from PingMessageHandler via NetworkService
      const serverLag = 0; // For now, no lag compensation
      const currentTime = Date.now() + serverLag;

      // Get player position
      const playerService = this.appContext.services.player;
      if (!playerService) {
        return; // Player service not available yet
      }

      const playerPos = playerService.getPosition();

      // Update all entities
      for (const clientEntity of this.entityCache.values()) {
        this.updateEntity_internal(clientEntity, currentTime, playerPos);
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'EntityService.update');
    }
  }

  /**
   * Update single entity (position interpolation and visibility)
   */
  private updateEntity_internal(
    clientEntity: ClientEntity,
    currentTime: number,
    playerPos: { x: number; y: number; z: number }
  ): void {
    // Update lastAccess
    clientEntity.lastAccess = currentTime;

    // Get pathway
    const pathway = this.entityPathwayCache.get(clientEntity.id);
    if (!pathway || pathway.waypoints.length === 0) {
      return;
    }

    // Interpolate position from waypoints
    const result = this.interpolatePosition(pathway, currentTime);
    if (result) {
      // Update entity position/rotation/pose
      clientEntity.currentPosition = result.position;
      clientEntity.currentRotation = result.rotation;
      clientEntity.currentPose = result.pose;
      clientEntity.currentWaypointIndex = result.waypointIndex;

      // Emit transform event
      this.emit('transform', {
        entityId: clientEntity.id,
        position: result.position,
        rotation: result.rotation,
        pose: result.pose,
      });
    }

    // Check visibility based on distance to player
    const distance = Math.sqrt(
      Math.pow(clientEntity.currentPosition.x - playerPos.x, 2) +
      Math.pow(clientEntity.currentPosition.y - playerPos.y, 2) +
      Math.pow(clientEntity.currentPosition.z - playerPos.z, 2)
    );

    const shouldBeVisible = distance <= this._visibilityRadius;

    // Update visibility if changed
    if (clientEntity.visible !== shouldBeVisible) {
      clientEntity.visible = shouldBeVisible;
      this.emit('visibility', { entityId: clientEntity.id, visible: shouldBeVisible });
    }
  }

  /**
   * Interpolate entity position from pathway waypoints
   */
  private interpolatePosition(
    pathway: EntityPathway,
    currentTime: number
  ): {
    position: { x: number; y: number; z: number };
    rotation: { y: number; p: number };
    pose: number;
    waypointIndex: number;
  } | null {
    const waypoints = pathway.waypoints;
    if (waypoints.length === 0) {
      return null;
    }

    // Find current waypoint segment
    let currentIndex = 0;
    for (let i = 0; i < waypoints.length - 1; i++) {
      if (currentTime >= waypoints[i].timestamp && currentTime < waypoints[i + 1].timestamp) {
        currentIndex = i;
        break;
      }
    }

    // If past last waypoint
    if (currentTime >= waypoints[waypoints.length - 1].timestamp) {
      const lastWaypoint = waypoints[waypoints.length - 1];
      return {
        position: { x: lastWaypoint.target.x, y: lastWaypoint.target.y, z: lastWaypoint.target.z },
        rotation: { y: lastWaypoint.rotation.y, p: lastWaypoint.rotation.p ?? 0 },
        pose: lastWaypoint.pose ?? pathway.idlePose ?? 0,
        waypointIndex: waypoints.length - 1,
      };
    }

    // Interpolate between two waypoints
    const from = waypoints[currentIndex];
    const to = waypoints[currentIndex + 1];

    const t = (currentTime - from.timestamp) / (to.timestamp - from.timestamp);
    const clampedT = Math.max(0, Math.min(1, t));

    // Linear interpolation for position
    const position = {
      x: from.target.x + (to.target.x - from.target.x) * clampedT,
      y: from.target.y + (to.target.y - from.target.y) * clampedT,
      z: from.target.z + (to.target.z - from.target.z) * clampedT,
    };

    // Linear interpolation for rotation
    const rotation = {
      y: from.rotation.y + (to.rotation.y - from.rotation.y) * clampedT,
      p: (from.rotation.p ?? 0) + ((to.rotation.p ?? 0) - (from.rotation.p ?? 0)) * clampedT,
    };

    // Use target pose when close to target
    const pose = clampedT > 0.5 ? (to.pose ?? pathway.idlePose ?? 0) : (from.pose ?? pathway.idlePose ?? 0);

    return {
      position,
      rotation,
      pose,
      waypointIndex: currentIndex,
    };
  }

  /**
   * Handle chunk unload - hide all entities in this chunk
   * Prevents "dead" entities from staying in the world
   */
  onChunkUnloaded(chunkX: number, chunkZ: number, chunkSize: number): void {
    try {
      let hiddenCount = 0;

      // Check all entities
      for (const clientEntity of this.entityCache.values()) {
        const pos = clientEntity.currentPosition;

        // Calculate chunk coordinates from entity position
        const entityChunkX = Math.floor(pos.x / chunkSize);
        const entityChunkZ = Math.floor(pos.z / chunkSize);

        // If entity is in the unloaded chunk, hide it
        if (entityChunkX === chunkX && entityChunkZ === chunkZ) {
          if (clientEntity.visible) {
            clientEntity.visible = false;
            this.emit('visibility', { entityId: clientEntity.id, visible: false });
            hiddenCount++;
          }
        }
      }

      if (hiddenCount > 0) {
        logger.debug('Entities hidden due to chunk unload', {
          chunkX,
          chunkZ,
          hiddenCount,
        });
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'EntityService.onChunkUnloaded', { chunkX, chunkZ });
    }
  }

  /**
   * Register event listener
   */
  on(event: EntityEventType, listener: EntityEventListener): void {
    if (!this.eventListeners.has(event)) {
      this.eventListeners.set(event, new Set());
    }
    this.eventListeners.get(event)!.add(listener);
  }

  /**
   * Unregister event listener
   */
  off(event: EntityEventType, listener: EntityEventListener): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      listeners.delete(listener);
    }
  }

  /**
   * Emit event to all listeners
   */
  private emit(event: EntityEventType, data: any): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      for (const listener of listeners) {
        try {
          listener(data);
        } catch (error) {
          ExceptionHandler.handle(error, 'EntityService.emit', { event, data });
        }
      }
    }
  }
}
