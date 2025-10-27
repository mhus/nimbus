/**
 * Main Voxel Server class
 */

import { WebSocketServer, WebSocket } from 'ws';
import { Registry } from './registry/Registry.js';
import { WorldManager } from './world/WorldManager.js';
import { EntityManager } from './entities/EntityManager.js';
import { FlatWorldGenerator } from './world/generators/FlatWorldGenerator.js';
import { NormalWorldGenerator } from './world/generators/NormalWorldGenerator.js';
import { AssetManager } from './assets/AssetManager.js';
import { AssetServer } from './assets/AssetServer.js';
import { NimbusMCPServer } from './mcp/NimbusMCPServer.js';
import { IPCServer } from './ipc/IPCServer.js';
import { ALL_DEFAULT_BLOCKS } from '@nimbus-client/core';
import { createRegistrySyncMessage, createAssetManifestMessage } from '@nimbus-client/protocol';
import type { World } from './world/World.js';
import * as path from 'path';

export interface ServerConfig {
  port: number;
  httpPort?: number;  // Optional HTTP port for assets (defaults to port + 1)
  worldName: string;
  worldSeed: number;
  generator: 'flat' | 'normal';
  assetsDir?: string;  // Optional assets directory (defaults to './assets')
  enableMCP?: boolean;  // Enable MCP server integration (defaults to false)
  enableIPC?: boolean;  // Enable IPC server for external communication (defaults to false)
}

/**
 * Main server class
 */
export class VoxelServer {
  private config: ServerConfig;
  private wss?: WebSocketServer;
  private assetServer?: AssetServer;
  private mcpServer?: NimbusMCPServer;
  private ipcServer?: IPCServer;

  readonly registry: Registry;
  readonly worldManager: WorldManager;
  readonly entityManager: EntityManager;
  readonly assetManager: AssetManager;

  private mainWorld?: World;
  private running = false;
  private clients: Set<WebSocket> = new Set();
  private playerData: Map<WebSocket, { x: number; y: number; z: number; rotationX: number; rotationY: number; rotationZ: number; worldName?: string }> = new Map();

  constructor(config: ServerConfig) {
    this.config = config;

    // Initialize systems
    this.registry = new Registry();
    this.worldManager = new WorldManager(this.registry);
    this.entityManager = new EntityManager();

    // Initialize asset manager
    const assetsDir = config.assetsDir || path.join(process.cwd(), 'assets');
    const httpPort = config.httpPort || config.port + 1;
    const baseUrl = `http://localhost:${httpPort}/assets`;
    this.assetManager = new AssetManager(assetsDir, baseUrl);

    // Register world generators
    this.worldManager.addGenerator('flat', FlatWorldGenerator);
    this.worldManager.addGenerator('normal', NormalWorldGenerator);

    // Register default blocks
    this.registerDefaultBlocks();

    // Register default items
    this.registerDefaultItems();
  }

  /**
   * Register default blocks from @nimbus-client/core
   */
  private registerDefaultBlocks(): void {
    console.log(`[Server] Registering ${ALL_DEFAULT_BLOCKS.length} default blocks...`);

    for (const blockType of ALL_DEFAULT_BLOCKS) {
      this.registry.addBlock({
        name: blockType.name,
        displayName: blockType.displayName,
        shape: blockType.shape,
        texture: blockType.texture,
        solid: blockType.solid !== false,
        transparent: blockType.transparent || false,
        hardness: blockType.hardness || 0,
        miningtime: blockType.miningtime || 0,
        tool: blockType.tool || 'any',
        unbreakable: blockType.unbreakable || false,
        options: blockType.options,
      });
    }

    console.log(`[Server] Registered ${ALL_DEFAULT_BLOCKS.length} blocks`);
  }

