/**
 * Backdrop - Dynamic pseudo walls at chunk boundaries
 *
 * Backdrops are rendered dynamically at the edges of the visible/loaded chunk area.
 * They are NOT stored in chunk data, but calculated based on which chunks are loaded.
 *
 * Used for:
 * - Preventing sun from shining into tunnels
 * - Far-away rendering with alpha fading at the edge of the visible world
 */

/**
 * Backdrop configuration
 *
 * Defines visual properties for a backdrop segment
 */
export interface Backdrop {
  /** Backdrop type ID reference (optional) */
  typeId?: number;

  /** Local X/Z coordinate a (0-16) - start, default 0 */
  la?: number;

  /** World Y coordinate a - start, default 0 */
  ya?: number;

  /** Local X/Z coordinate b (0-16) - end, default 16 */
  lb?: number;

  /** World Y coordinate b - end, default ya + 60 */
  yb?: number;

  /** Texture path (e.g., "textures/backdrop/hills.png") */
  texture?: string;

  /** Color tint (hex string like "#808080") */
  color?: string;

  /** Alpha transparency (0-1) */
  alpha?: number;

  /** Alpha blending mode */
  alphaMode?: number;
}

/**
 * Backdrop direction enum
 */
export enum BackdropDirection {
  NORTH = 'north',
  EAST = 'east',
  SOUTH = 'south',
  WEST = 'west',
}

/**
 * Backdrop position - identifies where a backdrop should be rendered
 */
export interface BackdropPosition {
  /** Chunk X coordinate where backdrop is needed */
  cx: number;

  /** Chunk Z coordinate where backdrop is needed */
  cz: number;

  /** Which direction(s) need backdrops at this position */
  directions: BackdropDirection[];
}
