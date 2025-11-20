/**
 * ItemType REST API Routes
 *
 * Provides endpoints for loading, searching, creating, updating, and deleting item type definitions
 * from files/itemtypes/
 */

import express from 'express';
import path from 'path';
import fs from 'fs/promises';
import { fileURLToPath } from 'url';
import type { ItemType } from '@nimbus/shared';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const router = express.Router();

/**
 * GET /api/worlds/:worldId/itemtypes/:type
 * Get a specific item type definition
 *
 * @param type - Item type identifier (e.g., 'sword', 'wand', 'potion')
 * @returns ItemType JSON
 */
router.get('/:worldId/itemtypes/:type', async (req, res) => {
  try {
    const { type } = req.params;

    // Validate type parameter (alphanumeric + underscore only)
    if (!/^[a-zA-Z0-9_]+$/.test(type)) {
      res.status(400).json({ error: 'Invalid item type format' });
      return;
    }

    // Load from files/itemtypes/{type}.json
    const filePath = path.join(__dirname, '../../../files/itemtypes', `${type}.json`);

    const data = await fs.readFile(filePath, 'utf-8');
    const itemType = JSON.parse(data);

    res.json(itemType);
  } catch (error: any) {
    if (error.code === 'ENOENT') {
      res.status(404).json({ error: `ItemType not found: ${req.params.type}` });
    } else if (error instanceof SyntaxError) {
      res.status(500).json({ error: 'Invalid ItemType JSON format' });
    } else {
      console.error('Failed to load ItemType:', error);
      res.status(500).json({ error: 'Failed to load ItemType' });
    }
  }
});

/**
 * GET /api/worlds/:worldId/itemtypes
 * List all available item types with optional search
 *
 * @query query - Optional search term to filter by name or id
 * @returns Array of ItemType objects (max 100)
 */
router.get('/:worldId/itemtypes', async (req, res) => {
  try {
    const itemTypesDir = path.join(__dirname, '../../../files/itemtypes');
    const searchQuery = req.query.query ? String(req.query.query).toLowerCase() : null;

    // Read all JSON files from itemtypes directory
    const files = await fs.readdir(itemTypesDir);

    let itemTypes: ItemType[] = [];
    for (const file of files) {
      if (file.endsWith('.json')) {
        try {
          const data = await fs.readFile(path.join(itemTypesDir, file), 'utf-8');
          const itemType: ItemType = JSON.parse(data);

          // Apply search filter if query provided
          if (searchQuery) {
            const matchesId = itemType.id?.toLowerCase().includes(searchQuery);
            const matchesName = itemType.name?.toLowerCase().includes(searchQuery);
            if (!matchesId && !matchesName) {
              continue; // Skip this item
            }
          }

          itemTypes.push(itemType);
        } catch (error) {
          console.warn(`Failed to load ItemType from ${file}:`, error);
          // Continue loading other files
        }
      }
    }

    // Limit to 100 results
    if (itemTypes.length > 100) {
      itemTypes = itemTypes.slice(0, 100);
    }

    res.json({
      itemTypes,
      count: itemTypes.length,
    });
  } catch (error: any) {
    console.error('Failed to list ItemTypes:', error);
    res.status(500).json({ error: 'Failed to list ItemTypes' });
  }
});

/**
 * POST /api/worlds/:worldId/itemtypes
 * Create a new item type
 *
 * @body ItemType object
 * @returns Created ItemType
 */
