/**
 * Terrain Generator - Simple flat plane for testing
 */

import { ServerChunk, ChunkStatus } from '../types/ServerChunk';

export class TerrainGenerator {
  /**
   * Generate a flat plane at Y=0 with stone (blockTypeId: 220)
   */
  generateChunk(cx: number, cz: number, chunkSize: number): ServerChunk {
    const chunk = new ServerChunk(cx, cz, chunkSize);

    // Fill Y=0 with stone (blockTypeId: 220)
    const worldX = cx * chunkSize;
    const worldZ = cz * chunkSize;

    for (let localX = 0; localX < chunkSize; localX++) {
      for (let localZ = 0; localZ < chunkSize; localZ++) {
        chunk.setBlock({
          x: worldX + localX,
          y: 0,
          z: worldZ + localZ,
          blockTypeId: 220, // Stone
          status: 0,
        });
      }
    }

    chunk.status = ChunkStatus.READY;
    return chunk;
  }
}
