import { getLogger, type TargetingMode } from '@nimbus/shared';
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

  /** Targeting mode for this shortcut (from item's actionTargeting) */
  targetingMode: TargetingMode;

  /** Last known player position (deprecated - kept for compatibility) */
  lastPlayerPos?: Vector3;

  /** Last known target position (deprecated - kept for compatibility) */
  lastTargetPos?: Vector3;
}

/**
 * Service for managing active shortcuts and their lifecycle.
 *
 * Responsibilities:
 * - Track active shortcuts and their state
 * - Implement blocking logic for exclusive shortcuts
 * - Store position/target data for continuous updates
 * - Send position updates to server (throttled to 100ms)
 * - Provide shortcut state queries
 */
export class ShortcutService {
  private activeShortcuts = new Map<number, ActiveShortcut>();
  private lastServerUpdateTime: number = 0;
  private serverUpdateInterval: number = 100; // Send updates every 100ms
  private serverUpdateTimerId: number | null = null;

  constructor(private readonly appContext: AppContext) {
    logger.info('ShortcutService initialized');

    // Start server update loop
    this.startServerUpdateLoop();
  }

  /**
   * Fire a shortcut (main entry point)
   *
   * Dual targeting strategy:
   * 1. Server Interaction: Uses BOTH mode (entity OR block required)
   * 2. Visual Effects: Uses item's actionTargeting mode (ENTITY/BLOCK/BOTH/GROUND/ALL)
   * 3. Sends BlockInteraction to server (if applicable)
   * 4. Emits PlayerService event with visual target data
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
      const networkService = this.appContext.services.network;
      const itemService = this.appContext.services.item;
      const targetingService = this.appContext.services.targeting;

      if (!playerService || !networkService || !targetingService) {
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

      // --- DUAL TARGETING RESOLUTION ---

      // 1. SERVER INTERACTION: Always use BOTH mode (entity OR block required)
      const interactionTarget = targetingService.resolveTarget('BOTH');
      const shouldSendInteraction = targetingService.shouldSendInteraction('BOTH', interactionTarget);

      // 2. VISUAL EFFECTS: Use item's actionTargeting mode
      let visualTargetMode: TargetingMode = 'ALL'; // Default
      if (shortcutDef.itemId && itemService) {
        const item = await itemService.getItem(shortcutDef.itemId);
        if (item) {
          const mergedModifier = await itemService.getMergedModifier(item);
          visualTargetMode = mergedModifier?.actionTargeting ?? 'ALL';
        }
      }
      const visualTarget = targetingService.resolveTarget(visualTargetMode);

      logger.info('Shortcut targeting resolved', {
        shortcutNr,
        interactionMode: 'BOTH',
        interactionTarget: interactionTarget.type,
        willSendInteraction: shouldSendInteraction,
        visualMode: visualTargetMode,
        visualTarget: visualTarget.type,
      });

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

      // Add distance and target position if interaction target exists
      if (interactionTarget.type !== 'none') {
        const distance = Math.sqrt(
          Math.pow(interactionTarget.position.x - playerPosition.x, 2) +
            Math.pow(interactionTarget.position.y - playerPosition.y, 2) +
            Math.pow(interactionTarget.position.z - playerPosition.z, 2)
        );
        params.distance = parseFloat(distance.toFixed(2));
        params.targetPosition = {
          x: interactionTarget.position.x,
          y: interactionTarget.position.y,
          z: interactionTarget.position.z,
        };
      }

      // Send interaction to server (only if target matches BOTH mode)
      if (shouldSendInteraction) {
        if (interactionTarget.type === 'entity') {
          networkService.sendEntityInteraction(
            interactionTarget.entity.id,
            'fireShortcut',
            undefined,
            params
          );
        } else if (interactionTarget.type === 'block') {
          const pos = interactionTarget.block.block.position;
          networkService.sendBlockInteraction(
            pos.x,
            pos.y,
            pos.z,
            'fireShortcut',
            params,
            interactionTarget.block.block.metadata?.id,
            interactionTarget.block.block.metadata?.groupId
          );
        }
      } else {
        logger.debug('Skipping server interaction (no valid target for BOTH mode)');
      }

      // Emit PlayerService event with VISUAL target (always fires)
      const legacyVisualTarget = targetingService.toLegacyTarget(visualTarget);
      playerService.emitShortcutActivated(
        shortcutKey,
        shortcutDef.itemId,
        legacyVisualTarget.target,
        legacyVisualTarget.targetPosition
      );

      logger.debug('Shortcut fired', {
        shortcutNr,
        shortcutKey,
        sentInteraction: shouldSendInteraction,
        hasVisualTarget: visualTarget.type !== 'none',
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
   * @param targetingMode Targeting mode from item's actionTargeting (default: 'ALL')
   */
  startShortcut(
    shortcutNr: number,
    shortcutKey: string,
    executorId: string,
    exclusive: boolean,
    itemId?: string,
    targetingMode: TargetingMode = 'ALL'
  ): void {
    const shortcut: ActiveShortcut = {
      shortcutNr,
      shortcutKey,
      executorId,
      itemId,
      startTime: Date.now(),
      exclusive,
      targetingMode,
    };

    this.activeShortcuts.set(shortcutNr, shortcut);

    logger.debug('Shortcut started', {
      shortcutNr,
      shortcutKey,
      executorId,
      exclusive,
      itemId,
      targetingMode,
    });
  }

