/**
 * CollisionDetector - Collision detection and resolution
 *
 * Implements:
 * - Swept-AABB collision (Y → X → Z order)
 * - PassableFrom logic for one-way blocks and thin walls
 * - Semi-solid/slope collision
 * - Auto-climb detection
 */

import { Vector3 } from '@babylonjs/core';
import { getLogger, Direction } from '@nimbus/shared';
import type { PhysicsEntity, PlayerBlockContext } from './types';
import type { ChunkService } from '../ChunkService';
import { BlockContextAnalyzer } from './BlockContextAnalyzer';
import { SurfaceAnalyzer } from './SurfaceAnalyzer';
import * as PhysicsUtils from './PhysicsUtils';

const logger = getLogger('CollisionDetector');

/**
 * Entity dimensions
 */
interface EntityDimensions {
  height: number;
  width: number;
  footprint: number;
}

/**
 * Collision event callback
 */
export type CollisionEventCallback = (x: number, y: number, z: number, action: string, id?: string, gId?: number) => void;

/**
 * CollisionDetector - Handles all collision detection
 */
export class CollisionDetector {
  private surfaceAnalyzer: SurfaceAnalyzer;
  private collisionEventCallback?: CollisionEventCallback;

  constructor(
    private chunkService: ChunkService,
    private contextAnalyzer: BlockContextAnalyzer,
    private maxClimbHeight: number = 0.1
  ) {
    this.surfaceAnalyzer = new SurfaceAnalyzer(chunkService);
  }

  /**
   * Set callback for collision events
   */
  setCollisionEventCallback(callback: CollisionEventCallback): void {
    this.collisionEventCallback = callback;
  }

  /**
   * Trigger collision event if block has collisionEvent flag
   *
   * @param blockInfo Block that was collided with
   * @param action Action type ('collision', 'climb')
   */
  private triggerCollisionEvent(blockInfo: any, action: string = 'collision'): void {
    if (!this.collisionEventCallback) {
      return;
    }

    if (!blockInfo.block) {
      return;
    }

    const physics = blockInfo.block.currentModifier.physics;
    if (!physics?.collisionEvent) {
      return;
    }

    // Send collision event
    this.collisionEventCallback(
      blockInfo.x,
      blockInfo.y,
      blockInfo.z,
      action,
      blockInfo.block.block.metadata?.id,
      blockInfo.block.block.metadata?.groupId
    );

    logger.debug('Collision event triggered', {
      position: { x: blockInfo.x, y: blockInfo.y, z: blockInfo.z },
      action,
      id: blockInfo.block.block.metadata?.id,
    });
  }

  /**
   * Check and resolve collisions for movement
   * Uses Swept-AABB approach: Y → X → Z
   *
   * @param entity Entity to check
   * @param wishPosition Desired next position
   * @param dimensions Entity dimensions
   * @returns Resolved position after collision
   */
  resolveCollision(
    entity: PhysicsEntity,
    wishPosition: Vector3,
    dimensions: EntityDimensions
  ): Vector3 {
    const resolved = wishPosition.clone();

    // Phase 1: Vertical collision (Y)
    resolved.y = this.resolveVerticalCollision(entity, resolved, dimensions);

    // Phase 2: Horizontal collision (X, Z)
    const horizontal = this.resolveHorizontalCollision(entity, resolved, dimensions);
    resolved.x = horizontal.x;
    resolved.z = horizontal.z;

    return resolved;
  }

  /**
   * Resolve vertical (Y-axis) collision
   */
  private resolveVerticalCollision(
    entity: PhysicsEntity,
    wishPosition: Vector3,
    dimensions: EntityDimensions
  ): number {
    const currentY = entity.position.y;
    const wishY = wishPosition.y;
    const movingUp = wishY > currentY;

    // Get blocks above/below
    const context = this.contextAnalyzer.getContext(entity, dimensions);

    if (movingUp) {
      // Check head collision
      if (context.headBlocks.hasSolid) {
        // Hit ceiling - clamp to block bottom
        const ceilingY = context.headBlocks.maxY;
        if (wishY + dimensions.height > ceilingY) {
          entity.velocity.y = 0;

          // Trigger collision events for head blocks
          for (const blockInfo of context.headBlocks.blocks) {
            this.triggerCollisionEvent(blockInfo);
          }

          return ceilingY - dimensions.height;
        }
      }
    } else {
      // Check ground collision
      if (context.groundBlocks.hasGround) {
        const groundY = context.groundBlocks.groundY + 1.0; // Top of ground block
        if (wishY < groundY) {
          entity.velocity.y = 0;
          entity.grounded = true;

          // Trigger collision events for ground blocks
          for (const blockInfo of context.groundBlocks.blocks) {
            this.triggerCollisionEvent(blockInfo);
          }

          return groundY;
        }
      }

      // Check for semi-solid blocks (slopes)
      if (context.groundFootBlocks.isSemiSolid && context.groundFootBlocks.maxHeight > 0) {
        const surfaceY = Math.floor(currentY) + 1.0 + context.groundFootBlocks.maxHeight;
        if (wishY < surfaceY) {
          entity.velocity.y = 0;
          entity.grounded = true;
          entity.onSlope = true;
          return surfaceY;
        }
      }
    }

    return wishY;
  }

