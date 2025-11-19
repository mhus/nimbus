/**
 * Item Utilities - Helper functions for Item-to-Block conversion
 *
 * Items are stored as Item objects but transmitted and rendered as Blocks.
 * This module provides conversion utilities used by both client and server.
 */

import type { Item } from '../types/Item';
import type { Block } from '../types/Block';

/**
 * Converts an Item to a Block for network transmission or rendering
 *
 * Items are stored with position, id, name, etc. directly.
 * For network transmission and rendering, they need to be converted to Blocks
 * with BlockType 1 (ITEM type) and metadata containing the item information.
 *
 * @param item Item to convert
 * @returns Block representation of the item
 */
export function itemToBlock(item: Item): Block {
  return {
    position: item.position,
    blockTypeId: 1, // ITEM blockType (fixed)
    metadata: {
      id: item.id,
      displayName: item.name,
    },
  };
}
