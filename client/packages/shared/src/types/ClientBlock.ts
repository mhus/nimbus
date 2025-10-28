/**
 * ClientBlock - Client-side block instance
 *
 * Contains the block instance with resolved client-side types
 */

import type { Block } from './Block';
import type { BlockType } from './BlockType';
import type { ClientBlockType } from './ClientBlockType';

/**
 * Client-side block instance
 */
export interface ClientBlock {
  /** Original block instance */
  block: Block;

  /** Chunk XZ coordinates */
  chunkXZ: { x: number; z: number };

  /** Original BlockType */
  originalBlockType: BlockType;

  /** Customized/optimized BlockType for rendering */
  customizedBlockType: ClientBlockType;

  /** Current status (string representation for debugging) */
  status: string;
}
