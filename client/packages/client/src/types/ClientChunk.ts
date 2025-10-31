/**
 * ClientChunk - Client-side chunk representation
 *
 * Wraps ChunkDataTransferObject from server with additional
 * client-side state for rendering and management.
 */

import type { ChunkDataTransferObject } from '@nimbus/shared';
import type { ClientBlock } from './ClientBlock';

/**
 * Height data for chunk
 * Array of 4 values describing height information
 */
export type ClientHeightData = readonly [
  x: number,
  z: number,
  maxHeight: number,
  minHeight: number,
  groundLevel: number,
  waterHeight?: number
];

/**
 * Client-side chunk data with processed blocks
 */
export interface ClientChunkData {
  /** Original transfer object from server */
  transfer: ChunkDataTransferObject;

  /** Map of block position key(x,y,z) -> ClientBlock (with merged modifiers) */
  data: Map<string, ClientBlock>;
  /** Map of height position key(x,z) -> ClientHeightData */
  hightData: Map<string, ClientHeightData>;
}

/**
 * Client-side chunk with rendering state
 */
export interface ClientChunk {
  /** Chunk data with processed blocks */
  data: ClientChunkData;

  /** Whether chunk has been rendered */
  isRendered: boolean;

  /** Last time chunk was accessed (for LRU) */
  lastAccessTime: number;

  /** Optional reference to Babylon.js mesh */
  renderMesh?: any;
}
