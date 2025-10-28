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
