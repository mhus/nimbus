import { Router } from 'express';
import type { ItemData } from '@nimbus/shared';

/**
 * Item routes for REST API
 *
 * Provides endpoints for:
 * - Searching items
 * - Getting item data
 * - Creating/updating/deleting items (future)
 */
export function createItemRoutes(): Router {
  const router = Router();

  // In-memory item storage (for testing)
  // In production, this would be a database
  const items = new Map<string, ItemData>();

  // Initialize with example items
  initializeExampleItems(items);

  /**
   * GET /api/worlds/:worldId/items?query={searchTerm}
   * Search for items (max 100 results)
   */
  router.get('/:worldId/items', (req, res) => {
    const query = (req.query.query as string || '').toLowerCase();
    const maxResults = 100;

    let results: Array<{ itemId: string; name: string; texture?: string }> = [];

    // Search through all items
    for (const [itemId, itemData] of items.entries()) {
      // Match query against itemId, description, or block display name
      const matchesQuery =
        !query ||
        itemId.toLowerCase().includes(query) ||
        (itemData.description?.toLowerCase().includes(query)) ||
        (itemData.block.metadata?.displayName?.toLowerCase().includes(query));

      if (matchesQuery) {
        // Extract texture path if available
        const texture = extractTexturePath(itemData);

        results.push({
          itemId,
          name: itemData.block.metadata?.displayName || itemId,
          texture,
        });

        if (results.length >= maxResults) {
          break;
        }
      }
    }

    return res.json({ items: results });
  });

  /**
   * GET /api/worlds/:worldId/item/:itemId
   * Get full item data
   */
  router.get('/:worldId/item/:itemId', (req, res) => {
    const itemId = req.params.itemId;
    const itemData = items.get(itemId);

    if (!itemData) {
      return res.status(404).json({ error: 'Item not found' });
    }

    return res.json(itemData);
  });

  /**
   * POST /api/worlds/:worldId/items
   * Create a new item
   */
  router.post('/:worldId/items', (req, res) => {
    const { itemId, itemData }: { itemId: string; itemData: ItemData } = req.body;

    if (!itemId || !itemData.block) {
      return res.status(400).json({ error: 'Invalid item data: itemId and block required' });
    }

    if (items.has(itemId)) {
      return res.status(400).json({ error: 'Item already exists' });
    }

    items.set(itemId, itemData);
    return res.status(201).json({ itemId });
  });

  /**
   * PUT /api/worlds/:worldId/item/:itemId
   * Update an existing item
   */
  router.put('/:worldId/item/:itemId', (req, res) => {
    const itemId = req.params.itemId;
    const itemData: ItemData = req.body;

    if (!items.has(itemId)) {
      return res.status(404).json({ error: 'Item not found' });
    }

    items.set(itemId, itemData);
    return res.json(itemData);
  });

  /**
   * DELETE /api/worlds/:worldId/item/:itemId
   * Delete an item
   */
  router.delete('/:worldId/item/:itemId', (req, res) => {
    const itemId = req.params.itemId;

    if (!items.has(itemId)) {
      return res.status(404).json({ error: 'Item not found' });
    }

    items.delete(itemId);
    return res.status(204).send();
  });

  return router;
}

/**
 * Extract texture path from item data
 */
function extractTexturePath(itemData: ItemData): string | undefined {
  const modifier = itemData.block.modifiers?.[itemData.block.status || 0];
  if (!modifier) return undefined;

  const textures = modifier.visibility?.textures;
  if (!textures || typeof textures !== 'object') return undefined;

  // Get first available texture from the textures record
  const textureValues = Object.values(textures);
  if (textureValues.length === 0) return undefined;

  const firstTexture = textureValues[0];
  return typeof firstTexture === 'string' ? firstTexture : (firstTexture as any)?.path;
}

/**
 * Initialize example items for testing
 */
function initializeExampleItems(items: Map<string, ItemData>): void {
  // Health Potion
  items.set('health_potion', {
    block: {
      position: { x: 0, y: 0, z: 0 },
      blockTypeId: 1000,
      status: 0,
      metadata: {
        displayName: 'Health Potion',
      },
    },
    description: 'Restores 50 health points',
    pose: 'use',
    wait: 500,
    duration: 1000,
    onUseEffect: {
      script: {
        id: 'health_potion_effect',
        root: {
          kind: 'Sequence',
          steps: [
            {
              kind: 'Cmd',
              cmd: 'notification',
              parameters: [0, 'System', 'Health restored!'],
            },
            {
              kind: 'Cmd',
              cmd: 'playSound',
              parameters: ['audio/items/potion_drink.ogg', 0.8],
            },
          ],
        },
      },
    },
  });

  // Explosion Wand
  items.set('explosion_wand', {
    block: {
      position: { x: 0, y: 0, z: 0 },
      blockTypeId: 1001,
      status: 0,
      metadata: {
        displayName: 'Explosion Wand',
      },
    },
    description: 'Creates a powerful explosion',
    pose: 'attack',
    wait: 1000,
    duration: 2000,
    onUseEffect: {
      script: {
        id: 'explosion_wand_effect',
        root: {
          kind: 'Sequence',
          steps: [
            {
              kind: 'Cmd',
              cmd: 'centerText',
              parameters: ['Charging...', 1000],
            },
            {
              kind: 'Wait',
              seconds: 1,
            },
            {
              kind: 'Parallel',
              steps: [
                {
                  kind: 'Cmd',
                  cmd: 'notification',
                  parameters: [20, 'null', 'BOOM!'],
                },
                {
                  kind: 'Cmd',
                  cmd: 'flashImage',
                  parameters: ['effects/explosion_flash.png', 500],
                },
                {
                  kind: 'Cmd',
                  cmd: 'playSoundAtPosition',
                  parameters: ['audio/effects/explosion.ogg', 0, 64, 0, 1.0],
                },
              ],
            },
          ],
        },
      },
    },
  });

  // Teleport Scroll
  items.set('teleport_scroll', {
    block: {
      position: { x: 0, y: 0, z: 0 },
      blockTypeId: 1002,
      status: 0,
      metadata: {
        displayName: 'Teleport Scroll',
      },
    },
    description: 'Teleports you to spawn',
    pose: 'use',
    wait: 0,
    duration: 3000,
    onUseEffect: {
      scriptId: 'teleport_to_spawn',
      parameters: {
        x: 0,
        y: 100,
        z: 0,
      },
    },
  });

  // Simple Sword (no effect, just pose)
  items.set('iron_sword', {
    block: {
      position: { x: 0, y: 0, z: 0 },
      blockTypeId: 1003,
      status: 0,
      metadata: {
        displayName: 'Iron Sword',
      },
    },
    description: 'A simple iron sword',
    pose: 'attack',
    duration: 500,
  });
}
