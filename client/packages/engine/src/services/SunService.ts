import { getLogger } from '@nimbus/shared';
import {
  Scene,
  ParticleSystem,
  TransformNode,
  Vector3,
  Color4,
  RawTexture,
  Constants,
} from '@babylonjs/core';
import type { AppContext } from '../AppContext';
import type { CameraService } from './CameraService';
import { RENDERING_GROUPS } from '../config/renderingGroups';

const logger = getLogger('SunService');

/**
 * SunService - Manages sun visualization using particle systems
 *
 * Creates a multi-layered particle-based sun with:
 * - Core center sphere
 * - Inner atmospheric glow
 * - 4 cardinal directional rays (N/S/E/W)
 * - Diagonal rays rendered via texture
 */
export class SunService {
  private scene: Scene;
  private appContext: AppContext;
  private cameraService: CameraService;

  // Sun emitter node (attached to camera environment root)
  private sunEmitter?: TransformNode;

  // Particle systems (5 total)
  private coreSystem?: ParticleSystem;
  private glowSystem?: ParticleSystem;
  private northRaySystem?: ParticleSystem;
  private southRaySystem?: ParticleSystem;
  private eastRaySystem?: ParticleSystem;
  private westRaySystem?: ParticleSystem;

  // Textures
  private coreTexture?: RawTexture; // Circle with 4 diagonal rays
  private glowTexture?: RawTexture; // Soft radial gradient
  private rayTexture?: RawTexture; // Elongated gradient for rays

  // Sun position parameters
  private currentAngleY: number = 90; // Default: East
  private currentElevation: number = 45; // Default: 45° above horizon
  private orbitRadius: number = 400; // Distance from camera

  // Sun appearance
  private sunColor: Color4 = new Color4(1, 1, 0.9, 1); // Warm white/yellow
  private enabled: boolean = true;

  constructor(scene: Scene, appContext: AppContext) {
    this.scene = scene;
    this.appContext = appContext;
    this.cameraService = appContext.services.camera!;

    this.initialize();
  }

  private initialize(): void {
    // Create textures
    this.coreTexture = this.createCoreTexture();
    this.glowTexture = this.createGlowTexture();
    this.rayTexture = this.createRayTexture();

    // Create emitter node attached to camera environment root
    const cameraRoot = this.cameraService.getCameraEnvironmentRoot();
    if (!cameraRoot) {
      logger.error('Camera environment root not available');
      return;
    }

    this.sunEmitter = new TransformNode('sunEmitter', this.scene);
    this.sunEmitter.parent = cameraRoot;

    // Create particle systems
    this.createCoreSystem();
    this.createGlowSystem();
    this.createCardinalRays();

    // Set initial position
    this.setSunPositionOnCircle(this.currentAngleY);
    this.setSunHeightOverCamera(this.currentElevation);

    logger.info('SunService initialized', {
      angleY: this.currentAngleY,
      elevation: this.currentElevation,
    });
  }

  /**
   * Create core texture with diagonal rays baked in
   * Center sphere + 4 diagonal rays (NE, SE, SW, NW)
   */
  private createCoreTexture(): RawTexture {
    const size = 256;
    const center = size / 2;
    const textureData = new Uint8Array(size * size * 4);

    for (let y = 0; y < size; y++) {
      for (let x = 0; x < size; x++) {
        const dx = x - center;
        const dy = y - center;
        const dist = Math.sqrt(dx * dx + dy * dy);
        const angle = Math.atan2(dy, dx);

        let alpha = 0;

        // Core circle (bright center)
        if (dist < 25) {
          alpha = 1.0 - dist / 25;
        }
        // Diagonal rays (NE, SE, SW, NW at 45°, 135°, 225°, 315°)
        else {
          alpha = this.calculateDiagonalRayAlpha(dx, dy, dist, angle);
        }

        const idx = (y * size + x) * 4;
        textureData[idx] = 255; // R
        textureData[idx + 1] = 255; // G
        textureData[idx + 2] = 255; // B
        textureData[idx + 3] = Math.floor(alpha * 255); // A
      }
    }

    return RawTexture.CreateRGBATexture(
      textureData,
      size,
      size,
      this.scene,
      false,
      false,
      Constants.TEXTURE_BILINEAR_SAMPLINGMODE
    );
  }

