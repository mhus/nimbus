/**
 * Tests for BlockValidator
 */

import { BlockValidator } from './BlockValidator';
import type { Block, Offsets } from '../types/Block';
import type { BlockType } from '../types/BlockType';
import type { BlockMetadata } from '../types/BlockMetadata';
import { BlockStatus } from '../types/BlockType';

describe('BlockValidator', () => {
  describe('isValidBlockTypeId', () => {
    it('should accept valid block type IDs', () => {
      expect(BlockValidator.isValidBlockTypeId(0)).toBe(true);
      expect(BlockValidator.isValidBlockTypeId(1)).toBe(true);
      expect(BlockValidator.isValidBlockTypeId(100)).toBe(true);
      expect(BlockValidator.isValidBlockTypeId(65535)).toBe(true);
    });

    it('should reject invalid block type IDs', () => {
      expect(BlockValidator.isValidBlockTypeId(-1)).toBe(false);
      expect(BlockValidator.isValidBlockTypeId(65536)).toBe(false);
      expect(BlockValidator.isValidBlockTypeId(1.5)).toBe(false);
      expect(BlockValidator.isValidBlockTypeId(NaN)).toBe(false);
      expect(BlockValidator.isValidBlockTypeId(Infinity)).toBe(false);
    });
  });

  describe('isValidStatus', () => {
    it('should accept valid status values', () => {
      expect(BlockValidator.isValidStatus(0)).toBe(true);
      expect(BlockValidator.isValidStatus(1)).toBe(true);
      expect(BlockValidator.isValidStatus(255)).toBe(true);
    });

    it('should reject invalid status values', () => {
      expect(BlockValidator.isValidStatus(-1)).toBe(false);
      expect(BlockValidator.isValidStatus(256)).toBe(false);
      expect(BlockValidator.isValidStatus(1.5)).toBe(false);
    });
  });

  describe('validateOffsets', () => {
    it('should validate empty offsets array', () => {
      const result = BlockValidator.validateOffsets([]);
      expect(result.valid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should validate correct offsets', () => {
      const offsets: Offsets = [10, 20, 30, -10, -20, -30];
      const result = BlockValidator.validateOffsets(offsets);
      expect(result.valid).toBe(true);
    });

    it('should reject non-array', () => {
      const result = BlockValidator.validateOffsets('invalid' as any);
      expect(result.valid).toBe(false);
      expect(result.errors).toContain('Offsets must be an array');
    });

    it('should reject offsets out of range', () => {
      const offsets: Offsets = [128, 0, 0];
      const result = BlockValidator.validateOffsets(offsets);
      expect(result.valid).toBe(false);
      expect(result.errors.length).toBeGreaterThan(0);
      expect(result.errors[0]).toContain('out of range');
    });

    it('should reject non-finite values', () => {
      const offsets: Offsets = [NaN, Infinity, -Infinity];
      const result = BlockValidator.validateOffsets(offsets);
      expect(result.valid).toBe(false);
      // Each non-finite value generates 2 errors: not finite + out of range
      expect(result.errors.length).toBeGreaterThanOrEqual(3);
    });

    it('should warn about non-integer values', () => {
      const offsets: Offsets = [1.5, 2.7, 3.9];
      const result = BlockValidator.validateOffsets(offsets);
      expect(result.warnings).toBeDefined();
      expect(result.warnings!.length).toBe(3);
    });

    it('should warn about unusual offset count', () => {
      const offsets: Offsets = [1, 2];
      const result = BlockValidator.validateOffsets(offsets);
      expect(result.warnings).toBeDefined();
      expect(result.warnings!.some((w) => w.includes('Unusual offset count'))).toBe(true);
    });
  });

  describe('isValidFaceVisibility', () => {
    it('should accept valid face visibility values', () => {
      expect(BlockValidator.isValidFaceVisibility(0)).toBe(true);
      expect(BlockValidator.isValidFaceVisibility(63)).toBe(true);
      expect(BlockValidator.isValidFaceVisibility(127)).toBe(true);
    });

    it('should reject invalid face visibility values', () => {
      expect(BlockValidator.isValidFaceVisibility(-1)).toBe(false);
      expect(BlockValidator.isValidFaceVisibility(128)).toBe(false);
      expect(BlockValidator.isValidFaceVisibility(1.5)).toBe(false);
    });
  });

  describe('validateBlock', () => {
    it('should validate correct block', () => {
      const block: Block = {
        position: { x: 10, y: 64, z: 20 },
        blockTypeId: 1,
      };

      const result = BlockValidator.validateBlock(block);
      expect(result.valid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should reject block without position', () => {
      const block: any = {
        blockTypeId: 1,
      };

      const result = BlockValidator.validateBlock(block);
      expect(result.valid).toBe(false);
      expect(result.errors).toContain('Block position is required');
    });

    it('should reject block with non-finite position coordinates', () => {
      const block: Block = {
        position: { x: NaN, y: Infinity, z: -Infinity },
        blockTypeId: 1,
      };

      const result = BlockValidator.validateBlock(block);
      expect(result.valid).toBe(false);
      expect(result.errors.length).toBeGreaterThanOrEqual(3);
    });

    it('should reject invalid block type ID', () => {
      const block: Block = {
        position: { x: 0, y: 0, z: 0 },
        blockTypeId: -1,
      };

      const result = BlockValidator.validateBlock(block);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Invalid block type ID'))).toBe(true);
    });

    it('should warn about unusual Y position', () => {
      const block: Block = {
        position: { x: 0, y: 1000, z: 0 },
        blockTypeId: 1,
      };

      const result = BlockValidator.validateBlock(block);
      expect(result.warnings).toBeDefined();
      expect(result.warnings!.some((w) => w.includes('Unusual Y position'))).toBe(true);
    });

    it('should validate block with all optional fields', () => {
      const block: Block = {
        position: { x: 0, y: 0, z: 0 },
        blockTypeId: 1,
        offsets: [1, 2, 3],
        faceVisibility: { value: 63 },
        status: 1,
      };

      const result = BlockValidator.validateBlock(block);
      expect(result.valid).toBe(true);
    });

    it('should reject invalid offsets', () => {
      const block: Block = {
        position: { x: 0, y: 0, z: 0 },
        blockTypeId: 1,
        offsets: [200, 0, 0], // Out of range
      };

      const result = BlockValidator.validateBlock(block);
      expect(result.valid).toBe(false);
    });

    it('should reject invalid face visibility', () => {
      const block: Block = {
        position: { x: 0, y: 0, z: 0 },
        blockTypeId: 1,
        faceVisibility: { value: 200 }, // Out of range
      };

      const result = BlockValidator.validateBlock(block);
      expect(result.valid).toBe(false);
    });

    it('should reject invalid status', () => {
      const block: Block = {
        position: { x: 0, y: 0, z: 0 },
        blockTypeId: 1,
        status: 300, // Out of range
      };

      const result = BlockValidator.validateBlock(block);
      expect(result.valid).toBe(false);
    });
  });

  describe('validateBlockType', () => {
    it('should validate correct block type', () => {
      const blockType: BlockType = {
        id: 1,
        modifiers: {
          [BlockStatus.DEFAULT]: { visibility: { shape: 1 } },
        },
      };

      const result = BlockValidator.validateBlockType(blockType);
      expect(result.valid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should reject invalid block type ID', () => {
      const blockType: BlockType = {
        id: -1,
        modifiers: {
          0: { visibility: { shape: 1 } },
        },
      };

      const result = BlockValidator.validateBlockType(blockType);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Invalid ID'))).toBe(true);
    });

    it('should reject block type without modifiers', () => {
      const blockType: any = {
        id: 1,
      };

      const result = BlockValidator.validateBlockType(blockType);
      expect(result.valid).toBe(false);
      expect(result.errors).toContain('Modifiers are required');
    });

    it('should reject block type without default modifier', () => {
      const blockType: BlockType = {
        id: 1,
        modifiers: {
          1: { visibility: { shape: 1 } },
        },
      };

      const result = BlockValidator.validateBlockType(blockType);
      expect(result.valid).toBe(false);
      expect(result.errors).toContain('Default modifier (status 0) is required');
    });

    it('should reject invalid modifier status keys', () => {
      const blockType: any = {
        id: 1,
        modifiers: {
          0: { visibility: { shape: 1 } },
          300: { visibility: { shape: 2 } }, // Invalid status
        },
      };

      const result = BlockValidator.validateBlockType(blockType);
      expect(result.valid).toBe(false);
    });

    it('should warn about initial status without modifier', () => {
      const blockType: BlockType = {
        id: 1,
        initialStatus: 5,
        modifiers: {
          0: { visibility: { shape: 1 } },
        },
      };

      const result = BlockValidator.validateBlockType(blockType);
      expect(result.warnings).toBeDefined();
      expect(result.warnings!.some((w) => w.includes('has no corresponding modifier'))).toBe(true);
    });
  });

  describe('validateBlockMetadata', () => {
    it('should validate empty metadata', () => {
      const metadata: BlockMetadata = {};
      const result = BlockValidator.validateBlockMetadata(metadata);
      expect(result.valid).toBe(true);
    });

    it('should validate correct metadata', () => {
      const metadata: BlockMetadata = {
        name: 'TestBlock',
        displayName: 'Test Block',
        groupId: 1,
      };

      const result = BlockValidator.validateBlockMetadata(metadata);
      expect(result.valid).toBe(true);
    });

    it('should warn about very long display name', () => {
      const metadata: BlockMetadata = {
        displayName: 'A'.repeat(150),
      };

      const result = BlockValidator.validateBlockMetadata(metadata);
      expect(result.warnings).toBeDefined();
      expect(result.warnings!.some((w) => w.includes('Display name is very long'))).toBe(true);
    });

    it('should warn about very long name', () => {
      const metadata: BlockMetadata = {
        name: 'A'.repeat(70),
      };

      const result = BlockValidator.validateBlockMetadata(metadata);
      expect(result.warnings).toBeDefined();
      expect(result.warnings!.some((w) => w.includes('Name is very long'))).toBe(true);
    });

    it('should reject invalid group ID', () => {
      const metadata: BlockMetadata = {
        groupId: -1,
      };

      const result = BlockValidator.validateBlockMetadata(metadata);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Invalid group ID'))).toBe(true);
    });

    it('should reject invalid modifier status', () => {
      const metadata: any = {
        modifiers: {
          300: { visibility: { shape: 1 } },
        },
      };

      const result = BlockValidator.validateBlockMetadata(metadata);
      expect(result.valid).toBe(false);
    });
  });

  describe('validateBlockArray', () => {
    it('should validate empty array', () => {
      const result = BlockValidator.validateBlockArray([]);
      expect(result.valid).toBe(true);
    });

    it('should validate array of correct blocks', () => {
      const blocks: Block[] = [
        { position: { x: 0, y: 0, z: 0 }, blockTypeId: 1 },
        { position: { x: 1, y: 0, z: 0 }, blockTypeId: 2 },
      ];

      const result = BlockValidator.validateBlockArray(blocks);
      expect(result.valid).toBe(true);
    });

    it('should reject non-array', () => {
      const result = BlockValidator.validateBlockArray('invalid' as any);
      expect(result.valid).toBe(false);
      expect(result.errors).toContain('Blocks must be an array');
    });

    it('should reject too many blocks', () => {
      const blocks: Block[] = Array(10001)
        .fill(null)
        .map(() => ({ position: { x: 0, y: 0, z: 0 }, blockTypeId: 1 }));

      const result = BlockValidator.validateBlockArray(blocks);
      expect(result.valid).toBe(false);
      expect(result.errors.some((e) => e.includes('Too many blocks'))).toBe(true);
    });

    it('should reject array with invalid blocks', () => {
      const blocks: Block[] = [
        { position: { x: 0, y: 0, z: 0 }, blockTypeId: 1 },
        { position: { x: NaN, y: 0, z: 0 }, blockTypeId: 1 },
      ];

      const result = BlockValidator.validateBlockArray(blocks);
      expect(result.valid).toBe(false);
    });
  });

  describe('isValid', () => {
    it('should return true for valid block', () => {
      const block: Block = {
        position: { x: 0, y: 0, z: 0 },
        blockTypeId: 1,
      };

      expect(BlockValidator.isValid(block)).toBe(true);
    });

    it('should return false for invalid block', () => {
      const block: any = {
        position: { x: NaN, y: 0, z: 0 },
        blockTypeId: -1,
      };

      expect(BlockValidator.isValid(block)).toBe(false);
    });
  });

  describe('sanitize', () => {
    it('should sanitize block with invalid position', () => {
      const block: any = {
        position: { x: NaN, y: Infinity, z: undefined },
        blockTypeId: 1,
      };

      const sanitized = BlockValidator.sanitize(block);
      expect(sanitized.position).toEqual({ x: 0, y: 0, z: 0 });
    });

    it('should sanitize invalid block type ID', () => {
      const block: Block = {
        position: { x: 0, y: 0, z: 0 },
        blockTypeId: -1,
      };

      const sanitized = BlockValidator.sanitize(block);
      expect(sanitized.blockTypeId).toBe(0);
    });

    it('should keep valid optional fields', () => {
      const block: Block = {
        position: { x: 0, y: 0, z: 0 },
        blockTypeId: 1,
        status: 1,
        faceVisibility: { value: 63 },
      };

      const sanitized = BlockValidator.sanitize(block);
      expect(sanitized.status).toBe(1);
      expect(sanitized.faceVisibility).toEqual({ value: 63 });
    });

    it('should remove invalid optional fields', () => {
      const block: Block = {
        position: { x: 0, y: 0, z: 0 },
        blockTypeId: 1,
        offsets: [200, 0, 0], // Invalid
        faceVisibility: { value: 200 }, // Invalid
      };

      const sanitized = BlockValidator.sanitize(block);
      expect(sanitized.offsets).toBeUndefined();
      expect(sanitized.faceVisibility).toBeUndefined();
    });
  });
});
