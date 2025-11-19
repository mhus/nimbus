/**
 * ItemRegistry - Manages items in the world
 *
 * Items are stored with position, id, and itemType directly (no Block wrapper).
 * Items reference ItemType templates for default properties and can override them.
 */

import type { Item } from '@nimbus/shared';
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import * as fs from 'fs';
import * as path from 'path';

const logger = getLogger('ItemRegistry');

/**
 * ItemRegistry manages all items in the world
 */
export class ItemRegistry {
  /** All items in the world, keyed by position "x,y,z" */
  private items: Map<string, Item>;

  /** World ID for this registry */
  private worldId: string;

  /** Is registry dirty (needs saving) */
  private isDirty: boolean = false;

  constructor(worldId: string) {
    this.worldId = worldId;
    this.items = new Map();
  }

  /**
   * Add item to registry
   *
   * @param x World X coordinate
   * @param y World Y coordinate
   * @param z World Z coordinate
   * @param displayName Display name for the item
   * @param itemType Item type identifier (e.g., 'sword', 'wand')
   * @param texturePath Optional texture path override
   * @param parameters Optional parameters for the item
   * @returns Created item
   */
  addItem(
    x: number,
    y: number,
    z: number,
    displayName: string,
    itemType: string,
    texturePath?: string,
    parameters?: Record<string, any>
  ): Item {
    const key = `${x},${y},${z}`;

    // Generate unique ID
    const id = `item_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;

    // Create Item
    const item: Item = {
      id,
      itemType,
      position: { x, y, z },
      name: displayName,
      parameters,
    };

    // Only set modifier.texture if provided (override ItemType default)
    if (texturePath) {
      item.modifier = {
        texture: texturePath,
      };
    }

    // Add to registry
    this.items.set(key, item);
    this.isDirty = true;

    logger.info('Item added', {
      position: { x, y, z },
      id,
      name: displayName,
      itemType,
      texturePath,
      hasParameters: !!parameters,
    });

    return item;
  }

  /**
   * Remove item from registry
   *
   * @param x World X coordinate
   * @param y World Y coordinate
   * @param z World Z coordinate
   * @returns True if item was removed, false if no item at position
   */
  removeItem(x: number, y: number, z: number): boolean {
    const key = `${x},${y},${z}`;
    const deleted = this.items.delete(key);

    if (deleted) {
      this.isDirty = true;
      logger.info('Item removed', { position: { x, y, z } });
    }

    return deleted;
  }

  /**
   * Get item at position
   *
   * @param x World X coordinate
   * @param y World Y coordinate
   * @param z World Z coordinate
   * @returns Item or undefined if no item at position
   */
  getItem(x: number, y: number, z: number): Item | undefined {
    const key = `${x},${y},${z}`;
    return this.items.get(key);
  }

  /**
   * Get item by ID
   *
   * @param itemId Item ID
   * @returns Item or undefined if not found
   */
  getItemById(itemId: string): Item | undefined {
    for (const item of this.items.values()) {
      if (item.id === itemId) {
        return item;
      }
    }
    return undefined;
  }

  /**
   * Check if item exists at position
   *
   * @param x World X coordinate
   * @param y World Y coordinate
   * @param z World Z coordinate
   * @returns True if item exists at position
   */
  hasItem(x: number, y: number, z: number): boolean {
    const key = `${x},${y},${z}`;
    return this.items.has(key);
  }

  /**
   * Get all items in chunk
   *
   * @param cx Chunk X coordinate
   * @param cz Chunk Z coordinate
   * @param chunkSize Chunk size (blocks per side)
   * @returns Array of items in the chunk
   */
  getItemsInChunk(cx: number, cz: number, chunkSize: number): Item[] {
    const minX = cx * chunkSize;
    const maxX = minX + chunkSize - 1;
    const minZ = cz * chunkSize;
    const maxZ = minZ + chunkSize - 1;

    const items: Item[] = [];

    for (const item of this.items.values()) {
      if (
        item.position.x >= minX &&
        item.position.x <= maxX &&
        item.position.z >= minZ &&
        item.position.z <= maxZ
      ) {
        items.push(item);
      }
    }

    return items;
  }

  /**
   * Get all items in the world
   *
   * @returns Array of all items
   */
  getAllItems(): Item[] {
    return Array.from(this.items.values());
  }

  /**
   * Get item count
   *
   * @returns Total number of items in the world
   */
  getItemCount(): number {
    return this.items.size;
  }

  /**
   * Clear all items
   */
  clearAll(): void {
    this.items.clear();
    this.isDirty = true;
    logger.info('All items cleared');
  }

  /**
   * Save items to disk
   *
   * Saves all items to data/worlds/{worldId}/items.json
   * Format: Array of Item objects
   */
  async save(): Promise<void> {
    try {
      const dataDir = path.join(process.cwd(), 'data', 'worlds', this.worldId);

      // Ensure directory exists
      if (!fs.existsSync(dataDir)) {
        fs.mkdirSync(dataDir, { recursive: true });
      }

      const itemsPath = path.join(dataDir, 'items.json');
      const itemsArray = Array.from(this.items.values());

      // Write Item array to file
      fs.writeFileSync(itemsPath, JSON.stringify(itemsArray, null, 2), 'utf-8');

      this.isDirty = false;

      logger.info('Items saved to disk', {
        worldId: this.worldId,
        path: itemsPath,
        itemCount: itemsArray.length,
      });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'ItemRegistry.save',
        { worldId: this.worldId }
      );
    }
  }

  /**
   * Load items from disk
   *
   * Loads items from data/worlds/{worldId}/items.json
   * Format: Array of Item objects
   *
   * @returns Number of items loaded
   */
  async load(): Promise<number> {
    try {
      const itemsPath = path.join(process.cwd(), 'data', 'worlds', this.worldId, 'items.json');

      // Check if file exists
      if (!fs.existsSync(itemsPath)) {
        logger.info('No items file found, starting with empty registry', {
          worldId: this.worldId,
          path: itemsPath,
        });
        return 0;
      }

      // Read and parse items file
      const itemsContent = fs.readFileSync(itemsPath, 'utf-8');
      const itemsArray: Item[] = JSON.parse(itemsContent);

      // Clear existing items
      this.items.clear();

      // Load items into registry
      for (const item of itemsArray) {
        const key = `${item.position.x},${item.position.y},${item.position.z}`;
        this.items.set(key, item);
      }

      this.isDirty = false;

      logger.info('Items loaded from disk', {
        worldId: this.worldId,
        path: itemsPath,
        itemCount: itemsArray.length,
      });

      return itemsArray.length;
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'ItemRegistry.load',
        { worldId: this.worldId }
      );
    }
  }

  /**
   * Check if registry has unsaved changes
   *
   * @returns True if registry needs saving
   */
  isDirtyFlag(): boolean {
    return this.isDirty;
  }
}
