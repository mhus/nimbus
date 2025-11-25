/**
 * ItemCommand - Manipulate items in the world
 *
 * Commands:
 * - /item add <displayName> <itemType> [texturePath] - Add item without position
 * - /item add <x> <y> <z> <displayName> <itemType> [texturePath] - Add item with position
 * - /item place <itemId> <x> <y> <z> - Place item at position
 * - /item remove <x> <y> <z> - Remove item from position
 * - /item list - List all items
 */

import { CommandHandler, type CommandContext, type CommandResult } from './CommandHandler';
import type { WorldManager } from '../world/WorldManager';
import type { ItemUpdateBuffer } from '../network/ItemUpdateBuffer';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('ItemCommand');

export class ItemCommand extends CommandHandler {
  private readonly usage = '/item <add|place|remove|get|list> [args...]';

  constructor(
    private worldManager: WorldManager,
    private itemUpdateBuffer: ItemUpdateBuffer
  ) {
    super();
  }

  name(): string {
    return 'item';
  }

  description(): string {
    return 'Manipulate items in the world';
  }

  async execute(context: CommandContext, args: any[]): Promise<CommandResult> {
    if (args.length === 0) {
      return {
        rc: 1,
        message: `Usage: ${this.usage}\n\nSubcommands:\n  add <displayName> <itemType> [texturePath] - Add item without position\n  add <x> <y> <z> <displayName> <itemType> [texturePath] - Add item with position\n  place <itemId> <x> <y> <z> - Place item at position\n  remove <x> <y> <z> - Remove item from position\n  get <x> <y> <z> - Get item info at position\n  list - List all items`,
      };
    }

    const subcommand = String(args[0]).toLowerCase();

    switch (subcommand) {
      case 'add':
        return this.handleAdd(args.slice(1), context);
      case 'place':
        return this.handlePlace(args.slice(1), context);
      case 'remove':
        return this.handleRemove(args.slice(1), context);
      case 'get':
        return this.handleGet(args.slice(1), context);
      case 'list':
        return this.handleList(context);
      default:
        return {
          rc: 1,
          message: `Unknown subcommand: ${subcommand}\nUse /item for help`,
        };
    }
  }

  /**
   * Handle /item add <displayName> <itemType> [texturePath]
   * or /item add <x> <y> <z> <displayName> <itemType> [texturePath]
   */
  private async handleAdd(args: any[], context: CommandContext): Promise<CommandResult> {
    if (args.length < 2) {
      return {
        rc: 1,
        message: 'Usage:\n  /item add <displayName> <itemType> [texturePath] - Add without position\n  /item add <x> <y> <z> <displayName> <itemType> [texturePath] - Add with position',
      };
    }

    const worldId = context.session.worldId;
    if (!worldId) {
      return {
        rc: 1,
        message: 'Not in a world',
      };
    }

    const world = this.worldManager.getWorld(worldId);
    if (!world) {
      return {
        rc: 1,
        message: `World not found: ${worldId}`,
      };
    }

    // Check if first three args are coordinates
    const firstArgIsNumber = !isNaN(parseInt(String(args[0]), 10));
    const secondArgIsNumber = args.length >= 2 && !isNaN(parseInt(String(args[1]), 10));
    const thirdArgIsNumber = args.length >= 3 && !isNaN(parseInt(String(args[2]), 10));
    const hasCoordinates = firstArgIsNumber && secondArgIsNumber && thirdArgIsNumber;

    let position: { x: number; y: number; z: number } | undefined;
    let displayName: string;
    let itemType: string;
    let texturePath: string | undefined;

    if (hasCoordinates) {
      // Format: /item add <x> <y> <z> <displayName> <itemType> [texturePath]
      if (args.length < 5) {
        return {
          rc: 1,
          message: 'Usage: /item add <x> <y> <z> <displayName> <itemType> [texturePath]',
        };
      }

      position = {
        x: parseInt(String(args[0]), 10),
        y: parseInt(String(args[1]), 10),
        z: parseInt(String(args[2]), 10),
      };
      displayName = String(args[3]);
      itemType = String(args[4]);
      texturePath = args[5] ? String(args[5]) : undefined;
    } else {
      // Format: /item add <displayName> <itemType> [texturePath]
      displayName = String(args[0]);
      itemType = String(args[1]);
      texturePath = args[2] ? String(args[2]) : undefined;
    }

    try {
      // Add item to registry
      const item = world.itemRegistry.addItem(displayName, itemType, position, texturePath);

      // Queue item update for broadcast (only if item has position)
      if (item.itemBlockRef) {
        this.itemUpdateBuffer.addUpdate(worldId, item.itemBlockRef);
      }

      logger.info('Item added via command', {
        worldId,
        position,
        displayName,
        itemType,
        texturePath,
        itemId: item.item.id,
      });

      const posMsg = position ? ` at (${position.x}, ${position.y}, ${position.z})` : ' (no position)';
      return {
        rc: 0,
        message: `Item "${displayName}" (${itemType}) added${posMsg}\nItem ID: ${item.item.id}`,
      };
    } catch (error) {
      logger.error('Failed to add item', { worldId, position }, error as Error);
      return {
        rc: -1,
        message: `Failed to add item: ${(error as Error).message}`,
      };
    }
  }

