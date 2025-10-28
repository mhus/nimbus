/**
 * Tests for BlockSerializer and BlockTypeSerializer
 */

import { BlockSerializer, BlockTypeSerializer } from './BlockSerializer';
import type { Block } from '../types/Block';
import type { BlockType } from '../types/BlockType';

describe('BlockSerializer', () => {
  describe('toJSON', () => {
    it('should serialize block to JSON string', () => {
      const block: Block = {
        position: { x: 1, y: 2, z: 3 },
        blockTypeId: 1,
      };

      const json = BlockSerializer.toJSON(block);
      expect(json).toBeTruthy();
      expect(typeof json).toBe('string');

      const parsed = JSON.parse(json);
      expect(parsed.position).toEqual({ x: 1, y: 2, z: 3 });
      expect(parsed.blockTypeId).toBe(1);
    });

    it('should serialize block with all optional fields', () => {
      const block: Block = {
        position: { x: 1, y: 2, z: 3 },
        blockTypeId: 2,
        offsets: [0, 1, 2],
        faceVisibility: { value: 63 },
        status: 1,
      };

      const json = BlockSerializer.toJSON(block);
      const parsed = JSON.parse(json);

      expect(parsed.offsets).toEqual([0, 1, 2]);
      expect(parsed.faceVisibility).toEqual({ value: 63 });
      expect(parsed.status).toBe(1);
    });
  });

  describe('fromJSON', () => {
    it('should deserialize valid block JSON', () => {
      const json = '{"position":{"x":1,"y":2,"z":3},"blockTypeId":1}';
      const block = BlockSerializer.fromJSON(json);

      expect(block).not.toBeNull();
      expect(block?.position).toEqual({ x: 1, y: 2, z: 3 });
      expect(block?.blockTypeId).toBe(1);
    });

    it('should return null for invalid JSON', () => {
      const block = BlockSerializer.fromJSON('invalid json');
      expect(block).toBeNull();
    });

    it('should return null for empty string', () => {
      const block = BlockSerializer.fromJSON('');
      expect(block).toBeNull();
    });

    it('should deserialize block with optional fields', () => {
      const json =
        '{"position":{"x":1,"y":2,"z":3},"blockTypeId":2,"offsets":[0,1,2],"status":1}';
      const block = BlockSerializer.fromJSON(json);

      expect(block).not.toBeNull();
      expect(block?.offsets).toEqual([0, 1, 2]);
      expect(block?.status).toBe(1);
    });
  });

  describe('fromObject', () => {
    it('should convert valid object to Block', () => {
      const obj = {
        position: { x: 1, y: 2, z: 3 },
        blockTypeId: 1,
      };

      const block = BlockSerializer.fromObject(obj);

      expect(block).not.toBeNull();
      expect(block?.position).toEqual({ x: 1, y: 2, z: 3 });
      expect(block?.blockTypeId).toBe(1);
    });

    it('should return null for null input', () => {
      const block = BlockSerializer.fromObject(null);
      expect(block).toBeNull();
    });

    it('should return null for non-object input', () => {
      expect(BlockSerializer.fromObject('string')).toBeNull();
      expect(BlockSerializer.fromObject(123)).toBeNull();
      expect(BlockSerializer.fromObject(true)).toBeNull();
    });

    it('should return null for object without position', () => {
      const obj = { blockTypeId: 1 };
      const block = BlockSerializer.fromObject(obj);
      expect(block).toBeNull();
    });

    it('should handle missing position coordinates with defaults', () => {
      const obj = {
        position: { x: 1 },
        blockTypeId: 1,
      };

      const block = BlockSerializer.fromObject(obj);

      expect(block).not.toBeNull();
      expect(block?.position).toEqual({ x: 1, y: 0, z: 0 });
    });

    it('should convert optional fields', () => {
      const obj = {
        position: { x: 1, y: 2, z: 3 },
        blockTypeId: 2,
        offsets: [0, 1, 2],
        faceVisibility: 63,
        status: 1,
      };

      const block = BlockSerializer.fromObject(obj);

      expect(block).not.toBeNull();
      expect(block?.offsets).toEqual([0, 1, 2]);
      expect(block?.faceVisibility).toEqual({ value: 63 });
      expect(block?.status).toBe(1);
    });

    it('should handle faceVisibility as object', () => {
      const obj = {
        position: { x: 1, y: 2, z: 3 },
        blockTypeId: 1,
        faceVisibility: { value: 63 },
      };

      const block = BlockSerializer.fromObject(obj);

      expect(block).not.toBeNull();
      expect(block?.faceVisibility).toEqual({ value: 63 });
    });
  });

  describe('toObject', () => {
    it('should convert Block to plain object', () => {
      const block: Block = {
        position: { x: 1, y: 2, z: 3 },
        blockTypeId: 1,
      };

      const obj = BlockSerializer.toObject(block);

      expect(obj.position).toEqual({ x: 1, y: 2, z: 3 });
      expect(obj.blockTypeId).toBe(1);
    });

    it('should include optional fields if present', () => {
      const block: Block = {
        position: { x: 1, y: 2, z: 3 },
        blockTypeId: 2,
        offsets: [0, 1, 2],
        faceVisibility: { value: 63 },
        status: 1,
      };

      const obj = BlockSerializer.toObject(block);

      expect(obj.offsets).toEqual([0, 1, 2]);
      expect(obj.faceVisibility).toBe(63);
      expect(obj.status).toBe(1);
    });

    it('should not include empty offsets array', () => {
      const block: Block = {
        position: { x: 1, y: 2, z: 3 },
        blockTypeId: 1,
        offsets: [],
      };

      const obj = BlockSerializer.toObject(block);

      expect(obj.offsets).toBeUndefined();
    });

    it('should not include undefined optional fields', () => {
      const block: Block = {
        position: { x: 1, y: 2, z: 3 },
        blockTypeId: 1,
      };

      const obj = BlockSerializer.toObject(block);

      expect(obj.offsets).toBeUndefined();
      expect(obj.faceVisibility).toBeUndefined();
      expect(obj.status).toBeUndefined();
    });
  });

  describe('arrayToJSON', () => {
    it('should serialize empty array', () => {
      const json = BlockSerializer.arrayToJSON([]);
      expect(json).toBe('[]');
    });

    it('should serialize array of blocks', () => {
      const blocks: Block[] = [
        { position: { x: 1, y: 2, z: 3 }, blockTypeId: 1 },
        { position: { x: 4, y: 5, z: 6 }, blockTypeId: 2 },
      ];

      const json = BlockSerializer.arrayToJSON(blocks);
      const parsed = JSON.parse(json);

      expect(Array.isArray(parsed)).toBe(true);
      expect(parsed).toHaveLength(2);
      expect(parsed[0].blockTypeId).toBe(1);
      expect(parsed[1].blockTypeId).toBe(2);
    });

    it('should serialize blocks with optional fields', () => {
      const blocks: Block[] = [
        {
          position: { x: 1, y: 2, z: 3 },
          blockTypeId: 2,
          status: 1,
        },
      ];

      const json = BlockSerializer.arrayToJSON(blocks);
      const parsed = JSON.parse(json);

      expect(parsed[0].status).toBe(1);
    });
  });

  describe('arrayFromJSON', () => {
    it('should deserialize empty array', () => {
      const blocks = BlockSerializer.arrayFromJSON('[]');
      expect(blocks).toEqual([]);
    });

    it('should deserialize array of blocks', () => {
      const json =
        '[{"position":{"x":1,"y":2,"z":3},"blockTypeId":1},{"position":{"x":4,"y":5,"z":6},"blockTypeId":2}]';
      const blocks = BlockSerializer.arrayFromJSON(json);

      expect(blocks).not.toBeNull();
      expect(blocks).toHaveLength(2);
      expect(blocks![0].blockTypeId).toBe(1);
      expect(blocks![1].blockTypeId).toBe(2);
    });

    it('should return null for invalid JSON', () => {
      const blocks = BlockSerializer.arrayFromJSON('invalid');
      expect(blocks).toBeNull();
    });

    it('should return null for non-array JSON', () => {
      const blocks = BlockSerializer.arrayFromJSON('{"foo":"bar"}');
      expect(blocks).toBeNull();
    });

    it('should filter out invalid blocks', () => {
      const json =
        '[{"position":{"x":1,"y":2,"z":3},"blockTypeId":1},{"invalid":"data"}]';
      const blocks = BlockSerializer.arrayFromJSON(json);

      expect(blocks).not.toBeNull();
      expect(blocks).toHaveLength(1);
      expect(blocks![0].blockTypeId).toBe(1);
    });
  });

  describe('round-trip serialization', () => {
    it('should preserve block data through serialization cycle', () => {
      const original: Block = {
        position: { x: 1, y: 2, z: 3 },
        blockTypeId: 2,
        offsets: [0, 1, 2],
        status: 1,
      };

      const json = BlockSerializer.toJSON(original);
      const deserialized = BlockSerializer.fromJSON(json);

      expect(deserialized).not.toBeNull();
      expect(deserialized?.position).toEqual(original.position);
      expect(deserialized?.blockTypeId).toBe(original.blockTypeId);
      expect(deserialized?.offsets).toEqual(original.offsets);
      expect(deserialized?.status).toBe(original.status);
    });

    it('should preserve array data through serialization cycle', () => {
      const original: Block[] = [
        { position: { x: 1, y: 2, z: 3 }, blockTypeId: 1 },
        { position: { x: 4, y: 5, z: 6 }, blockTypeId: 2, status: 1 },
      ];

      const json = BlockSerializer.arrayToJSON(original);
      const deserialized = BlockSerializer.arrayFromJSON(json);

      expect(deserialized).not.toBeNull();
      expect(deserialized).toHaveLength(2);
      expect(deserialized![0].blockTypeId).toBe(1);
      expect(deserialized![1].blockTypeId).toBe(2);
      expect(deserialized![1].status).toBe(1);
    });
  });
});

