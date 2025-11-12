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

    // Load entity models
    this.loadEntityModels();
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
   * Load all entity models from files/entitymodels/
   */
  private loadEntityModels(): void {
    try {
      if (!fs.existsSync(this.entityModelsPath)) {
        logger.warn('Entity models directory does not exist', { path: this.entityModelsPath });
        return;
      }

      const files = fs.readdirSync(this.entityModelsPath);
      let loadedCount = 0;

      for (const file of files) {
        if (!file.endsWith('.json')) {
          continue;
        }

        const filePath = path.join(this.entityModelsPath, file);
        const modelId = path.basename(file, '.json');

        try {
          const data = fs.readFileSync(filePath, 'utf-8');
          const modelData = JSON.parse(data);

          // Deserialize Maps from JSON
          const model: EntityModel = {
            ...modelData,
            poseMapping: new Map(Object.entries(modelData.poseMapping || {})),
            modelModifierMapping: new Map(Object.entries(modelData.modelModifierMapping || {})),
          };

          this.entityModels.set(modelId, model);
          loadedCount++;
          logger.debug('Loaded entity model', { modelId, file });
        } catch (error) {
          ExceptionHandler.handle(error, 'EntityManager.loadEntityModels.file', { file });
        }
      }

      logger.info('Entity models loaded', { count: loadedCount });
    } catch (error) {
      ExceptionHandler.handle(error, 'EntityManager.loadEntityModels');
    }
  }

  /**
   * Get entity model by ID
   */
  getEntityModel(modelId: string): EntityModel | null {
    return this.entityModels.get(modelId) || null;
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
