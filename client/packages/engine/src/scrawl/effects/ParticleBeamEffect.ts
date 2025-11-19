/**
 * ParticleBeamEffect - Magical beam effect between two positions
 *
 * Creates three intertwining particle strands that form a magical beam.
 * The beam builds up from start to end, stays active, then fades out.
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

const logger = getLogger('ParticleBeamEffect');

/**
 * Options for ParticleBeamEffect
 */
export interface ParticleBeamOptions {
  /** Start position */
  startPosition: { x: number; y: number; z: number };

  /** End position */
  endPosition: { x: number; y: number; z: number };

  /** First beam color (hex format, e.g., "#ff0000") */
  color1: string;

  /** Second beam color (hex format, e.g., "#00ff00") */
  color2: string;

  /** Third beam color (hex format, e.g., "#0000ff") */
  color3: string;

  /** Total duration of the beam effect in seconds */
  duration: number;

  /** Thickness of the beam strands (default: 0.1) */
  thickness?: number;

  /** Alpha transparency of the beam (0.0 = fully transparent, 1.0 = fully opaque, default: 1.0) */
  alpha?: number;

  /** Speed multiplier for particle movement (default: 1.0) */
  speed?: number;

  /** Weight of color1 strand (0.0 = invisible, 1.0 = full opacity, default: 1.0) */
  color1Weight?: number;

  /** Weight of color2 strand (0.0 = invisible, 1.0 = full opacity, default: 1.0) */
  color2Weight?: number;

  /** Weight of color3 strand (0.0 = invisible, 1.0 = full opacity, default: 1.0) */
  color3Weight?: number;

  /** Setup duration - time to build beam from start to end (default: 0.2s) */
  setupDuration?: number;

  /** Fade duration - time to fade out at the end (default: 0.2s) */
  fadeDuration?: number;
}

/**
 * ParticleBeamEffect - Creates a magical beam with three intertwining particle strands
 *
 * Usage:
 * ```json
 * {
 *   "kind": "Play",
 *   "effectId": "particleBeam",
 *   "ctx": {
 *     "startPosition": {"x": 0, "y": 65, "z": 0},
 *     "endPosition": {"x": 10, "y": 65, "z": 10},
 *     "color1": "#ff0000",
 *     "color2": "#00ff00",
 *     "color3": "#0000ff",
 *     "duration": 2.0,
 *     "thickness": 0.1,
 *     "alpha": 0.8,
 *     "speed": 1.5,
 *     "color1Weight": 1.0,
 *     "color2Weight": 0.5,
 *     "color3Weight": 1.0,
 *     "setupDuration": 0.2,
 *     "fadeDuration": 0.2
 *   }
 * }
 * ```
 */
export class ParticleBeamEffect extends ScrawlEffectHandler<ParticleBeamOptions> {
  private particleSystems: ParticleSystem[] = [];
  private startTime: number = 0;
  private startPos: Vector3 | null = null;
  private endPos: Vector3 | null = null;
  private animationHandle: number | null = null;
  private scene: Scene | null = null;
  private particleTexture: RawTexture | null = null;

