/**
 * Default PlayerInfo configuration
 *
 * These are the baseline values for player properties.
 * They can be overridden by:
 * - Server configuration
 * - Player equipment
 * - Status effects / power-ups
 * - World-specific settings
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
 * - Head height: 1.6 blocks (eye level for camera)
 */
export const DEFAULT_PLAYER_INFO: PlayerInfo = {
  // Identity
  playerId: 'local_player',
  displayName: 'Player',

  // Movement speeds (blocks per second)
  walkSpeed: 5.0,
  runSpeed: 7.0,
  underwaterSpeed: 3.0,
  crawlSpeed: 2.5,
  ridingSpeed: 8.0,

  // Physics
  jumpSpeed: 8.0,

  // Dimensions
  headHeight: 1.6,

  // Stealth
  stealthRange: 8.0,

  // Camera
  turnSpeed: 0.003,
};
