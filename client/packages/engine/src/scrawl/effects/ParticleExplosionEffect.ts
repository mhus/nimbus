/**
 * ParticleExplosionEffect - Particle explosion at a specific position
 *
 * Creates a radial particle explosion with up to three weighted colors.
 * Particles start at the initialRadius and expand outward to spreadRadius.
 */

import { getLogger } from '@nimbus/shared';
import { ScrawlEffectHandler } from '../ScrawlEffectHandler';
import type { ScrawlExecContext } from '../ScrawlExecContext';
import {
  Vector3,
  ParticleSystem,
  Color4,
  Scene,
  RawTexture,
  Constants,
} from '@babylonjs/core';

const logger = getLogger('ParticleExplosionEffect');

/**
 * Options for ParticleExplosionEffect
 */
export interface ParticleExplosionOptions {
  /** Position of the explosion */
  position: { x: number; y: number; z: number };

  /** First color (hex format, e.g., "#ff0000") */
  color1: string;

  /** Second color (hex format, e.g., "#ff9900") */
  color2: string;

  /** Third color (hex format, e.g., "#ffff00") */
  color3: string;

  /** Weight of color1 (0.0 = invisible, 1.0 = full opacity, default: 1.0) */
  color1Weight?: number;

  /** Weight of color2 (0.0 = invisible, 1.0 = full opacity, default: 1.0) */
  color2Weight?: number;

  /** Weight of color3 (0.0 = invisible, 1.0 = full opacity, default: 1.0) */
  color3Weight?: number;

  /** Initial radius where particles start (default: 0.1) */
  initialRadius?: number;

  /** Maximum spread radius (default: 5.0) */
  spreadRadius?: number;

  /** Number of particles to emit (default: 100) */
  particleCount?: number;

  /** Size of individual particles (default: 0.2) */
  particleSize?: number;

  /** Duration of the explosion effect in seconds (default: 1.0) */
  duration?: number;

  /** Speed multiplier for particle movement (default: 1.0) */
  speed?: number;

  /** Alpha transparency of particles (0.0 = fully transparent, 1.0 = fully opaque, default: 1.0) */
  alpha?: number;
}

/**
 * ParticleExplosionEffect - Creates a radial particle explosion
 *
 * Usage:
 * ```json
 * {
 *   "kind": "Play",
 *   "effectId": "particleExplosion",
 *   "ctx": {
 *     "position": {"x": 0, "y": 65, "z": 0},
 *     "color1": "#ff0000",
 *     "color2": "#ff9900",
 *     "color3": "#ffff00",
 *     "color1Weight": 1.0,
 *     "color2Weight": 0.7,
 *     "color3Weight": 0.3,
 *     "initialRadius": 0.1,
 *     "spreadRadius": 5.0,
 *     "particleCount": 200,
 *     "particleSize": 0.2,
 *     "duration": 1.5,
 *     "speed": 1.0,
 *     "alpha": 0.8
 *   }
 * }
 * ```
 */
export class ParticleExplosionEffect extends ScrawlEffectHandler<ParticleExplosionOptions> {
  private particleSystems: ParticleSystem[] = [];
  private startTime: number = 0;
  private animationHandle: number | null = null;
  private scene: Scene | null = null;
  private particleTexture: RawTexture | null = null;

  isSteadyEffect(): boolean {
    return false; // One-shot effect
  }

