import { Router } from 'express';
import type { Item } from '@nimbus/shared';
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

    const allItems = world.itemRegistry.getAllItems();
    let results: Array<{ itemId: string; name: string; texture?: string }> = [];

    // Search through all items
    for (const item of allItems) {
      // Match query against itemId, name, or description
      const matchesQuery =
        !query ||
        item.item.id.toLowerCase().includes(query) ||
        (item.item.name?.toLowerCase().includes(query)) ||
        (item.item.description?.toLowerCase().includes(query));

      if (matchesQuery) {
        // Extract texture path if available
        const texture = item.item.modifier?.texture;

        results.push({
          itemId: item.item.id,
          name: item.item.name || item.item.id,
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

    // Find item by id
    const item = world.itemRegistry.getItemById(itemId);

    if (!item) {
      console.log(`[ItemRoutes] Item not found: ${itemId}`);
      return res.status(404).json({ error: 'Item not found' });
    }

    console.log(`[ItemRoutes] Returning item: ${itemId}`);
    return res.json(item);
  });

  /**
   * POST /api/worlds/:worldId/items
   * Create a new item
   */
  router.post('/:worldId/items', (req, res) => {
    const worldId = req.params.worldId;
    const { item }: { item: Item } = req.body;

    const world = worldManager.getWorld(worldId);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const displayName = item.name || 'New Item';
    const itemType = item.itemType || 'sword'; // Default if not provided

    // Texture is optional - will use ItemType default if not provided
    const textureOverride = item.modifier?.texture;

    // Use ItemRegistry.addItem
    const createdItem = world.itemRegistry.addItem(
      displayName,
      itemType,
      undefined,
      textureOverride,
      item.parameters
    );

    return res.status(201).json({ itemId: createdItem.item.id });
  });

  /**
   * PUT /api/worlds/:worldId/item/:itemId
   * Update an existing item
   */
  router.put('/:worldId/item/:itemId', (req, res) => {
    const worldId = req.params.worldId;
    const itemId = req.params.itemId;
    const item: Item = req.body;

    const world = worldManager.getWorld(worldId);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    // Find existing item
    const existingItem = world.itemRegistry.getItemById(itemId);

    if (!existingItem) {
      return res.status(404).json({ error: 'Item not found' });
    }

    const displayName = item.name || 'Updated Item';

    // Remove old item
    world.itemRegistry.removeItem(
      existingItem.item.id
    );

    // Get itemType from request
    const itemType = item.itemType || existingItem.item.itemType || 'sword';

    // Texture is optional - will use ItemType default if not provided
    const textureOverride = item.modifier?.texture;

    // Add updated item (will generate new ID if position changed)
    const updatedItem = world.itemRegistry.addItem(
      displayName,
      itemType,
      undefined,
      textureOverride,
      item.parameters
    );

    return res.json(updatedItem);
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
    const item = world.itemRegistry.getItemById(itemId);

    if (!item) {
      return res.status(404).json({ error: 'Item not found' });
    }

    world.itemRegistry.removeItem(item.item.id);

    return res.status(204).send();
  });

  return router;
}
