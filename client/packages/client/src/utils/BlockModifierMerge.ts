/**
 * BlockModifierMerge - Merge block modifiers according to priority rules
 *
 * Merge priority (first match wins):
 * 1. Block.modifiers[status] (Instance-specific modifier)
 * 2. BlockType.modifiers[status] (Type-defined modifier for status)
 * 3. BlockType.modifiers[0] (Default status fallback)
 * 4. Default values (e.g., shape = 0)
 */

import { BlockModifier, BlockType, Block } from '@nimbus/shared';
import type { AppContext } from '../AppContext.js';

/**
 * Merge BlockModifier according to priority rules
 *
 * @param block Block instance
 * @param blockType BlockType definition
 * @returns Merged BlockModifier
 */
export function mergeBlockModifier(
  appContext: AppContext,
  block: Block,
  blockType: BlockType,
  overwriteStatus?: number
): BlockModifier {
  // Status is determined from BlockType.initialStatus (default: 0)
  const status = mergeStatus(appContext, block, blockType, overwriteStatus);

  // Priority 1: Block instance modifiers (if block has custom modifiers)
  if (block.modifiers && block.modifiers[status]) {
    return block.modifiers[status];
  }

  // Priority 2: BlockType base modifiers for this status
  if (blockType.modifiers[status]) {
    return blockType.modifiers[status];
  }

  // Priority 3: BlockType default status (0) as fallback
  if (blockType.modifiers[0]) {
    return blockType.modifiers[0];
  }

  // Priority 4: Return empty modifier with defaults
  return {
    visibility: {
      shape: 0, // Default shape
      textures: {},
    },
  };
}

export function mergeStatus(
  appContext: AppContext,
  block: Block,
  blockType: BlockType,
  overwriteStatus?: number
): number {

    // TODO worldStatus
    // seasonalStatus if seasonal status is defined in block modifier

  // Use overwriteStatus if provided
  if (overwriteStatus !== undefined) {
    return overwriteStatus;
  }

  // Use block's own status if defined
  if (block.status !== undefined) {
    return block.status;
  }

  // Fallback to BlockType's initialStatus or default to 0
  return blockType.initialStatus ?? 0;
}

/**
 * Get block position key for Map lookup
 *
 * @param x World X coordinate
 * @param y World Y coordinate
 * @param z World Z coordinate
 * @returns Position key string
 */
export function getBlockPositionKey(x: number, y: number, z: number): string {
  return `${x},${y},${z}`;
}