router.post('/:worldId/itemtypes', async (req, res) => {
  try {
    const itemType: ItemType = req.body;

    // Validate required fields
    if (!itemType.id) {
      res.status(400).json({ error: 'ItemType.id is required' });
      return;
    }

    // Validate id format (alphanumeric + underscore only)
    if (!/^[a-zA-Z0-9_]+$/.test(itemType.id)) {
      res.status(400).json({ error: 'Invalid ItemType.id format (alphanumeric + underscore only)' });
      return;
    }

    const itemTypesDir = path.join(__dirname, '../../../files/itemtypes');
    const filePath = path.join(itemTypesDir, `${itemType.id}.json`);

    // Check if file already exists
    try {
      await fs.access(filePath);
      res.status(409).json({ error: `ItemType already exists: ${itemType.id}` });
      return;
    } catch {
      // File doesn't exist, continue
    }

    // Ensure directory exists
    await fs.mkdir(itemTypesDir, { recursive: true });

    // Write to file
    await fs.writeFile(filePath, JSON.stringify(itemType, null, 2), 'utf-8');

    console.log(`ItemType created: ${itemType.id}`);
    res.status(201).json(itemType);
  } catch (error: any) {
    console.error('Failed to create ItemType:', error);
    res.status(500).json({ error: 'Failed to create ItemType' });
  }
});

/**
 * PUT /api/worlds/:worldId/itemtypes/:itemTypeId
 * Update an existing item type
 *
 * @param itemTypeId - Item type identifier
 * @body ItemType object (partial or full)
 * @returns Updated ItemType
 */
router.put('/:worldId/itemtypes/:itemTypeId', async (req, res) => {
  try {
    const { itemTypeId } = req.params;
    const updates: Partial<ItemType> = req.body;

    // Validate itemTypeId format
    if (!/^[a-zA-Z0-9_]+$/.test(itemTypeId)) {
      res.status(400).json({ error: 'Invalid item type format' });
      return;
    }

    const itemTypesDir = path.join(__dirname, '../../../files/itemtypes');
    const filePath = path.join(itemTypesDir, `${itemTypeId}.json`);

    // Check if file exists
    try {
      await fs.access(filePath);
    } catch {
      res.status(404).json({ error: `ItemType not found: ${itemTypeId}` });
      return;
    }

    // Read existing ItemType
    const existingData = await fs.readFile(filePath, 'utf-8');
    const existingItemType: ItemType = JSON.parse(existingData);

    // Merge updates (shallow merge)
    const updatedItemType: ItemType = {
      ...existingItemType,
      ...updates,
      id: itemTypeId, // Ensure id stays the same
    };

    // Write updated ItemType
    await fs.writeFile(filePath, JSON.stringify(updatedItemType, null, 2), 'utf-8');

    console.log(`ItemType updated: ${itemTypeId}`);
    res.json(updatedItemType);
  } catch (error: any) {
    if (error instanceof SyntaxError) {
      res.status(500).json({ error: 'Invalid ItemType JSON format in existing file' });
    } else {
      console.error('Failed to update ItemType:', error);
      res.status(500).json({ error: 'Failed to update ItemType' });
    }
  }
});

/**
 * DELETE /api/worlds/:worldId/itemtypes/:itemTypeId
 * Delete an item type
 *
 * @param itemTypeId - Item type identifier
 * @returns Success message
 */
router.delete('/:worldId/itemtypes/:itemTypeId', async (req, res) => {
  try {
    const { itemTypeId } = req.params;

    // Validate itemTypeId format
    if (!/^[a-zA-Z0-9_]+$/.test(itemTypeId)) {
      res.status(400).json({ error: 'Invalid item type format' });
      return;
    }

    const itemTypesDir = path.join(__dirname, '../../../files/itemtypes');
    const filePath = path.join(itemTypesDir, `${itemTypeId}.json`);

    // Check if file exists
    try {
      await fs.access(filePath);
    } catch {
      res.status(404).json({ error: `ItemType not found: ${itemTypeId}` });
      return;
    }

    // Delete file
    await fs.unlink(filePath);

    console.log(`ItemType deleted: ${itemTypeId}`);
    res.json({ message: `ItemType deleted: ${itemTypeId}` });
  } catch (error: any) {
    console.error('Failed to delete ItemType:', error);
    res.status(500).json({ error: 'Failed to delete ItemType' });
  }
});

/**
 * Create itemType routes
 * @returns Express router
 */
export function createItemTypeRoutes(): express.Router {
  return router;
}
