/**
 * IPC Server for communication between VoxelServer and external processes (e.g., MCP standalone)
 * Uses Unix domain socket for fast local communication
 */

import * as net from 'net';
import * as fs from 'fs';
import * as path from 'path';
import type { VoxelServer } from '../VoxelServer.js';

export interface IPCRequest {
  id: string;
  method: string;
  params: any;
}

export interface IPCResponse {
  id: string;
  result?: any;
  error?: string;
}

/**
 * IPC Server that allows external processes to query VoxelServer state
 */
export class IPCServer {
  private server: net.Server;
  private voxelServer: VoxelServer;
  private socketPath: string;
  private clients: Set<net.Socket> = new Set();

  constructor(voxelServer: VoxelServer, socketPath?: string) {
    this.voxelServer = voxelServer;
    this.socketPath = socketPath || '/tmp/nimbus-voxel-server.sock';
    this.server = net.createServer(this.handleConnection.bind(this));
  }

  /**
   * Start the IPC server
   */
  async start(): Promise<void> {
    // Remove existing socket file if it exists
    if (fs.existsSync(this.socketPath)) {
      fs.unlinkSync(this.socketPath);
    }

    return new Promise((resolve, reject) => {
      this.server.listen(this.socketPath, () => {
        console.log(`[IPC] Server listening on ${this.socketPath}`);
        resolve();
      });

      this.server.on('error', (error) => {
        console.error('[IPC] Server error:', error);
        reject(error);
      });
    });
  }

  /**
   * Stop the IPC server
   */
  async stop(): Promise<void> {
    return new Promise((resolve) => {
      // Close all client connections
      for (const client of this.clients) {
        client.destroy();
      }
      this.clients.clear();

      this.server.close(() => {
        // Remove socket file
        if (fs.existsSync(this.socketPath)) {
          fs.unlinkSync(this.socketPath);
        }
        console.log('[IPC] Server stopped');
        resolve();
      });
    });
  }

  /**
   * Handle new client connection
   */
  private handleConnection(socket: net.Socket): void {
    console.log('[IPC] Client connected');
    this.clients.add(socket);

    let buffer = '';

    socket.on('data', (data) => {
      buffer += data.toString();

      // Process complete JSON messages (separated by newlines)
      const messages = buffer.split('\n');
      buffer = messages.pop() || ''; // Keep incomplete message in buffer

      for (const message of messages) {
        if (message.trim()) {
          this.handleMessage(socket, message);
        }
      }
    });

    socket.on('close', () => {
      console.log('[IPC] Client disconnected');
      this.clients.delete(socket);
    });

    socket.on('error', (error) => {
      console.error('[IPC] Socket error:', error);
      this.clients.delete(socket);
    });
  }

  /**
   * Handle incoming IPC request
   */
  private async handleMessage(socket: net.Socket, message: string): Promise<void> {
    try {
      const request: IPCRequest = JSON.parse(message);
      const response = await this.processRequest(request);
      socket.write(JSON.stringify(response) + '\n');
    } catch (error) {
      console.error('[IPC] Failed to process message:', error);
      const errorResponse: IPCResponse = {
        id: 'unknown',
        error: error instanceof Error ? error.message : String(error),
      };
      socket.write(JSON.stringify(errorResponse) + '\n');
    }
  }

  /**
   * Process IPC request and return response
   */
  private async processRequest(request: IPCRequest): Promise<IPCResponse> {
    try {
      let result: any;

      switch (request.method) {
        case 'getPlayerPosition':
          result = await this.getPlayerPosition(request.params);
          break;

        case 'getAllPlayers':
          result = await this.getAllPlayers();
          break;

        case 'getWorldInfo':
          result = await this.getWorldInfo(request.params);
          break;

        case 'getEntityCount':
          result = await this.getEntityCount();
          break;

        default:
          throw new Error(`Unknown method: ${request.method}`);
      }

      return {
        id: request.id,
        result,
      };
    } catch (error) {
      return {
        id: request.id,
        error: error instanceof Error ? error.message : String(error),
      };
    }
  }

  /**
   * Get player position
   */
  private async getPlayerPosition(params: { worldName?: string }): Promise<any> {
    const worldName = params.worldName || 'main';
    const world = this.voxelServer.worldManager.get(worldName);

    if (!world) {
      throw new Error(`World "${worldName}" not found`);
    }

    // Get all entities
    const entities = this.voxelServer.entityManager.getAll();

    // Find player entity (type: 'player')
    let playerEntity = null;
    for (const entity of entities.values()) {
      if (entity.type === 'player') {
        playerEntity = entity;
        break;
      }
    }

    if (!playerEntity) {
      return null;
    }

    return {
      playerId: playerEntity.id,
      position: playerEntity.data.position,
      rotation: playerEntity.data.rotation,
      pitch: playerEntity.data.pitch,
      chunkId: playerEntity.chunkID,
      world: worldName,
    };
  }

  /**
   * Get all players
   */
  private async getAllPlayers(): Promise<any[]> {
    const entities = this.voxelServer.entityManager.getAll();
    const players: any[] = [];

    for (const entity of entities.values()) {
      if (entity.type === 'player') {
        players.push({
          playerId: entity.id,
          position: entity.data.position,
          rotation: entity.data.rotation,
          pitch: entity.data.pitch,
          chunkId: entity.chunkID,
        });
      }
    }

    return players;
  }

  /**
   * Get world info
   */
  private async getWorldInfo(params: { worldName?: string }): Promise<any> {
    const worldName = params.worldName || 'main';
    const world = this.voxelServer.worldManager.get(worldName);

    if (!world) {
      throw new Error(`World "${worldName}" not found`);
    }

    // Access metadata through the public getter or return available info
    return {
      name: worldName,
      loaded: true,
    };
  }

  /**
   * Get entity count
   */
  private async getEntityCount(): Promise<number> {
    return this.voxelServer.entityManager.getAll().size;
  }
}
