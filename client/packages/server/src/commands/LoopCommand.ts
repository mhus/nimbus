/**
 * LoopCommand - Sends a command back to the client
 *
 * This command allows the server to execute commands on the client.
 * The first argument is the command name for the client, remaining args are passed through.
 *
 * Usage: send loop <client-command> [args...]
 * Example: send loop info
 */

import { CommandHandler, CommandContext, CommandResult } from './CommandHandler';
import { MessageType } from '@nimbus/shared';

/**
 * Loop command - Sends commands back to the client for execution
 */
export class LoopCommand extends CommandHandler {
  name(): string {
    return 'loop';
  }

  description(): string {
    return 'Sends a command to the client for execution (loop <client-cmd> [args...])';
  }

  async execute(context: CommandContext, args: any[]): Promise<CommandResult> {
    if (args.length === 0) {
      return {
        rc: -3, // Invalid arguments
        message: 'Usage: loop <client-command> [args...]\nExample: loop info',
      };
    }

    const clientCmd = args[0].toString();
    const clientArgs = args.slice(1);

    context.sendMessage(`Sending command '${clientCmd}' to client...`);

    try {
      // Generate unique request ID
      const requestId = `scmd_${Date.now()}_${Math.random().toString(36).substring(7)}`;

      // Send server command to client (scmd)
      const serverCommand = {
        i: requestId,
        t: MessageType.SCMD,
        d: {
          cmd: clientCmd,
          args: clientArgs,
        },
      };

      context.session.ws.send(JSON.stringify(serverCommand));

      // Wait for response (scmd.rs) with timeout
      const result = await this.waitForClientResponse(context, requestId, 30000);

      if (result.rc === 0) {
        return {
          rc: 0,
          message: `Client command succeeded:\n${result.message}`,
        };
      } else {
        return {
          rc: 1,
          message: `Client command failed (rc=${result.rc}):\n${result.message}`,
        };
      }
    } catch (error) {
      return {
        rc: -4, // Internal error
        message: `Error: ${error instanceof Error ? error.message : 'Unknown error'}`,
      };
    }
  }

  /**
   * Wait for client response (scmd.rs)
   * @param context Command context
   * @param requestId Request ID to wait for
   * @param timeout Timeout in milliseconds
   * @returns Client response
   */
  private waitForClientResponse(
    context: CommandContext,
    requestId: string,
    timeout: number
  ): Promise<{ rc: number; message: string }> {
    return new Promise((resolve, reject) => {
      const timeoutHandle = setTimeout(() => {
        cleanup();
        reject(new Error('Client response timeout'));
      }, timeout);

      const messageHandler = (data: Buffer) => {
        try {
          const message = JSON.parse(data.toString());

          // Check if this is our response
          if (message.r === requestId && message.t === MessageType.SCMD_RESULT) {
            cleanup();
            resolve(message.d);
          }
        } catch (error) {
          // Ignore parse errors
        }
      };

      const cleanup = () => {
        clearTimeout(timeoutHandle);
        context.session.ws.off('message', messageHandler);
      };

      // Listen for response
      context.session.ws.on('message', messageHandler);
    });
  }
}
