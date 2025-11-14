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
      const playerInfo = playerEntity.playerInfo;
      const shortcutKey = `click${buttonNumber}`;
      const shortcut = playerInfo.shortcuts?.[shortcutKey];
      const shortcutType = shortcut?.type;
      const shortcutItemId = shortcut?.itemId;

      // Priority 1: Check if entity is selected
      const selectedEntity = selectService.getCurrentSelectedEntity();
      if (selectedEntity) {
        // Calculate distance to entity
        const entityPos = selectedEntity.currentPosition;
        const distance = Math.sqrt(
          Math.pow(entityPos.x - playerPosition.x, 2) +
          Math.pow(entityPos.y - playerPosition.y, 2) +
          Math.pow(entityPos.z - playerPosition.z, 2)
        );

        // Send entity interaction to server with full context
        networkService.sendEntityInteraction(
          selectedEntity.id,
          'click',
          buttonNumber,
          {
            entityId: selectedEntity.id,
            distance: parseFloat(distance.toFixed(2)),
            targetPosition: entityPos,
            playerPosition: { x: playerPosition.x, y: playerPosition.y, z: playerPosition.z },
            playerRotation: { yaw: rotation.y, pitch: rotation.x },
            selectionRadius,
            movementStatus,
            shortcutType,
            shortcutItemId,
          }
        );

        logger.debug('Entity interaction sent', {
          entityId: selectedEntity.id,
          clickType: buttonNumber,
          distance: distance.toFixed(2),
          movementStatus,
          shortcutType,
        });

        // Trigger highlight animation in UI (only for clicks 0-9)
        if (buttonNumber >= 0 && buttonNumber <= 9) {
          this.playerService.highlightShortcut(shortcutKey);
        }

        return;
      }

      // Priority 2: Check if block is selected
      const selectedBlock = selectService.getCurrentSelectedBlock();
      if (selectedBlock) {
        // Calculate distance to block center
        const blockPos = selectedBlock.block.position;
        const blockCenter = { x: blockPos.x + 0.5, y: blockPos.y + 0.5, z: blockPos.z + 0.5 };
        const distance = Math.sqrt(
          Math.pow(blockCenter.x - playerPosition.x, 2) +
          Math.pow(blockCenter.y - playerPosition.y, 2) +
          Math.pow(blockCenter.z - playerPosition.z, 2)
        );

        // Send block interaction to server with full context
        networkService.sendBlockInteraction(
          blockPos.x,
          blockPos.y,
          blockPos.z,
          'click',
          {
            clickType: buttonNumber,
            distance: parseFloat(distance.toFixed(2)),
            targetPosition: blockCenter,
            playerPosition: { x: playerPosition.x, y: playerPosition.y, z: playerPosition.z },
            playerRotation: { yaw: rotation.y, pitch: rotation.x },
            selectionRadius,
            movementStatus,
            shortcutType,
            shortcutItemId,
          },
          selectedBlock.block.metadata?.id,
          selectedBlock.block.metadata?.groupId
        );

        logger.debug('Block interaction sent', {
          position: blockPos,
          clickType: buttonNumber,
          distance: distance.toFixed(2),
          movementStatus,
          shortcutType,
          id: selectedBlock.block.metadata?.id,
        });

        // Trigger highlight animation in UI (only for clicks 0-9)
        if (buttonNumber >= 0 && buttonNumber <= 9) {
          this.playerService.highlightShortcut(shortcutKey);
        }
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'ClickInputHandler.onActivate', { buttonNumber });
    }
  }

  /**
   * Handle click deactivation (no-op for clicks)
   */
  protected onDeactivate(): void {
    // Clicks are instantaneous, no deactivation needed
  }

  /**
   * Update handler state (no-op for clicks)
   */
  protected onUpdate(deltaTime: number, value: number): void {
    // Clicks are instantaneous, no continuous update needed
  }
}