  /**
   * Calculate alpha for diagonal rays (thinner than cardinal)
   */
  private calculateDiagonalRayAlpha(
    dx: number,
    dy: number,
    dist: number,
    angle: number
  ): number {
    // Diagonal angles: 45°, 135°, 225°, 315° (π/4, 3π/4, 5π/4, 7π/4)
    const diagonalAngles = [Math.PI / 4, (3 * Math.PI) / 4, (-3 * Math.PI) / 4, -Math.PI / 4];

    const rayWidth = 10; // Thinner than cardinal rays
    const rayLength = 90;

    for (const targetAngle of diagonalAngles) {
      const angleDiff = Math.abs(((angle - targetAngle + Math.PI) % (2 * Math.PI)) - Math.PI);

      if (angleDiff < 0.2 && dist > 25 && dist < rayLength) {
        // Within ray cone
        const lengthFade = 1.0 - (dist - 25) / (rayLength - 25);
        const widthFade = 1.0 - angleDiff / 0.2;
        return lengthFade * widthFade * 0.6; // 60% opacity for diagonal rays
      }
    }

    return 0;
  }

  /**
   * Create soft radial gradient for glow effect
   */
  private createGlowTexture(): RawTexture {
    const size = 128;
    const center = size / 2;
    const textureData = new Uint8Array(size * size * 4);

    for (let y = 0; y < size; y++) {
      for (let x = 0; x < size; x++) {
        const dx = x - center;
        const dy = y - center;
        const dist = Math.sqrt(dx * dx + dy * dy);
        const normalizedDist = dist / center;

        // Smooth falloff
        const alpha = Math.max(0, 1.0 - normalizedDist);

        const idx = (y * size + x) * 4;
        textureData[idx] = 255;
        textureData[idx + 1] = 255;
        textureData[idx + 2] = 255;
        textureData[idx + 3] = Math.floor(alpha * 255);
      }
    }

    return RawTexture.CreateRGBATexture(
      textureData,
      size,
      size,
      this.scene,
      false,
      false,
      Constants.TEXTURE_BILINEAR_SAMPLINGMODE
    );
  }

  /**
   * Create elongated gradient for directional rays
   */
  private createRayTexture(): RawTexture {
    const width = 32;
    const height = 128;
    const textureData = new Uint8Array(width * height * 4);

    for (let y = 0; y < height; y++) {
      for (let x = 0; x < width; x++) {
        const centerX = width / 2;
        const distFromCenter = Math.abs(x - centerX) / centerX;

        // Fade along length (top to bottom)
        const lengthFade = 1.0 - y / height;

        // Fade from center to edges (horizontally)
        const widthFade = 1.0 - distFromCenter;

        const alpha = lengthFade * widthFade;

        const idx = (y * width + x) * 4;
        textureData[idx] = 255;
        textureData[idx + 1] = 255;
        textureData[idx + 2] = 255;
        textureData[idx + 3] = Math.floor(alpha * 255);
      }
    }

    return RawTexture.CreateRGBATexture(
      textureData,
      width,
      height,
      this.scene,
      false,
      false,
      Constants.TEXTURE_BILINEAR_SAMPLINGMODE
    );
  }

