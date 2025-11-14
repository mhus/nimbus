/**
 * ShortcutInputHandler - Handles shortcut key input from keyboard or gamepad
 *
 * Processes shortcut actions (number keys 1-9, 0 or gamepad buttons).
 * Shortcuts can trigger actions on selected entities/blocks or globally.
 *
 * This handler:
 * - Gets current movement status from PlayerService
 * - Reads shortcut configuration from PlayerInfo.shortcuts['key0'-'key9']
 * - Determines selected entity or block via SelectService
 * - Sends interaction to server with full context including shortcut data
 *
 * Shortcut mapping:
 * - shortcutNr 1-9 → PlayerInfo.shortcuts['key1'-'key9']
 * - shortcutNr 10 (key '0') → PlayerInfo.shortcuts['key0']
 *
 * Usage in InputController:
 * ```typescript
 * // Keyboard number key
 * onKeyDown = (event: KeyboardEvent) => {
 *   if (event.key >= '0' && event.key <= '9') {
 *     const shortcutNr = event.key === '0' ? 10 : parseInt(event.key, 10);
 *     this.shortcutHandler.activate(shortcutNr);
 *   }
 * }
 *
 * // GamePad button
 * onButtonPress = (buttonIndex: number) => {
 *   this.shortcutHandler.activate(buttonIndex + 1); // Map to 1-10
 * }
 * ```
 */

import { InputHandler } from '../InputHandler';
import type { PlayerService } from '../../services/PlayerService';
import type { AppContext } from '../../AppContext';
import { getLogger, ExceptionHandler } from '@nimbus/shared';

const logger = getLogger('ShortcutInputHandler');

export class ShortcutInputHandler extends InputHandler {
  constructor(playerService: PlayerService, appContext: AppContext) {
    super(playerService, appContext);
  }

  /**
   * Handle shortcut activation
   *
   * @param shortcutNr Shortcut number (1-10, where 10 = key '0')
   */
  protected onActivate(shortcutNr: number): void {
    try {
      // Ensure AppContext is available
      if (!this.appContext) {
        logger.warn('AppContext not available for shortcut');
        return;
      }

      const selectService = this.appContext.services.select;
      const networkService = this.appContext.services.network;

      if (!selectService) {
        logger.warn('SelectService not available for shortcut');
        return;
      }

      if (!networkService) {
        logger.warn('NetworkService not available for shortcut');
        return;
      }

      // Get player position
      const playerPosition = this.playerService.getPosition();

      // Get camera rotation
      const cameraService = (this.playerService as any).cameraService;
      if (!cameraService) {
        logger.warn('CameraService not available for shortcut');
        return;
      }
      const rotation = cameraService.getRotation();

      // Get current movement status
      const movementStatus = this.playerService.getMovementState();

      // Get selected entity (priority) or block
      const selectedEntity = selectService.getCurrentSelectedEntity();
      const selectedBlock = selectService.getCurrentSelectedBlock();

      // Map shortcut number to key name
      // shortcutNr 1-9 → key1-key9, shortcutNr 10 → key0
      const shortcutKey = shortcutNr === 10 ? 'key0' : `key${shortcutNr}`;

      // Get shortcut configuration
      const playerEntity = this.playerService.getPlayerEntity();
      const playerInfo = playerEntity.playerInfo;
      const shortcut = playerInfo.shortcuts?.[shortcutKey];
      const shortcutType = shortcut?.type;
      const shortcutItemId = shortcut?.itemId;

      // Calculate distance to selected target
      let distance: number | undefined;
      let targetPosition: { x: number; y: number; z: number } | undefined;
      let entityId: string | undefined;
      let blockId: string | undefined;
      let blockGroupId: number | undefined;
      let blockX: number | undefined;
      let blockY: number | undefined;
      let blockZ: number | undefined;

      if (selectedEntity) {
        // Entity selected
        entityId = selectedEntity.id;
        targetPosition = selectedEntity.currentPosition;
        distance = Math.sqrt(
          Math.pow(targetPosition.x - playerPosition.x, 2) +
          Math.pow(targetPosition.y - playerPosition.y, 2) +
          Math.pow(targetPosition.z - playerPosition.z, 2)
        );
      } else if (selectedBlock) {
        // Block selected
        const pos = selectedBlock.block.position;
        blockX = pos.x;
        blockY = pos.y;
        blockZ = pos.z;
        targetPosition = { x: pos.x + 0.5, y: pos.y + 0.5, z: pos.z + 0.5 }; // Block center
        blockId = selectedBlock.block.metadata?.id;
        blockGroupId = selectedBlock.block.metadata?.groupId;
        distance = Math.sqrt(
          Math.pow(targetPosition.x - playerPosition.x, 2) +
          Math.pow(targetPosition.y - playerPosition.y, 2) +
          Math.pow(targetPosition.z - playerPosition.z, 2)
        );
      }

      // Prepare params with all context
      const params: any = {
        shortcutNr,
        playerPosition: { x: playerPosition.x, y: playerPosition.y, z: playerPosition.z },
        playerRotation: { yaw: rotation.y, pitch: rotation.x },
        movementStatus,
        shortcutType,
        shortcutItemId,
      };

      if (distance !== undefined) {
        params.distance = parseFloat(distance.toFixed(2));
      }

      if (targetPosition) {
        params.targetPosition = targetPosition;
      }

      if (entityId) {
        params.entityId = entityId;
      }

      // Send to server
      if (selectedEntity) {
        // Send as entity interaction with full context
        networkService.sendEntityInteraction(
          selectedEntity.id,
          'fireShortcut',
          undefined, // clickType not applicable
          params
        );
      } else if (selectedBlock) {
        // Send as block interaction
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
        // No selection - send shortcut without target
        // Use block interaction with position (0,0,0) as placeholder
        networkService.sendBlockInteraction(
          0, 0, 0,
          'fireShortcut',
          params
        );
      }

      logger.debug('Shortcut fired', {
        shortcutNr,
        shortcutKey,
        hasEntity: !!selectedEntity,
        hasBlock: !!selectedBlock,
        distance,
        movementStatus,
        shortcutType,
      });
    } catch (error) {
      ExceptionHandler.handle(error, 'ShortcutInputHandler.onActivate', { shortcutNr });
    }
  }

  /**
   * Handle shortcut deactivation (no-op for shortcuts)
   */
  protected onDeactivate(): void {
    // Shortcuts are instantaneous, no deactivation needed
  }

  /**
   * Update handler state (no-op for shortcuts)
   */
  protected onUpdate(deltaTime: number, value: number): void {
    // Shortcuts are instantaneous, no continuous update needed
  }
}
