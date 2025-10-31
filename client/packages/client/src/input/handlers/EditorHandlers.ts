/**
 * Editor Input Handlers
 *
 * Handles editor-specific actions like selection mode rotation and editor activation.
 * These handlers are only available in Editor build mode.
 */

import { getLogger, ModalFlags, ModalSizePreset } from '@nimbus/shared';
import { InputHandler } from '../InputHandler';
import type { PlayerService } from '../../services/PlayerService';
import type { AppContext } from '../../AppContext';
import { SelectMode } from '../../services/SelectService';

const logger = getLogger('EditorHandlers');

/**
 * Selection Mode Order for rotation
 */
const SELECT_MODE_ORDER: SelectMode[] = [
  SelectMode.NONE,
  SelectMode.INTERACTIVE,
  SelectMode.BLOCK,
  SelectMode.AIR,
  SelectMode.ALL,
];

/**
 * EditSelectionRotator Handler (Key: '.')
 *
 * Rotates through selection modes:
 * NONE → INTERACTIVE → BLOCK → AIR → ALL → (back to NONE)
 */
export class EditSelectionRotatorHandler extends InputHandler {
  constructor(playerService: PlayerService, appContext: AppContext) {
    super(playerService, appContext);
  }

  protected onActivate(value: number): void {
    const selectService = this.appContext?.services.select;
    const notificationService = this.appContext?.services.notification;

    if (!selectService) {
      logger.warn('SelectService not available');
      return;
    }

    // Get current mode
    const currentMode = selectService.autoSelectMode;
    const currentIndex = SELECT_MODE_ORDER.indexOf(currentMode);

    // Calculate next mode (wrap around)
    const nextIndex = (currentIndex + 1) % SELECT_MODE_ORDER.length;
    const nextMode = SELECT_MODE_ORDER[nextIndex];

    // Set new mode
    selectService.autoSelectMode = nextMode;

    logger.info(`Selection mode changed: ${currentMode} → ${nextMode}`);

    // Show notification about mode change
    if (notificationService) {
      notificationService.newNotification(0, null, `Selection Mode: ${nextMode}`);
    }
  }

  protected onDeactivate(): void {
    // No action needed on deactivation
  }

  protected onUpdate(deltaTime: number, value: number): void {
    // Mode rotation doesn't need continuous updates
  }
}

/**
 * EditorActivate Handler (Key: '/')
 *
 * Opens block editor modal for currently selected block
 */
export class EditorActivateHandler extends InputHandler {
  constructor(playerService: PlayerService, appContext: AppContext) {
    super(playerService, appContext);
  }

  protected onActivate(value: number): void {
    const selectService = this.appContext?.services.select;
    const networkService = this.appContext?.services.network;
    const modalService = this.appContext?.services.modal;

    // Check service availability
    if (!selectService) {
      logger.warn('SelectService not available');
      return;
    }

    if (!networkService) {
      logger.warn('NetworkService not available');
      return;
    }

    if (!modalService) {
      logger.warn('ModalService not available');
      return;
    }

    // Get currently selected block
    const selectedBlock = selectService.getCurrentSelectedBlock();

    if (!selectedBlock) {
      logger.warn('No block selected - aim at a block to edit');
      return;
    }

    // Get block position
    const pos = selectedBlock.block.position;

    // Create editor URL with block coordinates
    const editorUrl = networkService.createBlockEditorUrl(pos.x, pos.y, pos.z);

    if (!editorUrl) {
      logger.warn('No editor URL configured for this world');
      return;
    }

    // Open modal with editor
    logger.info('Opening block editor', { position: pos });

    modalService.openModal(
      'block-editor', // referenceKey - reuse same modal for editor
      `Block Editor (${pos.x}, ${pos.y}, ${pos.z})`,
      editorUrl,
      ModalSizePreset.RIGHT,
      ModalFlags.CLOSEABLE
    );
  }

  protected onDeactivate(): void {
    // No action needed on deactivation
  }

  protected onUpdate(deltaTime: number, value: number): void {
    // Editor activation doesn't need continuous updates
  }
}
