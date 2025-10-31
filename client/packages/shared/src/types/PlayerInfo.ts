/**
 * PlayerInfo - Player configuration and properties
 *
 * Contains all player-specific parameters that can change dynamically
 * during gameplay (e.g., through power-ups, status effects, equipment).
 *
 * **Base vs Effective Values:**
 * - Base values: Original unmodified values
 * - Effective values: Base + modifiers (power-ups, equipment, status effects)
 * - Services use effective values for actual gameplay
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
  // Base Movement Speeds (blocks per second)
  // Original unmodified values
  // ============================================

  /** Base normal walking speed */
  baseWalkSpeed: number;

  /** Base sprint/running speed */
  baseRunSpeed: number;

  /** Base swimming/underwater movement speed */
  baseUnderwaterSpeed: number;

  /** Base sneaking/crouching speed */
  baseCrawlSpeed: number;

  /** Base speed when riding a mount or vehicle */
  baseRidingSpeed: number;

  /** Base jump vertical velocity */
  baseJumpSpeed: number;

  // ============================================
  // Effective Movement Speeds (blocks per second)
  // Base + modifiers (power-ups, equipment, status effects)
  // These values are used by PhysicsService
  // ============================================

  /** Effective walking speed (base + modifiers) */
  effectiveWalkSpeed: number;

  /** Effective sprint/running speed (base + modifiers) */
  effectiveRunSpeed: number;

  /** Effective swimming/underwater speed (base + modifiers) */
  effectiveUnderwaterSpeed: number;

  /** Effective sneaking/crouching speed (base + modifiers) */
  effectiveCrawlSpeed: number;

  /** Effective riding speed (base + modifiers) */
  effectiveRidingSpeed: number;

  /** Effective jump vertical velocity (base + modifiers) */
  effectiveJumpSpeed: number;

  // ============================================
  // Player Dimensions
  // ============================================

  /** Player head/eye height in blocks (for camera position and raycast) */
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
