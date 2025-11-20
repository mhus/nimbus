/**
 * ClickInputHandler - Handles click input from mouse or gamepad triggers
 *
 * Processes click events on interactive entities and blocks.
 * Supports multiple mouse buttons (0=left, 1=middle, 2=right, etc.)
 * or gamepad triggers mapped to button numbers.
 *
 * This handler:
 * - Checks if SelectService is in INTERACTIVE mode
 * - Gets current movement status from PlayerService
 * - Reads shortcut configuration from PlayerInfo.shortcuts['click0', 'click1', etc.]
 * - Determines selected entity or block via SelectService
 * - Sends interaction to server with full context (position, rotation, distance, etc.)
 *
 * Usage in InputController:
 * ```typescript
 * // Mouse click
 * onMouseDown = (event: MouseEvent) => {
 *   this.clickHandler.activate(event.button); // 0, 1, 2, ...
 * }
 *
 * // GamePad trigger
 * onTriggerPress = (triggerIndex: number) => {
 *   this.clickHandler.activate(triggerIndex);
 * }
 * ```
 */

import { InputHandler } from '../InputHandler';
import type { PlayerService } from '../../services/PlayerService';
import type { AppContext } from '../../AppContext';
import { getLogger, ExceptionHandler } from '@nimbus/shared';

const logger = getLogger('ClickInputHandler');

export class ClickInputHandler extends InputHandler {
  private activeButtonNumber: number | null = null;
  private activeShortcutKey: string | null = null;

  constructor(playerService: PlayerService, appContext: AppContext) {
    super(playerService, appContext);
  }

  /**
   * Handle click activation
   *
   * @param buttonNumber Mouse button number (0=left, 1=middle, 2=right) or gamepad trigger index
   */
  protected onActivate(buttonNumber: number): void {
    try {
      // Ensure AppContext is available
      if (!this.appContext) {
        logger.warn('AppContext not available for click event');
        return;
      }

      const selectService = this.appContext.services.select;
      const networkService = this.appContext.services.network;

      // Check if SelectService is in INTERACTIVE mode
      if (!selectService || selectService.getAutoSelectMode() !== 'INTERACTIVE') {
        return;
      }

      // Check if network service is available
      if (!networkService) {
        logger.warn('NetworkService not available for click event');
        return;
      }

      // Get player position
      const playerPosition = this.playerService.getPosition();

      // Get camera rotation
      const cameraService = (this.playerService as any).cameraService;
      if (!cameraService) {
        logger.warn('CameraService not available for click event');
        return;
      }
      const rotation = cameraService.getRotation();

      // Get selection radius from PlayerInfo (state-dependent)
      const playerEntity = this.playerService.getPlayerEntity();
      const selectionRadius = playerEntity.cachedSelectionRadius;

      // Get current movement status
      const movementStatus = this.playerService.getMovementState();

      // Get shortcut configuration for this button
      const shortcutKey = `click${buttonNumber}`;
      const shortcutService = this.appContext.services.shortcut;

      // Fire shortcut through ShortcutService (centralized)
      if (shortcutService) {
        shortcutService.fireShortcut(buttonNumber, shortcutKey);

        // Track active shortcut for deactivation
        this.activeButtonNumber = buttonNumber;
        this.activeShortcutKey = shortcutKey;
      } else {
        logger.warn('ShortcutService not available for click shortcut');
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'ClickInputHandler.onActivate', { buttonNumber });
    }
  }

  /**
   * Handle click deactivation
   * Called when mouse button is released
   */
  protected onDeactivate(): void {
    logger.info('ClickInputHandler.onDeactivate() called', {
      activeButtonNumber: this.activeButtonNumber,
      activeShortcutKey: this.activeShortcutKey
    });

    if (!this.activeButtonNumber && this.activeButtonNumber !== 0) {
      logger.debug('No active button to deactivate');
      return;
    }

    try {
      const shortcutService = this.appContext?.services.shortcut;
      if (!shortcutService) {
        logger.warn('ShortcutService not available for deactivation');
        return;
      }

      // Get shortcut data before ending
      const shortcut = shortcutService.endShortcut(this.activeButtonNumber);

      logger.info('Shortcut ended', {
        buttonNumber: this.activeButtonNumber,
        shortcutKey: this.activeShortcutKey,
        found: !!shortcut
      });

      // Emit ended event
      if (shortcut) {
        const duration = (Date.now() - shortcut.startTime) / 1000;

        this.playerService.emitShortcutEnded({
          shortcutNr: this.activeButtonNumber,
          shortcutKey: this.activeShortcutKey!,
          executorId: shortcut.executorId,
          duration,
        });

        logger.info('Click shortcut ended event emitted', {
          buttonNumber: this.activeButtonNumber,
          shortcutKey: this.activeShortcutKey,
          executorId: shortcut.executorId,
          duration,
        });
      } else {
        logger.warn('No active shortcut found to end', {
          buttonNumber: this.activeButtonNumber
        });
      }

      // Reset tracking
      this.activeButtonNumber = null;
      this.activeShortcutKey = null;
    } catch (error) {
      ExceptionHandler.handle(error, 'ClickInputHandler.onDeactivate', {
        buttonNumber: this.activeButtonNumber,
      });
    }
  }

  /**
   * Update handler state (no-op for clicks)
   */
  protected onUpdate(deltaTime: number, value: number): void {
    // Clicks are instantaneous, no continuous update needed
  }
}
