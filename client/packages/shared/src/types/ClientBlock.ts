/**
 * ClientBlock - Client-side block instance
 *
 * Contains the block instance with resolved client-side types and caches.
 * This type is NOT transmitted over network - it's only used client-side.
 */

import type { Block } from './Block';
import type { BlockType } from './BlockType';
import type { BlockMetadata } from './BlockMetadata';
import type { BlockModifier } from './BlockModifier';
import type { ClientBlockType } from './ClientBlockType';

/**
 * Client-side block instance with caches and resolved references
 */
export interface ClientBlock {
  /** Original block instance (network data) */
  block: Block;

  /** Chunk coordinates */
  chunk: { cx: number; cz: number };

  // Cached references (resolved from IDs)

  /**
   * Cached BlockType reference
   * Resolved from block.blockTypeId via registry
   */
  blockType: BlockType;

  /**
   * Cached BlockMetadata reference
   * Resolved from block.metadata
   */
  metadata?: BlockMetadata;

  /**
   * Cached current BlockModifier
   * Resolved from blockType.modifiers[block.status] or metadata.modifiers[block.status]
   */
  currentModifier: BlockModifier;

  /**
   * Customized/optimized BlockType for rendering
   * Pre-processed for fast rendering access
   */
  clientBlockType: ClientBlockType;

  /**
   * Current status (string representation for debugging)
   * @example "OPEN", "CLOSED", "WINTER"
   */
  statusName?: string;

  // Additional client-side caches

  /**
   * Is block currently visible (culling, distance, etc.)
   */
  isVisible?: boolean;

  /**
   * Last update timestamp (for change detection)
   */
  lastUpdate?: number;

  /**
   * Dirty flag (needs re-render)
   */
  isDirty?: boolean;
}
