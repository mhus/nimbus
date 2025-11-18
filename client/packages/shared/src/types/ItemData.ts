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
import type { ScriptActionDefinition } from '../scrawl/ScriptActionDefinition';

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
   * Item description
   *
   * Optional text description for the item.
   * Can be used for tooltips, item info displays, etc.
   */
  description?: string;

  /**
   * Pose to activate when item is used
   *
   * Example: 'attack', 'use', 'place', 'drink', etc.
   * Used for player animation when using this item.
   */
  pose?: string;

  /**
   * Wait time before activation in milliseconds
   *
   * Delay before the item action takes effect.
   * Default: 0 (immediate)
   */
  wait?: number;

  /**
   * Duration of action in milliseconds
   *
   * During this time, the action/pose is active.
   * After duration, player returns to normal state.
   */
  duration?: number;

  /**
   * Scrawl script to execute when item is used
   *
   * This allows items to trigger complex effect sequences, animations,
   * and commands through the Scrawl framework.
   *
   * Example:
   * ```typescript
   * {
   *   scriptId: "explosion_effect",
   *   parameters: { radius: 5, damage: 50 }
   * }
   * ```
   */
  onUseEffect?: ScriptActionDefinition;

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
