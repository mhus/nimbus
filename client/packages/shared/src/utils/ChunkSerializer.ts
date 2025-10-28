/**
 * Chunk serialization utilities
 * Convert between ChunkData and ChunkDataTransferObject
 */

import type { ChunkData } from '../types/ChunkData';
import { ChunkDataHelper } from '../types/ChunkData';
import type {
  ChunkDataTransferObject,
  HeightData,
} from '../network/messages/ChunkMessage';
import type { Block } from '../types/Block';
import type { BlockType } from '../types/BlockType';

/**
 * Chunk serialization helpers
 */
export namespace ChunkSerializer {
  /**
   * Convert ChunkData to ChunkDataTransferObject (for network transfer)
   * @param chunk Internal chunk data
   * @param worldHeight World height
   * @returns Transfer object
   */
  export function chunkToTransferObject(
    chunk: ChunkData,
    worldHeight: number = 256
  ): ChunkDataTransferObject {
    const blocks: Block[] = [];
    const heightData: HeightData[] = [];

    // Extract only non-air blocks
    for (let y = 0; y < worldHeight; y++) {
      for (let z = 0; z < chunk.size; z++) {
        for (let x = 0; x < chunk.size; x++) {
          const blockId = ChunkDataHelper.getBlock(chunk, x, y, z);

          if (blockId !== 0) {
            // Convert to world coordinates
            const worldPos = ChunkDataHelper.localToWorld(
              chunk.cx,
              chunk.cz,
              x,
              z,
              chunk.size
            );

            blocks.push({
              position: {
                x: worldPos.x,
                y: y,
                z: worldPos.z,
              },
              blockTypeId: blockId,
            });
          }
        }
      }
    }

    // Convert height data if present
    if (chunk.heightData) {
      const heightCount = chunk.size * chunk.size;
      for (let i = 0; i < heightCount; i++) {
        const offset = i * 4;
        heightData.push([
          chunk.heightData[offset],
          chunk.heightData[offset + 1],
          chunk.heightData[offset + 2],
          chunk.heightData[offset + 3],
        ]);
      }
    }

    return {
      cx: chunk.cx,
      cz: chunk.cz,
      b: blocks,
      h: heightData,
      // a and e are added by caller if needed
    };
  }

  /**
   * Convert ChunkDataTransferObject to ChunkData
   * @param transferObj Transfer object from network
   * @param chunkSize Chunk size (from world settings)
   * @param worldHeight World height
   * @returns Internal chunk data
   */
  export function transferObjectToChunk(
    transferObj: ChunkDataTransferObject,
    chunkSize: number = 16,
    worldHeight: number = 256
  ): ChunkData {
    const chunk = ChunkDataHelper.create(
      transferObj.cx,
      transferObj.cz,
      chunkSize,
      worldHeight
    );

    // Fill blocks
    transferObj.b.forEach((block) => {
      // Convert world to local coordinates
      const local = ChunkDataHelper.worldToLocal(
        block.position.x,
        block.position.z,
        chunkSize
      );

      ChunkDataHelper.setBlock(
        chunk,
        local.localX,
        block.position.y,
        local.localZ,
        block.blockTypeId
      );
    });

    // Fill height data if present
    if (transferObj.h && transferObj.h.length > 0) {
      chunk.heightData = new Array(chunk.size * chunk.size * 4);

      transferObj.h.forEach((heightData, index) => {
        const offset = index * 4;
        chunk.heightData![offset] = heightData[0]; // maxHeight
        chunk.heightData![offset + 1] = heightData[1]; // minHeight
        chunk.heightData![offset + 2] = heightData[2]; // groundLevel
        chunk.heightData![offset + 3] = heightData[3]; // waterHeight
      });
    }

    chunk.isDirty = false;
    chunk.lastModified = Date.now();

    return chunk;
  }

  /**
   * Create delta update (only changed blocks)
   * @param oldChunk Previous chunk state
   * @param newChunk New chunk state
   * @param chunkSize Chunk size
   * @returns Array of changed blocks
   */
  export function createDeltaUpdate(
    oldChunk: ChunkData,
    newChunk: ChunkData,
    chunkSize: number = 16
  ): Block[] {
    const changedBlocks: Block[] = [];

    if (oldChunk.blocks.length !== newChunk.blocks.length) {
      console.warn('Chunk sizes do not match, cannot create delta');
      return [];
    }

    for (let i = 0; i < newChunk.blocks.length; i++) {
      if (oldChunk.blocks[i] !== newChunk.blocks[i]) {
        // Block changed, convert index to XYZ
        const localX = i % chunkSize;
        const localZ = Math.floor(i / chunkSize) % chunkSize;
        const localY = Math.floor(i / (chunkSize * chunkSize));

        // Convert to world coordinates
        const worldPos = ChunkDataHelper.localToWorld(
          newChunk.cx,
          newChunk.cz,
          localX,
          localZ,
          chunkSize
        );

        changedBlocks.push({
          position: {
            x: worldPos.x,
            y: localY,
            z: worldPos.z,
          },
          blockTypeId: newChunk.blocks[i],
        });
      }
    }

    return changedBlocks;
  }

