/**
 * WebInputController - Browser input controller
 *
 * Handles keyboard and mouse input for web browsers.
 * Binds WASD + Space + Mouse for player control.
 */

import { getLogger } from '@nimbus/shared';
import type { InputController } from '../services/InputService';
import type { PlayerService } from '../services/PlayerService';
import type { AppContext } from '../AppContext';
import type { InputHandler } from './InputHandler';
import {
  MoveForwardHandler,
  MoveBackwardHandler,
  MoveLeftHandler,
  MoveRightHandler,
  MoveUpHandler,
  MoveDownHandler,
} from './handlers/MovementHandlers';
import { JumpHandler, CycleMovementStateHandler, ToggleViewModeHandler } from './handlers/ActionHandlers';
import { RotateHandler } from './handlers/RotationHandlers';
import {
  EditSelectionRotatorHandler,
  EditorActivateHandler,
  BlockEditorActivateHandler,
  EditConfigActivateHandler,
} from './handlers/EditorHandlers';

const logger = getLogger('WebInputController');

/**
 * Key binding for an action
 */
interface KeyBinding {
  key: string;
  handler: InputHandler;
}

/**
 * WebInputController - Browser input
 *
 * Key bindings:
 * - W: Move forward
 * - S: Move backward
 * - A: Move left
 * - D: Move right
 * - Space: Jump (Walk mode) / Move up (Fly mode)
 * - Shift: Move down (Fly mode only)
 * - F: Cycle movement state (WALK → SPRINT → CROUCH → WALK, includes FLY in Editor)
 * - F5: Toggle Ego/Third-Person view
 * - . (Period): Rotate selection mode (Editor only)
 * - / (Slash): Activate selected block editor (Editor only)
 * - F9: Open edit configuration (Editor only)
 * - F10: Open block editor for selected block (Editor only)
 * - Mouse: Look around (when pointer locked)
 */
export class WebInputController implements InputController {
  private canvas: HTMLCanvasElement;
  private playerService: PlayerService;
  private appContext: AppContext;

  private handlers: InputHandler[] = [];
  private keyBindings: Map<string, InputHandler> = new Map();

  // Handlers
  private moveForwardHandler: MoveForwardHandler;
  private moveBackwardHandler: MoveBackwardHandler;
  private moveLeftHandler: MoveLeftHandler;
  private moveRightHandler: MoveRightHandler;
  private moveUpHandler: MoveUpHandler;
  private moveDownHandler: MoveDownHandler;
  private jumpHandler: JumpHandler;
  private cycleMovementStateHandler: CycleMovementStateHandler;
  private toggleViewModeHandler: ToggleViewModeHandler;
  private rotateHandler: RotateHandler;

  // Editor handlers (Editor only)
  private editSelectionRotatorHandler?: EditSelectionRotatorHandler;
  private editorActivateHandler?: EditorActivateHandler;
  private blockEditorActivateHandler?: BlockEditorActivateHandler;
  private editConfigActivateHandler?: EditConfigActivateHandler;

  // Pointer lock state
  private pointerLocked: boolean = false;

  constructor(canvas: HTMLCanvasElement, playerService: PlayerService, appContext: AppContext) {
    this.canvas = canvas;
    this.playerService = playerService;
    this.appContext = appContext;

    // Create handlers
    this.moveForwardHandler = new MoveForwardHandler(playerService);
    this.moveBackwardHandler = new MoveBackwardHandler(playerService);
    this.moveLeftHandler = new MoveLeftHandler(playerService);
    this.moveRightHandler = new MoveRightHandler(playerService);
    this.moveUpHandler = new MoveUpHandler(playerService);
    this.moveDownHandler = new MoveDownHandler(playerService);
    this.jumpHandler = new JumpHandler(playerService);
    this.toggleViewModeHandler = new ToggleViewModeHandler(playerService);
    this.cycleMovementStateHandler = new CycleMovementStateHandler(playerService);
    this.rotateHandler = new RotateHandler(playerService);

    // Editor-only handlers
    if (__EDITOR__) {
      this.editSelectionRotatorHandler = new EditSelectionRotatorHandler(playerService, appContext);
      this.editorActivateHandler = new EditorActivateHandler(playerService, appContext);
      this.blockEditorActivateHandler = new BlockEditorActivateHandler(playerService, appContext);
      this.editConfigActivateHandler = new EditConfigActivateHandler(playerService, appContext);
    }

    this.handlers = [
      this.moveForwardHandler,
      this.moveBackwardHandler,
      this.moveLeftHandler,
      this.moveRightHandler,
      this.moveUpHandler,
      this.moveDownHandler,
      this.jumpHandler,
      this.toggleViewModeHandler,
      this.cycleMovementStateHandler,
      this.rotateHandler,
    ];

    // Add editor handlers to handlers list if available
    if (this.editSelectionRotatorHandler) {
      this.handlers.push(this.editSelectionRotatorHandler);
    }
    if (this.editorActivateHandler) {
      this.handlers.push(this.editorActivateHandler);
    }
    if (this.blockEditorActivateHandler) {
      this.handlers.push(this.blockEditorActivateHandler);
    }
    if (this.editConfigActivateHandler) {
      this.handlers.push(this.editConfigActivateHandler);
    }

    // Setup key bindings
    this.setupKeyBindings();

    logger.info('WebInputController created');
  }

