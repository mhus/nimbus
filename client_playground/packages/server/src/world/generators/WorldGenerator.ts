/**
 * World Generator interface
 */

import type { ChunkData, XZ } from '@nimbus-client/core';

/**
 * Base interface for world generators
 */
export interface WorldGenerator {
  /**
   * Generator name
   */
  readonly name: string;

  /**
   * Generate a chunk
   * @param chunkX Chunk X coordinate
   * @param chunkZ Chunk Z coordinate
   * @param chunkSize Size of chunk (blocks per dimension)
   * @param height Height of chunk
   */
  generateChunk(chunkX: number, chunkZ: number, chunkSize: number, height: number): Promise<ChunkData>;
}
