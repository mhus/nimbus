/**
 * View Commands
 * Commands for view settings (grid, collision, editor, etc.)
 */
import type { Command } from '../Command';
import type { CmdExecutionContext } from '../CmdExecutionContext';
import type { VoxelClient } from '../../VoxelClient';

/**
 * Grid Command - Toggle grid/wireframe view
 */
export class GridCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'raster';
  }

  getDescription(): string {
    return 'Toggle grid/wireframe view';
  }

  getHelp(): string {
    return `Usage: raster [on|off]

Toggles the grid/wireframe rendering mode for all blocks.

Examples:
  raster        - Toggle grid mode
  raster on     - Enable grid mode
  raster off    - Disable grid mode`;
  }

  execute(context: CmdExecutionContext): void {
    const chunkManager = (this.client as any).chunkManager;

    if (!chunkManager) {
      context.writeError('Chunk manager not available');
      return;
    }

    const param = context.getParameter(0)?.toLowerCase();
    let enableGrid: boolean;

    if (param === 'on') {
      enableGrid = true;
    } else if (param === 'off') {
      enableGrid = false;
    } else {
      // Toggle
      const currentState = chunkManager.isWireframeEnabled?.() ?? false;
      enableGrid = !currentState;
    }

    // Set wireframe mode on chunk manager
    if (enableGrid) {
      chunkManager.enableWireframe();
      context.writeLine('Grid mode enabled');
    } else {
      chunkManager.disableWireframe();
      context.writeLine('Grid mode disabled');
    }
  }
}

/**
 * Collision Command - Toggle collision detection in flight mode
 */
export class CollisionCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'collision';
  }

  getDescription(): string {
    return 'Toggle collision detection in flight mode';
  }

  getHelp(): string {
    return `Usage: collision [on|off]

Toggles collision detection in flight mode. When disabled, you can fly through blocks.
Collision is always active in walk mode.

Examples:
  collision        - Toggle collision detection
  collision on     - Enable collision detection
  collision off    - Disable collision detection`;
  }

  execute(context: CmdExecutionContext): void {
    const playerController = (this.client as any).playerController;

    if (!playerController) {
      context.writeError('Player controller not available');
      return;
    }

    const param = context.getParameter(0)?.toLowerCase();
    let enableCollision: boolean;

    if (param === 'on') {
      enableCollision = true;
    } else if (param === 'off') {
      enableCollision = false;
    } else {
      // Toggle
      const currentState = playerController.isCollisionEnabled?.() ?? true;
      enableCollision = !currentState;
    }

    // Set collision mode on player controller
    if (enableCollision) {
      playerController.enableCollision();
      context.writeLine('Collision detection enabled');
    } else {
      playerController.disableCollision();
      context.writeLine('Collision detection disabled (flight mode only)');
    }
  }
}

/**
 * Editor Command - Toggle block editor
 */
export class EditorCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'editor';
  }

  getDescription(): string {
    return 'Toggle block editor';
  }

  getHelp(): string {
    return `Usage: editor [on|off]

Toggles the block editor panel. When enabled, shows detailed information
about the currently selected block (requires selection mode to be active).

Examples:
  editor        - Toggle editor
  editor on     - Show editor
  editor off    - Hide editor`;
  }

  execute(context: CmdExecutionContext): void {
    const blockEditor = (this.client as any).blockEditor;

    if (!blockEditor) {
      context.writeError('Block editor not available');
      return;
    }

    const param = context.getParameter(0)?.toLowerCase();

    if (param === 'on') {
      blockEditor.show();
      context.writeLine('Block editor shown');
    } else if (param === 'off') {
      blockEditor.hide();
      context.writeLine('Block editor hidden');
    } else {
      // Toggle
      blockEditor.toggle();
      const isVisible = blockEditor.getIsVisible();
      context.writeLine(`Block editor ${isVisible ? 'shown' : 'hidden'}`);
    }
  }
}
