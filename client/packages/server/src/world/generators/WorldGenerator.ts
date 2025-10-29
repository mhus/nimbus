/**
 * WorldGenerator - Base interface for terrain generators
 */

import type { ServerChunk } from '../../types/ServerChunk';

/**
 * World generator interface
 */
export interface WorldGenerator {
  /** Generator name/type */
  readonly name: string;

  /**
   * Generate a chunk
   *
   * @param cx Chunk X coordinate
   * @param cz Chunk Z coordinate
   * @param chunkSize Chunk size (blocks per side)
   * @returns Generated ServerChunk
   */
  generateChunk(cx: number, cz: number, chunkSize: number): ServerChunk;
}

/**
 * Generator configuration from generator.json
 */
export interface GeneratorConfig {
  /** Generator type (flat, normal, etc.) */
  type: string;

  /** Random seed for terrain generation */
  seed?: number;

  /** Additional generator-specific parameters */
  parameters?: Record<string, any>;
}
