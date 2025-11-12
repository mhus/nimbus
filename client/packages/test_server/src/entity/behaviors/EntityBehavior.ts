/**
 * EntityBehavior - Base class for entity simulation behaviors
 *
 * Behaviors define how entities move and act in the world.
 */

import type { EntityPathway, Vector3, Rotation } from '@nimbus/shared';
import type { ServerEntitySpawnDefinition } from '@nimbus/shared';
import type { WorldManager } from '../../world/WorldManager';

/**
 * EntityBehavior - Abstract base class for entity behaviors
 */
export abstract class EntityBehavior {
  /** Behavior name/type */
  abstract readonly behaviorType: string;

  /** WorldManager for ground height calculation */
  protected worldManager: WorldManager | null = null;

  /**
   * Update behavior and generate new pathway if needed
   *
   * @param entity Entity spawn definition
   * @param currentTime Current server timestamp
   * @param worldId World ID for ground height calculation
   * @returns New pathway if generated, null otherwise
   */
  abstract update(
    entity: ServerEntitySpawnDefinition,
    currentTime: number,
    worldId: string
  ): Promise<EntityPathway | null>;

  /**
   * Check if entity needs new pathway
   *
   * @param entity Entity spawn definition
   * @param currentTime Current server timestamp
   * @returns True if new pathway should be generated
   */
  protected needsNewPathway(entity: ServerEntitySpawnDefinition, currentTime: number): boolean {
    if (!entity.currentPathway) {
      return true;
    }

    const pathway = entity.currentPathway;
    if (pathway.waypoints.length === 0) {
      return true;
    }

    // Check if pathway is finished (current time past last waypoint)
    const lastWaypoint = pathway.waypoints[pathway.waypoints.length - 1];
    return currentTime >= lastWaypoint.timestamp;
  }

  /**
   * Generate random position within radius
   *
   * @param center Center point
   * @param radius Radius
   * @returns Random position
   */
  protected randomPositionInRadius(center: Vector3, radius: number): Vector3 {
    const angle = Math.random() * Math.PI * 2;
    const distance = Math.random() * radius;

    return {
      x: center.x + Math.cos(angle) * distance,
      y: center.y,
      z: center.z + Math.sin(angle) * distance,
    };
  }

  /**
   * Calculate rotation from one position to another
   *
   * @param from Start position
   * @param to End position
   * @returns Rotation in degrees (yaw, pitch)
   */
  protected calculateRotation(from: Vector3, to: Vector3): Rotation {
    const dx = to.x - from.x;
    const dz = to.z - from.z;
    const dy = to.y - from.y;

    // Yaw (horizontal angle) in radians
    let yawRad = Math.atan2(dx, dz);

    // Pitch (vertical angle) in radians
    const horizontalDistance = Math.sqrt(dx * dx + dz * dz);
    const pitchRad = Math.atan2(dy, horizontalDistance);

    // Optimize rotation direction (choose shortest path):
    // Check if angle is close to PI (±180°) - if so, flip
    // Range: 2.79-3.49 rad (160°-200°) → flip by adding/subtracting PI
    if (Math.abs(yawRad) >= 2.79 && Math.abs(yawRad) <= 3.49) {
      // Close to ±180° - flip instead of rotating
      yawRad = yawRad > 0 ? yawRad - Math.PI : yawRad + Math.PI;
    }
    // Range: -0.35 to 0.35 rad (±20°) → keep as is (small angle)
    // Range: 0.35 to PI rad (20° to 180°) → rotate left (keep positive)
    // Range: -PI to -0.35 rad (-180° to -20°) → rotate right (keep negative)
    // The interpolation will automatically choose the shortest path

    // Convert to degrees
    const yawDeg = (yawRad * 180) / Math.PI;
    const pitchDeg = (pitchRad * 180) / Math.PI;

    return {
      y: yawDeg,
      p: pitchDeg,
    };
  }

  /**
   * Calculate distance between two positions
   *
   * @param a Position A
   * @param b Position B
   * @returns Distance
   */
  protected distance(a: Vector3, b: Vector3): number {
    const dx = b.x - a.x;
    const dy = b.y - a.y;
    const dz = b.z - a.z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  /**
   * Set WorldManager for ground height calculation
   */
  setWorldManager(worldManager: WorldManager): void {
    this.worldManager = worldManager;
  }

  /**
   * Get ground height at position
   * Returns the highest solid block Y coordinate at (x, z)
   */
  protected async getGroundHeight(worldId: string, x: number, z: number): Promise<number> {
    if (!this.worldManager) {
      return 64; // Default ground level
    }

    try {
      // Start from reasonable height and scan downward
      for (let y = 128; y >= 0; y--) {
        const block = await this.worldManager.getBlock(worldId, Math.floor(x), y, Math.floor(z));
        if (block && block.blockTypeId !== 0) {
          // Found solid block, return Y + 1 (top of block)
          return y + 1;
        }
      }

      // No ground found, use default
      return 64;
    } catch (error) {
      // Error getting ground, use default
      return 64;
    }
  }
}
