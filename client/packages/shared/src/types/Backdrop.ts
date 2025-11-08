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
  /** Backdrop ID - loads backdrop type from server (e.g., "fog1", "stone") */
  id?: string;

  /** Backdrop type ID reference (optional) */
  typeId?: number;

  /** Left position - local X/Z coordinate (0-16), default 0 */
  left?: number;

  /** Width of the backdrop segment (0-16), default 16 */
  width?: number;

  /** Base Y coordinate (bottom) - if not provided, use groundLevel at this edge */
  yBase?: number;

  /** Height of the backdrop - relative to yBase, default 60 */
  height?: number;

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
