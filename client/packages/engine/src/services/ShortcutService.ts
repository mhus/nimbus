import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { Vector3 } from '@babylonjs/core';

const logger = getLogger('ShortcutService');

/**
 * Active shortcut state
 */
export interface ActiveShortcut {
  /** Shortcut number (1-10) */
  shortcutNr: number;

  /** Shortcut key identifier (e.g., 'key1', 'key10') */
  shortcutKey: string;

  /** Running ScrawlExecutor ID */
  executorId: string;

  /** Item ID if shortcut is bound to an item */
  itemId?: string;

  /** When the shortcut was started (milliseconds) */
  startTime: number;

  /** Whether this shortcut blocks all other shortcuts */
  exclusive: boolean;

  /** Last known player position */
  lastPlayerPos?: Vector3;

  /** Last known target position */
  lastTargetPos?: Vector3;
}

/**
 * Service for managing active shortcuts and their lifecycle.
 *
 * Responsibilities:
 * - Track active shortcuts and their state
 * - Implement blocking logic for exclusive shortcuts
 * - Store position/target data for continuous updates
 * - Provide shortcut state queries
 */
export class ShortcutService {
  private activeShortcuts = new Map<number, ActiveShortcut>();

  constructor(private readonly appContext: AppContext) {
    logger.info('ShortcutService initialized');
  }

  /**
   * Fire a shortcut (main entry point)
   *
   * Centralized shortcut handling:
   * 1. Resolves target (Block or Entity) once
   * 2. Sends BlockInteraction to server
   * 3. Emits PlayerService event with target data
   * 4. Executes item script with target data
   *
   * @param shortcutNr Shortcut number
   * @param shortcutKey Shortcut key identifier
   */
  async fireShortcut(shortcutNr: number, shortcutKey: string): Promise<void> {
    try {
      // Check if blocked
      if (this.isShortcutBlocked(shortcutNr)) {
        logger.debug('Shortcut blocked', { shortcutNr });
        return;
      }

      const playerService = this.appContext.services.player;
      const selectService = this.appContext.services.select;
      const networkService = this.appContext.services.network;
      const itemService = this.appContext.services.item;

      if (!playerService || !selectService || !networkService) {
        logger.warn('Required services not available');
        return;
      }

      // Get player info and shortcut definition
      const playerInfo = playerService.getPlayerEntity().playerInfo;
      const shortcutDef = playerInfo.shortcuts?.[shortcutKey];

      if (!shortcutDef) {
        logger.debug('No shortcut definition', { shortcutKey });
        return;
      }

      // Get player state
      const playerPosition = playerService.getPosition();
      const cameraService = this.appContext.services.camera;
      const rotation = cameraService?.getRotation() || { x: 0, y: 0, z: 0 };
      const movementStatus = playerService.getMovementState();

      // Resolve target ONCE (Block or Entity)
      const selectedEntity = selectService.getCurrentSelectedEntity();
      const selectedBlock = selectService.getCurrentSelectedBlock();

      let distance: number | undefined;
      let targetPosition: { x: number; y: number; z: number } | undefined;
      let targetEntity: any = undefined;
      let targetBlock: any = undefined;
      let blockX: number | undefined;
      let blockY: number | undefined;
      let blockZ: number | undefined;
      let blockId: string | undefined;
      let blockGroupId: number | undefined;

      if (selectedEntity) {
        targetEntity = selectedEntity;
        targetPosition = selectedEntity.currentPosition;
        distance = Math.sqrt(
          Math.pow(targetPosition.x - playerPosition.x, 2) +
          Math.pow(targetPosition.y - playerPosition.y, 2) +
          Math.pow(targetPosition.z - playerPosition.z, 2)
        );
      } else if (selectedBlock) {
        const pos = selectedBlock.block.position;
        blockX = pos.x;
        blockY = pos.y;
        blockZ = pos.z;
        targetPosition = { x: pos.x + 0.5, y: pos.y + 0.5, z: pos.z + 0.5 };
        blockId = selectedBlock.block.metadata?.id;
        blockGroupId = selectedBlock.block.metadata?.groupId;
        distance = Math.sqrt(
          Math.pow(targetPosition.x - playerPosition.x, 2) +
          Math.pow(targetPosition.y - playerPosition.y, 2) +
          Math.pow(targetPosition.z - playerPosition.z, 2)
        );

        // Create simplified target object with position for script vars
        // ClientBlock doesn't have .position, so we create a wrapper
        targetBlock = {
          position: targetPosition,
          block: selectedBlock.block,
          blockType: selectedBlock.blockType,
        };
      }

      // Build interaction params
      const params: any = {
        shortcutNr,
        playerPosition: { x: playerPosition.x, y: playerPosition.y, z: playerPosition.z },
        playerRotation: { yaw: rotation.y, pitch: rotation.x },
        selectionRadius: 5,
        movementStatus,
        shortcutType: shortcutDef.type,
        shortcutItemId: shortcutDef.itemId,
      };

      if (distance !== undefined) {
        params.distance = parseFloat(distance.toFixed(2));
      }
      if (targetPosition) {
        params.targetPosition = targetPosition;
      }

      // Send BlockInteraction to server
      if (selectedEntity) {
        networkService.sendEntityInteraction(
          selectedEntity.id,
          'fireShortcut',
          undefined,
          params
        );
      } else if (selectedBlock) {
        networkService.sendBlockInteraction(
          blockX!,
          blockY!,
          blockZ!,
          'fireShortcut',
          params,
          blockId,
          blockGroupId
        );
      } else {
        networkService.sendBlockInteraction(0, 0, 0, 'fireShortcut', params);
      }

      // Emit PlayerService event with target data
      playerService.emitShortcutActivated(shortcutKey, shortcutDef.itemId, targetEntity || targetBlock, targetPosition);

      logger.debug('Shortcut fired', {
        shortcutNr,
        shortcutKey,
        hasTarget: !!(targetEntity || targetBlock),
        targetPosition,
      });
    } catch (error) {
      logger.error('Failed to fire shortcut', { shortcutNr, shortcutKey }, error as Error);
    }
  }

