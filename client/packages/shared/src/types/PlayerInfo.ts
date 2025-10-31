/**
 * PlayerInfo - Player configuration and properties
 *
 * Contains all player-specific parameters that can change dynamically
 * during gameplay (e.g., through power-ups, status effects, equipment).
 *
 * Movement speeds are in blocks per second.
 */

/**
 * Player information and properties
 */
export interface PlayerInfo {
  /** Player unique identifier */
  playerId: string;

  /** Player display name (shown to other players) */
  displayName: string;

  // ============================================
  // Movement Speeds (blocks per second)
  // ============================================

  /** Normal walking speed */
  walkSpeed: number;

  /** Sprint/running speed (faster than walk) */
  runSpeed: number;

  /** Swimming/underwater movement speed */
  underwaterSpeed: number;

  /** Sneaking/crouching speed (slower, stealthy) */
  crawlSpeed: number;

  /** Speed when riding a mount or vehicle */
  ridingSpeed: number;

  // ============================================
  // Physics Properties
  // ============================================

  /** Jump vertical velocity (determines jump height with gravity) */
  jumpSpeed: number;

  // ============================================
  // Player Dimensions
  // ============================================

  /** Player head/eye height in blocks (for camera position) */
  headHeight: number;

  // ============================================
  // Stealth & Detection
  // ============================================

  /** Detection range for mobs when sneaking (in blocks) */
  stealthRange: number;

  // ============================================
  // Camera Control
  // ============================================

  /** Mouse sensitivity for camera rotation */
  turnSpeed: number;
}
