/**
 * WalkModeController - Walk/ground movement physics
 *
 * Implements the complete doMovement schema:
 * 1. Vorbereitung (Teleport/Flight checks)
 * 2. Environment prüfen (passableFrom)
 * 3. Bodenprüfung / Auto-Funktionen
 * 4. Semi-Solid & Slopes
 * 5. Bewegung / Kollision
 * 6. Weltgrenzen
 * 7. Bewegung anwenden
 */

import { Vector3 } from '@babylonjs/core';
import { getLogger } from '@nimbus/shared';
import type { PhysicsEntity } from './types';
import type { AppContext } from '../../AppContext';
import type { ChunkService } from '../ChunkService';
import { BlockContextAnalyzer } from './BlockContextAnalyzer';
import { CollisionDetector } from './CollisionDetector';
import { MovementResolver, PhysicsConfig } from './MovementResolver';
import { SurfaceAnalyzer } from './SurfaceAnalyzer';
import * as PhysicsUtils from './PhysicsUtils';

const logger = getLogger('WalkModeController');

/**
 * Entity dimensions
 */
interface EntityDimensions {
  height: number;
  width: number;
  footprint: number;
}

/**
 * WalkModeController - Handles walk/ground physics
 */
export class WalkModeController {
  private contextAnalyzer: BlockContextAnalyzer;
  private collisionDetector: CollisionDetector;
  private movementResolver: MovementResolver;
  private surfaceAnalyzer: SurfaceAnalyzer;

  constructor(
    private appContext: AppContext,
    private chunkService: ChunkService,
    physicsConfig: PhysicsConfig
  ) {
    this.contextAnalyzer = new BlockContextAnalyzer(chunkService);
    this.collisionDetector = new CollisionDetector(
      chunkService,
      this.contextAnalyzer,
      physicsConfig.maxClimbHeight
    );
    this.movementResolver = new MovementResolver(physicsConfig);
    this.surfaceAnalyzer = new SurfaceAnalyzer(chunkService);

    // Setup collision event callback
    this.collisionDetector.setCollisionEventCallback((x, y, z, id, gId) => {
      // Send collision event to server
      this.appContext.services.network.sendBlockInteraction(
        x,
        y,
        z,
        'collision',
        undefined,
        id,
        gId
      );
    });
  }

