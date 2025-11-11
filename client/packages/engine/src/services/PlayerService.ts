/**
 * PlayerService - Manages player state and movement
 *
 * Handles player position, movement, and camera synchronization.
 * Initial implementation provides position/logic only (no rendering).
 */

import { Vector3 } from '@babylonjs/core';
import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { CameraService } from './CameraService';
import type { PhysicsService, MovementMode } from './PhysicsService';
import type { PlayerEntity } from '../types/PlayerEntity';

const logger = getLogger('PlayerService');

/**
 * Event listener type
 */
type EventListener = (...args: any[]) => void;

/**
 * PlayerService - Manages player state
 *
 * Features:
 * - Player position tracking
 * - Camera synchronization
 * - Movement logic
 * - Event emission for position updates
 *
 * Note: Player rendering (third-person view) is not implemented yet
 */
export class PlayerService {
  private appContext: AppContext;
  private cameraService: CameraService;
  private physicsService?: PhysicsService;

  // Player as physics entity with player info
  private playerEntity: PlayerEntity;

  // Last known position (for change detection)
  private lastPosition: { x: number; y: number; z: number };

  // Event system
  private eventListeners: Map<string, EventListener[]> = new Map();

  constructor(appContext: AppContext, cameraService: CameraService) {
    this.appContext = appContext;
    this.cameraService = cameraService;

    // Get PlayerInfo from AppContext (must be initialized before PlayerService)
    if (!appContext.playerInfo) {
      throw new Error('PlayerInfo must be initialized in AppContext before creating PlayerService');
    }

    // Initialize dimensions if not present
    if (!appContext.playerInfo.dimensions) {
      appContext.playerInfo.dimensions = {
        walk: { height: 2.0, width: 0.6, footprint: 0.3 },
        sprint: { height: 2.0, width: 0.6, footprint: 0.3 },
        crouch: { height: 1.0, width: 0.6, footprint: 0.3 },
        swim: { height: 1.8, width: 0.6, footprint: 0.3 },
        climb: { height: 1.8, width: 0.6, footprint: 0.3 },
        fly: { height: 1.8, width: 0.6, footprint: 0.3 },
        teleport: { height: 1.8, width: 0.6, footprint: 0.3 },
      };
    }

    // Create player entity (starts in Walk mode)
    this.playerEntity = {
      entityId: 'player',
      position: new Vector3(0, 64, 0),
      velocity: Vector3.Zero(),
      rotation: Vector3.Zero(), // Rotation in radians (x: pitch, y: yaw, z: roll)
      movementMode: 'walk' as MovementMode,
      wishMove: Vector3.Zero(), // Movement intention
      grounded: false, // Is on ground
      onSlope: false, // Is on slope
      inWater: false, // Is in water
      canAutoJump: false, // Can trigger auto-jump
      lastBlockPos: new Vector3(0, 64, 0), // Last block position for cache invalidation
      playerInfo: appContext.playerInfo,
      // Initialize cached effective values from PlayerInfo
      effectiveWalkSpeed: appContext.playerInfo.effectiveWalkSpeed,
      effectiveRunSpeed: appContext.playerInfo.effectiveRunSpeed,
      effectiveUnderwaterSpeed: appContext.playerInfo.effectiveUnderwaterSpeed,
      effectiveCrawlSpeed: appContext.playerInfo.effectiveCrawlSpeed,
      effectiveRidingSpeed: appContext.playerInfo.effectiveRidingSpeed,
      effectiveJumpSpeed: appContext.playerInfo.effectiveJumpSpeed,
      effectiveTurnSpeed: appContext.playerInfo.effectiveTurnSpeed,
      effectiveUnderwaterTurnSpeed: appContext.playerInfo.effectiveUnderwaterTurnSpeed,
    };

    // Initialize last position for change detection
    this.lastPosition = {
      x: this.playerEntity.position.x,
      y: this.playerEntity.position.y,
      z: this.playerEntity.position.z,
    };

    // Initialize player position and sync camera
    this.syncCameraToPlayer();

    logger.info('PlayerService initialized', {
      position: this.playerEntity.position,
      movementMode: this.playerEntity.movementMode,
      displayName: this.playerEntity.playerInfo.displayName,
    });
  }