  async execute(ctx: ScrawlExecContext): Promise<void> {
    const scene = ctx.appContext.services.engine?.getScene();
    if (!scene) {
      logger.warn('Scene not available');
      return;
    }
    this.scene = scene;

    try {
      // Parse position
      const position = new Vector3(
        this.options.position.x,
        this.options.position.y,
        this.options.position.z
      );

      // Parse parameters with defaults
      const initialRadius = this.options.initialRadius ?? 0.1;
      const spreadRadius = this.options.spreadRadius ?? 5.0;
      const particleCount = this.options.particleCount ?? 100;
      const particleSize = this.options.particleSize ?? 0.2;
      const duration = this.options.duration ?? 1.0;
      const speed = this.options.speed ?? 1.0;
      const alpha = this.options.alpha ?? 1.0;

      // Color weights
      const colorWeights = [
        this.options.color1Weight ?? 1.0,
        this.options.color2Weight ?? 1.0,
        this.options.color3Weight ?? 1.0,
      ];

      // Parse colors
      const colors = [
        this.parseColor(this.options.color1),
        this.parseColor(this.options.color2),
        this.parseColor(this.options.color3),
      ];

      // Create a simple white circular texture for particles
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

      // Create particle system for each color with non-zero weight
      for (let i = 0; i < 3; i++) {
        // Skip this color if weight is 0
        if (colorWeights[i] === 0) {
          continue;
        }

        const particleSystem = new ParticleSystem(
          `explosion${i}`,
          particleCount,
          this.scene
        );

        // Use the procedural circular texture
        particleSystem.particleTexture = this.particleTexture;

        // Emission from explosion center
        particleSystem.emitter = position.clone();

        // Particle appearance
        particleSystem.minSize = particleSize * 0.8;
        particleSystem.maxSize = particleSize * 1.2;

        // Particle lifetime equals effect duration
        particleSystem.minLifeTime = duration * 0.9;
        particleSystem.maxLifeTime = duration * 1.1;

        // Color with weight applied (will be used in color gradients)
        const weightedColor = new Color4(
          colors[i].r,
          colors[i].g,
          colors[i].b,
          colors[i].a * alpha * colorWeights[i]
        );

        // Radial emission in all directions from a single point
        particleSystem.createSphereEmitter(0.01, 1); // Small inner radius, full outer radius

        // Calculate speed to reach spreadRadius over duration
        const baseSpeed = (spreadRadius - initialRadius) / duration;
        const effectiveSpeed = baseSpeed * speed;
        particleSystem.minEmitPower = effectiveSpeed * 0.8;
        particleSystem.maxEmitPower = effectiveSpeed * 1.2;

        // No gravity
        particleSystem.gravity = Vector3.Zero();

        // Additive blending for bright explosion
        particleSystem.blendMode = ParticleSystem.BLENDMODE_ADD;

        // Emit all particles at once (burst)
        particleSystem.manualEmitCount = Math.floor(particleCount / 3);

        // Disable looping
        particleSystem.targetStopDuration = 0;

        // Use addColorGradient for fade effect instead of custom update
        particleSystem.addColorGradient(0.0, new Color4(colors[i].r, colors[i].g, colors[i].b, 0.0));
        particleSystem.addColorGradient(0.1, weightedColor);
        particleSystem.addColorGradient(0.7, weightedColor);
        particleSystem.addColorGradient(1.0, new Color4(colors[i].r, colors[i].g, colors[i].b, 0.0));

        particleSystem.start();
        this.particleSystems.push(particleSystem);
      }

      // Start animation timer
      this.startTime = this.now();
      this.animate();

      logger.debug('Particle explosion effect started', {
        position,
        particleCount,
        duration,
      });
    } catch (error) {
      logger.error('Failed to create particle explosion effect', { error });
      this.cleanup();
    }
  }

  private animate = () => {
    const elapsed = this.now() - this.startTime;
    const duration = this.options.duration ?? 1.0;

    // Stop after duration + small buffer for cleanup
    if (elapsed >= duration + 0.5) {
      this.cleanup();
      return;
    }

    // Continue animation
    this.animationHandle = requestAnimationFrame(this.animate);
  };

  private parseColor(colorString: string): Color4 {
    // Simple hex color parser (#RRGGBB)
    if (colorString.startsWith('#')) {
      const hex = colorString.substring(1);
      const r = parseInt(hex.substring(0, 2), 16) / 255;
      const g = parseInt(hex.substring(2, 4), 16) / 255;
      const b = parseInt(hex.substring(4, 6), 16) / 255;
      return new Color4(r, g, b, 1.0);
    }

    // Fallback to white
    return new Color4(1, 1, 1, 1);
  }

  private cleanup() {
    if (this.animationHandle !== null) {
      cancelAnimationFrame(this.animationHandle);
      this.animationHandle = null;
    }

    for (const ps of this.particleSystems) {
      ps.stop();
      ps.dispose();
    }
    this.particleSystems = [];

    if (this.particleTexture) {
      this.particleTexture.dispose();
      this.particleTexture = null;
    }

    logger.debug('Particle explosion effect cleaned up');
  }

  stop(): void {
    this.cleanup();
  }
}
