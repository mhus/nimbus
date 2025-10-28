/**
 * ChunkData - Internal chunk representation
 *
 * This is the internal representation of a chunk for both client and server.
 * For network transfer, see ChunkDataTransferObject.
 *
 * Chunks are always columns (XZ coordinates, full Y height).
 */

/**
 * Chunk size (blocks per side)
 * Typically 16 or 32, defined by world settings
 */
export type ChunkSize = number;

/**
 * Chunk data - internal representation
 *
 * Stores block data efficiently as a flat typed array.
 * Block index: localX + localZ * chunkSize + localY * chunkSize * chunkSize
 */
export interface ChunkData {
  /**
   * Chunk X coordinate (in chunk space)
   */
  cx: number;

  /**
   * Chunk Z coordinate (in chunk space)
   */
  cz: number;

  /**
   * Chunk size (blocks per side, typically 16 or 32)
   */
  size: number;

  /**
   * Block data as flat array
   * Stores block type IDs (0 = air)
   * Length: size * worldHeight * size
   * Index: localX + localZ * size + localY * size * size
   */
  blocks: Uint16Array;

  /**
   * Optional: Compact block data for sparse chunks
   * Only non-air blocks, with position encoding
   * Used when chunk is mostly empty for memory efficiency
   */
  sparseBlocks?: Map<number, number>; // index -> blockTypeId

  /**
   * Height data per XZ position
   * Array of [maxHeight, minHeight, groundLevel, waterHeight]
   * Length: size * size
   */
  heightData?: number[];

  /**
   * Chunk status
   */
  status: ChunkStatus;

  /**
   * Last modification timestamp
   */
  lastModified?: number;

  /**
   * Is chunk dirty (needs saving/sending)
   */
  isDirty?: boolean;
}

/**
 * Chunk loading/generation status
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
 * Helper functions for chunk data access
 */
export namespace ChunkDataHelper {
  /**
   * Calculate block index from local XYZ position within chunk
   * @param localX X position within chunk (0 to size-1)
   * @param localY Y position (0 to worldHeight-1)
   * @param localZ Z position within chunk (0 to size-1)
   * @param size Chunk size
   * @returns Flat array index
   */
  export function getBlockIndex(
    localX: number,
    localY: number,
    localZ: number,
    size: number
  ): number {
    return localX + localZ * size + localY * size * size;
  }

  /**
   * Get block type ID at local position within chunk
   * @param chunk Chunk data
   * @param localX X position within chunk (0 to size-1)
   * @param localY Y position (0 to worldHeight-1)
   * @param localZ Z position within chunk (0 to size-1)
   * @returns Block type ID (0 = air)
   */
  export function getBlock(
    chunk: ChunkData,
    localX: number,
    localY: number,
    localZ: number
  ): number {
    const index = getBlockIndex(localX, localY, localZ, chunk.size);
    return chunk.blocks[index] ?? 0;
  }

  /**
   * Set block type ID at local position within chunk
   * @param chunk Chunk data
   * @param localX X position within chunk (0 to size-1)
   * @param localY Y position (0 to worldHeight-1)
   * @param localZ Z position within chunk (0 to size-1)
   * @param blockTypeId Block type ID to set
   */
  export function setBlock(
    chunk: ChunkData,
    localX: number,
    localY: number,
    localZ: number,
    blockTypeId: number
  ): void {
    const index = getBlockIndex(localX, localY, localZ, chunk.size);
    chunk.blocks[index] = blockTypeId;
    chunk.isDirty = true;
    chunk.lastModified = Date.now();
  }

  /**
   * Create empty chunk data
   * @param cx Chunk X coordinate (in chunk space)
   * @param cz Chunk Z coordinate (in chunk space)
   * @param size Chunk size
   * @param worldHeight World height
   * @returns New empty chunk
   */
  export function create(
    cx: number,
    cz: number,
    size: number,
    worldHeight: number
  ): ChunkData {
    const totalBlocks = size * worldHeight * size;

    return {
      cx,
      cz,
      size,
      blocks: new Uint16Array(totalBlocks),
      status: ChunkStatus.EMPTY,
      isDirty: false,
    };
  }

  /**
   * Clone chunk data
   * @param chunk Chunk to clone
   * @returns New chunk with copied data
   */
  export function clone(chunk: ChunkData): ChunkData {
    return {
      ...chunk,
      blocks: new Uint16Array(chunk.blocks),
      heightData: chunk.heightData ? [...chunk.heightData] : undefined,
      sparseBlocks: chunk.sparseBlocks
        ? new Map(chunk.sparseBlocks)
        : undefined,
    };
  }

  /**
   * Check if chunk is empty (all air)
   * @param chunk Chunk to check
   * @returns true if all blocks are air
   */
  export function isEmpty(chunk: ChunkData): boolean {
    return chunk.blocks.every((blockId) => blockId === 0);
  }

  /**
   * Count non-air blocks
   * @param chunk Chunk to analyze
   * @returns Number of non-air blocks
   */
  export function countBlocks(chunk: ChunkData): number {
    let count = 0;
    for (let i = 0; i < chunk.blocks.length; i++) {
      if (chunk.blocks[i] !== 0) count++;
    }
    return count;
  }

  /**
   * Convert to sparse representation if beneficial
   * @param chunk Chunk to optimize
   * @param threshold Percentage of non-air blocks (0-1)
   */
  export function optimizeStorage(chunk: ChunkData, threshold = 0.1): void {
    const totalBlocks = chunk.blocks.length;
    const nonAirBlocks = countBlocks(chunk);
    const density = nonAirBlocks / totalBlocks;

    if (density < threshold) {
      // Convert to sparse representation
      chunk.sparseBlocks = new Map();
      for (let i = 0; i < chunk.blocks.length; i++) {
        const blockId = chunk.blocks[i];
        if (blockId !== 0) {
          chunk.sparseBlocks.set(i, blockId);
        }
      }
    }
  }

  /**
   * Get chunk key for mapping
   * @param cx Chunk X coordinate
   * @param cz Chunk Z coordinate
   * @returns String key "cx,cz"
   */
  export function getKey(cx: number, cz: number): string {
    return `${cx},${cz}`;
  }

  /**
   * Parse chunk key
   * @param key String key "cx,cz"
   * @returns Chunk coordinates or null
   */
  export function parseKey(key: string): { cx: number; cz: number } | null {
    const parts = key.split(',');
    if (parts.length !== 2) return null;

    const cx = parseInt(parts[0], 10);
    const cz = parseInt(parts[1], 10);

    if (isNaN(cx) || isNaN(cz)) return null;

    return { cx, cz };
  }

  /**
   * Convert world coordinates to chunk coordinates
   * @param x World X coordinate
   * @param z World Z coordinate
   * @param chunkSize Chunk size
   * @returns Chunk coordinates
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
   * @param x World X coordinate
   * @param z World Z coordinate
   * @param chunkSize Chunk size
   * @returns Local coordinates within chunk
   */
  export function worldToLocal(
    x: number,
    z: number,
    chunkSize: number
  ): { localX: number; localZ: number } {
    return {
      localX: ((x % chunkSize) + chunkSize) % chunkSize, // Handle negative coordinates
      localZ: ((z % chunkSize) + chunkSize) % chunkSize,
    };
  }

  /**
   * Convert chunk + local coordinates to world coordinates
   * @param cx Chunk X coordinate
   * @param cz Chunk Z coordinate
   * @param localX Local X within chunk
   * @param localZ Local Z within chunk
   * @param chunkSize Chunk size
   * @returns World coordinates
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
}
