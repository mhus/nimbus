import { getLogger } from '@nimbus/shared';
import { ScrawlEffectHandler } from '../ScrawlEffectHandler';
import type { ScrawlExecContext } from '../ScrawlExecContext';
import {
  Vector3,
  MeshBuilder,
  StandardMaterial,
  Color3,
  Mesh,
  Scene,
} from '@babylonjs/core';

const logger = getLogger('BeamFollowEffect');

export interface BeamFollowOptions {
  color: string;
  thickness?: number;
  alpha?: number;
}

/**
 * Beam effect with dynamic target position
 * Reacts to parameter updates via onParameterChanged()
 */
export class BeamFollowEffect extends ScrawlEffectHandler<BeamFollowOptions> {
  private mesh: Mesh | null = null;
  private material: StandardMaterial | null = null;
  private sourcePos: Vector3 | null = null;
  private targetPos: Vector3 | null = null;
  private animationHandle: number | null = null;
  private scene: Scene | null = null;

  isSteadyEffect(): boolean {
    return true;
  }

  async execute(ctx: ScrawlExecContext): Promise<void> {
    const scene = ctx.appContext.services.engine?.getScene();
    if (!scene) {
      logger.warn('Scene not available');
      return;
    }
    this.scene = scene;

    // Get initial positions from context
    const source = (ctx as any).source || ctx.actor;
    if (source) {
      this.sourcePos = new Vector3(
        source.position.x,
        source.position.y,
        source.position.z
      );
    }

    const target = (ctx as any).target || (ctx.patients && ctx.patients.length > 0 ? ctx.patients[0] : undefined);
    if (target) {
      this.targetPos = new Vector3(
        target.position.x,
        target.position.y,
        target.position.z
      );
    }

    if (!this.sourcePos || !this.targetPos) {
      logger.warn('Source or target position missing');
      return;
    }

    // Create beam geometry
    this.createBeam();

    // Start animation
    this.animate();

    logger.debug('Beam follow effect started', {
      source: this.sourcePos,
      target: this.targetPos,
    });
  }

  onParameterChanged(paramName: string, value: any, ctx: ScrawlExecContext): void {
    // Update target position from context
    const target = (ctx as any).target || (ctx.patients && ctx.patients.length > 0 ? ctx.patients[0] : undefined);
    if (target) {
      const newTarget = new Vector3(
        target.position.x,
        target.position.y,
        target.position.z
      );

      this.targetPos = newTarget;

      logger.debug('Beam target position updated', {
        paramName,
        newTarget,
      });
    }
  }

  private createBeam(): void {
    if (!this.scene || !this.sourcePos || !this.targetPos) return;

    // Cylinder between source and target
    const distance = Vector3.Distance(this.sourcePos, this.targetPos);
    this.mesh = MeshBuilder.CreateCylinder(
      'beam',
      {
        height: distance,
        diameter: this.options.thickness ?? 0.1,
      },
      this.scene
    );

    // Glowing material
    this.material = new StandardMaterial('beamMat', this.scene);
    this.material.emissiveColor = Color3.FromHexString(this.options.color);
    this.material.alpha = this.options.alpha ?? 1.0;
    this.mesh.material = this.material;

    this.updateBeamTransform();
  }

  private updateBeamTransform(): void {
    if (!this.mesh || !this.sourcePos || !this.targetPos) return;

    // Position at midpoint
    const midpoint = Vector3.Center(this.sourcePos, this.targetPos);
    this.mesh.position = midpoint;

    // Orient towards target
    const direction = this.targetPos.subtract(this.sourcePos);
    this.mesh.lookAt(this.targetPos);
    this.mesh.rotate(Vector3.Right(), Math.PI / 2);

    // Scale to distance
    const distance = direction.length();
    this.mesh.scaling.y = distance;
  }

  private animate = (): void => {
    if (!this.isRunning()) {
      return;
    }

    // Update beam transform every frame
    this.updateBeamTransform();

    this.animationHandle = requestAnimationFrame(this.animate);
  };

  stop(): void {
    if (this.animationHandle !== null) {
      cancelAnimationFrame(this.animationHandle);
      this.animationHandle = null;
    }

    if (this.mesh) {
      this.mesh.dispose();
      this.mesh = null;
    }

    if (this.material) {
      this.material.dispose();
      this.material = null;
    }

    logger.debug('Beam follow effect stopped');
  }

  isRunning(): boolean {
    return this.mesh !== null;
  }
}
