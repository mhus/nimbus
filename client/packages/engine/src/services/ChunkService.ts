/**
 * ChunkService - Chunk management and loading
 *
 * Manages chunk registration, loading, unloading, and provides
 * access to chunk data for rendering and gameplay.
 */

import {
  BaseMessage,
  MessageType,
  ChunkCoordinate,
  ChunkRegisterData,
  ChunkDataTransferObject,
  Block,
  Shape,
  getLogger,
  ExceptionHandler,
  Backdrop, Vector3,
} from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { NetworkService } from './NetworkService';
import type { ClientChunkData, ClientHeightData } from '../types/ClientChunk';
import { ClientChunk } from '../types/ClientChunk';
import type { ClientBlock } from '../types/ClientBlock';
import {
  worldToChunk,
  getChunkKey,
  getBrowserSpecificRenderDistance,
  getBrowserSpecificUnloadDistance,
} from '../utils/ChunkUtils';
import { mergeBlockModifier, getBlockPositionKey } from '../utils/BlockModifierMerge';

const logger = getLogger('ChunkService');

/**
 * Default backdrop configuration used when chunk data doesn't provide backdrop
 */
const DEFAULT_BACKDROP: Backdrop = {
    id: 'fadeout'
};

/**
 * Event listener
 */
type EventListener = (...args: any[]) => void;

/**
 * ChunkService - Manages chunks on the client
 *
 * Features:
 * - Chunk registration with server
 * - Dynamic chunk loading based on player position
 * - Chunk unloading for distant chunks
 * - Block access API
 * - Browser-specific render distances
 * - Event emission for rendering
 */
export class ChunkService {
  private chunks = new Map<string, ClientChunk>();
  private registeredChunks = new Set<string>();
  private lastRegistration: ChunkCoordinate[] = [];
  private eventListeners: Map<string, EventListener[]> = new Map();

  private renderDistance: number;
  private unloadDistance: number;

  // Track if initial chunks are loaded (to enable physics)
  private initialChunksLoaded: boolean = false;
  private initialPlayerChunk: { cx: number; cz: number } | null = null;

  constructor(
    private networkService: NetworkService,
    private appContext: AppContext
  ) {
    this.renderDistance = getBrowserSpecificRenderDistance();
    this.unloadDistance = getBrowserSpecificUnloadDistance();

    logger.info('ChunkService initialized', {
      renderDistance: this.renderDistance,
      unloadDistance: this.unloadDistance,
    });
  }

