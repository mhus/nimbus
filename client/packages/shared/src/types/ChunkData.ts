/**
 * ChunkData - Internal chunk representation
 *
 * This is the internal representation of a chunk for both client and server.
 * For network transfer, see ChunkDataTransferObject.
 *
 * Chunks are always columns (XZ coordinates, full Y height).
 */

/**
 * Block instance in chunk (sparse storage)
 */
export interface Block {
  x: number;
  y: number;
  z: number;
  blockTypeId: number;
  status?: number;
  modifierIndex?: number;
  metadata?: any;
}

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
   * Sparse block data - array of Block instances
   * Only stores non-air blocks (blockTypeId !== 0)
   * Each Block contains world coordinates (x, y, z)
   */
  blocks: Block[];

  /**
   * Height data per XZ position (optional)
   * Flat array of height values
   */
  heightData?: number[];
}
