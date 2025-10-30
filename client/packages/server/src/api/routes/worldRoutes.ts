import { Router } from 'express';
import type { WorldManager } from '../../world/WorldManager';

export function createWorldRoutes(worldManager: WorldManager): Router {
  const router = Router();

  // GET /api/worlds - List all worlds
  router.get('/', (_req, res) => {
    const worlds = worldManager.getAllWorlds();
    res.json(worlds.map(w => ({
      worldId: w.worldId,
      name: w.name,
      description: w.description,
      chunkSize: w.chunkSize,
      status: w.status,
    })));
  });

  // GET /api/worlds/:id - Get world details
  router.get('/:id', (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }
    return res.json(world);
  });

  // GET /api/worlds/:id/blocktypes - Get all BlockTypes or search
  router.get('/:id/blocktypes', (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const query = req.query.query as string | undefined;
    const registry = worldManager.getBlockTypeRegistry();

    if (query) {
      // Search BlockTypes
      const blockTypes = registry.searchBlockTypes(query);
      return res.json({ blockTypes });
    } else {
      // Get all BlockTypes
      const blockTypes = registry.getAllBlockTypes();
      return res.json({ blockTypes });
    }
  });

  // GET /api/worlds/:id/blocktypes/:blockId - Get single BlockType
  router.get('/:id/blocktypes/:blockId', (req, res) => {
    const blockType = worldManager.getBlockTypeRegistry().getBlockType(Number(req.params.blockId));
    if (!blockType) {
      return res.status(404).json({ error: 'BlockType not found' });
    }
    return res.json(blockType);
  });

  // GET /api/worlds/:id/blocktypes/:from/:to - Get BlockType range
  router.get('/:id/blocktypes/:from/:to', (req, res) => {
    const from = Number(req.params.from);
    const to = Number(req.params.to);
    const blockTypes = worldManager.getBlockTypeRegistry().getBlockTypeRange(from, to);
    res.json(blockTypes);
  });

  // POST /api/worlds/:id/blocktypes - Create new BlockType
  router.post('/:id/blocktypes', (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const blockTypeData = req.body;
    const registry = worldManager.getBlockTypeRegistry();

    // If no ID provided, get next available
    if (!blockTypeData.id) {
      blockTypeData.id = registry.getNextAvailableId();
    }

    // Validate required fields
    if (!blockTypeData.modifiers || Object.keys(blockTypeData.modifiers).length === 0) {
      return res.status(400).json({ error: 'BlockType must have at least one modifier (status 0)' });
    }

    // Create BlockType
    const createdBlockType = registry.createBlockType(blockTypeData);

    if (!createdBlockType) {
      return res.status(400).json({ error: 'Failed to create BlockType (may already exist)' });
    }

    return res.status(201).json({ id: createdBlockType.id });
  });

  // PUT /api/worlds/:id/blocktypes/:blockTypeId - Update BlockType
  router.put('/:id/blocktypes/:blockTypeId', (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const blockTypeId = Number(req.params.blockTypeId);
    const blockTypeData = req.body;
    const registry = worldManager.getBlockTypeRegistry();

    // Ensure ID matches URL parameter
    blockTypeData.id = blockTypeId;

    // Validate required fields
    if (!blockTypeData.modifiers || Object.keys(blockTypeData.modifiers).length === 0) {
      return res.status(400).json({ error: 'BlockType must have at least one modifier (status 0)' });
    }

    // Update BlockType
    const updatedBlockType = registry.updateBlockType(blockTypeData);

    if (!updatedBlockType) {
      return res.status(404).json({ error: 'BlockType not found' });
    }

    return res.json(updatedBlockType);
  });

  // DELETE /api/worlds/:id/blocktypes/:blockTypeId - Delete BlockType
  router.delete('/:id/blocktypes/:blockTypeId', (req, res) => {
    const world = worldManager.getWorld(req.params.id);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const blockTypeId = Number(req.params.blockTypeId);
    const registry = worldManager.getBlockTypeRegistry();

    // Delete BlockType
    const deleted = registry.deleteBlockType(blockTypeId);

    if (!deleted) {
      return res.status(404).json({ error: 'BlockType not found' });
    }

    return res.status(204).send();
  });

  return router;
}
