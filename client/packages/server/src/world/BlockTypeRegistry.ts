/**
 * BlockType Registry
 */

import { getLogger, BlockConstants, Shape } from '@nimbus/shared';
import type { BlockType } from '@nimbus/shared';
import { BlockStatus } from '@nimbus/shared';

const logger = getLogger('BlockTypeRegistry');

export class BlockTypeRegistry {
  private blockTypes = new Map<number, BlockType>();

  constructor() {
    this.initializeDefaultBlocks();
  }

  private initializeDefaultBlocks(): void {
    // AIR (ID: 0) - Required
    this.registerBlockType({
      id: BlockConstants.AIR_BLOCK_ID,
      modifiers: {
        [BlockStatus.DEFAULT]: {
          visibility: {
            shape: Shape.INVISIBLE,
          },
        },
      },
    });

    // Stone (ID: 1)
    this.registerBlockType({
      id: 1,
      modifiers: {
        [BlockStatus.DEFAULT]: {
          visibility: {
            shape: Shape.CUBE,
          },
        },
      },
    });

    // Grass (ID: 2)
    this.registerBlockType({
      id: 2,
      modifiers: {
        [BlockStatus.DEFAULT]: {
          visibility: {
            shape: Shape.CUBE,
          },
        },
      },
    });

    logger.info(`Initialized ${this.blockTypes.size} block types`);
  }

  registerBlockType(blockType: BlockType): void {
    this.blockTypes.set(blockType.id, blockType);
  }

  getBlockType(id: number): BlockType | undefined {
    return this.blockTypes.get(id);
  }

  getAllBlockTypes(): BlockType[] {
    return Array.from(this.blockTypes.values());
  }

  getBlockTypeRange(from: number, to: number): BlockType[] {
    const result: BlockType[] = [];
    for (let id = from; id <= to; id++) {
      const blockType = this.blockTypes.get(id);
      if (blockType) result.push(blockType);
    }
    return result;
  }
}
