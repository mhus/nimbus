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

  constructor(appContext: AppContext, playerService: PlayerService) {
    this.appContext = appContext;
    this.playerService = playerService;

    logger.info('InputService initialized');
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
