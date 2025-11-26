/**
 * PrecipitationService - Manages rain and snow particle effects
 *
 * Features:
 * - Particle-based rain and snow
 * - Follows camera movement
 * - Configurable intensity (0-100)
 * - Configurable particle size, color, and texture
 * - Can be enabled/disabled
 */

import {
  Scene,
  ParticleSystem,
  Texture,
  Color4,
  Vector3,
  TransformNode,
} from '@babylonjs/core';
import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { CameraService } from './CameraService';
import { RENDERING_GROUPS } from '../config/renderingGroups';

const logger = getLogger('PrecipitationService');

/**
 * Precipitation type
 */
export enum PrecipitationType {
  RAIN = 'rain',
  SNOW = 'snow',
}

/**
 * PrecipitationService - Manages precipitation effects
 */
export class PrecipitationService {
  private scene: Scene;
  private appContext: AppContext;
  private cameraService: CameraService;

  // Particle system
  private particleSystem?: ParticleSystem;
  private emitterNode?: TransformNode; // Dummy node as emitter, attached to camera root

  // Configuration
  private enabled: boolean = false;
  private intensity: number = 0; // 0-100
  private precipitationType: PrecipitationType = PrecipitationType.RAIN;

  // Particle properties
  private particleSize: number = 0.3; // Default size for rain
  private particleColor: Color4 = new Color4(0.5, 0.5, 0.8, 1.0); // Slightly blue for rain
  private particleTexture?: Texture;

  constructor(scene: Scene, appContext: AppContext) {
    this.scene = scene;
    this.appContext = appContext;
    this.cameraService = appContext.services.camera!;

    logger.info('PrecipitationService initialized');
  }

  /**
   * Enable or disable precipitation
   * @param enabled True to enable, false to disable
   */
  public setEnabled(enabled: boolean): void {
    if (this.enabled === enabled) return;

    this.enabled = enabled;

    if (enabled) {
      logger.info('Enabling precipitation...', {
        type: this.precipitationType,
        intensity: this.intensity,
        size: this.particleSize,
      });
      this.createParticleSystem();
    } else {
      logger.info('Disabling precipitation...');
      this.disposeParticleSystem();
    }

    logger.info('Precipitation enabled state changed', { enabled });
  }

  /**
   * Check if precipitation is enabled
   * @returns True if enabled
   */
  public isEnabled(): boolean {
    return this.enabled;
  }

  /**
   * Set precipitation intensity
   * @param intensity Intensity (0-100)
   */
  public setIntensity(intensity: number): void {
    // Clamp to valid range
    this.intensity = Math.max(0, Math.min(100, intensity));

    // Update particle system if active
    if (this.particleSystem) {
      this.updateParticleIntensity();
    }

    logger.info('Precipitation intensity changed', { intensity: this.intensity });
  }

  /**
   * Get current intensity
   * @returns Intensity (0-100)
   */
  public getIntensity(): number {
    return this.intensity;
  }

  /**
   * Set precipitation type
   * @param type Rain or snow
   */
  public setPrecipitationType(type: PrecipitationType): void {
    if (this.precipitationType === type) return;

    this.precipitationType = type;

    // Update default properties based on type
    if (type === PrecipitationType.RAIN) {
      this.particleSize = 0.3; // Medium size for visibility
      this.particleColor = new Color4(0.5, 0.5, 0.8, 1.0); // Slightly blue
    } else {
      // Snow
      this.particleSize = 0.5; // Larger for snow
      this.particleColor = new Color4(1.0, 1.0, 1.0, 1.0); // White
    }

    // Recreate particle system with new settings
    if (this.enabled) {
      this.disposeParticleSystem();
      this.createParticleSystem();
    }

    logger.info('Precipitation type changed', { type });
  }

  /**
   * Get current precipitation type
   * @returns Precipitation type
   */
  public getPrecipitationType(): PrecipitationType {
    return this.precipitationType;
  }

  /**
   * Set particle size
   * @param size Particle size
   */
  public setParticleSize(size: number): void {
    if (size <= 0) {
      throw new Error('Particle size must be positive');
    }

    this.particleSize = size;

    if (this.particleSystem) {
      this.particleSystem.minSize = size * 0.8;
      this.particleSystem.maxSize = size * 1.2;
    }

    logger.info('Particle size changed', { size });
  }

  /**
   * Get current particle size
   * @returns Particle size
   */
  public getParticleSize(): number {
    return this.particleSize;
  }

  /**
   * Set particle color
   * @param color Particle color (RGBA)
   */
  public setParticleColor(color: Color4): void {
    this.particleColor = color;

    if (this.particleSystem) {
      this.particleSystem.color1 = color;
      this.particleSystem.color2 = color;
    }

    logger.info('Particle color changed', {
      r: color.r,
      g: color.g,
      b: color.b,
      a: color.a,
    });
  }

  /**
   * Get current particle color
   * @returns Particle color
   */
  public getParticleColor(): Color4 {
    return this.particleColor.clone();
  }