  /**
   * Main movement update - implements complete schema
   */
  doMovement(
    entity: PhysicsEntity,
    movementVector: Vector3,
    startJump: boolean,
    dimensions: EntityDimensions,
    deltaTime: number
  ): void {
    // === 1. VORBEREITUNG ===

    // Store movement intention
    entity.wishMove.copyFrom(movementVector);

    // Check if chunks are loaded
    const chunkSize = this.appContext.worldInfo?.chunkSize || 16;
    if (!PhysicsUtils.isChunkLoaded(entity.position.x, entity.position.z, this.chunkService, chunkSize)) {
      // Chunks not loaded - prevent movement
      entity.velocity.x = 0;
      entity.velocity.z = 0;
      return;
    }

    // === 2. ENVIRONMENT PRÜFEN ===

    const context = this.contextAnalyzer.getContext(entity, dimensions);

    // Check if stuck in solid block
    if (context.currentBlocks.hasSolid && !context.currentBlocks.allNonSolid) {
      // Check passableFrom
      if (context.currentBlocks.passableFrom !== undefined) {
        // Has passableFrom - may be able to exit
        // This is handled in collision detection
      } else {
        // Completely stuck - try to push up
        if (this.collisionDetector.checkAndPushUp(entity, dimensions)) {
          return; // Pushed up, skip this frame
        }
      }
    }

    // === 3. BODENPRÜFUNG / AUTO-FUNKTIONEN ===

    // Update grounded state
    this.collisionDetector.checkGroundCollision(entity, dimensions);

    // Apply auto-functions from ground/foot blocks
    if (entity.grounded || context.footBlocks.hasSolid) {
      // Auto-rotation
      if (context.footBlocks.hasAutoRotationY && context.footBlocks.autoOrientationY !== undefined) {
        this.movementResolver.applyAutoOrientation(
          entity,
          context.footBlocks.autoOrientationY,
          deltaTime
        );
      } else if (
        context.groundBlocks.hasAutoRotationY &&
        context.groundBlocks.autoOrientationY !== undefined
      ) {
        this.movementResolver.applyAutoOrientation(
          entity,
          context.groundBlocks.autoOrientationY,
          deltaTime
        );
      }

      // Auto-move
      if (context.footBlocks.hasAutoMove) {
        this.movementResolver.applyAutoMove(entity, context.footBlocks.autoMove, deltaTime);
      } else if (context.groundBlocks.hasAutoMove) {
        this.movementResolver.applyAutoMove(entity, context.groundBlocks.autoMove, deltaTime);
      }

      // Auto-jump
      if (context.footBlocks.hasAutoJump || context.groundBlocks.hasAutoJump) {
        entity.canAutoJump = true;
        if (!startJump) {
          // Trigger auto-jump
          startJump = true;
        }
      }
    }

    // === 4. SEMI-SOLID & SLOPES ===

    if (entity.onSlope && context.groundFootBlocks.cornerHeights) {
      // Apply slope forces
      const slope = this.surfaceAnalyzer.calculateSlope(context.groundFootBlocks.cornerHeights);
      this.movementResolver.applySlopeForces(entity, slope, deltaTime);

      // Clamp to slope surface
      if (context.groundFootBlocks.maxHeight > 0) {
        const surfaceY = Math.floor(entity.position.y) + 1.0 + context.groundFootBlocks.maxHeight;
        if (entity.position.y < surfaceY) {
          entity.position.y = surfaceY;
        }
      }
    }

    // === 5. BEWEGUNG / KOLLISION ===

    // Update velocity
    const resistance = context.groundBlocks.resistance;
    this.movementResolver.updateVelocity(entity, entity.wishMove, context, resistance, deltaTime);

    // Handle jump
    this.movementResolver.handleJump(entity, startJump, deltaTime);

    // Calculate next position
    const wishPosition = entity.position.add(entity.velocity.scale(deltaTime));

    // Resolve collisions (Swept-AABB: Y → X → Z)
    const resolvedPosition = this.collisionDetector.resolveCollision(
      entity,
      wishPosition,
      dimensions
    );

    // === 5.5 ENTITY COLLISIONS ===

    // Check entity collisions
    const entityService = this.appContext.services.entity;
    if (entityService) {
      const entitiesInRadius = entityService.getEntitiesInRadius(
        resolvedPosition,
        entityService.getCollisionCheckRadius(),
        entity.movementMode
      );

      const entityCollisionResult = this.collisionDetector.checkEntityCollisions(
        resolvedPosition,
        dimensions,
        entitiesInRadius
      );

      // Apply entity collision correction
      resolvedPosition.copyFrom(entityCollisionResult.position);

      // Notify EntityService of collisions
      for (const entityId of entityCollisionResult.collidedEntities) {
        entityService.onPlayerCollision(entityId, resolvedPosition);
      }
    }

    // === 6. WELTGRENZEN ===

    // Apply position
    entity.position.copyFrom(resolvedPosition);

    // Clamp to world bounds
    PhysicsUtils.clampToWorldBounds(entity, this.appContext);

    // Clamp to loaded chunks
    PhysicsUtils.clampToLoadedChunks(
      entity,
      entity.position.x,
      entity.position.z,
      this.chunkService,
      chunkSize
    );

    // === 7. CHECK UNDERWATER STATE ===

    // Only check if block position changed
    if (PhysicsUtils.hasBlockPositionChanged(entity)) {
      const eyeHeight = dimensions.height * 0.9; // Eye height approximation
      PhysicsUtils.checkUnderwaterState(entity, this.chunkService, this.appContext, eyeHeight);

      // Invalidate context cache
      this.contextAnalyzer.invalidateCache(entity.entityId);
    }
  }

  /**
   * Dispose resources
   */
  dispose(): void {
    this.movementResolver.dispose();
  }
}
