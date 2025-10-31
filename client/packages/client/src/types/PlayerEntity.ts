/**
 * PlayerEntity - Player as physics entity with player info
 *
 * Extends PhysicsEntity with PlayerInfo to store player-specific
 * properties that can change dynamically during gameplay.
 */

import type { PhysicsEntity } from '../services/PhysicsService';
import type { PlayerInfo } from '@nimbus/shared';

/**
 * Player entity with physics and player-specific properties
 *
 * Combines physics simulation (PhysicsEntity) with player configuration (PlayerInfo).
 * PlayerInfo values can be updated dynamically through power-ups, status effects, equipment, etc.
 */
export interface PlayerEntity extends PhysicsEntity {
  /** Player-specific configuration and properties */
  playerInfo: PlayerInfo;
}