  /**
   * Register default items
   */
  private registerDefaultItems(): void {
    this.registry.addItem({
      id: 1,
      name: 'Stone',
      stackSize: 64,
      texture: 'stone',
      type: 'ItemBlock',
      block: 'stone',
    });

    this.registry.addItem({
      id: 2,
      name: 'Dirt',
      stackSize: 64,
      texture: 'dirt',
      type: 'ItemBlock',
      block: 'dirt',
    });

    this.registry.addItem({
      id: 3,
      name: 'Grass Block',
      stackSize: 64,
      texture: 'grass_top',
      type: 'ItemBlock',
      block: 'grass',
    });
  }

  /**
   * Start the server
   */
  async start(): Promise<void> {
    console.log('[Server] Starting VoxelSrv server...');

    // Initialize asset manager
    await this.assetManager.initialize();

    // Start asset HTTP server
    const httpPort = this.config.httpPort || this.config.port + 1;
    this.assetServer = new AssetServer(this.assetManager, httpPort, this.worldManager);
    await this.assetServer.start();

    // Finalize registry
    this.registry.loadPalette();
    this.registry.finalize();

    // Create or load main world
    let world = await this.worldManager.load(this.config.worldName);
    if (!world) {
      world = await this.worldManager.create({
        name: this.config.worldName,
        seed: this.config.worldSeed,
        generator: this.config.generator,
        chunkSize: 32,
        worldHeight: 256,
      });
    }

    if (!world) {
      throw new Error('Failed to create or load world');
    }

    this.mainWorld = world;

    // Start WebSocket server
    this.wss = new WebSocketServer({ port: this.config.port });

    this.wss.on('connection', (ws: WebSocket) => {
      this.handleConnection(ws);
    });

    this.running = true;

    // Start IPC server if enabled
    if (this.config.enableIPC) {
      this.ipcServer = new IPCServer(this);
      await this.ipcServer.start();
      console.log('[Server] IPC server enabled');
    }

    // Start MCP server if enabled
    if (this.config.enableMCP) {
      this.mcpServer = new NimbusMCPServer(this);
      await this.mcpServer.start();
      console.log('[Server] MCP server enabled');
    }

    console.log(`[Server] Server started on port ${this.config.port}`);
    console.log(`[Server] Asset HTTP server on port ${httpPort}`);
    console.log(`[Server] World: ${this.config.worldName} (seed: ${this.config.worldSeed})`);
    console.log(`[Server] Generator: ${this.config.generator}`);
  }

  /**
   * Stop the server
   */
  async stop(): Promise<void> {
    console.log('[Server] Stopping server...');

    this.running = false;

    // Stop MCP server if running
    if (this.mcpServer) {
      await this.mcpServer.stop();
    }

    // Stop IPC server if running
    if (this.ipcServer) {
      await this.ipcServer.stop();
    }

    // Close asset HTTP server
    if (this.assetServer) {
      await this.assetServer.stop();
    }

    // Close WebSocket server
    if (this.wss) {
      this.wss.close();
    }

    // Shutdown worlds
    await this.worldManager.shutdownAll();

    console.log('[Server] Server stopped');
  }

  /**
   * Handle new WebSocket connection
   */
  private handleConnection(ws: WebSocket): void {
    console.log('[Server] New client connected');

    // Add to clients set
    this.clients.add(ws);

    ws.on('message', (data: Buffer) => {
      this.handleMessage(ws, data);
    });

    ws.on('close', () => {
      console.log('[Server] Client disconnected');
      this.clients.delete(ws);
      this.playerData.delete(ws);

      // Remove player entity from world
      if (this.mainWorld) {
        const entities = this.mainWorld.getAllEntities();
        for (const [id, entity] of entities.entries()) {
          if (entity.client === ws && entity.type === 'player') {
            this.mainWorld.removeEntity(id);
            console.log(`[Server] Removed player entity ${id}`);
            break;
          }
        }
      }
    });

    ws.on('error', (error) => {
      console.error('[Server] WebSocket error:', error);
      this.clients.delete(ws);
      this.playerData.delete(ws);
    });

    // Send welcome message
    ws.send(JSON.stringify({
      type: 'welcome',
      message: 'Welcome to VoxelSrv!',
    }));

    // Send asset manifest
    this.sendAssetManifest(ws);

    // Send registry sync (dynamic block/item/entity definitions)
    this.sendRegistrySync(ws);
  }

