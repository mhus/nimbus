/**
 * EntityManager - Manages entities and entity models
 *
 * Features:
 * - Load entity models from files/entitymodels/
 * - Keep entities in memory
 * - Provide REST API access
 */

import * as fs from 'fs';
import * as path from 'path';
import { EntityModel, Entity, getLogger, ExceptionHandler } from '@nimbus/shared';

const logger = getLogger('EntityManager');

/**
 * EntityManager - Server-side entity management
 */
export class EntityManager {
  private entityModels: Map<string, EntityModel> = new Map();
  private entities: Map<string, Entity> = new Map();

  private entityModelsPath: string;
  private worldDataPath: string;

  constructor(dataPath: string, worldId: string) {
    // EntityModels are in files/entitymodels (sibling to data/)
    this.entityModelsPath = path.join('files', 'entitymodels');
    // World data is in data/worlds/{worldId}
    this.worldDataPath = path.join(dataPath, 'worlds', worldId);

    logger.info('EntityManager initialized', {
      entityModelsPath: this.entityModelsPath,
      worldDataPath: this.worldDataPath,
    });

    // Create directories if they don't exist
    this.ensureDirectories();

    // Note: Entity models are now loaded lazily on request, not at startup
  }

  /**
   * Ensure required directories exist
   */
  private ensureDirectories(): void {
    try {
      if (!fs.existsSync(this.entityModelsPath)) {
        fs.mkdirSync(this.entityModelsPath, { recursive: true });
        logger.info('Created entity models directory', { path: this.entityModelsPath });
      }

      const entitiesPath = path.join(this.worldDataPath, 'entities');
      if (!fs.existsSync(entitiesPath)) {
        fs.mkdirSync(entitiesPath, { recursive: true });
        logger.info('Created entities directory', { path: entitiesPath });
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'EntityManager.ensureDirectories');
    }
  }

  /**
   * Load entity model from file (lazy loading)
   */
  private loadEntityModelFromFile(modelId: string): EntityModel | null {
    try {
      const filePath = path.join(this.entityModelsPath, `${modelId}.json`);

      if (!fs.existsSync(filePath)) {
        logger.debug('Entity model file not found', { modelId, filePath });
        return null;
      }

      const data = fs.readFileSync(filePath, 'utf-8');
      const modelData = JSON.parse(data);

      // Deserialize Maps from JSON
      const model: EntityModel = {
        ...modelData,
        poseMapping: new Map(Object.entries(modelData.poseMapping || {})),
        modelModifierMapping: new Map(Object.entries(modelData.modelModifierMapping || {})),
      };

      logger.debug('Entity model loaded from file', { modelId, filePath });
      return model;
    } catch (error) {
      ExceptionHandler.handle(error, 'EntityManager.loadEntityModelFromFile', { modelId });
      return null;
    }
  }

  /**
   * Get entity model by ID (lazy loading)
   */
  getEntityModel(modelId: string): EntityModel | null {
    // Check cache first
    const cached = this.entityModels.get(modelId);
    if (cached) {
      return cached;
    }

    // Load from file
    const model = this.loadEntityModelFromFile(modelId);
    if (model) {
      // Cache for future requests
      this.entityModels.set(modelId, model);
      return model;
    }

    return null;
  }

  /**
   * Get all entity models
   */
  getAllEntityModels(): EntityModel[] {
    return Array.from(this.entityModels.values());
  }

  /**
   * Add entity to memory
   */
  addEntity(entity: Entity): void {
    this.entities.set(entity.id, entity);
    logger.debug('Entity added', { entityId: entity.id });
  }

  /**
   * Get entity by ID
   */
  getEntity(entityId: string): Entity | null {
    return this.entities.get(entityId) || null;
  }

  /**
   * Get all entities
   */
  getAllEntities(): Entity[] {
    return Array.from(this.entities.values());
  }

  /**
   * Update entity
   */
  updateEntity(entity: Entity): void {
    this.entities.set(entity.id, entity);
    logger.debug('Entity updated', { entityId: entity.id });
  }

  /**
   * Remove entity
   */
  removeEntity(entityId: string): void {
    this.entities.delete(entityId);
    logger.debug('Entity removed', { entityId });
  }

  /**
   * Save entity model to file
   */
  saveEntityModel(model: EntityModel): void {
    try {
      const filePath = path.join(this.entityModelsPath, `${model.id}.json`);

      // Serialize Maps to plain objects
      const data = {
        ...model,
        poseMapping: Object.fromEntries(model.poseMapping),
        modelModifierMapping: Object.fromEntries(model.modelModifierMapping),
      };

      fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf-8');

      this.entityModels.set(model.id, model);
      logger.info('Entity model saved', { modelId: model.id });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'EntityManager.saveEntityModel', {
        modelId: model.id,
      });
    }
  }

  /**
   * Get entity models path
   */
  getEntityModelsPath(): string {
    return this.entityModelsPath;
  }

  /**
   * Get entities path for current world
   */
  getEntitiesPath(): string {
    return path.join(this.worldDataPath, 'entities');
  }
}