describe('BlockTypeSerializer', () => {
  describe('toJSON', () => {
    it('should serialize BlockType to JSON', () => {
      const blockType: BlockType = {
        id: 1,
        modifiers: {
          0: {
            visibility: { shape: 1 },
          },
        },
      };

      const json = BlockTypeSerializer.toJSON(blockType);
      expect(json).toBeTruthy();
      expect(typeof json).toBe('string');

      const parsed = JSON.parse(json);
      expect(parsed.id).toBe(1);
    });
  });

  describe('fromJSON', () => {
    it('should deserialize valid BlockType JSON', () => {
      const json = '{"id":1,"modifiers":{"0":{"visibility":{"shape":1}}}}';
      const blockType = BlockTypeSerializer.fromJSON(json);

      expect(blockType).not.toBeNull();
      expect(blockType?.id).toBe(1);
      expect(blockType?.modifiers).toBeDefined();
    });

    it('should return null for invalid JSON', () => {
      const blockType = BlockTypeSerializer.fromJSON('invalid');
      expect(blockType).toBeNull();
    });

    it('should return null for non-object JSON', () => {
      const blockType = BlockTypeSerializer.fromJSON('[]');
      expect(blockType).toBeNull();
    });

    it('should return null for object without id', () => {
      const json = '{"modifiers":{}}';
      const blockType = BlockTypeSerializer.fromJSON(json);
      expect(blockType).toBeNull();
    });

    it('should return null for object without modifiers', () => {
      const json = '{"id":1}';
      const blockType = BlockTypeSerializer.fromJSON(json);
      expect(blockType).toBeNull();
    });
  });

  describe('arrayToJSON', () => {
    it('should serialize empty array', () => {
      const json = BlockTypeSerializer.arrayToJSON([]);
      expect(json).toBe('[]');
    });

    it('should serialize array of BlockTypes', () => {
      const blockTypes: BlockType[] = [
        { id: 1, modifiers: { 0: { visibility: { shape: 1 } } } },
        { id: 2, modifiers: { 0: { visibility: { shape: 2 } } } },
      ];

      const json = BlockTypeSerializer.arrayToJSON(blockTypes);
      const parsed = JSON.parse(json);

      expect(Array.isArray(parsed)).toBe(true);
      expect(parsed).toHaveLength(2);
      expect(parsed[0].id).toBe(1);
      expect(parsed[1].id).toBe(2);
    });
  });

  describe('arrayFromJSON', () => {
    it('should deserialize empty array', () => {
      const blockTypes = BlockTypeSerializer.arrayFromJSON('[]');
      expect(blockTypes).toEqual([]);
    });

    it('should deserialize array of BlockTypes', () => {
      const json =
        '[{"id":1,"modifiers":{"0":{"visibility":{"shape":1}}}},{"id":2,"modifiers":{"0":{"visibility":{"shape":2}}}}]';
      const blockTypes = BlockTypeSerializer.arrayFromJSON(json);

      expect(blockTypes).not.toBeNull();
      expect(blockTypes).toHaveLength(2);
      expect(blockTypes![0].id).toBe(1);
      expect(blockTypes![1].id).toBe(2);
    });

    it('should return null for invalid JSON', () => {
      const blockTypes = BlockTypeSerializer.arrayFromJSON('invalid');
      expect(blockTypes).toBeNull();
    });

    it('should return null for non-array JSON', () => {
      const blockTypes = BlockTypeSerializer.arrayFromJSON('{"foo":"bar"}');
      expect(blockTypes).toBeNull();
    });
  });

  describe('round-trip serialization', () => {
    it('should preserve BlockType data through serialization cycle', () => {
      const original: BlockType = {
        id: 2,
        modifiers: {
          0: { visibility: { shape: 1 } },
          1: { visibility: { shape: 2 } },
        },
      };

      const json = BlockTypeSerializer.toJSON(original);
      const deserialized = BlockTypeSerializer.fromJSON(json);

      expect(deserialized).not.toBeNull();
      expect(deserialized?.id).toBe(original.id);
      expect(deserialized?.modifiers).toEqual(original.modifiers);
    });

    it('should preserve array data through serialization cycle', () => {
      const original: BlockType[] = [
        { id: 1, modifiers: { 0: { visibility: { shape: 1 } } } },
        { id: 2, modifiers: { 0: { visibility: { shape: 2 } } } },
      ];

      const json = BlockTypeSerializer.arrayToJSON(original);
      const deserialized = BlockTypeSerializer.arrayFromJSON(json);

      expect(deserialized).not.toBeNull();
      expect(deserialized).toHaveLength(2);
      expect(deserialized![0].id).toBe(1);
      expect(deserialized![1].id).toBe(2);
    });
  });
});
