/**
 * PhysicsService - Physics simulation for entities
 *
 * Handles movement physics for locally managed entities (player, etc.).
 * Supports two movement modes: Walk and Fly.
 *
 * For PlayerEntity, uses dynamic values from PlayerInfo.
 * For other entities, uses default physics constants.
 */

import { Vector3 } from '@babylonjs/core';
import { getLogger, Direction, DirectionHelper } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { ChunkService } from './ChunkService';
import type { PlayerEntity } from '../types/PlayerEntity';
import type { ClientBlock } from '../types/ClientBlock';

const logger = getLogger('PhysicsService');

/**
 * Movement mode
 */
export type MovementMode = 'walk' | 'fly';

/**
 * Entity with physics
 */
export interface PhysicsEntity {
  /** Entity position */
  position: Vector3;

  /** Entity velocity */
  velocity: Vector3;

  /** Entity rotation (Euler angles in radians) */
  rotation: Vector3;

  /** Movement mode */
  movementMode: MovementMode;

  /** Is entity on ground? (Walk mode only) */
  isOnGround: boolean;

  /** Entity ID for logging */
  entityId: string;

  /** Auto-climb state (for smooth animation) */
  climbState?: {
    active: boolean;
    startY: number;
    targetY: number;
    startX: number;
    targetX: number;
    startZ: number;
    targetZ: number;
    progress: number; // 0.0 to 1.0
  };
}

/**
 * Block context around player
 * Contains all relevant blocks and their aggregated physics properties
 */
interface PlayerBlockContext {
  /** Blocks at feet level (current Y) */
  currentLevel: {
    blocks: Array<{ x: number; y: number; z: number; block: any }>;
    hasSolid: boolean;
    hasAutoClimbable: boolean;
    hasAutoJump: boolean;
    climbableSpeed: number; // Climb speed when moving forward into this block (0 = not climbable)
    resistance: number; // Max resistance from all blocks
    autoMove: { x: number; y: number; z: number }; // Max velocity per axis
    autoOrientationY: number | undefined; // Last (most recent) orientation from blocks
    passableFrom: number | undefined; // Collected passableFrom from blocks (OR combined)
  };

  /** Blocks one level above (Y + 1) */
  aboveLevel: {
    blocks: Array<{ x: number; y: number; z: number; block: any }>;
    hasSolid: boolean;
    isClear: boolean; // All positions are free
  };

  /** Blocks below (Y - 1, Y - 2, Y - 3 for ground detection) */
  belowLevel: {
    blocks: Array<{ x: number; y: number; z: number; block: any }>;
    hasGround: boolean;
    hasAutoJump: boolean; // AutoJump on block player is standing on
    autoMove: { x: number; y: number; z: number }; // Max velocity per axis
    autoOrientationY: number | undefined; // Last (most recent) orientation from blocks
    groundY: number; // Y coordinate of ground, -1 if no ground
  };

  /** Block player is standing in (occupies player's body space) */
  occupiedBlocks: {
    blocks: Array<{ x: number; y: number; z: number; block: any }>;
    hasSolid: boolean;
    hasLiquid: boolean; // For future water/lava detection
    hasAutoJump: boolean; // Trigger auto-jump when inside this block
    autoMove: { x: number; y: number; z: number }; // Max velocity per axis
  };
}

/**
 * Surface state analysis result
 * Contains all surface properties at current entity position
 */
interface SurfaceState {
  /** Type of surface player is on/in */
  type: 'flat' | 'slope' | 'none' | 'climbing';

  /** Interpolated surface height at current position */
  surfaceHeight: number;

  /** Corner heights if surface is a slope */
  cornerHeights?: [number, number, number, number];

  /** Slope vector (0 if flat) */
  slope: { x: number; z: number };

  /** Movement resistance (0 = no resistance, 1 = full resistance) */
  resistance: number;

  /** Can player walk on this surface? */
  canWalkOn: boolean;

  /** Is surface solid? */
  isSolid: boolean;

  /** Block context (cached) */
  context: PlayerBlockContext;
}

/**
 * Accumulated forces acting on entity
 */
interface ForceState {
  /** Gravity force (Y-axis) */
  gravity: { x: 0; y: number; z: 0 };

  /** Player input force (movement) */
  input: { x: number; y: number; z: number };

  /** Slope sliding force */
  slope: { x: number; y: number; z: number };

  /** AutoMove force (conveyors, currents) */
  autoMove: { x: number; y: number; z: number };

  /** Climbable force (ladders) */
  climb: { x: 0; y: number; z: 0 };

  /** Combined total force */
  total: { x: number; y: number; z: number };

  /** Should gravity be applied? */
  applyGravity: boolean;

  /** Is entity climbing? */
  isClimbing: boolean;
}

/**
 * Type guard to check if entity is a PlayerEntity
 */
function isPlayerEntity(entity: PhysicsEntity): entity is PlayerEntity {
  return 'playerInfo' in entity;
}

/**
 * PhysicsService - Manages physics for entities
 *
 * Features:
 * - Walk mode: XZ movement, gravity, jumping, auto-climb
 * - Fly mode: Full 3D movement, no gravity
 * - Underwater mode: Fly-like movement with gravity disabled, collisions enabled
 * - Block collision detection
 * - Auto-push-up when inside block
 * - Water detection from ClientHeightData
 *
 * For PlayerEntity: Uses dynamic values from PlayerInfo
 * For other entities: Uses default physics constants
 */
export class PhysicsService {
  private appContext: AppContext;
  private chunkService?: ChunkService;

  // Physics constants (global, not player-specific)
  private readonly gravity: number = -20.0; // blocks per second²
  private readonly underwaterGravity: number = -2.0; // blocks per second² (10% of normal, slow sinking)

  // Default values for non-player entities
  private readonly defaultWalkSpeed: number = 5.0; // blocks per second
  private readonly defaultFlySpeed: number = 10.0; // blocks per second
  private readonly defaultJumpSpeed: number = 8.0; // blocks per second
  private readonly defaultEntityHeight: number = 1.8; // Entity height in blocks
  private readonly defaultEntityWidth: number = 0.6; // Entity width in blocks
  private readonly defaultUnderwaterSpeed: number = 3.0; // blocks per second

  // Entities managed by physics
  private entities: Map<string, PhysicsEntity> = new Map();

  // Underwater state (TODO: Track per entity instead of global)
  private isUnderwater: boolean = false;

  // Last checked block coordinates for underwater detection (optimization)
  // Only check underwater state when block coordinates change
  private lastCheckedBlockCoords: Map<string, { blockX: number; blockY: number; blockZ: number }> = new Map();

  // Physics enabled state - prevents falling through world on initial load
  // Physics is paused until initial chunks around player are loaded
  private physicsEnabled: boolean = false;

  // Teleportation pending state - disables physics while waiting for chunks
  // Used when player is teleported to a new location
  private teleportationPending: boolean = false;
  private teleportCheckTimer: NodeJS.Timeout | null = null;
  private teleportTarget: { x: number; y: number; z: number } | null = null;

  // Track if climbable velocity was set this frame (before updateWalkMode runs)
  private climbableVelocitySetThisFrame: Map<string, boolean> = new Map();

  constructor(appContext: AppContext) {
    this.appContext = appContext;

    logger.info('PhysicsService initialized', {
      gravity: this.gravity,
      underwaterGravity: this.underwaterGravity,
      defaultWalkSpeed: this.defaultWalkSpeed,
      defaultFlySpeed: this.defaultFlySpeed,
      defaultUnderwaterSpeed: this.defaultUnderwaterSpeed,
    });
  }

  /**
   * Set ChunkService for collision detection (called after ChunkService is created)
   */
  setChunkService(chunkService: ChunkService): void {
    this.chunkService = chunkService;
    logger.debug('ChunkService set for collision detection');
  }

  /**
   * Register an entity for physics simulation
   */
  registerEntity(entity: PhysicsEntity): void {
    this.entities.set(entity.entityId, entity);

    // Clamp to world bounds on registration (in case entity spawns outside)
    this.clampToWorldBounds(entity);

    logger.debug('Entity registered', { entityId: entity.entityId, mode: entity.movementMode });
  }

  /**
   * Unregister an entity
   */
  unregisterEntity(entityId: string): void {
    this.entities.delete(entityId);
    this.lastCheckedBlockCoords.delete(entityId); // Clean up block coordinate tracking
    logger.debug('Entity unregistered', { entityId });
  }

  /**
   * Get an entity
   */
  getEntity(entityId: string): PhysicsEntity | undefined {
    return this.entities.get(entityId);
  }

  /**
   * Enable physics simulation
   *
   * Call this after initial chunks are loaded to prevent falling through the world
   */
  enablePhysics(): void {
    if (!this.physicsEnabled) {
      this.physicsEnabled = true;
      logger.info('Physics enabled - initial chunks loaded');
    }
  }

  /**
   * Disable physics simulation
   */
  disablePhysics(): void {
    this.physicsEnabled = false;
    logger.info('Physics disabled');
  }

  /**
   * Check if physics is enabled
   */
  isPhysicsEnabled(): boolean {
    return this.physicsEnabled;
  }

  /**
   * Teleport entity to specific block coordinates
   * Waits for chunks to be loaded before positioning
   *
   * @param entity Entity to teleport
   * @param blockX Target block X coordinate (integer)
   * @param blockY Target block Y coordinate (integer)
   * @param blockZ Target block Z coordinate (integer)
   */
  teleport(entity: PhysicsEntity, blockX: number, blockY: number, blockZ: number): void {
    // Convert block coordinates to position (center of block)
    // Block N contains positions from N.0 to (N+1).0
    // Center is at N + 0.5
    const posX = Math.floor(blockX) + 0.5;
    const posY = Math.floor(blockY) + 0.5;
    const posZ = Math.floor(blockZ) + 0.5;

    // Store target position
    this.teleportTarget = { x: posX, y: posY, z: posZ };

    // Position player at target XZ, high Y for falling
    entity.position.x = posX;
    entity.position.z = posZ;
    entity.position.y = 200;
    entity.velocity.set(0, 0, 0);

    // Start teleportation mode
    this.startTeleportation(entity.entityId);

    logger.info('Teleport initiated', {
      entityId: entity.entityId,
      blockCoords: { x: blockX, y: blockY, z: blockZ },
      position: this.teleportTarget,
    });
  }

  /**
   * Start teleportation pending mode
   *
   * Disables physics and starts a timer to check for chunk/heightData availability
   * Once ready, positions player at groundLevel + 4 and re-enables physics
   *
   * @param entityId Entity to teleport (usually player)
   */
  startTeleportation(entityId: string): void {
    this.teleportationPending = true;
    this.physicsEnabled = false;

    // If no teleportTarget set (initial spawn), set player to height 200
    const entity = this.entities.get(entityId);
    if (entity && !this.teleportTarget) {
      entity.position.y = 200;
      entity.velocity.y = 0; // Reset velocity
      logger.info('Player positioned at Y=200 for spawn');
    }

    // Clear existing timer if any
    if (this.teleportCheckTimer) {
      clearInterval(this.teleportCheckTimer);
    }

    logger.info('Teleportation pending - physics disabled, starting chunk check timer');

    // Check every 1 second if chunk and heightData are ready
    this.teleportCheckTimer = setInterval(() => {
      this.checkTeleportationReady(entityId);
    }, 1000);

    // Also check immediately
    this.checkTeleportationReady(entityId);
  }

