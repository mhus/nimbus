/**
 * NavigateSelectedBlockCommand - Navigate to a block position (change selection only, no action)
 *
 * This command updates the selectedEditBlock in the session and sends the selection to the client
 * for visual feedback (green highlight), but does NOT execute any editAction.
 *
 * Usage: send navigateSelectedBlock [x] [y] [z]
 * Example: send navigateSelectedBlock 10 64 5
 */

import { CommandHandler, CommandContext, CommandResult } from './CommandHandler';
import { MessageType } from '@nimbus/shared';

/**
 * NavigateSelectedBlock command - Updates selection without executing action
 */
export class NavigateSelectedBlockCommand extends CommandHandler {
  constructor() {
    super();
  }

  name(): string {
    return 'navigateSelectedBlock';
  }

  description(): string {
    return 'Navigate to block position (selection only, no action execution)';
  }

  async execute(context: CommandContext, args: any[]): Promise<CommandResult> {
    // Parse coordinates
    if (args.length === 0) {
      // Clear selection
      context.session.selectedEditBlock = null;

      // Send setSelectedEditBlock to client to clear highlight
      await this.sendClientCommand(context, 'setSelectedEditBlock', []);

      return {
        rc: 0,
        message: 'Selection cleared',
      };
    }

    if (args.length !== 3) {
      return {
        rc: -3, // Invalid arguments
        message: 'Usage: navigateSelectedBlock [x] [y] [z] (or no args to clear)',
      };
    }

    // Validate coordinates
    const x = parseFloat(args[0]);
    const y = parseFloat(args[1]);
    const z = parseFloat(args[2]);

    if (isNaN(x) || isNaN(y) || isNaN(z)) {
      return {
        rc: -3, // Invalid arguments
        message: 'Invalid coordinates. Usage: navigateSelectedBlock [x] [y] [z]',
      };
    }

    const position = { x, y, z };

    // Update session selection
    context.session.selectedEditBlock = position;

    // Send setSelectedEditBlock to client for visual feedback (green highlight)
    await this.sendClientCommand(context, 'setSelectedEditBlock', [
      x.toString(),
      y.toString(),
      z.toString(),
    ]);

    return {
      rc: 0,
      message: `Navigated to block (${x}, ${y}, ${z})`,
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