  /**
   * Send asset manifest to client
   */
  private sendAssetManifest(ws: WebSocket): void {
    const manifest = this.assetManager.getManifest();
    const assetMessage = createAssetManifestMessage(manifest);

    ws.send(JSON.stringify(assetMessage));
    console.log(`[Server] Sent asset manifest: ${manifest.assets.length} assets`);
  }

  /**
   * Send registry synchronization to client
   */
  private sendRegistrySync(ws: WebSocket): void {
    const blockDefs = Array.from(this.registry.getAllBlocks().values());
    const items = Array.from(this.registry.getAllItems().values());

    // Convert BlockDefinition to BlockType for client
    const blocks = blockDefs.map(b => ({
      id: b.id,
      name: b.name,
      displayName: b.displayName,
      shape: b.shape,
      texture: b.texture,
      solid: b.solid,
      transparent: b.transparent,
      hardness: b.hardness,
      miningtime: b.miningtime,
      tool: b.tool,
      unbreakable: b.unbreakable,
      options: b.options,
    }));

    const registryMessage = createRegistrySyncMessage(
      blocks,
      items,
      [], // No entities yet
      '1.0.0'
    );

    ws.send(JSON.stringify(registryMessage));
    console.log(`[Server] Sent registry sync: ${blocks.length} blocks, ${items.length} items`);
  }

  /**
   * Handle world selection from client
   */
  private async handleWorldSelection(ws: WebSocket, worldName: string): Promise<void> {
    console.log(`[Server] Client requested world: ${worldName}`);

    try {
      // Check if world is already loaded
      let world: any = this.worldManager.get(worldName);

      if (!world) {
        // Try to load the world
        console.log(`[Server] Loading world: ${worldName}`);
        world = await this.worldManager.load(worldName);
      }

      if (!world) {
        // World doesn't exist
        console.error(`[Server] World "${worldName}" not found`);
        ws.send(JSON.stringify({
          type: 'error',
          message: `World "${worldName}" not found`,
        }));
        return;
      }

      // Update client's main world
      this.mainWorld = world;

      // Send confirmation
      ws.send(JSON.stringify({
        type: 'world_selected',
        world: worldName,
      }));

      console.log(`[Server] Client connected to world: ${worldName}`);
    } catch (error) {
      console.error('[Server] Error loading world:', error);
      ws.send(JSON.stringify({
        type: 'error',
        message: `Failed to load world: ${error instanceof Error ? error.message : String(error)}`,
      }));
    }
  }

