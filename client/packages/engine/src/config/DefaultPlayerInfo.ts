/**
 * Default PlayerInfo configuration
 *
 * These are the baseline values for player properties.
 * They can be overridden by:
 * - Server configuration
 * - Player equipment
 * - Status effects / power-ups
 * - World-specific settings
 *
 * Initially, effective values equal base values.
 * Effective values are modified by power-ups, equipment, status effects, etc.
 */

import type { PlayerInfo } from '@nimbus/shared';

/**
 * Default player configuration
 *
 * Speed values are based on the original PhysicsService constants:
 * - Walk speed: 5.0 blocks/second (normal movement)
 * - Run speed: 7.0 blocks/second (sprint)
 * - Underwater speed: 3.0 blocks/second (swimming)
 * - Jump speed: 8.0 blocks/second (results in ~1.25 block jump height)
 * - Eye height: 1.6 blocks (eye level for first-person camera)
 */
export const DEFAULT_PLAYER_INFO: PlayerInfo = {
  // Identity
  playerId: 'local_player',
  displayName: 'Player',

  // Base movement speeds (blocks per second)
  baseWalkSpeed: 5.0,
  baseRunSpeed: 7.0,
  baseUnderwaterSpeed: 3.0,
  baseCrawlSpeed: 2.5,
  baseRidingSpeed: 8.0,
  baseJumpSpeed: 8.0,

  // Effective movement speeds (initially same as base)
  effectiveWalkSpeed: 5.0,
  effectiveRunSpeed: 7.0,
  effectiveUnderwaterSpeed: 3.0,
  effectiveCrawlSpeed: 2.5,
  effectiveRidingSpeed: 8.0,
  effectiveJumpSpeed: 8.0,

  // Dimensions
  eyeHeight: 1.6,
  dimensions: {
    walk: { height: 2.0, width: 0.6, footprint: 0.3 },
    sprint: { height: 2.0, width: 0.6, footprint: 0.3 },
    crouch: { height: 1.0, width: 0.6, footprint: 0.3 },
    swim: { height: 1.8, width: 0.6, footprint: 0.3 },
    climb: { height: 1.8, width: 0.6, footprint: 0.3 },
    fly: { height: 1.8, width: 0.6, footprint: 0.3 },
    teleport: { height: 1.8, width: 0.6, footprint: 0.3 },
  },

  // Stealth
  stealthRange: 8.0,

  // Camera control
  baseTurnSpeed: 0.003,
  effectiveTurnSpeed: 0.003,
  baseUnderwaterTurnSpeed: 0.002, // 33% slower underwater for realistic feel
  effectiveUnderwaterTurnSpeed: 0.002,

  // Third-person view
  thirdPersonModelId: 'farmer1', // Use farmer model for third-person view
};
