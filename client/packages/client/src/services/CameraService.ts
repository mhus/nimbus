/**
 * CameraService - Manages camera control
 *
 * Handles camera positioning, rotation, and view modes.
 * Initial implementation supports ego-view (first-person) only.
 */

import {
  FreeCamera,
  Vector3,
  Scene,
  MeshBuilder,
  StandardMaterial,
  Color3,
  Color4,
  Mesh,
} from '@babylonjs/core';
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';

const logger = getLogger('CameraService');

/**
 * CameraService - Manages camera
 *
 * Features:
 * - First-person camera (ego-view)
 * - Position and rotation control
 * - Underwater water sphere rendering
 * - Future: Third-person view support
 */
export class CameraService {
  private scene: Scene;
  private appContext: AppContext;

  private camera?: FreeCamera;
  private _egoView: boolean = true;

  // Underwater effects
  private waterSphereMesh?: Mesh;
  private waterMaterial?: StandardMaterial;
  private isUnderwater: boolean = false;
  private originalFogDensity: number = 0;
  private originalFogColor: Color3 = Color3.White();

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
   *
   * Updates water sphere position to follow camera
   */
  update(deltaTime: number): void {
    // Update water sphere position to follow camera
    if (this.waterSphereMesh && this.camera && this.isUnderwater) {
      this.waterSphereMesh.position.copyFrom(this.camera.position);
    }
  }

  /**
   * Set underwater state and render water sphere
   *
   * When underwater (below waterHeight in ClientHeightData):
   * - Renders a translucent water sphere around the camera
   * - Adds blue-tinted fog effect
   * - Reduces visibility for underwater atmosphere
   *
   * @param underwater True if camera is underwater
   */
  setUnderwater(underwater: boolean): void {
    try {
      // No state change, skip
      if (this.isUnderwater === underwater) {
        return;
      }

      this.isUnderwater = underwater;

      if (underwater) {
        this.enableUnderwaterEffects();
      } else {
        this.disableUnderwaterEffects();
      }

      logger.info('Underwater state changed', { underwater });
    } catch (error) {
      ExceptionHandler.handle(error, 'CameraService.setUnderwater', { underwater });
    }
  }

  /**
   * Enable underwater visual effects
   */
  private enableUnderwaterEffects(): void {
    if (!this.camera) {
      logger.warn('💧 Cannot enable underwater effects: camera not initialized');
      return;
    }

    logger.info('💧 ENABLING UNDERWATER EFFECTS');

    // Create water sphere mesh around camera
    if (!this.waterSphereMesh) {
      this.waterSphereMesh = MeshBuilder.CreateSphere(
        'waterSphere',
        {
          diameter: 8, // 8 block radius around camera
          segments: 16, // Lower segments for performance
        },
        this.scene
      );

      // Flip normals so sphere is visible from inside
      this.waterSphereMesh.flipFaces(true);

      // Create water material
      this.waterMaterial = new StandardMaterial('waterMaterial', this.scene);
      this.waterMaterial.diffuseColor = new Color3(0.1, 0.3, 0.6); // Blue color
      this.waterMaterial.alpha = 0.3; // Semi-transparent
      this.waterMaterial.backFaceCulling = false; // Render from inside

      this.waterSphereMesh.material = this.waterMaterial;

      // Position at camera
      this.waterSphereMesh.position.copyFrom(this.camera.position);

      logger.info('💧 Water sphere created', {
        position: this.waterSphereMesh.position,
        diameter: 8,
        material: {
          color: this.waterMaterial.diffuseColor,
          alpha: this.waterMaterial.alpha,
        },
      });
    }

    // Show water sphere
    this.waterSphereMesh.isVisible = true;
    logger.info('💧 Water sphere visible:', this.waterSphereMesh.isVisible);

    // Store original fog settings
    this.originalFogDensity = this.scene.fogDensity;
    this.originalFogColor = this.scene.fogColor.clone();

    // Enable fog with blue tint
    this.scene.fogMode = Scene.FOGMODE_EXP2;
    this.scene.fogDensity = 0.05; // Moderate fog density
    this.scene.fogColor = new Color3(0.1, 0.4, 0.7); // Blue fog

    // Optionally reduce ambient light
    // this.scene.ambientColor = new Color3(0.3, 0.4, 0.5);

    logger.info('💧 Underwater effects enabled', {
      fogMode: this.scene.fogMode,
      fogDensity: this.scene.fogDensity,
      fogColor: { r: this.scene.fogColor.r, g: this.scene.fogColor.g, b: this.scene.fogColor.b },
      sphereVisible: this.waterSphereMesh.isVisible,
    });
  }

  /**
   * Disable underwater visual effects
   */
  private disableUnderwaterEffects(): void {
    logger.info('💧 DISABLING UNDERWATER EFFECTS');

    // Hide water sphere
    if (this.waterSphereMesh) {
      this.waterSphereMesh.isVisible = false;
      logger.info('💧 Water sphere hidden');
    }

    // Restore original fog settings
    this.scene.fogMode = Scene.FOGMODE_NONE;
    this.scene.fogDensity = this.originalFogDensity;
    this.scene.fogColor = this.originalFogColor;

    // Restore ambient light
    // this.scene.ambientColor = new Color3(1, 1, 1);

    logger.info('💧 Underwater effects disabled', {
      fogMode: this.scene.fogMode,
      fogRestored: true,
    });
  }

  /**
   * Dispose camera and underwater effects
   */
  dispose(): void {
    // Dispose water sphere
    if (this.waterSphereMesh) {
      this.waterSphereMesh.dispose();
      this.waterSphereMesh = undefined;
    }

    // Dispose water material
    if (this.waterMaterial) {
      this.waterMaterial.dispose();
      this.waterMaterial = undefined;
    }

    // Dispose camera
    this.camera?.dispose();

    logger.info('Camera disposed');
  }
}
