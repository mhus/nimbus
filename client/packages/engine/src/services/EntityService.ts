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
  private networkService: NetworkService;
  private config: Required<EntityServiceConfig>;

  // Caches
  private entityModelCache: Map<string, EntityModel> = new Map();
  private entityCache: Map<string, ClientEntity> = new Map();
  private entityPathwayCache: Map<string, EntityPathway> = new Map(); // entityId -> pathway

  // Cache cleanup
  private cleanupInterval?: NodeJS.Timeout;

  constructor(appContext: AppContext, config?: EntityServiceConfig) {
    if (!appContext.services.network) {
      throw new Error('NetworkService is required for EntityService');
    }

    this.networkService = appContext.services.network;

    // Default configuration
    this.config = {
      maxModelCacheSize: config?.maxModelCacheSize ?? 100,
      maxEntityCacheSize: config?.maxEntityCacheSize ?? 1000,
      cacheEvictionTimeout: config?.cacheEvictionTimeout ?? 300000, // 5 minutes
      cacheCleanupInterval: config?.cacheCleanupInterval ?? 60000, // 1 minute
    };

    logger.info('EntityService initialized', { config: this.config });

    // Start cache cleanup
    this.startCacheCleanup();
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
      logger.debug('Loading entity model from API', { modelId });
      const url = `${this.networkService.getApiUrl()}/entity-models/${modelId}`;
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

      logger.info('Entity model loaded', { modelId });
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
      logger.debug('Loading entity from API', { entityId });
      const url = `${this.networkService.getApiUrl()}/entities/${entityId}`;
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

      logger.info('Entity loaded', { entityId });
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
   */
  setEntityPathway(pathway: EntityPathway): void {
    this.entityPathwayCache.set(pathway.entityId, pathway);

    // Update ClientEntity with waypoints
    const clientEntity = this.entityCache.get(pathway.entityId);
    if (clientEntity) {
      clientEntity.currentWaypoints = pathway.waypoints;
      clientEntity.currentWaypointIndex = 0;
      clientEntity.lastAccess = Date.now();
      logger.debug('Entity pathway set', { entityId: pathway.entityId, waypointCount: pathway.waypoints.length });
    }
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
   * Dispose service (stop cleanup interval)
   */
  dispose(): void {
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
}
