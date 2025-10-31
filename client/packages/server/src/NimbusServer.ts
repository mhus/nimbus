/**
 * NimbusServer - Test server for Nimbus Client development
 */

import express from 'express';
import cors from 'cors';
import { WebSocketServer } from 'ws';
import { SHARED_VERSION, getLogger, ExceptionHandler, LoggerFactory, LogLevel, type Block } from '@nimbus/shared';
import { loadServerConfig } from './config/ServerConfig';
import { WorldManager } from './world/WorldManager';
import { TerrainGenerator } from './world/TerrainGenerator';
import { createAuthMiddleware } from './api/middleware/auth';
import { createWorldRoutes } from './api/routes/worldRoutes';
import { createAssetRoutes } from './api/routes/assetRoutes';
import { getChunkKey } from './types/ServerTypes';
import type { ClientSession } from './types/ServerTypes';
import { BlockUpdateBuffer } from './network/BlockUpdateBuffer';

const SERVER_VERSION = '2.0.0';
const logger = getLogger('NimbusServer');

// Configure logging
LoggerFactory.setDefaultLevel(LogLevel.DEBUG);

class NimbusServer {
  private static instance: NimbusServer | null = null;

  private app: express.Application;
  private wsServer: WebSocketServer | null = null;
  private worldManager: WorldManager;
  private terrainGenerator: TerrainGenerator;
  private sessions = new Map<string, ClientSession>();
  private blockUpdateBuffer: BlockUpdateBuffer;

  private constructor() {
    this.app = express();
    this.worldManager = new WorldManager();
    this.terrainGenerator = new TerrainGenerator();

    // Initialize block update buffer with broadcast callback
    this.blockUpdateBuffer = new BlockUpdateBuffer((worldId, blocks) => {
      this.broadcastBlockUpdates(worldId, blocks);
    });
  }

  /**
   * Get singleton instance
   */
  static getInstance(): NimbusServer {
    if (!NimbusServer.instance) {
      NimbusServer.instance = new NimbusServer();
    }
    return NimbusServer.instance;
  }

  /**
   * Get block update buffer (for REST endpoints)
   */
  getBlockUpdateBuffer(): BlockUpdateBuffer {
    return this.blockUpdateBuffer;
  }

