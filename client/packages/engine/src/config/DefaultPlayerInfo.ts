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
 *
 * Player IDs are generated after login from username and sessionId
 * Format: @{username}_{sessionId}
 */
export const DEFAULT_PLAYER_INFO: PlayerInfo = {
  // Identity (will be overwritten after login)
  playerId: '@player_temp', // Temporary ID, replaced after login
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
  distanceNotifyReductionWalk: 0, // No reduction when walking (default)
  distanceNotifyReductionCrouch: 0, // No reduction when crouching (default)

  // Selection & Interaction
  selectionRadius: 5.0, // Maximum range for auto-select and shortcuts (in blocks)

  // Camera control
  baseTurnSpeed: 0.003,
  effectiveTurnSpeed: 0.003,
  baseUnderwaterTurnSpeed: 0.002, // 33% slower underwater for realistic feel
  effectiveUnderwaterTurnSpeed: 0.002,

  // Third-person view
  thirdPersonModelId: 'wizard1', // Use farmer model for third-person view

  // State-based values (unified matrix for all state-dependent properties)
  stateValues: {
    walk: {
      dimensions: { height: 2.0, width: 0.6, footprint: 0.3 },
      baseMoveSpeed: 5.0,
      effectiveMoveSpeed: 5.0,
      baseJumpSpeed: 8.0,
      effectiveJumpSpeed: 8.0,
      eyeHeight: 1.6,
      baseTurnSpeed: 0.003,
      effectiveTurnSpeed: 0.003,
      selectionRadius: 5.0,
      stealthRange: 8.0,
      distanceNotifyReduction: 0,
    },
    sprint: {
      dimensions: { height: 2.0, width: 0.6, footprint: 0.3 },
      baseMoveSpeed: 7.0,       // baseRunSpeed
      effectiveMoveSpeed: 7.0,  // effectiveRunSpeed
      baseJumpSpeed: 8.0,
      effectiveJumpSpeed: 8.0,
      eyeHeight: 1.6,
      baseTurnSpeed: 0.003,
      effectiveTurnSpeed: 0.003,
      selectionRadius: 5.0,
      stealthRange: 12.0,       // More visible when sprinting
      distanceNotifyReduction: 0,
    },
    crouch: {
      dimensions: { height: 1.0, width: 0.6, footprint: 0.3 },
      baseMoveSpeed: 2.5,       // baseCrawlSpeed
      effectiveMoveSpeed: 2.5,  // effectiveCrawlSpeed
      baseJumpSpeed: 4.0,       // Lower jump
      effectiveJumpSpeed: 4.0,
      eyeHeight: 0.8,           // Lower eye height
      baseTurnSpeed: 0.002,     // Slower turn
      effectiveTurnSpeed: 0.002,
      selectionRadius: 4.0,     // Shorter reach
      stealthRange: 4.0,        // Stealthier
      distanceNotifyReduction: 0.5,
    },
    swim: {
      dimensions: { height: 1.8, width: 0.6, footprint: 0.3 },
      baseMoveSpeed: 3.0,       // baseUnderwaterSpeed
      effectiveMoveSpeed: 3.0,  // effectiveUnderwaterSpeed
      baseJumpSpeed: 4.0,
      effectiveJumpSpeed: 4.0,
      eyeHeight: 1.4,
      baseTurnSpeed: 0.002,     // baseUnderwaterTurnSpeed
      effectiveTurnSpeed: 0.002,
      selectionRadius: 4.0,
      stealthRange: 6.0,
      distanceNotifyReduction: 0.3,
    },
    climb: {
      dimensions: { height: 1.8, width: 0.6, footprint: 0.3 },
      baseMoveSpeed: 2.5,       // 0.5x walk (no hardcoded multiplier!)
      effectiveMoveSpeed: 2.5,
      baseJumpSpeed: 0.0,       // Can't jump while climbing
      effectiveJumpSpeed: 0.0,
      eyeHeight: 1.5,
      baseTurnSpeed: 0.002,
      effectiveTurnSpeed: 0.002,
      selectionRadius: 4.0,
      stealthRange: 6.0,
      distanceNotifyReduction: 0.2,
    },
    fly: {
      dimensions: { height: 1.8, width: 0.6, footprint: 0.3 },
      baseMoveSpeed: 10.0,      // 2x walk (no hardcoded multiplier!)
      effectiveMoveSpeed: 10.0,
      baseJumpSpeed: 0.0,       // No jumping in fly
      effectiveJumpSpeed: 0.0,
      eyeHeight: 1.6,
      baseTurnSpeed: 0.004,     // Faster turn in fly
      effectiveTurnSpeed: 0.004,
      selectionRadius: 8.0,     // Longer reach from air
      stealthRange: 15.0,       // Very visible
      distanceNotifyReduction: 0,
    },
    teleport: {
      dimensions: { height: 1.8, width: 0.6, footprint: 0.3 },
      baseMoveSpeed: 20.0,      // Very fast
      effectiveMoveSpeed: 20.0,
      baseJumpSpeed: 0.0,
      effectiveJumpSpeed: 0.0,
      eyeHeight: 1.6,
      baseTurnSpeed: 0.005,
      effectiveTurnSpeed: 0.005,
      selectionRadius: 10.0,
      stealthRange: 20.0,
      distanceNotifyReduction: 0,
    },
    riding: {
      dimensions: { height: 2.5, width: 1.0, footprint: 0.5 },
      baseMoveSpeed: 8.0,       // baseRidingSpeed
      effectiveMoveSpeed: 8.0,  // effectiveRidingSpeed
      baseJumpSpeed: 10.0,      // Mount can jump higher
      effectiveJumpSpeed: 10.0,
      eyeHeight: 2.0,           // Higher on mount
      baseTurnSpeed: 0.003,
      effectiveTurnSpeed: 0.003,
      selectionRadius: 6.0,
      stealthRange: 10.0,
      distanceNotifyReduction: 0,
    },
  },
};
