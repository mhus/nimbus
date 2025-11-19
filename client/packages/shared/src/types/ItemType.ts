/**
 * ItemType - Template definition for items
 *
 * Provides default properties for item categories (sword, wand, potion, etc.)
 * Individual items can override these defaults via ItemData.modifierOverrides.
 *
 * Similar to BlockType but for items. Stored in server files/itemtypes/*.json
 */

import type { ItemModifier } from './ItemModifier';

export interface ItemType {
  /**
   * Unique item type identifier (e.g., 'sword', 'wand', 'potion')
   * Used to reference this ItemType from ItemData
   */
  type: string;

  /**
   * Display name for this item type
   * Example: "Sword", "Magic Wand", "Health Potion"
   */
  name: string;

  /**
   * Optional description
   * Provides context about this item type
   */
  description?: string;

  /**
   * BlockTypeId for rendering
   * Usually 1 (ITEM blockType)
   */
  blockTypeId: number;

  /**
   * Default item modifier
   * Contains texture, scaling, pose, onUseEffect, etc.
   * Individual items can override these via ItemData.modifierOverrides
   */
  modifier: ItemModifier;

  /**
   * Optional metadata
   * Can include category, rarity, exclusive flag, etc.
   *
   * Example:
   * ```json
   * {
   *   "category": "magic",
   *   "rarity": "rare",
   *   "exclusive": true
   * }
   * ```
   */
  metadata?: Record<string, any>;
}
