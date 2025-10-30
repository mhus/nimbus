/**
 * Block - A concrete block instance in the world
 *
 * Blocks have minimal parameters. Standard situations are defined by BlockTypes.
 * Blocks can have additional metadata that is always block-specific.
 *
 * **Network transfer:** This type is transmitted over network and used on server.
 * **Client-side:** See ClientBlock for client-side representation with caches.
 */

import type { Vector3 } from './Vector3';
import type { BlockMetadata } from './BlockMetadata';

/**
 * Offsets for block geometry modification
 *
 * Flexible array that can contain different types of offsets depending on block shape.
 * Values are stored as floats in JSON for precise geometry manipulation.
 *
 * **For Cubes (Edge/Corner offsets):**
 * - 8 corners × 3 axes = up to 24 values
 * - Values: [corner0.x, corner0.y, corner0.z, corner1.x, corner1.y, corner1.z, ...]
 * - Each value is a number (supports both integers and floats)
 * - Corner order: bottom (0-3), top (4-7)
 *
 * **For other shapes:**
 * - Shape-specific offset data
 * - Length and interpretation depends on shape type
 * - See BlockModifier.offsets documentation for details
 *
 * **Network optimization:**
 * - Can omit trailing values if they are 0 (default)
 * - Empty array or undefined = no offsets (all default)
 * - Reduces network data transmission
 *
 * @example
 * // Full cube with integer offsets
 * offsets: [1, 0, 0, -1, 0, 0, 1, 0, 0, -1, 0, 0, 1, 2, 0, -1, 2, 0, 1, 2, 0, -1, 2, 0]
 *
 * @example
 * // Cube with precise float offsets for smooth geometry
 * offsets: [0.5, 0.25, 0, -0.5, 0.25, 0]
 *
 * @example
 * // Only first 3 values (corner 0), rest default to 0
 * offsets: [1, 0, 0]
 *
 * @example
 * // No offsets (default geometry)
 * offsets: undefined
 */
export type Offsets = number[];

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
 * Face flags for bitfield operations
 */
export enum FaceFlag {
  TOP = 1 << 0,    // 0b00000001 = 1
  BOTTOM = 1 << 1, // 0b00000010 = 2
  LEFT = 1 << 2,   // 0b00000100 = 4
  RIGHT = 1 << 3,  // 0b00001000 = 8
  FRONT = 1 << 4,  // 0b00010000 = 16
  BACK = 1 << 5,   // 0b00100000 = 32
  FIXED = 1 << 6,  // 0b01000000 = 64 (fixed mode, not auto-calculated)
}

/**
 * Helper utilities for FaceVisibility bitfield operations
 */
export namespace FaceVisibilityHelper {
  /**
   * Create a new FaceVisibility with all faces invisible
   */
  export function create(): FaceVisibility {
    return { value: 0 };
  }

  /**
   * Create a new FaceVisibility with all faces visible
   */
  export function createAllVisible(): FaceVisibility {
    return {
      value:
        FaceFlag.TOP |
        FaceFlag.BOTTOM |
        FaceFlag.LEFT |
        FaceFlag.RIGHT |
        FaceFlag.FRONT |
        FaceFlag.BACK,
    };
  }

  /**
   * Check if a specific face is visible
   * @param fv FaceVisibility to check
   * @param face Face flag to check
   * @returns true if face is visible
   */
  export function isVisible(fv: FaceVisibility, face: FaceFlag): boolean {
    return (fv.value & face) !== 0;
  }

  /**
   * Set a face as visible
   * @param fv FaceVisibility to modify
   * @param face Face flag to set
   */
  export function setVisible(fv: FaceVisibility, face: FaceFlag): void {
    fv.value |= face;
  }

  /**
   * Set a face as invisible
   * @param fv FaceVisibility to modify
   * @param face Face flag to clear
   */
  export function setInvisible(fv: FaceVisibility, face: FaceFlag): void {
    fv.value &= ~face;
  }

  /**
   * Toggle a face visibility
   * @param fv FaceVisibility to modify
   * @param face Face flag to toggle
   */
  export function toggle(fv: FaceVisibility, face: FaceFlag): void {
    fv.value ^= face;
  }

