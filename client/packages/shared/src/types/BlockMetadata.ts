/**
 * BlockMetadata - Additional metadata for block instances
 *
 * Metadata merging priority (first match wins):
 * 1. Block-BlockType-status-Metadata (instance status)
 * 2. Block-BlockType-ID status-Metadata (instance status = world status)
 * 3. Block-BlockType-ID status-Metadata (base status)
 * 4. Block-BlockType-ID status-Metadata (base status = world status)
 * 5. Default values (e.g., shape = 0)
 */

import type { BlockModifier } from './BlockModifier';

export interface BlockMetadata {
  /**
   * Display name for UI
   */
  displayName?: string;

  /**
   * Internal name
   */
  name?: string;

  /**
   * Group ID for organization/categorization
   */
  groupId?: number;

  /**
   * Instance-specific modifiers map: status → BlockModifier
   *
   * Optional block instance overrides for specific status values.
   * These override the BlockType modifiers for this specific block instance.
   *
   * Use case: A specific door that looks different than the standard door type.
   *
   * @example
   * modifiers: {
   *   1: { visibility: { shape: Shape.CUBE, textures: {...} } }  // custom open state
   * }
   */
  modifiers?: Record<number, BlockModifier>;
}
