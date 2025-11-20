import { Router } from 'express';
import type { Item, Vector3 } from '@nimbus/shared';
import type { WorldManager } from '../../world/WorldManager';
import type { ItemUpdateBuffer } from '../../network/ItemUpdateBuffer';

/**
 * Item routes for REST API
 *
 * Provides endpoints for:
 * - Searching items
 * - Getting item data
 * - Creating/updating/deleting items
 */
export function createItemRoutes(worldManager: WorldManager, itemUpdateBuffer: ItemUpdateBuffer): Router {
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
   * Create a new item (with or without position)
   *
   * Body: {
   *   name: string,
   *   itemType: string,
   *   position?: { x: number, y: number, z: number },
   *   texture?: string,
   *   parameters?: Record<string, any>
   * }
   */
  router.post('/:worldId/items', (req, res) => {
    const worldId = req.params.worldId;
    const { name, itemType, position, texture, parameters } = req.body;

    const world = worldManager.getWorld(worldId);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    // Validate required fields
    if (!name || !itemType) {
      return res.status(400).json({ error: 'name and itemType are required' });
    }

    try {
      // Create item with or without position
      const createdItem = world.itemRegistry.addItem(
        name,
        itemType,
        position,
        texture,
        parameters
      );

      // Queue item update for broadcast (only if item has position)
      if (createdItem.itemBlockRef) {
        itemUpdateBuffer.addUpdate(worldId, createdItem.itemBlockRef);
      }

      console.log(`[ItemRoutes] Item created: ${createdItem.item.id}`, { hasPosition: !!position });
      return res.status(201).json(createdItem);
    } catch (error) {
      console.error('[ItemRoutes] Failed to create item:', error);
      return res.status(500).json({ error: (error as Error).message });
    }
  });

  /**
   * PUT /api/worlds/:worldId/item/:itemId
   * Update an existing item
   *
   * Body: {
   *   name?: string,
   *   itemType?: string,
   *   position?: { x: number, y: number, z: number } | null,
   *   texture?: string,
   *   parameters?: Record<string, any>
   * }
   */
  router.put('/:worldId/item/:itemId', (req, res) => {
    const worldId = req.params.worldId;
    const itemId = req.params.itemId;
    const { name, itemType, position, texture, parameters } = req.body;

    const world = worldManager.getWorld(worldId);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    // Find existing item
    const existingItem = world.itemRegistry.getItemById(itemId);

    if (!existingItem) {
      return res.status(404).json({ error: 'Item not found' });
    }

    try {
      const hadPosition = !!existingItem.itemBlockRef;
      const oldPosition = existingItem.itemBlockRef?.position;

      // Remove old item
      world.itemRegistry.removeItem(itemId);

      // If item had a position and now doesn't (position = null), send delete marker
      if (hadPosition && position === null) {
        const deleteMarker: any = {
          id: itemId,
          texture: '__deleted__',
          position: oldPosition,
        };
        itemUpdateBuffer.addUpdate(worldId, deleteMarker);
      }

      // Merge updates with existing data
      const updatedName = name ?? existingItem.item.name;
      const updatedItemType = itemType ?? existingItem.item.itemType;
      const updatedTexture = texture ?? existingItem.item.parameters?.texture;
      const updatedParameters = parameters ?? existingItem.item.parameters;
      const updatedPosition = position === null ? undefined : (position ?? oldPosition);

      // Add updated item
      const updatedItem = world.itemRegistry.addItem(
        updatedName,
        updatedItemType,
        updatedPosition,
        updatedTexture,
        updatedParameters
      );

      // Queue item update for broadcast (only if item has position)
      if (updatedItem.itemBlockRef) {
        itemUpdateBuffer.addUpdate(worldId, updatedItem.itemBlockRef);
      }

      console.log(`[ItemRoutes] Item updated: ${itemId}`, { hasPosition: !!updatedPosition });
      return res.json(updatedItem);
    } catch (error) {
      console.error('[ItemRoutes] Failed to update item:', error);
      return res.status(500).json({ error: (error as Error).message });
    }
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

    // Find item
    const item = world.itemRegistry.getItemById(itemId);

    if (!item) {
      return res.status(404).json({ error: 'Item not found' });
    }

    // If item has position, send delete marker to clients
    if (item.itemBlockRef) {
      const deleteMarker: any = {
        id: item.item.id,
        texture: '__deleted__',
        position: item.itemBlockRef.position,
      };
      itemUpdateBuffer.addUpdate(worldId, deleteMarker);
    }

    // Remove from registry
    world.itemRegistry.removeItem(item.item.id);

    console.log(`[ItemRoutes] Item deleted: ${itemId}`, { hadPosition: !!item.itemBlockRef });
    return res.status(204).send();
  });

  return router;
}
