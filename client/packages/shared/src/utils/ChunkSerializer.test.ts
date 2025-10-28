/**
 * Tests for ChunkSerializer
 */

import { ChunkSerializer } from './ChunkSerializer';
import { ChunkDataHelper } from '../types/ChunkData';
import type { ChunkDataTransferObject } from '../network/messages/ChunkMessage';

describe('ChunkSerializer', () => {
  describe('chunkToTransferObject', () => {
    it('should convert empty chunk to transfer object', () => {
      const chunk = ChunkDataHelper.create(0, 0, 16, 256);
      const transferObj = ChunkSerializer.chunkToTransferObject(chunk);

      expect(transferObj.cx).toBe(0);
      expect(transferObj.cz).toBe(0);
      expect(transferObj.b).toEqual([]);
      expect(transferObj.h).toEqual([]);
    });

    it('should convert chunk with blocks to transfer object', () => {
      const chunk = ChunkDataHelper.create(1, 2, 16, 256);

      // Add some blocks
      ChunkDataHelper.setBlock(chunk, 0, 0, 0, 1); // stone at (0,0,0)
      ChunkDataHelper.setBlock(chunk, 1, 1, 1, 2); // grass at (1,1,1)

      const transferObj = ChunkSerializer.chunkToTransferObject(chunk);

      expect(transferObj.cx).toBe(1);
      expect(transferObj.cz).toBe(2);
      expect(transferObj.b).toHaveLength(2);

      // Blocks should be in world coordinates
      expect(transferObj.b[0].position).toEqual({ x: 16, y: 0, z: 32 });
      expect(transferObj.b[0].blockTypeId).toBe(1);

      expect(transferObj.b[1].position).toEqual({ x: 17, y: 1, z: 33 });
      expect(transferObj.b[1].blockTypeId).toBe(2);
    });

    it('should not include air blocks (blockId 0)', () => {
      const chunk = ChunkDataHelper.create(0, 0, 16, 256);

      ChunkDataHelper.setBlock(chunk, 0, 0, 0, 0); // air
      ChunkDataHelper.setBlock(chunk, 1, 1, 1, 1); // stone

      const transferObj = ChunkSerializer.chunkToTransferObject(chunk);

      expect(transferObj.b).toHaveLength(1);
      expect(transferObj.b[0].blockTypeId).toBe(1);
    });

    it('should convert height data if present', () => {
      const chunk = ChunkDataHelper.create(0, 0, 2, 256);
      chunk.heightData = new Array(2 * 2 * 4); // 2x2 chunk

      // Set height data for first position
      chunk.heightData[0] = 64; // maxHeight
      chunk.heightData[1] = 0;  // minHeight
      chunk.heightData[2] = 32; // groundLevel
      chunk.heightData[3] = 0;  // waterHeight

      const transferObj = ChunkSerializer.chunkToTransferObject(chunk);

      expect(transferObj.h).toHaveLength(4); // 2x2 positions
      expect(transferObj.h[0]).toEqual([64, 0, 32, 0]);
    });
  });

  describe('transferObjectToChunk', () => {
    it('should convert empty transfer object to chunk', () => {
      const transferObj: ChunkDataTransferObject = {
        cx: 1,
        cz: 2,
        b: [],
        h: [],
      };

      const chunk = ChunkSerializer.transferObjectToChunk(transferObj, 16, 256);

      expect(chunk.cx).toBe(1);
      expect(chunk.cz).toBe(2);
      expect(chunk.size).toBe(16);
      expect(chunk.isDirty).toBe(false);
    });

    it('should convert transfer object with blocks to chunk', () => {
      const transferObj: ChunkDataTransferObject = {
        cx: 1,
        cz: 2,
        b: [
          { position: { x: 16, y: 0, z: 32 }, blockTypeId: 1 },
          { position: { x: 17, y: 1, z: 33 }, blockTypeId: 2 },
        ],
        h: [],
      };

      const chunk = ChunkSerializer.transferObjectToChunk(transferObj, 16, 256);

      // Check blocks are set correctly (in local coordinates)
      expect(ChunkDataHelper.getBlock(chunk, 0, 0, 0)).toBe(1);
      expect(ChunkDataHelper.getBlock(chunk, 1, 1, 1)).toBe(2);
    });

    it('should convert height data if present', () => {
      const transferObj: ChunkDataTransferObject = {
        cx: 0,
        cz: 0,
        b: [],
        h: [
          [64, 0, 32, 0],
          [60, 5, 30, 0],
        ],
      };

      const chunk = ChunkSerializer.transferObjectToChunk(transferObj, 2, 256);

      expect(chunk.heightData).toBeDefined();
      expect(chunk.heightData![0]).toBe(64);
      expect(chunk.heightData![1]).toBe(0);
      expect(chunk.heightData![2]).toBe(32);
      expect(chunk.heightData![3]).toBe(0);
    });
  });

  describe('createDeltaUpdate', () => {
    it('should return empty array for identical chunks', () => {
      const chunk1 = ChunkDataHelper.create(0, 0, 16, 256);
      const chunk2 = ChunkDataHelper.create(0, 0, 16, 256);

      const delta = ChunkSerializer.createDeltaUpdate(chunk1, chunk2, 16);

      expect(delta).toEqual([]);
    });

    it('should return changed blocks', () => {
      const chunk1 = ChunkDataHelper.create(0, 0, 16, 256);
      const chunk2 = ChunkDataHelper.create(0, 0, 16, 256);

      // Change some blocks in chunk2
      ChunkDataHelper.setBlock(chunk2, 0, 0, 0, 1);
      ChunkDataHelper.setBlock(chunk2, 1, 1, 1, 2);

      const delta = ChunkSerializer.createDeltaUpdate(chunk1, chunk2, 16);

      expect(delta).toHaveLength(2);
      expect(delta[0].blockTypeId).toBe(1);
      expect(delta[1].blockTypeId).toBe(2);
    });

    it('should return empty array for mismatched chunk sizes', () => {
      const chunk1 = ChunkDataHelper.create(0, 0, 16, 256);
      const chunk2 = ChunkDataHelper.create(0, 0, 32, 256);

      const delta = ChunkSerializer.createDeltaUpdate(chunk1, chunk2, 16);

      expect(delta).toEqual([]);
    });
  });

  describe('toJSON and fromJSON', () => {
    it('should serialize empty chunk to JSON', () => {
      const chunk = ChunkDataHelper.create(1, 2, 16, 256);

      const json = ChunkSerializer.toJSON(chunk);
      expect(json).toBeTruthy();
      expect(typeof json).toBe('string');
    });

    it('should deserialize chunk from JSON', () => {
      const original = ChunkDataHelper.create(1, 2, 16, 256);
      ChunkDataHelper.setBlock(original, 0, 0, 0, 1);
      ChunkDataHelper.setBlock(original, 1, 1, 1, 2);

      const json = ChunkSerializer.toJSON(original);
      const deserialized = ChunkSerializer.fromJSON(json, 256);

      expect(deserialized).not.toBeNull();
      expect(deserialized!.cx).toBe(1);
      expect(deserialized!.cz).toBe(2);
      expect(deserialized!.size).toBe(16);
      expect(ChunkDataHelper.getBlock(deserialized!, 0, 0, 0)).toBe(1);
      expect(ChunkDataHelper.getBlock(deserialized!, 1, 1, 1)).toBe(2);
    });

    it('should return null for invalid JSON', () => {
      const chunk = ChunkSerializer.fromJSON('invalid json', 256);
      expect(chunk).toBeNull();
    });

    it('should return null for non-object JSON', () => {
      const chunk = ChunkSerializer.fromJSON('[]', 256);
      expect(chunk).toBeNull();
    });

    it('should preserve height data through serialization', () => {
      const original = ChunkDataHelper.create(0, 0, 16, 256);
      original.heightData = new Array(16 * 16 * 4).fill(0);
      original.heightData[0] = 64;
      original.heightData[1] = 0;
      original.heightData[2] = 32;
      original.heightData[3] = 0;

      const json = ChunkSerializer.toJSON(original);
      const deserialized = ChunkSerializer.fromJSON(json, 256);

      expect(deserialized).not.toBeNull();
      expect(deserialized!.heightData).toBeDefined();
      expect(deserialized!.heightData![0]).toBe(64);
    });
  });

  describe('compress and decompress', () => {
    it('should compress empty chunk', () => {
      const chunk = ChunkDataHelper.create(1, 2, 16, 256);

      const compressed = ChunkSerializer.compress(chunk);

      expect(compressed.cx).toBe(1);
      expect(compressed.cz).toBe(2);
      expect(compressed.size).toBe(16);
      expect(compressed.blocks).toEqual([]);
    });

    it('should compress chunk with blocks', () => {
      const chunk = ChunkDataHelper.create(0, 0, 16, 256);
      ChunkDataHelper.setBlock(chunk, 0, 0, 0, 1);
      ChunkDataHelper.setBlock(chunk, 1, 1, 1, 2);

      const compressed = ChunkSerializer.compress(chunk);

      expect(compressed.blocks).toHaveLength(2);
      expect(compressed.blocks[0].blockId).toBe(1);
      expect(compressed.blocks[1].blockId).toBe(2);
    });

    it('should decompress empty compressed chunk', () => {
      const compressed = {
        cx: 1,
        cz: 2,
        size: 16,
        blocks: [],
      };

      const chunk = ChunkSerializer.decompress(compressed, 256);

      expect(chunk.cx).toBe(1);
      expect(chunk.cz).toBe(2);
      expect(chunk.size).toBe(16);
      expect(ChunkDataHelper.countBlocks(chunk)).toBe(0);
    });

    it('should decompress compressed chunk', () => {
      const compressed = {
        cx: 0,
        cz: 0,
        size: 16,
        blocks: [
          { index: 0, blockId: 1 },
          { index: 1, blockId: 2 },
        ],
      };

      const chunk = ChunkSerializer.decompress(compressed, 256);

      expect(chunk.blocks[0]).toBe(1);
      expect(chunk.blocks[1]).toBe(2);
    });

    it('should preserve data through compression cycle', () => {
      const original = ChunkDataHelper.create(1, 2, 16, 256);
      ChunkDataHelper.setBlock(original, 0, 0, 0, 1);
      ChunkDataHelper.setBlock(original, 5, 10, 3, 2);

      const compressed = ChunkSerializer.compress(original);
      const decompressed = ChunkSerializer.decompress(compressed, 256);

      expect(decompressed.cx).toBe(original.cx);
      expect(decompressed.cz).toBe(original.cz);
      expect(ChunkDataHelper.getBlock(decompressed, 0, 0, 0)).toBe(1);
      expect(ChunkDataHelper.getBlock(decompressed, 5, 10, 3)).toBe(2);
    });
  });

  describe('getCompressionRatio', () => {
    it('should return 0 for empty chunk', () => {
      const chunk = ChunkDataHelper.create(0, 0, 16, 256);

      const ratio = ChunkSerializer.getCompressionRatio(chunk);

      expect(ratio).toBe(0);
    });

    it('should return ratio for partially filled chunk', () => {
      const chunk = ChunkDataHelper.create(0, 0, 16, 256);

      // Fill bottom layer (16x16 = 256 blocks)
      for (let x = 0; x < 16; x++) {
        for (let z = 0; z < 16; z++) {
          ChunkDataHelper.setBlock(chunk, x, 0, z, 1);
        }
      }

      const ratio = ChunkSerializer.getCompressionRatio(chunk);

      // 256 blocks out of 16*16*256 = 65536 total
      expect(ratio).toBeCloseTo(256 / 65536, 4);
    });

    it('should return 1 for completely filled chunk', () => {
      const chunk = ChunkDataHelper.create(0, 0, 2, 4);

      // Fill all blocks
      for (let y = 0; y < 4; y++) {
        for (let x = 0; x < 2; x++) {
          for (let z = 0; z < 2; z++) {
            ChunkDataHelper.setBlock(chunk, x, y, z, 1);
          }
        }
      }

      const ratio = ChunkSerializer.getCompressionRatio(chunk);

      expect(ratio).toBe(1);
    });
  });

  describe('round-trip serialization', () => {
    it('should preserve chunk through full serialization cycle', () => {
      const original = ChunkDataHelper.create(5, 7, 16, 256);

      // Add some blocks
      ChunkDataHelper.setBlock(original, 0, 0, 0, 1);
      ChunkDataHelper.setBlock(original, 5, 10, 3, 2);
      ChunkDataHelper.setBlock(original, 15, 255, 15, 3);

      // Round-trip through JSON
      const json = ChunkSerializer.toJSON(original);
      const fromJson = ChunkSerializer.fromJSON(json, 256);

      // Round-trip through transfer object
      const transferObj = ChunkSerializer.chunkToTransferObject(original);
      const fromTransfer = ChunkSerializer.transferObjectToChunk(transferObj, 16, 256);

      // Verify JSON round-trip
      expect(fromJson).not.toBeNull();
      expect(fromJson!.cx).toBe(5);
      expect(fromJson!.cz).toBe(7);
      expect(ChunkDataHelper.getBlock(fromJson!, 0, 0, 0)).toBe(1);
      expect(ChunkDataHelper.getBlock(fromJson!, 5, 10, 3)).toBe(2);
      expect(ChunkDataHelper.getBlock(fromJson!, 15, 255, 15)).toBe(3);

      // Verify transfer object round-trip
      expect(fromTransfer.cx).toBe(5);
      expect(fromTransfer.cz).toBe(7);
      expect(ChunkDataHelper.getBlock(fromTransfer, 0, 0, 0)).toBe(1);
      expect(ChunkDataHelper.getBlock(fromTransfer, 5, 10, 3)).toBe(2);
      expect(ChunkDataHelper.getBlock(fromTransfer, 15, 255, 15)).toBe(3);
    });
  });
});