  /**
   * Create core sun particle system
   */
  private createCoreSystem(): void {
    this.coreSystem = new ParticleSystem('sunCore', 200, this.scene);
    this.coreSystem.particleTexture = this.coreTexture!;
    this.coreSystem.emitter = this.sunEmitter!.position;

    // Size
    this.coreSystem.minSize = 60;
    this.coreSystem.maxSize = 70;

    // Lifetime and emission
    this.coreSystem.minLifeTime = 2.0;
    this.coreSystem.maxLifeTime = 3.0;
    this.coreSystem.emitRate = 80;

    // Static particles (no movement)
    this.coreSystem.direction1 = Vector3.Zero();
    this.coreSystem.direction2 = Vector3.Zero();
    this.coreSystem.minEmitPower = 0;
    this.coreSystem.maxEmitPower = 0;

    // Color
    this.coreSystem.color1 = this.sunColor;
    this.coreSystem.color2 = new Color4(1, 0.95, 0.8, 1);

    // Blend mode and rendering
    this.coreSystem.blendMode = ParticleSystem.BLENDMODE_ADD;
    this.coreSystem.renderingGroupId = RENDERING_GROUPS.ENVIRONMENT;

    this.coreSystem.start();
  }

  /**
   * Create inner glow particle system
   */
  private createGlowSystem(): void {
    this.glowSystem = new ParticleSystem('sunGlow', 300, this.scene);
    this.glowSystem.particleTexture = this.glowTexture!;
    this.glowSystem.emitter = this.sunEmitter!.position;

    // Larger size for glow effect
    this.glowSystem.minSize = 100;
    this.glowSystem.maxSize = 120;

    // Lifetime and emission
    this.glowSystem.minLifeTime = 3.0;
    this.glowSystem.maxLifeTime = 4.0;
    this.glowSystem.emitRate = 100;

    // Static particles
    this.glowSystem.direction1 = Vector3.Zero();
    this.glowSystem.direction2 = Vector3.Zero();
    this.glowSystem.minEmitPower = 0;
    this.glowSystem.maxEmitPower = 0;

    // Softer color
    this.glowSystem.color1 = new Color4(1, 0.95, 0.7, 0.5);
    this.glowSystem.color2 = new Color4(1, 0.9, 0.6, 0.4);

    // Blend mode and rendering
    this.glowSystem.blendMode = ParticleSystem.BLENDMODE_ADD;
    this.glowSystem.renderingGroupId = RENDERING_GROUPS.ENVIRONMENT;

    this.glowSystem.start();
  }

  /**
   * Create 4 cardinal ray particle systems (N, S, E, W)
   */
  private createCardinalRays(): void {
    // North ray (rotated 0°)
    this.northRaySystem = this.createRaySystem('sunRayNorth', 0);

    // East ray (rotated 90°)
    this.eastRaySystem = this.createRaySystem('sunRayEast', Math.PI / 2);

    // South ray (rotated 180°)
    this.southRaySystem = this.createRaySystem('sunRaySouth', Math.PI);

    // West ray (rotated 270°)
    this.westRaySystem = this.createRaySystem('sunRayWest', (3 * Math.PI) / 2);
  }

  /**
   * Create a single directional ray particle system
   */
  private createRaySystem(name: string, rotationY: number): ParticleSystem {
    const raySystem = new ParticleSystem(name, 150, this.scene);
    raySystem.particleTexture = this.rayTexture!;
    raySystem.emitter = this.sunEmitter!.position;

    // Size (thicker than diagonal rays)
    raySystem.minSize = 40;
    raySystem.maxSize = 50;

    // Lifetime and emission
    raySystem.minLifeTime = 2.5;
    raySystem.maxLifeTime = 3.5;
    raySystem.emitRate = 60;

    // Static particles
    raySystem.direction1 = Vector3.Zero();
    raySystem.direction2 = Vector3.Zero();
    raySystem.minEmitPower = 0;
    raySystem.maxEmitPower = 0;

    // Color (slightly more intense)
    raySystem.color1 = new Color4(1, 1, 0.95, 0.9);
    raySystem.color2 = new Color4(1, 0.95, 0.85, 0.8);

    // Billboard mode for proper orientation
    raySystem.billboardMode = ParticleSystem.BILLBOARDMODE_ALL;

    // Blend mode and rendering
    raySystem.blendMode = ParticleSystem.BLENDMODE_ADD;
    raySystem.renderingGroupId = RENDERING_GROUPS.ENVIRONMENT;

    raySystem.start();
    return raySystem;
  }

