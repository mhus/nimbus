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
import type { PlayerService } from './PlayerService';

const logger = getLogger('CameraService');

/**
 * CameraService - Manages camera
 *
 * Features:
 * - First-person camera (ego-view)
 * - Position and rotation control
 * - Dynamic turn speed from PlayerInfo (updated via events)
 * - Underwater water sphere rendering
 * - Future: Third-person view support
 */
export class CameraService {
  private scene: Scene;
  private appContext: AppContext;
  private playerService?: PlayerService;

  private camera?: FreeCamera;
  private _egoView: boolean = true;

  // Camera control
  private effectiveTurnSpeed: number = 0.003; // Mouse sensitivity on land (updated via event)
  private effectiveUnderwaterTurnSpeed: number = 0.002; // Mouse sensitivity underwater (updated via event)

  // Independent camera rotation (for third-person mode)
  private cameraYaw: number = 0; // Degrees
  private cameraPitch: number = 0; // Degrees

  // Underwater effects
  private waterSphereMesh?: Mesh;
  private waterMaterial?: StandardMaterial;
  private isUnderwater: boolean = false;
  private originalFogDensity: number = 0;
  private originalFogColor: Color3 = Color3.White();

  constructor(scene: Scene, appContext: AppContext) {
    this.scene = scene;
    this.appContext = appContext;

    // Initialize turn speeds from PlayerInfo
    if (appContext.playerInfo) {
      this.effectiveTurnSpeed = appContext.playerInfo.effectiveTurnSpeed;
      this.effectiveUnderwaterTurnSpeed = appContext.playerInfo.effectiveUnderwaterTurnSpeed;
    }

    this.initializeCamera();

    logger.info('CameraService initialized', {
      turnSpeed: this.effectiveTurnSpeed,
    });
  }

  /**
   * Set PlayerService and subscribe to PlayerInfo updates
   *
   * Called after PlayerService is created to avoid circular dependency.
   *
   * @param playerService PlayerService instance
   */
  setPlayerService(playerService: PlayerService): void {
    this.playerService = playerService;

    // Subscribe to PlayerInfo updates
    playerService.on('playerInfo:updated', (info: import('@nimbus/shared').PlayerInfo) => {
      this.effectiveTurnSpeed = info.effectiveTurnSpeed;
      this.effectiveUnderwaterTurnSpeed = info.effectiveUnderwaterTurnSpeed;
      logger.debug('CameraService: turnSpeed updated', {
        turnSpeed: this.effectiveTurnSpeed,
        underwaterTurnSpeed: this.effectiveUnderwaterTurnSpeed,
      });
    });

    logger.debug('PlayerService connected to CameraService');
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
   * Set camera position for third-person view (orbiting around player)
   *
   * @param playerPosition Player's position
   * @param distance Distance from player
   */
  setThirdPersonPosition(playerPosition: Vector3, distance: number): void {
    if (!this.camera) {
      logger.warn('Cannot set third-person position: camera not initialized');
      return;
    }

    // Use independent camera rotation (not player rotation)
    const yawRad = this.cameraYaw * (Math.PI / 180);
    const pitchRad = this.cameraPitch * (Math.PI / 180);

    // Calculate offset from player based on camera rotation
    // Camera orbits around player
    const horizontalDistance = distance * Math.cos(pitchRad);
    const offsetX = -Math.sin(yawRad) * horizontalDistance;
    const offsetZ = -Math.cos(yawRad) * horizontalDistance;
    const offsetY = distance * Math.sin(pitchRad) + 1.5; // Height based on pitch + base height

    // Set camera position
    this.camera.position.set(
      playerPosition.x + offsetX,
      playerPosition.y + offsetY,
      playerPosition.z + offsetZ
    );

    // Camera rotation is already set in rotate() method
    // No need to call setTarget() - rotation is independent
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
   * Get camera yaw (in degrees)
   */
  getCameraYaw(): number {
    return this.cameraYaw;
  }

  /**
   * Get camera pitch (in degrees)
   */
  getCameraPitch(): number {
    return this.cameraPitch;
  }

  /**
   * Rotate camera by delta
   *
   * Applies effectiveTurnSpeed scaling for player-controlled sensitivity.
   * Uses separate underwater turn speed for realistic underwater feel.
   *
   * @param deltaPitch Pitch delta (from mouse movement)
   * @param deltaYaw Yaw delta (from mouse movement)
   */
  rotate(deltaPitch: number, deltaYaw: number): void {
    if (!this.camera) {
      return;
    }

    // Use appropriate turn speed based on underwater state
    const turnSpeed = this.isUnderwater
      ? this.effectiveUnderwaterTurnSpeed // Slower/more realistic underwater
      : this.effectiveTurnSpeed;          // Normal on land

    // Apply effective turn speed scaling (for dynamic sensitivity control)
    // Note: RotationHandlers already applies a base sensitivity,
    // this adds player-specific sensitivity from PlayerInfo
    const scaleFactor = turnSpeed / 0.003; // Normalize against default

    const scaledDeltaPitch = deltaPitch * scaleFactor;
    const scaledDeltaYaw = deltaYaw * scaleFactor;

    // Update independent camera rotation (in degrees for easier handling)
    this.cameraYaw += scaledDeltaYaw * (180 / Math.PI); // Convert to degrees
    this.cameraPitch += scaledDeltaPitch * (180 / Math.PI);

    // Clamp pitch to prevent camera flip
    const maxPitchDeg = 89; // Slightly less than 90 degrees
    this.cameraPitch = Math.max(-maxPitchDeg, Math.min(maxPitchDeg, this.cameraPitch));

    // Apply to camera (convert back to radians)
    this.camera.rotation.x = this.cameraPitch * (Math.PI / 180);
    this.camera.rotation.y = this.cameraYaw * (Math.PI / 180);
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

      // Notify PlayerService for auto ego-view switch
      const playerService = this.appContext.services.player;
      if (playerService) {
        playerService.setUnderwaterViewMode(underwater);
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
