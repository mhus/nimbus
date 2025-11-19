/**
 * ItemType REST API Routes
 *
 * Provides endpoints for loading item type definitions from files/itemtypes/
 */

import express from 'express';
import path from 'path';
import fs from 'fs/promises';

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
 * List all available item types
 *
 * @returns Array of ItemType objects
 */
router.get('/:worldId/itemtypes', async (req, res) => {
  try {
    const itemTypesDir = path.join(__dirname, '../../../files/itemtypes');

    // Read all JSON files from itemtypes directory
    const files = await fs.readdir(itemTypesDir);

    const itemTypes = [];
    for (const file of files) {
      if (file.endsWith('.json')) {
        try {
          const data = await fs.readFile(path.join(itemTypesDir, file), 'utf-8');
          const itemType = JSON.parse(data);
          itemTypes.push(itemType);
        } catch (error) {
          console.warn(`Failed to load ItemType from ${file}:`, error);
          // Continue loading other files
        }
      }
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
 * Create itemType routes
 * @returns Express router
 */
export function createItemTypeRoutes(): express.Router {
  return router;
}
