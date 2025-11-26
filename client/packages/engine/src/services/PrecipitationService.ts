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
  RawTexture,
  Constants,
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

  // Configuration
  private enabled: boolean = false;
  private intensity: number = 0; // 0-100
  private precipitationType: PrecipitationType = PrecipitationType.RAIN;

  // Particle properties (these are used when creating the particle system)
  private particleSize: number = 0.3;
  private particleColor: Color4 = new Color4(0.5, 0.5, 0.8, 1.0);
  private particleSpeed: number = 25; // EmitPower
  private particleGravity: number = 15; // Gravity strength
  private particleTexture?: RawTexture;

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
      this.createParticleSystem();
    } else {
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
   * @param intensity Intensity (0+, higher = more particles)
   */
  public setIntensity(intensity: number): void {
    // Clamp to non-negative
    this.intensity = Math.max(0, intensity);

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

    // Remember current state
    const wasEnabled = this.enabled;

    // Change type
    this.precipitationType = type;

    // Dispose old particle system if exists
    if (this.particleSystem) {
      this.disposeParticleSystem();
    }

    // Recreate particle system if it was enabled
    if (wasEnabled) {
      this.createParticleSystem();
    }

    logger.info('Precipitation type changed', { type, wasEnabled });
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
    logger.info('Particle size set', { size });
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
    logger.info('Particle color set', {
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
   * Set particle speed (emit power)
   * @param speed Emit power value
   */
  public setParticleSpeed(speed: number): void {
    if (speed < 0) {
      throw new Error('Particle speed cannot be negative');
    }

    this.particleSpeed = speed;
    logger.info('Particle speed set', { speed });
  }

  /**
   * Set particle gravity
   * @param gravity Gravity strength (negative for downward)
   */
  public setParticleGravity(gravity: number): void {
    this.particleGravity = Math.abs(gravity);
    logger.info('Particle gravity set', { gravity: this.particleGravity });
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
    if (this.particleSystem && this.enabled) {
      // Get camera position and update emitter
      const cameraPos = this.cameraService.getPosition();
      if (cameraPos) {
        const emitterPos = cameraPos.clone();
        emitterPos.y += 30; // 30 blocks above camera
        this.particleSystem.emitter = emitterPos;

        // Optional: Debug logging (disabled by default)
        // if (Math.random() < 0.016) {
        //   logger.debug('Precipitation update', {
        //     activeParticles: this.particleSystem.getActiveCount(),
        //   });
        // }
      }
    }
  }

  /**
   * Dispose precipitation service
   */
  public dispose(): void {
    this.disposeParticleSystem();
    this.particleTexture?.dispose();
    logger.info('PrecipitationService disposed');
  }

  // ========== Private Helper Methods ==========

  /**
   * Create particle system
   */
  private createParticleSystem(): void {
    // Get camera position
    const cameraPos = this.cameraService.getPosition();
    if (!cameraPos) {
      logger.error('Camera position not available');
      return;
    }

    // Create particle texture (CRITICAL - without this, particles won't render!)
    this.createParticleTexture();

    // Create particle system with large capacity
    // At 500 particles/sec with 6 sec lifetime = 3000 particles
    // Allow up to 20000 for heavy precipitation
    this.particleSystem = new ParticleSystem('precipitation', 20000, this.scene);

    // Set emitter ABOVE camera
    const emitterPos = cameraPos.clone();
    emitterPos.y += 30; // 30 blocks above camera
    this.particleSystem.emitter = emitterPos;

    // LARGE emitter area - min 5x5 chunks, camera in center
    this.particleSystem.minEmitBox = new Vector3(-80, -5, -80);
    this.particleSystem.maxEmitBox = new Vector3(80, 5, 80);

    // USE the texture we created
    this.particleSystem.particleTexture = this.particleTexture!;

    // Use stored particle properties
    this.particleSystem.direction1 = new Vector3(-0.1, -1, -0.1);
    this.particleSystem.direction2 = new Vector3(0.1, -1, 0.1);
    this.particleSystem.minEmitPower = this.particleSpeed * 0.8;
    this.particleSystem.maxEmitPower = this.particleSpeed * 1.2;
    this.particleSystem.gravity = new Vector3(0, -this.particleGravity, 0);

    // Particle size from stored value
    this.particleSystem.minSize = this.particleSize * 0.8;
    this.particleSystem.maxSize = this.particleSize * 1.2;

    // Particle color from stored value
    this.particleSystem.addColorGradient(0.0, this.particleColor);
    this.particleSystem.addColorGradient(1.0, this.particleColor);

    // Particle lifetime
    this.particleSystem.minLifeTime = 4;
    this.particleSystem.maxLifeTime = 6;

    // Size gradients - constant size
    this.particleSystem.addSizeGradient(0.0, 1.0);
    this.particleSystem.addSizeGradient(1.0, 1.0);

    // Emission rate based on intensity (direct mapping: intensity = particles/sec)
    this.particleSystem.emitRate = this.intensity;

    // Emission rate (based on intensity)
    this.updateParticleIntensity();

    // Billboard mode
    this.particleSystem.isBillboardBased = true;

    // Blending - use ADD for better visibility
    this.particleSystem.blendMode = ParticleSystem.BLENDMODE_ADD;

    // Update speed
    this.particleSystem.updateSpeed = 0.02;

    // Rendering group
    this.particleSystem.renderingGroupId = RENDERING_GROUPS.PRECIPITATION;

    // Start emitting
    this.particleSystem.start();

    logger.info('✨ Particle system created and started', {
      type: this.precipitationType,
      intensity: this.intensity,
      emissionRate: this.particleSystem.emitRate,
      particleCount: this.particleSystem.getCapacity(),
      size: {
        min: this.particleSystem.minSize,
        max: this.particleSystem.maxSize,
      },
      color: {
        r: this.particleColor.r,
        g: this.particleColor.g,
        b: this.particleColor.b,
        a: this.particleColor.a,
      },
      emitPower: {
        min: this.particleSystem.minEmitPower,
        max: this.particleSystem.maxEmitPower,
      },
      lifetime: {
        min: this.particleSystem.minLifeTime,
        max: this.particleSystem.maxLifeTime,
      },
      emitterPosition: this.particleSystem.emitter,
      emitBox: {
        min: this.particleSystem.minEmitBox,
        max: this.particleSystem.maxEmitBox,
      },
      renderingGroupId: this.particleSystem.renderingGroupId,
      isStarted: this.particleSystem.isStarted(),
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

    logger.info('💧 Particle intensity updated', {
      intensity: this.intensity,
      emissionRate,
      isStarted: this.particleSystem.isStarted(),
    });
  }

  /**
   * Create particle texture - simple circular gradient
   */
  private createParticleTexture(): void {
    // Always recreate texture to avoid disposed texture issues
    if (this.particleTexture) {
      try {
        this.particleTexture.dispose();
      } catch (e) {
        // Ignore dispose errors
      }
      this.particleTexture = undefined;
    }

    const textureSize = 16;
    const textureData = new Uint8Array(textureSize * textureSize * 4);
    const center = textureSize / 2;

    for (let y = 0; y < textureSize; y++) {
      for (let x = 0; x < textureSize; x++) {
        const dx = x - center + 0.5;
        const dy = y - center + 0.5;
        const dist = Math.sqrt(dx * dx + dy * dy) / center;
        const texAlpha = Math.max(0, 1 - dist);

        const index = (y * textureSize + x) * 4;
        textureData[index] = 255;     // R
        textureData[index + 1] = 255; // G
        textureData[index + 2] = 255; // B
        textureData[index + 3] = Math.floor(texAlpha * 255); // A
      }
    }

    this.particleTexture = RawTexture.CreateRGBATexture(
      textureData,
      textureSize,
      textureSize,
      this.scene,
      false,
      false,
      Constants.TEXTURE_BILINEAR_SAMPLINGMODE
    );

    logger.info('Particle texture created');
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
