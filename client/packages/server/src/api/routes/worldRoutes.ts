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

  return router;
}
