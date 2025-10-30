/**
 * BlockModifierMerge - Merge block modifiers according to priority rules
 *
 * Merge priority (first match wins):
 * 1. Block-BlockType-status-Metadata (Instance status)
 * 2. Block-BlockType-ID status-Metadata (Instance status = world status)
 * 3. Block-BlockType-ID status-Metadata (Base status)
 * 4. Block-BlockType-ID status-Metadata (Base status = world status)
 * 5. Default values for metadata (e.g., shape = 0)
 */

import { BlockModifier, BlockType, Block } from '@nimbus/shared';

/**
 * Merge BlockModifier according to priority rules
 *
 * @param block Block instance
 * @param blockType BlockType definition
 * @returns Merged BlockModifier
 */
export function mergeBlockModifier(
  block: Block,
  blockType: BlockType
): BlockModifier {
  // Status is determined from BlockType.initialStatus (default: 0)
  const status = blockType.initialStatus || 0;

  // Priority 1: Block instance metadata modifiers (if block has custom metadata)
  if (block.metadata?.modifiers && block.metadata.modifiers[status]) {
    return block.metadata.modifiers[status];
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