  /**
   * Resolve horizontal (X, Z) collision
   */
  private resolveHorizontalCollision(
    entity: PhysicsEntity,
    wishPosition: Vector3,
    dimensions: EntityDimensions
  ): { x: number; z: number } {
    let x = wishPosition.x;
    let z = wishPosition.z;

    const dx = x - entity.position.x;
    const dz = z - entity.position.z;

    // No horizontal movement
    if (Math.abs(dx) < 0.001 && Math.abs(dz) < 0.001) {
      return { x, z };
    }

    // Check if moving into solid blocks
    const frontBlocks = this.getFrontBlocks(entity, dimensions, dx, dz);

    for (const blockInfo of frontBlocks) {
      if (!blockInfo.block) continue;

      const physics = blockInfo.block.currentModifier.physics;
      if (!physics?.solid) continue;

      // Get movement direction
      const dir = PhysicsUtils.getMovementDirection(dx, dz);

      // Check passableFrom
      if (physics.passableFrom !== undefined) {
        // Solid + passableFrom: One-way gate
        if (!PhysicsUtils.canEnterFrom(physics.passableFrom, dir, true)) {
          // Blocked - stop ALL horizontal movement (prevent sliding)
          x = entity.position.x;
          z = entity.position.z;
          entity.velocity.x = 0;
          entity.velocity.z = 0;

          // Trigger collision event if enabled
          this.triggerCollisionEvent(blockInfo);
          break;
        }
      } else {
        // Regular solid block - check if can climb

        // Check 1: Slope blocks with low corner heights (always passable)
        const cornerHeights = this.surfaceAnalyzer.getCornerHeights(blockInfo.block);
        if (cornerHeights) {
          const maxHeight = Math.max(...cornerHeights);
          if (maxHeight <= this.maxClimbHeight) {
            // Low slope - can step over without climb
            continue;
          }
        }

        // Check 2: Full 1-block step - check autoClimbable property
        const blockHeight = blockInfo.block.block.position.y + 1.0; // Top of block
        const currentY = entity.position.y;
        const heightDiff = blockHeight - currentY;

        // If height difference <= 1.0 and block has autoClimbable property (or is undefined = default true)
        if (heightDiff > 0 && heightDiff <= 1.0) {
          // Check if autoClimbable is explicitly set to false
          if (physics.autoClimbable === false) {
            // Explicitly disabled - cannot climb
            // Continue to blocking logic below
          } else {
            // autoClimbable is true or undefined (default: allow climbing)
            // Trigger climb event if enabled
            this.triggerCollisionEvent(blockInfo, 'climb');

            // WalkModeController will adjust Y position
            continue;
          }
        }

        // Check 3: Alternative - if physical dimensions height >= 1.5, allow climbing 1-block
        // (This makes taller entities able to step over 1-block obstacles naturally)
        if (heightDiff > 0 && heightDiff <= 1.0 && dimensions.height >= 1.5) {
          // Trigger climb event if enabled
          this.triggerCollisionEvent(blockInfo, 'climb');

          // Tall entity can step over 1-block obstacles
          continue;
        }

        // Cannot pass - stop ALL horizontal movement
        x = entity.position.x;
        z = entity.position.z;
        entity.velocity.x = 0;
        entity.velocity.z = 0;

        // Trigger collision event if enabled
        this.triggerCollisionEvent(blockInfo);
        break;
      }
    }

    // Check current block passableFrom (wall barriers)
    const currentContext = this.contextAnalyzer.getContext(entity, dimensions);
    if (currentContext.currentBlocks.passableFrom !== undefined) {
      const exitDir = PhysicsUtils.getMovementDirection(dx, dz);
      const isSolid = currentContext.currentBlocks.hasSolid;

      if (!PhysicsUtils.canLeaveTo(currentContext.currentBlocks.passableFrom, exitDir, isSolid)) {
        // Cannot leave in this direction (thin wall) - stop ALL movement
        x = entity.position.x;
        z = entity.position.z;
        entity.velocity.x = 0;
        entity.velocity.z = 0;
      }
    }

    return { x, z };
  }

