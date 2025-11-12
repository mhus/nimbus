/**
 * EntityRenderService - Renders entities in the world
 *
 * Manages entity visualization:
 * - Loads entity models via ModelService
 * - Creates and updates entity meshes
 * - Handles entity animations and poses
 * - Responds to EntityService events (appear, disappear, transform, pose changes)
 */

import { Mesh, Scene, Vector3, AnimationGroup } from '@babylonjs/core';
import { getLogger, ExceptionHandler, type ClientEntity, type EntityPathway } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { EntityService } from './EntityService';
import type { ModelService } from './ModelService';

const logger = getLogger('EntityRenderService');

/**
 * Rendered entity data
 */
interface RenderedEntity {
  /** Entity ID */
  id: string;

  /** Root mesh */
  mesh: Mesh;

  /** Pathway lines (for debugging) */
  pathwayLines?: any;

  /** Animation groups (if any) */
  animations?: AnimationGroup[];

  /** Current animation group playing */
  currentAnimation?: AnimationGroup;

  /** Current pose ID (to detect changes) */
  currentPose?: number;
}

/**
 * EntityRenderService - Manages entity rendering
 *
 * Features:
 * - Listens to EntityService events
 * - Loads models via ModelService
 * - Creates and updates entity meshes
 * - Handles animations and poses
 * - Automatic cleanup when entities disappear
 */
export class EntityRenderService {
  private scene: Scene;
  private appContext: AppContext;
  private entityService: EntityService;
  private modelService: ModelService;

  // Rendered entities: entityId -> RenderedEntity
  private renderedEntities: Map<string, RenderedEntity> = new Map();

  constructor(scene: Scene, appContext: AppContext, entityService: EntityService, modelService: ModelService) {
    this.scene = scene;
    this.appContext = appContext;
    this.entityService = entityService;
    this.modelService = modelService;

    logger.info('EntityRenderService initialized');

    // Register event listeners
    this.registerEventListeners();
  }

  /**
   * Register event listeners on EntityService
   */
  private registerEventListeners(): void {
    // Listen for pathway updates (entity appears or moves)
    this.entityService.on('pathway', (pathway: EntityPathway) => {
      this.onEntityPathway(pathway);
    });

    // Listen for transform updates (position/rotation/pose changes)
    this.entityService.on('transform', (data: any) => {
      this.updateEntityTransform(data.entityId, data.position, data.rotation);
      if (data.pose !== undefined) {
        this.updateEntityPose(data.entityId, data.pose, data.velocity);
      }
    });

    // Listen for entity visibility changes
    this.entityService.on('visibility', (data: { entityId: string; visible: boolean }) => {
      this.onEntityVisibility(data.entityId, data.visible);
    });

    // Listen for entity removal
    this.entityService.on('removed', (entityId: string) => {
      this.onEntityRemoved(entityId);
    });

    logger.debug('Event listeners registered');
  }

  /**
   * Handle entity pathway update (entity appears or moves)
   */
  private async onEntityPathway(pathway: EntityPathway): Promise<void> {
    try {
      const entityId = pathway.entityId;

      logger.info('🔵 PATHWAY EVENT RECEIVED', {
        entityId,
        waypointCount: pathway.waypoints.length,
        firstWaypoint: pathway.waypoints[0],
        isAlreadyRendered: this.renderedEntities.has(entityId),
      });

      // Check if entity is already rendered
      if (this.renderedEntities.has(entityId)) {
        // Entity already exists, update pathway lines
        logger.debug('Entity pathway updated, redrawing lines', { entityId });
        await this.drawPathwayLines(entityId, pathway);
        return;
      }

      // Entity doesn't exist yet, create it
      logger.info('🔵 CREATING NEW ENTITY', { entityId });
      await this.createEntity(entityId);
      await this.drawPathwayLines(entityId, pathway);
    } catch (error) {
      ExceptionHandler.handle(error, 'EntityRenderService.onEntityPathway', {
        entityId: pathway.entityId,
      });
    }
  }

  /**
   * Handle entity visibility change
   */
  private onEntityVisibility(entityId: string, visible: boolean): void {
    try {
      const rendered = this.renderedEntities.get(entityId);
      if (!rendered) {
        return;
      }

      rendered.mesh.setEnabled(visible);
      logger.debug('Entity visibility changed', { entityId, visible });
    } catch (error) {
      ExceptionHandler.handle(error, 'EntityRenderService.onEntityVisibility', { entityId, visible });
    }
  }