  /**
   * Set physics service (called after PhysicsService is created)
   */
  setPhysicsService(physicsService: PhysicsService): void {
    this.physicsService = physicsService;
    this.physicsService.registerEntity(this.playerEntity);
    logger.debug('PhysicsService set and player registered');
  }

  /**
   * Get player position
   */
  getPosition(): Vector3 {
    return this.playerEntity.position.clone();
  }

  /**
   * Set player position
   *
   * @param x World X coordinate
   * @param y World Y coordinate
   * @param z World Z coordinate
   */
  setPosition(x: number, y: number, z: number): void {
    this.playerEntity.position.set(x, y, z);
    this.syncCameraToPlayer();
    this.emit('position:changed', this.playerEntity.position.clone());
  }

  /**
   * Move player forward/backward relative to camera direction
   *
   * @param distance Distance to move (positive = forward, negative = backward)
   */
  moveForward(distance: number): void {
    if (!this.physicsService) return;

    const cameraRotation = this.cameraService.getRotation();
    this.physicsService.moveForward(
      this.playerEntity,
      distance,
      cameraRotation.y, // yaw
      cameraRotation.x  // pitch
    );

    this.syncCameraToPlayer();
    this.emit('position:changed', this.playerEntity.position.clone());
  }

  /**
   * Move player left/right relative to camera direction
   *
   * @param distance Distance to move (positive = right, negative = left)
   */
  moveRight(distance: number): void {
    if (!this.physicsService) return;

    const cameraRotation = this.cameraService.getRotation();
    this.physicsService.moveRight(
      this.playerEntity,
      distance,
      cameraRotation.y // yaw
    );

    this.syncCameraToPlayer();
    this.emit('position:changed', this.playerEntity.position.clone());
  }

  /**
   * Move player up/down (Fly mode only)
   *
   * @param distance Distance to move (positive = up, negative = down)
   */
  moveUp(distance: number): void {
    if (!this.physicsService) return;

    this.physicsService.moveUp(this.playerEntity, distance);

    this.syncCameraToPlayer();
    this.emit('position:changed', this.playerEntity.position.clone());
  }

  /**
   * Jump (Walk mode only, if on ground)
   */
  jump(): void {
    if (!this.physicsService) return;

    this.physicsService.jump(this.playerEntity);
  }

  /**
   * Rotate camera (controls player look direction)
   *
   * @param deltaPitch Pitch delta in radians
   * @param deltaYaw Yaw delta in radians
   */
  rotate(deltaPitch: number, deltaYaw: number): void {
    this.cameraService.rotate(deltaPitch, deltaYaw);
  }

  /**
   * Update player (physics is handled by PhysicsService)
   *
   * @param deltaTime Time since last frame in seconds
   */
  update(deltaTime: number): void {
    // Physics is now handled by PhysicsService
    // Just sync camera after physics update
    this.syncCameraToPlayer();

    // Emit position change if moved (for chunk loading, etc.)
    const currentPos = this.playerEntity.position;
    if (
      currentPos.x !== this.lastPosition.x ||
      currentPos.y !== this.lastPosition.y ||
      currentPos.z !== this.lastPosition.z
    ) {
      this.lastPosition.x = currentPos.x;
      this.lastPosition.y = currentPos.y;
      this.lastPosition.z = currentPos.z;

      this.emit('position:changed', currentPos.clone());
    }
  }

  /**
   * Sync camera position to player position
   */
  private syncCameraToPlayer(): void {
    // In ego-view, camera is at player eye level (from PlayerInfo)
    const eyeHeight = this.playerEntity.playerInfo.eyeHeight;
    this.cameraService.setPosition(
      this.playerEntity.position.x,
      this.playerEntity.position.y + eyeHeight,
      this.playerEntity.position.z
    );
  }

