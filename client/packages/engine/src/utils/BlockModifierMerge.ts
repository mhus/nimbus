/**
 * BlockModifierMerge - Merge block modifiers according to priority rules
 *
 * Merge priority (first defined field wins):
 * 1. Block.modifiers[status] (Instance-specific modifier)
 * 2. BlockType.modifiers[status] (Type-defined modifier for status)
 * 3. BlockType.modifiers[0] (Default status fallback)
 * 4. Default values (e.g., shape = 0)
 *
 * Each field (visibility, physics, wind, etc.) is merged independently.
 * If a field is undefined in a higher priority modifier, it falls back to lower priority.
 */
import { BlockModifier, BlockType, Block } from '@nimbus/shared';
import type { AppContext } from '../AppContext.js';

/**
 * Deep merge two objects field by field
 * Only merges defined fields (skips undefined)
 *
 * @param target Base object (lower priority)
 * @param source Override object (higher priority)
 * @returns Merged object
 */
function mergeObjects<T extends Record<string, any>>(target: T | undefined, source: T | undefined): T {
  if (!source) {
    return (target || {}) as T;
  }
  if (!target) {
    return source;
  }

  const result = { ...target };
  for (const key in source) {
    if (source[key] !== undefined) {
      result[key] = source[key];
    }
  }
  return result;
}

/**
 * Merge visibility modifiers with special handling for textures
 * Textures are merged by texture key (TextureKey enum values)
 *
 * Special effect/effectParameters handling:
 * - VisibilityModifier.effect is the default for all textures
 * - If a TextureDefinition doesn't have effect set, it inherits from VisibilityModifier
 * - If a TextureDefinition is a string, it's converted to an object with effect from VisibilityModifier
 *
 * @param target Base visibility modifier
 * @param source Override visibility modifier
 * @returns Merged visibility modifier
 */
function mergeVisibility(target: any, source: any): any {
  if (!source) {
    return target;
  }
  if (!target) {
    return source;
  }

  const result = { ...target };

  // Merge all fields except textures
  for (const key in source) {
    if (key === 'textures') {
      continue; // Handle textures separately
    }
    if (source[key] !== undefined) {
      result[key] = source[key];
    }
  }

  // Special handling for textures: merge texture records by key
  if (source.textures !== undefined) {
    result.textures = {
      ...(target.textures || {}),
      ...(source.textures || {}),
    };
  }

  // Apply VisibilityModifier.effect and effectParameters to textures
  if (result.textures && (result.effect !== undefined || result.effectParameters !== undefined)) {
    const defaultEffect = result.effect;
    const defaultEffectParameters = result.effectParameters;

    for (const key in result.textures) {
      let texture = result.textures[key];

      // If texture is a string and we have default effect/effectParameters, convert to object
      if (typeof texture === 'string' && (defaultEffect !== undefined || defaultEffectParameters !== undefined)) {
        texture = { path: texture };
        result.textures[key] = texture;
      }

      // If texture is an object, apply defaults if not set
      if (typeof texture === 'object' && texture !== null) {
        if (defaultEffect !== undefined && texture.effect === undefined) {
          texture.effect = defaultEffect;
        }
        if (defaultEffectParameters !== undefined && texture.effectParameters === undefined) {
          texture.effectParameters = defaultEffectParameters;
        }
      }
    }
  }

  return result;
}

/**
 * Deep merge two BlockModifier objects
 * Fields from source override fields in target only if they are not undefined
 *
 * @param target Base modifier (lower priority)
 * @param source Override modifier (higher priority)
 * @returns Merged BlockModifier
 */
function deepMergeModifiers(target: BlockModifier | undefined, source: BlockModifier | undefined): BlockModifier {
  // If source is undefined, return target or empty object
  if (!source) {
    return target || {};
  }

  // If target is undefined, return source
  if (!target) {
    return source;
  }

  // Merge each top-level field independently with deep merge
  return {
    visibility: source.visibility !== undefined
      ? mergeVisibility(target.visibility, source.visibility)
      : target.visibility,
    wind: source.wind !== undefined
      ? mergeObjects(target.wind, source.wind)
      : target.wind,
    illumination: source.illumination !== undefined
      ? mergeObjects(target.illumination, source.illumination)
      : target.illumination,
    physics: source.physics !== undefined
      ? mergeObjects(target.physics, source.physics)
      : target.physics,
    effects: source.effects !== undefined
      ? mergeObjects(target.effects, source.effects)
      : target.effects,
    sound: source.sound !== undefined
      ? mergeObjects(target.sound, source.sound)
      : target.sound,
  };
}

/**
 * Merge BlockModifier according to priority rules
 *
 * Each field (visibility, physics, etc.) is merged independently.
 * Higher priority modifiers override lower priority only for defined fields.
 *
 * @param block Block instance
 * @param blockType BlockType definition
 * @returns Merged BlockModifier
 */
export function mergeBlockModifier(
  appContext: AppContext,
  block: Block,
  blockType: BlockType,
  overwriteStatus?: number
): BlockModifier {
  // Status is determined from BlockType.initialStatus (default: 0)
  const status = mergeStatus(appContext, block, blockType, overwriteStatus);

  // Start with default modifier
  let result: BlockModifier = {
    visibility: {
      shape: 0, // Default shape
      textures: {},
    },
  };

  // Priority 3: Merge BlockType default status (0) as base
  if (blockType.modifiers[0]) {
    result = deepMergeModifiers(result, blockType.modifiers[0]);
  }

  // Priority 2: Merge BlockType status-specific modifiers
  if (status !== 0 && blockType.modifiers[status]) {
    result = deepMergeModifiers(result, blockType.modifiers[status]);
  }

  // Priority 1: Merge Block instance modifiers (highest priority)
  if (block.modifiers && block.modifiers[status]) {
    result = deepMergeModifiers(result, block.modifiers[status]);
  }

  return result;
}

export function mergeStatus(
  appContext: AppContext,
  block: Block,
  blockType: BlockType,
  overwriteStatus?: number
): number {

    // TODO worldStatus
    // seasonalStatus if seasonal status is defined in block modifier

  // Use overwriteStatus if provided
  if (overwriteStatus !== undefined) {
    return overwriteStatus;
  }

  // Use block's own status if defined
  if (block.status !== undefined) {
    return block.status;
  }

  // Fallback to BlockType's initialStatus or default to 0
  return blockType.initialStatus ?? 0;
}

/**
 * Get block position key for Map lookup
 *
 * @param x World X coordinate
 * @param y World Y coordinate
 * @param z World Z coordinate
 * @returns Position key string
 */
export function getBlockPositionKey(x: number, y: number, z: number): string {
  return `${x},${y},${z}`;
}
