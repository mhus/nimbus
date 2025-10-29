/**
 * Action Input Handlers
 *
 * Handles discrete actions like jumping.
 */

import { InputHandler } from '../InputHandler';
import type { PlayerService } from '../../services/PlayerService';

/**
 * Jump Handler
 */
export class JumpHandler extends InputHandler {
  protected onActivate(value: number): void {
    // Jump is a discrete action, execute immediately
    this.playerService.jump();
  }

  protected onDeactivate(): void {
    // No action needed on deactivation
  }

  protected onUpdate(deltaTime: number, value: number): void {
    // Jump doesn't need continuous updates
  }
}
