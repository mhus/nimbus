/**
 * Nimbus MCP Server
 * Provides Model Context Protocol tools for interacting with the Nimbus voxel server
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  Tool,
} from '@modelcontextprotocol/sdk/types.js';
import type { VoxelServer } from '../VoxelServer.js';
import type { IPCClient } from '../ipc/IPCClient.js';
import type { BlockType } from '@nimbus-client/core';

/**
 * MCP Server for Nimbus that exposes tools for block manipulation
 */
export class NimbusMCPServer {
  private server: Server;
  private voxelServer: VoxelServer;
  private ipcClient?: IPCClient;

  constructor(voxelServer: VoxelServer, ipcClient?: IPCClient) {
    this.voxelServer = voxelServer;
    this.ipcClient = ipcClient;

    // Create MCP server instance
    this.server = new Server(
      {
        name: 'nimbus-server',
        version: '1.0.0',
      },
      {
        capabilities: {
          tools: {},
        },
      }
    );

    this.setupHandlers();
  }

  /**
   * Setup MCP request handlers
   */
  private setupHandlers(): void {
    // List available tools
    this.server.setRequestHandler(ListToolsRequestSchema, async () => {
      return {
        tools: this.getTools(),
      };
    });

    // Handle tool calls
    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      const { name, arguments: args } = request.params;

      try {
        switch (name) {
          case 'create_block':
            return await this.handleCreateBlock(args);

          case 'place_block':
            return await this.handlePlaceBlock(args);

          case 'get_block':
            return await this.handleGetBlock(args);

          case 'list_blocks':
            return await this.handleListBlocks(args);

          case 'remove_block':
            return await this.handleRemoveBlock(args);

          case 'get_block_definition':
            return await this.handleGetBlockDefinition(args);

          case 'get_player_position':
            return await this.handleGetPlayerPosition(args);

          default:
            throw new Error(`Unknown tool: ${name}`);
        }
      } catch (error) {
        return {
          content: [
            {
              type: 'text',
              text: `Error: ${error instanceof Error ? error.message : String(error)}`,
            },
          ],
          isError: true,
        };
      }
    });
  }

  /**
   * Get list of available tools
   */
  private getTools(): Tool[] {
    return [
      {
        name: 'create_block',
        description: 'Create a new block type in the registry',
        inputSchema: {
          type: 'object',
          properties: {
            name: {
              type: 'string',
              description: 'Internal name of the block (e.g., "custom_stone")',
            },
            displayName: {
              type: 'string',
              description: 'Display name shown to users (e.g., "Custom Stone")',
            },
            shape: {
              type: 'number',
              description: 'Block shape: 0=CUBE, 1=CROSS, 2=HASH, 3=MODEL, 4=GLASS, 5=FLAT, 6=SPHERE, 7=COLUMN, 8=ROUND_CUBE, 9=STEPS, 10=STAIR, 11=BILLBOARD, 12=SPRITE, 13=FLAME',
              default: 0,
            },
            texture: {
              type: 'string',
              description: 'Texture path (e.g., "stone.png" or "models/tree.babylon" for MODEL shape)',
            },
            solid: {
              type: 'boolean',
              description: 'Whether the block is solid (has collision)',
              default: true,
            },
            transparent: {
              type: 'boolean',
              description: 'Whether the block is transparent',
              default: false,
            },
            hardness: {
              type: 'number',
              description: 'Mining hardness (0 = instant break)',
              default: 1,
            },
          },
          required: ['name', 'displayName', 'texture'],
        },
      },
      {
        name: 'place_block',
        description: 'Place a block at specific coordinates in the world with optional modifiers (rotation, scale, color, etc.)',
        inputSchema: {
          type: 'object',
          properties: {
            blockName: {
              type: 'string',
              description: 'Name of the block to place',
            },
            x: {
              type: 'number',
              description: 'X coordinate',
            },
            y: {
              type: 'number',
              description: 'Y coordinate (height)',
            },
            z: {
              type: 'number',
              description: 'Z coordinate',
            },
            worldName: {
              type: 'string',
              description: 'Name of the world (defaults to main world)',
              default: 'main',
            },
            modifier: {
              type: 'object',
              description: 'Optional block modifier with properties like rotation, scale, color, etc.',
              properties: {
                rotationX: {
                  type: 'number',
                  description: 'Rotation around X axis in degrees (vertical tilt)',
                },
                rotationY: {
                  type: 'number',
                  description: 'Rotation around Y axis in degrees (horizontal rotation)',
                },
                scale: {
                  type: 'array',
                  description: 'Scale [x, y, z], default [1.0, 1.0, 1.0]',
                  items: { type: 'number' },
                  minItems: 3,
                  maxItems: 3,
                },
                color: {
                  type: 'array',
                  description: 'Color tint [r, g, b] (0-255), default [255, 255, 255]',
                  items: { type: 'number' },
                  minItems: 3,
                  maxItems: 3,
                },
                texture: {
                  type: 'string',
                  description: 'Override texture path',
                },
                facing: {
                  type: 'number',
                  description: 'Facing direction (0=North, 1=East, 2=South, 3=West)',
                },
              },
            },
          },
          required: ['blockName', 'x', 'y', 'z'],
        },
      },
      {
        name: 'get_block',
        description: 'Get information about a block at specific coordinates',
        inputSchema: {
          type: 'object',
          properties: {
            x: {
              type: 'number',
              description: 'X coordinate',
            },
            y: {
              type: 'number',
              description: 'Y coordinate (height)',
            },
            z: {
              type: 'number',
              description: 'Z coordinate',
            },
            worldName: {
              type: 'string',
              description: 'Name of the world (defaults to main world)',
              default: 'main',
            },
          },
          required: ['x', 'y', 'z'],
        },
      },
      {
        name: 'list_blocks',
        description: 'List all registered block types',
        inputSchema: {
          type: 'object',
          properties: {
            filter: {
              type: 'string',
              description: 'Optional filter string to search block names',
            },
          },
        },
      },
      {
        name: 'remove_block',
        description: 'Remove a block (place air) at specific coordinates',
        inputSchema: {
          type: 'object',
          properties: {
            x: {
              type: 'number',
              description: 'X coordinate',
            },
            y: {
              type: 'number',
              description: 'Y coordinate (height)',
            },
            z: {
              type: 'number',
              description: 'Z coordinate',
            },
            worldName: {
              type: 'string',
              description: 'Name of the world (defaults to main world)',
              default: 'main',
            },
          },
          required: ['x', 'y', 'z'],
        },
      },
      {
        name: 'get_block_definition',
        description: 'Get the complete definition/properties of a block type by name',
        inputSchema: {
          type: 'object',
          properties: {
            blockName: {
              type: 'string',
              description: 'Name of the block to get definition for',
            },
          },
          required: ['blockName'],
        },
      },
      {
        name: 'get_player_position',
        description: 'Get the current position and rotation of the player',
        inputSchema: {
          type: 'object',
          properties: {
            worldName: {
              type: 'string',
              description: 'Name of the world (defaults to main world)',
              default: 'main',
            },
          },
        },
      },
    ];
  }

  /**
   * Handle create_block tool call
   */
  private async handleCreateBlock(args: any) {
    const { name, displayName, shape = 0, texture, solid = true, transparent = false, hardness = 1 } = args;

    // Add block to registry
    this.voxelServer.registry.addBlock({
      name,
      displayName,
      shape,
      texture,
      solid,
      transparent,
      hardness,
      miningtime: hardness * 1000,
      tool: 'any',
      unbreakable: false,
    });

    // Get the created block
    const blockType = this.voxelServer.registry.getBlock(name);

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({
            success: true,
            message: `Created block '${displayName}' (${name})`,
            blockType,
          }, null, 2),
        },
      ],
    };
  }

  /**
   * Handle place_block tool call
   */
  private async handlePlaceBlock(args: any) {
    const { blockName, x, y, z, worldName = 'main', modifier } = args;

    // Get block type
    const blockType = this.voxelServer.registry.getBlock(blockName);
    if (!blockType) {
      throw new Error(`Block '${blockName}' not found in registry`);
    }

    // Get block ID from palette
    const blockId = this.voxelServer.registry.getBlockID(blockName);
    if (blockId === undefined) {
      throw new Error(`Block '${blockName}' has no ID assigned in palette`);
    }

    // Get world
    const world = this.voxelServer.worldManager.get(worldName);
    if (!world) {
      throw new Error(`World '${worldName}' not found`);
    }

    // Place block (World API uses XYZ arrays)
    await world.setBlock([x, y, z], blockId);

    // Apply modifier if provided
    if (modifier) {
      await world.setBlockModifier([x, y, z], modifier);
    }

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({
            success: true,
            message: `Placed ${blockType.displayName} at (${x}, ${y}, ${z})${modifier ? ' with modifier' : ''}`,
            position: { x, y, z },
            blockId: blockId,
            modifier: modifier || null,
          }, null, 2),
        },
      ],
    };
  }

  /**
   * Handle get_block tool call
   */
  private async handleGetBlock(args: any) {
    const { x, y, z, worldName = 'main' } = args;

    // Get world
    const world = this.voxelServer.worldManager.get(worldName);
    if (!world) {
      throw new Error(`World '${worldName}' not found`);
    }

    // Get block (World API uses XYZ arrays)
    const blockId = await world.getBlock([x, y, z]);
    const blockType = this.voxelServer.registry.getBlockByID(blockId);

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({
            position: { x, y, z },
            blockId,
            blockType: blockType ? {
              name: blockType.name,
              displayName: blockType.displayName,
              shape: blockType.shape,
              texture: blockType.texture,
            } : null,
          }, null, 2),
        },
      ],
    };
  }

  /**
   * Handle list_blocks tool call
   */
  private async handleListBlocks(args: any) {
    const { filter } = args;

    // Get all blocks
    const blocksMap = this.voxelServer.registry.getAllBlocks();
    const blocks = Array.from(blocksMap.values());

    // Apply filter if provided
    const filteredBlocks = filter
      ? blocks.filter((b) =>
          b.name.toLowerCase().includes(filter.toLowerCase()) ||
          b.displayName?.toLowerCase().includes(filter.toLowerCase())
        )
      : blocks;

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({
            totalBlocks: blocks.length,
            filteredBlocks: filteredBlocks.length,
            blocks: filteredBlocks.map((b) => ({
              id: b.id,
              name: b.name,
              displayName: b.displayName,
              shape: b.shape,
              texture: b.texture,
            })),
          }, null, 2),
        },
      ],
    };
  }

  /**
   * Handle remove_block tool call
   */
  private async handleRemoveBlock(args: any) {
    const { x, y, z, worldName = 'main' } = args;

    // Get world
    const world = this.voxelServer.worldManager.get(worldName);
    if (!world) {
      throw new Error(`World '${worldName}' not found`);
    }

    // Place air (block ID 0, World API uses XYZ arrays)
    await world.setBlock([x, y, z], 0);

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({
            success: true,
            message: `Removed block at (${x}, ${y}, ${z})`,
            position: { x, y, z },
          }, null, 2),
        },
      ],
    };
  }

  /**
   * Handle get_block_definition tool call
   */
  private async handleGetBlockDefinition(args: any) {
    const { blockName } = args;

    // Get block type
    const blockType = this.voxelServer.registry.getBlock(blockName);
    if (!blockType) {
      throw new Error(`Block '${blockName}' not found in registry`);
    }

    // Get block ID
    const blockId = this.voxelServer.registry.getBlockID(blockName);

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({
            blockId,
            blockDefinition: {
              id: blockType.id,
              name: blockType.name,
              displayName: blockType.displayName,
              shape: blockType.shape,
              texture: blockType.texture,
              solid: blockType.solid,
              transparent: blockType.transparent,
              hardness: blockType.hardness,
              miningtime: blockType.miningtime,
              tool: blockType.tool,
              unbreakable: blockType.unbreakable,
              options: blockType.options,
            },
          }, null, 2),
        },
      ],
    };
  }

  /**
   * Handle get_player_position tool call
   */
  private async handleGetPlayerPosition(args: any) {
    const { worldName = 'main' } = args;

    // Try to get player position from live server via IPC
    if (this.ipcClient && this.ipcClient.isConnected()) {
      try {
        const playerData = await this.ipcClient.getPlayerPosition(worldName);

        if (!playerData) {
          throw new Error('No player found in the live server');
        }

        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                ...playerData,
                source: 'live_server',
              }, null, 2),
            },
          ],
        };
      } catch (error) {
        console.error('[MCP] Failed to get player position from live server:', error);
        throw error;
      }
    }

    // Fallback: Get from local VoxelServer instance
    const world = this.voxelServer.worldManager.get(worldName);
    if (!world) {
      throw new Error(`World '${worldName}' not found`);
    }

    const entities = this.voxelServer.entityManager.getAll();

    let playerEntity = null;
    for (const entity of entities.values()) {
      if (entity.type === 'player') {
        playerEntity = entity;
        break;
      }
    }

    if (!playerEntity) {
      throw new Error('No player found in the world');
    }

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({
            playerId: playerEntity.id,
            position: playerEntity.data.position,
            rotation: playerEntity.data.rotation,
            pitch: playerEntity.data.pitch,
            chunkId: playerEntity.chunkID,
            world: worldName,
            source: 'standalone',
          }, null, 2),
        },
      ],
    };
  }

  /**
   * Start the MCP server
   */
  async start(): Promise<void> {
    const transport = new StdioServerTransport();
    await this.server.connect(transport);
    console.log('[MCP] Nimbus MCP server started');
  }

  /**
   * Stop the MCP server
   */
  async stop(): Promise<void> {
    await this.server.close();
    console.log('[MCP] Nimbus MCP server stopped');
  }
}
