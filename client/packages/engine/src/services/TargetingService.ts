/**
 * TargetingService - Centralized target resolution using Strategy Pattern
 *
 * Provides clean separation between:
 * - Visual targeting (for effects/poses)
 * - Interaction targeting (for server events)
 *
 * Each targeting mode implements a specific strategy for resolving targets.
 */

import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type {
  TargetingMode,
  ResolvedTarget,
  ClientEntity,
  ClientPosition,
} from '@nimbus/shared';
import { Vector3 } from '@babylonjs/core';

const logger = getLogger('TargetingService');

/**
 * Client-side block type (matches SelectService's getCurrentSelectedBlock return type)
 */
interface ClientBlock {
  block: {
    position: { x: number; y: number; z: number };
    metadata?: {
      id?: string;
      groupId?: number;
      [key: string]: any;
    };
    [key: string]: any;
  };
  blockType: any;
  [key: string]: any;
}

/**
 * TargetingService - Centralized target resolution
 */
export class TargetingService {
  constructor(private readonly appContext: AppContext) {
    logger.info('TargetingService initialized');
  }

  /**
   * Resolve target based on targeting mode
   *
   * @param mode Targeting mode
   * @returns Resolved target or none
   */
  resolveTarget(mode: TargetingMode): ResolvedTarget {
    const selectService = this.appContext.services.select;
    if (!selectService) {
      logger.warn('SelectService not available');
      return { type: 'none' };
    }

    // Get current selections from SelectService
    const selectedEntity = selectService.getCurrentSelectedEntity();
    const selectedBlock = selectService.getCurrentSelectedBlock();

    return this.resolveTargetFromSelections(mode, selectedEntity, selectedBlock);
  }

  /**
   * Resolve target from explicit selections
   *
   * @param mode Targeting mode
   * @param entity Selected entity (or null)
   * @param block Selected block (or null)
   * @returns Resolved target
   */
  private resolveTargetFromSelections(
    mode: TargetingMode,
    entity: ClientEntity | null,
    block: ClientBlock | null
  ): ResolvedTarget {
    switch (mode) {
      case 'ENTITY':
        return this.resolveEntity(entity);

      case 'BLOCK':
        return this.resolveBlock(block);

      case 'BOTH':
        return this.resolveEntityOrBlock(entity, block);

      case 'GROUND':
        return this.resolveGround();

      case 'ALL':
        return this.resolveAll(entity, block);

      default:
        logger.warn('Unknown targeting mode, defaulting to ALL', { mode });
        return this.resolveAll(entity, block);
    }
  }

  /**
   * ENTITY strategy: Only resolve entity
   */
  private resolveEntity(entity: ClientEntity | null): ResolvedTarget {
    if (!entity) {
      return { type: 'none' };
    }

    const position: ClientPosition = {
      x: entity.currentPosition.x,
      y: entity.currentPosition.y,
      z: entity.currentPosition.z,
    };

    return {
      type: 'entity',
      entity,
      position,
    };
  }

  /**
   * BLOCK strategy: Only resolve block
   */
  private resolveBlock(block: ClientBlock | null): ResolvedTarget {
    if (!block) {
      return { type: 'none' };
    }

    const pos = block.block.position;
    const position: ClientPosition = {
      x: pos.x + 0.5,
      y: pos.y + 0.5,
      z: pos.z + 0.5,
    };

    return {
      type: 'block',
      block: block as any, // Cast to shared ClientBlock type
      position,
    };
  }

  /**
   * BOTH strategy: Entity priority, fallback to block
   */
  private resolveEntityOrBlock(
    entity: ClientEntity | null,
    block: ClientBlock | null
  ): ResolvedTarget {
    if (entity) {
      return this.resolveEntity(entity);
    }
    if (block) {
      return this.resolveBlock(block);
    }
    return { type: 'none' };
  }