  /**
   * Serialize ChunkData to JSON (for saving/loading)
   * @param chunk Chunk data
   * @returns JSON string
   */
  export function toJSON(chunk: ChunkData): string {
    // Convert Uint16Array to regular array for JSON
    const serializable = {
      cx: chunk.cx,
      cz: chunk.cz,
      size: chunk.size,
      blocks: Array.from(chunk.blocks),
      heightData: chunk.heightData,
      status: chunk.status,
      lastModified: chunk.lastModified,
    };

    return JSON.stringify(serializable);
  }

  /**
   * Deserialize ChunkData from JSON
   * @param json JSON string
   * @param worldHeight World height
   * @returns ChunkData or null if invalid
   */
  export function fromJSON(json: string, worldHeight: number = 256): ChunkData | null {
    try {
      const data = JSON.parse(json);

      if (!data || typeof data !== 'object' || Array.isArray(data)) {
        return null;
      }

      const chunk = ChunkDataHelper.create(
        data.cx,
        data.cz,
        data.size,
        worldHeight
      );

      // Restore blocks
      if (Array.isArray(data.blocks)) {
        chunk.blocks = new Uint16Array(data.blocks);
      }

      // Restore height data
      if (data.heightData) {
        chunk.heightData = data.heightData;
      }

      chunk.status = data.status ?? 0;
      chunk.lastModified = data.lastModified;

      return chunk;
    } catch (e) {
      console.error('Failed to parse ChunkData JSON:', e);
      return null;
    }
  }

  /**
   * Compress chunk data (remove air blocks from array)
   * @param chunk Chunk data
   * @returns Compressed representation
   */
  export function compress(chunk: ChunkData): any {
    const nonAirBlocks: Array<{ index: number; blockId: number }> = [];

    for (let i = 0; i < chunk.blocks.length; i++) {
      if (chunk.blocks[i] !== 0) {
        nonAirBlocks.push({ index: i, blockId: chunk.blocks[i] });
      }
    }

    return {
      cx: chunk.cx,
      cz: chunk.cz,
      size: chunk.size,
      blocks: nonAirBlocks,
      heightData: chunk.heightData,
    };
  }

  /**
   * Decompress chunk data
   * @param compressed Compressed representation
   * @param worldHeight World height
   * @returns ChunkData
   */
  export function decompress(
    compressed: any,
    worldHeight: number = 256
  ): ChunkData {
    const chunk = ChunkDataHelper.create(
      compressed.cx,
      compressed.cz,
      compressed.size,
      worldHeight
    );

    // Restore non-air blocks
    if (Array.isArray(compressed.blocks)) {
      compressed.blocks.forEach(
        (entry: { index: number; blockId: number }) => {
          chunk.blocks[entry.index] = entry.blockId;
        }
      );
    }

    if (compressed.heightData) {
      chunk.heightData = compressed.heightData;
    }

    return chunk;
  }

  /**
   * Calculate compression ratio
   * @param chunk Chunk data
   * @returns Ratio (0-1, lower is better compression)
   */
  export function getCompressionRatio(chunk: ChunkData): number {
    const nonAirCount = ChunkDataHelper.countBlocks(chunk);
    const totalBlocks = chunk.blocks.length;

    return nonAirCount / totalBlocks;
  }
}

/**
 * BlockType serialization helpers
 */
export namespace BlockTypeSerializer {
  /**
   * Serialize BlockType to JSON
   * @param blockType BlockType to serialize
   * @returns JSON string
   */
  export function toJSON(blockType: BlockType): string {
    return JSON.stringify(blockType);
  }

  /**
   * Deserialize BlockType from JSON
   * @param json JSON string
   * @returns BlockType or null
   */
  export function fromJSON(json: string): BlockType | null {
    try {
      const data = JSON.parse(json);

      if (!data || !data.id === undefined || !data.modifiers) {
        return null;
      }

      return data as BlockType;
    } catch (e) {
      console.error('Failed to parse BlockType JSON:', e);
      return null;
    }
  }

  /**
   * Serialize BlockType registry (map of ID -> BlockType)
   * @param registry Map of block types
   * @returns JSON string
   */
  export function registryToJSON(registry: Map<number, BlockType>): string {
    const array = Array.from(registry.values());
    return JSON.stringify(array);
  }

  /**
   * Deserialize BlockType registry
   * @param json JSON string
   * @returns Map of block types or null
   */
  export function registryFromJSON(json: string): Map<number, BlockType> | null {
    try {
      const data = JSON.parse(json);

      if (!Array.isArray(data)) {
        return null;
      }

      const registry = new Map<number, BlockType>();
      data.forEach((blockType: BlockType) => {
        registry.set(blockType.id, blockType);
      });

      return registry;
    } catch (e) {
      console.error('Failed to parse BlockType registry JSON:', e);
      return null;
    }
  }
}