  /**
   * Set sun position on circular orbit around camera using Y-axis angle
   * @param angleY Horizontal angle in degrees (0=North, 90=East, 180=South, 270=West)
   */
  setSunPositionOnCircle(angleY: number): void {
    this.currentAngleY = angleY;
    this.updateSunPosition();
  }

  /**
   * Set sun height (elevation) over camera
   * @param elevation Vertical angle in degrees (-90=down, 0=horizon, 90=up)
   */
  setSunHeightOverCamera(elevation: number): void {
    this.currentElevation = elevation;
    this.updateSunPosition();
  }

  /**
   * Update sun emitter position based on current angleY and elevation
   */
  private updateSunPosition(): void {
    if (!this.sunEmitter) return;

    // Convert to radians
    const angleYRad = this.currentAngleY * (Math.PI / 180);
    const elevationRad = this.currentElevation * (Math.PI / 180);

    // Calculate position on sphere (relative to camera)
    const y = this.orbitRadius * Math.sin(elevationRad);
    const horizontalDist = this.orbitRadius * Math.cos(elevationRad);
    const x = horizontalDist * Math.sin(angleYRad);
    const z = horizontalDist * Math.cos(angleYRad);

    this.sunEmitter.position.set(x, y, z);

    logger.info('Sun position updated', {
      angleY: this.currentAngleY,
      elevation: this.currentElevation,
      position: { x, y, z },
    });
  }

  /**
   * Set sun color
   * @param r Red component (0-1)
   * @param g Green component (0-1)
   * @param b Blue component (0-1)
   */
  setSunColor(r: number, g: number, b: number): void {
    this.sunColor = new Color4(r, g, b, 1);

    // Update all particle system colors
    if (this.coreSystem) {
      this.coreSystem.color1 = this.sunColor;
      this.coreSystem.color2 = new Color4(r * 0.95, g * 0.95, b * 0.8, 1);
    }

    if (this.glowSystem) {
      this.glowSystem.color1 = new Color4(r, g * 0.95, b * 0.7, 0.5);
      this.glowSystem.color2 = new Color4(r, g * 0.9, b * 0.6, 0.4);
    }

    // Update ray colors
    const raySystems = [
      this.northRaySystem,
      this.southRaySystem,
      this.eastRaySystem,
      this.westRaySystem,
    ];

    raySystems.forEach((system) => {
      if (system) {
        system.color1 = new Color4(r, g, b * 0.95, 0.9);
        system.color2 = new Color4(r, g * 0.95, b * 0.85, 0.8);
      }
    });

    logger.info('Sun color updated', { r, g, b });
  }

  /**
   * Enable/disable sun visibility
   * @param enabled True to show sun, false to hide
   */
  setEnabled(enabled: boolean): void {
    this.enabled = enabled;

    const systems = [
      this.coreSystem,
      this.glowSystem,
      this.northRaySystem,
      this.southRaySystem,
      this.eastRaySystem,
      this.westRaySystem,
    ];

    systems.forEach((system) => {
      if (system) {
        if (enabled) {
          system.start();
        } else {
          system.stop();
        }
      }
    });

    logger.info('Sun visibility changed', { enabled });
  }

  /**
   * Get current sun position
   */
  getSunPosition(): { angleY: number; elevation: number } {
    return {
      angleY: this.currentAngleY,
      elevation: this.currentElevation,
    };
  }

  /**
   * Cleanup and dispose resources
   */
  dispose(): void {
    // Dispose particle systems
    this.coreSystem?.dispose();
    this.glowSystem?.dispose();
    this.northRaySystem?.dispose();
    this.southRaySystem?.dispose();
    this.eastRaySystem?.dispose();
    this.westRaySystem?.dispose();

    // Dispose textures
    this.coreTexture?.dispose();
    this.glowTexture?.dispose();
    this.rayTexture?.dispose();

    // Dispose emitter
    this.sunEmitter?.dispose();

    logger.info('SunService disposed');
  }
}
