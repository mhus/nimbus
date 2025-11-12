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

  /** Animation groups (if any) */
  animations?: AnimationGroup[];

  /** Current animation group playing */
  currentAnimation?: AnimationGroup;
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

      // Check if entity is already rendered
      if (this.renderedEntities.has(entityId)) {
        // Entity already exists, just update pathway
        logger.debug('Entity pathway updated', { entityId });
        return;
      }

      // Entity doesn't exist yet, create it
      await this.createEntity(entityId);
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

      // Load model via ModelService
      const modelPath = clientEntity.model.modelPath;
      const templateMesh = await this.modelService.loadModel(modelPath);
      if (!templateMesh) {
        logger.error('Failed to load entity model', { entityId, modelPath });
        return;
      }

      // Clone mesh for this entity
      const mesh = templateMesh.clone(`entity_${entityId}`, null)!;
      mesh.setEnabled(true);

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

      // Store rendered entity
      const rendered: RenderedEntity = {
        id: entityId,
        mesh: mesh,
      };

      this.renderedEntities.set(entityId, rendered);

      // Store mesh reference in ClientEntity for EntityService
      clientEntity.meshes = [mesh];

      logger.info('Entity created', {
        entityId,
        modelPath,
        position: mesh.position,
        rotation: mesh.rotation,
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
  }

  /**
   * Update entity pose/animation
   */
  updateEntityPose(entityId: string, pose: number): void {
    const rendered = this.renderedEntities.get(entityId);
    if (!rendered || !rendered.animations) {
      return;
    }

    // Get entity to check pose mapping
    const clientEntity = this.entityService.getAllEntities().find(e => e.id === entityId);
    if (!clientEntity) {
      return;
    }

    // Get animation name from pose mapping
    const animationName = clientEntity.model.poseMapping.get(pose);
    if (!animationName) {
      logger.warn('No animation found for pose', { entityId, pose });
      return;
    }

    // Find animation group by name
    const animation = rendered.animations.find(a => a.name === animationName);
    if (!animation) {
      logger.warn('Animation not found', { entityId, animationName });
      return;
    }

    // Stop current animation
    if (rendered.currentAnimation && rendered.currentAnimation !== animation) {
      rendered.currentAnimation.stop();
    }

    // Play new animation
    animation.start(true); // Loop
    rendered.currentAnimation = animation;

    logger.debug('Entity pose updated', { entityId, pose, animationName });
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
