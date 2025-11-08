/**
 * ClientChunk - Client-side chunk representation
 *
 * Wraps ChunkDataTransferObject from server with additional
 * client-side state for rendering and management.
 */

import type { ChunkDataTransferObject, Backdrop } from '@nimbus/shared';
import type { ClientBlock } from './ClientBlock';
import type { DisposableResources } from '../rendering/DisposableResources';

/**
 * Height data for chunk column
 *
 * Describes vertical boundaries and special levels for a single column (x, z) in a chunk.
 *
 * @field x - Local X coordinate within chunk (0 to chunkSize-1)
 * @field z - Local Z coordinate within chunk (0 to chunkSize-1)
 * @field maxHeight - Maximum Y boundary:
 *   - Usually world.stop.y (e.g. 1000)
 *   - Exception: If blocks exceed world.stop.y, set to (highestBlock + 10) for headroom
 *   - Can be overridden by server via chunkData.h
 * @field minHeight - Minimum Y boundary (lowest block Y position, or world.start.y if no blocks)
 * @field groundLevel - Y position of lowest solid block (ground surface)
 * @field waterHeight - Y position of highest water block (water surface), undefined if no water
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

  statusData: Map<string, number>;

  /** Backdrop data for chunk edges (with defaults applied) */
  backdrop?: {
    n?: Array<Backdrop>;
    e?: Array<Backdrop>;
    s?: Array<Backdrop>;
    w?: Array<Backdrop>;
  };

  /** Disposable rendering resources (meshes, sprites, etc.) created for this chunk */
  resourcesToDispose?: DisposableResources;
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
