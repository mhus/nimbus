/**
 * PreyAnimalBehavior - Slow-moving prey animal that roams around a center point
 *
 * Behavior:
 * - Moves slowly within radius around middle point
 * - Generates new pathways every 5 seconds
 * - New pathways connect to end of previous pathway
 * - Random walk pattern
 */

import { EntityBehavior } from './EntityBehavior';
import type { EntityPathway, Waypoint, ServerEntitySpawnDefinition } from '@nimbus/shared';

/**
 * PreyAnimalBehavior - Passive roaming behavior
 */
export class PreyAnimalBehavior extends EntityBehavior {
  readonly behaviorType = 'PreyAnimalBehavior';

  /** Interval between pathway generation (milliseconds) */
  private readonly pathwayInterval = 5000; // 5 seconds

  /** Number of waypoints per pathway */
  private readonly waypointsPerPathway = 3;

  /**
   * Update behavior and generate new pathway if needed
   */
  update(entity: ServerEntitySpawnDefinition, currentTime: number): EntityPathway | null {
    // Check if we need a new pathway
    if (!this.needsNewPathway(entity, currentTime)) {
      return null;
    }

    // Generate new pathway
    return this.generatePathway(entity, currentTime);
  }

  /**
   * Generate new pathway for entity
   */
  private generatePathway(
    entity: ServerEntitySpawnDefinition,
    currentTime: number
  ): EntityPathway {
    const waypoints: Waypoint[] = [];

    // Start position: end of previous pathway or initial position
    let currentPosition = entity.initialPosition;
    if (entity.currentPathway && entity.currentPathway.waypoints.length > 0) {
      const lastWaypoint = entity.currentPathway.waypoints[entity.currentPathway.waypoints.length - 1];
      currentPosition = lastWaypoint.target;
    }

    let currentTimestamp = currentTime;

    // Generate waypoints
    for (let i = 0; i < this.waypointsPerPathway; i++) {
      // Generate random target within radius
      const target = this.randomPositionInRadius(entity.middlePoint, entity.radius);

      // Calculate distance and time to reach
      const distance = this.distance(currentPosition, target);
      const travelTime = (distance / entity.speed) * 1000; // Convert to milliseconds

      // Calculate rotation towards target
      const rotation = this.calculateRotation(currentPosition, target);

      // Create waypoint
      currentTimestamp += travelTime;
      waypoints.push({
        timestamp: currentTimestamp,
        target,
        rotation,
        pose: this.selectPose(entity.speed),
      });

      // Update current position for next iteration
      currentPosition = target;

      // Add small pause between waypoints (idle time)
      currentTimestamp += 500 + Math.random() * 1000; // 0.5-1.5 seconds pause
    }

    // Create pathway
    return {
      entityId: entity.entityId,
      startAt: currentTime,
      waypoints,
      isLooping: false,
      idlePose: 0, // Idle pose
    };
  }

  /**
   * Select pose based on speed
   */
  private selectPose(speed: number): number {
    // Pose mapping (example):
    // 0 = idle
    // 1 = walk
    // 2 = run

    if (speed < 1) {
      return 1; // Slow walk
    } else if (speed < 3) {
      return 1; // Walk
    } else {
      return 2; // Run
    }
  }

  /**
   * Check if entity needs new pathway
   * Override to use custom interval
   */
  protected needsNewPathway(entity: ServerEntitySpawnDefinition, currentTime: number): boolean {
    if (!entity.currentPathway) {
      return true;
    }

    const pathway = entity.currentPathway;
    if (pathway.waypoints.length === 0) {
      return true;
    }

    // Check if enough time has passed since pathway start
    const timeSinceStart = currentTime - pathway.startAt;
    if (timeSinceStart < this.pathwayInterval) {
      return false;
    }

    // Check if pathway is finished
    const lastWaypoint = pathway.waypoints[pathway.waypoints.length - 1];
    return currentTime >= lastWaypoint.timestamp;
  }
}
