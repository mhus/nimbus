/**
 * Block serialization utilities
 * Convert between Block and JSON representations
 */

import type { Block } from '../types/Block';
import type { BlockType } from '../types/BlockType';

/**
 * Block serialization helpers
 */
export namespace BlockSerializer {
  /**
   * Serialize block to JSON string
   * @param block Block to serialize
   * @returns JSON string
   */
  export function toJSON(block: Block): string {
    return JSON.stringify(block);
  }

  /**
   * Deserialize block from JSON string
   * @param json JSON string
   * @returns Block instance or null if invalid
   */
  export function fromJSON(json: string): Block | null {
    try {
      const data = JSON.parse(json);
      return fromObject(data);
    } catch (e) {
      console.error('Failed to parse block JSON:', e);
      return null;
    }
  }

  /**
   * Convert plain object to Block
   * @param obj Plain object
   * @returns Block instance or null if invalid
   */
  export function fromObject(obj: any): Block | null {
    if (!obj || typeof obj !== 'object') {
      return null;
    }

    if (!obj.position || !obj.blockTypeId === undefined) {
      return null;
    }

    const block: Block = {
      position: {
        x: obj.position.x ?? 0,
        y: obj.position.y ?? 0,
        z: obj.position.z ?? 0,
      },
      blockTypeId: obj.blockTypeId,
    };

    // Optional fields
    if (obj.offsets) {
      block.offsets = Array.isArray(obj.offsets) ? obj.offsets : undefined;
    }

    if (obj.faceVisibility) {
      block.faceVisibility =
        typeof obj.faceVisibility === 'object'
          ? obj.faceVisibility
          : { value: obj.faceVisibility };
    }

    if (obj.status !== undefined) {
      block.status = obj.status;
    }

    if (obj.metadata) {
      block.metadata = obj.metadata;
    }

    return block;
  }

  /**
   * Convert Block to plain object (for JSON serialization)
   * Removes undefined fields for smaller JSON
   * @param block Block to convert
   * @returns Plain object
   */
  export function toObject(block: Block): any {
    const obj: any = {
      position: block.position,
      blockTypeId: block.blockTypeId,
    };

    // Only include optional fields if present
    if (block.offsets && block.offsets.length > 0) {
      obj.offsets = block.offsets;
    }

    if (block.faceVisibility) {
      obj.faceVisibility = block.faceVisibility.value;
    }

    if (block.status !== undefined) {
      obj.status = block.status;
    }

    if (block.metadata) {
      obj.metadata = block.metadata;
    }

    return obj;
  }

  /**
   * Serialize block array to JSON
   * @param blocks Array of blocks
   * @returns JSON string
   */
  export function arrayToJSON(blocks: Block[]): string {
    const objects = blocks.map(toObject);
    return JSON.stringify(objects);
  }

  /**
   * Deserialize block array from JSON
   * @param json JSON string
   * @returns Array of blocks or null if invalid
   */
  export function arrayFromJSON(json: string): Block[] | null {
    try {
      const data = JSON.parse(json);
      if (!Array.isArray(data)) {
        return null;
      }

      const blocks = data.map(fromObject).filter((b): b is Block => b !== null);
      return blocks;
    } catch (e) {
      console.error('Failed to parse block array JSON:', e);
      return null;
    }
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
   * @returns BlockType or null if invalid
   */
  export function fromJSON(json: string): BlockType | null {
    try {
      const data = JSON.parse(json);

      if (!data || typeof data !== 'object') {
        return null;
      }

      if (data.id === undefined || !data.modifiers) {
        return null;
      }

      return data as BlockType;
    } catch (e) {
      console.error('Failed to parse BlockType JSON:', e);
      return null;
    }
  }

  /**
   * Serialize BlockType array (for registry sync)
   * @param blockTypes Array of block types
   * @returns JSON string
   */
  export function arrayToJSON(blockTypes: BlockType[]): string {
    return JSON.stringify(blockTypes);
  }

  /**
   * Deserialize BlockType array
   * @param json JSON string
   * @returns Array of block types or null
   */
  export function arrayFromJSON(json: string): BlockType[] | null {
    try {
      const data = JSON.parse(json);
      if (!Array.isArray(data)) {
        return null;
      }

      return data as BlockType[];
    } catch (e) {
      console.error('Failed to parse BlockType array JSON:', e);
      return null;
    }
  }
}
