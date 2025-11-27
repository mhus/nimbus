/**
 * Action Input Handlers
 *
 * Handles discrete actions like jumping and toggling movement mode.
 */

import { InputHandler } from '../InputHandler';
import type { PlayerService } from '../../services/PlayerService';
import { PlayerMovementState, getLogger } from '@nimbus/shared';

const logger = getLogger('ActionHandlers');

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
 * Cycle Movement State Handler (F key)
 * Cycles through movement states: FREE_FLY → FLY → SPRINT → CROUCH → WALK
 * FREE_FLY and FLY are only available in Editor mode (__EDITOR__ = true)
 */
export class CycleMovementStateHandler extends InputHandler {
  protected onActivate(value: number): void {
    const current = this.playerService.getMovementState();

    // Determine next state in cycle
    let nextState: PlayerMovementState;

    if (__EDITOR__) {
      // Editor mode: Include FREE_FLY and FLY in rotation
      switch (current) {
        case PlayerMovementState.WALK:
          nextState = PlayerMovementState.FREE_FLY;
          break;
        case PlayerMovementState.FREE_FLY:
          nextState = PlayerMovementState.FLY;
          break;
        case PlayerMovementState.FLY:
          nextState = PlayerMovementState.SPRINT;
          break;
        case PlayerMovementState.SPRINT:
          nextState = PlayerMovementState.CROUCH;
          break;
        case PlayerMovementState.CROUCH:
        default:
          nextState = PlayerMovementState.WALK;
          break;
      }
    } else {
      // Viewer mode: Skip FLY modes in rotation
      switch (current) {
        case PlayerMovementState.WALK:
          nextState = PlayerMovementState.SPRINT;
          break;
        case PlayerMovementState.SPRINT:
          nextState = PlayerMovementState.CROUCH;
          break;
        case PlayerMovementState.CROUCH:
        default:
          nextState = PlayerMovementState.WALK;
          break;
      }
    }

    this.playerService.setMovementState(nextState);
  }

  protected onDeactivate(): void {
    // No action needed on deactivation
  }

  protected onUpdate(deltaTime: number, value: number): void {
    // Cycle doesn't need continuous updates
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

/**
 * Toggle Fullscreen Handler (F6)
 * Toggles browser fullscreen mode
 */
export class ToggleFullscreenHandler extends InputHandler {
  protected onActivate(value: number): void {
    try {
      if (!document.fullscreenElement) {
        // Enter fullscreen
        document.documentElement.requestFullscreen().catch(err => {
          logger.error('Failed to enter fullscreen:', err);
        });
      } else {
        // Exit fullscreen
        document.exitFullscreen().catch(err => {
          logger.error('Failed to exit fullscreen:', err);
        });
      }
    } catch (error) {
      logger.error('Fullscreen toggle error:', error);
    }
  }

  protected onDeactivate(): void {
    // No action needed on deactivation
  }

  protected onUpdate(deltaTime: number, value: number): void {
    // Toggle doesn't need continuous updates
  }
}

/**
 * Toggle Shortcuts Handler (T key)
 * Toggles shortcut display: keys -> clicks -> slots0 -> slots1 -> off
 */
export class ToggleShortcutsHandler extends InputHandler {
  protected onActivate(value: number): void {
    // Toggle is a discrete action, execute immediately
    const notificationService = this.appContext?.services.notification;
    if (notificationService) {
      notificationService.toggleShowShortcuts();
    }
  }

  protected onDeactivate(): void {
    // No action needed on deactivation
  }

  protected onUpdate(deltaTime: number, value: number): void {
    // Toggle doesn't need continuous updates
  }
}
