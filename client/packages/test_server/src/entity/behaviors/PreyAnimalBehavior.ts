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
import { ENTITY_POSES } from '@nimbus/shared';

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
  async update(entity: ServerEntitySpawnDefinition, currentTime: number, worldId: string): Promise<EntityPathway | null> {
    // Check if we need a new pathway
    if (!this.needsNewPathway(entity, currentTime)) {
      return null;
    }

    // Generate new pathway
    return this.generatePathway(entity, currentTime, worldId);
  }

  /**
   * Generate new pathway for entity
   */
  private async generatePathway(
    entity: ServerEntitySpawnDefinition,
    currentTime: number,
    worldId: string
  ): Promise<EntityPathway> {
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
      const targetXZ = this.randomPositionInRadius(entity.middlePoint, entity.radius);

      // Get ground height at target position
      const groundY = await this.getGroundHeight(worldId, targetXZ.x, targetXZ.z);
      const target = {
        x: targetXZ.x,
        y: groundY,
        z: targetXZ.z,
      };

      // Calculate distance and time to reach
      const distance = this.distance(currentPosition, target);
      const travelTime = (distance / entity.speed) * 1000; // Convert to milliseconds

      // Calculate rotation towards target
      const rotation = this.calculateRotation(currentPosition, target);

      // Create waypoint with WALK pose
      currentTimestamp += travelTime;
      waypoints.push({
        timestamp: currentTimestamp,
        target,
        rotation,
        pose: ENTITY_POSES.WALK,
      });

      // Update current position for next iteration
      currentPosition = target;

      // Add idle pause between waypoints
      const pauseDuration = 1000 + Math.random() * 2000; // 1-3 seconds pause
      currentTimestamp += pauseDuration;

      // Add IDLE waypoint at same position during pause
      waypoints.push({
        timestamp: currentTimestamp,
        target: { ...target }, // Stay at same position
        rotation,
        pose: ENTITY_POSES.IDLE,
      });
    }

    // Create pathway
    return {
      entityId: entity.entityId,
      startAt: currentTime,
      waypoints,
      isLooping: false,
      idlePose: ENTITY_POSES.IDLE,
    };
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
