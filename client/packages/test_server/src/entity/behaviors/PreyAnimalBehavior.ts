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
import { ENTITY_POSES, getLogger } from '@nimbus/shared';
import type { EntityPhysicsSimulator } from '../EntityPhysicsSimulator';
import { BlockBasedMovement } from '../movement/BlockBasedMovement';

const logger = getLogger('PreyAnimalBehavior');

/**
 * PreyAnimalBehavior - Passive roaming behavior
 */
export class PreyAnimalBehavior extends EntityBehavior {
  readonly behaviorType = 'PreyAnimalBehavior';

  /** Default interval between pathway generation (milliseconds) */
  private readonly defaultPathwayInterval = 5000; // 5 seconds

  /** Default number of waypoints per pathway */
  private readonly defaultWaypointsPerPathway = 5;

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

  /** Block-based movement helper */
  private blockMovement: BlockBasedMovement = new BlockBasedMovement();

  /**
   * Override setWorldManager to also set it in blockMovement
   */
  setWorldManager(worldManager: any): void {
    super.setWorldManager(worldManager);
    this.blockMovement.setWorldManager(worldManager);
  }

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
   * Generate new pathway for entity (block-based movement)
   */
  private async generatePathway(
    entity: ServerEntitySpawnDefinition,
    currentTime: number,
    worldId: string
  ): Promise<EntityPathway> {
    // Get behavior config with defaults
    const config = entity.behaviorConfig || {};
    const waypointsPerPath = config.waypointsPerPath ?? this.defaultWaypointsPerPathway;
    const minIdleDuration = config.minIdleDuration ?? this.defaultMinIdleDuration;
    const maxIdleDuration = config.maxIdleDuration ?? this.defaultMaxIdleDuration;

    // Start position: end of previous pathway or initial position
    let startPosition = entity.initialPosition;
    if (entity.currentPathway && entity.currentPathway.waypoints.length > 0) {
      const lastWaypoint = entity.currentPathway.waypoints[entity.currentPathway.waypoints.length - 1];
      startPosition = lastWaypoint.target;
    }

    // Ensure start position is on solid ground
    const startY = await this.blockMovement.findStartPosition(worldId, startPosition.x, startPosition.z);
    startPosition = { x: startPosition.x, y: startY, z: startPosition.z };

    // Choose random direction
    const direction = this.blockMovement.getRandomDirection();

    // Generate waypoints using block-based movement
    const waypoints = await this.blockMovement.generatePathway(
      worldId,
      startPosition,
      direction,
      waypointsPerPath,
      entity.speed,
      currentTime
    );

    // Add idle waypoints between movement waypoints
    const waypointsWithIdle: Waypoint[] = [];
    for (const waypoint of waypoints) {
      waypointsWithIdle.push(waypoint);

      // Add idle pause after each movement
      const pauseDuration = minIdleDuration + Math.random() * (maxIdleDuration - minIdleDuration);
      waypointsWithIdle.push({
        timestamp: waypoint.timestamp + pauseDuration,
        target: { ...waypoint.target },
        rotation: waypoint.rotation,
        pose: ENTITY_POSES.IDLE,
      });
    }

    // Create pathway
    return {
      entityId: entity.entityId,
      startAt: currentTime,
      waypoints: waypointsWithIdle,
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
