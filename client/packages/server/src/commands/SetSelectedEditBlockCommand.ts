/**
 * SetSelectedEditBlockCommand - Sets the selected edit block for a session
 *
 * This command stores the selected edit block position in the session and
 * executes the configured edit action (OPEN_CONFIG_DIALOG, OPEN_EDITOR, MARK_BLOCK, etc.).
 *
 * Usage: send setSelectedEditBlock [x] [y] [z]
 * Example: send setSelectedEditBlock 10 64 5
 */

import { CommandHandler, CommandContext, CommandResult } from './CommandHandler';
import { MessageType, EditAction, type Block } from '@nimbus/shared';
import type { WorldManager } from '../world/WorldManager';

/**
 * SetSelectedEditBlock command - Sets selected edit block and executes action
 */
export class SetSelectedEditBlockCommand extends CommandHandler {
  private worldManager: WorldManager;

  constructor(worldManager: WorldManager) {
    super();
    this.worldManager = worldManager;
  }

  name(): string {
    return 'setSelectedEditBlock';
  }

  description(): string {
    return 'Sets the selected edit block for the session (setSelectedEditBlock [x] [y] [z])';
  }

  async execute(context: CommandContext, args: any[]): Promise<CommandResult> {
    // Parse coordinates
    let position: { x: number; y: number; z: number } | null = null;

    if (args.length === 0) {
      // Clear selection
      context.session.selectedEditBlock = null;
    } else if (args.length === 3) {
      // Set position
      const x = parseFloat(args[0]);
      const y = parseFloat(args[1]);
      const z = parseFloat(args[2]);

      if (isNaN(x) || isNaN(y) || isNaN(z)) {
        return {
          rc: -3, // Invalid arguments
          message: 'Invalid coordinates. Usage: setSelectedEditBlock [x] [y] [z]',
        };
      }

      position = { x, y, z };
      context.session.selectedEditBlock = position;
    } else {
      return {
        rc: -3, // Invalid arguments
        message: 'Usage: setSelectedEditBlock [x] [y] [z] (or no args to clear)',
      };
    }

    // Always send setSelectedEditBlock to client for visual feedback
    await this.sendClientCommand(context, 'setSelectedEditBlock', position ? [position.x.toString(), position.y.toString(), position.z.toString()] : []);

    // Execute action based on editAction (default: OPEN_CONFIG_DIALOG)
    if (position) {
      const action = context.session.editAction || EditAction.OPEN_CONFIG_DIALOG;

      try {
        const actionResult = await this.executeAction(context, action, position);
        if (actionResult.rc !== 0) {
          return actionResult;
        }
      } catch (error) {
        return {
          rc: -4, // Internal error
          message: `Action execution failed: ${error instanceof Error ? error.message : 'Unknown error'}`,
        };
      }
    }

    return {
      rc: 0,
      message: position
        ? `Selected edit block set to (${position.x}, ${position.y}, ${position.z})`
        : 'Selected edit block cleared',
    };
  }

  /**
   * Execute the configured edit action
   */
  private async executeAction(
    context: CommandContext,
    action: EditAction,
    position: { x: number; y: number; z: number }
  ): Promise<CommandResult> {
    switch (action) {
      case EditAction.OPEN_CONFIG_DIALOG:
        // Send openComponent command to client
        await this.sendClientCommand(context, 'openComponent', ['edit_config']);
        return { rc: 0, message: 'Opening config dialog' };

      case EditAction.OPEN_EDITOR:
        // Send openComponent command to client with position
        await this.sendClientCommand(context, 'openComponent', [
          'block_editor',
          position.x.toString(),
          position.y.toString(),
          position.z.toString(),
        ]);
        return { rc: 0, message: 'Opening block editor' };

      case EditAction.MARK_BLOCK:
        // Store marked block (server-side only)
        context.session.markedEditBlock = position;
        return { rc: 0, message: `Block marked at (${position.x}, ${position.y}, ${position.z})` };

      case EditAction.COPY_BLOCK:
        return await this.executeCopyBlock(context, position);

      case EditAction.DELETE_BLOCK:
        return await this.executeDeleteBlock(context, position);

      case EditAction.MOVE_BLOCK:
        return await this.executeMoveBlock(context, position);

      default:
        return { rc: 1, message: `Unknown action: ${action}` };
    }
  }