  /**
   * Setup key bindings
   */
  private setupKeyBindings(): void {
    this.keyBindings.set('w', this.moveForwardHandler);
    this.keyBindings.set('W', this.moveForwardHandler);
    this.keyBindings.set('s', this.moveBackwardHandler);
    this.keyBindings.set('S', this.moveBackwardHandler);
    this.keyBindings.set('a', this.moveLeftHandler);
    this.keyBindings.set('A', this.moveLeftHandler);
    this.keyBindings.set('d', this.moveRightHandler);
    this.keyBindings.set('D', this.moveRightHandler);

    // Editor-only key bindings
    if (this.editSelectionRotatorHandler) {
      this.keyBindings.set('.', this.editSelectionRotatorHandler);
    }
    if (this.editorActivateHandler) {
      this.keyBindings.set('/', this.editorActivateHandler);
    }
    if (this.editConfigActivateHandler) {
      this.keyBindings.set('F9', this.editConfigActivateHandler);
    }
    if (this.blockEditorActivateHandler) {
      this.keyBindings.set('F10', this.blockEditorActivateHandler);
    }

    // F: Cycle movement state (SPRINT → CROUCH → WALK, FLY in Editor)
    this.keyBindings.set('f', this.cycleMovementStateHandler);
    this.keyBindings.set('F', this.cycleMovementStateHandler);

    // F5: Toggle view mode (ego/third-person)
    this.keyBindings.set('F5', this.toggleViewModeHandler);

    // Space: Jump in Walk mode, Move up in Fly mode (handled dynamically)
    // Shift: Move down in Fly mode (handled dynamically)
    // . : Rotate selection mode (Editor only)
    // / : Activate selected block editor (Editor only)
    // F9: Open edit configuration (Editor only)
    // F10: Open block editor for selected block (Editor only)
  }

  /**
   * Initialize controller
   */
  initialize(): void {
    // Add event listeners
    window.addEventListener('keydown', this.onKeyDown);
    window.addEventListener('keyup', this.onKeyUp);
    this.canvas.addEventListener('click', this.onCanvasClick);
    this.canvas.addEventListener('mousedown', this.onMouseDown);
    document.addEventListener('pointerlockchange', this.onPointerLockChange);
    document.addEventListener('mousemove', this.onMouseMove);

    // Activate rotation handler (always active for mouse look)
    this.rotateHandler.activate();

    logger.info('WebInputController initialized');
  }

  /**
   * Handle keydown event
   */
  private onKeyDown = (event: KeyboardEvent): void => {
    // Handle Space key dynamically based on movement mode
    if (event.key === ' ') {
      const mode = this.playerService.getMovementMode();
      if (mode === 'walk') {
        // Walk mode: Jump
        if (!this.jumpHandler.isActive()) {
          this.jumpHandler.activate();
          event.preventDefault();
        }
      } else if (mode === 'fly') {
        // Fly mode: Move up
        if (!this.moveUpHandler.isActive()) {
          this.moveUpHandler.activate();
          event.preventDefault();
        }
      }
      return;
    }

    // Handle Shift key for Fly mode down movement
    if (event.key === 'Shift') {
      const mode = this.playerService.getMovementMode();
      if (mode === 'fly') {
        if (!this.moveDownHandler.isActive()) {
          this.moveDownHandler.activate();
          event.preventDefault();
        }
      }
      return;
    }

    // Handle F key for toggling movement mode (Editor only)
    if ((event.key === 'f' || event.key === 'F') && this.toggleMovementModeHandler) {
      if (!this.toggleMovementModeHandler.isActive()) {
        this.toggleMovementModeHandler.activate();
        event.preventDefault();
      }
      return;
    }

    // Handle number keys (1-9, 0) for shortcuts
    if (event.key >= '0' && event.key <= '9') {
      const shortcutNr = event.key === '0' ? 10 : parseInt(event.key, 10);
      this.handleShortcut(shortcutNr);
      event.preventDefault();
      return;
    }

    // Handle other keys via bindings
    const handler = this.keyBindings.get(event.key);
    if (handler && !handler.isActive()) {
      handler.activate();
      event.preventDefault();
    }
  };

  /**
   * Handle shortcut key press (1-9, 0)
   */
  private handleShortcut(shortcutNr: number): void {
    const selectService = this.appContext.services.select;
    if (!selectService) {
      return;
    }

    selectService.fireShortcut(shortcutNr);
  };