  /**
   * Handle /item place <itemId> <x> <y> <z>
   * Places an item (that has no position) at a specific position
   */
  private async handlePlace(args: any[], context: CommandContext): Promise<CommandResult> {
    if (args.length < 4) {
      return {
        rc: 1,
        message: 'Usage: /item place <itemId> <x> <y> <z>',
      };
    }

    const worldId = context.session.worldId;
    if (!worldId) {
      return {
        rc: 1,
        message: 'Not in a world',
      };
    }

    const world = this.worldManager.getWorld(worldId);
    if (!world) {
      return {
        rc: 1,
        message: `World not found: ${worldId}`,
      };
    }

    const itemId = String(args[0]);
    const x = parseInt(String(args[1]), 10);
    const y = parseInt(String(args[2]), 10);
    const z = parseInt(String(args[3]), 10);

    if (isNaN(x) || isNaN(y) || isNaN(z)) {
      return {
        rc: 1,
        message: 'Invalid coordinates. Must be integers.',
      };
    }

    try {
      // Get item by ID
      const serverItem = world.itemRegistry.getItemById(itemId);
      if (!serverItem) {
        return {
          rc: 1,
          message: `Item not found: ${itemId}`,
        };
      }

      // Check if item already has a position
      if (serverItem.itemBlockRef) {
        return {
          rc: 1,
          message: `Item already has a position at (${serverItem.itemBlockRef.position.x}, ${serverItem.itemBlockRef.position.y}, ${serverItem.itemBlockRef.position.z}). Remove it first.`,
        };
      }

      // Check if target position is occupied
      const existingItem = world.itemRegistry.getItem(x, y, z);
      if (existingItem) {
        return {
          rc: 1,
          message: `Position (${x}, ${y}, ${z}) already has an item: ${existingItem.item.name}`,
        };
      }

      // Load ItemType to get modifier data
      const itemType = String(serverItem.item.itemType);

      // Remove item and re-add it with position using original ID
      world.itemRegistry.removeItem(itemId);
      const newItem = world.itemRegistry.addItem(
        serverItem.item.name || 'Item',
        itemType,
        { x, y, z },
        serverItem.item.parameters?.texturePath as string | undefined,
        serverItem.item.parameters,
        itemId // Preserve original ID
      );

      // Restore full item data (modifier, description)
      newItem.item.modifier = serverItem.item.modifier;
      newItem.item.description = serverItem.item.description;

      // Queue item update for broadcast
      if (newItem.itemBlockRef) {
        this.itemUpdateBuffer.addUpdate(worldId, newItem.itemBlockRef);
      }

      logger.info('Item placed via command', {
        worldId,
        itemId,
        position: { x, y, z },
      });

      return {
        rc: 0,
        message: `Item "${serverItem.item.name}" placed at (${x}, ${y}, ${z})`,
      };
    } catch (error) {
      logger.error('Failed to place item', { worldId, itemId, position: { x, y, z } }, error as Error);
      return {
        rc: -1,
        message: `Failed to place item: ${(error as Error).message}`,
      };
    }
  }

  /**
   * Handle /item remove <x> <y> <z>
   */
  private async handleRemove(args: any[], context: CommandContext): Promise<CommandResult> {
    if (args.length < 3) {
      return {
        rc: 1,
        message: 'Usage: /item remove <x> <y> <z>',
      };
    }

    const worldId = context.session.worldId;
    if (!worldId) {
      return {
        rc: 1,
        message: 'Not in a world',
      };
    }

    const world = this.worldManager.getWorld(worldId);
    if (!world) {
      return {
        rc: 1,
        message: `World not found: ${worldId}`,
      };
    }

    // Parse coordinates
    const x = parseInt(String(args[0]), 10);
    const y = parseInt(String(args[1]), 10);
    const z = parseInt(String(args[2]), 10);

    if (isNaN(x) || isNaN(y) || isNaN(z)) {
      return {
        rc: 1,
        message: 'Invalid coordinates. Must be integers.',
      };
    }

    try {
      // Check if item exists
      const item = world.itemRegistry.getItem(x, y, z);
      if (!item) {
        return {
          rc: 1,
          message: `No item at position (${x}, ${y}, ${z})`,
        };
      }

      // Remove item from registry
      world.itemRegistry.removeItem(item.item.id);

      // Queue item deletion update for broadcast
      // Create a delete marker item with special itemType
      const deleteItem: any = {
        id: item.item.id,
        texture: '__deleted__', // Special marker for deletion
        position: { x, y, z },
      };
      this.itemUpdateBuffer.addUpdate(worldId, deleteItem);

      logger.info('Item removed via command', {
        worldId,
        position: { x, y, z },
        itemId: item.item.id,
        displayName: item.item.name,
      });

      return {
        rc: 0,
        message: `Item "${item.item.name || 'unknown'}" removed from (${x}, ${y}, ${z})`,
      };
    } catch (error) {
      logger.error('Failed to remove item', { worldId, position: { x, y, z } }, error as Error);
      return {
        rc: -1,
        message: `Failed to remove item: ${(error as Error).message}`,
      };
    }
  }

