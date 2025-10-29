/**
 * NimbusServer - Test server for Nimbus Client development
 */

import express from 'express';
import cors from 'cors';
import { WebSocketServer } from 'ws';
import { SHARED_VERSION, getLogger, ExceptionHandler, LoggerFactory, LogLevel } from '@nimbus/shared';
import { loadServerConfig } from './config/ServerConfig';
import { WorldManager } from './world/WorldManager';
import { TerrainGenerator } from './world/TerrainGenerator';
import { createAuthMiddleware } from './api/middleware/auth';
import { createWorldRoutes } from './api/routes/worldRoutes';
import { createAssetRoutes } from './api/routes/assetRoutes';
import { getChunkKey } from './types/ServerTypes';
import type { ClientSession } from './types/ServerTypes';

const SERVER_VERSION = '2.0.0';
const logger = getLogger('NimbusServer');

// Configure logging
LoggerFactory.setDefaultLevel(LogLevel.DEBUG);

class NimbusServer {
  private app: express.Application;
  private wsServer: WebSocketServer | null = null;
  private worldManager: WorldManager;
  private terrainGenerator: TerrainGenerator;
  private sessions = new Map<string, ClientSession>();

  constructor() {
    this.app = express();
    this.worldManager = new WorldManager();
    this.terrainGenerator = new TerrainGenerator();
  }

  async initialize() {
    try {
      logger.info(`Nimbus Server v${SERVER_VERSION} (Shared v${SHARED_VERSION})`);

      const config = loadServerConfig();

      // Setup Express middleware
      this.app.use(express.json());
      if (config.cors) {
        this.app.use(cors({ origin: config.corsOrigins }));
      }

      // Auth middleware
      const authMiddleware = createAuthMiddleware(config);

      // REST API routes
      // Note: Asset routes must be registered BEFORE auth middleware (public access)
      this.app.use('/api/worlds', createAssetRoutes());
      this.app.use('/api/worlds', authMiddleware, createWorldRoutes(this.worldManager));

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
        worldInfo: world ? {
          worldId: world.worldId,
          name: world.name,
          description: world.description,
          chunkSize: world.chunkSize,
          ...world.dimensions,
          seaLevel: world.seaLevel,
          groundLevel: world.groundLevel,
          status: world.status,
          assetPath: `/api/worlds/${world.worldId}/assets`,
          // assetPort can be omitted (uses same server port)
        } : null,
        sessionId: session.sessionId,
      },
    }));

    logger.info(`Login successful: ${session.username} in world ${data.worldId}`);
  }

  private handleChunkRegistration(session: ClientSession, data: any) {
    const chunks = data.c || [];
    session.registeredChunks.clear();

    chunks.forEach((coord: any) => {
      const key = getChunkKey(coord.x, coord.z);
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
      const cx = coord.x;
      const cz = coord.z;
      const key = getChunkKey(cx, cz);

      // Get or generate chunk
      let chunk = world.chunks.get(key);
      if (!chunk) {
        chunk = this.terrainGenerator.generateChunk(cx, cz, world.chunkSize);
        world.chunks.set(key, chunk);
      }

      // Convert ServerChunk to ChunkData, then to transfer format
      const chunkData = chunk.toChunkData();

      // Map blocks to BlockData format (network protocol)
      const blockData = chunkData.blocks.map(block => ({
        x: block.position.x,
        y: block.position.y,
        z: block.position.z,
        bt: block.blockTypeId,
        s: block.status || 0,
        mi: block.offsets,
        d: block.metadata,
      }));

      return {
        cx,
        cz,
        b: blockData,
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
}

// Start server
const server = new NimbusServer();
server.initialize().catch((error) => {
  logger.fatal('Failed to start server', {}, error);
  process.exit(1);
});

// Graceful shutdown
process.on('SIGINT', () => {
  logger.info('Shutting down server...');
  process.exit(0);
});