  /**
   * Check if teleportation target is ready (chunk + heightData + blocks exist)
   */
  private checkTeleportationReady(entityId: string): void {
    const entity = this.entities.get(entityId);
    if (!entity) {
      logger.warn('Entity not found for teleportation check', { entityId });
      this.cancelTeleportation();
      return;
    }

    // Get chunk service
    if (!this.chunkService) {
      logger.warn('ChunkService not available for teleportation check');
      return;
    }

    const chunkSize = this.appContext.worldInfo?.chunkSize || 16;
    const chunkX = Math.floor(entity.position.x / chunkSize);
    const chunkZ = Math.floor(entity.position.z / chunkSize);

    // Check if chunk is loaded
    const chunk = this.chunkService.getChunk(chunkX, chunkZ);
    if (!chunk) {
      logger.debug('Waiting for chunk to load', { chunkX, chunkZ });
      return;
    }

    // Calculate local coordinates
    const localX = ((Math.floor(entity.position.x) % chunkSize) + chunkSize) % chunkSize;
    const localZ = ((Math.floor(entity.position.z) % chunkSize) + chunkSize) % chunkSize;
    const heightKey = `${localX},${localZ}`;

    // Check if heightData exists
    const heightData = chunk.data.hightData.get(heightKey);
    if (!heightData) {
      logger.debug('Waiting for heightData', { localX, localZ });
      return;
    }

    const [x, z, maxHeight, minHeight, groundLevel, waterHeight] = heightData;

    // Everything is ready - position player and enable physics
    const oldY = entity.position.y;
    // TODO: Reduce offset from 20 to 4 once groundLevel calculation is more accurate
    const targetY = groundLevel + 20;

    logger.info('Teleportation ready - positioning player', {
      entityId,
      oldY,
      targetY,
      groundLevel,
      maxHeight,
      minHeight,
      playerX: entity.position.x,
      playerZ: entity.position.z,
      localX,
      localZ,
    });

    entity.position.y = targetY;
    entity.velocity.y = 0; // Reset vertical velocity

    logger.info('Teleportation complete - player positioned', {
      entityId,
      newY: entity.position.y,
    });

    // Clear timer and re-enable physics
    this.cancelTeleportation();
    this.physicsEnabled = true;
    logger.info('Physics re-enabled after teleportation');
  }

  /**
   * Cancel teleportation pending mode
   */
  private cancelTeleportation(): void {
    this.teleportationPending = false;
    this.teleportTarget = null; // Clear target

    if (this.teleportCheckTimer) {
      clearInterval(this.teleportCheckTimer);
      this.teleportCheckTimer = null;
    }
  }

  /**
   * Update all entities
   */
  update(deltaTime: number): void {
    // Skip physics simulation if not enabled or teleportation pending
    if (!this.physicsEnabled || this.teleportationPending) {
      return;
    }

    for (const entity of this.entities.values()) {
      this.updateEntity(entity, deltaTime);
    }

    // Clear climbable flags AFTER updating entities
    // Flags will be checked in the NEXT frame's updateWalkMode
    this.climbableVelocitySetThisFrame.clear();
  }

  /**
   * Update a single entity
   */
  private updateEntity(entity: PhysicsEntity, deltaTime: number): void {
    // Check underwater state only if position changed (optimization)
    this.checkUnderwaterStateIfMoved(entity);

    if (entity.movementMode === 'walk') {
      this.updateWalkMode(entity, deltaTime);
    } else if (entity.movementMode === 'fly') {
      this.updateFlyMode(entity, deltaTime);
    }
  }

  /**
   * Check if entity block coordinates have changed and run underwater check
   *
   * Optimization: Only check underwater state when entity moves to a different block
   * to avoid expensive chunk lookups every frame.
   *
   * Since waterHeight is per block column, we only need to check when:
   * - Block X coordinate changes (Math.floor(x))
   * - Block Y coordinate changes (Math.floor(y))
   * - Block Z coordinate changes (Math.floor(z))
   *
   * @param entity Entity to check
   */
  private checkUnderwaterStateIfMoved(entity: PhysicsEntity): void {
    const currentBlockX = Math.floor(entity.position.x);
    const currentBlockY = Math.floor(entity.position.y);
    const currentBlockZ = Math.floor(entity.position.z);

    const lastCoords = this.lastCheckedBlockCoords.get(entity.entityId);

    // Check if block coordinates changed
    const blockChanged = !lastCoords ||
      currentBlockX !== lastCoords.blockX ||
      currentBlockY !== lastCoords.blockY ||
      currentBlockZ !== lastCoords.blockZ;

    if (blockChanged) {
      // Block changed - run underwater check
      this.checkUnderwaterState(entity);

      // Update last checked block coordinates
      this.lastCheckedBlockCoords.set(entity.entityId, {
        blockX: currentBlockX,
        blockY: currentBlockY,
        blockZ: currentBlockZ,
      });
    }
  }

  /**
   * Check if entity is underwater based on ClientHeightData
   *
   * Gets ClientHeightData for entity's column and:
   * - Checks if entity.position.y < waterHeight
   * - Calls CameraService.setUnderwater(true/false) on state change
   * - Uses minHeight/maxHeight as boundaries for physics
   *
   * @param entity Entity to check
   */
  private checkUnderwaterState(entity: PhysicsEntity): void {
    if (!this.chunkService) {
      logger.warn('💧 ChunkService not available!', { entityId: entity.entityId });
      return;
    }

    const chunkSize = this.appContext.worldInfo?.chunkSize || 16;
    const chunkX = Math.floor(entity.position.x / chunkSize);
    const chunkZ = Math.floor(entity.position.z / chunkSize);
    const chunk = this.chunkService.getChunk(chunkX, chunkZ);

    if (!chunk) {
      return;
    }

    // Calculate local coordinates within chunk (handle negative positions correctly)
    const localX = ((entity.position.x % chunkSize) + chunkSize) % chunkSize;
    const localZ = ((entity.position.z % chunkSize) + chunkSize) % chunkSize;
    const heightKey = `${Math.floor(localX)},${Math.floor(localZ)}`;
    const heightData = chunk.data.hightData.get(heightKey);

    if (heightData && heightData[5] !== undefined) {
      const [x, z, maxHeight, minHeight, groundLevel, waterHeight] = heightData;
      const wasUnderwater = this.isUnderwater;

      // Player is underwater when camera/eyes are below water surface
      // waterHeight = Y of highest water block (bottom face = surface)
      // Add offset so player is "underwater" when partially submerged
      const waterSurfaceY = waterHeight + 1.0; // Raise surface by 1.0 blocks
      const eyeY = entity.position.y + 1.6; // Eye level for first-person camera
      this.isUnderwater = eyeY <= waterSurfaceY;

      // Notify CameraService on state change
      if (wasUnderwater !== this.isUnderwater) {
        const cameraService = this.appContext.services.camera;
        if (cameraService) {
          cameraService.setUnderwater(this.isUnderwater);
          logger.info('💧 UNDERWATER STATE CHANGED', {
            entityId: entity.entityId,
            underwater: this.isUnderwater,
            waterBlockY: waterHeight,
            waterSurfaceY: waterSurfaceY,
            entityY: entity.position.y.toFixed(2),
            eyeY: eyeY.toFixed(2),
          });
        }
      }

      // Clamp to min/max height boundaries
      if (entity.position.y < minHeight) {
        entity.position.y = minHeight;
        entity.velocity.y = 0;
      } else if (entity.position.y > maxHeight) {
        entity.position.y = maxHeight;
        entity.velocity.y = 0;
      }
    } else {
      // No waterHeight data - definitely not underwater!
      const wasUnderwater = this.isUnderwater;
      this.isUnderwater = false;

      // Notify CameraService if state changed from underwater to not underwater
      if (wasUnderwater) {
        const cameraService = this.appContext.services.camera;
        if (cameraService) {
          cameraService.setUnderwater(false);
          logger.info('💧 Left water area', { entityId: entity.entityId });
        }
      }
    }
  }

  /**
   * Update entity in Walk mode
   *
   * When underwater, physics behave like fly mode (no gravity)
   * but with collision detection enabled.
   */
  private updateWalkMode(entity: PhysicsEntity, deltaTime: number): void {
    // Handle auto-climb animation
    if (entity.climbState?.active) {
      this.updateClimbAnimation(entity, deltaTime);
      return; // Skip normal physics during climb
    }

    // === PHASE 1: ANALYZE SURFACE ===
    const surface = this.analyzeSurface(entity);

    // === PHASE 2: CALCULATE FORCES ===
    const forces = this.calculateForces(entity, surface, entity.velocity, deltaTime);

    // === PHASE 3: RESOLVE MOVEMENT ===
    this.resolveMovement(entity, forces, surface, deltaTime);

    // === PHASE 4: POST-MOVEMENT ADJUSTMENTS ===

    // Clamp to world boundaries
    this.clampToWorldBounds(entity);

    // Ground check with block collision
    this.checkGroundCollision(entity);

    // Auto-push-up if inside block
    this.checkAndPushUp(entity);

    // Auto-jump if standing on/in autoJump block
    this.checkAutoJump(entity);

    // Auto-orientation if standing on/in autoOrientationY block
    this.applyAutoOrientation(entity, deltaTime);
  }

  /**
   * Get climbable speed if player is currently in a climbable block
   * Returns 0 if not in climbable block
   */
  private getPlayerClimbableSpeed(entity: PhysicsEntity): number {
    if (!this.chunkService) return 0;

    const pos = entity.position;
    const context = this.getPlayerBlockContext(
      pos.x,
      pos.z,
      pos.y,
      this.defaultEntityWidth,
      this.defaultEntityHeight
    );

    return context.currentLevel.climbableSpeed;
  }

  /**
   * Update smooth climb animation
   *
   * Interpolates position diagonally from start to target (X, Y, Z simultaneously)
   * for a smooth climbing motion that moves forward and upward at the same time.
   * Climb speed is based on entity's walk speed for consistent movement feel.
   */
  private updateClimbAnimation(entity: PhysicsEntity, deltaTime: number): void {
    if (!entity.climbState) return;

    // Calculate total distance to climb (diagonal distance)
    const dx = entity.climbState.targetX - entity.climbState.startX;
    const dy = entity.climbState.targetY - entity.climbState.startY;
    const dz = entity.climbState.targetZ - entity.climbState.startZ;
    const totalDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);

    // Use walk speed to determine climb speed (blocks per second)
    const climbSpeed = this.getMoveSpeed(entity);