  /**
   * Handle entity removal
   */
  private onEntityRemoved(entityId: string): void {
    try {
      this.removeEntity(entityId);
    } catch (error) {
      ExceptionHandler.handle(error, 'EntityRenderService.onEntityRemoved', { entityId });
    }
  }

  /**
   * Create and render entity
   */
  private async createEntity(entityId: string): Promise<void> {
    try {
      // Get entity from EntityService
      const clientEntity = await this.entityService.getEntity(entityId);
      if (!clientEntity) {
        logger.warn('Entity not found in EntityService', { entityId });
        return;
      }

      // Load GLB container via ModelService
      const modelPath = clientEntity.model.modelPath;
      const container = await this.modelService.loadGlbContainer(modelPath);
      if (!container) {
        logger.error('Failed to load entity model container', { entityId, modelPath });
        return;
      }

      // Instantiate model for this entity (clones mesh + skeleton + animations)
      const result = container.instantiateModelsToScene(
        name => `entity_${entityId}_${name}`,
        false, // Don't clone materials
        { doNotInstantiate: false }
      );

      if (!result.rootNodes || result.rootNodes.length === 0) {
        logger.error('No root nodes in instantiated model', { entityId, modelPath });
        return;
      }

      // Get root mesh
      const mesh = result.rootNodes[0] as Mesh;
      mesh.setEnabled(true);

      // Get cloned animation groups (automatically retargeted to this instance)
      const animations = result.animationGroups || [];

      logger.info('Entity model instantiated', {
        entityId,
        modelPath,
        meshCount: result.rootNodes.length,
        animationCount: animations.length,
        animationNames: animations.map(ag => ag.name),
      });

      // Apply initial transform
      const pos = clientEntity.currentPosition;
      const rot = clientEntity.currentRotation;

      mesh.position = new Vector3(pos.x, pos.y, pos.z);
      mesh.rotation.y = (rot.y * Math.PI) / 180;
      if ('p' in rot) {
        mesh.rotation.x = ((rot as any).p * Math.PI) / 180;
      }

      // Apply offset from model
      const offset = clientEntity.model.positionOffset;
      mesh.position.addInPlace(new Vector3(offset.x, offset.y, offset.z));

      // Apply rotation offset from model
      const rotOffset = clientEntity.model.rotationOffset;
      mesh.rotation.y += (rotOffset.y * Math.PI) / 180;
      if ('p' in rotOffset) {
        mesh.rotation.x += ((rotOffset as any).p * Math.PI) / 180;
      }

      // Apply scale from model
      const scale = clientEntity.model.scale;
      mesh.scaling = new Vector3(scale.x, scale.y, scale.z);

      // Store rendered entity
      const rendered: RenderedEntity = {
        id: entityId,
        mesh: mesh,
        animations: animations.length > 0 ? animations : undefined,
      };

      this.renderedEntities.set(entityId, rendered);

      // Start initial animation based on current pose
      if (animations.length > 0) {
        this.updateEntityPose(entityId, clientEntity.currentPose);
      }

      // Store mesh reference in ClientEntity for EntityService
      clientEntity.meshes = [mesh];

      logger.info('✅ ENTITY MODEL LOADED AND RENDERED', {
        entityId,
        modelPath,
        position: {
          x: mesh.position.x,
          y: mesh.position.y,
          z: mesh.position.z,
        },
        rotation: {
          y: mesh.rotation.y,
          x: mesh.rotation.x,
        },
        scale: {
          x: mesh.scaling.x,
          y: mesh.scaling.y,
          z: mesh.scaling.z,
        },
        visible: clientEntity.visible,
        meshEnabled: mesh.isEnabled(),
      });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'EntityRenderService.createEntity', { entityId });
    }
  }

  /**
   * Remove entity from scene
   */
  private removeEntity(entityId: string): void {
    const rendered = this.renderedEntities.get(entityId);
    if (!rendered) {
      return;
    }

    // Stop animations
    if (rendered.currentAnimation) {
      rendered.currentAnimation.stop();
    }

    // Dispose animations
    if (rendered.animations) {
      for (const anim of rendered.animations) {
        anim.dispose();
      }
    }

    // Dispose pathway lines
    if (rendered.pathwayLines) {
      rendered.pathwayLines.dispose();
    }

    // Dispose mesh
    rendered.mesh.dispose();

    this.renderedEntities.delete(entityId);
    logger.debug('Entity removed', { entityId });
  }

  /**
   * Update entity transform (called by EntityService or interpolation)
   */
  updateEntityTransform(
    entityId: string,
    position: { x: number; y: number; z: number },
    rotation: { y: number; p?: number }
  ): void {
    const rendered = this.renderedEntities.get(entityId);
    if (!rendered) {
      return;
    }

    rendered.mesh.position.set(position.x, position.y, position.z);
    rendered.mesh.rotation.y = (rotation.y * Math.PI) / 180;
    if (rotation.p !== undefined) {
      rendered.mesh.rotation.x = (rotation.p * Math.PI) / 180;
    }

    // Log every 10th update to avoid spam
    if (Math.random() < 0.1) {
      logger.debug('🔄 Entity transform updated', {
        entityId,
        position,
        rotation,
      });
    }
  }

  /**
   * Update entity pose/animation
   */
  updateEntityPose(entityId: string, pose: number, velocity?: number): void {
    const rendered = this.renderedEntities.get(entityId);
    if (!rendered || !rendered.animations) {
      return;
    }

    // Check if pose actually changed
    if (rendered.currentPose === pose) {
      return; // Pose hasn't changed, don't restart animation
    }

    // Get entity to check pose mapping
    const clientEntity = this.entityService.getAllEntities().find(e => e.id === entityId);
    if (!clientEntity) {
      return;
    }

    // Get animation config from pose mapping
    const poseConfig = clientEntity.model.poseMapping.get(pose);
    if (!poseConfig) {
      logger.warn('No animation config found for pose', { entityId, pose });
      return;
    }

    // Find animation group by name (instantiated animations have entity prefix)
    const animation = rendered.animations.find(a =>
      a.name === poseConfig.animationName ||
      a.name === `entity_${entityId}_${poseConfig.animationName}`
    );
    if (!animation) {
      logger.warn('Animation not found', {
        entityId,
        animationName: poseConfig.animationName,
        searchedFor: [poseConfig.animationName, `entity_${entityId}_${poseConfig.animationName}`],
        availableAnimations: rendered.animations.map(a => a.name),
      });
      return;
    }

    // Stop all animations first
    this.scene.animationGroups.forEach(a => a.stop());

    // Calculate speed ratio from speedMultiplier and velocity
    let speedRatio = poseConfig.speedMultiplier;
    if (velocity !== undefined && velocity > 0) {
      // Adjust speed based on actual movement velocity
      speedRatio = poseConfig.speedMultiplier * velocity;
    }

    // Play new animation
    animation.start(poseConfig.loop, speedRatio);
    rendered.currentAnimation = animation;
    rendered.currentPose = pose; // Remember current pose

    logger.info('Entity animation changed', {
      entityId,
      pose,
      animationName: poseConfig.animationName,
      loop: poseConfig.loop,
      speedRatio,
    });
  }

  /**
   * Get all rendered entities
   */
  getRenderedEntities(): RenderedEntity[] {
    return Array.from(this.renderedEntities.values());
  }

  /**
   * Get rendered entity by ID
   */
  getRenderedEntity(entityId: string): RenderedEntity | undefined {
    return this.renderedEntities.get(entityId);
  }

  /**
   * Draw pathway lines for debugging
   */
  private async drawPathwayLines(entityId: string, pathway: EntityPathway): Promise<void> {
    try {
      const rendered = this.renderedEntities.get(entityId);
      if (!rendered) {
        return;
      }

      // Remove old pathway lines if they exist
      if (rendered.pathwayLines) {
        rendered.pathwayLines.dispose();
        rendered.pathwayLines = undefined;
      }

      if (pathway.waypoints.length < 2) {
        logger.debug('Not enough waypoints to draw lines', { entityId, count: pathway.waypoints.length });
        return;
      }

      const { MeshBuilder, Color3 } = await import('@babylonjs/core');
      const points = pathway.waypoints.map(wp => new Vector3(wp.target.x, wp.target.y, wp.target.z));

      const lines = MeshBuilder.CreateLines(
        `pathway_lines_${entityId}`,
        { points },
        this.scene
      );
      lines.color = new Color3(0, 1, 0); // Green lines

      rendered.pathwayLines = lines;

      logger.info('🟢 PATHWAY LINES DRAWN', {
        entityId,
        waypointCount: pathway.waypoints.length,
        waypoints: pathway.waypoints.map(wp => ({
          x: wp.target.x,
          y: wp.target.y,
          z: wp.target.z,
        })),
      });
    } catch (error) {
      ExceptionHandler.handle(error, 'EntityRenderService.drawPathwayLines', { entityId });
    }
  }

  /**
   * Dispose service (cleanup all entities)
   */
  dispose(): void {
    // Remove all entities
    for (const entityId of Array.from(this.renderedEntities.keys())) {
      this.removeEntity(entityId);
    }

    logger.info('EntityRenderService disposed');
  }
}
