/**
 * WebInputController - Browser input controller
 *
 * Handles keyboard and mouse input for web browsers.
 * Binds WASD + Space + Mouse for player control.
 */

import { getLogger } from '@nimbus/shared';
import type { InputController } from '../services/InputService';
import type { PlayerService } from '../services/PlayerService';
import type { InputHandler } from './InputHandler';
import {
  MoveForwardHandler,
  MoveBackwardHandler,
  MoveLeftHandler,
  MoveRightHandler,
} from './handlers/MovementHandlers';
import { JumpHandler } from './handlers/ActionHandlers';
import { RotateHandler } from './handlers/RotationHandlers';

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
 * - Space: Jump
 * - Mouse: Look around (when pointer locked)
 */
export class WebInputController implements InputController {
  private canvas: HTMLCanvasElement;
  private playerService: PlayerService;

  private handlers: InputHandler[] = [];
  private keyBindings: Map<string, InputHandler> = new Map();

  // Handlers
  private moveForwardHandler: MoveForwardHandler;
  private moveBackwardHandler: MoveBackwardHandler;
  private moveLeftHandler: MoveLeftHandler;
  private moveRightHandler: MoveRightHandler;
  private jumpHandler: JumpHandler;
  private rotateHandler: RotateHandler;

  // Pointer lock state
  private pointerLocked: boolean = false;

  constructor(canvas: HTMLCanvasElement, playerService: PlayerService) {
    this.canvas = canvas;
    this.playerService = playerService;

    // Create handlers
    this.moveForwardHandler = new MoveForwardHandler(playerService);
    this.moveBackwardHandler = new MoveBackwardHandler(playerService);
    this.moveLeftHandler = new MoveLeftHandler(playerService);
    this.moveRightHandler = new MoveRightHandler(playerService);
    this.jumpHandler = new JumpHandler(playerService);
    this.rotateHandler = new RotateHandler(playerService);

    this.handlers = [
      this.moveForwardHandler,
      this.moveBackwardHandler,
      this.moveLeftHandler,
      this.moveRightHandler,
      this.jumpHandler,
      this.rotateHandler,
    ];

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
    this.keyBindings.set(' ', this.jumpHandler); // Space
  }

  /**
   * Initialize controller
   */
  initialize(): void {
    // Add event listeners
    window.addEventListener('keydown', this.onKeyDown);
    window.addEventListener('keyup', this.onKeyUp);
    this.canvas.addEventListener('click', this.onCanvasClick);
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
    const handler = this.keyBindings.get(event.key);
    if (handler && !handler.isActive()) {
      handler.activate();
      event.preventDefault();
    }
  };

  /**
   * Handle keyup event
   */
  private onKeyUp = (event: KeyboardEvent): void => {
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
