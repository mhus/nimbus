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
    } catch (error) {
      ExceptionHandler.handle(error, 'InputService.update');
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
