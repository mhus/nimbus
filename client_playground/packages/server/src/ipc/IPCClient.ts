/**
 * IPC Client for connecting to VoxelServer from external processes
 */

import * as net from 'net';
import type { IPCRequest, IPCResponse } from './IPCServer.js';

/**
 * IPC Client that connects to VoxelServer IPC socket
 */
export class IPCClient {
  private socket?: net.Socket;
  private socketPath: string;
  private connected = false;
  private requestId = 0;
  private pendingRequests = new Map<string, { resolve: (value: any) => void; reject: (error: Error) => void }>();
  private buffer = '';

  constructor(socketPath?: string) {
    this.socketPath = socketPath || '/tmp/nimbus-voxel-server.sock';
  }

  /**
   * Connect to the IPC server
   */
  async connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.socket = net.createConnection(this.socketPath, () => {
        this.connected = true;
        console.log('[IPC Client] Connected to VoxelServer');
        resolve();
      });

      this.socket.on('data', (data) => {
        this.handleData(data);
      });

      this.socket.on('close', () => {
        this.connected = false;
        console.log('[IPC Client] Disconnected from VoxelServer');
      });

      this.socket.on('error', (error) => {
        console.error('[IPC Client] Error:', error);
        this.connected = false;
        reject(error);
      });
    });
  }

  /**
   * Disconnect from the IPC server
   */
  disconnect(): void {
    if (this.socket) {
      this.socket.destroy();
      this.socket = undefined;
      this.connected = false;
    }
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.connected;
  }

  /**
   * Send request and wait for response
   */
  async request(method: string, params: any = {}): Promise<any> {
    if (!this.connected || !this.socket) {
      throw new Error('Not connected to IPC server');
    }

    const id = `req-${++this.requestId}`;
    const request: IPCRequest = { id, method, params };

    return new Promise((resolve, reject) => {
      this.pendingRequests.set(id, { resolve, reject });

      this.socket!.write(JSON.stringify(request) + '\n', (error) => {
        if (error) {
          this.pendingRequests.delete(id);
          reject(error);
        }
      });

      // Set timeout
      setTimeout(() => {
        if (this.pendingRequests.has(id)) {
          this.pendingRequests.delete(id);
          reject(new Error('Request timeout'));
        }
      }, 5000);
    });
  }

  /**
   * Handle incoming data
   */
  private handleData(data: Buffer): void {
    this.buffer += data.toString();

    // Process complete JSON messages (separated by newlines)
    const messages = this.buffer.split('\n');
    this.buffer = messages.pop() || ''; // Keep incomplete message in buffer

    for (const message of messages) {
      if (message.trim()) {
        this.handleResponse(message);
      }
    }
  }

  /**
   * Handle response message
   */
  private handleResponse(message: string): void {
    try {
      const response: IPCResponse = JSON.parse(message);
      const pending = this.pendingRequests.get(response.id);

      if (pending) {
        this.pendingRequests.delete(response.id);

        if (response.error) {
          pending.reject(new Error(response.error));
        } else {
          pending.resolve(response.result);
        }
      }
    } catch (error) {
      console.error('[IPC Client] Failed to parse response:', error);
    }
  }

  /**
   * Get player position from live server
   */
  async getPlayerPosition(worldName?: string): Promise<any> {
    return this.request('getPlayerPosition', { worldName });
  }

  /**
   * Get all players from live server
   */
  async getAllPlayers(): Promise<any[]> {
    return this.request('getAllPlayers');
  }

  /**
   * Get world info from live server
   */
  async getWorldInfo(worldName?: string): Promise<any> {
    return this.request('getWorldInfo', { worldName });
  }

  /**
   * Get entity count from live server
   */
  async getEntityCount(params?: { worldName?: string }): Promise<number> {
    return this.request('getEntityCount', params);
  }
}
