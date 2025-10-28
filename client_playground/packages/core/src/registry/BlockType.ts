/**
 * Block Type System
 *
 * Central block type definitions used by server, client, and generators.
 */

/**
 * Block rendering shape/model type
 */
export enum BlockShape {
  /** Standard cube block (6 faces) */
  CUBE = 0,

  /** Cross model (plants, flowers) - 2 intersecting planes */
  CROSS = 1,

  /** All shapes are horizontal and vertical, edgeOffset will move the shapes and so create a hash like construct but will not distort the cube edges. */
  HASH = 2,

  /** Custom model (reserved) */
  MODEL = 3,

  /** Glass/transparent cube */
  GLASS = 4,

  /** Flat block (like carpet, water surface), only the upper face is rendered (1 face) */
  FLAT = 5,

  /** SPHERE instead of a cube, edgeOffset will resize the sphere radius */
  SPHERE = 6,

  /** COLUMN shape - a cylinder like shape centered in the block space, edgeOffset will resize the column radius */
  COLUMN = 7,

  /** CUBE With ROUND EDGES - A cube shape with rounded edges, edgeOffset will determine how rounded every edge are */
  ROUND_CUBE = 8,

  /** STEPS shape - Stair/treppen form with intermediate step for chaining blocks, supports edgeOffset and rotation */
  STEPS = 9,

  /** STAIR shape - Filled stair with full-width base and half-width top step, supports rotation */
  STAIR = 10,

  /** BILLBOARD shape - Flat textured quad that can be scaled and rotated, center adjustable via edgeOffset */
  BILLBOARD = 11,

  /** SPRITE shape - Cross-style (X-shaped) 2D sprite using 2 intersecting quads, optimized for many instances */
  SPRITE = 12,

  /** FLAME shape - Animated flame effect using vertical plane, edgeOffset adjusts center, scale controls size */
  FLAME = 13,

}

/**
 * Material type for special block behaviors
 */
export type BlockMaterial =
  | 'solid'      // Default solid block
  | 'water'      // Fluid water
  | 'lava'       // Fluid lava
  | 'barrier'    // Invisible barrier
  | 'gas';       // Air-like materials

/**
 * Tool types that can mine blocks
 */
export type ToolType =
  | 'any'        // Can be mined with any tool
  | 'none'       // Cannot be mined
  | 'pickaxe'
  | 'axe'
  | 'shovel'
  | 'hoe'
  | string[];    // Array of acceptable tools

/**
 * Block options - additional properties
 */
export interface BlockOptions {
  /** Whether the block is solid (has collision) */
  solid?: boolean;

  /** Whether the block is opaque (blocks light, culls faces) */
  opaque?: boolean;

  /** Whether the block is transparent (like glass) */
  transparent?: boolean;

  /** Block material type */
  material?: BlockMaterial;

  /** Whether the block is a fluid */
  fluid?: boolean;

  /** Fluid density (for physics) */
  fluidDensity?: number;

  /** Fluid viscosity (for physics) */
  viscosity?: number;

  /** Custom properties */
  [key: string]: any;
}

/**
 * Block Modifier - Overrides BlockType properties for individual block instances
 * All properties are optional and override the base BlockType when set
 */
export interface BlockModifier {
  /** Override block shape */
  shape?: BlockShape;

  /** Override texture */
  texture?: string | string[];

  /** Override block options */
  options?: BlockOptions;

  /** Override hardness */
  hardness?: number;

  /** Override mining time */
  miningtime?: number;

  /** Override required tool */
  tool?: ToolType;

  /** Override unbreakable flag */
  unbreakable?: boolean;

  /** Override solid flag */
  solid?: boolean;

  /** Override transparent flag */
  transparent?: boolean;

  /** Custom rotation around Y axis (0-360 degrees) - horizontal rotation */
  rotationY?: number;

  /** Custom rotation around X axis (0-360 degrees) - vertical tilt/pitch */
  rotationX?: number;

  /** Custom facing direction (0-5 for 6 directions) */
  facing?: number;

  /** @deprecated Use rotationY instead */
  rotation?: number;

  /** Custom color tint [R, G, B] (0-255) */
  color?: [number, number, number];

  /** Custom scale [x, y, z] */
  scale?: [number, number, number];

  /** Wind leafiness (0-1): 1 for leaf-like blocks, 0 for solid blocks */
  windLeafiness?: number;

  /** Wind stability (0-1): How resistant the block is to wind (0.1 for leaves, 0.6 for wood, 0.95 for stone) */
  windStability?: number;

  /** Wind lever up (0-n): Amplitude for upward/top vertices movement */
  windLeverUp?: number;

  /** Wind lever down (0-n): Amplitude for downward/bottom vertices movement */
  windLeverDown?: number;

  /** Sprite count - Number of sprites to render per block (for SPRITE shape) */
  spriteCount?: number;

  /** Additional custom properties */
  customProperties?: Record<string, any>;
}