  /**
   * Updates shortcut position/target data.
   *
   * @deprecated This method is no longer needed. ShortcutService now uses
   * TargetingService to dynamically resolve targets every 100ms in
   * sendActiveShortcutUpdatesToServer(). Kept for backward compatibility
   * with ClickInputHandler.
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

    logger.info('Shortcut position updated (deprecated - using TargetingService instead)', {
      shortcutNr,
      hasTarget: !!targetPos,
      executorId: shortcut.executorId,
    });
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

    // Send stop event to server before removing
    this.sendShortcutStopToServer(shortcut);

    this.activeShortcuts.delete(shortcutNr);

    const duration = (Date.now() - shortcut.startTime) / 1000;

    logger.info('Shortcut ended', {
      shortcutNr,
      shortcutKey: shortcut.shortcutKey,
      duration,
      executorId: shortcut.executorId,
    });

    return shortcut;
  }

  /**
   * Send shortcut stop event to server
   *
   * Sends a special "stop" parameter update to terminate Until/While loops
   * on remote clients.
   */
  private sendShortcutStopToServer(shortcut: ActiveShortcut): void {
    const networkService = this.appContext.services.network;
    const scrawlService = this.appContext.services.scrawl;

    if (!networkService || !scrawlService) {
      return;
    }

    // Get effectId for this executor
    const effectId = scrawlService.getEffectIdForExecutor(shortcut.executorId);
    if (!effectId) {
      return; // No server sync needed
    }

    try {
      // Send stop event as parameter update
      networkService.sendEffectParameterUpdate(
        effectId,
        '__stop__', // Special parameter to signal stop
        true
      );

      logger.info('Shortcut stop event sent to server', {
        shortcutNr: shortcut.shortcutNr,
        effectId,
      });
    } catch (error) {
      logger.warn('Failed to send shortcut stop to server', {
        error: (error as Error).message,
      });
    }
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
   * Start server update loop
   *
   * Sends position updates for active shortcuts to server every 100ms
   * for multiplayer synchronization.
   */
  private startServerUpdateLoop(): void {
    this.serverUpdateTimerId = window.setInterval(() => {
      this.sendActiveShortcutUpdatesToServer();
    }, this.serverUpdateInterval);

    logger.debug('Server update loop started', { intervalMs: this.serverUpdateInterval });
  }

  /**
   * Send position updates for all active shortcuts to server
   *
   * Uses TargetingService to resolve current targets and send full targeting context
   * to remote clients via ef.p.u messages.
   *
   * Only sends if:
   * - TargetingService can resolve a target
   * - NetworkService is available
   * - ScrawlService has effectId for the executor
   */
  private sendActiveShortcutUpdatesToServer(): void {
    if (this.activeShortcuts.size === 0) {
      return;
    }

    const networkService = this.appContext.services.network;
    const scrawlService = this.appContext.services.scrawl;
    const targetingService = this.appContext.services.targeting;

    if (!networkService || !scrawlService || !targetingService) {
      logger.warn('Required services not available for server updates');
      return;
    }

    let sentCount = 0;
    let skippedCount = 0;

    for (const shortcut of this.activeShortcuts.values()) {
      // Get effectId for this executor
      const effectId = scrawlService.getEffectIdForExecutor(shortcut.executorId);
      if (!effectId) {
        // No effectId means no server synchronization needed
        logger.info('Skipping shortcut update - no effectId', {
          shortcutNr: shortcut.shortcutNr,
          executorId: shortcut.executorId,
        });
        skippedCount++;
        continue;
      }

      // Resolve current target using TargetingService with item's targeting mode
      const currentTarget = targetingService.resolveTarget(shortcut.targetingMode);

      if (currentTarget.type === 'none') {
        logger.info('Skipping shortcut update - no target position', {
          shortcutNr: shortcut.shortcutNr,
          targetingMode: shortcut.targetingMode,
        });
        skippedCount++;
        continue;
      }

      // Send position update to server with targeting context
      try {
        const targetPosition = {
          x: currentTarget.position.x,
          y: currentTarget.position.y,
          z: currentTarget.position.z,
        };

        // Create serializable targeting context
        const targetingContext = targetingService.toSerializableContext(shortcut.targetingMode, currentTarget);

        networkService.sendEffectParameterUpdate(
          effectId,
          'targetPos',
          targetPosition,
          targetingContext
        );

        logger.info('Shortcut position update sent to server', {
          shortcutNr: shortcut.shortcutNr,
          effectId,
          targetType: currentTarget.type,
          hasTargetingContext: !!targetingContext,
        });
        sentCount++;
      } catch (error) {
        logger.warn('Failed to send shortcut update to server', {
          error: (error as Error).message,
          shortcutNr: shortcut.shortcutNr,
        });
      }
    }

    if (sentCount > 0 || skippedCount > 0) {
      logger.info('Server update batch complete', { sentCount, skippedCount });
    }
  }

  /**
   * Cleans up all active shortcuts.
   * Called on service disposal.
   */
  dispose(): void {
    logger.info('Disposing ShortcutService...', {
      activeShortcuts: this.activeShortcuts.size,
    });

    // Stop server update loop
    if (this.serverUpdateTimerId !== null) {
      clearInterval(this.serverUpdateTimerId);
      this.serverUpdateTimerId = null;
    }

    this.activeShortcuts.clear();
  }
}
