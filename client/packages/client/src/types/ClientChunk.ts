/**
 * ClientChunk - Client-side chunk representation
 *
 * Wraps ChunkDataTransferObject from server with additional
 * client-side state for rendering and management.
 */

import type { ChunkDataTransferObject } from '@nimbus/shared';

/**
 * Client-side chunk with rendering state
 */
export interface ClientChunk {
  /** Chunk data from server */
  data: ChunkDataTransferObject;

  /** Whether chunk has been rendered */
  isRendered: boolean;

  /** Last time chunk was accessed (for LRU) */
  lastAccessTime: number;

  /** Optional reference to Babylon.js mesh */
  renderMesh?: any;
}