  /**
   * Handle /item get <x> <y> <z>
   */
  private async handleGet(args: any[], context: CommandContext): Promise<CommandResult> {
    if (args.length < 3) {
      return {
        rc: 1,
        message: 'Usage: /item get <x> <y> <z>',
      };
    }

    const worldId = context.session.worldId;
    if (!worldId) {
      return {
        rc: 1,
        message: 'Not in a world',
      };
    }

    const world = this.worldManager.getWorld(worldId);
    if (!world) {
      return {
        rc: 1,
        message: `World not found: ${worldId}`,
      };
    }

    // Parse coordinates
    const x = parseInt(String(args[0]), 10);
    const y = parseInt(String(args[1]), 10);
    const z = parseInt(String(args[2]), 10);

    if (isNaN(x) || isNaN(y) || isNaN(z)) {
      return {
        rc: 1,
        message: 'Invalid coordinates. Must be integers.',
      };
    }

    try {
      // Get item (includes all data)
      const item = world.itemRegistry.getItem(x, y, z);
      if (!item) {
        return {
          rc: 0,
          message: `No item at position (${x}, ${y}, ${z})`,
        };
      }

      // Format item information
      const itemInfo = [
        `Item at (${x}, ${y}, ${z}):`,
        `  Display Name: ${item.item.name || 'unknown'}`,
        `  Item ID: ${item.item.id || 'unknown'}`,
        `  Type: ${item.item.itemType}`,
      ];

      // Add description if available
      if (item.item.description) {
        itemInfo.push(`  Description: ${item.item.description}`);
      }

      // Add modifier properties if available
      if (item.item.modifier) {
        if (item.item.modifier.pose) {
          itemInfo.push(`  Pose: ${item.item.modifier.pose}`);
        }
        if (item.item.modifier.texture) {
          itemInfo.push(`  Texture: ${item.item.modifier.texture}`);
        }
        if (item.item.modifier.scaleX !== undefined || item.item.modifier.scaleY !== undefined) {
          itemInfo.push(`  Scale: ${item.item.modifier.scaleX ?? 0.5} x ${item.item.modifier.scaleY ?? 0.5}`);
        }
      }

      // Add position info
      if (item.itemBlockRef) {
        itemInfo.push(`  Position: x=${item.itemBlockRef?.position.x}, y=${item.itemBlockRef?.position.y}, z=${item.itemBlockRef?.position.z}`);
      }
      // Add parameters if they exist
      if (item.item.parameters && Object.keys(item.item.parameters).length > 0) {
        itemInfo.push(`  Parameters:`);
        for (const [key, value] of Object.entries(item.item.parameters)) {
          itemInfo.push(`    ${key}: ${JSON.stringify(value)}`);
        }
      }

      return {
        rc: 0,
        message: itemInfo.join('\n'),
      };
    } catch (error) {
      logger.error('Failed to get item info', { worldId, position: { x, y, z } }, error as Error);
      return {
        rc: -1,
        message: `Failed to get item info: ${(error as Error).message}`,
      };
    }
  }

  /**
   * Handle /item list
   */
  private async handleList(context: CommandContext): Promise<CommandResult> {
    const worldId = context.session.worldId;
    if (!worldId) {
      return {
        rc: 1,
        message: 'Not in a world',
      };
    }

    const world = this.worldManager.getWorld(worldId);
    if (!world) {
      return {
        rc: 1,
        message: `World not found: ${worldId}`,
      };
    }

    try {
      const items = world.itemRegistry.getAllItems();

      if (items.length === 0) {
        return {
          rc: 0,
          message: 'No items in world',
        };
      }

      const itemList = items
        .map((item, index) => {
          const pos = item.itemBlockRef?.position || 'none';
          const name = item.item.name || 'unknown';
          const id = item.item.id || 'unknown';
          const type = item.item.itemType || 'unknown';
          return `${index + 1}. "${name}" (${type}) at (${pos}) [ID: ${id}]`;
        })
        .join('\n');

      return {
        rc: 0,
        message: `Items in world (${items.length} total):\n${itemList}`,
      };
    } catch (error) {
      logger.error('Failed to list items', { worldId }, error as Error);
      return {
        rc: -1,
        message: `Failed to list items: ${(error as Error).message}`,
      };
    }
  }
}