  /**
   * Register chunks with server for updates
   *
   * Server will automatically send chunk data for registered chunks.
   * Filters out already-registered chunks to avoid duplicate requests.
   *
   * @param coords - Chunk coordinates to register
   */
  async registerChunks(coords: ChunkCoordinate[]): Promise<void> {
    try {
      if (coords.length === 0) {
        return;
      }

      // Filter to only new chunks
      const newCoords = coords.filter(c => {
        const key = getChunkKey(c.cx, c.cz);
        return !this.registeredChunks.has(key);
      });

      if (newCoords.length === 0) {
        logger.debug('All chunks already registered');
        return;
      }

      // Add to registered set
      newCoords.forEach(c => {
        const key = getChunkKey(c.cx, c.cz);
        this.registeredChunks.add(key);
      });

      // Send registration message (send all coords, not just new ones)
      const message: BaseMessage<ChunkRegisterData> = {
        t: MessageType.CHUNK_REGISTER,
        d: { c: coords },
      };

      this.networkService.send(message);

      // Save for reconnect
      this.lastRegistration = coords;

      logger.debug('Registered chunks', {
        total: coords.length,
        new: newCoords.length,
      });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'ChunkService.registerChunks', {
        count: coords.length,
      });
    }
  }

  /**
   * Update chunks around a world position
   *
   * Calculates which chunks should be loaded based on render distance
   * and registers them with the server.
   *
   * @param worldX - World X coordinate
   * @param worldZ - World Z coordinate
   */
  updateChunksAroundPosition(worldX: number, worldZ: number): void {
    try {
      const chunkSize = this.appContext.worldInfo?.chunkSize || 16;
      const playerChunk = worldToChunk(worldX, worldZ, chunkSize);

      const coords: ChunkCoordinate[] = [];
      for (let dx = -this.renderDistance; dx <= this.renderDistance; dx++) {
        for (let dz = -this.renderDistance; dz <= this.renderDistance; dz++) {
          coords.push({
            cx: playerChunk.cx + dx,
            cz: playerChunk.cz + dz,
          });
        }
      }

      this.registerChunks(coords);
      this.unloadDistantChunks(playerChunk.cx, playerChunk.cz);
    } catch (error) {
      ExceptionHandler.handle(error, 'ChunkService.updateChunksAroundPosition', {
        worldX,
        worldZ,
      });
    }
  }

  /**
   * Unload chunks that are too far from player
   *
   * @param playerCx - Player chunk X coordinate
   * @param playerCz - Player chunk Z coordinate
   */
  unloadDistantChunks(playerCx: number, playerCz: number): void {
    try {
      // First, collect all chunks to unload (don't delete during iteration)
      const chunksToUnload: Array<{ key: string; chunk: ClientChunk; distance: number }> = [];

      for (const [key, chunk] of this.chunks) {
        const distance = Math.max(
          Math.abs(chunk.data.transfer.cx - playerCx),
          Math.abs(chunk.data.transfer.cz - playerCz)
        );

        if (distance > this.unloadDistance) {
          chunksToUnload.push({ key, chunk, distance });
        }
      }

      // Now unload all collected chunks
      for (const { key, chunk, distance } of chunksToUnload) {
        const cx = chunk.data.transfer.cx;
        const cz = chunk.data.transfer.cz;

        // Emit event for rendering cleanup BEFORE deleting chunk
        // so RenderService can still access DisposableResources
        this.emit('chunk:unloaded', { cx, cz });

        // Now delete the chunk
        this.chunks.delete(key);
        this.registeredChunks.delete(key);

        logger.debug('Unloaded chunk', {
          cx,
          cz,
          distance,
        });
      }

      if (chunksToUnload.length > 0) {
        logger.info('Unloaded distant chunks', { count: chunksToUnload.length });
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'ChunkService.unloadDistantChunks', {
        playerCx,
        playerCz,
      });
    }
  }

  /**
   * Handle chunk update from server (called by ChunkMessageHandler)
   *
   * @param chunks - Array of chunk data from server
   */
  async onChunkUpdate(chunks: ChunkDataTransferObject[]): Promise<void> {
    try {
      // Process chunks sequentially to maintain order
      for (const chunkData of chunks) {
        const key = getChunkKey(chunkData.cx, chunkData.cz);

        // Check if chunk already exists
        const existingChunk = this.chunks.get(key);
        if (existingChunk) {
          logger.debug('Chunk already loaded, skipping duplicate', {
            cx: chunkData.cx,
            cz: chunkData.cz,
          });
          continue;
        }

        // Process blocks into ClientBlocks with merged modifiers
        const clientChunkData = await this.processChunkData(chunkData);

        const clientChunk: ClientChunk = new ClientChunk(clientChunkData);

        this.chunks.set(key, clientChunk);

        // Emit event for rendering
        this.emit('chunk:loaded', clientChunk);

        logger.debug('Chunk loaded', {
          cx: chunkData.cx,
          cz: chunkData.cz,
          blocks: chunkData.b.length,
          clientBlocks: clientChunkData.data.size,
        });
      }

      // Check if initial player spawn is ready
      this.checkInitialSpawnReady();
    } catch (error) {
      ExceptionHandler.handle(error, 'ChunkService.onChunkUpdate', {
        count: chunks.length,
      });
    }
  }

  /**
   * Check if initial player spawn is ready
   *
   * Starts teleportation mode for initial spawn on first call
   */
  private checkInitialSpawnReady(): void {
    // Only trigger once
    if (this.initialChunksLoaded) {
      return;
    }

    // Need player service
    const playerService = this.appContext.services.player;
    if (!playerService) {
      return;
    }

    // Get physics service
    const physicsService = this.appContext.services.physics;
    if (!physicsService) {
      return;
    }

    // Mark as handled
    this.initialChunksLoaded = true;

    // Start teleportation mode - PhysicsService will handle the rest
    // It will check for chunk, heightData, blocks and position player automatically
    const playerEntity = physicsService.getEntity('player');
    if (playerEntity) {
      physicsService.teleport(playerEntity, playerEntity.position);
      logger.info('Initial spawn - teleportation mode started');
    } else {
      logger.warn('Player entity not found for initial spawn');
    }
  }

  private processStatusData(chunkData: ChunkDataTransferObject): Map<string, number> {
      const statusData = new Map<string, number>();
      // Extract status from blocks instead of non-existent 's' property
      for (const block of chunkData.b || []) {
        if (block.status !== undefined) {
          const posKey = getBlockPositionKey(block.position.x, block.position.y, block.position.z);
          statusData.set(posKey, block.status);
        }
      }
      return statusData;
  }

  /**
   * Process chunk data from server into ClientChunkData with ClientBlocks
   *
   * Performance-optimized: Processes blocks and height data in a single pass
   *
   * @param chunkData Raw chunk data from server
   * @returns Processed ClientChunkData with merged modifiers and height data
   */
  private async processChunkData(chunkData: ChunkDataTransferObject): Promise<ClientChunkData> {

    const clientBlocksMap = new Map<string, ClientBlock>();
    const blockTypeService = this.appContext.services.blockType;
    const statusData = this.processStatusData(chunkData);
    const chunkSize = this.appContext.worldInfo?.chunkSize || 16;

    // World bounds
    const worldMaxY = this.appContext.worldInfo?.stop?.y ?? 1000;
    const worldMinY = this.appContext.worldInfo?.start?.y ?? -100;

    // Height data structures for single-pass calculation
    const heightData = new Map<string, ClientHeightData>();

    // Track blocks per column for height calculation (localX, localZ -> blocks)
    // Note: maxY is usually worldMaxY, but if blocks exceed it, we add 10 blocks headroom
    const columnBlocks = new Map<string, {
      blocks: Block[],
      minY: number,
      highestBlockY: number,  // Track highest block for worldMaxY override check
      groundLevel: number | null,
      waterLevel: number | null
    }>();

    // STEP 1: Use server-provided height data if available
    if (chunkData.h) {
      for (const entry of chunkData.h) {
        const [x, z, maxHeight, groundLevel, waterLevel] = entry;
        const heightKey = `${x},${z}`;
        // Server provides: [x, z, maxHeight, groundLevel, waterLevel]
        // ClientHeightData format: [x, z, maxHeight, minHeight, groundLevel, waterHeight]
        // Use worldMinY as minHeight since server doesn't provide it
        heightData.set(heightKey, [x, z, maxHeight, worldMinY, groundLevel, waterLevel]);
      }
    }

    if (!blockTypeService) {
      logger.warn('BlockTypeService not available - cannot process blocks');
      return {
        transfer: chunkData,
        data: clientBlocksMap,
        hightData: heightData,
        statusData: statusData
      };
    }

    // STEP 1.5: Preload all BlockTypes needed for this chunk
    // Collect unique BlockType IDs from blocks AND items
    const blockTypeIds = new Set(chunkData.b.map(block => block.blockTypeId));
    if (chunkData.i) {
      for (const item of chunkData.i) {
        blockTypeIds.add(item.blockTypeId);
      }
    }
    logger.debug('Preloading BlockTypes for chunk', {
      cx: chunkData.cx,
      cz: chunkData.cz,
      uniqueBlockTypes: blockTypeIds.size,
    });

    // Preload all required chunks in parallel
    await blockTypeService.preloadBlockTypes(Array.from(blockTypeIds));

    // STEP 2: Process each block - creating ClientBlocks AND collecting height data in ONE pass
    for (const block of chunkData.b) {
      const blockType = blockTypeService.getBlockType(block.blockTypeId);
      if (!blockType) {
        logger.warn('BlockType not found for block', {
          blockTypeId: block.blockTypeId,
          position: block.position,
        });
        continue;
      }

      // Get position key for this block
      const posKey = getBlockPositionKey(block.position.x, block.position.y, block.position.z);

      // Merge block modifiers according to priority rules
      const currentModifier = mergeBlockModifier(this.appContext, block, blockType, statusData.get(posKey));

      // Create ClientBlock
      const clientBlock: ClientBlock = {
        block,
        chunk: { cx: chunkData.cx, cz: chunkData.cz },
        blockType,
        currentModifier,
        clientBlockType: blockType as any, // TODO: Convert to ClientBlockType
        isVisible: true,
        isDirty: false,
        lastUpdate: Date.now(),
      };

      // Add to map with position key
      clientBlocksMap.set(posKey, clientBlock);

      // PERFORMANCE: Calculate height data in same pass
      // Calculate local x, z coordinates within chunk
      const localX = ((block.position.x % chunkSize) + chunkSize) % chunkSize;
      const localZ = ((block.position.z % chunkSize) + chunkSize) % chunkSize;
      const columnKey = `${Math.floor(localX)},${Math.floor(localZ)}`;

      // Skip if server already provided height data for this column
      if (heightData.has(columnKey)) {
        continue;
      }

      // Track blocks in this column for height calculation
      if (!columnBlocks.has(columnKey)) {
        columnBlocks.set(columnKey, {
          blocks: [],
          minY: Infinity,
          highestBlockY: -Infinity,
          groundLevel: null,
          waterLevel: null
        });
      }

      const columnData = columnBlocks.get(columnKey)!;
      columnData.blocks.push(block);

      // Update minY and highestBlockY (more efficient than separate pass later)
      if (block.position.y < columnData.minY) {
        columnData.minY = block.position.y;
        // Ground level is the lowest block (first from bottom)
        columnData.groundLevel = block.position.y;
      }
      if (block.position.y > columnData.highestBlockY) {
        columnData.highestBlockY = block.position.y;
      }

      // Check for water blocks (check shape first, then description fallback)
      const modifier = clientBlock.currentModifier;
      const shape = modifier?.visibility?.shape;
      const isWater = shape === Shape.OCEAN ||
                        shape === Shape.WATER ||
                        shape === Shape.RIVER ||
                        shape === Shape.OCEAN_MAELSTROM ||
                        shape === Shape.OCEAN_COAST;

      if (isWater) {
        if (columnData.waterLevel === null || block.position.y > columnData.waterLevel) {
          columnData.waterLevel = block.position.y;
        }
      }
    }

    // STEP 3: Finalize height data for columns with blocks (already calculated during block processing)
    for (const [columnKey, columnData] of columnBlocks.entries()) {
      const [xStr, zStr] = columnKey.split(',');
      const x = parseInt(xStr, 10);
      const z = parseInt(zStr, 10);

      // Calculate maxHeight:
      // - Usually worldMaxY
      // - Exception: If highest block exceeds worldMaxY, use highestBlock + 10 for headroom
      let maxHeight = worldMaxY;
      if (columnData.highestBlockY !== -Infinity && columnData.highestBlockY > worldMaxY) {
        maxHeight = columnData.highestBlockY + 10;
        logger.warn('Block exceeds worldMaxY, adding headroom', {
          cx: chunkData.cx,
          cz: chunkData.cz,
          columnKey,
          highestBlock: columnData.highestBlockY,
          worldMaxY,
          newMaxHeight: maxHeight
        });
      }

      heightData.set(columnKey, [
        x,
        z,
        maxHeight,
        columnData.minY !== Infinity ? columnData.minY : worldMinY,
        columnData.groundLevel !== null ? columnData.groundLevel : worldMinY,
        columnData.waterLevel !== null ? columnData.waterLevel : undefined
      ]);
    }

    // STEP 3.5: Process items list - add items only at AIR positions
    if (chunkData.i && chunkData.i.length > 0) {
      logger.debug('Processing items for chunk', {
        cx: chunkData.cx,
        cz: chunkData.cz,
        itemCount: chunkData.i.length,
      });

      // BlockTypes already preloaded in STEP 1.5
      for (const item of chunkData.i) {
        // Get position key
        const posKey = getBlockPositionKey(item.position.x, item.position.y, item.position.z);

        // Only add item if position is AIR (no block exists at this position)
        if (clientBlocksMap.has(posKey)) {
          logger.debug('Item skipped - position occupied by block', {
            position: item.position,
            itemId: item.metadata?.id,
          });
          continue;
        }

        // Get BlockType for item
        const blockType = blockTypeService.getBlockType(item.blockTypeId);
        if (!blockType) {
          logger.warn('BlockType not found for item', {
            blockTypeId: item.blockTypeId,
            position: item.position,
            itemId: item.metadata?.id,
          });
          continue;
        }

        // Merge block modifiers
        const currentModifier = mergeBlockModifier(this.appContext, item, blockType, statusData.get(posKey));

        // Create ClientBlock for item
        const clientBlock: ClientBlock = {
          block: item,
          chunk: { cx: chunkData.cx, cz: chunkData.cz },
          blockType,
          currentModifier,
          clientBlockType: blockType as any,
          isVisible: true,
          isDirty: false,
          lastUpdate: Date.now(),
        };

        // Add to map with position key
        clientBlocksMap.set(posKey, clientBlock);

        logger.debug('Item added to chunk', {
          position: item.position,
          itemId: item.metadata?.id,
          displayName: item.metadata?.displayName,
        });
      }
    }

    // STEP 4: Fill in empty columns with default values (no blocks in column)
    for (let x = 0; x < chunkSize; x++) {
      for (let z = 0; z < chunkSize; z++) {
        const heightKey = `${x},${z}`;

        // Skip if already have data (from server or calculated)
        if (heightData.has(heightKey)) {
          continue;
        }

        // Empty column - use world bounds as defaults
        heightData.set(heightKey, [
          x,
          z,
          worldMaxY,  // No blocks, so max is world top
          worldMinY,  // No blocks, so min is world bottom
          worldMinY,  // No ground level (no blocks)
          undefined   // No water
        ]);
      }
    }

    // Summary log (only if water found)
    const waterColumns = Array.from(heightData.values()).filter(h => h[5] !== undefined);
    if (waterColumns.length > 0) {
      logger.debug('💧 Water found in chunk', {
        chunk: { cx: chunkData.cx, cz: chunkData.cz },
        waterColumns: waterColumns.length,
      });
    }

    // STEP 5: Process backdrop data - add default backdrop if not provided
    const backdrop = this.processBackdropData(chunkData);

    return {
      transfer: chunkData,
      data: clientBlocksMap,
      hightData: heightData,
      statusData: statusData,
      backdrop,
    };
  }

  /**
   * Process backdrop data - set default backdrop for sides that are not provided
   */
  private processBackdropData(chunkData: ChunkDataTransferObject): {
    n?: Array<Backdrop>;
    e?: Array<Backdrop>;
    s?: Array<Backdrop>;
    w?: Array<Backdrop>;
  } {
    const backdrop = chunkData.backdrop || {};

    // Set default backdrop for each side if not provided
    return {
      n: backdrop.n && backdrop.n.length > 0 ? backdrop.n : [DEFAULT_BACKDROP],
      e: backdrop.e && backdrop.e.length > 0 ? backdrop.e : [DEFAULT_BACKDROP],
      s: backdrop.s && backdrop.s.length > 0 ? backdrop.s : [DEFAULT_BACKDROP],
      w: backdrop.w && backdrop.w.length > 0 ? backdrop.w : [DEFAULT_BACKDROP],
    };
  }

  /**
   * Get chunk at world block coordinates
   *
   * @param x - Block X coordinate
   * @param z - Block Z coordinate
   * @returns Chunk or undefined if not loaded
   */
  getChunkForBlockPosition(pos : Vector3): ClientChunk | undefined {
    const chunkSize = this.appContext.worldInfo?.chunkSize || 16;
    const { cx, cz } = worldToChunk(pos.x, pos.z, chunkSize);
    return this.chunks.get(getChunkKey(cx, cz));
  }

  /**
   * Get chunk at world block coordinates
   *
   * @param x - Block X coordinate
   * @param z - Block Z coordinate
   * @returns Chunk or undefined if not loaded
   */
  getChunkForBlock(x: number, z: number): ClientChunk | undefined {
    const chunkSize = this.appContext.worldInfo?.chunkSize || 16;
    const { cx, cz } = worldToChunk(x, z, chunkSize);
    return this.chunks.get(getChunkKey(cx, cz));
  }

  /**
   * Get chunk at chunk coordinates
   *
   * @param cx - Chunk X coordinate
   * @param cz - Chunk Z coordinate
   * @returns Chunk or undefined if not loaded
   */
  getChunk(cx: number, cz: number): ClientChunk | undefined {
    return this.chunks.get(getChunkKey(cx, cz));
  }

  /**
   * Get block at world coordinates
   *
   * @param x - World X coordinate
   * @param y - World Y coordinate
   * @param z - World Z coordinate
   * @returns ClientBlock or undefined if not found
   */
  getBlockAt(x: number, y: number, z: number): ClientBlock | undefined {
    const chunkSize = this.appContext.worldInfo?.chunkSize || 16;
    const chunkCoord = worldToChunk(x, z, chunkSize);
    const chunk = this.getChunk(chunkCoord.cx, chunkCoord.cz);

    if (!chunk) {
      return undefined;
    }

    // Look up block in processed data map
    const posKey = getBlockPositionKey(x, y, z);
    return chunk.data.data.get(posKey);
  }

  /**
   * Handle block updates from server (called by BlockUpdateHandler)
   *
   * Updates individual blocks in loaded chunks. Blocks with blockTypeId: 0 are deleted.
   *
   * @param blocks - Array of block updates from server
   */
  async onBlockUpdate(blocks: Block[]): Promise<void> {
    try {
      logger.info('🔵 ChunkService.onBlockUpdate called', {
        blockCount: blocks.length,
      });

      const chunkSize = this.appContext.worldInfo?.chunkSize || 16;
      const blockTypeService = this.appContext.services.blockType;

      if (!blockTypeService) {
        logger.warn('BlockTypeService not available - cannot process block updates');
        return;
      }

      // Preload all BlockTypes needed for these updates
      const blockTypeIds = new Set(
        blocks
          .filter(block => block.blockTypeId !== 0) // Skip deletions
          .map(block => block.blockTypeId)
      );

      if (blockTypeIds.size > 0) {
        logger.debug('Preloading BlockTypes for block updates', {
          uniqueBlockTypes: blockTypeIds.size,
        });
        await blockTypeService.preloadBlockTypes(Array.from(blockTypeIds));
      }

      // Track affected chunks for re-rendering
      const affectedChunks = new Set<string>();

      for (const block of blocks) {
        // Calculate chunk coordinates from block position
        const chunkCoord = worldToChunk(block.position.x, block.position.z, chunkSize);
        const chunkKey = getChunkKey(chunkCoord.cx, chunkCoord.cz);

        // Get chunk
        const clientChunk = this.chunks.get(chunkKey);
        if (!clientChunk) {
          // Chunk not loaded on client - ignore update
          logger.debug('Block update for unloaded chunk, ignoring', {
            position: block.position,
            cx: chunkCoord.cx,
            cz: chunkCoord.cz,
          });
          continue;
        }

        // Get position key
        const posKey = getBlockPositionKey(
          block.position.x,
          block.position.y,
          block.position.z
        );

        // Handle deletion (blockTypeId: 0)
        if (block.blockTypeId === 0) {
          const wasDeleted = clientChunk.data.data.delete(posKey);
          if (wasDeleted) {
            logger.debug('Block deleted', { position: block.position });
            affectedChunks.add(chunkKey);
          }
          continue;
        }

        // Handle update/create
        const blockType = blockTypeService.getBlockType(block.blockTypeId);
        if (!blockType) {
          logger.warn('BlockType not found for block update', {
            blockTypeId: block.blockTypeId,
            position: block.position,
          });
          continue;
        }

        // Merge block modifiers
        const currentModifier = mergeBlockModifier(this.appContext, block, blockType, clientChunk.data.statusData.get(posKey));

        // Create/update ClientBlock
        const clientBlock: ClientBlock = {
          block,
          chunk: { cx: chunkCoord.cx, cz: chunkCoord.cz },
          blockType,
          currentModifier,
          clientBlockType: blockType as any,
          isVisible: true,
          isDirty: true,
          lastUpdate: Date.now(),
        };

        // Update in chunk
        clientChunk.data.data.set(posKey, clientBlock);
        affectedChunks.add(chunkKey);

        logger.debug('Block updated', {
          position: block.position,
          blockTypeId: block.blockTypeId,
        });
      }

      // Emit events for affected chunks (triggers re-rendering)
      for (const chunkKey of affectedChunks) {
        const chunk = this.chunks.get(chunkKey);
        if (chunk) {
          // Mark for re-rendering
          chunk.isRendered = false;
          this.emit('chunk:updated', chunk);
          logger.info('🔵 Emitting chunk:updated event', { chunkKey });
        }
      }

      logger.info('🔵 Block updates applied', {
        totalBlocks: blocks.length,
        affectedChunks: affectedChunks.size,
      });
    } catch (error) {
      ExceptionHandler.handle(error, 'ChunkService.onBlockUpdate', {
        count: blocks.length,
      });
    }
  }

  /**
   * Handle item block updates from server (called by ItemBlockUpdateHandler)
   *
   * Updates individual item blocks in loaded chunks.
   * - Items with blockTypeId: 0 → remove item (only if an item exists at that position)
   * - Items with blockTypeId: 1 → add/update item (can overwrite existing items)
   * - Items can only exist at AIR positions or replace existing items
   *
   * @param blocks - Array of item block updates from server
   */
  async onItemBlockUpdate(blocks: Block[]): Promise<void> {
    try {
      logger.info('🔵 ChunkService.onItemBlockUpdate called', {
        itemCount: blocks.length,
      });

      const chunkSize = this.appContext.worldInfo?.chunkSize || 16;
      const blockTypeService = this.appContext.services.blockType;

      if (!blockTypeService) {
        logger.warn('BlockTypeService not available - cannot process item updates');
        return;
      }

      // Preload all BlockTypes needed for these updates
      const blockTypeIds = new Set(
        blocks
          .filter(block => block.blockTypeId !== 0) // Skip deletions
          .map(block => block.blockTypeId)
      );

      if (blockTypeIds.size > 0) {
        logger.debug('Preloading BlockTypes for item updates', {
          uniqueBlockTypes: blockTypeIds.size,
        });
        await blockTypeService.preloadBlockTypes(Array.from(blockTypeIds));
      }

      // Track affected chunks for re-rendering
      const affectedChunks = new Set<string>();

      for (const block of blocks) {
        // Calculate chunk coordinates from block position
        const chunkCoord = worldToChunk(block.position.x, block.position.z, chunkSize);
        const chunkKey = getChunkKey(chunkCoord.cx, chunkCoord.cz);

        // Get chunk
        const clientChunk = this.chunks.get(chunkKey);
        if (!clientChunk) {
          // Chunk not loaded on client - ignore update
          logger.debug('Item update for unloaded chunk, ignoring', {
            position: block.position,
            cx: chunkCoord.cx,
            cz: chunkCoord.cz,
          });
          continue;
        }

        // Get position key
        const posKey = getBlockPositionKey(
          block.position.x,
          block.position.y,
          block.position.z
        );

        // Get existing block at this position
        const existingBlock = clientChunk.data.data.get(posKey);

        // Handle deletion (blockTypeId: 0)
        if (block.blockTypeId === 0) {
          // Only delete if an item exists at this position (blockTypeId: 1)
          if (existingBlock && existingBlock.block.blockTypeId === 1) {
            const wasDeleted = clientChunk.data.data.delete(posKey);
            if (wasDeleted) {
              logger.debug('Item deleted', {
                position: block.position,
                itemId: block.metadata?.id,
              });
              affectedChunks.add(chunkKey);
            }
          } else {
            logger.debug('Item deletion ignored - no item at position or different block type', {
              position: block.position,
              existingBlockTypeId: existingBlock?.block.blockTypeId,
            });
          }
          continue;
        }

        // Handle add/update (blockTypeId: 1)
        if (block.blockTypeId === 1) {
          // Check if position is AIR or already has an item
          const isAir = !existingBlock;
          const isItem = existingBlock && existingBlock.block.blockTypeId === 1;

          if (!isAir && !isItem) {
            logger.debug('Item add/update ignored - position occupied by non-item block', {
              position: block.position,
              existingBlockTypeId: existingBlock.block.blockTypeId,
              itemId: block.metadata?.id,
            });
            continue;
          }

          // Get BlockType
          const blockType = blockTypeService.getBlockType(block.blockTypeId);
          if (!blockType) {
            logger.warn('BlockType not found for item update', {
              blockTypeId: block.blockTypeId,
              position: block.position,
            });
            continue;
          }

          // Merge block modifiers
          const currentModifier = mergeBlockModifier(this.appContext, block, blockType, clientChunk.data.statusData.get(posKey));

          // Create/update ClientBlock
          const clientBlock: ClientBlock = {
            block,
            chunk: { cx: chunkCoord.cx, cz: chunkCoord.cz },
            blockType,
            currentModifier,
            clientBlockType: blockType as any,
            isVisible: true,
            isDirty: true,
            lastUpdate: Date.now(),
          };

          // Update in chunk
          clientChunk.data.data.set(posKey, clientBlock);
          affectedChunks.add(chunkKey);

          logger.debug('Item added/updated', {
            position: block.position,
            itemId: block.metadata?.id,
            displayName: block.metadata?.displayName,
            wasUpdate: isItem,
          });
        } else {
          logger.warn('Item block update with unexpected blockTypeId', {
            blockTypeId: block.blockTypeId,
            position: block.position,
          });
        }
      }

      // Emit events for affected chunks (triggers re-rendering)
      for (const chunkKey of affectedChunks) {
        const chunk = this.chunks.get(chunkKey);
        if (chunk) {
          // Mark for re-rendering
          chunk.isRendered = false;
          this.emit('chunk:updated', chunk);
          logger.info('🔵 Emitting chunk:updated event for items', { chunkKey });
        }
      }

      logger.info('🔵 Item updates applied', {
        totalItems: blocks.length,
        affectedChunks: affectedChunks.size,
      });
    } catch (error) {
      ExceptionHandler.handle(error, 'ChunkService.onItemBlockUpdate', {
        count: blocks.length,
      });
    }
  }

  /**
   * Resend last chunk registration (called after reconnect)
   */
  resendLastRegistration(): void {
    if (this.lastRegistration.length > 0) {
      logger.info('Resending last chunk registration after reconnect', {
        count: this.lastRegistration.length,
      });
      this.registerChunks(this.lastRegistration);
    }
  }

  /**
   * Get current render distance
   */
  getRenderDistance(): number {
    return this.renderDistance;
  }

  /**
   * Get current unload distance
   */
  getUnloadDistance(): number {
    return this.unloadDistance;
  }

  /**
   * Get total number of loaded chunks
   */
  getLoadedChunkCount(): number {
    return this.chunks.size;
  }

  /**
   * Get all loaded chunks
   */
  getAllChunks(): ClientChunk[] {
    return Array.from(this.chunks.values());
  }

  /**
   * Add event listener
   */
  on(event: string, listener: EventListener): void {
    const listeners = this.eventListeners.get(event) || [];
    listeners.push(listener);
    this.eventListeners.set(event, listeners);
  }

  /**
   * Remove event listener
   */
  off(event: string, listener: EventListener): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      const index = listeners.indexOf(listener);
      if (index !== -1) {
        listeners.splice(index, 1);
      }
    }
  }

  /**
   * Emit event
   */
  private emit(event: string, ...args: any[]): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      listeners.forEach(listener => {
        try {
          listener(...args);
        } catch (error) {
          ExceptionHandler.handle(error, 'ChunkService.emit', { event });
        }
      });
    }
  }
}
