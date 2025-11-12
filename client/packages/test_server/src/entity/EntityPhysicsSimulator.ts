/**
 * EntityPhysicsSimulator - Lightweight physics simulation for entities on the server
 *
 * This class handles server-side physics simulation for entities:
 * - Gravity
 * - Block collision (simple AABB)
 * - Ground detection
 * - Velocity integration
 *
 * The physics is simplified compared to player physics - no slopes, auto-climb, or water physics.
 */

import type { Vector3 } from '@nimbus/shared';
import type { Rotation } from '@nimbus/shared';
import type { ServerEntitySpawnDefinition } from '@nimbus/shared';
import type { EntityModel } from '@nimbus/shared';
import type { WorldManager } from '../world/WorldManager';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('EntityPhysicsSimulator');

/**
 * Physics constants (aligned with player physics for consistency)
 */
const PHYSICS_CONSTANTS = {
  /** Gravity acceleration (blocks/s²) */
  GRAVITY: -20.0,

  /** Ground friction coefficient (velocity dampening when on ground) */
  GROUND_FRICTION: 0.8,

  /** Air drag coefficient (velocity dampening in air) */
  AIR_DRAG: 0.98,

  /** Minimum velocity magnitude to consider entity moving */
  MIN_VELOCITY: 0.001,

  /** Ground detection tolerance (blocks above ground) */
  GROUND_TOLERANCE: 0.1,

  /** Update rate for physics (ticks per second) */
  TICKS_PER_SECOND: 20,
};

/**
 * Simple AABB for collision detection
 */
interface AABB {
  minX: number;
  minY: number;
  minZ: number;
  maxX: number;
  maxY: number;
  maxZ: number;
}

/**
 * EntityPhysicsSimulator - Handles physics simulation for entities
 */
export class EntityPhysicsSimulator {
  private worldManager: WorldManager | null = null;

  constructor(worldManager?: WorldManager) {
    this.worldManager = worldManager ?? null;
  }

  /**
   * Set the WorldManager for ground detection
   */
  setWorldManager(worldManager: WorldManager): void {
    this.worldManager = worldManager;
  }

  /**
   * Initialize physics state for an entity
   */
  initializePhysicsState(
    spawnDef: ServerEntitySpawnDefinition,
    position: Vector3,
    rotation: Rotation
  ): void {
    spawnDef.physicsState = {
      position: { ...position },
      velocity: { x: 0, y: 0, z: 0 },
      rotation: { ...rotation },
      grounded: false,
    };
  }

  /**
   * Update physics for a single entity
   *
   * @param spawnDef Entity spawn definition with physics state
   * @param entityModel Entity model for dimensions
   * @param deltaTime Time step in seconds
   * @param worldId World ID for ground detection
   * @returns Updated physics state
   */
  async updatePhysics(
    spawnDef: ServerEntitySpawnDefinition,
    entityModel: EntityModel,
    deltaTime: number,
    worldId: string
  ): Promise<void> {
    if (!spawnDef.physicsState) {
      logger.warn('Entity has no physics state', { entityId: spawnDef.entityId });
      return;
    }

    const state = spawnDef.physicsState;
    const position = state.position;
    const velocity = state.velocity;

    // Get entity dimensions (use walk dimensions by default)
    const dimensions = this.getEntityDimensions(entityModel);

    // 1. Apply gravity
    velocity.y += PHYSICS_CONSTANTS.GRAVITY * deltaTime;

    // 2. Apply friction/drag
    if (state.grounded) {
      // Ground friction
      velocity.x *= PHYSICS_CONSTANTS.GROUND_FRICTION;
      velocity.z *= PHYSICS_CONSTANTS.GROUND_FRICTION;

      // Stop small movements
      if (Math.abs(velocity.x) < PHYSICS_CONSTANTS.MIN_VELOCITY) velocity.x = 0;
      if (Math.abs(velocity.z) < PHYSICS_CONSTANTS.MIN_VELOCITY) velocity.z = 0;
    } else {
      // Air drag
      velocity.x *= PHYSICS_CONSTANTS.AIR_DRAG;
      velocity.z *= PHYSICS_CONSTANTS.AIR_DRAG;
    }

    // 3. Integrate velocity (predict new position)
    const newPosition = {
      x: position.x + velocity.x * deltaTime,
      y: position.y + velocity.y * deltaTime,
      z: position.z + velocity.z * deltaTime,
    };

    // 4. Collision detection
    const collisionResult = await this.checkCollisions(
      position,
      newPosition,
      velocity,
      dimensions,
      worldId,
      spawnDef.entityId
    );

    // 5. Apply collision resolution
    state.position = collisionResult.position;
    state.velocity = collisionResult.velocity;
    state.grounded = collisionResult.grounded;

    // 6. Stop falling through ground
    if (state.grounded && state.velocity.y < 0) {
      state.velocity.y = 0;
    }
  }

  /**
   * Get entity dimensions from model
   */
  private getEntityDimensions(entityModel: EntityModel): {
    height: number;
    width: number;
    footprint: number;
  } {
    // Try walk dimensions first, fallback to other movement modes
    const dimensions = entityModel.dimensions;
    return (
      dimensions.walk ??
      dimensions.sprint ??
      dimensions.crouch ??
      dimensions.swim ??
      dimensions.fly ??
      { height: 1.8, width: 0.6, footprint: 0.6 }
    );
  }

