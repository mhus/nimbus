/**
 * BlockModifier - Visual and behavioral properties for blocks
 *
 * All parameters are optional to minimize data transmission and storage.
 * Parameter names may be shortened or replaced with numbers to reduce data size.
 *
 * Rendering order: Offsets → Scale → Rotation
 */

import type { Shape } from './Shape';
import type { Vector3 } from './Vector3';

/**
 * Texture mapping keys
 */
export enum TextureKey {
  ALL = 0,
  TOP = 1,
  BOTTOM = 2,
  LEFT = 3,
  RIGHT = 4,
  FRONT = 5,
  BACK = 6,
  SIDE = 7,
  DIFFUSE = 8,
  DISTORTION = 9,
  OPACITY = 10,
  // 100+ reserved for shape-specific textures
}

/**
 * Human-readable texture key names
 */
export const TextureKeyNames: Record<TextureKey, string> = {
  [TextureKey.ALL]: 'all',
  [TextureKey.TOP]: 'top',
  [TextureKey.BOTTOM]: 'bottom',
  [TextureKey.LEFT]: 'left',
  [TextureKey.RIGHT]: 'right',
  [TextureKey.FRONT]: 'front',
  [TextureKey.BACK]: 'back',
  [TextureKey.SIDE]: 'side',
  [TextureKey.DIFFUSE]: 'diffuse',
  [TextureKey.DISTORTION]: 'distortion',
  [TextureKey.OPACITY]: 'opacity',
};

/**
 * Texture rotation values
 */
export enum TextureRotation {
  R0 = 0,
  R90 = 1,
  R180 = 2,
  R270 = 3,
  FLIP_0 = 4,
  FLIP_90 = 5,
  FLIP_180 = 6,
  FLIP_270 = 7,
}

/**
 * Texture sampling mode
 */
export enum SamplingMode {
  NEAREST = 0,
  LINEAR = 1,
  MIPMAP = 2,
}

/**
 * Texture transparency mode
 */
export enum TransparencyMode {
  NONE = 0,
  HAS_ALPHA = 1,
  ALPHA_FROM_RGB = 2,
}

/**
 * Direction flags
 */
export enum Direction {
  NORTH = 1,
  SOUTH = 2,
  EAST = 4,
  WEST = 8,
  UP = 16,
  DOWN = 32,
}

/**
 * UV mapping coordinates
 */
export interface UVMapping {
  x: number;
  y: number;
  w: number;
  h: number;
}

/**
 * Texture definition
 */
export interface TextureDefinition {
  /** Path to texture file */
  path: string;

  /** UV mapping coordinates */
  uvMapping?: UVMapping;

  /** Texture rotation */
  rotation?: TextureRotation;

  /** Sampling mode */
  samplingMode?: SamplingMode;

  /** Transparency mode */
  transparencyMode?: TransparencyMode;

  /** Tint color */
  color?: string;
}

/**
 * Visibility properties
 */
export interface VisibilityModifier {
  /** Shape type */
  shape?: Shape;

  /** Effect name (e.g., water, wind, flipbox, lava, fog) */
  effect?: string;

  /** Effect-specific parameters */
  effectParameters?: Record<string, any>;

  /**
   * Corner offsets (8 corners × 3 axes = 24 bytes)
   * Values range from -127 to 127
   */
  offsets?: number[];

  /** Scaling factor X */
  scalingX?: number;

  /** Scaling factor Y */
  scalingY?: number;

  /** Scaling factor Z */
  scalingZ?: number;

  /** Rotation X (degrees) */
  rotationX?: number;

  /** Rotation Y (degrees) */
  rotationY?: number;

  /** Path to model file (for shape=MODEL) */
  path?: string;

  /** Texture definitions (key = TextureKey) */
  textures?: Record<number, TextureDefinition>;
}

/**
 * Wind properties
 */
export interface WindModifier {
  /** How much the block is affected by wind (leaf-like behavior) */
  leafiness?: number;

  /** Stability/rigidity */
  stability?: number;

  /** Upper lever arm */
  leverUp?: number;

  /** Lower lever arm */
  leverDown?: number;
}

/**
 * Illumination properties
 */
export interface IlluminationModifier {
  /** Light color */
  color?: string;

