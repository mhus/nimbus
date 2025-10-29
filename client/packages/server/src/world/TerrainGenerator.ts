/**
 * Terrain Generator - Simple flat plane for testing
 */

import type { ChunkData } from '@nimbus/shared';

export class TerrainGenerator {
  /**
   * Generate a flat plane at Y=0 with stone (blockTypeId: 1)
   */
  generateChunk(cx: number, cz: number, chunkSize: number, worldHeight: number): ChunkData {
    const blocks = new Uint16Array(chunkSize * worldHeight * chunkSize);

    // Fill Y=0 with stone (ID: 1), rest with air (ID: 0)
    for (let x = 0; x < chunkSize; x++) {
      for (let z = 0; z < chunkSize; z++) {
        const y = 0; // Ground level
        const index = x + z * chunkSize + y * chunkSize * chunkSize;
        blocks[index] = 220; // Stone
      }
    }

    // Height data: [maxHeight, minHeight, groundLevel, waterHeight]
    const heightData: number[] = [];
    for (let i = 0; i < chunkSize * chunkSize; i++) {
      heightData.push(0, 0, 1, 0); // Flat at Y=0
    }

    return {
      cx,
      cz,
      size: chunkSize,
      blocks,
      heightData,
      status: 0,
    };
  }
}
