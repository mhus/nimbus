import { Router } from 'express';
import type { EntityManager } from '../../entity/EntityManager';
import { EntitySerializer } from '@nimbus/shared';

export function createEntityRoutes(entityManager: EntityManager): Router {
  const router = Router();

  // GET /api/worlds/:worldId/entitymodel/:modelId - Get entity model by ID
  router.get('/:worldId/entitymodel/:modelId', (req, res) => {
    const model = entityManager.getEntityModel(req.params.modelId);

    if (!model) {
      return res.status(404).json({ error: 'Entity model not found' });
    }

    // Serialize Maps to plain objects for JSON response
    const serializedModel = {
      ...model,
      poseMapping: Object.fromEntries(model.poseMapping),
      modelModifierMapping: Object.fromEntries(model.modelModifierMapping),
    };

    return res.json(serializedModel);
  });

  // GET /api/worlds/:worldId/entitymodels - Get all entity models
  router.get('/:worldId/entitymodels', (_req, res) => {
    const models = entityManager.getAllEntityModels();

    // Serialize all models
    const serializedModels = models.map(model => ({
      ...model,
      poseMapping: Object.fromEntries(model.poseMapping),
      modelModifierMapping: Object.fromEntries(model.modelModifierMapping),
    }));

    return res.json(serializedModels);
  });

  // GET /api/worlds/:worldId/entity/:entityId - Get entity by ID
  router.get('/:worldId/entity/:entityId', (req, res) => {
    const entity = entityManager.getEntity(req.params.entityId);

    if (!entity) {
      return res.status(404).json({ error: 'Entity not found' });
    }

    return res.json(entity);
  });

  // GET /api/worlds/:worldId/entities - Get all entities
  router.get('/:worldId/entities', (_req, res) => {
    const entities = entityManager.getAllEntities();
    return res.json(entities);
  });

  // POST /api/worlds/:worldId/entity - Create new entity
  router.post('/:worldId/entity', (req, res) => {
    try {
      const entityData = req.body;

      // Validate required fields
      if (!entityData.id) {
        return res.status(400).json({ error: 'Entity ID is required' });
      }

      if (!entityData.model) {
        return res.status(400).json({ error: 'Entity model reference is required' });
      }

      // Verify entity model exists
      const entityModel = entityManager.getEntityModel(entityData.model);
      if (!entityModel) {
        return res.status(400).json({ error: `Entity model '${entityData.model}' not found` });
      }

      // Check if entity already exists
      const existingEntity = entityManager.getEntity(entityData.id);
      if (existingEntity) {
        return res.status(409).json({ error: `Entity with ID '${entityData.id}' already exists` });
      }

      // Add entity
      entityManager.addEntity(entityData);

      return res.status(201).json(entityData);
    } catch (error) {
      return res.status(500).json({
        error: 'Failed to create entity',
        message: error instanceof Error ? error.message : 'Unknown error',
      });
    }
  });

  // PUT /api/worlds/:worldId/entity/:entityId - Update entity
  router.put('/:worldId/entity/:entityId', (req, res) => {
    try {
      const entityId = req.params.entityId;
      const entityData = req.body;

      // Check if entity exists
      const existingEntity = entityManager.getEntity(entityId);
      if (!existingEntity) {
        return res.status(404).json({ error: 'Entity not found' });
      }

      // Ensure ID matches URL parameter
      entityData.id = entityId;

      // If model is being changed, verify it exists
      if (entityData.model) {
        const entityModel = entityManager.getEntityModel(entityData.model);
        if (!entityModel) {
          return res.status(400).json({ error: `Entity model '${entityData.model}' not found` });
        }
      }

      // Update entity
      entityManager.updateEntity(entityData);

      return res.json(entityData);
    } catch (error) {
      return res.status(500).json({
        error: 'Failed to update entity',
        message: error instanceof Error ? error.message : 'Unknown error',
      });
    }
  });

  // DELETE /api/worlds/:worldId/entity/:entityId - Delete entity
  router.delete('/:worldId/entity/:entityId', (req, res) => {
    const entityId = req.params.entityId;

    // Check if entity exists
    const existingEntity = entityManager.getEntity(entityId);
    if (!existingEntity) {
      return res.status(404).json({ error: 'Entity not found' });
    }

    // Remove entity
    entityManager.removeEntity(entityId);

    return res.status(204).send();
  });

  return router;
}