/**
 * Core Block Type definition
 */
export interface BlockType {
  /** Numeric ID (assigned by registry) */
  id?: number;

  /** Unique string identifier (e.g., 'stone', 'grass', 'water') */
  name: string;

  /** Display name (optional, defaults to name) */
  displayName?: string;

  /** Block shape/model type */
  shape: BlockShape;

  /**
   * Texture paths
   * - Single string: all faces use same texture
   * - Array [top, bottom, sides]: different textures per face group
   * - Array [top, bottom, north, south, east, west]: individual faces
   */
  texture: string | string[];

  /** Additional block options */
  options?: BlockOptions;

  /** Block hardness (affects mining time) */
  hardness?: number;

  /** Base mining time in milliseconds */
  miningtime?: number;

  /** Required tool type(s) to mine */
  tool?: ToolType;

  /** Whether block is unbreakable */
  unbreakable?: boolean;

  /** Whether block is solid (shorthand for options.solid) */
  solid?: boolean;

  /** Whether block is transparent (shorthand for options.transparent) */
  transparent?: boolean;

  /** Wind leafiness (0-1): 1 for leaf-like blocks, 0 for solid blocks */
  windLeafiness?: number;

  /** Wind stability (0-1): How resistant the block is to wind (0.1 for leaves, 0.6 for wood, 0.95 for stone) */
  windStability?: number;

  /** Wind lever up (0-n): Amplitude for upward/top vertices movement */
  windLeverUp?: number;

  /** Wind lever down (0-n): Amplitude for downward/bottom vertices movement */
  windLeverDown?: number;

  /** Sprite count - Number of sprites to render per block (for SPRITE shape, default: 1) */
  spriteCount?: number;
}

/**
 * Create a standard block type with defaults
 */
export function createBlockType(
  name: string,
  shape: BlockShape,
  texture: string | string[],
  options?: Partial<BlockType>
): BlockType {
  return {
    name,
    shape,
    texture,
    hardness: 0,
    miningtime: 0,
    tool: 'any',
    unbreakable: false,
    solid: options?.options?.solid !== false,  // Default true
    transparent: options?.options?.transparent || false,
    ...options,
  };
}

/**
 * Create a solid cube block (most common)
 */
export function createCubeBlock(
  name: string,
  texture: string | string[],
  options?: Partial<BlockType>
): BlockType {
  return createBlockType(name, BlockShape.CUBE, texture, options);
}

/**
 * Create a plant/cross block
 */
export function createPlantBlock(
  name: string,
  texture: string,
  options?: Partial<BlockType>
): BlockType {
  return createBlockType(name, BlockShape.CROSS, texture, {
    ...options,
    options: {
      solid: false,
      opaque: false,
      ...options?.options,
    },
  });
}

/**
 * Create a transparent block (like glass)
 */
export function createTransparentBlock(
  name: string,
  texture: string,
  options?: Partial<BlockType>
): BlockType {
  return createBlockType(name, BlockShape.GLASS, texture, {
    ...options,
    transparent: true,
    options: {
      opaque: false,
      ...options?.options,
    },
  });
}

/**
 * Create a fluid block
 */
export function createFluidBlock(
  name: string,
  texture: string,
  material: 'water' | 'lava',
  options?: Partial<BlockType>
): BlockType {
  return createBlockType(name, BlockShape.FLAT, texture, {
    ...options,
    options: {
      material,
      fluid: true,
      fluidDensity: 30.0,
      viscosity: 200.5,
      ...options?.options,
    },
  });
}

/**
 * Apply a BlockModifier to a BlockType, creating a new modified BlockType
 * This creates a copy of the original BlockType with modifier properties applied
 */
export function applyModifier(baseType: BlockType, modifier: BlockModifier | null | undefined): BlockType {
  if (!modifier) {
    return baseType;
  }

  // Create a deep copy of the base type
  const modifiedType: BlockType = {
    ...baseType,
    // Apply modifier overrides
    shape: modifier.shape ?? baseType.shape,
    texture: modifier.texture ?? baseType.texture,
    hardness: modifier.hardness ?? baseType.hardness,
    miningtime: modifier.miningtime ?? baseType.miningtime,
    tool: modifier.tool ?? baseType.tool,
    unbreakable: modifier.unbreakable ?? baseType.unbreakable,
    solid: modifier.solid ?? baseType.solid,
    transparent: modifier.transparent ?? baseType.transparent,
    windLeafiness: modifier.windLeafiness ?? baseType.windLeafiness,
    windStability: modifier.windStability ?? baseType.windStability,
    windLeverUp: modifier.windLeverUp ?? baseType.windLeverUp,
    windLeverDown: modifier.windLeverDown ?? baseType.windLeverDown,
    spriteCount: modifier.spriteCount ?? baseType.spriteCount,
  };

  // Merge options
  if (modifier.options) {
    modifiedType.options = {
      ...baseType.options,
      ...modifier.options,
    };
  }

  return modifiedType;
}