  /**
   * Check if fixed mode is enabled (not auto-calculated)
   * @param fv FaceVisibility to check
   * @returns true if fixed mode
   */
  export function isFixed(fv: FaceVisibility): boolean {
    return (fv.value & FaceFlag.FIXED) !== 0;
  }

  /**
   * Set fixed mode (disable auto-calculation)
   * @param fv FaceVisibility to modify
   */
  export function setFixed(fv: FaceVisibility): void {
    fv.value |= FaceFlag.FIXED;
  }

  /**
   * Set auto mode (enable auto-calculation)
   * @param fv FaceVisibility to modify
   */
  export function setAuto(fv: FaceVisibility): void {
    fv.value &= ~FaceFlag.FIXED;
  }

  /**
   * Get all visible faces as array
   * @param fv FaceVisibility to check
   * @returns Array of visible face names
   */
  export function getVisibleFaces(fv: FaceVisibility): string[] {
    const faces: string[] = [];
    if (isVisible(fv, FaceFlag.TOP)) faces.push('top');
    if (isVisible(fv, FaceFlag.BOTTOM)) faces.push('bottom');
    if (isVisible(fv, FaceFlag.LEFT)) faces.push('left');
    if (isVisible(fv, FaceFlag.RIGHT)) faces.push('right');
    if (isVisible(fv, FaceFlag.FRONT)) faces.push('front');
    if (isVisible(fv, FaceFlag.BACK)) faces.push('back');
    return faces;
  }

  /**
   * Count number of visible faces
   * @param fv FaceVisibility to check
   * @returns Number of visible faces (0-6)
   */
  export function countVisible(fv: FaceVisibility): number {
    let count = 0;
    // Count set bits in first 6 bits
    let value = fv.value & 0b00111111; // Mask out FIXED bit
    while (value) {
      count += value & 1;
      value >>= 1;
    }
    return count;
  }

  /**
   * Create FaceVisibility from face names
   * @param faces Array of face names
   * @param fixed Whether to use fixed mode
   * @returns New FaceVisibility
   */
  export function fromFaces(faces: string[], fixed = false): FaceVisibility {
    const fv = create();
    faces.forEach((face) => {
      switch (face.toLowerCase()) {
        case 'top':
          setVisible(fv, FaceFlag.TOP);
          break;
        case 'bottom':
          setVisible(fv, FaceFlag.BOTTOM);
          break;
        case 'left':
          setVisible(fv, FaceFlag.LEFT);
          break;
        case 'right':
          setVisible(fv, FaceFlag.RIGHT);
          break;
        case 'front':
          setVisible(fv, FaceFlag.FRONT);
          break;
        case 'back':
          setVisible(fv, FaceFlag.BACK);
          break;
      }
    });
    if (fixed) {
      setFixed(fv);
    }
    return fv;
  }

  /**
   * Clone a FaceVisibility
   * @param fv FaceVisibility to clone
   * @returns New FaceVisibility with same value
   */
  export function clone(fv: FaceVisibility): FaceVisibility {
    return { value: fv.value };
  }

  /**
   * Convert to string representation for debugging
   * @param fv FaceVisibility to convert
   * @returns String representation
   */
  export function toString(fv: FaceVisibility): string {
    const faces = getVisibleFaces(fv);
    const mode = isFixed(fv) ? 'fixed' : 'auto';
    return `FaceVisibility(${faces.join(',')}, ${mode})`;
  }
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
   * Geometry offsets (optional)
   *
   * Flexible offset array for geometry modification.
   * Length and interpretation depends on block shape.
   * Can be partial (trailing zeros omitted for network optimization).
   *
   * @see Offsets for detailed documentation
   */
  offsets?: Offsets;

  /**
   * Face visibility flags (1 byte)
   * Determines which faces are visible or if it's auto-calculated
   */
  faceVisibility?: FaceVisibility;

  /**
   * Block-specific metadata (optional)
   *
   * Contains instance-specific data like display name and optional modifier overrides.
   * Transmitted over network only when present.
   * @see BlockMetadata
   */
  metadata?: BlockMetadata;
}
