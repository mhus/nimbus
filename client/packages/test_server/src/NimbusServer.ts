/**
 * NimbusServer - Test server for Nimbus Client development
 */

import express from 'express';
import cors from 'cors';
import { WebSocketServer } from 'ws';
import { SHARED_VERSION, getLogger, ExceptionHandler, LoggerFactory, LogLevel, type Block, type HeightData, MessageType, type EntityPathway, type Vector2 } from '@nimbus/shared';
import { loadServerConfig } from './config/ServerConfig';
import { WorldManager } from './world/WorldManager';
import { TerrainGenerator } from './world/TerrainGenerator';
import { createAuthMiddleware } from './api/middleware/auth';
import { createWorldRoutes } from './api/routes/worldRoutes';
import { createAssetRoutes } from './api/routes/assetRoutes';
import { createBackdropRoutes } from './api/routes/backdropRoutes';
import { createEntityRoutes } from './api/routes/entityRoutes';
import { getChunkKey } from './types/ServerTypes';
import type { ClientSession } from './types/ServerTypes';
import { BlockUpdateBuffer } from './network/BlockUpdateBuffer';
import { CommandService } from './commands/CommandService';
import { HelpCommand } from './commands/HelpCommand';
import { LoopCommand } from './commands/LoopCommand';
import { SetSelectedEditBlockCommand } from './commands/SetSelectedEditBlockCommand';
import { NavigateSelectedBlockCommand } from './commands/NavigateSelectedBlockCommand';
import { EntityManager } from './entity/EntityManager';
import { EntitySimulator } from './entity/EntitySimulator';

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
  private commandService: CommandService;
  private entityManager: EntityManager | null = null;
  private entitySimulator: EntitySimulator | null = null;

  private constructor() {
    this.app = express();
    this.worldManager = new WorldManager();
    this.terrainGenerator = new TerrainGenerator();

    // Initialize block update buffer with broadcast callback
    this.blockUpdateBuffer = new BlockUpdateBuffer((worldId, blocks) => {
      this.broadcastBlockUpdates(worldId, blocks);
    });

    // Initialize command service
    this.commandService = new CommandService();

    // Register command handlers
    this.commandService.registerHandler(new HelpCommand(this.commandService));
    this.commandService.registerHandler(new LoopCommand());
    this.commandService.registerHandler(new SetSelectedEditBlockCommand(this.worldManager, this.blockUpdateBuffer));
    this.commandService.registerHandler(new NavigateSelectedBlockCommand());
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

      // Initialize EntityManager and EntitySimulator
      const worldId = 'main'; // TODO: Get from active world
      this.entityManager = new EntityManager(config.dataPath, worldId);
      this.entitySimulator = new EntitySimulator(
        `${config.dataPath}/worlds/${worldId}`,
        1000 // Update every second
      );

      // Set WorldManager for ground height calculation
      this.entitySimulator.setWorldManager(this.worldManager);

      // Set EntityManager for model access (maxPitch, etc.)
      this.entitySimulator.setEntityManager(this.entityManager);

      // Register pathway callback
      this.entitySimulator.onPathwayGenerated((pathway) => {
        this.queuePathwayForClients(pathway);
      });

      // Start entity simulator
      this.entitySimulator.start();

      // Load entities from spawn definitions into EntityManager
      const spawnDefinitions = this.entitySimulator.getAllSpawnDefinitions();
      for (const spawnDef of spawnDefinitions) {
        // Create Entity instance from spawn definition
        // Read physics flags from spawn definition if available
        const spawnDefAny = spawnDef as any;
        const entity = {
          id: spawnDef.entityId,
          name: spawnDef.entityId, // Use ID as name for now
          model: spawnDef.entityModelId,
          modelModifier: {},
          movementType: 'passive' as const,
          solid: false,
          interactive: false,
          physics: spawnDefAny.physics ?? false, // Read from spawn definition
          clientPhysics: spawnDefAny.clientPhysics ?? false, // Read from spawn definition
        };
        this.entityManager.addEntity(entity);
        logger.info('Entity loaded from spawn definition', {
          entityId: entity.id,
          physics: entity.physics,
          clientPhysics: entity.clientPhysics
        });
      }
      logger.info('Entities loaded from spawn definitions', { count: spawnDefinitions.length });

      // Start pathway broadcast interval (every 100ms)
      setInterval(() => {
        this.broadcastPathways();
      }, 100);

      // REST API routes
      // Note: Management routes (GET/POST/PUT/DELETE for assets metadata) require auth
      // Asset file serving (GET /assets/*) routes are public
      this.app.use('/api/worlds', authMiddleware, createAssetRoutes(this.worldManager));
      this.app.use('/api/worlds', authMiddleware, createWorldRoutes(this.worldManager, this.blockUpdateBuffer, this.sessions));

      // Entity routes
      if (this.entityManager) {
        this.app.use('/api/worlds', authMiddleware, createEntityRoutes(this.entityManager));
      }

      // Backdrop configuration routes (public - no auth needed)
      this.app.use('/api/backdrop', createBackdropRoutes());

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
        entityPathwayQueue: [],
      };
      this.sessions.set(sessionId, session);

      logger.info(`Client connected: ${sessionId}`);

      ws.on('message', async (data) => {
        try {
          const message = JSON.parse(data.toString());
          await this.handleMessage(session, message);
        } catch (error) {
          logger.error('Failed to parse message', {}, error as Error);
        }
      });

      ws.on('close', () => {
        this.sessions.delete(sessionId);
        logger.info(`Client disconnected: ${sessionId}`);
      });
    });

    // Ping interval - DISABLED for debugging
    // TODO: Re-enable after debugging block updates
    /*
    setInterval(() => {
      this.sessions.forEach((session) => {
        if (Date.now() - session.lastPingAt > config.pingInterval * 1000 + 10000) {
          logger.warn(`Session timeout: ${session.sessionId}`);
          session.ws.close();
        }
      });
    }, 5000);
    */
    logger.info('Session timeout check DISABLED for debugging');

    logger.info('WebSocket server ready');
  }

  private async handleMessage(session: ClientSession, message: any) {
    const { t, i, d } = message;

    switch (t) {
      case 'login':
        this.handleLogin(session, i, d);
        break;
      case 'p': // Ping
        session.lastPingAt = Date.now();
        // Send pong with client timestamp (echoed) and server timestamp
        const pongData = {
          cTs: d?.cTs ?? Date.now(), // Echo client timestamp
          sTs: Date.now(), // Server timestamp
        };
        session.ws.send(JSON.stringify({ r: i, t: 'p', d: pongData }));
        break;
      case 'c.r': // Chunk registration
        await this.handleChunkRegistration(session, d);
        break;
      case 'c.q': // Chunk query
        await this.handleChunkQuery(session, d);
        break;
      case 'cmd': // Command execution
        this.handleCommand(session, i, d);
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

  private async handleChunkRegistration(session: ClientSession, data: any) {
    const chunks = data.c || [];

    // Track old chunks before clearing
    const oldChunks = new Set(session.registeredChunks);
    session.registeredChunks.clear();

    const newChunks: Array<{ cx: number; cz: number }> = [];

    chunks.forEach((coord: any) => {
      const cx = coord.cx ?? coord.x;
      const cz = coord.cz ?? coord.z;
      const key = getChunkKey(cx, cz);

      // Check if this is a NEW chunk (not previously registered)
      if (!oldChunks.has(key)) {
        newChunks.push({ cx, cz });
      }

      session.registeredChunks.add(key);
    });

    logger.debug(`Registered ${chunks.length} chunks for ${session.username}`, {
      newChunks: newChunks.length,
    });

    // Send chunks
    await this.sendChunks(session, chunks);

    // Queue pathways for newly registered chunks
    if (newChunks.length > 0 && this.entitySimulator) {
      const pathwaysToSend = new Map<string, EntityPathway>();

      for (const { cx, cz } of newChunks) {
        const pathways = this.entitySimulator.getPathwaysForChunk(cx, cz);
        for (const pathway of pathways) {
          // Use Map to deduplicate (entity might be in multiple chunks)
          pathwaysToSend.set(pathway.entityId, pathway);
        }
      }

      if (pathwaysToSend.size > 0) {
        // Add to queue (will be sent in next broadcast cycle - max 100ms delay)
        session.entityPathwayQueue.push(...pathwaysToSend.values());

        logger.info('Queued entity pathways for newly registered chunks', {
          sessionId: session.sessionId,
          newChunks: newChunks.length,
          pathwayCount: pathwaysToSend.size,
        });
      }
    }
  }

  private async handleChunkQuery(session: ClientSession, data: any) {
    const chunks = data.c || [];
    await this.sendChunks(session, chunks);
  }

  private handleCommand(session: ClientSession, messageId: string, data: any) {
    const { cmd, args } = data;

    if (!cmd) {
      logger.warn('Received command message without cmd field', {
        sessionId: session.sessionId,
      });
      return;
    }

    logger.debug(`Received command: ${cmd}`, {
      sessionId: session.sessionId,
      username: session.username,
      args,
    });

    // Execute command via CommandService
    this.commandService.executeCommand(session, messageId, cmd, args || []);
  }

  private async sendChunks(session: ClientSession, coords: any[]) {
    if (!session.worldId) return;

    const world = this.worldManager.getWorld(session.worldId);
    if (!world) return;

    // Use Promise.all to load all chunks in parallel
    const chunkDataPromises = coords.map(async (coord: any) => {
      const cx = coord.cx ?? coord.x;
      const cz = coord.cz ?? coord.z;

      // Use WorldManager.getChunkData() which handles:
      // 1. Check memory (world.chunks)
      // 2. Load from storage
      // 3. Generate if needed
      const chunkData = await this.worldManager.getChunkData(session.worldId!, cx, cz);

      if (!chunkData) {
        logger.error('Failed to get chunk data', { worldId: session.worldId, cx, cz });
        return null;
      }

      // Use Block type directly from @nimbus/shared (no compression)
      return {
        cx,
        cz,
        b: chunkData.blocks, // Use blocks as-is (already in correct Block format)
        h: chunkData.heightData, // HeightData[] already contains x, z, maxHeight, groundLevel
      };
    });

    const chunkData = (await Promise.all(chunkDataPromises)).filter(c => c !== null);

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

      logger.info('🔵 SERVER: Starting broadcastBlockUpdates', {
        worldId,
        blockCount: blocks.length,
        totalSessions: this.sessions.size,
      });

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

      logger.info('🔵 SERVER: Blocks grouped by chunks', {
        worldId,
        totalBlocks: blocks.length,
        affectedChunks: blocksByChunk.size,
        chunkKeys: Array.from(blocksByChunk.keys()),
      });

      // Send updates to relevant clients
      let clientCount = 0;
      let skippedCount = 0;

      for (const [sessionId, session] of this.sessions) {
        logger.info('🔵 SERVER: Checking session', {
          sessionId,
          username: session.username,
          sessionWorld: session.worldId,
          targetWorld: worldId,
          registeredChunks: Array.from(session.registeredChunks),
        });

        // Skip if not in this world
        if (session.worldId !== worldId) {
          logger.info('🔴 SERVER: Session in different world, skipping', {
            sessionId,
            sessionWorld: session.worldId,
          });
          skippedCount++;
          continue;
        }

        // Collect blocks for this client (only chunks they have registered)
        const clientBlocks: Block[] = [];
        for (const [chunkKey, chunkBlocks] of blocksByChunk) {
          if (session.registeredChunks.has(chunkKey)) {
            clientBlocks.push(...chunkBlocks);
            logger.info('🔵 SERVER: Client has chunk registered', {
              sessionId,
              chunkKey,
              blockCount: chunkBlocks.length,
            });
          } else {
            logger.info('🔴 SERVER: Client does NOT have chunk registered', {
              sessionId,
              chunkKey,
            });
          }
        }

        // Send if client has any relevant blocks
        if (clientBlocks.length > 0) {
          try {
            const message = {
              t: 'b.u',
              d: clientBlocks,
            };
            const messageStr = JSON.stringify(message);

            logger.info('🔵 SERVER: Sending b.u message to client', {
              sessionId,
              username: session.username,
              blockCount: clientBlocks.length,
              messageLength: messageStr.length,
              wsReadyState: session.ws.readyState,
            });

            session.ws.send(messageStr);
            clientCount++;

            logger.info('✅ SERVER: Message sent successfully', {
              sessionId,
              username: session.username,
            });
          } catch (error) {
            logger.error('❌ SERVER: Failed to send block updates to client', { sessionId }, error as Error);
          }
        } else {
          logger.info('🔴 SERVER: No relevant blocks for this client', {
            sessionId,
            username: session.username,
          });
        }
      }

      logger.info('🔵 SERVER: Block updates broadcast complete', {
        worldId,
        clientCount,
        skippedCount,
        totalBlocks: blocks.length,
      });
    } catch (error) {
      ExceptionHandler.handle(error, 'NimbusServer.broadcastBlockUpdates', { worldId });
    }
  }

  /**
   * Queue pathway for clients based on registered chunks
   */
  private queuePathwayForClients(pathway: EntityPathway): void {
    try {
      if (!this.entitySimulator) {
        return;
      }

      // Get spawn definition to get affected chunks
      const spawnDef = this.entitySimulator.getSpawnDefinition(pathway.entityId);
      if (!spawnDef) {
        logger.warn('Cannot queue pathway: spawn definition not found', { entityId: pathway.entityId });
        return;
      }

      // For each session, check if they have any of the affected chunks registered
      for (const session of this.sessions.values()) {
        if (!session.isAuthenticated) {
          continue;
        }

        // Check if session has any of the affected chunks registered
        const hasRelevantChunk = spawnDef.chunks.some((chunk: Vector2) => {
          const chunkKey = getChunkKey(chunk.x, chunk.z);
          return session.registeredChunks.has(chunkKey);
        });

        if (hasRelevantChunk) {
          session.entityPathwayQueue.push(pathway);
        }
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'NimbusServer.queuePathwayForClients', { entityId: pathway.entityId });
    }
  }

  /**
   * Broadcast pathways to clients (called every 100ms)
   */
  private broadcastPathways(): void {
    try {
      for (const session of this.sessions.values()) {
        if (!session.isAuthenticated || session.entityPathwayQueue.length === 0) {
          continue;
        }

        try {
          // Send all queued pathways
          const message = {
            t: MessageType.ENTITY_CHUNK_PATHWAY,
            d: session.entityPathwayQueue,
          };

          session.ws.send(JSON.stringify(message));

          logger.debug('Entity pathways sent to client', {
            sessionId: session.sessionId,
            pathwayCount: session.entityPathwayQueue.length,
          });

          // Clear queue after sending
          session.entityPathwayQueue = [];
        } catch (error) {
          ExceptionHandler.handle(error, 'NimbusServer.broadcastPathways.send', {
            sessionId: session.sessionId,
          });
        }
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'NimbusServer.broadcastPathways');
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
process.on('SIGINT', async () => {
  logger.info('Shutting down server...');
  try {
    // Get WorldManager from server instance
    const worldManager = server['worldManager'] as WorldManager;
    await worldManager.saveAll();
    logger.info('All chunks saved successfully');
  } catch (error) {
    logger.error('Failed to save chunks during shutdown', {}, error as Error);
  }
  process.exit(0);
});