  /**
   * Handle keyup event
   */
  private onKeyUp = (event: KeyboardEvent): void => {
    // Handle Space key dynamically based on movement mode
    if (event.key === ' ') {
      const mode = this.playerService.getMovementMode();
      if (mode === 'walk') {
        // Walk mode: Jump
        if (this.jumpHandler.isActive()) {
          this.jumpHandler.deactivate();
          event.preventDefault();
        }
      } else if (mode === 'fly') {
        // Fly mode: Move up
        if (this.moveUpHandler.isActive()) {
          this.moveUpHandler.deactivate();
          event.preventDefault();
        }
      }
      return;
    }

    // Handle Shift key for Fly mode down movement
    if (event.key === 'Shift') {
      const mode = this.playerService.getMovementMode();
      if (mode === 'fly') {
        if (this.moveDownHandler.isActive()) {
          this.moveDownHandler.deactivate();
          event.preventDefault();
        }
      }
      return;
    }

    // Handle F key for toggling movement mode (Editor only)
    if ((event.key === 'f' || event.key === 'F') && this.toggleMovementModeHandler) {
      if (this.toggleMovementModeHandler.isActive()) {
        this.toggleMovementModeHandler.deactivate();
        event.preventDefault();
      }
      return;
    }

    // Handle other keys via bindings
    const handler = this.keyBindings.get(event.key);
    if (handler && handler.isActive()) {
      handler.deactivate();
      event.preventDefault();
    }
  };

  /**
   * Handle canvas click (request pointer lock)
   */
  private onCanvasClick = (): void => {
    if (!this.pointerLocked) {
      this.canvas.requestPointerLock();
    }
  };

  /**
   * Handle mouse button down (for block and entity interactions)
   */
  private onMouseDown = (event: MouseEvent): void => {
    if (!this.pointerLocked) {
      return;
    }

    // Check if SelectService is in INTERACTIVE mode
    const selectService = this.appContext.services.select;
    if (!selectService || selectService.getAutoSelectMode() !== 'INTERACTIVE') {
      return;
    }

    // Use mouse button number directly (0 = left, 1 = middle, 2 = right, etc.)
    const clickType = event.button;

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

    // Check if network service is available
    if (!this.appContext.services.network) {
      logger.warn('NetworkService not available for click event');
      return;
    }

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
      this.appContext.services.network.sendEntityInteraction(
        selectedEntity.id,
        'click',
        clickType,
        {
          entityId: selectedEntity.id,
          distance: parseFloat(distance.toFixed(2)),
          targetPosition: entityPos,
          playerPosition: { x: playerPosition.x, y: playerPosition.y, z: playerPosition.z },
          playerRotation: { yaw: rotation.y, pitch: rotation.x },
          selectionRadius,
        }
      );

      logger.debug('Entity interaction sent', {
        entityId: selectedEntity.id,
        clickType,
        distance: distance.toFixed(2),
      });

      event.preventDefault();
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
      this.appContext.services.network.sendBlockInteraction(
        blockPos.x,
        blockPos.y,
        blockPos.z,
        'click',
        {
          clickType,
          distance: parseFloat(distance.toFixed(2)),
          targetPosition: blockCenter,
          playerPosition: { x: playerPosition.x, y: playerPosition.y, z: playerPosition.z },
          playerRotation: { yaw: rotation.y, pitch: rotation.x },
          selectionRadius,
        },
        selectedBlock.block.metadata?.id,
        selectedBlock.block.metadata?.groupId
      );

      logger.debug('Block interaction sent', {
        position: blockPos,
        clickType,
        distance: distance.toFixed(2),
        id: selectedBlock.block.metadata?.id,
      });

      event.preventDefault();
    }
  };

  /**
   * Handle pointer lock change
   */
  private onPointerLockChange = (): void => {
    this.pointerLocked = document.pointerLockElement === this.canvas;

    if (this.pointerLocked) {
      logger.debug('Pointer locked');
    } else {
      logger.debug('Pointer unlocked');
    }
  };

  /**
   * Handle mouse move (rotation)
   */
  private onMouseMove = (event: MouseEvent): void => {
    if (!this.pointerLocked) {
      return;
    }

    // Get mouse movement
    const deltaX = event.movementX || 0;
    const deltaY = event.movementY || 0;

    // Update rotation handler
    this.rotateHandler.setDelta(deltaX, deltaY);
  };

  /**
   * Get all handlers
   */
  getHandlers(): InputHandler[] {
    return this.handlers;
  }

  /**
   * Dispose controller
   */
  dispose(): void {
    // Remove event listeners
    window.removeEventListener('keydown', this.onKeyDown);
    window.removeEventListener('keyup', this.onKeyUp);
    this.canvas.removeEventListener('click', this.onCanvasClick);
    document.removeEventListener('pointerlockchange', this.onPointerLockChange);
    document.removeEventListener('mousemove', this.onMouseMove);

    // Exit pointer lock
    if (this.pointerLocked) {
      document.exitPointerLock();
    }

    // Deactivate all handlers
    for (const handler of this.handlers) {
      if (handler.isActive()) {
        handler.deactivate();
      }
    }

    logger.info('WebInputController disposed');
  }
}
