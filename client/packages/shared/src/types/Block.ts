/**
 * Block - A concrete block instance in the world
 *
 * Blocks have minimal parameters. Standard situations are defined by BlockTypes.
 * Blocks can have additional metadata that is always block-specific.
 */

import type { Vector3 } from './Vector3';

/**
 * Edge offsets for block corners (8 corners, each with XYZ offset)
 * Values range from -127 to 127 (1 byte per axis)
 * Total: 24 bytes (8 corners × 3 axes)
 */
export type EdgeOffsets = number[]; // Array of 24 values

/**
 * Face visibility flags
 * 6 bits for faces (top, bottom, left, right, front, back)
 * 1 bit for fixed/auto mode
 * Total: 1 byte
 */
export interface FaceVisibility {
  /** Bitfield: bit 0-5 = faces, bit 6 = fixed/auto */
  value: number;
}

/**
 * Block instance in the world
 */
export interface Block {
  /**
   * Position in world coordinates
   */
  position: Vector3;

  /**
   * Reference to BlockType by ID
   */
  blockTypeId: number;

  /**
   * Edge offsets for corners (optional, 24 bytes)
   * Array of 24 values: [corner0.x, corner0.y, corner0.z, corner1.x, ...]
   */
  offsets?: EdgeOffsets;

  /**
   * Face visibility flags (1 byte)
   * Determines which faces are visible or if it's auto-calculated
   */
  faceVisibility?: FaceVisibility;

  /**
   * Cached BlockType reference (client-side only, not transmitted)
   */
  blockType?: any; // Will be resolved to BlockType

  /**
   * Block-specific metadata (optional)
   */
  metadata?: any; // Will be resolved to BlockMetadata
}