  /** Light strength */
  strength?: number;
}

/**
 * Physics properties
 */
export interface PhysicsModifier {
  /** Solid collision */
  solid?: boolean;

  /** Movement resistance */
  resistance?: number;

  /** Climbable with resistance factor */
  climbable?: number;

  /** Auto-move velocity when standing on block */
  autoMoveXYZ?: Vector3;

  /** Interactive block */
  interactive?: boolean;

  /** Gate passable from specific directions */
  gateFromDirection?: Direction;
}

/**
 * Sky effect properties
 */
export interface SkyEffect {
  /** Sky intensity */
  intensity?: number;

  /** Sky color */
  color?: string;

  /** Wind/weather effect */
  wind?: any; // TODO: Define weather structure
}

/**
 * Effects properties
 */
export interface EffectsModifier {
  /** Force ego/first-person view */
  forceEgoView?: boolean;

  /** Sky effect */
  sky?: SkyEffect;
}

/**
 * Sound properties
 */
export interface SoundModifier {
  /** Walking sound */
  walk?: string;

  /** Walking sound volume */
  walkVolume?: number;

  /** Permanent/ambient sound */
  permanent?: string;

  /** Permanent sound volume */
  permanentVolume?: number;

  /** Status change sound */
  changeStatus?: string;

  /** Status change sound volume */
  changeStatusVolume?: number;
}

/**
 * BlockModifier - Complete modifier definition
 */
export interface BlockModifier {
  /** Visibility properties */
  visibility?: VisibilityModifier;

  /** Wind properties */
  wind?: WindModifier;

  /** Sprite count */
  spriteCount?: number;

  /** Alpha/transparency */
  alpha?: number;

  /** Illumination properties */
  illumination?: IlluminationModifier;

  /** Physics properties */
  physics?: PhysicsModifier;

  /** Effects */
  effects?: EffectsModifier;

  /** Sound properties */
  sound?: SoundModifier;
}

/**
 * Texture helper functions for efficient texture access
 */
export namespace TextureHelper {
  /**
   * Convert TextureKey enum to numeric index
   * @param key TextureKey enum value
   * @returns Numeric index
   */
  export function textureKeyToIndex(key: TextureKey): number {
    return key as number;
  }

  /**
   * Convert numeric index to TextureKey enum
   * @param index Numeric index
   * @returns TextureKey enum value
   */
  export function indexToTextureKey(index: number): TextureKey {
    return index as TextureKey;
  }

  /**
   * Get texture from visibility modifier
   * @param visibility Visibility modifier
   * @param key Texture key
   * @returns Texture definition or undefined
   */
  export function getTexture(
    visibility: VisibilityModifier,
    key: TextureKey
  ): TextureDefinition | undefined {
    if (!visibility.textures) {
      return undefined;
    }
    const index = textureKeyToIndex(key);
    return visibility.textures[index];
  }

  /**
   * Set texture in visibility modifier
   * @param visibility Visibility modifier
   * @param key Texture key
   * @param texture Texture definition
   */
  export function setTexture(
    visibility: VisibilityModifier,
    key: TextureKey,
    texture: TextureDefinition
  ): void {
    if (!visibility.textures) {
      visibility.textures = {};
    }
    const index = textureKeyToIndex(key);
    visibility.textures[index] = texture;
  }

  /**
   * Remove texture from visibility modifier
   * @param visibility Visibility modifier
   * @param key Texture key
   */
  export function removeTexture(
    visibility: VisibilityModifier,
    key: TextureKey
  ): void {
    if (!visibility.textures) {
      return;
    }
    const index = textureKeyToIndex(key);
    delete visibility.textures[index];
  }

  /**
   * Check if texture exists
   * @param visibility Visibility modifier
   * @param key Texture key
   * @returns True if texture exists
   */
  export function hasTexture(
    visibility: VisibilityModifier,
    key: TextureKey
  ): boolean {
    if (!visibility.textures) {
      return false;
    }
    const index = textureKeyToIndex(key);
    return visibility.textures[index] !== undefined;
  }