  /**
   * Get blocks in front of entity for collision check
   */
  private getFrontBlocks(
    entity: PhysicsEntity,
    dimensions: EntityDimensions,
    dx: number,
    dz: number
  ) {
    const blocks = [];
    const feetY = Math.floor(entity.position.y);
    const numLevels = Math.ceil(dimensions.height);

    // Determine front direction
    const frontX = dx > 0 ? 1 : dx < 0 ? -1 : 0;
    const frontZ = dz > 0 ? 1 : dz < 0 ? -1 : 0;

    // Check footprint corners
    const footprint = dimensions.footprint;
    const corners = [
      { x: entity.position.x - footprint, z: entity.position.z - footprint },
      { x: entity.position.x + footprint, z: entity.position.z - footprint },
      { x: entity.position.x + footprint, z: entity.position.z + footprint },
      { x: entity.position.x - footprint, z: entity.position.z + footprint },
    ];

    for (const corner of corners) {
      const blockX = Math.floor(corner.x + frontX);
      const blockZ = Math.floor(corner.z + frontZ);

      for (let dy = 0; dy < numLevels; dy++) {
        const block = this.chunkService.getBlockAt(blockX, feetY + dy, blockZ);
        if (block) {
          blocks.push({ x: blockX, y: feetY + dy, z: blockZ, block });
        }
      }
    }

    return blocks;
  }

  /**
   * Check if entity is stuck in solid block and needs push-up
   */
  checkAndPushUp(entity: PhysicsEntity, dimensions: EntityDimensions): boolean {
    const context = this.contextAnalyzer.getContext(entity, dimensions);

    // If inside solid block, push up
    if (context.currentBlocks.hasSolid && !context.currentBlocks.allNonSolid) {
      // Check if space above is clear
      if (!context.headBlocks.hasSolid) {
        entity.position.y += 1.0;
        entity.velocity.y = 0;
        logger.debug('Pushed entity up - was inside solid block', {
          entityId: entity.entityId,
        });
        return true;
      }
    }

    return false;
  }

  /**
   * Check ground collision and update grounded state
   */
  checkGroundCollision(entity: PhysicsEntity, dimensions: EntityDimensions): void {
    const context = this.contextAnalyzer.getContext(entity, dimensions);

    // Check if on ground
    if (context.groundBlocks.hasGround) {
      const groundY = context.groundBlocks.groundY + 1.0;
      const distanceToGround = entity.position.y - groundY;

      if (distanceToGround < 0.01) {
        entity.grounded = true;
        entity.position.y = groundY; // Snap to ground
        if (entity.velocity.y < 0) {
          entity.velocity.y = 0;
        }
      } else {
        entity.grounded = false;
      }
    } else {
      entity.grounded = false;
    }

    // Check for slopes
    entity.onSlope = context.groundFootBlocks.isSemiSolid;
  }

  /**
   * Check if entity can trigger auto-jump
   */
  canAutoJump(entity: PhysicsEntity, dimensions: EntityDimensions): boolean {
    const context = this.contextAnalyzer.getContext(entity, dimensions);

    return (
      context.footBlocks.hasAutoJump ||
      context.groundBlocks.hasAutoJump ||
      context.currentBlocks.hasSolid
    );
  }

  /**
   * Check if entity is in climbable block
   */
  isClimbing(entity: PhysicsEntity, dimensions: EntityDimensions): boolean {
    const context = this.contextAnalyzer.getContext(entity, dimensions);

    // Check if in climbable block (ladder)
    for (const blockInfo of context.currentBlocks.blocks) {
      if (blockInfo.block?.currentModifier.physics?.climbable) {
        return true;
      }
    }

    return false;
  }

  /**
   * Check and resolve collisions with entities
   *
   * @param playerPosition Player's current position
   * @param playerDimensions Player's dimensions
   * @param entities Entities to check collision against
   * @returns Object with corrected position and list of collided entity IDs
   */
  checkEntityCollisions(
    playerPosition: Vector3,
    playerDimensions: EntityDimensions,
    entities: Array<{
      entityId: string;
      position: Vector3;
      dimensions: { height: number; width: number; footprint: number };
      solid: boolean;
    }>
  ): { position: Vector3; collidedEntities: string[] } {
    const collidedEntities: string[] = [];
    let correctedPosition = playerPosition.clone();

    for (const entity of entities) {
      // Check 2D circle collision (X, Z plane)
      const dx = entity.position.x - correctedPosition.x;
      const dz = entity.position.z - correctedPosition.z;
      const distance2D = Math.sqrt(dx * dx + dz * dz);

      // Calculate minimum distance (sum of radii)
      const minDistance = (entity.dimensions.footprint / 2) + (playerDimensions.footprint / 2);

      // Check if circles overlap
      if (distance2D < minDistance) {
        // Check Y overlap (height check)
        const playerTop = correctedPosition.y + playerDimensions.height;
        const entityTop = entity.position.y + entity.dimensions.height;

        const yOverlap = correctedPosition.y < entityTop && playerTop > entity.position.y;

        if (yOverlap) {
          // Collision detected
          collidedEntities.push(entity.entityId);

          // If entity is solid, push player away
          if (entity.solid && distance2D > 0.001) {
            // Calculate push vector (away from entity center)
            const pushDistance = minDistance - distance2D;
            const pushX = (dx / distance2D) * pushDistance;
            const pushZ = (dz / distance2D) * pushDistance;

            // Apply push (move player away from entity)
            correctedPosition.x -= pushX;
            correctedPosition.z -= pushZ;
          }
        }
      }
    }

    return {
      position: correctedPosition,
      collidedEntities,
    };
  }
}
