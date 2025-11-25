/**
 * InputService - Manages input handling
 *
 * Coordinates input controllers and handlers.
 * Updates handlers each frame.
 */

import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { PlayerService } from './PlayerService';
import type { InputHandler } from '../input/InputHandler';
import { ClickInputHandler } from '../input/handlers/ClickInputHandler';
import { ShortcutInputHandler } from '../input/handlers/ShortcutInputHandler';
import {
  MoveForwardHandler,
  MoveBackwardHandler,
  MoveLeftHandler,
  MoveRightHandler,
  MoveUpHandler,
  MoveDownHandler,
} from '../input/handlers/MovementHandlers';
import {
  JumpHandler,
  CycleMovementStateHandler,
  ToggleViewModeHandler,
  ToggleShortcutsHandler,
} from '../input/handlers/ActionHandlers';
import { RotateHandler } from '../input/handlers/RotationHandlers';
import {
  EditSelectionRotatorHandler,
  EditorActivateHandler,
  BlockEditorActivateHandler,
  EditConfigActivateHandler,
} from '../input/handlers/EditorHandlers';

const logger = getLogger('InputService');

/**
 * Input controller interface
 */
export interface InputController {
  /**
   * Initialize the controller
   */
  initialize(): void;

  /**
   * Dispose the controller
   */
  dispose(): void;

  /**
   * Get all handlers
   */
  getHandlers(): InputHandler[];
}

/**
 * InputService - Manages input
 *
 * Features:
 * - Controller management
 * - Handler registration
 * - Update loop integration
 */
export class InputService {
  private appContext: AppContext;
  private playerService: PlayerService;

  private controller?: InputController;
  private handlers: InputHandler[] = [];
  private handlerRegistry: Map<string, InputHandler> = new Map();
  private inputEnabled: boolean = true; // Input enabled by default

  constructor(appContext: AppContext, playerService: PlayerService) {
    this.appContext = appContext;
    this.playerService = playerService;

    // Register central handlers that can be used by any controller
    this.registerCentralHandlers();

    // Listen for DEAD mode changes
    this.playerService.on('player:deadStateChanged', (isDead: boolean) => {
      this.inputEnabled = !isDead;
      logger.info('Input enabled state changed', { enabled: this.inputEnabled });
    });

    logger.info('InputService initialized');
  }

  /**
   * Register central input handlers
   * These handlers are available to all input controllers via getHandler()
   */
  private registerCentralHandlers(): void {
    // Click handler (for mouse clicks, gamepad triggers)
    this.handlerRegistry.set('click', new ClickInputHandler(this.playerService, this.appContext));

    // Shortcut handler (for keyboard shortcuts, gamepad buttons)
    this.handlerRegistry.set('shortcut', new ShortcutInputHandler(this.playerService, this.appContext));

    // Movement handlers
    this.handlerRegistry.set('moveForward', new MoveForwardHandler(this.playerService));
    this.handlerRegistry.set('moveBackward', new MoveBackwardHandler(this.playerService));
    this.handlerRegistry.set('moveLeft', new MoveLeftHandler(this.playerService));
    this.handlerRegistry.set('moveRight', new MoveRightHandler(this.playerService));
    this.handlerRegistry.set('moveUp', new MoveUpHandler(this.playerService));
    this.handlerRegistry.set('moveDown', new MoveDownHandler(this.playerService));

    // Action handlers
    this.handlerRegistry.set('jump', new JumpHandler(this.playerService));
    this.handlerRegistry.set('cycleMovementState', new CycleMovementStateHandler(this.playerService));
    this.handlerRegistry.set('toggleViewMode', new ToggleViewModeHandler(this.playerService));
    this.handlerRegistry.set('toggleShortcuts', new ToggleShortcutsHandler(this.playerService, this.appContext));

    // Rotation handler
    this.handlerRegistry.set('rotate', new RotateHandler(this.playerService));

    // Editor handlers (only in editor mode)
    if (__EDITOR__) {
      this.handlerRegistry.set('editSelectionRotator', new EditSelectionRotatorHandler(this.playerService, this.appContext));
      this.handlerRegistry.set('editorActivate', new EditorActivateHandler(this.playerService, this.appContext));
      this.handlerRegistry.set('blockEditorActivate', new BlockEditorActivateHandler(this.playerService, this.appContext));
      this.handlerRegistry.set('editConfigActivate', new EditConfigActivateHandler(this.playerService, this.appContext));
    }

    logger.debug('Central handlers registered', {
      handlers: Array.from(this.handlerRegistry.keys()),
    });
  }

  /**
   * Get a handler by key
   * Used by input controllers to retrieve shared handlers
   *
   * @param key Handler key (e.g., 'click', 'shortcut')
   * @returns InputHandler instance or undefined
   */
  getHandler(key: string): InputHandler | undefined {
    return this.handlerRegistry.get(key);
  }

  /**
   * Set the input controller
   *
   * @param controller Input controller instance
   */
  setController(controller: InputController): void {
    // Dispose existing controller
    if (this.controller) {
      this.controller.dispose();
    }

    this.controller = controller;
    this.controller.initialize();

    // Get handlers from controller
    this.handlers = this.controller.getHandlers();

    logger.info('Input controller set', { handlerCount: this.handlers.length });
  }

  /**
   * Update input handlers (called each frame)
   *
   * @param deltaTime Time since last frame in seconds
   */
  update(deltaTime: number): void {
    // Skip input handling if disabled (DEAD mode)
    if (!this.inputEnabled) {
      return;
    }

    try {
      for (const handler of this.handlers) {
        handler.update(deltaTime);
      }

      // After updating handlers, propagate position updates to active executors
      this.updateActiveExecutors();
    } catch (error) {
      ExceptionHandler.handle(error, 'InputService.update');
    }
  }

  /**
   * Propagates position/target updates from active shortcuts to ScrawlService.
   * Called each frame after handlers have been updated.
   */
  private updateActiveExecutors(): void {
    const shortcutService = this.appContext.services.shortcut;
    const scrawlService = this.appContext.services.scrawl;

    if (!shortcutService || !scrawlService) {
      return;
    }

    // For each active shortcut, update its executor parameters
    for (const shortcut of shortcutService.getActiveShortcuts()) {
      if (shortcut.lastPlayerPos) {
        scrawlService.updateExecutorParameter(
          shortcut.executorId,
          'playerPos',
          shortcut.lastPlayerPos
        );
      }

      if (shortcut.lastTargetPos) {
        scrawlService.updateExecutorParameter(
          shortcut.executorId,
          'targetPos',
          shortcut.lastTargetPos
        );
      }
    }
  }

  /**
   * Get player service
   */
  getPlayerService(): PlayerService {
    return this.playerService;
  }

  /**
   * Dispose input service
   */
  dispose(): void {
    if (this.controller) {
      this.controller.dispose();
    }

    this.handlers = [];

    logger.info('InputService disposed');
  }
}
