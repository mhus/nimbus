/**
 * ItemCommand - Manipulate items in the world
 *
 * Commands:
 * - /item add <x> <y> <z> <displayName> <texturePath> - Add item
 * - /item remove <x> <y> <z> - Remove item
 * - /item list - List all items
 */

import { CommandHandler, type CommandContext, type CommandResult } from './CommandHandler';
import type { WorldManager } from '../world/WorldManager';
import type { ItemUpdateBuffer } from '../network/ItemUpdateBuffer';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('ItemCommand');

export class ItemCommand extends CommandHandler {
  private readonly usage = '/item <add|remove|get|list> [args...]';

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
        message: `Usage: ${this.usage}\n\nSubcommands:\n  add <x> <y> <z> <displayName> <texturePath> - Add item\n  remove <x> <y> <z> - Remove item\n  get <x> <y> <z> - Get item info at position\n  list - List all items`,
      };
    }

    const subcommand = String(args[0]).toLowerCase();

    switch (subcommand) {
      case 'add':
        return this.handleAdd(args.slice(1), context);
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
   * Handle /item add <x> <y> <z> <displayName> <texturePath>
   */
  private async handleAdd(args: any[], context: CommandContext): Promise<CommandResult> {
    if (args.length < 5) {
      return {
        rc: 1,
        message: 'Usage: /item add <x> <y> <z> <displayName> <texturePath>',
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

    const displayName = String(args[3]);
    const texturePath = String(args[4]);

    try {
      // Add item to registry
      const item = world.itemRegistry.addItem(x, y, z, displayName, texturePath);

      // Queue item update for broadcast
      this.itemUpdateBuffer.addUpdate(worldId, item);

      logger.info('Item added via command', {
        worldId,
        position: { x, y, z },
        displayName,
        texturePath,
        itemId: item.metadata?.id,
      });

      return {
        rc: 0,
        message: `Item "${displayName}" added at (${x}, ${y}, ${z})\nItem ID: ${item.metadata?.id}`,
      };
    } catch (error) {
      logger.error('Failed to add item', { worldId, position: { x, y, z } }, error as Error);
      return {
        rc: -1,
        message: `Failed to add item: ${(error as Error).message}`,
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
      world.itemRegistry.removeItem(x, y, z);

      // Queue item deletion update for broadcast (blockTypeId: 0)
      const deleteUpdate = {
        position: { x, y, z },
        blockTypeId: 0, // AIR = deletion
      };
      this.itemUpdateBuffer.addUpdate(worldId, deleteUpdate);

      logger.info('Item removed via command', {
        worldId,
        position: { x, y, z },
        itemId: item.metadata?.id,
        displayName: item.metadata?.displayName,
      });

      return {
        rc: 0,
        message: `Item "${item.metadata?.displayName || 'unknown'}" removed from (${x}, ${y}, ${z})`,
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
      // Get item data (includes parameters)
      const itemData = world.itemRegistry.getItemData(x, y, z);
      if (!itemData) {
        return {
          rc: 0,
          message: `No item at position (${x}, ${y}, ${z})`,
        };
      }

      const item = itemData.block;

      // Format item information
      const itemInfo = [
        `Item at (${x}, ${y}, ${z}):`,
        `  Display Name: ${item.metadata?.displayName || 'unknown'}`,
        `  Item ID: ${item.metadata?.id || 'unknown'}`,
        `  Block Type ID: ${item.blockTypeId}`,
      ];

      // Add description if available
      if (itemData.description) {
        itemInfo.push(`  Description: ${itemData.description}`);
      }

      // Add texture info if available
      if (item.modifiers && item.modifiers['0']?.visibility?.textures) {
        const texture = item.modifiers['0'].visibility.textures['0'];
        if (texture) {
          itemInfo.push(`  Texture: ${typeof texture === 'string' ? texture : texture.path || 'unknown'}`);
        }
      }

      // Add position info
      itemInfo.push(`  Position: x=${item.position.x}, y=${item.position.y}, z=${item.position.z}`);

      // Add parameters if they exist
      if (itemData.parameters && Object.keys(itemData.parameters).length > 0) {
        itemInfo.push(`  Parameters:`);
        for (const [key, value] of Object.entries(itemData.parameters)) {
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
          const pos = item.position;
          const name = item.metadata?.displayName || 'unknown';
          const id = item.metadata?.id || 'unknown';
          return `${index + 1}. "${name}" at (${pos.x}, ${pos.y}, ${pos.z}) [ID: ${id}]`;
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