  /**
   * Get all texture keys that have textures defined
   * @param visibility Visibility modifier
   * @returns Array of TextureKey values
   */
  export function getDefinedTextureKeys(
    visibility: VisibilityModifier
  ): TextureKey[] {
    if (!visibility.textures) {
      return [];
    }
    return Object.keys(visibility.textures)
      .map((key) => parseInt(key, 10))
      .filter((key) => !isNaN(key))
      .map((key) => indexToTextureKey(key));
  }

  /**
   * Get texture count
   * @param visibility Visibility modifier
   * @returns Number of defined textures
   */
  export function getTextureCount(visibility: VisibilityModifier): number {
    if (!visibility.textures) {
      return 0;
    }
    return Object.keys(visibility.textures).length;
  }

  /**
   * Clone textures map
   * @param visibility Visibility modifier
   * @returns Cloned textures map
   */
  export function cloneTextures(
    visibility: VisibilityModifier
  ): Record<number, TextureDefinition> | undefined {
    if (!visibility.textures) {
      return undefined;
    }
    const cloned: Record<number, TextureDefinition> = {};
    for (const [key, texture] of Object.entries(visibility.textures)) {
      cloned[parseInt(key, 10)] = { ...texture };
    }
    return cloned;
  }

  /**
   * Get texture by name (for convenience)
   * @param visibility Visibility modifier
   * @param name Texture name ('top', 'bottom', etc.)
   * @returns Texture definition or undefined
   */
  export function getTextureByName(
    visibility: VisibilityModifier,
    name: string
  ): TextureDefinition | undefined {
    const key = getTextureKeyByName(name);
    if (key === undefined) {
      return undefined;
    }
    return getTexture(visibility, key);
  }

  /**
   * Set texture by name (for convenience)
   * @param visibility Visibility modifier
   * @param name Texture name ('top', 'bottom', etc.)
   * @param texture Texture definition
   */
  export function setTextureByName(
    visibility: VisibilityModifier,
    name: string,
    texture: TextureDefinition
  ): void {
    const key = getTextureKeyByName(name);
    if (key !== undefined) {
      setTexture(visibility, key, texture);
    }
  }

  /**
   * Get TextureKey by name
   * @param name Texture name
   * @returns TextureKey or undefined
   */
  export function getTextureKeyByName(name: string): TextureKey | undefined {
    const normalized = name.toLowerCase();
    for (const [key, keyName] of Object.entries(TextureKeyNames)) {
      if (keyName === normalized) {
        return parseInt(key, 10) as TextureKey;
      }
    }
    return undefined;
  }

  /**
   * Get name for TextureKey
   * @param key TextureKey
   * @returns Texture name
   */
  export function getTextureName(key: TextureKey): string {
    return TextureKeyNames[key] ?? 'unknown';
  }

  /**
   * Create simple texture definition
   * @param path Texture path
   * @returns Simple texture definition
   */
  export function createTexture(path: string): TextureDefinition {
    return { path };
  }

  /**
   * Create texture definition with UV mapping
   * @param path Texture path
   * @param uvMapping UV coordinates
   * @returns Texture definition with UV mapping
   */
  export function createTextureWithUV(
    path: string,
    uvMapping: UVMapping
  ): TextureDefinition {
    return { path, uvMapping };
  }

  /**
   * Set all cube face textures to same texture
   * @param visibility Visibility modifier
   * @param texture Texture definition
   */
  export function setAllFaces(
    visibility: VisibilityModifier,
    texture: TextureDefinition
  ): void {
    setTexture(visibility, TextureKey.TOP, texture);
    setTexture(visibility, TextureKey.BOTTOM, texture);
    setTexture(visibility, TextureKey.LEFT, texture);
    setTexture(visibility, TextureKey.RIGHT, texture);
    setTexture(visibility, TextureKey.FRONT, texture);
    setTexture(visibility, TextureKey.BACK, texture);
  }

  /**
   * Set side faces (left, right, front, back) to same texture
   * @param visibility Visibility modifier
   * @param texture Texture definition
   */
  export function setSideFaces(
    visibility: VisibilityModifier,
    texture: TextureDefinition
  ): void {
    setTexture(visibility, TextureKey.LEFT, texture);
    setTexture(visibility, TextureKey.RIGHT, texture);
    setTexture(visibility, TextureKey.FRONT, texture);
    setTexture(visibility, TextureKey.BACK, texture);
  }
}
