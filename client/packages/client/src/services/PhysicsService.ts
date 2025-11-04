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
   * Update all entities
   */
  update(deltaTime: number): void {
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

    // Check if player is trying to climb (flag set by tryMoveHorizontal)
    const isClimbing = this.climbableVelocitySetThisFrame.get(entity.entityId) || false;

    // Apply gravity (reduced when underwater, disabled when climbing)
    if (!this.isUnderwater && !isClimbing) {
      // Normal gravity on land
      if (!entity.isOnGround) {
        entity.velocity.y += this.gravity * deltaTime;
      } else {
        // On ground, reset vertical velocity
        entity.velocity.y = 0;
      }
    } else if (this.isUnderwater) {
      // Underwater: Reduced gravity (slow sinking)
      // Player slowly sinks but can swim up
      entity.velocity.y += this.underwaterGravity * deltaTime;

      // Apply water drag to limit sink speed
      entity.velocity.y *= 0.95; // Damping factor
    } else if (isClimbing) {
      // Climbing: No gravity, no damping
      // Velocity.y was already set by tryMoveHorizontal
      // Just keep it as is
    }

    // Apply velocity to position
    if (entity.velocity.lengthSquared() > 0) {
      entity.position.addInPlace(entity.velocity.scale(deltaTime));
    }

    // Clamp to world boundaries
    this.clampToWorldBounds(entity);

    // Ground check with block collision
    this.checkGroundCollision(entity);

    // Auto-push-up if inside block
    this.checkAndPushUp(entity);

    // Auto-jump if standing on/in autoJump block
    this.checkAutoJump(entity);

    // Auto-move if standing on/in autoMove block
    this.applyAutoMove(entity, deltaTime);

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
   * Check if movement is passable based on passableFrom flags
   *
   * @param passableFrom Direction bitfield from block
   * @param movementDir Direction of entity movement
   * @param isSolid Whether the block is solid
   * @returns true if passage is allowed, false if blocked
   */
  private isPassable(passableFrom: number | undefined, movementDir: Direction, isSolid: boolean): boolean {
    // No passableFrom set - use default behavior
    if (passableFrom === undefined || passableFrom === 0) {
      return !isSolid; // Solid blocks block, non-solid blocks allow
    }

    // passableFrom is set - check movement direction
    const canPassFrom = DirectionHelper.hasDirection(passableFrom, movementDir);

    if (isSolid) {
      // Solid block with passableFrom: One-way block
      // Can enter from specified directions
      return canPassFrom;
    } else {
      // Non-solid block with passableFrom: Wall behavior
      // Can move through but not exit where not specified
      // Since we're checking entry, allow entry from anywhere
      // Exit blocking will be handled when trying to leave
      return true;
    }
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
            belowLevel.groundY = checkY + 1; // Top of the block
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
          const blockTopY = checkY + 1;

          // Only snap if we're falling and at or below this level
          if (feetY <= blockTopY + 0.1) {
            foundGround = true;
            groundY = blockTopY;
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
  private applyAutoMove(entity: PhysicsEntity, deltaTime: number): void {
    if (!this.chunkService) {
      return;
    }

    // Only apply autoMove in walk mode
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

    // Collect autoMove from both belowLevel (standing on) and currentLevel (at feet)
    const autoMoveX = Math.max(Math.abs(context.belowLevel.autoMove.x), Math.abs(context.currentLevel.autoMove.x)) *
                      (Math.abs(context.belowLevel.autoMove.x) > Math.abs(context.currentLevel.autoMove.x)
                        ? Math.sign(context.belowLevel.autoMove.x)
                        : Math.sign(context.currentLevel.autoMove.x));

    const autoMoveY = Math.max(Math.abs(context.belowLevel.autoMove.y), Math.abs(context.currentLevel.autoMove.y)) *
                      (Math.abs(context.belowLevel.autoMove.y) > Math.abs(context.currentLevel.autoMove.y)
                        ? Math.sign(context.belowLevel.autoMove.y)
                        : Math.sign(context.currentLevel.autoMove.y));

    const autoMoveZ = Math.max(Math.abs(context.belowLevel.autoMove.z), Math.abs(context.currentLevel.autoMove.z)) *
                      (Math.abs(context.belowLevel.autoMove.z) > Math.abs(context.currentLevel.autoMove.z)
                        ? Math.sign(context.belowLevel.autoMove.z)
                        : Math.sign(context.currentLevel.autoMove.z));

    // Apply autoMove velocity if any axis has movement
    if (autoMoveX !== 0 || autoMoveY !== 0 || autoMoveZ !== 0) {
      entity.position.x += autoMoveX * deltaTime;
      entity.position.y += autoMoveY * deltaTime;
      entity.position.z += autoMoveZ * deltaTime;

      logger.debug('Auto-move applied', {
        entityId: entity.entityId,
        velocity: { x: autoMoveX, y: autoMoveY, z: autoMoveZ },
      });
    }
  }

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

    // Determine movement direction
    const movementDir = this.getMovementDirection(dx, dz);

    // Get current block context for exit checks
    const currentContext = this.getPlayerBlockContext(entity.position.x, entity.position.z, feetY, entityWidth, entityHeight);

    // Exit check: Check if we're leaving a non-solid block with passableFrom
    // For non-solid blocks, passableFrom acts as walls at edges
    // Use already collected blocks from currentContext
    const currentBlockX = Math.floor(entity.position.x);
    const currentBlockZ = Math.floor(entity.position.z);
    const targetBlockX = Math.floor(targetX);
    const targetBlockZ = Math.floor(targetZ);

    // Check if we're crossing a block boundary
    const crossingBoundary = currentBlockX !== targetBlockX || currentBlockZ !== targetBlockZ;

    if (crossingBoundary) {
      // Check blocks from currentContext for non-solid passableFrom
      for (const blockInfo of currentContext.currentLevel.blocks) {
        const physics = blockInfo.block.currentModifier.physics;

        // If this block is non-solid with passableFrom, check if we can exit
        if (physics?.passableFrom !== undefined && !physics.solid) {
          const canExit = DirectionHelper.hasDirection(physics.passableFrom, movementDir);

          logger.debug('Exit check: crossing boundary from non-solid passableFrom block', {
            entityId: entity.entityId,
            currentBlock: { x: blockInfo.x, y: blockInfo.y, z: blockInfo.z },
            targetBlock: { x: targetBlockX, z: targetBlockZ },
            passableFrom: physics.passableFrom,
            movementDir,
            canExit,
            dx,
            dz,
          });

          if (!canExit) {
            // Cannot exit in this direction - blocked by wall at edge
            logger.debug('Movement BLOCKED: cannot exit non-solid passableFrom block in this direction');
            return true; // Movement blocked
          }
        }
      }
    }

    // Entry check: Check passableFrom logic at target position
    if (context.currentLevel.passableFrom !== undefined) {
      if (context.currentLevel.hasSolid) {
        // Solid block with passableFrom: One-way block
        // Can only enter from specified directions (from outside)
        // BUT: If already inside, allow free movement
        const alreadyInside = currentContext.currentLevel.hasSolid &&
                              currentContext.currentLevel.passableFrom !== undefined;

        const canEnter = DirectionHelper.hasDirection(context.currentLevel.passableFrom, movementDir);

        if (!canEnter && !alreadyInside) {
          // Cannot enter from this direction (and not already inside)
          return true; // Movement blocked
        }
        // Can pass through - allow movement (either can enter OR already inside)
        entity.position.x = targetX;
        entity.position.z = targetZ;
        return false;
      } else {
        // Non-solid block with passableFrom: Wall at edges
        // Can only enter from directions specified in passableFrom
        const canEnter = DirectionHelper.hasDirection(context.currentLevel.passableFrom, movementDir);
        if (!canEnter) {
          // Cannot enter from this direction - blocked by wall at edge
          return true; // Movement blocked
        }
        // Entry is allowed, also check if we're leaving a passableFrom block
        // (exit check already done above)
        entity.position.x = targetX;
        entity.position.z = targetZ;
        return false;
      }
    }

    // No passableFrom at target - check if we're exiting a passableFrom block
    // If currentContext had passableFrom (non-solid), exit check already passed above
    // So we can move freely here

    // No passableFrom - standard collision logic
    // No collision at current level - move freely
    if (!context.currentLevel.hasSolid) {
      entity.position.x = targetX;
      entity.position.z = targetZ;
      return false; // Normal movement occurred
    }

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
