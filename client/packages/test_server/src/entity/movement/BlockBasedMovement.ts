/**
 * BlockBasedMovement - Block-based pathfinding for entities
 *
 * Entities move from block to block, following terrain naturally:
 * - Handles stairs (1 block up)
 * - Handles slopes (1 block down)
 * - Avoids obstacles (solid blocks)
 * - No continuous physics, just block-to-block movement
 */

import type { Vector3 } from '@nimbus/shared';
import type { Rotation } from '@nimbus/shared';
import type { Waypoint } from '@nimbus/shared';
import type { WorldManager } from '../../world/WorldManager';
import { getLogger, Shape } from '@nimbus/shared';

const logger = getLogger('BlockBasedMovement');

/**
 * Direction for movement (cardinal directions + diagonals)
 */
export interface Direction {
  dx: number; // -1, 0, or 1
  dz: number; // -1, 0, or 1
}

/**
 * BlockBasedMovement - Handles block-based pathfinding
 */
export class BlockBasedMovement {
  private worldManager: WorldManager | null = null;

  constructor(worldManager?: WorldManager) {
    this.worldManager = worldManager ?? null;
  }

  /**
   * Set WorldManager for block queries
   */
  setWorldManager(worldManager: WorldManager): void {
    this.worldManager = worldManager;
  }

  /**
   * Check if a block is water (OCEAN shapes)
   * @param worldId World ID
   * @param x Block X coordinate
   * @param y Block Y coordinate
   * @param z Block Z coordinate
   * @returns true if block is water
   */
  private async isWaterBlock(worldId: string, x: number, y: number, z: number): Promise<boolean> {
    if (!this.worldManager) {
      return false;
    }

    const block = await this.worldManager.getBlock(worldId, x, y, z);
    if (!block) {
      return false;
    }

    const blockType = this.worldManager.getBlockTypeRegistry().getBlockType(block.blockTypeId);
    if (!blockType) {
      return false;
    }

    // Get default modifier (status 0)
    const modifier = blockType.modifiers[0];
    if (!modifier || !modifier.visibility) {
      return false;
    }

    const shape = modifier.visibility.shape;
    return shape === Shape.OCEAN ||
           shape === Shape.OCEAN_COAST ||
           shape === Shape.OCEAN_MAELSTROM;
  }

  /**
   * Find start position (on top of highest solid block)
   *
   * @param worldId World ID
   * @param x World X coordinate
   * @param z World Z coordinate
   * @param maxHeight Maximum search height
   * @returns Y position on top of solid block
   */
  async findStartPosition(
    worldId: string,
    x: number,
    z: number,
    maxHeight: number = 128
  ): Promise<number> {
    if (!this.worldManager) {
      return 64; // Default
    }

    const floorX = Math.floor(x);
    const floorZ = Math.floor(z);

    try {
      // Search downward for first solid block
      for (let y = maxHeight; y >= 0; y--) {
        const block = await this.worldManager.getBlock(worldId, floorX, y, floorZ);
        if (block && block.blockTypeId !== '0') {
          // Found solid block, stand on top
          return y + 1;
        }
      }
    } catch (error) {
      logger.warn('Failed to find start position', { x: floorX, z: floorZ });
    }

    return 64; // Default fallback
  }

