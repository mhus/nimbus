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
import type { EntityPathway, Waypoint, ServerEntitySpawnDefinition, Vector3 } from '@nimbus/shared';
import { ENTITY_POSES } from '@nimbus/shared';
import type { EntityPhysicsSimulator } from '../EntityPhysicsSimulator';

/**
 * PreyAnimalBehavior - Passive roaming behavior
 */
export class PreyAnimalBehavior extends EntityBehavior {
  readonly behaviorType = 'PreyAnimalBehavior';

  /** Default interval between pathway generation (milliseconds) */
  private readonly defaultPathwayInterval = 5000; // 5 seconds

  /** Default number of waypoints per pathway */
  private readonly defaultWaypointsPerPathway = 3;

  /** Default min step distance (blocks) */
  private readonly defaultMinStepDistance = 2;

  /** Default max step distance (blocks) */
  private readonly defaultMaxStepDistance = 3;

  /** Default min idle duration (milliseconds) */
  private readonly defaultMinIdleDuration = 1000;

  /** Default max idle duration (milliseconds) */
  private readonly defaultMaxIdleDuration = 3000;

  /** Current target position for physics-based movement */
  private physicsTargets: Map<string, { target: Vector3; reachedAt: number | null }> = new Map();

  /**
   * Update behavior and generate new pathway if needed (waypoint-based)
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
   * Update physics-based entity movement
   * Applies velocity to move entity towards random targets
   */
  async updatePhysics(
    entity: ServerEntitySpawnDefinition,
    physicsSimulator: EntityPhysicsSimulator,
    worldId: string
  ): Promise<void> {
    if (!entity.physicsState) {
      return;
    }

    const position = entity.physicsState.position;
    const config = entity.behaviorConfig || {};
    const minStepDistance = config.minStepDistance ?? this.defaultMinStepDistance;
    const maxStepDistance = config.maxStepDistance ?? this.defaultMaxStepDistance;
    const minIdleDuration = config.minIdleDuration ?? this.defaultMinIdleDuration;
    const maxIdleDuration = config.maxIdleDuration ?? this.defaultMaxIdleDuration;

    // Get or create target for this entity
    let targetInfo = this.physicsTargets.get(entity.entityId);

    // Check if we need a new target
    const needsNewTarget = !targetInfo || this.hasReachedTarget(position, targetInfo.target);

    if (needsNewTarget) {
      // Check if we're in idle period
      if (targetInfo?.reachedAt) {
        const idleDuration = minIdleDuration + Math.random() * (maxIdleDuration - minIdleDuration);
        const timeSinceReached = Date.now() - targetInfo.reachedAt;

        if (timeSinceReached < idleDuration) {
          // Still idle, don't move
          physicsSimulator.setVelocity(entity, { x: 0, y: entity.physicsState.velocity.y, z: 0 });
          return;
        }
      }

      // Generate new random target
      const stepDistance = minStepDistance + Math.random() * (maxStepDistance - minStepDistance);
      const targetXZ = this.randomPositionInRadius(position, stepDistance);
      const groundY = await this.getGroundHeight(worldId, targetXZ.x, targetXZ.z);

      const newTarget = {
        x: targetXZ.x,
        y: groundY,
        z: targetXZ.z,
      };

      targetInfo = {
        target: newTarget,
        reachedAt: null,
      };
      this.physicsTargets.set(entity.entityId, targetInfo);
    }

    // Calculate direction to target
    const target = targetInfo.target;
    const dx = target.x - position.x;
    const dz = target.z - position.z;
    const distance = Math.sqrt(dx * dx + dz * dz);

    if (distance < 0.5) {
      // Reached target
      targetInfo.reachedAt = Date.now();
      physicsSimulator.setVelocity(entity, { x: 0, y: entity.physicsState.velocity.y, z: 0 });

      // Update rotation towards target
      const rotation = this.calculateRotation(position, target);
      entity.physicsState.rotation = rotation;
    } else {
      // Move towards target
      const speed = entity.speed;
      const velocityX = (dx / distance) * speed;
      const velocityZ = (dz / distance) * speed;

      physicsSimulator.setVelocity(entity, {
        x: velocityX,
        y: entity.physicsState.velocity.y, // Keep Y velocity (gravity)
        z: velocityZ,
      });

      // Update rotation towards target
      const rotation = this.calculateRotation(position, target);
      entity.physicsState.rotation = rotation;
    }
  }

  /**
   * Check if entity has reached target position
   */
  private hasReachedTarget(position: Vector3, target: Vector3): boolean {
    const dx = target.x - position.x;
    const dz = target.z - position.z;
    const distance = Math.sqrt(dx * dx + dz * dz);
    return distance < 0.5;
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

    // Get behavior config with defaults
    const config = entity.behaviorConfig || {};
    const minStepDistance = config.minStepDistance ?? this.defaultMinStepDistance;
    const maxStepDistance = config.maxStepDistance ?? this.defaultMaxStepDistance;
    const waypointsPerPath = config.waypointsPerPath ?? this.defaultWaypointsPerPathway;
    const minIdleDuration = config.minIdleDuration ?? this.defaultMinIdleDuration;
    const maxIdleDuration = config.maxIdleDuration ?? this.defaultMaxIdleDuration;

    // Start position: end of previous pathway or initial position
    let currentPosition = entity.initialPosition;
    if (entity.currentPathway && entity.currentPathway.waypoints.length > 0) {
      const lastWaypoint = entity.currentPathway.waypoints[entity.currentPathway.waypoints.length - 1];
      currentPosition = lastWaypoint.target;
    }

    let currentTimestamp = currentTime;

    // Generate waypoints
    for (let i = 0; i < waypointsPerPath; i++) {
      // Generate random target within configured step distance
      const stepDistance = minStepDistance + Math.random() * (maxStepDistance - minStepDistance);
      const targetXZ = this.randomPositionInRadius(currentPosition, stepDistance);

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

      // Get maxPitch from entity model
      let maxPitch: number | undefined;
      if (this.entityManager) {
        const entityModel = this.entityManager.getEntityModel(entity.entityModelId);
        maxPitch = entityModel?.maxPitch;
      }

      // Calculate rotation towards target with pitch limit
      const rotation = this.calculateRotation(currentPosition, target, maxPitch);

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
      const pauseDuration = minIdleDuration + Math.random() * (maxIdleDuration - minIdleDuration);
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
   * Override to use custom interval from config
   */
  protected needsNewPathway(entity: ServerEntitySpawnDefinition, currentTime: number): boolean {
    if (!entity.currentPathway) {
      return true;
    }

    const pathway = entity.currentPathway;
    if (pathway.waypoints.length === 0) {
      return true;
    }

    // Get pathway interval from config or use default
    const pathwayInterval = entity.behaviorConfig?.pathwayInterval ?? this.defaultPathwayInterval;

    // Check if enough time has passed since pathway start
    const timeSinceStart = currentTime - pathway.startAt;
    if (timeSinceStart < pathwayInterval) {
      return false;
    }

    // Check if pathway is finished
    const lastWaypoint = pathway.waypoints[pathway.waypoints.length - 1];
    return currentTime >= lastWaypoint.timestamp;
  }
}
