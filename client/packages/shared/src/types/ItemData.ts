/**
 * ItemData - Item definition with block and parameters
 *
 * Items are managed server-side with this structure.
 * The Block contains the visual/physical representation,
 * while parameters store additional item-specific data.
 *
 * **Server-side only**: This structure is used internally by the server.
 * **Client receives**: Only the `block` field via REST API.
 */

import type { Block } from './Block';

/**
 * Item data structure
 *
 * Combines a Block definition (visual/physical properties)
 * with optional parameters (custom item data).
 */
export interface ItemData {
  /**
   * Block definition
   *
   * Contains the visual and physical properties of the item.
   * This is what clients receive when requesting item data.
   */
  block: Block;

  /**
   * Optional parameters
   *
   * Map of custom key-value pairs for item-specific data.
   * Examples:
   * - durability: number
   * - enchantments: string[]
   * - customData: any
   *
   * These parameters are server-side only and not transmitted to clients
   * unless explicitly requested.
   */
  parameters?: Record<string, any>;
}
