/**
 * ServerChunk - Server-side chunk representation
 *
 * This is the server-side working representation of a chunk.
 * Uses a Map for efficient CRUD operations on blocks.
 * Converts to/from ChunkData for storage and network transfer.
 */

import type { ChunkData, Block } from '@nimbus/shared';

/**
 * Chunk status for server-side management
 */
export enum ChunkStatus {
  /** Not yet generated */
  EMPTY = 0,

  /** Structure generation in progress */
  GENERATING = 1,

  /** Features (trees, etc.) being placed */
  DECORATING = 2,

  /** Fully generated and ready */
  READY = 3,

  /** Currently being loaded from storage */
  LOADING = 4,

  /** Unloading/saving in progress */
  UNLOADING = 5,
}

/**
 * Server-side chunk with efficient block access via Map
 */
export class ServerChunk {
  /** Chunk X coordinate (in chunk space) */
  cx: number;

  /** Chunk Z coordinate (in chunk space) */
  cz: number;

  /** Chunk size (blocks per side) */
  size: number;

  /** Blocks stored in Map for efficient CRUD: "x,y,z" → Block */
  private blocks: Map<string, Block>;

  /** Chunk status */
  status: ChunkStatus;

  /** Last modification timestamp */
  lastModified: number;

  /** Is chunk dirty (needs saving) */
  isDirty: boolean;

  constructor(cx: number, cz: number, size: number) {
    this.cx = cx;
    this.cz = cz;
    this.size = size;
    this.blocks = new Map();
    this.status = ChunkStatus.EMPTY;
    this.lastModified = Date.now();
    this.isDirty = false;
  }

  /**
   * Get block at world coordinates
   */
  getBlock(x: number, y: number, z: number): Block | undefined {
    return this.blocks.get(`${x},${y},${z}`);
  }

  /**
   * Add or update block
   */
  setBlock(block: Block): void {
    const key = `${block.position.x},${block.position.y},${block.position.z}`;
    this.blocks.set(key, block);
    this.isDirty = true;
    this.lastModified = Date.now();
  }

  /**
   * Delete block at world coordinates
   */
  deleteBlock(x: number, y: number, z: number): boolean {
    const key = `${x},${y},${z}`;
    const deleted = this.blocks.delete(key);
    if (deleted) {
      this.isDirty = true;
      this.lastModified = Date.now();
    }
    return deleted;
  }

  /**
   * Check if block exists at position
   */
  hasBlock(x: number, y: number, z: number): boolean {
    return this.blocks.has(`${x},${y},${z}`);
  }

  /**
   * Get all blocks in chunk
   */
  getAllBlocks(): Block[] {
    return Array.from(this.blocks.values());
  }

  /**
   * Get block count
   */
  getBlockCount(): number {
    return this.blocks.size;
  }

  /**
   * Check if chunk is empty
   */
  isEmpty(): boolean {
    return this.blocks.size === 0;
  }

  /**
   * Clear all blocks
   */
  clear(): void {
    this.blocks.clear();
    this.isDirty = true;
    this.lastModified = Date.now();
  }

  /**
   * Convert to ChunkData for storage/network transfer
   */
  toChunkData(): ChunkData {
    return {
      cx: this.cx,
      cz: this.cz,
      size: this.size,
      blocks: this.getAllBlocks(),
    };
  }

  /**
   * Create ServerChunk from ChunkData
   */
  static fromChunkData(data: ChunkData): ServerChunk {
    const chunk = new ServerChunk(data.cx, data.cz, data.size);

    // Load all blocks into Map
    for (const block of data.blocks) {
      const key = `${block.position.x},${block.position.y},${block.position.z}`;
      chunk.blocks.set(key, block);
    }

    chunk.isDirty = false;
    return chunk;
  }

  /**
   * Get blocks in local coordinate range
   */
  getBlocksInRange(
    localX: number,
    localY: number,
    localZ: number,
    rangeX: number,
    rangeY: number,
    rangeZ: number
  ): Block[] {
    const result: Block[] = [];
    const worldX = this.cx * this.size;
    const worldZ = this.cz * this.size;

    for (let x = localX; x < localX + rangeX; x++) {
      for (let y = localY; y < localY + rangeY; y++) {
        for (let z = localZ; z < localZ + rangeZ; z++) {
          const block = this.getBlock(worldX + x, y, worldZ + z);
          if (block) {
            result.push(block);
          }
        }
      }
    }

    return result;
  }

  /**
   * Get blocks by type
   */
  getBlocksByType(blockTypeId: string | number): Block[] {
    const normalizedId = typeof blockTypeId === 'number' ? String(blockTypeId) : blockTypeId;
    return this.getAllBlocks().filter((b) => b.blockTypeId === normalizedId);
  }
}

/**
 * Helper functions for ServerChunk
 */
export namespace ServerChunkHelper {
  /**
   * Convert world coordinates to chunk coordinates
   */
  export function worldToChunk(
    x: number,
    z: number,
    chunkSize: number
  ): { cx: number; cz: number } {
    return {
      cx: Math.floor(x / chunkSize),
      cz: Math.floor(z / chunkSize),
    };
  }

  /**
   * Convert world coordinates to local coordinates within chunk
   */
  export function worldToLocal(
    x: number,
    z: number,
    chunkSize: number
  ): { localX: number; localZ: number } {
    return {
      localX: ((x % chunkSize) + chunkSize) % chunkSize,
      localZ: ((z % chunkSize) + chunkSize) % chunkSize,
    };
  }

  /**
   * Convert chunk + local coordinates to world coordinates
   */
  export function localToWorld(
    cx: number,
    cz: number,
    localX: number,
    localZ: number,
    chunkSize: number
  ): { x: number; z: number } {
    return {
      x: cx * chunkSize + localX,
      z: cz * chunkSize + localZ,
    };
  }

  /**
   * Get chunk key for mapping
   */
  export function getChunkKey(cx: number, cz: number): string {
    return `${cx},${cz}`;
  }

  /**
   * Parse chunk key
   */
  export function parseChunkKey(
    key: string
  ): { cx: number; cz: number } | null {
    const parts = key.split(',');
    if (parts.length !== 2) return null;

    const cx = parseInt(parts[0], 10);
    const cz = parseInt(parts[1], 10);

    if (isNaN(cx) || isNaN(cz)) return null;

    return { cx, cz };
  }

  /**
   * Check if world coordinates are within chunk bounds
   */
  export function isInChunk(
    chunk: ServerChunk,
    x: number,
    z: number
  ): boolean {
    const minX = chunk.cx * chunk.size;
    const maxX = minX + chunk.size - 1;
    const minZ = chunk.cz * chunk.size;
    const maxZ = minZ + chunk.size - 1;

    return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
  }
}
