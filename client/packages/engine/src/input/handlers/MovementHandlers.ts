/**
 * Movement Input Handlers
 *
 * Handles player movement actions (forward, backward, left, right).
 */

import { InputHandler } from '../InputHandler';
import type { PlayerService } from '../../services/PlayerService';

/**
 * Move Forward Handler
 */
export class MoveForwardHandler extends InputHandler {
  protected onActivate(value: number): void {
    // Activation handled in update
  }

  protected onDeactivate(): void {
    // Deactivation handled in update
  }

  protected onUpdate(deltaTime: number, value: number): void {
    const distance = this.playerService.getMoveSpeed() * deltaTime * value;
    this.playerService.moveForward(distance);
  }
}

/**
 * Move Backward Handler
 */
export class MoveBackwardHandler extends InputHandler {
  protected onActivate(value: number): void {
    // Activation handled in update
  }

  protected onDeactivate(): void {
    // Deactivation handled in update
  }

  protected onUpdate(deltaTime: number, value: number): void {
    const distance = this.playerService.getMoveSpeed() * deltaTime * value;
    this.playerService.moveForward(-distance);
  }
}

/**
 * Move Left Handler
 */
export class MoveLeftHandler extends InputHandler {
  protected onActivate(value: number): void {
    // Activation handled in update
  }

  protected onDeactivate(): void {
    // Deactivation handled in update
  }

  protected onUpdate(deltaTime: number, value: number): void {
    const distance = this.playerService.getMoveSpeed() * deltaTime * value;
    this.playerService.moveRight(-distance);
  }
}

/**
 * Move Right Handler
 */
export class MoveRightHandler extends InputHandler {
  protected onActivate(value: number): void {
    // Activation handled in update
  }

  protected onDeactivate(): void {
    // Deactivation handled in update
  }

  protected onUpdate(deltaTime: number, value: number): void {
    const distance = this.playerService.getMoveSpeed() * deltaTime * value;
    this.playerService.moveRight(distance);
  }
}

/**
 * Move Up Handler (Fly mode only)
 */
export class MoveUpHandler extends InputHandler {
  protected onActivate(value: number): void {
    // Activation handled in update
  }

  protected onDeactivate(): void {
    // Deactivation handled in update
  }

  protected onUpdate(deltaTime: number, value: number): void {
    const distance = this.playerService.getMoveSpeed() * deltaTime * value;
    this.playerService.moveUp(distance);
  }
}

/**
 * Move Down Handler (Fly mode only)
 */
export class MoveDownHandler extends InputHandler {
  protected onActivate(value: number): void {
    // Activation handled in update
  }

  protected onDeactivate(): void {
    // Deactivation handled in update
  }

  protected onUpdate(deltaTime: number, value: number): void {
    const distance = this.playerService.getMoveSpeed() * deltaTime * value;
    this.playerService.moveUp(-distance);
  }
}
