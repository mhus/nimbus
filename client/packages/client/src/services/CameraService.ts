/**
 * CameraService - Manages camera control
 *
 * Handles camera positioning, rotation, and view modes.
 * Initial implementation supports ego-view (first-person) only.
 */

import { FreeCamera, Vector3, Scene } from '@babylonjs/core';
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';

const logger = getLogger('CameraService');

/**
 * CameraService - Manages camera
 *
 * Features:
 * - First-person camera (ego-view)
 * - Position and rotation control
 * - Future: Third-person view support
 */
export class CameraService {
  private scene: Scene;
  private appContext: AppContext;

  private camera?: FreeCamera;
  private _egoView: boolean = true;

  constructor(scene: Scene, appContext: AppContext) {
    this.scene = scene;
    this.appContext = appContext;

    this.initializeCamera();

    logger.info('CameraService initialized');
  }

  /**
   * Initialize the camera
   */
  private initializeCamera(): void {
    try {
      // Create first-person camera
      this.camera = new FreeCamera('playerCamera', new Vector3(0, 64, 0), this.scene);

      // Set camera properties
      this.camera.minZ = 0.1;
      this.camera.maxZ = 500;
      this.camera.fov = 70 * (Math.PI / 180); // 70 degrees in radians

      // Set initial rotation (looking forward)
      this.camera.rotation = new Vector3(0, 0, 0);

      // Attach camera to canvas for input (will be controlled by InputService later)
      // this.camera.attachControl(canvas, true);

      logger.debug('Camera initialized', {
        position: this.camera.position,
        fov: this.camera.fov,
      });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'CameraService.initializeCamera');
    }
  }

  /**
   * Get the camera
   */
  getCamera(): FreeCamera | undefined {
    return this.camera;
  }

  /**
   * Set camera position
   *
   * @param x World X coordinate
   * @param y World Y coordinate
   * @param z World Z coordinate
   */
  setPosition(x: number, y: number, z: number): void {
    if (!this.camera) {
      logger.warn('Cannot set position: camera not initialized');
      return;
    }

    this.camera.position.set(x, y, z);
  }

  /**
   * Get camera position
   */
  getPosition(): Vector3 {
    if (!this.camera) {
      return Vector3.Zero();
    }

    return this.camera.position.clone();
  }

  /**
   * Set camera rotation
   *
   * @param pitch Pitch (X rotation) in radians
   * @param yaw Yaw (Y rotation) in radians
   * @param roll Roll (Z rotation) in radians (usually 0)
   */
  setRotation(pitch: number, yaw: number, roll: number = 0): void {
    if (!this.camera) {
      logger.warn('Cannot set rotation: camera not initialized');
      return;
    }

    this.camera.rotation.set(pitch, yaw, roll);
  }

  /**
   * Get camera rotation
   */
  getRotation(): Vector3 {
    if (!this.camera) {
      return Vector3.Zero();
    }

    return this.camera.rotation.clone();
  }

  /**
   * Rotate camera by delta
   *
   * @param deltaPitch Pitch delta in radians
   * @param deltaYaw Yaw delta in radians
   */
  rotate(deltaPitch: number, deltaYaw: number): void {
    if (!this.camera) {
      return;
    }

    this.camera.rotation.x += deltaPitch;
    this.camera.rotation.y += deltaYaw;

    // Clamp pitch to prevent camera flip
    const maxPitch = Math.PI / 2 - 0.01; // Slightly less than 90 degrees
    this.camera.rotation.x = Math.max(-maxPitch, Math.min(maxPitch, this.camera.rotation.x));
  }

  /**
   * Check if camera is in ego-view (first-person)
   */
  get egoView(): boolean {
    return this._egoView;
  }

  /**
   * Set ego-view mode
   *
   * Note: Third-person view is not implemented yet
   */
  set egoView(value: boolean) {
    if (!value) {
      logger.warn('Third-person view not implemented yet');
      return;
    }

    this._egoView = value;
  }

  /**
   * Update camera (called each frame if needed)
   */
  update(deltaTime: number): void {
    // Future: Add camera animation, smoothing, etc.
  }

  /**
   * Dispose camera
   */
  dispose(): void {
    this.camera?.dispose();
    logger.info('Camera disposed');
  }
}