  async execute(ctx: ScrawlExecContext): Promise<void> {
    const scene = ctx.appContext.services.engine?.getScene();
    if (!scene) {
      logger.warn('Scene not available');
      return;
    }
    this.scene = scene;

    try {
      // Parse positions
      this.startPos = new Vector3(
        this.options.startPosition.x,
        this.options.startPosition.y,
        this.options.startPosition.z
      );
      this.endPos = new Vector3(
        this.options.endPosition.x,
        this.options.endPosition.y,
        this.options.endPosition.z
      );

      const thickness = this.options.thickness ?? 0.1;
      const alpha = this.options.alpha ?? 1.0;
      const speed = this.options.speed ?? 1.0;
      const setupDuration = this.options.setupDuration ?? 0.2;

      // Color weights for individual strands
      const colorWeights = [
        this.options.color1Weight ?? 1.0,
        this.options.color2Weight ?? 1.0,
        this.options.color3Weight ?? 1.0,
      ];

      // Create three particle systems for three intertwining strands
      const colors = [
        this.parseColor(this.options.color1),
        this.parseColor(this.options.color2),
        this.parseColor(this.options.color3),
      ];

      const direction = this.endPos.subtract(this.startPos);
      const distance = direction.length();

      // Create a simple white circular texture for particles
      const textureSize = 16;
      const textureData = new Uint8Array(textureSize * textureSize * 4);
      const center = textureSize / 2;

      for (let y = 0; y < textureSize; y++) {
        for (let x = 0; x < textureSize; x++) {
          const dx = x - center + 0.5;
          const dy = y - center + 0.5;
          const dist = Math.sqrt(dx * dx + dy * dy) / center;
          const alpha = Math.max(0, 1 - dist);

          const index = (y * textureSize + x) * 4;
          textureData[index] = 255;     // R
          textureData[index + 1] = 255; // G
          textureData[index + 2] = 255; // B
          textureData[index + 3] = Math.floor(alpha * 255); // A
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

      for (let i = 0; i < 3; i++) {
        // Skip this strand if weight is 0
        if (colorWeights[i] === 0) {
          continue;
        }

        const particleSystem = new ParticleSystem(
          `beamStrand${i}`,
          2000,
          this.scene
        );

        // Use the procedural circular texture
        particleSystem.particleTexture = this.particleTexture;

        // Emission from start point
        particleSystem.emitter = this.startPos.clone();

        // Particle appearance
        particleSystem.minSize = thickness * 0.5;
        particleSystem.maxSize = thickness * 1.5;
        particleSystem.minLifeTime = 0.1;
        particleSystem.maxLifeTime = 0.3;

        // Color
        particleSystem.color1 = colors[i];
        particleSystem.color2 = colors[i];
        particleSystem.colorDead = new Color4(colors[i].r, colors[i].g, colors[i].b, 0);

        // Emission rate (adjust based on beam length)
        particleSystem.emitRate = 500 * (distance / 10);

        // Direction along beam with slight spiral offset
        particleSystem.direction1 = direction.normalize();
        particleSystem.direction2 = direction.normalize();

        // Speed (particles travel the beam distance during setup, multiplied by speed parameter)
        const baseSpeed = distance / setupDuration;
        const effectiveSpeed = baseSpeed * speed;
        particleSystem.minEmitPower = effectiveSpeed * 0.9;
        particleSystem.maxEmitPower = effectiveSpeed * 1.1;

        // Gravity and forces
        particleSystem.gravity = Vector3.Zero();

        // Blending
        particleSystem.blendMode = ParticleSystem.BLENDMODE_ADD;

        // Custom update function for spiral effect
        const phaseOffset = (i * Math.PI * 2) / 3; // 120 degrees apart
        const spiralRadius = thickness * 2;
        const spiralSpeed = 4; // rotations per second

        particleSystem.updateFunction = (particles: any) => {
          const currentTime = this.now() - this.startTime;
          const beamProgress = Math.min(currentTime / setupDuration, 1.0);

          for (const particle of particles) {
            particle.age += this.scene!.getEngine().getDeltaTime() / 1000;

            if (particle.age >= particle.lifeTime) {
              particles.splice(particles.indexOf(particle), 1);
              continue;
            }

            // Calculate position along beam
            const normalizedAge = particle.age / particle.lifeTime;
            const beamPosition = normalizedAge * distance * beamProgress;

            // Apply spiral offset
            const spiralAngle = beamPosition * spiralSpeed + phaseOffset + particle._randomOffset;
            const perpendicular1 = Vector3.Cross(direction, Vector3.Up());
            if (perpendicular1.lengthSquared() < 0.0001) {
              // If beam is vertical, use a different reference vector
              perpendicular1.copyFrom(Vector3.Cross(direction, Vector3.Right()));
            }
            perpendicular1.normalize();
            const perpendicular2 = Vector3.Cross(direction, perpendicular1).normalize();

            const spiralOffset = perpendicular1.scale(Math.cos(spiralAngle) * spiralRadius)
              .add(perpendicular2.scale(Math.sin(spiralAngle) * spiralRadius));

            // Set particle position
            particle.position = this.startPos!.clone()
              .add(direction.normalize().scale(beamPosition))
              .add(spiralOffset);

            // Fade in/out based on effect phase
            const fadeInProgress = Math.min(currentTime / (setupDuration * 0.5), 1.0);
            const fadeOutStart = this.options.duration - (this.options.fadeDuration ?? 0.2);
            const fadeOutProgress = currentTime > fadeOutStart
              ? 1.0 - Math.min((currentTime - fadeOutStart) / (this.options.fadeDuration ?? 0.2), 1.0)
              : 1.0;

            particle.color.a = colors[i].a * alpha * colorWeights[i] * fadeInProgress * fadeOutProgress;
          }
        };

        // Add random offset to each particle on creation
        const originalStartParticle = particleSystem.startPositionFunction;
        particleSystem.startPositionFunction = (
          worldMatrix: any,
          positionToUpdate: Vector3,
          particle: any,
          isLocal: boolean
        ) => {
          if (originalStartParticle) {
            originalStartParticle(worldMatrix, positionToUpdate, particle, isLocal);
          }
          particle._randomOffset = Math.random() * Math.PI * 2;
        };

        particleSystem.start();
        this.particleSystems.push(particleSystem);
      }

      // Start animation
      this.startTime = this.now();
      this.animate();
    } catch (error) {
      logger.error('Failed to create particle beam effect', { error });
      this.cleanup();
    }
  }

  private animate = () => {
    const elapsed = this.now() - this.startTime;

    // Stop after duration
    if (elapsed >= this.options.duration) {
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
  }

  stop(): void {
    this.cleanup();
  }
}