    // Calculate progress based on speed and distance
    // progress per frame = (speed * deltaTime) / totalDistance
    const progressIncrement = totalDistance > 0 ? (climbSpeed * deltaTime) / totalDistance : 1.0;
    entity.climbState.progress += progressIncrement;

    if (entity.climbState.progress >= 1.0) {
      // Climb complete - snap to final position
      entity.position.x = entity.climbState.targetX;
      entity.position.y = entity.climbState.targetY;
      entity.position.z = entity.climbState.targetZ;
      entity.climbState = undefined;
      entity.isOnGround = true;
      entity.velocity.y = 0;
    } else {
      // Smooth, linear interpolation for steady climbing motion
      // Move diagonally: X, Y, Z all interpolate simultaneously
      const t = entity.climbState.progress;

      entity.position.x = entity.climbState.startX + dx * t;
      entity.position.y = entity.climbState.startY + dy * t;
      entity.position.z = entity.climbState.startZ + dz * t;
    }
  }

  /**
   * Determine the primary movement direction from delta X and Z
   * Returns the dominant horizontal direction based on movement vector
   *
   * @param dx Delta X movement
   * @param dz Delta Z movement
   * @returns Primary direction of movement (NORTH, SOUTH, EAST, WEST)
   */
  private getMovementDirection(dx: number, dz: number): Direction {
    // Determine which axis has larger movement
    const absDx = Math.abs(dx);
    const absDz = Math.abs(dz);

    if (absDx > absDz) {
      // X-axis dominant
      return dx > 0 ? Direction.EAST : Direction.WEST;
    } else {
      // Z-axis dominant (or equal, prefer Z)
      return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
  }

  /**
   * Check if we can enter a block from a specific direction
   *
   * Moving from WEST to EAST:
   * - canEnterFrom(targetBlock, WEST) - entering from the WEST side
   *
   * @param passableFrom Direction bitfield from block (which sides are passable)
   * @param entrySide Which side of the block we're entering from
   * @param isSolid Whether the block is solid
   * @returns true if entry is allowed, false if blocked
   */
  private canEnterFrom(passableFrom: number | undefined, entrySide: Direction, isSolid: boolean): boolean {
    // No passableFrom set - use default behavior
    if (passableFrom === undefined || passableFrom === 0) {
      return !isSolid; // Solid blocks block, non-solid blocks allow
    }

    // Check if the entry side is passable
    return DirectionHelper.hasDirection(passableFrom, entrySide);
  }

  /**
   * Check if we can leave a block towards a specific direction
   *
   * Moving WEST: We exit through the WEST side
   * - canLeaveTo(sourceBlock, WEST) checks if WEST side is passable
   *
   * Example: passableFrom = ALL but WEST
   * - canLeaveTo(block, WEST) = FALSE (WEST not in passableFrom)
   * - canLeaveTo(block, EAST) = TRUE (EAST in passableFrom)
   *
   * @param passableFrom Direction bitfield from block (which sides are passable)
   * @param exitDir Which direction we're moving towards (which side we're exiting through)
   * @param isSolid Whether the block is solid
   * @returns true if exit is allowed, false if blocked
   */
  private canLeaveTo(passableFrom: number | undefined, exitDir: Direction, isSolid: boolean): boolean {
    // No passableFrom set - always allow exit
    if (passableFrom === undefined || passableFrom === 0) {
      return true;
    }

    // Check if the exit direction/side is passable
    return DirectionHelper.hasDirection(passableFrom, exitDir);
  }

  /**
   * Check if chunk at position is loaded
   */
  private isChunkLoaded(worldX: number, worldZ: number): boolean {
    if (!this.chunkService) {
      return true; // If no chunk service, allow movement
    }

    const chunkSize = this.appContext.worldInfo?.chunkSize || 16;
    const chunkX = Math.floor(worldX / chunkSize);
    const chunkZ = Math.floor(worldZ / chunkSize);

    const chunk = this.chunkService.getChunk(chunkX, chunkZ);
    return chunk !== undefined;
  }

  /**
   * Clamp entity to loaded chunk boundaries
   * Prevents player from moving into unloaded chunks
   */
  private clampToLoadedChunks(entity: PhysicsEntity, oldX: number, oldZ: number): void {
    if (!this.chunkService) {
      return;
    }

    // Check if new position is in a loaded chunk
    if (!this.isChunkLoaded(entity.position.x, entity.position.z)) {
      // Not loaded - revert position
      entity.position.x = oldX;
      entity.position.z = oldZ;
      entity.velocity.x = 0;
      entity.velocity.z = 0;

      logger.debug('Movement blocked - chunk not loaded', {
        entityId: entity.entityId,
        attemptedX: entity.position.x,
        attemptedZ: entity.position.z,
      });
    }
  }

  /**
   * Clamp entity position to world boundaries
   */
  private clampToWorldBounds(entity: PhysicsEntity): void {
    const worldInfo = this.appContext.worldInfo;
    if (!worldInfo || !worldInfo.start || !worldInfo.stop) {
      return;
    }

    const start = worldInfo.start;
    const stop = worldInfo.stop;
    let clamped = false;

    // Clamp X
    if (entity.position.x < start.x) {
      entity.position.x = start.x;
      entity.velocity.x = 0;
      clamped = true;
    } else if (entity.position.x > stop.x) {
      entity.position.x = stop.x;
      entity.velocity.x = 0;
      clamped = true;
    }

    // Clamp Y
    if (entity.position.y < start.y) {
      entity.position.y = start.y;
      entity.velocity.y = 0;
      clamped = true;
    } else if (entity.position.y > stop.y) {
      entity.position.y = stop.y;
      entity.velocity.y = 0;
      clamped = true;
    }

    // Clamp Z
    if (entity.position.z < start.z) {
      entity.position.z = start.z;
      entity.velocity.z = 0;
      clamped = true;
    } else if (entity.position.z > stop.z) {
      entity.position.z = stop.z;
      entity.velocity.z = 0;
      clamped = true;
    }

    if (clamped) {
      logger.debug('Entity clamped to world bounds', {
        entityId: entity.entityId,
        position: { x: entity.position.x, y: entity.position.y, z: entity.position.z },
        bounds: { start, stop },
      });
    }
  }

  /**
   * Check if block at position is solid
   */
  private isBlockSolid(x: number, y: number, z: number): boolean {
    if (!this.chunkService) {
      return false;
    }

    // Floor coordinates to get block position
    const blockX = Math.floor(x);
    const blockY = Math.floor(y);
    const blockZ = Math.floor(z);

    const clientBlock = this.chunkService.getBlockAt(blockX, blockY, blockZ);
    if (!clientBlock) {
      return false; // No block = not solid
    }

    // Direct access to pre-merged modifier (much faster!)
    return clientBlock.currentModifier.physics?.solid === true;
  }

  /**
   * Get corner heights for a block using priority cascade.
   *
   * Priority order:
   * 1. Block.cornerHeights (highest priority)
   * 2. PhysicsModifier.cornerHeights
   * 3. Auto-derived from offsets (if autoCornerHeights=true)
   *    - Block.offsets (Y values of top 4 corners)
   *    - VisibilityModifier.offsets
   * 4. undefined (no corner heights)
   *
   * @param block ClientBlock to get corner heights from
   * @returns Corner heights array [NW, NE, SE, SW] or undefined
   */
  private getCornerHeights(block: ClientBlock): [number, number, number, number] | undefined {
    let cornerHeights: [number, number, number, number] | undefined;
    let source = 'none';

    // Priority 1: Block.cornerHeights (highest)
    if (block.block.cornerHeights && block.block.cornerHeights.length === 4) {
      cornerHeights = block.block.cornerHeights;
      source = 'block.cornerHeights';
    }
    // Priority 2: PhysicsModifier.cornerHeights
    else if (block.currentModifier.physics?.cornerHeights && block.currentModifier.physics.cornerHeights.length === 4) {
      cornerHeights = block.currentModifier.physics.cornerHeights;
      source = 'physics.cornerHeights';
    }
    // Priority 3: Auto-derive from offsets (if autoCornerHeights=true)
    else if (block.currentModifier.physics?.autoCornerHeights) {
      console.log('🔎 [getCornerHeights] autoCornerHeights=true, checking offsets',
        '\n  blockPos:', JSON.stringify(block.block.position),
        '\n  hasBlockOffsets:', !!block.block.offsets,
        '\n  blockOffsetsLength:', block.block.offsets?.length,
        '\n  hasVisibilityOffsets:', !!block.currentModifier.visibility?.offsets,
        '\n  visibilityOffsetsLength:', block.currentModifier.visibility?.offsets?.length
      );

      // Try Block.offsets first
      // Note: Array may be shorter than 24 due to trailing null optimization
      if (block.block.offsets && block.block.offsets.length > 0) {
        // Extract Y-offsets from top 4 corners
        // Offsets order: [bottom 4 corners (0-11), top 4 corners (12-23)]
        // Top corners: [4]=SW(12-14), [5]=SE(15-17), [6]=NW(18-20), [7]=NE(21-23)
        // cornerHeights order: [NW, NE, SE, SW]
        const yNW = block.block.offsets[19] ?? 0; // Corner 6 Y
        const yNE = block.block.offsets[22] ?? 0; // Corner 7 Y
        const ySE = block.block.offsets[16] ?? 0; // Corner 5 Y
        const ySW = block.block.offsets[13] ?? 0; // Corner 4 Y
        cornerHeights = [yNW, yNE, ySE, ySW];
        source = 'block.offsets (auto-derived)';

          console.log('🔍 [getCornerHeights] AUTO-DERIVED from block.offsets:',
          '\n  blockPos:', JSON.stringify(block.block.position),
          '\n  offsets:', JSON.stringify(block.block.offsets),
          '\n  indices: 13(SW-Y)=' + block.block.offsets[13] + ', 16(SE-Y)=' + block.block.offsets[16] +
          ', 19(NW-Y)=' + (block.block.offsets[19] ?? 0) + ', 22(NE-Y)=' + (block.block.offsets[22] ?? 0),
          '\n  cornerHeights:', JSON.stringify(cornerHeights)
        );
      }
      // Try VisibilityModifier.offsets
      else if (block.currentModifier.visibility?.offsets && block.currentModifier.visibility.offsets.length > 0) {
        const offsets = block.currentModifier.visibility.offsets;
        const yNW = offsets[19] ?? 0;
        const yNE = offsets[22] ?? 0;
        const ySE = offsets[16] ?? 0;
        const ySW = offsets[13] ?? 0;
        cornerHeights = [yNW, yNE, ySE, ySW];
        source = 'visibility.offsets (auto-derived)';
      }
    }

    if (cornerHeights) {
      console.log('✅ [getCornerHeights] Corner heights FOUND!',
        '\n  blockPos:', JSON.stringify(block.block.position),
        '\n  source:', source,
        '\n  cornerHeights:', JSON.stringify(cornerHeights)
      );
    } else if (block.currentModifier.physics?.autoCornerHeights ||
               block.block.cornerHeights ||
               block.currentModifier.physics?.cornerHeights) {
      // Only log if block was EXPECTED to have cornerHeights
      console.log('❌ [getCornerHeights] NO corner heights despite config',
        '\n  blockPos:', JSON.stringify(block.block.position),
        '\n  autoCornerHeights:', block.currentModifier.physics?.autoCornerHeights,
        '\n  hasBlockOffsets:', !!block.block.offsets,
        '\n  blockOffsetsLength:', block.block.offsets?.length,
        '\n  hasPhysicsCornerHeights:', !!block.currentModifier.physics?.cornerHeights
      );
    }

    return cornerHeights;
  }

  /**
   * Calculate interpolated surface height at a position within a block.
   * Uses bilinear interpolation between the four corner heights.
   *
   * @param block ClientBlock with potential cornerHeights
   * @param worldX World X position (can be fractional)
   * @param worldZ World Z position (can be fractional)
   * @returns Interpolated Y position of the block surface at this location
   */
  private getBlockSurfaceHeight(block: ClientBlock, worldX: number, worldZ: number): number {
    const cornerHeights = this.getCornerHeights(block);

    // No corner heights found - use standard block top
    if (!cornerHeights) {
      return block.block.position.y + 1.0;
    }

    // Calculate local coordinates within the block (0.0 to 1.0)
    const localX = worldX - block.block.position.x;
    const localZ = worldZ - block.block.position.z;

    // Bilinear interpolation between 4 corners
    // Corner indices: [0]=NW, [1]=NE, [2]=SE, [3]=SW
    // NW(-X,-Z)  NE(+X,-Z)
    //     [0]--------[1]
    //      |          |
    //      |    *     |  <- interpolation point
    //      |          |
    //     [3]--------[2]
    // SW(-X,+Z)  SE(+X,+Z)

    const heightNW = cornerHeights[0]; // North-West
    const heightNE = cornerHeights[1]; // North-East
    const heightSE = cornerHeights[2]; // South-East
    const heightSW = cornerHeights[3]; // South-West

    // Interpolate along North edge (Z = 0) between NW and NE
    const heightNorth = heightNW + (heightNE - heightNW) * localX;

    // Interpolate along South edge (Z = 1) between SW and SE
    const heightSouth = heightSW + (heightSE - heightSW) * localX;

    // Interpolate between North and South edges
    const interpolatedHeight = heightNorth + (heightSouth - heightNorth) * localZ;

    return block.block.position.y + 1.0 + interpolatedHeight;
  }

  /**
   * Analyze surface state at entity position.
   * Determines surface type, height, slope, and walkability.
   *
   * This is the central function for all slope/surface-related physics.
   * Called once per frame to cache surface properties.
   *
   * @param entity Entity to analyze surface for
   * @returns SurfaceState with all surface properties
   */
  private analyzeSurface(entity: PhysicsEntity): SurfaceState {
    if (!this.chunkService) {
      // No chunk service - default state
      return {
        type: 'none',
        surfaceHeight: entity.position.y,
        slope: { x: 0, z: 0 },
        resistance: 0,
        canWalkOn: false,
        isSolid: false,
        context: {
          currentLevel: { blocks: [], hasSolid: false, hasAutoClimbable: false, hasAutoJump: false, climbableSpeed: 0, resistance: 0, autoMove: { x: 0, y: 0, z: 0 }, autoOrientationY: undefined, passableFrom: undefined },
          aboveLevel: { blocks: [], hasSolid: false, isClear: true },
          belowLevel: { blocks: [], hasGround: false, hasAutoJump: false, autoMove: { x: 0, y: 0, z: 0 }, autoOrientationY: undefined, groundY: -1 },
          occupiedBlocks: { blocks: [], hasSolid: false, hasLiquid: false, hasAutoJump: false, autoMove: { x: 0, y: 0, z: 0 } },
        },
      };
    }

    // Get entity dimensions
    const entityWidth = this.defaultEntityWidth;
    const entityHeight = isPlayerEntity(entity) ? entity.playerInfo.eyeHeight * 1.125 : this.defaultEntityHeight;

    logger.debug('[analyzeSurface] Starting analysis', {
      entityId: entity.entityId,
      position: { x: entity.position.x.toFixed(2), y: entity.position.y.toFixed(2), z: entity.position.z.toFixed(2) },
    });

    // Get block context at current position (SINGLE CALL!)
    const context = this.getPlayerBlockContext(
      entity.position.x,
      entity.position.z,
      entity.position.y,
      entityWidth,
      entityHeight
    );

    logger.debug('[analyzeSurface] Block context retrieved', {
      belowLevelCount: context.belowLevel.blocks.length,
      currentLevelCount: context.currentLevel.blocks.length,
      belowHasGround: context.belowLevel.hasGround,
      currentHasSolid: context.currentLevel.hasSolid,
      belowBlocks: context.belowLevel.blocks.map(b => ({ pos: b.block.block.position, solid: b.block.currentModifier.physics?.solid })),
      currentBlocks: context.currentLevel.blocks.map(b => ({ pos: b.block.block.position, solid: b.block.currentModifier.physics?.solid })),
    });

    // Find surface block and analyze it
    let surfaceBlock: ClientBlock | null = null;
    let cornerHeights: [number, number, number, number] | undefined;
    let surfaceHeight = entity.position.y;
    let surfaceType: 'flat' | 'slope' | 'none' | 'climbing' = 'none';
    let canWalkOn = false;
    let isSolid = false;

    // Check if climbing (ladders)
    if (context.currentLevel.climbableSpeed > 0 && context.currentLevel.passableFrom === undefined) {
      logger.debug('[analyzeSurface] Climbing surface detected');
      return {
        type: 'climbing',
        surfaceHeight: entity.position.y,
        slope: { x: 0, z: 0 },
        resistance: 0,
        canWalkOn: true,
        isSolid: false,
        context,
      };
    }

    // Check blocks we're standing ON (belowLevel, Y-1)
    for (const blockInfo of context.belowLevel.blocks) {
      if (blockInfo.block.currentModifier.physics?.solid) {
        surfaceBlock = blockInfo.block;
        logger.debug('[analyzeSurface] Found surface block in belowLevel', {
          position: blockInfo.block.block.position,
        });
        break;
      }
    }

    // If no ground below, check blocks we're IN (currentLevel, Y)
    // Important for slope blocks where player is inside the block
    if (!surfaceBlock) {
      for (const blockInfo of context.currentLevel.blocks) {
        if (blockInfo.block.currentModifier.physics?.solid) {
          surfaceBlock = blockInfo.block;
          logger.debug('[analyzeSurface] Found surface block in currentLevel', {
            position: blockInfo.block.block.position,
          });
          break;
        }
      }
    }

    // Analyze surface block
    if (surfaceBlock) {
      isSolid = true;
      cornerHeights = this.getCornerHeights(surfaceBlock);
      surfaceHeight = this.getBlockSurfaceHeight(surfaceBlock, entity.position.x, entity.position.z);

      logger.debug('[analyzeSurface] Surface block analyzed', {
        blockPos: surfaceBlock.block.position,
        cornerHeights,
        surfaceHeight: surfaceHeight.toFixed(3),
      });

      if (cornerHeights) {
        surfaceType = 'slope';
        canWalkOn = true; // Slopes are always walkable
      } else {
        surfaceType = 'flat';
        canWalkOn = !context.currentLevel.hasSolid; // Flat ground is walkable if not colliding
      }
    } else {
      // No surface found
      surfaceHeight = context.belowLevel.groundY >= 0 ? context.belowLevel.groundY : entity.position.y;
    }

    // Calculate slope vector
    const slope = cornerHeights ? this.calculateSlope(cornerHeights) : { x: 0, z: 0 };

    // Log after slope is calculated
    if (cornerHeights) {
      logger.debug('[analyzeSurface] SLOPE DETECTED!', { cornerHeights, slope });
    } else if (surfaceBlock) {
      logger.debug('[analyzeSurface] Flat surface', { canWalkOn });
    } else {
      logger.debug('[analyzeSurface] No surface block found');
    }

    // Get resistance (from context)
    const resistance = Math.max(context.currentLevel.resistance, context.belowLevel.blocks.length > 0
      ? Math.max(...context.belowLevel.blocks.map(b => b.block.currentModifier.physics?.resistance ?? 0))
      : 0);

    const result = {
      type: surfaceType,
      surfaceHeight,
      cornerHeights,
      slope,
      resistance,
      canWalkOn,
      isSolid,
      context,
    };

    logger.debug('[analyzeSurface] Final result', {
      type: result.type,
      canWalkOn: result.canWalkOn,
      slope: result.slope,
      surfaceHeight: result.surfaceHeight.toFixed(3),
    });

    return result;
  }

  /**
   * Calculate all forces acting on entity.
   * Accumulates gravity, input, slope, autoMove, and climbing forces.
   *
   * @param entity Entity to calculate forces for
   * @param surface Surface state analysis
   * @param inputVelocity Input velocity from player (before this frame)
   * @param deltaTime Frame time
   * @returns ForceState with all accumulated forces
   */
  private calculateForces(
    entity: PhysicsEntity,
    surface: SurfaceState,
    inputVelocity: Vector3,
    deltaTime: number
  ): ForceState {
    const forces: ForceState = {
      gravity: { x: 0, y: 0, z: 0 },
      input: { x: 0, y: 0, z: 0 },
      slope: { x: 0, y: 0, z: 0 },
      autoMove: { x: 0, y: 0, z: 0 },
      climb: { x: 0, y: 0, z: 0 },
      total: { x: 0, y: 0, z: 0 },
      applyGravity: true,
      isClimbing: false,
    };

    // 1. GRAVITY FORCE
    if (surface.type === 'climbing') {
      // Climbing: No gravity
      forces.applyGravity = false;
      forces.isClimbing = true;
    } else if (this.isUnderwater) {
      // Underwater: Reduced gravity
      forces.gravity.y = this.underwaterGravity;
      forces.applyGravity = true;
    } else if (entity.isOnGround) {
      // On ground: No gravity (already supported)
      forces.applyGravity = false;
    } else {
      // Falling: Normal gravity
      forces.gravity.y = this.gravity;
      forces.applyGravity = true;
    }

    // 2. INPUT FORCE (from current velocity - set by moveForward/moveRight/jump)
    forces.input.x = inputVelocity.x;
    forces.input.y = inputVelocity.y;
    forces.input.z = inputVelocity.z;

    // 3. SLOPE FORCE (sliding)
    if (surface.type === 'slope' && entity.isOnGround) {
      const slidingFactor = Math.max(0, 1 - surface.resistance);
      const slidingStrength = 3.0; // blocks per second

      forces.slope.x = surface.slope.x * slidingFactor * slidingStrength;
      forces.slope.z = surface.slope.z * slidingFactor * slidingStrength;
    }

    // 4. AUTOMOVE FORCE (conveyors)
    const autoMoveX = Math.max(
      Math.abs(surface.context.belowLevel.autoMove.x),
      Math.abs(surface.context.currentLevel.autoMove.x)
    ) * (Math.abs(surface.context.belowLevel.autoMove.x) > Math.abs(surface.context.currentLevel.autoMove.x)
      ? Math.sign(surface.context.belowLevel.autoMove.x)
      : Math.sign(surface.context.currentLevel.autoMove.x));

    const autoMoveY = Math.max(
      Math.abs(surface.context.belowLevel.autoMove.y),
      Math.abs(surface.context.currentLevel.autoMove.y)
    ) * (Math.abs(surface.context.belowLevel.autoMove.y) > Math.abs(surface.context.currentLevel.autoMove.y)
      ? Math.sign(surface.context.belowLevel.autoMove.y)
      : Math.sign(surface.context.currentLevel.autoMove.y));

    const autoMoveZ = Math.max(
      Math.abs(surface.context.belowLevel.autoMove.z),
      Math.abs(surface.context.currentLevel.autoMove.z)
    ) * (Math.abs(surface.context.belowLevel.autoMove.z) > Math.abs(surface.context.currentLevel.autoMove.z)
      ? Math.sign(surface.context.belowLevel.autoMove.z)
      : Math.sign(surface.context.currentLevel.autoMove.z));

    forces.autoMove.x = autoMoveX;
    forces.autoMove.y = autoMoveY;
    forces.autoMove.z = autoMoveZ;

    // 5. CLIMB FORCE (ladders)
    if (surface.type === 'climbing') {
      forces.climb.y = surface.context.currentLevel.climbableSpeed;
    }

    // 6. COMBINE ALL FORCES
    // X-axis: input + slope + autoMove
    forces.total.x = forces.input.x + forces.slope.x + forces.autoMove.x;

    // Y-axis: input + gravity + climb + autoMove
    forces.total.y = forces.input.y + forces.gravity.y + forces.climb.y + forces.autoMove.y;

    // Z-axis: input + slope + autoMove
    forces.total.z = forces.input.z + forces.slope.z + forces.autoMove.z;

    return forces;
  }

  /**
   * Resolve movement with collision detection and force application.
   * Central function that applies all forces while respecting collisions.
   *
   * @param entity Entity to move
   * @param forces Accumulated forces
   * @param surface Current surface state
   * @param deltaTime Frame time
   */
  private resolveMovement(
    entity: PhysicsEntity,
    forces: ForceState,
    surface: SurfaceState,
    deltaTime: number
  ): void {
    // Calculate desired velocity from forces
    const desiredVelocity = new Vector3(
      forces.total.x,
      forces.total.y,
      forces.total.z
    );

    // Apply gravity to velocity (accumulates over frames)
    if (forces.applyGravity) {
      entity.velocity.y += forces.gravity.y * deltaTime;
    } else {
      entity.velocity.y = 0;
    }

    // For horizontal movement (X, Z):
    if (surface.type === 'slope') {
      // On slope: Input movement is already applied by tryMoveHorizontal
      // Only apply automatic forces (slope sliding, autoMove)
      entity.position.x += (forces.slope.x + forces.autoMove.x) * deltaTime;
      entity.position.z += (forces.slope.z + forces.autoMove.z) * deltaTime;

      // Adjust Y to follow surface (REPLACES gravity/velocity)
      entity.position.y = surface.surfaceHeight;
      entity.velocity.y = 0; // Reset Y velocity on slope

      console.log('📍 Slope movement - automatic forces only',
        '\n  position:', JSON.stringify({ x: entity.position.x.toFixed(2), y: entity.position.y.toFixed(2), z: entity.position.z.toFixed(2) }),
        '\n  slope force:', JSON.stringify({ x: forces.slope.x, z: forces.slope.z }),
        '\n  autoMove:', JSON.stringify({ x: forces.autoMove.x, z: forces.autoMove.z }),
        '\n  surfaceHeight:', surface.surfaceHeight.toFixed(3)
      );
    } else if (surface.type === 'climbing') {
      // Climbing: Use climb velocity
      entity.velocity.y = forces.climb.y;
      // Horizontal movement from input only (no sliding on ladders)
      entity.position.x += forces.input.x * deltaTime;
      entity.position.z += forces.input.z * deltaTime;
      // Apply vertical velocity for climbing
      entity.position.y += entity.velocity.y * deltaTime;
    } else {
      // Flat or no surface: Normal physics
      // Input velocity is already in entity.velocity (set by moveForward/etc)
      // Just apply autoMove
      entity.position.x += forces.autoMove.x * deltaTime;
      entity.position.z += forces.autoMove.z * deltaTime;

      // Apply vertical velocity (gravity, jump)
      entity.position.y += entity.velocity.y * deltaTime;
    }
  }

  /**
   * Calculate slope vector from corner heights.
   * Returns normalized slope in X and Z directions.
   *
   * @param cornerHeights Array of 4 corner heights [NW, NE, SE, SW]
   * @returns Vector2 with slope in X and Z directions (range: -1 to 1 per axis)
   */
  private calculateSlope(cornerHeights: [number, number, number, number]): { x: number; z: number } {
    // Calculate average slope in X direction (West to East)
    // Compare West side (NW, SW) to East side (NE, SE)
    const westHeight = (cornerHeights[0] + cornerHeights[3]) / 2; // Average of NW and SW
    const eastHeight = (cornerHeights[1] + cornerHeights[2]) / 2; // Average of NE and SE
    const slopeX = eastHeight - westHeight; // Positive = rising to East

    // Calculate average slope in Z direction (North to South)
    // Compare North side (NW, NE) to South side (SW, SE)
    const northHeight = (cornerHeights[0] + cornerHeights[1]) / 2; // Average of NW and NE
    const southHeight = (cornerHeights[3] + cornerHeights[2]) / 2; // Average of SW and SE
    const slopeZ = southHeight - northHeight; // Positive = rising to South

    return { x: slopeX, z: slopeZ };
  }

  /**
   * Get all relevant blocks around player position and aggregate their physics properties
   *
   * This central function collects:
   * - Blocks at current Y level (for horizontal collision)
   * - Blocks above (Y + 1, for climb clearance)
   * - Blocks below (Y - 1 to Y - 3, for ground detection)
   * - Blocks player occupies (for push-up, water detection, etc.)
   *
   * Future physics properties to support:
   * - resistance (movement speed reduction)
   * - climbable (ladder-like climbing)
   * - autoClimbable (auto step-up)
   * - interactive (button, door, etc.)
   * - autoMove (conveyor belt, water current)
   * - gateFromDirection (one-way passage)
   * - liquid properties (water, lava)
   *
   * @param x X position (world coordinates)
   * @param z Z position (world coordinates)
   * @param y Y position (world coordinates, feet level)
   * @param entityWidth Entity width for bounding box
   * @param entityHeight Entity height for body collision
   * @returns Block context with aggregated properties
   */
  private getPlayerBlockContext(
    x: number,
    z: number,
    y: number,
    entityWidth: number,
    entityHeight: number
  ): PlayerBlockContext {
    if (!this.chunkService) {
      // No chunk service - return empty context
      return {
        currentLevel: { blocks: [], hasSolid: false, hasAutoClimbable: false, hasAutoJump: false, climbableSpeed: 0, resistance: 0, autoMove: { x: 0, y: 0, z: 0 }, autoOrientationY: undefined, passableFrom: undefined },
        aboveLevel: { blocks: [], hasSolid: false, isClear: true },
        belowLevel: { blocks: [], hasGround: false, hasAutoJump: false, autoMove: { x: 0, y: 0, z: 0 }, autoOrientationY: undefined, groundY: -1 },
        occupiedBlocks: { blocks: [], hasSolid: false, hasLiquid: false, hasAutoJump: false, autoMove: { x: 0, y: 0, z: 0 } },
      };
    }

    const currentBlockY = Math.floor(y);

    // Calculate entity bounds (4 corners)
    const minX = x - entityWidth / 2;
    const maxX = x + entityWidth / 2;
    const minZ = z - entityWidth / 2;
    const maxZ = z + entityWidth / 2;

    const cornerPositions = [
      { x: Math.floor(minX), z: Math.floor(minZ) },
      { x: Math.floor(maxX), z: Math.floor(minZ) },
      { x: Math.floor(minX), z: Math.floor(maxZ) },
      { x: Math.floor(maxX), z: Math.floor(maxZ) },
    ];

    // --- Current Level (feet) ---
    const currentLevel = {
      blocks: [] as Array<{ x: number; y: number; z: number; block: any }>,
      hasSolid: false,
      hasAutoClimbable: false,
      hasAutoJump: false,
      climbableSpeed: 0,
      resistance: 0,
      autoMove: { x: 0, y: 0, z: 0 },
      autoOrientationY: undefined as number | undefined,
      passableFrom: undefined as number | undefined,
    };

    for (const pos of cornerPositions) {
      const block = this.chunkService.getBlockAt(pos.x, currentBlockY, pos.z);
      if (block) {
        currentLevel.blocks.push({ x: pos.x, y: currentBlockY, z: pos.z, block });
        const physics = block.currentModifier.physics;

        if (physics?.solid) currentLevel.hasSolid = true;
        if (physics?.autoClimbable) currentLevel.hasAutoClimbable = true;
        if (physics?.autoJump) currentLevel.hasAutoJump = true;
        // Collect climbable speed: Use maximum speed from all blocks
        if (physics?.climbable && physics.climbable > 0) {
          currentLevel.climbableSpeed = Math.max(currentLevel.climbableSpeed, physics.climbable);
        }
        if (physics?.resistance) {
          currentLevel.resistance = Math.max(currentLevel.resistance, physics.resistance);
        }
        // Collect autoMove: Use maximum per axis
        if (physics?.autoMove) {
          currentLevel.autoMove.x = Math.max(currentLevel.autoMove.x, Math.abs(physics.autoMove.x)) * Math.sign(physics.autoMove.x || 0);
          currentLevel.autoMove.y = Math.max(currentLevel.autoMove.y, Math.abs(physics.autoMove.y)) * Math.sign(physics.autoMove.y || 0);
          currentLevel.autoMove.z = Math.max(currentLevel.autoMove.z, Math.abs(physics.autoMove.z)) * Math.sign(physics.autoMove.z || 0);
        }
        // Collect autoOrientationY: Use last (most recent) value
        if (physics?.autoOrientationY !== undefined) {
          currentLevel.autoOrientationY = physics.autoOrientationY;
        }
        // Collect passableFrom: OR combine all blocks (union of allowed directions)
        if (physics?.passableFrom !== undefined) {
          currentLevel.passableFrom = (currentLevel.passableFrom ?? 0) | physics.passableFrom;
        }
      }
    }

    // --- Above Level (Y + 1) ---
    const aboveLevel = {
      blocks: [] as Array<{ x: number; y: number; z: number; block: any }>,
      hasSolid: false,
      isClear: true,
    };

    for (const pos of cornerPositions) {
      const block = this.chunkService.getBlockAt(pos.x, currentBlockY + 1, pos.z);
      if (block) {
        aboveLevel.blocks.push({ x: pos.x, y: currentBlockY + 1, z: pos.z, block });
        if (block.currentModifier.physics?.solid) {
          aboveLevel.hasSolid = true;
          aboveLevel.isClear = false;
        }
      }
    }

    // --- Below Level (ground detection, Y - 1 to Y - 3) ---
    const belowLevel = {
      blocks: [] as Array<{ x: number; y: number; z: number; block: any }>,
      hasGround: false,
      hasAutoJump: false,
      autoMove: { x: 0, y: 0, z: 0 },
      autoOrientationY: undefined as number | undefined,
      groundY: -1,
    };

    // Search downward for ground (up to 3 blocks below for fast falling)
    for (let checkY = currentBlockY - 1; checkY >= currentBlockY - 3; checkY--) {
      for (const pos of cornerPositions) {
        const block = this.chunkService.getBlockAt(pos.x, checkY, pos.z);
        if (block) {
          belowLevel.blocks.push({ x: pos.x, y: checkY, z: pos.z, block });
          const physics = block.currentModifier.physics;

          if (physics?.solid && !belowLevel.hasGround) {
            belowLevel.hasGround = true;
            // Use interpolated surface height if block has cornerHeights
            belowLevel.groundY = this.getBlockSurfaceHeight(block, x, z);
          }
          // Check for autoJump on ground blocks (the block we're standing on)
          if (physics?.autoJump && checkY === currentBlockY - 1) {
            belowLevel.hasAutoJump = true;
          }
          // Collect autoMove from blocks below (standing on)
          if (physics?.autoMove && checkY === currentBlockY - 1) {
            belowLevel.autoMove.x = Math.max(belowLevel.autoMove.x, Math.abs(physics.autoMove.x)) * Math.sign(physics.autoMove.x || 0);
            belowLevel.autoMove.y = Math.max(belowLevel.autoMove.y, Math.abs(physics.autoMove.y)) * Math.sign(physics.autoMove.y || 0);
            belowLevel.autoMove.z = Math.max(belowLevel.autoMove.z, Math.abs(physics.autoMove.z)) * Math.sign(physics.autoMove.z || 0);
          }
          // Collect autoOrientationY from blocks below (standing on): Use last value
          if (physics?.autoOrientationY !== undefined && checkY === currentBlockY - 1) {
            belowLevel.autoOrientationY = physics.autoOrientationY;
          }
        }
      }
      if (belowLevel.hasGround) break; // Found ground, stop searching
    }

    // --- Occupied Blocks (player body space, from Y to Y + entityHeight) ---
    const occupiedBlocks = {
      blocks: [] as Array<{ x: number; y: number; z: number; block: any }>,
      hasSolid: false,
      hasLiquid: false,
      hasAutoJump: false,
      autoMove: { x: 0, y: 0, z: 0 },
    };

    const headBlockY = Math.floor(y + entityHeight);
    for (let checkY = currentBlockY; checkY <= headBlockY; checkY++) {
      const block = this.chunkService.getBlockAt(Math.floor(x), checkY, Math.floor(z));
      if (block) {
        occupiedBlocks.blocks.push({ x: Math.floor(x), y: checkY, z: Math.floor(z), block });
        const physics = block.currentModifier.physics;

        if (physics?.solid) {
          occupiedBlocks.hasSolid = true;
        }
        if (physics?.autoJump) {
          occupiedBlocks.hasAutoJump = true;
        }
        // Collect autoMove from occupied blocks
        if (physics?.autoMove) {
          occupiedBlocks.autoMove.x = Math.max(occupiedBlocks.autoMove.x, Math.abs(physics.autoMove.x)) * Math.sign(physics.autoMove.x || 0);
          occupiedBlocks.autoMove.y = Math.max(occupiedBlocks.autoMove.y, Math.abs(physics.autoMove.y)) * Math.sign(physics.autoMove.y || 0);
          occupiedBlocks.autoMove.z = Math.max(occupiedBlocks.autoMove.z, Math.abs(physics.autoMove.z)) * Math.sign(physics.autoMove.z || 0);
        }
        // TODO: Add liquid detection when implemented
        // if (block.isLiquid) occupiedBlocks.hasLiquid = true;
      }
    }

    return { currentLevel, aboveLevel, belowLevel, occupiedBlocks };
  }

  /**
   * Check ground collision and update entity state
   */
  private checkGroundCollision(entity: PhysicsEntity): void {
    if (!this.chunkService) {
      // Fallback to simple ground check
      if (entity.position.y <= 64 && entity.velocity.y <= 0) {
        entity.position.y = 64;
        entity.velocity.y = 0;
        entity.isOnGround = true;
      } else if (entity.velocity.y > 0) {
        entity.isOnGround = false;
      }
      return;
    }

    // Get entity width (for now same for all entities, but can be PlayerInfo-based later)
    const entityWidth = this.defaultEntityWidth;

    // Check multiple Y levels to prevent falling through
    const feetY = entity.position.y;

    // Check center and corners for more robust collision
    const checkPoints = [
      { x: entity.position.x, z: entity.position.z }, // Center
      { x: entity.position.x - entityWidth / 2, z: entity.position.z - entityWidth / 2 }, // Corner
      { x: entity.position.x + entityWidth / 2, z: entity.position.z - entityWidth / 2 }, // Corner
      { x: entity.position.x - entityWidth / 2, z: entity.position.z + entityWidth / 2 }, // Corner
      { x: entity.position.x + entityWidth / 2, z: entity.position.z + entityWidth / 2 }, // Corner
    ];

    // Search downward for ground
    let foundGround = false;
    let groundY = -1;

    // Check from current position down to 3 blocks below (to catch fast falling)
    for (let checkY = Math.floor(feetY); checkY >= Math.floor(feetY - 3); checkY--) {
      for (const point of checkPoints) {
        const blockX = Math.floor(point.x);
        const blockZ = Math.floor(point.z);
        const clientBlock = this.chunkService.getBlockAt(blockX, checkY, blockZ);

        // Check if block is solid AND does not have passableFrom
        // Blocks with passableFrom should NOT act as ground
        if (clientBlock && clientBlock.currentModifier.physics?.solid) {
          // Skip blocks with passableFrom - they don't act as ground
          if (clientBlock.currentModifier.physics?.passableFrom !== undefined) {
            continue;
          }

          // Found solid ground at this Y level
          // Use interpolated surface height if block has cornerHeights
          const blockTopY = this.getBlockSurfaceHeight(clientBlock, point.x, point.z);

          // Only snap if we're falling and at or below this level
          if (feetY <= blockTopY + 0.1) {
            foundGround = true;
            groundY = Math.max(groundY, blockTopY); // Use highest ground if multiple blocks
            break;
          }
        }
      }
      if (foundGround) break;
    }

    if (foundGround && entity.velocity.y <= 0) {
      // Snap to top of block
      entity.position.y = groundY;
      entity.velocity.y = 0;
      entity.isOnGround = true;
      logger.debug('Snapped to ground', {
        entityId: entity.entityId,
        groundY,
        velocityY: entity.velocity.y,
      });
    } else if (entity.velocity.y > 0) {
      // Jumping up
      entity.isOnGround = false;
    } else {
      // Falling but no ground found
      entity.isOnGround = false;
      if (entity.velocity.y <= 0) {
        logger.debug('No ground found - falling', {
          entityId: entity.entityId,
          feetY,
          velocityY: entity.velocity.y,
          posX: entity.position.x,
          posZ: entity.position.z,
        });
      }
    }
  }

  /**
   * Check if player is inside a block and push up if needed
   *
   * Note: Auto-push-up is DISABLED when block has passableFrom set (as per spec)
   */
  private checkAndPushUp(entity: PhysicsEntity): void {
    if (!this.chunkService) {
      return;
    }

    // Get entity height (PlayerEntity uses eyeHeight * 1.125)
    const entityHeight = isPlayerEntity(entity)
      ? entity.playerInfo.eyeHeight * 1.125
      : this.defaultEntityHeight;

    // Check from feet to head for solid blocks
    const feetY = entity.position.y;
    const headY = entity.position.y + entityHeight;

    // Check if any part of player is inside a solid block
    for (let y = Math.floor(feetY); y <= Math.floor(headY); y++) {
      const blockX = Math.floor(entity.position.x);
      const blockZ = Math.floor(entity.position.z);
      const clientBlock = this.chunkService.getBlockAt(blockX, y, blockZ);

      if (clientBlock && clientBlock.currentModifier.physics?.solid) {
        // Check if block has passableFrom - if so, disable auto-push-up
        if (clientBlock.currentModifier.physics?.passableFrom !== undefined) {
          // Block has passableFrom - don't auto-push up
          return;
        }

        // Push player up to top of this block
        entity.position.y = y + 1;
        entity.velocity.y = 0;
        return;
      }
    }
  }

  /**
   * Apply autoMove velocity to entity based on blocks at feet level or below
   *
   * AutoMove is applied when:
   * - Player is standing on autoMove block (block below feet, Y - 1), OR
   * - Player has autoMove block at feet level (Y)
   *
   * The maximum velocity per axis from all relevant blocks is used.
   * Movement is smooth and continuous while on/in the block.
   *
   * Use case: Conveyor belts, water currents, ice sliding
   */
  // applyAutoMove() is now integrated into calculateForces() and resolveMovement()

  /**
   * Apply autoOrientationY rotation to entity based on blocks at feet level or below
   *
   * AutoOrientationY is applied when:
   * - Player is standing on autoOrientationY block (block below feet, Y - 1), OR
   * - Player has autoOrientationY block at feet level (Y)
   *
   * When multiple blocks have orientation, the last (most recent) value is used.
   * Rotation is smooth and gradual with a standard rotation speed.
   *
   * Use case: Directional blocks (arrows), rotating platforms, alignment zones
   */
  private applyAutoOrientation(entity: PhysicsEntity, deltaTime: number): void {
    if (!this.chunkService) {
      return;
    }

    // Only apply autoOrientation in walk mode
    if (entity.movementMode !== 'walk') {
      return;
    }

    // Get entity dimensions
    const entityWidth = this.defaultEntityWidth;
    const entityHeight = isPlayerEntity(entity) ? entity.playerInfo.eyeHeight * 1.125 : this.defaultEntityHeight;

    // Get block context at current position
    const context = this.getPlayerBlockContext(
      entity.position.x,
      entity.position.z,
      entity.position.y,
      entityWidth,
      entityHeight
    );

    // Collect autoOrientationY from belowLevel (standing on) or currentLevel (at feet)
    // Prefer belowLevel (standing on block) over currentLevel (feet position)
    const targetOrientationY = context.belowLevel.autoOrientationY !== undefined
      ? context.belowLevel.autoOrientationY
      : context.currentLevel.autoOrientationY;

    // Apply smooth rotation if target orientation is defined
    if (targetOrientationY !== undefined) {
      const currentRotationY = entity.rotation.y;
      const targetRotationY = targetOrientationY;

      // Calculate shortest rotation distance (handle wrapping at 2π)
      let deltaRotation = targetRotationY - currentRotationY;

      // Normalize to [-π, π] range
      while (deltaRotation > Math.PI) deltaRotation -= Math.PI * 2;
      while (deltaRotation < -Math.PI) deltaRotation += Math.PI * 2;

      // Standard rotation speed: 2π radians per second (full rotation in 1 second)
      const rotationSpeed = Math.PI * 2;
      const maxRotationThisFrame = rotationSpeed * deltaTime;

      // Apply rotation smoothly
      if (Math.abs(deltaRotation) > 0.01) { // Small threshold to avoid jitter
        const rotationAmount = Math.sign(deltaRotation) * Math.min(Math.abs(deltaRotation), maxRotationThisFrame);
        entity.rotation.y = currentRotationY + rotationAmount;

        // Normalize rotation to [0, 2π] range
        if (entity.rotation.y < 0) entity.rotation.y += Math.PI * 2;
        if (entity.rotation.y >= Math.PI * 2) entity.rotation.y -= Math.PI * 2;

        // Sync camera rotation for player entity
        if (isPlayerEntity(entity)) {
          const cameraService = this.appContext.services.camera;
          if (cameraService) {
            const currentCameraRotation = cameraService.getRotation();
            // Set camera yaw (Y rotation) while keeping pitch (X) unchanged
            cameraService.setRotation(currentCameraRotation.x, entity.rotation.y, 0);
          }
        }

        logger.debug('Auto-orientation applied', {
          entityId: entity.entityId,
          from: currentRotationY,
          to: entity.rotation.y,
          target: targetRotationY,
          delta: deltaRotation,
        });
      }
    }
  }

  /**
   * Apply sliding from sloped surfaces based on cornerHeights.
   * Sliding velocity is calculated from slope and modified by resistance.
   *
   * Sliding is applied when:
   * - Player is standing on ground (isOnGround)
   * - Block has cornerHeights defined
   * - Block has resistance (used to dampen sliding)
   *
   * Formula: effectiveSliding = slope × (1 - resistance)
   * - resistance = 0: Full sliding
   * - resistance = 1: No sliding (completely blocked)
   *
   * Use case: Ramps, slides, sloped terrain
   */
  // applySlidingFromSlope() is now integrated into calculateForces() and resolveMovement()

  /**
   * Check if player is on or in a block with autoJump and trigger jump
   *
   * AutoJump is triggered when:
   * - Player is standing on autoJump block (block below feet, Y - 1), OR
   * - Player has autoJump block at feet level (Y) - for pressure plates
   *
   * Only these two levels are checked. No occupiedBlocks check (body).
   *
   * Use case: Trampoline blocks (below), pressure plates (at feet level)
   */
  private checkAutoJump(entity: PhysicsEntity): void {
    if (!this.chunkService) {
      return;
    }

    // Only trigger autoJump in walk mode
    if (entity.movementMode !== 'walk') {
      return;
    }

    // Get entity dimensions
    const entityWidth = this.defaultEntityWidth;
    const entityHeight = isPlayerEntity(entity) ? entity.playerInfo.eyeHeight * 1.125 : this.defaultEntityHeight;

    // Get block context at current position
    const context = this.getPlayerBlockContext(
      entity.position.x,
      entity.position.z,
      entity.position.y,
      entityWidth,
      entityHeight
    );

    // Check 1: Block below feet (Y - 1) - Trampoline case
    const onAutoJumpBlock = entity.isOnGround && context.belowLevel.hasAutoJump;

    // Check 2: Block at feet level (Y) - Pressure plate case
    const atAutoJumpBlock = context.currentLevel.hasAutoJump;

    // Trigger jump if either condition is met
    if (onAutoJumpBlock || atAutoJumpBlock) {
      this.jump(entity);
      logger.debug('Auto-jump triggered', {
        entityId: entity.entityId,
        onGround: entity.isOnGround,
        onBlock: onAutoJumpBlock,
        atFeetLevel: atAutoJumpBlock,
      });
    }
  }

  /**
   * Try to move entity horizontally with collision detection and auto-climb
   *
   * Uses getPlayerBlockContext() to collect all relevant blocks and their properties.
   *
   * Decision flow:
   * 1. Get block context at target position
   * 2. No collision → Move
   * 3. Collision with solid block:
   *    - Has autoClimbable + space above + on ground → Climb + Move
   *    - No autoClimbable or blocked above → Block movement
   *
   * @param entity Entity to move
   * @param dx Delta X movement
   * @param dz Delta Z movement
   * @returns true if movement was fully handled (climbing or blocked), false if normal movement occurred
   */
  private tryMoveHorizontal(entity: PhysicsEntity, dx: number, dz: number): boolean {
    if (!this.chunkService) {
      // No chunk service - just move
      entity.position.x += dx;
      entity.position.z += dz;
      return false;
    }

    // Calculate target position
    const targetX = entity.position.x + dx;
    const targetZ = entity.position.z + dz;
    const feetY = entity.position.y;

    // Check if target position is in loaded chunk
    if (!this.isChunkLoaded(targetX, targetZ)) {
      // Chunk not loaded - don't move
      return true; // Movement blocked (chunk not loaded)
    }

    // Get entity dimensions
    const entityWidth = this.defaultEntityWidth;
    const entityHeight = isPlayerEntity(entity) ? entity.playerInfo.eyeHeight * 1.125 : this.defaultEntityHeight;

    // Get block context at target position
    const context = this.getPlayerBlockContext(targetX, targetZ, feetY, entityWidth, entityHeight);


    // Check for climbable block FIRST (before other collision checks)
    // Climbable blocks can be solid (like ladders)
    // BUT: If block has passableFrom set, disable climbing (as per spec)
    if (context.currentLevel.climbableSpeed > 0 && context.currentLevel.passableFrom === undefined) {
      // Climbable block detected: Set upward velocity, no horizontal movement
      entity.velocity.y = context.currentLevel.climbableSpeed;

      // Mark that climbing velocity was set this frame
      this.climbableVelocitySetThisFrame.set(entity.entityId, true);

      return true; // Movement handled by climbing
    }

    // Get block coordinates
    // For Minecraft-style coordinates: block N contains positions from N.0 to N.999...
    // For negative coords: block -8 contains -8.0 to -8.999, so -8.01 is in block -8
    // Math.floor works correctly for positive, but for negative we get wrong results:
    // Math.floor(-8.01) = -9 (wrong! should be -8)
    // Correct formula: coord < 0 ? Math.ceil(coord) - 1 : Math.floor(coord)
    // But simpler: just use Math.floor universally, it's correct for block coords
    // WAIT - actually for negative coords in Minecraft:
    // Block -8: from -8.0 to -8.999
    // Block -9: from -9.0 to -9.999
    // Position -8.01 should be in block -8, but Math.floor(-8.01)=-9
    // We need: Math.floor for positive, Math.floor for negative (already correct!)
    // Actually the issue is getBlockAt might use different coord system!

    const currentBlockX = Math.floor(entity.position.x);
    const currentBlockZ = Math.floor(entity.position.z);
    const currentBlockY = Math.floor(feetY);
    const targetBlockX = Math.floor(targetX);
    const targetBlockZ = Math.floor(targetZ);

    // CHECK FOR SLOPE BLOCKS EARLY (before passableFrom checks)
    // Slopes with cornerHeights should be walkable even when solid
    // Check BOTH current block (standing on slope) AND target block (moving to slope)
    const currentCenterPos = { x: currentBlockX, y: currentBlockY, z: currentBlockZ };
    const targetCenterPos = { x: targetBlockX, y: currentBlockY, z: targetBlockZ };

    // Check if we're currently ON a slope
    const currentBlock = this.chunkService.getBlockAt(currentCenterPos.x, currentCenterPos.y, currentCenterPos.z);
    const currentHasSlope = currentBlock ? !!this.getCornerHeights(currentBlock) : false;

    // Check if TARGET is a slope
    const targetBlock = this.chunkService.getBlockAt(targetCenterPos.x, targetCenterPos.y, targetCenterPos.z);
    const targetHasSlope = targetBlock ? !!this.getCornerHeights(targetBlock) : false;

    // Debug logging for specific test blocks
    const isTestArea = (currentCenterPos.x === 2 && currentCenterPos.y === 64 && currentCenterPos.z >= -9 && currentCenterPos.z <= -5) ||
                       (targetCenterPos.x === 2 && targetCenterPos.y === 64 && targetCenterPos.z >= -9 && targetCenterPos.z <= -5);

    if (isTestArea) {
      console.log('🔍 [tryMoveHorizontal] Slope check in test area',
        '\n  FROM:', JSON.stringify({ x: entity.position.x.toFixed(2), y: entity.position.y.toFixed(2), z: entity.position.z.toFixed(2) }),
        '\n  TO:', JSON.stringify({ x: targetX.toFixed(2), z: targetZ.toFixed(2) }),
        '\n  currentBlock:', JSON.stringify(currentCenterPos), 'hasSlope:', currentHasSlope,
        '\n  targetBlock:', JSON.stringify(targetCenterPos), 'hasSlope:', targetHasSlope,
        '\n  hasSolid:', context.currentLevel.hasSolid,
        '\n  WILL ALLOW:', (currentHasSlope || targetHasSlope)
      );
    }

    if (currentHasSlope || targetHasSlope) {
      // Either current OR target is a slope - allow movement!
      entity.position.x = targetX;
      entity.position.z = targetZ;

      console.log('✅ [tryMoveHorizontal] SLOPE MOVEMENT (current or target is slope)',
        '\n  currentBlock:', JSON.stringify(currentCenterPos), 'hasSlope:', currentHasSlope,
        '\n  targetBlock:', JSON.stringify(targetCenterPos), 'hasSlope:', targetHasSlope,
        '\n  moving to:', JSON.stringify({ x: targetX.toFixed(2), z: targetZ.toFixed(2) })
      );
      return false; // Movement allowed
    }

    if (isTestArea) {
      console.log('❌ [tryMoveHorizontal] NO slope detected, continuing with normal collision',
        '\n  will check passableFrom and other collision logic'
      );
    }

    // Determine movement direction
    const movementDir = this.getMovementDirection(dx, dz);

    // Get current block context for exit checks (SOURCE BLOCK)
    const currentContext = this.getPlayerBlockContext(entity.position.x, entity.position.z, feetY, entityWidth, entityHeight);

    // EXIT CHECK: Check the center block only (not corner blocks)
    // This prevents false positives when standing at block boundaries
    // Check when approaching the edge (at 0.8 or 0.2 depending on direction)

    if (this.chunkService) {
      const centerBlock = this.chunkService.getBlockAt(currentBlockX, currentBlockY, currentBlockZ);
      if (centerBlock) {
        const physics = centerBlock.currentModifier.physics;

        // Check if block has passableFrom
        if (physics?.passableFrom !== undefined) {
          // Check if we're near the edge in the movement direction
          const posInBlockX = entity.position.x - currentBlockX;
          const posInBlockZ = entity.position.z - currentBlockZ;
          const nearEdge =
            (movementDir === Direction.WEST && posInBlockX < 0.2) ||
            (movementDir === Direction.EAST && posInBlockX > 0.8) ||
            (movementDir === Direction.NORTH && posInBlockZ < 0.2) ||
            (movementDir === Direction.SOUTH && posInBlockZ > 0.8);

          // Only check exit if near the edge
          if (nearEdge) {
            // canLeaveTo: Check if we can leave towards movementDir
            const canLeave = this.canLeaveTo(physics.passableFrom, movementDir, physics.solid ?? false);

            if (!canLeave) {
              // Cannot exit in this direction - blocked by wall at edge
              return true; // Movement blocked
            }
          }
        }
      }
    }

    // Entry check: Only check passableFrom if crossing into a different block
    const crossingIntoNewBlock = currentBlockX !== targetBlockX || currentBlockZ !== targetBlockZ;

    // Entry check: Only run if we're moving to a different block AND it has passableFrom
    // If moving within same block, skip entry check (only exit check matters)
    if (crossingIntoNewBlock && context.currentLevel.passableFrom !== undefined) {
      if (context.currentLevel.hasSolid) {
        // Solid block with passableFrom: One-way block
        // Can only enter from specified directions when crossing into new block
        const entrySide = DirectionHelper.getOpposite(movementDir);
        const canEnter = this.canEnterFrom(context.currentLevel.passableFrom, entrySide, true);

        if (!canEnter) {
          // Cannot enter from this direction
          return true; // Movement blocked
        }
        // Can enter - allow movement
        entity.position.x = targetX;
        entity.position.z = targetZ;
        return false;
      } else {
        // Non-solid block with passableFrom: Wall at edges
        // Can only enter from directions specified in passableFrom
        const entrySide = DirectionHelper.getOpposite(movementDir);
        const canEnter = this.canEnterFrom(context.currentLevel.passableFrom, entrySide, false);

        if (!canEnter) {
          // Cannot enter from this direction - blocked by wall at edge
          return true; // Movement blocked
        }
        // Entry is allowed
        entity.position.x = targetX;
        entity.position.z = targetZ;
        return false;
      }
    }

    // If target has passableFrom but entry check didn't run (not crossing into new block),
    // check if we're already inside the same block or approaching from outside
    if (context.currentLevel.passableFrom !== undefined && !crossingIntoNewBlock) {
      // Check if source also has passableFrom (means we're already inside)
      const sourceBlock = this.chunkService?.getBlockAt(currentBlockX, currentBlockY, currentBlockZ);
      const sourcePassableFrom = sourceBlock?.currentModifier.physics?.passableFrom;
      const alreadyInsidePassableFromBlock = sourcePassableFrom !== undefined;

      if (alreadyInsidePassableFromBlock) {
        // Already inside a block with passableFrom - allow free movement
        entity.position.x = targetX;
        entity.position.z = targetZ;
        return false;
      }

      // Approaching from outside - check if entry would be allowed
      const entrySide = DirectionHelper.getOpposite(movementDir);
      const canEnter = this.canEnterFrom(context.currentLevel.passableFrom, entrySide, context.currentLevel.hasSolid);

      if (!canEnter) {
        return true; // Movement blocked
      }

      // Entry allowed - allow movement towards the block
      entity.position.x = targetX;
      entity.position.z = targetZ;
      return false;
    }

    // Standard collision logic
    // No collision at current level - move freely
    if (!context.currentLevel.hasSolid) {
      entity.position.x = targetX;
      entity.position.z = targetZ;
      return false; // Normal movement occurred
    }

    // This slope check was moved earlier (line 1916-1940) - remove duplicate

    // Collision detected - check if we can auto-climb
    if (context.currentLevel.hasAutoClimbable && entity.isOnGround && context.aboveLevel.isClear) {
      // Auto-climb conditions met:
      // 1. Block has autoClimbable property
      // 2. Entity is on ground
      // 3. Space above is clear
      if (!entity.climbState?.active) {
        const targetY = Math.floor(feetY) + 1;

        entity.climbState = {
          active: true,
          startX: entity.position.x,
          startY: feetY,
          startZ: entity.position.z,
          targetX: targetX,
          targetY: targetY,
          targetZ: targetZ,
          progress: 0.0,
        };
        entity.isOnGround = false;

        logger.debug('Auto-climb started (diagonal)', {
          entityId: entity.entityId,
          fromY: feetY.toFixed(2),
          toY: targetY,
          fromX: entity.position.x.toFixed(2),
          toX: targetX.toFixed(2),
          fromZ: entity.position.z.toFixed(2),
          toZ: targetZ.toFixed(2),
          blockCount: context.currentLevel.blocks.length,
        });
      }
      return true; // Movement fully handled by climb animation
    }

    // Cannot move:
    // - Solid block without autoClimbable, OR
    // - Has autoClimbable but not on ground, OR
    // - Has autoClimbable but space above blocked
    // Do nothing (position stays unchanged)
    return true; // Movement blocked
  }

  /**
   * Update entity in Fly mode
   */
  private updateFlyMode(entity: PhysicsEntity, deltaTime: number): void {
    // No gravity in fly mode
    // Velocity is directly controlled by input (no damping in fly mode for precise control)
    // Position is updated in moveForward/moveRight/moveUp methods

    // Clamp to world boundaries
    this.clampToWorldBounds(entity);
  }

  /**
   * Move entity forward/backward (relative to camera)
   *
   * TODO: When underwater, use fly-like movement (including pitch) but with collisions
   *
   * @param entity Entity to move
   * @param distance Distance to move (positive = forward)
   * @param cameraYaw Camera yaw rotation in radians
   * @param cameraPitch Camera pitch rotation in radians
   */
  moveForward(entity: PhysicsEntity, distance: number, cameraYaw: number, cameraPitch: number): void {
    if (entity.movementMode === 'walk' && !this.isUnderwater) {
      // Walk mode: Normal horizontal movement with collision detection
      // Climbing is handled in tryMoveHorizontal when climbable block is detected
      const dx = Math.sin(cameraYaw) * distance;
      const dz = Math.cos(cameraYaw) * distance;

      this.tryMoveHorizontal(entity, dx, dz);
    } else if (entity.movementMode === 'fly' || this.isUnderwater) {
      // Fly mode OR Underwater: Move in camera direction (including pitch)
      // TODO: Add collision detection when underwater
      const dx = Math.sin(cameraYaw) * Math.cos(cameraPitch) * distance;
      const dy = -Math.sin(cameraPitch) * distance;
      const dz = Math.cos(cameraYaw) * Math.cos(cameraPitch) * distance;

      entity.position.x += dx;
      entity.position.y += dy;
      entity.position.z += dz;
    }
  }

  /**
   * Move entity right/left (strafe)
   *
   * @param entity Entity to move
   * @param distance Distance to move (positive = right)
   * @param cameraYaw Camera yaw rotation in radians
   */
  moveRight(entity: PhysicsEntity, distance: number, cameraYaw: number): void {
    if (entity.movementMode === 'walk') {
      // Walk mode: Move perpendicular to camera yaw (in XZ plane) with collision
      const dx = Math.sin(cameraYaw + Math.PI / 2) * distance;
      const dz = Math.cos(cameraYaw + Math.PI / 2) * distance;

      this.tryMoveHorizontal(entity, dx, dz);
    } else {
      // Fly mode: Direct movement without collision
      const dx = Math.sin(cameraYaw + Math.PI / 2) * distance;
      const dz = Math.cos(cameraYaw + Math.PI / 2) * distance;

      entity.position.x += dx;
      entity.position.z += dz;
    }
  }

  /**
   * Move entity up/down (Fly mode or underwater)
   *
   * TODO: Underwater movement should also allow up/down movement
   *
   * @param entity Entity to move
   * @param distance Distance to move (positive = up)
   */
  moveUp(entity: PhysicsEntity, distance: number): void {
    if (entity.movementMode === 'fly' || this.isUnderwater) {
      entity.position.y += distance;
    }
  }

  /**
   * Jump (Walk mode only)
   *
   * For PlayerEntity: Uses cached effectiveJumpSpeed
   * For other entities: Uses default jumpSpeed
   */
  jump(entity: PhysicsEntity): void {
    if (entity.movementMode === 'walk' && entity.isOnGround) {
      // Get jump speed based on entity type
      const jumpSpeed = isPlayerEntity(entity)
        ? entity.effectiveJumpSpeed // Use cached effective value
        : this.defaultJumpSpeed;

      entity.velocity.y = jumpSpeed;
      entity.isOnGround = false;

      logger.debug('Entity jumped', {
        entityId: entity.entityId,
        jumpSpeed,
      });
    }
  }

  /**
   * Set movement mode
   */
  setMovementMode(entity: PhysicsEntity, mode: MovementMode): void {
    // Fly mode only available in editor
    if (mode === 'fly' && !__EDITOR__) {
      logger.warn('Fly mode only available in Editor build');
      return;
    }

    entity.movementMode = mode;
    logger.info('Movement mode changed', { entityId: entity.entityId, mode });

    // Reset velocity when switching modes
    entity.velocity.set(0, 0, 0);

    // In fly mode, no ground state
    if (mode === 'fly') {
      entity.isOnGround = false;
    }
  }

  /**
   * Toggle between Walk and Fly modes
   */
  toggleMovementMode(entity: PhysicsEntity): void {
    const newMode = entity.movementMode === 'walk' ? 'fly' : 'walk';
    this.setMovementMode(entity, newMode);
  }

  /**
   * Get current move speed for entity
   *
   * Returns appropriate speed based on movement mode, underwater state, and PlayerEntity cached values.
   * For PlayerEntity: Uses cached effective values (updated via 'playerInfo:updated' event)
   * For other entities: Uses default constants
   *
   * TODO: Add support for sprint/crawl/riding states
   */
  getMoveSpeed(entity: PhysicsEntity): number {
    if (isPlayerEntity(entity)) {
      // Player: Use cached effective values (performance optimized)
      if (this.isUnderwater) {
        return entity.effectiveUnderwaterSpeed;
      }

      // TODO: Check player state for speed modifiers
      // if (entity.isSprinting) return entity.effectiveRunSpeed;
      // if (entity.isCrouching) return entity.effectiveCrawlSpeed;
      // if (entity.isRiding) return entity.effectiveRidingSpeed;

      return entity.movementMode === 'walk'
        ? entity.effectiveWalkSpeed
        : entity.effectiveRunSpeed; // Fly mode uses run speed
    } else {
      // Other entities: Use default constants
      if (this.isUnderwater) {
        return this.defaultUnderwaterSpeed;
      }

      return entity.movementMode === 'walk' ? this.defaultWalkSpeed : this.defaultFlySpeed;
    }
  }

  /**
   * Dispose physics service
   */
  dispose(): void {
    this.entities.clear();
    this.lastCheckedBlockCoords.clear();
    logger.info('PhysicsService disposed');
  }
}
