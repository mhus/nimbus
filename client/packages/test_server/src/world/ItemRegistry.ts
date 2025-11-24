/**
 * ItemRegistry - Manages items in the world
 *
 * Items are stored with position, id, and itemType directly (no Block wrapper).
 * Items reference ItemType templates for default properties and can override them.
 */

import type {Item, ItemBlockRef, ItemType, Vector3} from '@nimbus/shared';
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import * as fs from 'fs';
import * as path from 'path';

const logger = getLogger('ItemRegistry');

/**
 * Load ItemType from disk
 *
 * @param itemTypeId Item type identifier (e.g., 'sword', 'wand')
 * @returns ItemType or undefined if not found
 */
function loadItemType(itemTypeId: string): ItemType | undefined {
  try {
    // Load from files/itemtypes/{itemTypeId}.json
    const filePath = path.join(process.cwd(), 'files', 'itemtypes', `${itemTypeId}.json`);

    if (!fs.existsSync(filePath)) {
      logger.warn('ItemType file not found', { itemTypeId, filePath });
      return undefined;
    }

    const data = fs.readFileSync(filePath, 'utf-8');
    const itemType: ItemType = JSON.parse(data);

    logger.debug('ItemType loaded', { itemTypeId, filePath });
    return itemType;
  } catch (error) {
    ExceptionHandler.handle(error, 'loadItemType', { itemTypeId });
    return undefined;
  }
}

export interface ServerItem {
  /**
   * If the item has an position in the world, this is the reference to it
   * and other important data loaded from item and itemType in time of storage.
   */
  itemBlockRef?: ItemBlockRef;
  /**
   * The item data itself
   */
  item: Item;
}

/**
 * ItemRegistry manages all items in the world
 */
export class ItemRegistry {
  /** All items in the world, keyed by position "x,y,z" */
  private items: Map<string, ServerItem>;

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
   * @param displayName Display name for the item
   * @param itemTypeId Item type identifier (e.g., 'sword', 'wand')
   * @param position World Position (X,Y,Z) coordinate (optional)
   * @param texturePath Optional texture path override
   * @param parameters Optional parameters for the item
   * @param itemId Optional item ID (if not provided, generates new one)
   * @returns Created item
   */
  addItem(
    displayName: string,
    itemTypeId: string,
    position?: Vector3,
    texturePath?: string,
    parameters?: Record<string, any>,
    itemId?: string
  ): ServerItem {

    // Use provided ID or generate new one
    const id = itemId || `item_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;

    const itemType = loadItemType(itemTypeId);

    if (!itemType) {
      logger.error('ItemType not found, cannot create item', { itemTypeId });
      throw new Error(`ItemType not found: ${itemTypeId}`);
    }

    // Create Item
    const item: Item = {
      id,
      itemType: itemTypeId,
      name: displayName,
      parameters,
    };

    const itemBlockRef: ItemBlockRef | undefined = position ? {
      id,
      position,
      scaleX: itemType.modifier?.scaleX || 0,
      scaleY: itemType.modifier?.scaleY || 0,
      offset: itemType.modifier?.offset || [0, 0, 0],
      texture: texturePath || itemType.modifier?.texture || '',
    } : undefined;

    const serverItem = {
        itemBlockRef,
        item,
    }

    // Add to registry
    this.items.set(id, serverItem);
    this.isDirty = true;

    logger.info('Item added', {
      position,
      id,
      name: displayName,
      itemType,
      texturePath,
      hasParameters: !!parameters,
    });

    return serverItem;
  }

  /**
   * Remove item from registry
   *
   * @param x World X coordinate
   * @param y World Y coordinate
   * @param z World Z coordinate
   * @returns True if item was removed, false if no item at position
   */
  removeItem(id : string): boolean {
    const deleted = this.items.delete(id);

    if (deleted) {
      this.isDirty = true;
      logger.info('Item removed', id);
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
  getItem(x: number, y: number, z: number): ServerItem | undefined {
    for (const item of this.items.values()) {
      if (
        item.itemBlockRef &&
        item.itemBlockRef.position.x === x &&
        item.itemBlockRef.position.y === y &&
        item.itemBlockRef.position.z === z
      ) {
        return item;
      }
    }
    return undefined;
  }

  /**
   * Get item by ID
   *
   * @param itemId Item ID
   * @returns Item or undefined if not found
   */
  getItemById(itemId: string): ServerItem | undefined {
    return this.items.get(itemId);
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
  getItemsInChunk(cx: number, cz: number, chunkSize: number): ItemBlockRef[] {
    const minX = cx * chunkSize;
    const maxX = minX + chunkSize - 1;
    const minZ = cz * chunkSize;
    const maxZ = minZ + chunkSize - 1;

    const items: ItemBlockRef[] = [];

    for (const item of this.items.values()) {
      if (
          item.itemBlockRef &&
        item.itemBlockRef.position.x >= minX &&
        item.itemBlockRef.position.x <= maxX &&
        item.itemBlockRef.position.z >= minZ &&
        item.itemBlockRef.position.z <= maxZ
      ) {
        items.push(item.itemBlockRef);
      }
    }

    return items;
  }

  /**
   * Get all items in the world
   *
   * @returns Array of all items
   */
  getAllItems(): ServerItem[] {
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
      const serverItemsArray: ServerItem[] = JSON.parse(itemsContent);

      // Clear existing items
      this.items.clear();

      // Load items into registry
      for (const serverItem of serverItemsArray) {
//        const key = `${item.position.x},${item.position.y},${item.position.z}`;
        this.items.set(serverItem.item.id, serverItem);
      }

      this.isDirty = false;

      logger.info('Items loaded from disk', {
        worldId: this.worldId,
        path: itemsPath,
        itemCount: serverItemsArray.length,
      });

      return serverItemsArray.length;
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
