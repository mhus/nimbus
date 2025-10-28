/**
 * EntityData - Entity (NPC, Player, Mob, Item, etc.) definition
 *
 * Unified entity system where Player is an Entity with type='player'.
 * All player-specific fields are optional and only used when type='player'.
 */

import type { Vector3 } from './Vector3';
import type { Rotation } from './Rotation';

/**
 * Entity type
 */
export enum EntityType {
  PLAYER = 'player',
  NPC = 'npc',
  MOB = 'mob',
  ITEM = 'item',
  PROJECTILE = 'projectile',
  VEHICLE = 'vehicle',
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

  /** Skin/texture path (for players) */
  skinPath?: string;

  /** Animation state (e.g., 'idle', 'walk', 'run') */
  animationState?: string;
}

/**
 * Entity health/status
 */
export interface EntityHealth {
  /** Current health */
  current: number;

  /** Maximum health */
  max: number;

  /** Is alive */
  alive: boolean;
}

/**
 * Entity definition
 * Unified for all entity types (Player, NPC, Mob, Item, etc.)
 */
export interface EntityData {
  /** Unique entity ID */
  id: string;

  /** Entity type */
  type: EntityType;

  /** Visibility properties */
  visibility?: EntityVisibility;

  /** Current position (world coordinates) */
  position: Vector3;

  /** Current rotation */
  rotation: Rotation;

  /** Target position for movement (AI, pathfinding) */
  walkToPosition?: Vector3;

  /** Movement velocity */
  velocity?: Vector3;

  // Player-specific fields (optional, only when type='player')

  /**
   * Username (for players)
   */
  username?: string;

  /**
   * Display name (for players and NPCs)
   */
  displayName?: string;

  /**
   * User ID (for players)
   */
  userId?: string;

  /**
   * Health status (for players, mobs, NPCs)
   */
  health?: EntityHealth;

  /**
   * Inventory (for players)
   * Array of item IDs or item data
   */
  inventory?: any[]; // TODO: Define Inventory type

  /**
   * Currently held item (for players)
   */
  heldItem?: number; // Item ID or slot index

  /**
   * Player state (for players)
   * e.g., 'idle', 'walking', 'running', 'jumping', 'swimming', 'flying'
   */
  state?: string;

  /**
   * Is crouching (for players)
   */
  isCrouching?: boolean;

  /**
   * Is sprinting (for players)
   */
  isSprinting?: boolean;

  /**
   * Permissions/role (for players)
   */
  role?: string;

  /**
   * Team/faction (for players and NPCs)
   */
  team?: string;

  // NPC-specific fields (optional, only when type='npc')

  /**
   * NPC dialog ID (for NPCs)
   */
  dialogId?: string;

  /**
   * NPC behavior/AI state (for NPCs and mobs)
   */
  aiState?: string;

  // Mob-specific fields (optional, only when type='mob')

  /**
   * Mob aggression level (for mobs)
   */
  aggression?: number;

  /**
   * Target entity ID (for mobs and NPCs)
   */
  targetId?: string;

  // Item-specific fields (optional, only when type='item')

  /**
   * Item type ID (for items)
   */
  itemTypeId?: number;

  /**
   * Item stack count (for items)
   */
  stackCount?: number;

  /**
   * Can be picked up (for items)
   */
  canPickup?: boolean;

  // Additional metadata

  /**
   * Custom metadata (extensible)
   */
  metadata?: Record<string, any>;

  /**
   * Last update timestamp
   */
  lastUpdate?: number;
}

/**
 * Helper to create player entity
 */
export function createPlayer(
  id: string,
  userId: string,
  username: string,
  displayName: string,
  position: Vector3
): EntityData {
  return {
    id,
    type: EntityType.PLAYER,
    userId,
    username,
    displayName,
    position,
    rotation: { y: 0, p: 0 },
    health: {
      current: 20,
      max: 20,
      alive: true,
    },
    state: 'idle',
    visibility: {
      visible: true,
    },
  };
}

/**
 * Helper to create NPC entity
 */
export function createNPC(
  id: string,
  displayName: string,
  position: Vector3,
  modelPath: string,
  dialogId?: string
): EntityData {
  return {
    id,
    type: EntityType.NPC,
    displayName,
    position,
    rotation: { y: 0, p: 0 },
    dialogId,
    visibility: {
      modelPath,
      visible: true,
    },
    aiState: 'idle',
  };
}

/**
 * Helper to create item entity
 */
export function createItem(
  id: string,
  itemTypeId: number,
  position: Vector3,
  stackCount: number = 1
): EntityData {
  return {
    id,
    type: EntityType.ITEM,
    itemTypeId,
    stackCount,
    position,
    rotation: { y: 0, p: 0 },
    canPickup: true,
    visibility: {
      visible: true,
    },
  };
}

/**
 * Type guard: Check if entity is a player
 */
export function isPlayer(entity: EntityData): boolean {
  return entity.type === EntityType.PLAYER;
}

/**
 * Type guard: Check if entity is an NPC
 */
export function isNPC(entity: EntityData): boolean {
  return entity.type === EntityType.NPC;
}

/**
 * Type guard: Check if entity is a mob
 */
export function isMob(entity: EntityData): boolean {
  return entity.type === EntityType.MOB;
}

/**
 * Type guard: Check if entity is an item
 */
export function isItem(entity: EntityData): boolean {
  return entity.type === EntityType.ITEM;
}
