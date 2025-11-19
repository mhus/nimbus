/**
 * ItemData - Item instance with ItemType reference
 *
 * Items reference an ItemType for default properties and can override them.
 * The ItemType provides texture, scaling, pose, and onUseEffect defaults.
 * Individual items can customize these via modifierOverrides.
 *
 * **Server-side**: Full structure with parameters
 * **Client receives**: Block with merged itemModifier
 */

import type { Block } from './Block';
import type { ItemModifier } from './ItemModifier';

/**
 * Item data structure
 *
 * References an ItemType and optionally overrides its properties.
 */
export interface ItemData {
  /**
   * Item type identifier (e.g., 'sword', 'wand', 'potion')
   * References an ItemType definition loaded from files/itemtypes/{type}.json
   */
  itemType: string;

  /**
   * Block definition
   *
   * Contains position and metadata.
   * The itemModifier will be populated by merging ItemType.modifier
   * with modifierOverrides.
   */
  block: Block;

  /**
   * Optional description override
   *
   * Overrides the ItemType description for this specific item instance.
   */
  description?: string;

  /**
   * Optional modifier overrides
   *
   * Allows individual items to override ItemType.modifier properties.
   * Merged with ItemType.modifier to create final block.itemModifier.
   *
   * Example:
   * ```json
   * {
   *   "modifierOverrides": {
   *     "texture": "items/enchanted_sword.png",
   *     "color": "#ff00ff",
   *     "scaleX": 0.7
   *   }
   * }
   * ```
   */
  modifierOverrides?: Partial<ItemModifier>;

  /**
   * Optional parameters
   *
   * Custom key-value pairs for item-specific data (server-side only).
   * Examples: durability, enchantments, customData
   */
  parameters?: Record<string, any>;
}