  /**
   * Execute COPY_BLOCK action
   */
  private async executeCopyBlock(
    context: CommandContext,
    targetPosition: { x: number; y: number; z: number }
  ): Promise<CommandResult> {
    // Check if block is marked
    if (!context.session.markedEditBlock) {
      return {
        rc: 1,
        message: 'No block marked for copy. Use MARK_BLOCK action first.',
      };
    }

    const worldId = context.session.worldId;
    if (!worldId) {
      return { rc: 1, message: 'No world selected' };
    }

    // Get source block
    const sourceBlock = await this.worldManager.getBlock(
      worldId,
      context.session.markedEditBlock.x,
      context.session.markedEditBlock.y,
      context.session.markedEditBlock.z
    );

    if (!sourceBlock) {
      return {
        rc: 1,
        message: `No block found at marked position (${context.session.markedEditBlock.x}, ${context.session.markedEditBlock.y}, ${context.session.markedEditBlock.z})`,
      };
    }

    // Copy block to target position
    const targetBlock: Block = {
      ...sourceBlock,
      position: targetPosition,
    };

    const success = await this.worldManager.setBlock(worldId, targetBlock);

    if (!success) {
      return { rc: 1, message: 'Failed to copy block' };
    }

    return {
      rc: 0,
      message: `Block copied from (${context.session.markedEditBlock.x}, ${context.session.markedEditBlock.y}, ${context.session.markedEditBlock.z}) to (${targetPosition.x}, ${targetPosition.y}, ${targetPosition.z})`,
    };
  }

  /**
   * Execute DELETE_BLOCK action
   */
  private async executeDeleteBlock(
    context: CommandContext,
    position: { x: number; y: number; z: number }
  ): Promise<CommandResult> {
    const worldId = context.session.worldId;
    if (!worldId) {
      return { rc: 1, message: 'No world selected' };
    }

    const success = await this.worldManager.deleteBlock(worldId, position.x, position.y, position.z);

    if (!success) {
      return { rc: 1, message: 'Failed to delete block (block may not exist)' };
    }

    // Clear selection after deletion
    context.session.selectedEditBlock = null;
    await this.sendClientCommand(context, 'setSelectedEditBlock', []);

    return { rc: 0, message: `Block deleted at (${position.x}, ${position.y}, ${position.z})` };
  }

  /**
   * Execute MOVE_BLOCK action
   */
  private async executeMoveBlock(
    context: CommandContext,
    targetPosition: { x: number; y: number; z: number }
  ): Promise<CommandResult> {
    // Check if block is marked
    if (!context.session.markedEditBlock) {
      return {
        rc: 1,
        message: 'No block marked for move. Use MARK_BLOCK action first.',
      };
    }

    const worldId = context.session.worldId;
    if (!worldId) {
      return { rc: 1, message: 'No world selected' };
    }

    // Get source block
    const sourceBlock = await this.worldManager.getBlock(
      worldId,
      context.session.markedEditBlock.x,
      context.session.markedEditBlock.y,
      context.session.markedEditBlock.z
    );

    if (!sourceBlock) {
      return {
        rc: 1,
        message: `No block found at marked position (${context.session.markedEditBlock.x}, ${context.session.markedEditBlock.y}, ${context.session.markedEditBlock.z})`,
      };
    }

    // Copy block to target position
    const targetBlock: Block = {
      ...sourceBlock,
      position: targetPosition,
    };

    const copySuccess = await this.worldManager.setBlock(worldId, targetBlock);

    if (!copySuccess) {
      return { rc: 1, message: 'Failed to copy block to new position' };
    }

    // Delete old position
    const deleteSuccess = await this.worldManager.deleteBlock(
      worldId,
      context.session.markedEditBlock.x,
      context.session.markedEditBlock.y,
      context.session.markedEditBlock.z
    );

    if (!deleteSuccess) {
      // Block was copied but deletion failed - warn user
      return {
        rc: 1,
        message: `Block copied but failed to delete old position at (${context.session.markedEditBlock.x}, ${context.session.markedEditBlock.y}, ${context.session.markedEditBlock.z})`,
      };
    }

    // Clear marked block after successful move
    const sourcePos = context.session.markedEditBlock;
    context.session.markedEditBlock = null;

    return {
      rc: 0,
      message: `Block moved from (${sourcePos.x}, ${sourcePos.y}, ${sourcePos.z}) to (${targetPosition.x}, ${targetPosition.y}, ${targetPosition.z})`,
    };
  }

  /**
   * Send command to client (SCMD)
   * Does not wait for response (fire-and-forget)
   */
  private async sendClientCommand(context: CommandContext, cmd: string, args: string[]): Promise<void> {
    const requestId = `scmd_${Date.now()}_${Math.random().toString(36).substring(7)}`;

    const serverCommand = {
      i: requestId,
      t: MessageType.SCMD,
      d: {
        cmd,
        args,
      },
    };

    context.session.ws.send(JSON.stringify(serverCommand));
  }
}