  /**
   * Find next valid waypoint from current position in given direction
   *
   * @param worldId World ID
   * @param from Current position
   * @param direction Direction to move (dx, dz)
   * @returns Next waypoint or null if blocked
   */
  async findNextWaypoint(
    worldId: string,
    from: Vector3,
    direction: Direction
  ): Promise<Vector3 | null> {
    if (!this.worldManager) {
      return null;
    }

    const x = Math.floor(from.x);
    const y = Math.floor(from.y);
    const z = Math.floor(from.z);

    // Target position (1 block in direction)
    const targetX = x + direction.dx;
    const targetZ = z + direction.dz;

    try {
      // Check block at target position (same height)
      const blockAtTarget = await this.worldManager.getBlock(worldId, targetX, y, targetZ);
      const targetSolid = blockAtTarget ? blockAtTarget.blockTypeId !== '0' : false;

      // Check if target block is OCEAN - entities should not enter water
      if (await this.isWaterBlock(worldId, targetX, y, targetZ)) {
        return null; // Blocked by water
      }

      if (targetSolid) {
        // Block at target is solid → try stepping UP
        const blockAbove = await this.worldManager.getBlock(worldId, targetX, y + 1, targetZ);
        const aboveSolid = blockAbove ? blockAbove.blockTypeId !== '0' : false;

        if (!aboveSolid) {
          // Block above is not solid → can step up
          return { x: targetX + 0.5, y: y + 1, z: targetZ + 0.5 };
        } else {
          // Blocked by wall (2 blocks high)
          return null;
        }
      } else {
        // Block at target is NOT solid → check block BELOW
        const blockBelow = await this.worldManager.getBlock(worldId, targetX, y - 1, targetZ);
        const belowSolid = blockBelow ? blockBelow.blockTypeId !== '0' : false;

        // Check if block below is OCEAN - entities should not walk into water
        if (await this.isWaterBlock(worldId, targetX, y - 1, targetZ)) {
          return null; // Blocked by water below
        }

        if (belowSolid) {
          // Block below is solid → same height (walk on flat ground)
          return { x: targetX + 0.5, y: y, z: targetZ + 0.5 };
        } else {
          // No block below → check 2 blocks down (step down)
          const blockBelow2 = await this.worldManager.getBlock(worldId, targetX, y - 2, targetZ);
          const below2Solid = blockBelow2 ? blockBelow2.blockTypeId !== '0' : false;

          // Check if 2 blocks below is OCEAN
          if (await this.isWaterBlock(worldId, targetX, y - 2, targetZ)) {
            return null; // Blocked by water 2 blocks down
          }

          if (below2Solid) {
            // Block 2 down is solid → step down 1 block
            return { x: targetX + 0.5, y: y - 1, z: targetZ + 0.5 };
          } else {
            // Too deep (no ground within 2 blocks) → blocked
            return null;
          }
        }
      }
    } catch (error) {
      logger.warn('Failed to find next waypoint', {
        from,
        direction,
        error: error instanceof Error ? error.message : 'Unknown error'
      });
      return null;
    }
  }

  /**
   * Generate pathway with multiple waypoints
   *
   * @param worldId World ID
   * @param startPosition Starting position
   * @param targetDirection General direction to move
   * @param waypointCount Number of waypoints to generate
   * @param speed Movement speed (blocks per second)
   * @param startTime Start timestamp
   * @returns Array of waypoints
   */
  async generatePathway(
    worldId: string,
    startPosition: Vector3,
    targetDirection: Direction,
    waypointCount: number,
    speed: number,
    startTime: number
  ): Promise<Waypoint[]> {
    const waypoints: Waypoint[] = [];
    let currentPosition = { ...startPosition };
    let currentTime = startTime;

    for (let i = 0; i < waypointCount; i++) {
      // Find next waypoint in direction
      const nextPosition = await this.findNextWaypoint(worldId, currentPosition, targetDirection);

      if (!nextPosition) {
        // Blocked - stop generating waypoints
        logger.debug('Movement blocked, stopping pathway generation', {
          currentPosition,
          direction: targetDirection,
          waypointsGenerated: waypoints.length,
        });
        break;
      }

      // Calculate distance (approximately 1 block, but could vary with height changes)
      const dx = nextPosition.x - currentPosition.x;
      const dy = nextPosition.y - currentPosition.y;
      const dz = nextPosition.z - currentPosition.z;
      const distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

      // Calculate time to reach (distance / speed)
      const travelTime = (distance / speed) * 1000; // Convert to milliseconds
      currentTime += travelTime;

      // Calculate rotation (yaw only, no pitch for simple movement)
      // Babylon.js uses: atan2(dx, dz) where Z is forward
      const yawRad = Math.atan2(dx, dz);
      const yaw = yawRad * (180 / Math.PI);

      // Create waypoint
      waypoints.push({
        timestamp: currentTime,
        target: nextPosition,
        rotation: { y: yaw, p: 0 },
        pose: 1, // WALK pose
      });

      // Update current position for next iteration
      currentPosition = nextPosition;
    }

    return waypoints;
  }

  /**
   * Get random cardinal or diagonal direction
   */
  getRandomDirection(): Direction {
    const directions: Direction[] = [
      { dx: 1, dz: 0 },   // East
      { dx: -1, dz: 0 },  // West
      { dx: 0, dz: 1 },   // South
      { dx: 0, dz: -1 },  // North
      { dx: 1, dz: 1 },   // Southeast
      { dx: 1, dz: -1 },  // Northeast
      { dx: -1, dz: 1 },  // Southwest
      { dx: -1, dz: -1 }, // Northwest
    ];

    return directions[Math.floor(Math.random() * directions.length)];
  }

  /**
   * Get direction towards target (quantized to block grid)
   */
  getDirectionTowards(from: Vector3, to: Vector3): Direction {
    const dx = to.x - from.x;
    const dz = to.z - from.z;

    return {
      dx: dx > 0 ? 1 : dx < 0 ? -1 : 0,
      dz: dz > 0 ? 1 : dz < 0 ? -1 : 0,
    };
  }
}
