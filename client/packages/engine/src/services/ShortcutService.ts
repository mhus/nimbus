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
   * Starts tracking a shortcut.
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