  /**
   * GROUND strategy: Raycast to ground plane
   */
  private resolveGround(): ResolvedTarget {
    const playerService = this.appContext.services.player;
    const cameraService = this.appContext.services.camera;

    if (!playerService || !cameraService) {
      return { type: 'none' };
    }

    const playerPos = playerService.getPosition();
    const rotation = cameraService.getRotation();

    // Calculate ray direction from camera rotation
    const pitch = rotation.x;
    const yaw = rotation.y;

    const dirX = Math.cos(pitch) * Math.sin(yaw);
    const dirY = -Math.sin(pitch);
    const dirZ = Math.cos(pitch) * Math.cos(yaw);

    // Ground plane intersection (y=0)
    if (dirY >= 0) {
      // Looking up, no ground intersection
      return { type: 'none' };
    }

    const t = -playerPos.y / dirY;
    const position: ClientPosition = {
      x: playerPos.x + dirX * t,
      y: 0,
      z: playerPos.z + dirZ * t,
    };

    return {
      type: 'ground',
      position,
    };
  }

  /**
   * ALL strategy: Try entity, then block, then ground
   */
  private resolveAll(
    entity: ClientEntity | null,
    block: ClientBlock | null
  ): ResolvedTarget {
    if (entity) {
      return this.resolveEntity(entity);
    }
    if (block) {
      return this.resolveBlock(block);
    }
    return this.resolveGround();
  }

  /**
   * Check if target should trigger server interaction
   *
   * @param mode Targeting mode
   * @param target Resolved target
   * @returns true if interaction should be sent
   */
  shouldSendInteraction(mode: TargetingMode, target: ResolvedTarget): boolean {
    switch (mode) {
      case 'ENTITY':
        return target.type === 'entity';
      case 'BLOCK':
        return target.type === 'block';
      case 'BOTH':
        return target.type === 'entity' || target.type === 'block';
      case 'GROUND':
      case 'ALL':
        return true; // Always send
      default:
        return true;
    }
  }

  /**
   * Convert ResolvedTarget to legacy format for backward compatibility
   *
   * Used by ShortcutService to emit PlayerService events in the old format.
   */
  toLegacyTarget(target: ResolvedTarget): {
    target?: any;
    targetPosition?: { x: number; y: number; z: number };
  } {
    switch (target.type) {
      case 'entity':
        return {
          target: target.entity,
          targetPosition: {
            x: target.position.x,
            y: target.position.y,
            z: target.position.z,
          },
        };
      case 'block':
        return {
          target: {
            position: {
              x: target.position.x,
              y: target.position.y,
              z: target.position.z,
            },
            block: target.block.block,
            blockType: target.block.blockType,
          },
          targetPosition: {
            x: target.position.x,
            y: target.position.y,
            z: target.position.z,
          },
        };
      case 'ground':
        return {
          targetPosition: {
            x: target.position.x,
            y: target.position.y,
            z: target.position.z,
          },
        };
      case 'none':
        return {};
    }
  }

  /**
   * Convert ResolvedTarget to SerializableTargetingContext for network transmission
   *
   * @param mode Targeting mode used
   * @param target Resolved target
   * @returns Serializable targeting context for ef.p.u messages
   */
  toSerializableContext(
    mode: TargetingMode,
    target: ResolvedTarget
  ): import('@nimbus/shared').SerializableTargetingContext | undefined {
    if (target.type === 'none') {
      return undefined; // No targeting context needed
    }

    const position = {
      x: target.position.x,
      y: target.position.y,
      z: target.position.z,
    };

    switch (target.type) {
      case 'entity':
        return {
          mode,
          targetType: 'entity',
          entityId: target.entity.id,
          position,
        };
      case 'block':
        return {
          mode,
          targetType: 'block',
          blockPosition: {
            x: target.block.block.position.x,
            y: target.block.block.position.y,
            z: target.block.block.position.z,
          },
          position,
        };
      case 'ground':
        return {
          mode,
          targetType: 'ground',
          position,
        };
    }
  }

  /**
   * Dispose service (currently no cleanup needed)
   */
  dispose(): void {
    logger.info('TargetingService disposed');
  }
}