  /**
   * Set particle texture
   * @param texturePath Path to texture or null to remove
   */
  public setParticleTexture(texturePath: string | null): void {
    // Dispose old texture
    if (this.particleTexture) {
      this.particleTexture.dispose();
      this.particleTexture = undefined;
    }

    // Load new texture
    if (texturePath) {
      try {
        this.particleTexture = new Texture(texturePath, this.scene);
        if (this.particleSystem) {
          this.particleSystem.particleTexture = this.particleTexture;
        }
        logger.info('Particle texture loaded', { path: texturePath });
      } catch (error) {
        logger.error('Failed to load particle texture', { path: texturePath, error });
      }
    } else {
      // No texture - use default point sprite
      if (this.particleSystem) {
        this.particleSystem.particleTexture = null;
      }
    }
  }

  /**
   * Update precipitation (called every frame)
   * @param deltaTime Time since last frame in seconds
   */
  public update(deltaTime: number): void {
    // Update emitter position to follow camera
    if (this.particleSystem && this.emitterNode && this.enabled) {
      // Get world position of emitter node (which follows camera via parent)
      const worldPos = this.emitterNode.getAbsolutePosition();
      this.particleSystem.emitter = worldPos;
    }
  }

  /**
   * Dispose precipitation service
   */
  public dispose(): void {
    this.disposeParticleSystem();
    this.emitterNode?.dispose();
    this.emitterNode = undefined;
    this.particleTexture?.dispose();
    logger.info('PrecipitationService disposed');
  }

  // ========== Private Helper Methods ==========

  /**
   * Create particle system
   */
  private createParticleSystem(): void {
    // Get camera environment root
    const cameraRoot = this.cameraService.getCameraEnvironmentRoot();
    if (!cameraRoot) {
      logger.error('Camera environment root not available');
      return;
    }

    // Create dummy emitter node attached to camera environment root
    this.emitterNode = new TransformNode('precipitationEmitter', this.scene);
    this.emitterNode.parent = cameraRoot;
    this.emitterNode.position.y = 20; // 20 blocks above camera

    // Create particle system
    this.particleSystem = new ParticleSystem('precipitation', 2000, this.scene);

    // Set emitter to the world position (will be updated in update())
    this.particleSystem.emitter = this.emitterNode.getAbsolutePosition();

    // Emitter area - emit particles in area around camera
    this.particleSystem.minEmitBox = new Vector3(-50, 0, -50);
    this.particleSystem.maxEmitBox = new Vector3(50, 0, 50);

    // Particle direction (downward)
    this.particleSystem.direction1 = new Vector3(0, -1, 0);
    this.particleSystem.direction2 = new Vector3(0, -1, 0);

    // Particle speed
    if (this.precipitationType === PrecipitationType.RAIN) {
      this.particleSystem.minEmitPower = 20;
      this.particleSystem.maxEmitPower = 30;
    } else {
      // Snow falls slower
      this.particleSystem.minEmitPower = 3;
      this.particleSystem.maxEmitPower = 6;
    }

    // Particle lifetime (longer to see them fall)
    this.particleSystem.minLifeTime = 3;
    this.particleSystem.maxLifeTime = 5;

    // Particle size
    this.particleSystem.minSize = this.particleSize * 0.8;
    this.particleSystem.maxSize = this.particleSize * 1.2;

    // Particle color
    this.particleSystem.color1 = this.particleColor;
    this.particleSystem.color2 = this.particleColor;
    this.particleSystem.colorDead = new Color4(this.particleColor.r, this.particleColor.g, this.particleColor.b, 0);

    // Texture - if no texture is set, particles will be rendered as simple sprites
    if (this.particleTexture) {
      this.particleSystem.particleTexture = this.particleTexture;
    }

    // Make sure particles are visible
    this.particleSystem.updateSpeed = 0.01;

    // Emission rate (updated by intensity)
    this.updateParticleIntensity();

    // Gravity (particles fall down)
    this.particleSystem.gravity = new Vector3(0, -9.81, 0);

    // Blending
    this.particleSystem.blendMode = ParticleSystem.BLENDMODE_STANDARD;

    // Rendering group
    this.particleSystem.renderingGroupId = RENDERING_GROUPS.PRECIPITATION;

    // Start emitting
    this.particleSystem.start();

    logger.info('Particle system created', {
      type: this.precipitationType,
      intensity: this.intensity,
      size: this.particleSize,
      color: this.particleColor,
    });
  }

  /**
   * Update particle emission rate based on intensity
   */
  private updateParticleIntensity(): void {
    if (!this.particleSystem) return;

    // Map intensity (0-100) to emission rate (0-500 particles/sec)
    const emissionRate = (this.intensity / 100) * 500;
    this.particleSystem.emitRate = emissionRate;

    logger.debug('Particle intensity updated', {
      intensity: this.intensity,
      emissionRate,
    });
  }

  /**
   * Dispose particle system
   */
  private disposeParticleSystem(): void {
    if (this.particleSystem) {
      this.particleSystem.dispose();
      this.particleSystem = undefined;
      logger.debug('Particle system disposed');
    }
  }
}
