/**
 * ShortcutDefinition - Defines an action bound to a shortcut key or click
 *
 * Shortcuts can be bound to:
 * - Number keys: key0...key9 (0 = key '0', 1-9 = keys '1'-'9')
 * - Mouse clicks: click0, click1, click2 (0 = left, 1 = middle, 2 = right)
 * - Inventory slots: slot0...slotN
 */

/**
 * Shortcut action type
 */
export type ShortcutActionType = 'block' | 'attack' | 'use' | 'none';

/**
 * Shortcut key type
 * - key0...key9: Number keys (key '0' through '9')
 * - click0, click1, click2: Mouse buttons (left, middle, right)
 * - slot0...slotN: Inventory slots
 */
export type ShortcutKey = string; // Format: 'key0'-'key9', 'click0'-'click2', 'slot0'-'slotN'

/**
 * Shortcut definition
 */
export interface ShortcutDefinition {
  /**
   * Action type
   * - 'block': Place/use a block
   * - 'attack': Attack action
   * - 'use': Use item action
   * - 'none': No action (default)
   */
  type: ShortcutActionType;

  /**
   * Item ID to use (for block, attack, or use actions)
   * Optional - if not specified, uses currently selected item
   */
  itemId?: string;

  /**
   * Pose to activate during action (optional)
   * Example: 'attack', 'use', 'place', etc.
   */
  pose?: string;

  /**
   * Wait time before activation in milliseconds
   * Default: 0
   */
  wait?: number;

  /**
   * Duration of action in milliseconds (optional)
   * During this time, no other actions can be performed
   */
  duration?: number;
}

/**
 * Default shortcut (no action)
 */
export const DEFAULT_SHORTCUT: ShortcutDefinition = {
  type: 'none',
};

/**
 * Shortcuts map type
 */
export type ShortcutsMap = Map<ShortcutKey, ShortcutDefinition>;
