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
  getLogger,
  ExceptionHandler,
} from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { NetworkService } from './NetworkService';
import type { ClientChunk, ClientChunkData, ClientHeightData } from '../types/ClientChunk';
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
      for (const [key, chunk] of this.chunks) {
        const distance = Math.max(
          Math.abs(chunk.data.transfer.cx - playerCx),
          Math.abs(chunk.data.transfer.cz - playerCz)
        );

        if (distance > this.unloadDistance) {
          this.chunks.delete(key);
          this.registeredChunks.delete(key);

          // Emit event for rendering cleanup
          this.emit('chunk:unloaded', chunk);

          logger.debug('Unloaded chunk', {
            cx: chunk.data.transfer.cx,
            cz: chunk.data.transfer.cz,
            distance,
          });
        }
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
  onChunkUpdate(chunks: ChunkDataTransferObject[]): void {
    try {
      chunks.forEach(chunkData => {
        const key = getChunkKey(chunkData.cx, chunkData.cz);

        // Process blocks into ClientBlocks with merged modifiers
        const clientChunkData = this.processChunkData(chunkData);

        const clientChunk: ClientChunk = {
          data: clientChunkData,
          isRendered: false,
          lastAccessTime: Date.now(),
        };

        this.chunks.set(key, clientChunk);

        // Emit event for rendering
        this.emit('chunk:loaded', clientChunk);

        logger.debug('Chunk loaded', {
          cx: chunkData.cx,
          cz: chunkData.cz,
          blocks: chunkData.b.length,
          clientBlocks: clientChunkData.data.size,
        });
      });
    } catch (error) {
      ExceptionHandler.handle(error, 'ChunkService.onChunkUpdate', {
        count: chunks.length,
      });
    }
  }

  /**
   * Process height data for chunk
   *
   * Calculates height information for each column (x, z) in the chunk:
   * - maxHeight: highest block position or world max
   * - minHeight: lowest block position or world min
   * - groundLevel: first solid block from bottom
   * - waterHeight: highest water block (currently undefined, needs BlockType info)
   *
   * @param chunkData Raw chunk data from server
   * @returns Map of position key -> ClientHeightData
   */
  private processHeightData(chunkData: ChunkDataTransferObject): Map<string, ClientHeightData> {
    const heightData = new Map<string, ClientHeightData>();
    const chunkSize = this.appContext.worldInfo?.chunkSize || 16;

    // World bounds for fallback values
    const worldMaxY = this.appContext.worldInfo?.stop?.y ?? 256;
    const worldMinY = this.appContext.worldInfo?.start?.y ?? -64;

    // Process existing height data from server if available
    if (chunkData.h) {
      for (const entry of chunkData.h) {
        const [x, z, maxHeight, groundLevel, waterLevel] = entry;
        const heightKey = `${x},${z}`;
        // Server provides maxHeight, groundLevel, and waterLevel
        heightData.set(heightKey, [x, z, maxHeight, worldMinY, groundLevel, waterLevel]);
      }
    }

    // Group blocks by column (x, z)
    const blocksByColumn = new Map<string, Block[]>();
    if (chunkData.b) {
      for (const block of chunkData.b) {
        // Calculate local x, z coordinates within chunk
        // Handle negative coordinates correctly: (-5 % 16) = -5, but we need 11
        const localX = ((block.position.x % chunkSize) + chunkSize) % chunkSize;
        const localZ = ((block.position.z % chunkSize) + chunkSize) % chunkSize;
        const columnKey = `${Math.floor(localX)},${Math.floor(localZ)}`;

        if (!blocksByColumn.has(columnKey)) {
          blocksByColumn.set(columnKey, []);
        }
        blocksByColumn.get(columnKey)!.push(block);
      }
    }

    // Calculate height data for each position in chunk
    for (let x = 0; x < chunkSize; x++) {
      for (let z = 0; z < chunkSize; z++) {
        const heightKey = `${x},${z}`;

        // Skip if we already have server-provided height data
        if (heightData.has(heightKey)) {
          continue;
        }

        // Get blocks in this column
        const columnBlocks = blocksByColumn.get(heightKey) || [];

        let maxHeight = worldMaxY;
        let minHeight = worldMinY;
        let groundLevel = worldMinY;
        let waterHeight: number | undefined = undefined;

        if (columnBlocks.length > 0) {
          // Find min and max Y positions
          const yPositions = columnBlocks.map(b => b.position.y);
          maxHeight = Math.max(...yPositions);
          minHeight = Math.min(...yPositions);

          // Find ground level: first solid block from bottom (lowest non-air block)
          // Sort blocks by Y position ascending
          const sortedBlocks = [...columnBlocks].sort((a, b) => a.position.y - b.position.y);
          const firstBlock = sortedBlocks[0];
          groundLevel = firstBlock ? firstBlock.position.y : worldMinY;

          // Find water height: highest water block in column
          const blockTypeService = this.appContext.services.blockType;
          if (blockTypeService) {
            let maxWaterY = -Infinity;
            let waterBlockCount = 0;
            for (const block of columnBlocks) {
              const blockType = blockTypeService.getBlockType(block.blockTypeId);
              // Check if block is water by description
              // Water blocks should have 'water' in their description
              if (blockType?.description?.toLowerCase().includes('water')) {
                maxWaterY = Math.max(maxWaterY, block.position.y);
                waterBlockCount++;
              }
            }
            // Set waterHeight only if water blocks were found
            if (maxWaterY > -Infinity) {
              waterHeight = maxWaterY; // Water block Y position
            }
          }
        }

        heightData.set(heightKey, [x, z, maxHeight, minHeight, groundLevel, waterHeight]);
      }
    }

    // Summary log (only if water found)
    const waterColumns = Array.from(heightData.values()).filter(h => h[5] !== undefined);
    if (waterColumns.length > 0) {
      logger.info('💧 Water found in chunk', {
        chunk: { cx: chunkData.cx, cz: chunkData.cz },
        waterColumns: waterColumns.length,
      });
    }

    return heightData;
  }
  /**
   * Process chunk data from server into ClientChunkData with ClientBlocks
   *
   * @param chunkData Raw chunk data from server
   * @returns Processed ClientChunkData with merged modifiers
   */
  private processChunkData(chunkData: ChunkDataTransferObject): ClientChunkData {
    const clientBlocksMap = new Map<string, ClientBlock>();
    const blockTypeService = this.appContext.services.blockType;

    if (!blockTypeService) {
      logger.warn('BlockTypeService not available - cannot process blocks');
      const heightData = this.processHeightData(chunkData);
      return {
        transfer: chunkData,
        data: clientBlocksMap,
        hightData: heightData,
      };
    }

    // Process each block in the chunk
    for (const block of chunkData.b) {
      const blockType = blockTypeService.getBlockType(block.blockTypeId);
      if (!blockType) {
        logger.warn('BlockType not found for block', {
          blockTypeId: block.blockTypeId,
          position: block.position,
        });
        continue;
      }

      // Merge block modifiers according to priority rules
      const currentModifier = mergeBlockModifier(block, blockType);

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
      const posKey = getBlockPositionKey(block.position.x, block.position.y, block.position.z);
      clientBlocksMap.set(posKey, clientBlock);
    }

    const hightData = this.processHeightData(chunkData);

    return {
      transfer: chunkData,
      data: clientBlocksMap,
      hightData: hightData,
    };
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
  onBlockUpdate(blocks: Block[]): void {
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
        const currentModifier = mergeBlockModifier(block, blockType);

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