  /**
   * Check collisions with blocks
   */
  private async checkCollisions(
    oldPosition: Vector3,
    newPosition: Vector3,
    velocity: Vector3,
    dimensions: { height: number; width: number; footprint: number },
    worldId: string,
    entityId: string
  ): Promise<{
    position: Vector3;
    velocity: Vector3;
    grounded: boolean;
  }> {
    // Create AABB for entity at new position
    const halfWidth = dimensions.footprint / 2;
    const aabb: AABB = {
      minX: newPosition.x - halfWidth,
      minY: newPosition.y,
      minZ: newPosition.z - halfWidth,
      maxX: newPosition.x + halfWidth,
      maxY: newPosition.y + dimensions.height,
      maxZ: newPosition.z + halfWidth,
    };

    // Check ground
    const groundHeight = await this.getGroundHeight(worldId, newPosition.x, newPosition.z);
    const grounded = newPosition.y <= groundHeight + PHYSICS_CONSTANTS.GROUND_TOLERANCE;

    // If grounded, snap to ground
    if (grounded) {
      newPosition.y = groundHeight;
      velocity.y = 0; // Stop falling
    }

    // Safety check: Don't let entities fall below Y=0
    if (newPosition.y < 0) {
      newPosition.y = 64; // Reset to safe height
      velocity.y = 0;
      logger.warn('Entity fell below world, resetting to safe height', {
        entityId: 'unknown',
        position: newPosition
      });
    }

    // Check X-axis collision
    const hasXCollision = await this.hasBlockCollision(
      worldId,
      { ...newPosition, x: newPosition.x },
      dimensions
    );

    if (hasXCollision) {
      newPosition.x = oldPosition.x;
      velocity.x = 0;
    }

    // Check Z-axis collision
    const hasZCollision = await this.hasBlockCollision(
      worldId,
      { ...newPosition, z: newPosition.z },
      dimensions
    );

    if (hasZCollision) {
      newPosition.z = oldPosition.z;
      velocity.z = 0;
    }

    return {
      position: newPosition,
      velocity,
      grounded,
    };
  }

  /**
   * Check if entity collides with any solid block at position
   */
  private async hasBlockCollision(
    worldId: string,
    position: Vector3,
    dimensions: { height: number; width: number; footprint: number }
  ): Promise<boolean> {
    if (!this.worldManager) {
      return false;
    }

    // Check blocks in entity's AABB (excluding ground block)
    const halfWidth = dimensions.footprint / 2;
    const minX = Math.floor(position.x - halfWidth);
    const maxX = Math.ceil(position.x + halfWidth);
    const minY = Math.floor(position.y) + 1; // Start ABOVE ground (don't check ground block)
    const maxY = Math.ceil(position.y + dimensions.height);
    const minZ = Math.floor(position.z - halfWidth);
    const maxZ = Math.ceil(position.z + halfWidth);

    for (let x = minX; x <= maxX; x++) {
      for (let y = minY; y <= maxY; y++) {
        for (let z = minZ; z <= maxZ; z++) {
          const block = await this.worldManager.getBlock(worldId, x, y, z);
          if (block && block.blockTypeId !== 'air') {
            // Simple check: non-air blocks are solid
            return true;
          }
        }
      }
    }

    return false;
  }

  /**
   * Get ground height at position
   * Returns the highest solid block Y coordinate at (x, z)
   */
  private async getGroundHeight(worldId: string, x: number, z: number): Promise<number> {
    if (!this.worldManager) {
      return 64; // Default ground level
    }

    const floorX = Math.floor(x);
    const floorZ = Math.floor(z);

    try {
      // Search downward from current position or reasonable max height
      const maxHeight = 128; // Reduced from 256 for performance
      const minHeight = 0;

      for (let y = maxHeight; y >= minHeight; y--) {
        const block = await this.worldManager.getBlock(worldId, floorX, y, floorZ);
        if (block && block.blockTypeId !== 'air') {
          return y + 1; // Stand on top of block
        }
      }
    } catch (error) {
      // If chunk loading fails, use default ground height
      // This can happen if chunk file is corrupted or doesn't exist
      return 64;
    }

    return 64; // Default ground level if nothing found
  }

  /**
   * Apply an impulse to an entity (e.g., from behavior)
   *
   * @param spawnDef Entity spawn definition
   * @param impulse Velocity impulse to apply
   */
  applyImpulse(spawnDef: ServerEntitySpawnDefinition, impulse: Vector3): void {
    if (!spawnDef.physicsState) {
      return;
    }

    const velocity = spawnDef.physicsState.velocity;
    velocity.x += impulse.x;
    velocity.y += impulse.y;
    velocity.z += impulse.z;
  }

  /**
   * Set entity velocity (e.g., for behavior-driven movement)
   *
   * @param spawnDef Entity spawn definition
   * @param velocity New velocity
   */
  setVelocity(spawnDef: ServerEntitySpawnDefinition, velocity: Vector3): void {
    if (!spawnDef.physicsState) {
      return;
    }

    spawnDef.physicsState.velocity = { ...velocity };
  }

  /**
   * Get current position
   */
  getPosition(spawnDef: ServerEntitySpawnDefinition): Vector3 | null {
    return spawnDef.physicsState?.position ?? null;
  }

  /**
   * Get current velocity
   */
  getVelocity(spawnDef: ServerEntitySpawnDefinition): Vector3 | null {
    return spawnDef.physicsState?.velocity ?? null;
  }

  /**
   * Check if entity is on ground
   */
  isGrounded(spawnDef: ServerEntitySpawnDefinition): boolean {
    return spawnDef.physicsState?.grounded ?? false;
  }
}
