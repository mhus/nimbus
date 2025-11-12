/**
 * EntityBehavior - Base class for entity simulation behaviors
 *
 * Behaviors define how entities move and act in the world.
 */

import type { EntityPathway, Vector3, Rotation } from '@nimbus/shared';
import type { ServerEntitySpawnDefinition } from '@nimbus/shared';

/**
 * EntityBehavior - Abstract base class for entity behaviors
 */
export abstract class EntityBehavior {
  /** Behavior name/type */
  abstract readonly behaviorType: string;

  /**
   * Update behavior and generate new pathway if needed
   *
   * @param entity Entity spawn definition
   * @param currentTime Current server timestamp
   * @returns New pathway if generated, null otherwise
   */
  abstract update(
    entity: ServerEntitySpawnDefinition,
    currentTime: number
  ): EntityPathway | null;

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
   * @returns Rotation (yaw, pitch)
   */
  protected calculateRotation(from: Vector3, to: Vector3): Rotation {
    const dx = to.x - from.x;
    const dz = to.z - from.z;
    const dy = to.y - from.y;

    // Yaw (horizontal angle)
    const yaw = Math.atan2(dx, dz);

    // Pitch (vertical angle)
    const horizontalDistance = Math.sqrt(dx * dx + dz * dz);
    const pitch = Math.atan2(dy, horizontalDistance);

    return {
      y: yaw,
      p: pitch,
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
}
