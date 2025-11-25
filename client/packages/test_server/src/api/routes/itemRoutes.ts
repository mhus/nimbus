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
   * Body: Complete Item object
   */
  router.post('/:worldId/items', (req, res) => {
    const worldId = req.params.worldId;
    const item: Item = req.body;

    const world = worldManager.getWorld(worldId);
    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    // Validate required fields
    if (!item.name || !item.itemType) {
      return res.status(400).json({ error: 'name and itemType are required' });
    }

    try {
      // Create Item directly and save to registry
      const newItem: Item = {
        id: item.id || `item_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
        itemType: item.itemType,
        name: item.name,
        description: item.description,
        modifier: item.modifier,
        parameters: item.parameters,
      };

      // Add to registry with specified ID
      const serverItem = world.itemRegistry.addItem(
        newItem.name || 'Item',
        newItem.itemType!,
        undefined, // No position from this API call
        newItem.modifier?.texture,
        newItem.parameters,
        newItem.id // Pass ID to use existing ID
      );

      // Set the full modifier including onUseEffect
      serverItem.item.modifier = newItem.modifier;
      serverItem.item.description = newItem.description;

      // Save to disk
      world.itemRegistry.save();

      console.log(`[ItemRoutes] Item created: ${serverItem.item.id}`, {
        hasModifier: !!newItem.modifier,
        hasOnUseEffect: !!newItem.modifier?.onUseEffect,
      });

      return res.status(201).json(serverItem);
    } catch (error) {
      console.error('[ItemRoutes] Failed to create item:', error);
      return res.status(500).json({ error: (error as Error).message });
    }
  });

  /**
   * PUT /api/worlds/:worldId/item/:itemId
   * Update an existing item
   *
   * Body: Complete or partial Item object
   */
  router.put('/:worldId/item/:itemId', (req, res) => {
    const worldId = req.params.worldId;
    const itemId = req.params.itemId;
    const updates: Partial<Item> = req.body;

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
      // Merge updates with existing item
      const updatedItem: Item = {
        ...existingItem.item,
        ...updates,
        id: itemId, // Ensure ID stays the same
      };

      // Merge modifiers carefully (shallow merge)
      if (updates.modifier) {
        updatedItem.modifier = {
          ...existingItem.item.modifier,
          ...updates.modifier,
        };
      }

      // Update the item in registry (replace)
      world.itemRegistry.removeItem(itemId);

      // Re-add with merged data using original ID
      const serverItem = world.itemRegistry.addItem(
        updatedItem.name || 'Item',
        updatedItem.itemType,
        undefined, // Position handled separately
        updatedItem.modifier?.texture,
        updatedItem.parameters,
        itemId // Use original ID
      );

      // Set the full modifier including onUseEffect
      serverItem.item.modifier = updatedItem.modifier;
      serverItem.item.description = updatedItem.description;

      // Save to disk
      world.itemRegistry.save();

      console.log(`[ItemRoutes] Item updated: ${itemId}`, {
        hasModifier: !!updatedItem.modifier,
        hasOnUseEffect: !!updatedItem.modifier?.onUseEffect,
      });

      return res.json(serverItem);
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
