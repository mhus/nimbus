/**
 * EntityData - Entity (NPC, Player, etc.) definition
 *
 * TODO: Not yet fully defined
 */

import type { Vector3 } from './Vector3';

/**
 * Entity type
 */
export enum EntityType {
  NPC = 'npc',
  PLAYER = 'player',
  MOB = 'mob',
  ITEM = 'item',
}

/**
 * Entity visibility properties
 */
export interface EntityVisibility {
  /** Path to 3D model */
  modelPath?: string;

  /** Scale */
  scale?: Vector3;

  /** Visibility flag */
  visible?: boolean;
}

/**
 * Entity definition
 */
export interface EntityData {
  /** Unique entity ID */
  id: string;

  /** Entity type */
  type: EntityType;

  /** Visibility properties */
  visibility?: EntityVisibility;

  /** Current position */
  position: Vector3;

  /** Current rotation (euler angles) */
  rotation: Vector3;

  /** Target position for movement */
  walkToPosition?: Vector3;

  /** Additional entity-specific data */
  [key: string]: any;
}
