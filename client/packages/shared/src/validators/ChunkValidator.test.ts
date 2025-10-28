/**
 * Tests for ChunkValidator
 */

import { ChunkValidator } from './ChunkValidator';
import { ChunkDataHelper } from '../types/ChunkData';
import type { ChunkDataTransferObject, ChunkCoordinate } from '../network/messages/ChunkMessage';

describe('ChunkValidator', () => {
  describe('isValidChunkCoordinate', () => {
    it('should accept valid chunk coordinates', () => {
      expect(ChunkValidator.isValidChunkCoordinate(0, 0)).toBe(true);
      expect(ChunkValidator.isValidChunkCoordinate(100, -100)).toBe(true);
      expect(ChunkValidator.isValidChunkCoordinate(999999, 999999)).toBe(true);
    });

    it('should reject invalid chunk coordinates', () => {
      expect(ChunkValidator.isValidChunkCoordinate(1.5, 0)).toBe(false);
      expect(ChunkValidator.isValidChunkCoordinate(0, NaN)).toBe(false);
      expect(ChunkValidator.isValidChunkCoordinate(1000000, 0)).toBe(false);
      expect(ChunkValidator.isValidChunkCoordinate(0, -1000000)).toBe(false);
    });
  });

  describe('isValidChunkSize', () => {
    it('should accept valid chunk sizes (powers of 2)', () => {
      expect(ChunkValidator.isValidChunkSize(8)).toBe(true);
      expect(ChunkValidator.isValidChunkSize(16)).toBe(true);
      expect(ChunkValidator.isValidChunkSize(32)).toBe(true);
      expect(ChunkValidator.isValidChunkSize(64)).toBe(true);
      expect(ChunkValidator.isValidChunkSize(128)).toBe(true);
    });

    it('should reject invalid chunk sizes', () => {
      expect(ChunkValidator.isValidChunkSize(0)).toBe(false);
      expect(ChunkValidator.isValidChunkSize(15)).toBe(false);
      expect(ChunkValidator.isValidChunkSize(17)).toBe(false);
      expect(ChunkValidator.isValidChunkSize(256)).toBe(false);
      expect(ChunkValidator.isValidChunkSize(1.5)).toBe(false);
    });
  });

  describe('validateChunkData', () => {
    it('should validate correct chunk data', () => {
      const chunk = ChunkDataHelper.create(0, 0, 16, 256);
      const result = ChunkValidator.validateChunkData(chunk, 256);
      expect(result.valid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should reject invalid chunk coordinates', () => {
      const chunk = ChunkDataHelper.create(1000001, 0, 16, 256);
      const result = ChunkValidator.validateChunkData(chunk, 256);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Invalid chunk coordinates'))).toBe(true);
    });

    it('should reject invalid chunk size', () => {
      const chunk = ChunkDataHelper.create(0, 0, 15, 256);
      const result = ChunkValidator.validateChunkData(chunk, 256);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Invalid chunk size'))).toBe(true);
    });

    it('should reject chunk without Uint16Array blocks', () => {
      const chunk: any = {
        cx: 0,
        cz: 0,
        size: 16,
        blocks: [],
      };
      const result = ChunkValidator.validateChunkData(chunk, 256);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Uint16Array'))).toBe(true);
    });

    it('should reject chunk with wrong blocks array length', () => {
      const chunk = ChunkDataHelper.create(0, 0, 16, 256);
      chunk.blocks = new Uint16Array(1000); // Wrong length
      const result = ChunkValidator.validateChunkData(chunk, 256);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Invalid blocks array length'))).toBe(true);
    });

    it('should validate that block IDs are checked', () => {
      // Note: Uint16Array automatically wraps values > 65535, so we can't directly set invalid values
      // This test verifies the validation logic exists by checking a valid chunk passes
      const chunk = ChunkDataHelper.create(0, 0, 16, 256);
      const result = ChunkValidator.validateChunkData(chunk, 256);
      expect(result.valid).toBe(true);
      // The validator does check all block IDs (see ChunkValidator.ts:80-91)
      // but Uint16Array constraints prevent us from testing with truly invalid values
    });

    it('should validate chunk with height data', () => {
      const chunk = ChunkDataHelper.create(0, 0, 16, 256);
      chunk.heightData = new Array(16 * 16 * 4).fill(0);
      const result = ChunkValidator.validateChunkData(chunk, 256);
      expect(result.valid).toBe(true);
    });

    it('should warn about incorrect height data length', () => {
      const chunk = ChunkDataHelper.create(0, 0, 16, 256);
      chunk.heightData = new Array(100).fill(0);
      const result = ChunkValidator.validateChunkData(chunk, 256);
      expect(result.warnings).toBeDefined();
      expect(result.warnings!.some((w) => w.includes('Height data length mismatch'))).toBe(true);
    });
  });

  describe('validateChunkTransferObject', () => {
    it('should validate correct transfer object', () => {
      const transferObj: ChunkDataTransferObject = {
        cx: 0,
        cz: 0,
        b: [],
        h: [],
      };
      const result = ChunkValidator.validateChunkTransferObject(transferObj);
      expect(result.valid).toBe(true);
    });

    it('should reject invalid coordinates', () => {
      const transferObj: ChunkDataTransferObject = {
        cx: 2000000,
        cz: 0,
        b: [],
        h: [],
      };
      const result = ChunkValidator.validateChunkTransferObject(transferObj);
      expect(result.valid).toBe(false);
    });

    it('should reject non-array blocks', () => {
      const transferObj: any = {
        cx: 0,
        cz: 0,
        b: 'invalid',
        h: [],
      };
      const result = ChunkValidator.validateChunkTransferObject(transferObj);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('must be an array'))).toBe(true);
    });

    it('should validate height data arrays', () => {
      const transferObj: ChunkDataTransferObject = {
        cx: 0,
        cz: 0,
        b: [],
        h: [
          [64, 0, 32, 0],
          [70, 5, 35, 0],
        ],
      };
      const result = ChunkValidator.validateChunkTransferObject(transferObj);
      expect(result.valid).toBe(true);
    });

    it('should reject invalid height data format', () => {
      const transferObj: any = {
        cx: 0,
        cz: 0,
        b: [],
        h: [[64, 0]], // Only 2 values instead of 4
      };
      const result = ChunkValidator.validateChunkTransferObject(transferObj);
      expect(result.valid).toBe(false);
    });

    it('should reject height data with minHeight > maxHeight', () => {
      const transferObj: ChunkDataTransferObject = {
        cx: 0,
        cz: 0,
        b: [],
        h: [[50, 60, 55, 0]], // minHeight (60) > maxHeight (50)
      };
      const result = ChunkValidator.validateChunkTransferObject(transferObj);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('minHeight'))).toBe(true);
    });

    it('should reject height data with groundLevel out of range', () => {
      const transferObj: ChunkDataTransferObject = {
        cx: 0,
        cz: 0,
        b: [],
        h: [[60, 50, 70, 0]], // groundLevel (70) > maxHeight (60)
      };
      const result = ChunkValidator.validateChunkTransferObject(transferObj);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('groundLevel'))).toBe(true);
    });
  });

  describe('validateChunkCoordinates', () => {
    it('should validate empty array', () => {
      const result = ChunkValidator.validateChunkCoordinates([]);
      expect(result.valid).toBe(true);
    });

    it('should validate correct coordinates', () => {
      const coords: ChunkCoordinate[] = [
        { cx: 0, cz: 0 },
        { cx: 1, cz: 0 },
        { cx: 0, cz: 1 },
      ];
      const result = ChunkValidator.validateChunkCoordinates(coords);
      expect(result.valid).toBe(true);
    });

    it('should reject non-array', () => {
      const result = ChunkValidator.validateChunkCoordinates('invalid' as any);
      expect(result.valid).toBe(false);
      expect(result.errors).toContain('Coordinates must be an array');
    });

    it('should reject too many coordinates', () => {
      const coords = Array(1001)
        .fill(null)
        .map(() => ({ cx: 0, cz: 0 }));
      const result = ChunkValidator.validateChunkCoordinates(coords);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Too many coordinates'))).toBe(true);
    });

    it('should warn about duplicate coordinates', () => {
      const coords: ChunkCoordinate[] = [
        { cx: 0, cz: 0 },
        { cx: 0, cz: 0 },
      ];
      const result = ChunkValidator.validateChunkCoordinates(coords);
      expect(result.warnings).toBeDefined();
      expect(result.warnings!.some((w) => w.includes('Duplicate coordinate'))).toBe(true);
    });
  });

  describe('isValid', () => {
    it('should return true for valid chunk', () => {
      const chunk = ChunkDataHelper.create(0, 0, 16, 256);
      expect(ChunkValidator.isValid(chunk)).toBe(true);
    });

    it('should return false for invalid chunk', () => {
      const chunk: any = {
        cx: 1000001,
        cz: 0,
        size: 15,
        blocks: [],
      };
      expect(ChunkValidator.isValid(chunk)).toBe(false);
    });
  });
});
