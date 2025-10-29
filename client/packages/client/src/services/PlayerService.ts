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

  // Player state
  private position: Vector3 = new Vector3(0, 64, 0);
  private velocity: Vector3 = Vector3.Zero();

  // Movement parameters
  private readonly moveSpeed: number = 5.0; // blocks per second
  private readonly jumpSpeed: number = 8.0; // blocks per second
  private readonly gravity: number = -20.0; // blocks per second squared

  // State flags
  private isOnGround: boolean = false;

  // Event system
  private eventListeners: Map<string, EventListener[]> = new Map();

  constructor(appContext: AppContext, cameraService: CameraService) {
    this.appContext = appContext;
    this.cameraService = cameraService;

    // Initialize player position and sync camera
    this.syncCameraToPlayer();

    logger.info('PlayerService initialized', {
      position: this.position,
      moveSpeed: this.moveSpeed,
    });
  }

  /**
   * Get player position
   */
  getPosition(): Vector3 {
    return this.position.clone();
  }

  /**
   * Set player position
   *
   * @param x World X coordinate
   * @param y World Y coordinate
   * @param z World Z coordinate
   */
  setPosition(x: number, y: number, z: number): void {
    this.position.set(x, y, z);
    this.syncCameraToPlayer();
    this.emit('position:changed', this.position.clone());
  }

  /**
   * Move player relative to current position
   *
   * @param dx Delta X
   * @param dy Delta Y
   * @param dz Delta Z
   */
  move(dx: number, dy: number, dz: number): void {
    this.position.x += dx;
    this.position.y += dy;
    this.position.z += dz;
    this.syncCameraToPlayer();
    this.emit('position:changed', this.position.clone());
  }

  /**
   * Move player forward/backward relative to camera direction
   *
   * @param distance Distance to move (positive = forward, negative = backward)
   */
  moveForward(distance: number): void {
    const cameraRotation = this.cameraService.getRotation();
    const yaw = cameraRotation.y;

    // Calculate forward direction based on camera yaw
    const dx = Math.sin(yaw) * distance;
    const dz = Math.cos(yaw) * distance;

    this.move(dx, 0, dz);
  }

  /**
   * Move player left/right relative to camera direction
   *
   * @param distance Distance to move (positive = right, negative = left)
   */
  moveRight(distance: number): void {
    const cameraRotation = this.cameraService.getRotation();
    const yaw = cameraRotation.y;

    // Calculate right direction based on camera yaw (perpendicular to forward)
    const dx = Math.sin(yaw + Math.PI / 2) * distance;
    const dz = Math.cos(yaw + Math.PI / 2) * distance;

    this.move(dx, 0, dz);
  }

  /**
   * Jump (if on ground)
   */
  jump(): void {
    if (!this.isOnGround) {
      return;
    }

    this.velocity.y = this.jumpSpeed;
    this.isOnGround = false;

    logger.debug('Player jumped');
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
   * Update player physics and movement
   *
   * @param deltaTime Time since last frame in seconds
   */
  update(deltaTime: number): void {
    // Apply gravity
    if (!this.isOnGround) {
      this.velocity.y += this.gravity * deltaTime;
    }

    // Apply velocity to position
    if (this.velocity.lengthSquared() > 0) {
      this.position.addInPlace(this.velocity.scale(deltaTime));
      this.syncCameraToPlayer();
      this.emit('position:changed', this.position.clone());
    }

    // Simple ground check (replace with proper collision detection later)
    if (this.position.y <= 64 && this.velocity.y <= 0) {
      this.position.y = 64;
      this.velocity.y = 0;
      this.isOnGround = true;
    }
  }

  /**
   * Sync camera position to player position
   */
  private syncCameraToPlayer(): void {
    // In ego-view, camera is at player eye level (add 1.6 blocks for eye height)
    const eyeHeight = 1.6;
    this.cameraService.setPosition(this.position.x, this.position.y + eyeHeight, this.position.z);
  }

  /**
   * Get current move speed
   */
  getMoveSpeed(): number {
    return this.moveSpeed;
  }

  /**
   * Check if player is on ground
   */
  isPlayerOnGround(): boolean {
    return this.isOnGround;
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
