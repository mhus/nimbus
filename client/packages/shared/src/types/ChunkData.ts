/**
 * ChunkData - Internal chunk representation
 *
 * This is the internal representation of a chunk for both client and server.
 * For network transfer, see ChunkDataTransferObject.
 *
 * Chunks are always columns (XZ coordinates, full Y height).
 */

import type { Block } from './Block';

/**
 * Height data for chunk column
 * Array of 4 values describing height information for a specific XZ position
 */
export type HeightData = readonly [
  x: number,
  z: number,
  maxHeight: number,
  groundLevel: number,
  waterLevel?: number
];

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
  heightData?: HeightData[];
}
