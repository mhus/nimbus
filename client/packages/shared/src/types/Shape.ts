/**
 * Shape - Defines the visual shape/geometry of a block
 */

export enum Shape {
  /** Pseudo-block, no modifiers possible, truly empty space */
  AIR = 0,

  /** Not visible but can have modifiers (effects, etc.) */
  INVISIBLE = 1,

  /** Standard cube */
  CUBE = 2,

  /** Cross shape (two intersecting planes) */
  CROSS = 3,

  /** Hash/grid pattern */
  HASH = 4,

  /** Custom 3D model */
  MODEL = 5,

  /** Glass cube */
  GLASS = 6,

  /** Flat glass pane */
  GLASS_FLAT = 7,

  /** Flat surface */
  FLAT = 8,

  /** Sphere */
  SPHERE = 9,

  /** Column/pillar */
  COLUMN = 10,

  /** Rounded cube */
  ROUND_CUBE = 11,

  /** Steps */
  STEPS = 12,

  /** Stairs */
  STAIR = 13,

  /** Billboard (always faces camera) */
  BILLBOARD = 14,

  /** Sprite */
  SPRITE = 15,

  /** Flame effect */
  FLAME = 16,

  /** Ocean water (flat) */
  OCEAN = 17,

  /** Ocean coast variation */
  OCEAN_COAST = 18,

  /** Ocean maelstrom variation */
  OCEAN_MAELSTROM = 19,

  /** River water (flat, directional) */
  RIVER = 20,

  /** River waterfall */
  RIVER_WATERFALL = 21,

  /** River waterfall with whirlpool */
  RIVER_WATERFALL_WHIRLPOOL = 22,

  /** Water cube */
  WATER = 23,

  /** Lava */
  LAVA = 24,

  /** Fog */
  FOG = 25,
}

/**
 * Human-readable shape names
 */
export const ShapeNames: Record<Shape, string> = {
  [Shape.AIR]: 'air',
  [Shape.INVISIBLE]: 'invisible',
  [Shape.CUBE]: 'cube',
  [Shape.CROSS]: 'cross',
  [Shape.HASH]: 'hash',
  [Shape.MODEL]: 'model',
  [Shape.GLASS]: 'glass',
  [Shape.GLASS_FLAT]: 'glass_flat',
  [Shape.FLAT]: 'flat',
  [Shape.SPHERE]: 'sphere',
  [Shape.COLUMN]: 'column',
  [Shape.ROUND_CUBE]: 'round_cube',
  [Shape.STEPS]: 'steps',
  [Shape.STAIR]: 'stair',
  [Shape.BILLBOARD]: 'billboard',
  [Shape.SPRITE]: 'sprite',
  [Shape.FLAME]: 'flame',
  [Shape.OCEAN]: 'ocean',
  [Shape.OCEAN_COAST]: 'ocean_coast',
  [Shape.OCEAN_MAELSTROM]: 'ocean_maelstrom',
  [Shape.RIVER]: 'river',
  [Shape.RIVER_WATERFALL]: 'river_waterfall',
  [Shape.RIVER_WATERFALL_WHIRLPOOL]: 'river_waterfall_whirlpool',
  [Shape.WATER]: 'water',
  [Shape.LAVA]: 'lava',
  [Shape.FOG]: 'fog',
};