  /**
   * Get PlayerEntity (for advanced access)
   */
  getPlayerEntity(): PlayerEntity {
    return this.playerEntity;
  }

  /**
   * Update player info dynamically (for power-ups, equipment, status effects)
   *
   * Updates PlayerInfo and emits 'playerInfo:updated' event so all services
   * can react to the changes (PhysicsService, CameraService, SelectService, etc.)
   *
   * @param updates Partial PlayerInfo with values to update
   */
  updatePlayerInfo(updates: Partial<import('@nimbus/shared').PlayerInfo>): void {
    // Update PlayerInfo
    Object.assign(this.playerEntity.playerInfo, updates);

    // Update cached effective values on entity
    this.playerEntity.effectiveWalkSpeed = this.playerEntity.playerInfo.effectiveWalkSpeed;
    this.playerEntity.effectiveRunSpeed = this.playerEntity.playerInfo.effectiveRunSpeed;
    this.playerEntity.effectiveUnderwaterSpeed = this.playerEntity.playerInfo.effectiveUnderwaterSpeed;
    this.playerEntity.effectiveCrawlSpeed = this.playerEntity.playerInfo.effectiveCrawlSpeed;
    this.playerEntity.effectiveRidingSpeed = this.playerEntity.playerInfo.effectiveRidingSpeed;
    this.playerEntity.effectiveJumpSpeed = this.playerEntity.playerInfo.effectiveJumpSpeed;
    this.playerEntity.effectiveTurnSpeed = this.playerEntity.playerInfo.effectiveTurnSpeed;
    this.playerEntity.effectiveUnderwaterTurnSpeed = this.playerEntity.playerInfo.effectiveUnderwaterTurnSpeed;

    logger.debug('PlayerInfo updated', { updates });

    // Emit event so all services can react
    this.emit('playerInfo:updated', this.playerEntity.playerInfo);

    // Sync camera in case eyeHeight changed (direct update for immediate feedback)
    this.syncCameraToPlayer();
  }

  /**
   * Get current move speed
   */
  getMoveSpeed(): number {
    if (!this.physicsService) return 5.0;
    return this.physicsService.getMoveSpeed(this.playerEntity);
  }

  /**
   * Check if player is on ground
   */
  isPlayerOnGround(): boolean {
    return this.playerEntity.grounded;
  }

  /**
   * Get current movement mode
   */
  getMovementMode(): MovementMode {
    return this.playerEntity.movementMode;
  }

  /**
   * Set movement mode
   */
  setMovementMode(mode: MovementMode): void {
    if (!this.physicsService) return;
    this.physicsService.setMovementMode(this.playerEntity, mode);
  }

  /**
   * Toggle between Walk and Fly modes
   */
  toggleMovementMode(): void {
    if (!this.physicsService) return;
    this.physicsService.toggleMovementMode(this.playerEntity);
  }

  /**
   * Add event listener
   *
   * @param event Event name
   * @param listener Event listener function
   */
  on(event: string, listener: EventListener): void {
    const listeners = this.eventListeners.get(event) || [];
    listeners.push(listener);
    this.eventListeners.set(event, listeners);
  }

  /**
   * Remove event listener
   *
   * @param event Event name
   * @param listener Event listener function
   */
  off(event: string, listener: EventListener): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      const index = listeners.indexOf(listener);
      if (index !== -1) {
        listeners.splice(index, 1);
      }
    }
  }

  /**
   * Emit event
   *
   * @param event Event name
   * @param args Event arguments
   */
  private emit(event: string, ...args: any[]): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      listeners.forEach((listener) => {
        try {
          listener(...args);
        } catch (error) {
          logger.error('Error in event listener', { event }, error as Error);
        }
      });
    }
  }

  /**
   * Dispose player service
   */
  dispose(): void {
    this.eventListeners.clear();
    logger.info('PlayerService disposed');
  }
}
