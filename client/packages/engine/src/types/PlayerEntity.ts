/**
 * PlayerEntity - Player as physics entity with player info
 *
 * Extends PhysicsEntity with PlayerInfo to store player-specific
 * properties that can change dynamically during gameplay.
 *
 * **Cached Effective Values:**
 * For performance, effective values from PlayerInfo are cached directly on the entity.
 * These are updated when 'playerInfo:updated' event is emitted.
 * PhysicsService uses these cached values instead of looking up PlayerInfo each frame.
 */

import type { PhysicsEntity } from '../services/PhysicsService';
import type { PlayerInfo } from '@nimbus/shared';

/**
 * Player entity with physics and player-specific properties
 *
 * Combines physics simulation (PhysicsEntity) with player configuration (PlayerInfo).
 * PlayerInfo values can be updated dynamically through power-ups, status effects, equipment, etc.
 *
 * Cached effective values are updated via 'playerInfo:updated' event for performance.
 */
export interface PlayerEntity extends PhysicsEntity {
  /** Player-specific configuration and properties (full info) */
  playerInfo: PlayerInfo;

  // ============================================
  // Cached Effective Values (Performance Optimization)
  // Updated via 'playerInfo:updated' event
  // ============================================

  /** Cached effective walk speed for physics calculations */
  effectiveWalkSpeed: number;

  /** Cached effective run speed for physics calculations */
  effectiveRunSpeed: number;

  /** Cached effective underwater speed for physics calculations */
  effectiveUnderwaterSpeed: number;

  /** Cached effective crawl speed for physics calculations */
  effectiveCrawlSpeed: number;

  /** Cached effective riding speed for physics calculations */
  effectiveRidingSpeed: number;

  /** Cached effective jump speed for physics calculations */
  effectiveJumpSpeed: number;

  /** Cached effective turn speed for camera control (on land) */
  effectiveTurnSpeed: number;

  /** Cached effective underwater turn speed for camera control */
  effectiveUnderwaterTurnSpeed: number;
}
