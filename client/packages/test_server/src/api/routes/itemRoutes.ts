import { Router } from 'express';
import type { ItemData } from '@nimbus/shared';
import type { WorldManager } from '../../world/WorldManager';

/**
 * Item routes for REST API
 *
 * Provides endpoints for:
 * - Searching items
 * - Getting item data
 * - Creating/updating/deleting items
 */
export function createItemRoutes(worldManager: WorldManager): Router {
  const router = Router();

  /**
   * GET /api/worlds/:worldId/items?query={searchTerm}
   * Search for items (max 100 results)
   */
  router.get('/:worldId/items', (req, res) => {
    const worldId = req.params.worldId;
    const query = (req.query.query as string || '').toLowerCase();
    const maxResults = 100;

    const world = worldManager.getWorld(worldId);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const allItems = world.itemRegistry.getAllItemData();
    let results: Array<{ itemId: string; name: string; texture?: string }> = [];

    // Search through all items
    for (const itemData of allItems) {
      const itemId = itemData.block.metadata?.id || 'unknown';

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
    const worldId = req.params.worldId;
    const itemId = req.params.itemId;

    const world = worldManager.getWorld(worldId);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    // Find item by metadata.id
    const allItems = world.itemRegistry.getAllItemData();
    const itemData = allItems.find(item => item.block.metadata?.id === itemId);

    if (!itemData) {
      console.log(`[ItemRoutes] Item not found: ${itemId}`);
      return res.status(404).json({ error: 'Item not found' });
    }

    console.log(`[ItemRoutes] Returning item: ${itemId}`);
    return res.json(itemData);
  });

  /**
   * POST /api/worlds/:worldId/items
   * Create a new item
   */
  router.post('/:worldId/items', (req, res) => {
    const worldId = req.params.worldId;
    const { itemData }: { itemData: ItemData } = req.body;

    if (!itemData?.block) {
      return res.status(400).json({ error: 'Invalid item data: block required' });
    }

    const world = worldManager.getWorld(worldId);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const { position } = itemData.block;
    const displayName = itemData.block.metadata?.displayName || 'New Item';
    const texturePath = extractTexturePath(itemData) || 'items/default.png';

    // Use ItemRegistry.addItem with proper signature
    const createdBlock = world.itemRegistry.addItem(
      position.x,
      position.y,
      position.z,
      displayName,
      texturePath,
      itemData.parameters
    );

    const createdItemId = createdBlock.metadata?.id || 'unknown';

    return res.status(201).json({ itemId: createdItemId });
  });

  /**
   * PUT /api/worlds/:worldId/item/:itemId
   * Update an existing item
   */
  router.put('/:worldId/item/:itemId', (req, res) => {
    const worldId = req.params.worldId;
    const itemId = req.params.itemId;
    const itemData: ItemData = req.body;

    const world = worldManager.getWorld(worldId);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    // Find existing item
    const allItems = world.itemRegistry.getAllItemData();
    const existingItem = allItems.find(item => item.block.metadata?.id === itemId);

    if (!existingItem) {
      return res.status(404).json({ error: 'Item not found' });
    }

    const { position } = itemData.block;
    const displayName = itemData.block.metadata?.displayName || 'Updated Item';
    const texturePath = extractTexturePath(itemData) || 'items/default.png';

    // Remove old item
    world.itemRegistry.removeItem(
      existingItem.block.position.x,
      existingItem.block.position.y,
      existingItem.block.position.z
    );

    // Add updated item (will generate new ID if position changed)
    world.itemRegistry.addItem(
      position.x,
      position.y,
      position.z,
      displayName,
      texturePath,
      { ...itemData.parameters, ...itemData }
    );

    return res.json(itemData);
  });

  /**
   * DELETE /api/worlds/:worldId/item/:itemId
   * Delete an item
   */
  router.delete('/:worldId/item/:itemId', (req, res) => {
    const worldId = req.params.worldId;
    const itemId = req.params.itemId;

    const world = worldManager.getWorld(worldId);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    // Find and remove item
    const allItems = world.itemRegistry.getAllItemData();
    const itemData = allItems.find(item => item.block.metadata?.id === itemId);

    if (!itemData) {
      return res.status(404).json({ error: 'Item not found' });
    }

    world.itemRegistry.removeItem(itemData.block.position.x, itemData.block.position.y, itemData.block.position.z);

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