  /**
   * Handle player position update
   */
  private handlePlayerPosition(ws: WebSocket, message: any): void {
    const { x, y, z, rotationX, rotationY, rotationZ } = message;

    // Store player position
    const currentData = this.playerData.get(ws);
    this.playerData.set(ws, {
      x,
      y,
      z,
      rotationX,
      rotationY,
      rotationZ,
      worldName: currentData?.worldName || 'main',
    });

    // Create or update player entity
    if (this.mainWorld) {
      // Find existing player entity for this client
      const entities = this.mainWorld.getAllEntities();
      let playerEntityId: string | null = null;

      for (const [id, entity] of entities.entries()) {
        if (entity.client === ws && entity.type === 'player') {
          playerEntityId = id;
          break;
        }
      }

      if (!playerEntityId) {
        // Create new player entity
        const entityId = `player_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        this.mainWorld.setEntity(entityId, {
          id: entityId,
          type: 'player',
          position: { x, y, z },
          rotation: { x: rotationX, y: rotationY, z: rotationZ },
          client: ws,
        });
        console.log(`[Server] Created player entity ${entityId} at (${x.toFixed(2)}, ${y.toFixed(2)}, ${z.toFixed(2)})`);
      } else {
        // Update existing player entity
        const entity = this.mainWorld.getEntity(playerEntityId);
        if (entity) {
          entity.position = { x, y, z };
          entity.rotation = { x: rotationX, y: rotationY, z: rotationZ };
          this.mainWorld.setEntity(playerEntityId, entity);
        }
      }
    }
  }

  /**
   * Handle incoming message
   */
  private handleMessage(ws: WebSocket, data: Buffer): void {
    try {
      const message = JSON.parse(data.toString());
      console.log('[Server] Received message:', message.type);

      // Handle different message types
      switch (message.type) {
        case 'ping':
          ws.send(JSON.stringify({ type: 'pong', time: Date.now() }));
          break;

        case 'select_world':
          this.handleWorldSelection(ws, message.world);
          break;

        case 'player_position':
          this.handlePlayerPosition(ws, message);
          break;

        case 'request_chunk':
          this.handleChunkRequest(ws, message.chunkX, message.chunkZ);
          break;

        case 'apply_block_changes':
          this.handleBlockChanges(ws, message.changes);
          break;

        default:
          console.warn('[Server] Unknown message type:', message.type);
      }
    } catch (error) {
      console.error('[Server] Failed to handle message:', error);
    }
  }

  /**
   * Handle chunk request
   */
  private async handleChunkRequest(ws: WebSocket, chunkX: number, chunkZ: number): Promise<void> {
    if (!this.mainWorld) return;

    try {
      console.log(`[Server] Chunk request: ${chunkX}, ${chunkZ}`);
      const chunk = await this.mainWorld.getChunk([chunkX, chunkZ]);

      console.log(`[Server] Sending chunk ${chunkX}, ${chunkZ} with ${chunk.data.length} blocks`);

      // Serialize modifiers if present
      let modifiersArray: any[] | undefined = undefined;
      if (chunk.modifiers) {
        if (chunk.modifiers instanceof Map) {
          if (chunk.modifiers.size > 0) {
            modifiersArray = Array.from(chunk.modifiers.entries()).map(([index, modifier]) => ({
              index,
              modifier
            }));
            console.log(`[Server] Sending ${modifiersArray.length} modifiers in chunk ${chunkX},${chunkZ}`);
          }
        } else {
          const entries = Object.entries(chunk.modifiers);
          if (entries.length > 0) {
            modifiersArray = entries.map(([index, modifier]) => ({
              index: parseInt(index),
              modifier
            }));
            console.log(`[Server] Sending ${modifiersArray.length} modifiers in chunk ${chunkX},${chunkZ}`);
          }
        }
      }

      // Serialize blockMetadata if present
      let blockMetadataArray: any[] | undefined = undefined;
      if (chunk.blockMetadata) {
        if (chunk.blockMetadata instanceof Map) {
          if (chunk.blockMetadata.size > 0) {
            blockMetadataArray = Array.from(chunk.blockMetadata.entries()).map(([index, metadata]) => ({
              index,
              metadata
            }));
            console.log(`[Server] Sending ${blockMetadataArray.length} block metadata in chunk ${chunkX},${chunkZ}`);
          }
        } else {
          const entries = Object.entries(chunk.blockMetadata);
          if (entries.length > 0) {
            blockMetadataArray = entries.map(([index, metadata]) => ({
              index: parseInt(index),
              metadata
            }));
            console.log(`[Server] Sending ${blockMetadataArray.length} block metadata in chunk ${chunkX},${chunkZ}`);
          }
        }
      }

      ws.send(JSON.stringify({
        type: 'chunk_data',
        chunkX,
        chunkZ,
        data: Array.from(chunk.data),  // Convert Uint16Array to regular array for JSON
        height: chunk.height,
        metadata: chunk.metadata ? Array.from(chunk.metadata) : undefined,
        edgeOffset: chunk.edgeOffset ? Array.from(chunk.edgeOffset) : undefined,
        modifiers: modifiersArray,
        blockMetadata: blockMetadataArray,
      }));

      console.log(`[Server] Sent chunk ${chunkX}, ${chunkZ}`);
    } catch (error) {
      console.error(`[Server] Failed to send chunk (${chunkX}, ${chunkZ}):`, error);
    }
  }

  /**
   * Handle block changes from client (Apply All)
   */
  private async handleBlockChanges(ws: WebSocket, changes: any[]): Promise<void> {
    if (!this.mainWorld) return;

    try {
      console.log(`[Server] Received ${changes.length} block changes from client`);

      // Collect affected chunks to broadcast updates
      const affectedChunks = new Set<string>();

      // Apply each block change to the world
      for (const change of changes) {
        const { x, y, z, blockId, modifier, metadata } = change;

        // Validate coordinates
        if (typeof x !== 'number' || typeof y !== 'number' || typeof z !== 'number' || typeof blockId !== 'number') {
          console.warn(`[Server] Invalid block change data:`, change);
          continue;
        }

        // DEBUG: Log block 94
        if (blockId === 94) {
          console.log(`[Server] 🔵 BLOCK 94 - Received from client at (${x}, ${y}, ${z})`);
          console.log(`[Server] 🔵 BLOCK 94 - Modifier:`, modifier ? JSON.stringify(modifier) : 'none');
        }

        console.log(`[Server] Setting block at (${x}, ${y}, ${z}) to ID ${blockId}`);

        // Apply to world (this will mark chunk as modified and auto-save)
        await this.mainWorld.setBlock([x, y, z], blockId);

        // Apply modifier if present
        if (modifier) {
          console.log(`[Server] Setting modifier for block at (${x}, ${y}, ${z}):`, modifier);
          await this.mainWorld.setBlockModifier([x, y, z], modifier);

          // DEBUG: Log block 94 modifier
          if (blockId === 94) {
            console.log(`[Server] 🔵 BLOCK 94 - Modifier set successfully`);
          }
        }

        // Apply metadata if present
        if (metadata) {
          console.log(`[Server] Setting metadata for block at (${x}, ${y}, ${z}):`, metadata);
          await this.mainWorld.setBlockMetadata([x, y, z], metadata);
        }

        // DEBUG: Verify block 94 was set
        if (blockId === 94) {
          const verifyBlock = await this.mainWorld.getBlock([x, y, z]);
          console.log(`[Server] 🔵 BLOCK 94 - Verification after setBlock: ${verifyBlock}`);
        }

        // Track affected chunk
        const chunkX = Math.floor(x / 32);
        const chunkZ = Math.floor(z / 32);
        affectedChunks.add(`${chunkX},${chunkZ}`);
      }

      console.log(`[Server] Applied ${changes.length} block changes, ${affectedChunks.size} chunks affected`);

      // Broadcast updated chunks to all clients
      for (const chunkKey of affectedChunks) {
        const [chunkX, chunkZ] = chunkKey.split(',').map(Number);
        await this.broadcastChunk(chunkX, chunkZ);
      }

      // Send success response to requesting client
      ws.send(JSON.stringify({
        type: 'block_changes_applied',
        success: true,
        count: changes.length,
        affectedChunks: affectedChunks.size,
      }));

      console.log(`[Server] Block changes applied and broadcast to all clients`);
    } catch (error) {
      console.error(`[Server] Failed to apply block changes:`, error);

      // Send error response
      ws.send(JSON.stringify({
        type: 'block_changes_applied',
        success: false,
        error: String(error),
      }));
    }
  }

  /**
   * Broadcast chunk update to all connected clients
   */
  private async broadcastChunk(chunkX: number, chunkZ: number): Promise<void> {
    if (!this.mainWorld) return;

    try {
      const chunk = await this.mainWorld.getChunk([chunkX, chunkZ]);

      // DEBUG: Check if chunk contains block 94
      const chunkData = Array.from(chunk.data);
      const block94Count = chunkData.filter(id => id === 94).length;
      if (block94Count > 0) {
        console.log(`[Server] 🔵 BLOCK 94 - Broadcasting chunk ${chunkX},${chunkZ} with ${block94Count} blocks of type 94`);

        // Find positions of block 94
        for (let i = 0; i < chunkData.length; i++) {
          if (chunkData[i] === 94) {
            // Correct index formula: index = x + y * 32 + z * 32 * 256
            const localX = i % 32;
            const localY = Math.floor((i / 32) % 256);
            const localZ = Math.floor(i / (32 * 256));
            const worldX = chunkX * 32 + localX;
            const worldZ = chunkZ * 32 + localZ;
            console.log(`[Server] 🔵 BLOCK 94 - Found at world pos (${worldX}, ${localY}, ${worldZ}), chunk local (${localX}, ${localY}, ${localZ}), index ${i}`);
          }
        }
      }

      // Serialize modifiers if present
      let modifiersArray: any[] | undefined = undefined;
      if (chunk.modifiers) {
        if (chunk.modifiers instanceof Map) {
          if (chunk.modifiers.size > 0) {
            modifiersArray = Array.from(chunk.modifiers.entries()).map(([index, modifier]) => ({
              index,
              modifier
            }));
            console.log(`[Server] Broadcasting ${modifiersArray.length} modifiers in chunk ${chunkX},${chunkZ}`);
          }
        } else {
          const entries = Object.entries(chunk.modifiers);
          if (entries.length > 0) {
            modifiersArray = entries.map(([index, modifier]) => ({
              index: parseInt(index),
              modifier
            }));
            console.log(`[Server] Broadcasting ${modifiersArray.length} modifiers in chunk ${chunkX},${chunkZ}`);
          }
        }
      }

      // Serialize blockMetadata if present
      let blockMetadataArray: any[] | undefined = undefined;
      if (chunk.blockMetadata) {
        if (chunk.blockMetadata instanceof Map) {
          if (chunk.blockMetadata.size > 0) {
            blockMetadataArray = Array.from(chunk.blockMetadata.entries()).map(([index, metadata]) => ({
              index,
              metadata
            }));
            console.log(`[Server] Broadcasting ${blockMetadataArray.length} block metadata in chunk ${chunkX},${chunkZ}`);
          }
        } else {
          const entries = Object.entries(chunk.blockMetadata);
          if (entries.length > 0) {
            blockMetadataArray = entries.map(([index, metadata]) => ({
              index: parseInt(index),
              metadata
            }));
            console.log(`[Server] Broadcasting ${blockMetadataArray.length} block metadata in chunk ${chunkX},${chunkZ}`);
          }
        }
      }

      const message = JSON.stringify({
        type: 'chunk_data',
        chunkX,
        chunkZ,
        data: chunkData,
        height: chunk.height,
        metadata: chunk.metadata ? Array.from(chunk.metadata) : undefined,
        edgeOffset: chunk.edgeOffset ? Array.from(chunk.edgeOffset) : undefined,
        modifiers: modifiersArray,
        blockMetadata: blockMetadataArray,
      });

      // Send to all connected clients
      let sentCount = 0;
      for (const client of this.clients) {
        if (client.readyState === 1) { // WebSocket.OPEN = 1
          client.send(message);
          sentCount++;
        }
      }

      console.log(`[Server] Broadcast chunk ${chunkX},${chunkZ} to ${sentCount} clients`);
    } catch (error) {
      console.error(`[Server] Failed to broadcast chunk (${chunkX}, ${chunkZ}):`, error);
    }
  }
}