  async initialize() {
    try {
      logger.info(`Nimbus Server v${SERVER_VERSION} (Shared v${SHARED_VERSION})`);

      const config = loadServerConfig();

      // Setup Express middleware
      this.app.use(express.json());
      if (config.cors) {
        // Allow all origins in development
        this.app.use(cors({
          origin: true, // Allow all origins
          credentials: true, // Allow credentials
        }));
      }

      // Auth middleware
      const authMiddleware = createAuthMiddleware(config);

      // REST API routes
      // Note: Management routes (GET/POST/PUT/DELETE for assets metadata) require auth
      // Asset file serving (GET /assets/*) routes are public
      this.app.use('/api/worlds', authMiddleware, createAssetRoutes(this.worldManager));
      this.app.use('/api/worlds', authMiddleware, createWorldRoutes(this.worldManager, this.blockUpdateBuffer));

      // Health check
      this.app.get('/health', (_req, res) => res.json({ status: 'ok', version: SERVER_VERSION }));

      // Start Express server
      const server = this.app.listen(config.port, config.host, () => {
        logger.info(`REST API listening on http://${config.host}:${config.port}`);
      });

      // Setup WebSocket server
      this.wsServer = new WebSocketServer({ server });
      this.setupWebSocket(config);

      logger.info('Server initialized successfully');
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'NimbusServer.initialize');
    }
  }

  private setupWebSocket(config: any) {
    if (!this.wsServer) return;

    this.wsServer.on('connection', (ws) => {
      const sessionId = Math.random().toString(36).substring(7);
      const session: ClientSession = {
        sessionId,
        ws,
        registeredChunks: new Set(),
        lastPingAt: Date.now(),
        createdAt: Date.now(),
        isAuthenticated: false,
      };
      this.sessions.set(sessionId, session);

      logger.info(`Client connected: ${sessionId}`);

      ws.on('message', (data) => {
        try {
          const message = JSON.parse(data.toString());
          this.handleMessage(session, message);
        } catch (error) {
          logger.error('Failed to parse message', {}, error as Error);
        }
      });

      ws.on('close', () => {
        this.sessions.delete(sessionId);
        logger.info(`Client disconnected: ${sessionId}`);
      });
    });

    // Ping interval
    setInterval(() => {
      this.sessions.forEach((session) => {
        if (Date.now() - session.lastPingAt > config.pingInterval * 1000 + 10000) {
          logger.warn(`Session timeout: ${session.sessionId}`);
          session.ws.close();
        }
      });
    }, 5000);

    logger.info('WebSocket server ready');
  }

  private handleMessage(session: ClientSession, message: any) {
    const { t, i, d } = message;

    switch (t) {
      case 'login':
        this.handleLogin(session, i, d);
        break;
      case 'p': // Ping
        session.lastPingAt = Date.now();
        session.ws.send(JSON.stringify({ r: i, t: 'p' }));
        break;
      case 'c.r': // Chunk registration
        this.handleChunkRegistration(session, d);
        break;
      case 'c.q': // Chunk query
        this.handleChunkQuery(session, d);
        break;
      default:
        logger.warn(`Unknown message type: ${t}`);
    }
  }

  private handleLogin(session: ClientSession, messageId: string, data: any) {
    session.isAuthenticated = true;
    session.userId = data.username;
    session.username = data.username;
    session.displayName = data.username;
    session.worldId = data.worldId;

    const world = this.worldManager.getWorld(data.worldId);

    session.ws.send(JSON.stringify({
      r: messageId,
      t: 'loginResponse',
      d: {
        success: true,
        userId: session.userId,
        displayName: session.displayName,
        worldInfo: world ? world.worldInfo : null,
        sessionId: session.sessionId,
      },
    }));

    logger.info(`Login successful: ${session.username} in world ${data.worldId}`);
  }

  private handleChunkRegistration(session: ClientSession, data: any) {
    const chunks = data.c || [];
    session.registeredChunks.clear();

    chunks.forEach((coord: any) => {
      const cx = coord.cx ?? coord.x;
      const cz = coord.cz ?? coord.z;
      const key = getChunkKey(cx, cz);
      session.registeredChunks.add(key);
    });

    logger.debug(`Registered ${chunks.length} chunks for ${session.username}`);

    // Send chunks
    this.sendChunks(session, chunks);
  }

  private handleChunkQuery(session: ClientSession, data: any) {
    const chunks = data.c || [];
    this.sendChunks(session, chunks);
  }

  private sendChunks(session: ClientSession, coords: any[]) {
    if (!session.worldId) return;

    const world = this.worldManager.getWorld(session.worldId);
    if (!world) return;

    const chunkData = coords.map((coord: any) => {
      const cx = coord.cx ?? coord.x;
      const cz = coord.cz ?? coord.z;
      const key = getChunkKey(cx, cz);

      // Get or generate chunk
      let chunk = world.chunks.get(key);
      if (!chunk) {
        if (!world.generator) {
          logger.error('World has no generator', { worldId: world.worldId });
          return null;
        }
        chunk = world.generator.generateChunk(cx, cz, world.chunkSize);
        world.chunks.set(key, chunk);
      }

      // Convert ServerChunk to ChunkData
      const chunkData = chunk.toChunkData();

      // Use Block type directly from @nimbus/shared (no compression)
      return {
        cx,
        cz,
        b: chunkData.blocks, // Use blocks as-is (already in correct Block format)
        h: chunkData.heightData
          ? Array.from({ length: world.chunkSize * world.chunkSize }, (_, i) => {
              const offset = i * 4;
              return [
                chunkData.heightData![offset],
                chunkData.heightData![offset + 1],
                chunkData.heightData![offset + 2],
                chunkData.heightData![offset + 3],
              ] as [number, number, number, number];
            })
          : undefined,
      };
    });

    session.ws.send(JSON.stringify({
      t: 'c.u',
      d: chunkData,
    }));

    logger.debug(`Sent ${chunkData.length} chunks to ${session.username}`);
  }

  /**
   * Broadcast block updates to all clients that have the affected chunks registered
   *
   * @param worldId World ID
   * @param blocks Blocks to broadcast
   */
  private broadcastBlockUpdates(worldId: string, blocks: Block[]): void {
    try {
      if (blocks.length === 0) {
        return;
      }

      // Get world to access chunk size
      const world = this.worldManager.getWorld(worldId);
      if (!world) {
        logger.warn('Cannot broadcast block updates: world not found', { worldId });
        return;
      }

      // Group blocks by chunk for efficient filtering
      const blocksByChunk = new Map<string, Block[]>();
      for (const block of blocks) {
        const cx = Math.floor(block.position.x / world.chunkSize);
        const cz = Math.floor(block.position.z / world.chunkSize);
        const chunkKey = getChunkKey(cx, cz);

        let chunkBlocks = blocksByChunk.get(chunkKey);
        if (!chunkBlocks) {
          chunkBlocks = [];
          blocksByChunk.set(chunkKey, chunkBlocks);
        }
        chunkBlocks.push(block);
      }

      logger.debug('Broadcasting block updates', {
        worldId,
        totalBlocks: blocks.length,
        affectedChunks: blocksByChunk.size,
      });

      // Send updates to relevant clients
      let clientCount = 0;
      for (const [sessionId, session] of this.sessions) {
        // Skip if not in this world
        if (session.worldId !== worldId) {
          continue;
        }

        // Collect blocks for this client (only chunks they have registered)
        const clientBlocks: Block[] = [];
        for (const [chunkKey, chunkBlocks] of blocksByChunk) {
          if (session.registeredChunks.has(chunkKey)) {
            clientBlocks.push(...chunkBlocks);
          }
        }

        // Send if client has any relevant blocks
        if (clientBlocks.length > 0) {
          try {
            session.ws.send(
              JSON.stringify({
                t: 'b.u',
                d: clientBlocks,
              })
            );
            clientCount++;

            logger.debug('Sent block updates to client', {
              sessionId,
              username: session.username,
              blockCount: clientBlocks.length,
            });
          } catch (error) {
            logger.error('Failed to send block updates to client', { sessionId }, error as Error);
          }
        }
      }

      logger.debug('Block updates broadcast complete', {
        worldId,
        clientCount,
        totalBlocks: blocks.length,
      });
    } catch (error) {
      ExceptionHandler.handle(error, 'NimbusServer.broadcastBlockUpdates', { worldId });
    }
  }
}

// Start server (using singleton)
const server = NimbusServer.getInstance();
server.initialize().catch((error) => {
  logger.fatal('Failed to start server', {}, error);
  process.exit(1);
});

// Graceful shutdown
process.on('SIGINT', () => {
  logger.info('Shutting down server...');
  process.exit(0);
});
