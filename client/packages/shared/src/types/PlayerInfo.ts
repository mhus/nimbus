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

  /** Player eye height in blocks (for camera position and raycast origin) */
  eyeHeight: number;

  /**
   * Entity dimensions per movement mode
   * - height: Entity height in blocks (collision box)
   * - width: Entity width in blocks (collision box diameter)
   * - footprint: Footprint radius in blocks (for corner sampling)
   */
  dimensions: {
    walk: { height: number; width: number; footprint: number };
    sprint: { height: number; width: number; footprint: number };
    crouch: { height: number; width: number; footprint: number };
    swim: { height: number; width: number; footprint: number };
    climb: { height: number; width: number; footprint: number };
    fly: { height: number; width: number; footprint: number };
    teleport: { height: number; width: number; footprint: number };
  };

  // ============================================
  // Stealth & Detection
  // ============================================

  /** Detection range for mobs when sneaking (in blocks) */
  stealthRange: number;

  // ============================================
  // Camera Control
  // ============================================

  /** Base mouse sensitivity for camera rotation (on land) */
  baseTurnSpeed: number;

  /** Effective mouse sensitivity (base + modifiers, e.g., dizzy effects) */
  effectiveTurnSpeed: number;

  /** Base mouse sensitivity for camera rotation (underwater) */
  baseUnderwaterTurnSpeed: number;

  /** Effective underwater mouse sensitivity (base + modifiers) */
  effectiveUnderwaterTurnSpeed: number;
}
