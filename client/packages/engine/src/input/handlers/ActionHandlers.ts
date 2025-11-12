/**
 * Action Input Handlers
 *
 * Handles discrete actions like jumping and toggling movement mode.
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

/**
 * Toggle Movement Mode Handler (Editor only)
 * Toggles between Walk and Fly modes
 */
export class ToggleMovementModeHandler extends InputHandler {
  protected onActivate(value: number): void {
    // Toggle is a discrete action, execute immediately
    this.playerService.toggleMovementMode();
  }

  protected onDeactivate(): void {
    // No action needed on deactivation
  }

  protected onUpdate(deltaTime: number, value: number): void {
    // Toggle doesn't need continuous updates
  }
}

/**
 * Toggle View Mode Handler
 * Toggles between Ego (first-person) and Third-Person view
 */
export class ToggleViewModeHandler extends InputHandler {
  protected onActivate(value: number): void {
    // Toggle is a discrete action, execute immediately
    this.playerService.toggleViewMode();
  }

  protected onDeactivate(): void {
    // No action needed on deactivation
  }

  protected onUpdate(deltaTime: number, value: number): void {
    // Toggle doesn't need continuous updates
  }
}