  /**
   * Starts tracking a shortcut.
   *
   * Called by ItemService after script execution starts.
   *
   * @param shortcutNr Shortcut number (1-10)
   * @param shortcutKey Shortcut key identifier (e.g., 'key1')
   * @param executorId ScrawlExecutor ID from ScrawlService
   * @param exclusive Whether this shortcut blocks all others
   * @param itemId Optional item ID
   */
  startShortcut(
    shortcutNr: number,
    shortcutKey: string,
    executorId: string,
    exclusive: boolean,
    itemId?: string
  ): void {
    const shortcut: ActiveShortcut = {
      shortcutNr,
      shortcutKey,
      executorId,
      itemId,
      startTime: Date.now(),
      exclusive,
    };

    this.activeShortcuts.set(shortcutNr, shortcut);

    logger.debug('Shortcut started', {
      shortcutNr,
      shortcutKey,
      executorId,
      exclusive,
      itemId,
    });
  }

  /**
   * Updates shortcut position/target data.
   * Called each frame from ShortcutInputHandler.onUpdate()
   *
   * @param shortcutNr Shortcut number
   * @param playerPos Current player position
   * @param targetPos Current target position (optional)
   */
  updateShortcut(shortcutNr: number, playerPos: Vector3, targetPos?: Vector3): void {
    const shortcut = this.activeShortcuts.get(shortcutNr);
    if (!shortcut) {
      logger.warn(`Cannot update inactive shortcut: ${shortcutNr}`);
      return;
    }

    shortcut.lastPlayerPos = playerPos;
    shortcut.lastTargetPos = targetPos;
  }

  /**
   * Ends shortcut tracking and returns its data.
   * Called when key is released from ShortcutInputHandler.onDeactivate()
   *
   * @param shortcutNr Shortcut number
   * @returns Shortcut data if it was active, undefined otherwise
   */
  endShortcut(shortcutNr: number): ActiveShortcut | undefined {
    const shortcut = this.activeShortcuts.get(shortcutNr);
    if (!shortcut) {
      logger.warn(`Cannot end inactive shortcut: ${shortcutNr}`);
      return undefined;
    }

    this.activeShortcuts.delete(shortcutNr);

    const duration = (Date.now() - shortcut.startTime) / 1000;

    logger.debug('Shortcut ended', {
      shortcutNr,
      shortcutKey: shortcut.shortcutKey,
      duration,
      executorId: shortcut.executorId,
    });

    return shortcut;
  }

  /**
   * Checks if a shortcut is currently blocked.
   *
   * Blocking logic:
   * - The same shortcut can always re-trigger (not blocked by itself)
   * - If any OTHER exclusive shortcut is active, this shortcut is blocked
   *
   * @param shortcutNr Shortcut number to check
   * @returns true if blocked, false if can activate
   */
  isShortcutBlocked(shortcutNr: number): boolean {
    // Check if THIS shortcut is already active (allow re-trigger)
    if (this.activeShortcuts.has(shortcutNr)) {
      return false; // Same shortcut can re-trigger
    }

    // Check if ANY other exclusive shortcut is active
    for (const [activeNr, shortcut] of this.activeShortcuts) {
      if (activeNr !== shortcutNr && shortcut.exclusive) {
        logger.debug(`Shortcut ${shortcutNr} blocked by exclusive shortcut ${activeNr}`);
        return true; // Blocked by exclusive shortcut
      }
    }

    return false; // Not blocked
  }

  /**
   * Gets the active shortcut for a number.
   *
   * @param shortcutNr Shortcut number
   * @returns Active shortcut data or undefined
   */
  getActiveShortcut(shortcutNr: number): ActiveShortcut | undefined {
    return this.activeShortcuts.get(shortcutNr);
  }

  /**
   * Gets all currently active shortcuts.
   *
   * @returns Array of all active shortcuts
   */
  getActiveShortcuts(): ActiveShortcut[] {
    return Array.from(this.activeShortcuts.values());
  }

  /**
   * Checks if any shortcut is currently active.
   *
   * @returns true if at least one shortcut is active
   */
  hasActiveShortcuts(): boolean {
    return this.activeShortcuts.size > 0;
  }

  /**
   * Cleans up all active shortcuts.
   * Called on service disposal.
   */
  dispose(): void {
    logger.info('Disposing ShortcutService...', {
      activeShortcuts: this.activeShortcuts.size,
    });
    this.activeShortcuts.clear();
  }
}
