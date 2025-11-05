/**
 * SendCommand - Sends commands to the server for execution
 *
 * Usage: send <command> [args...]
 * Example: send say "Hello, world!"
 */

import { CommandHandler } from './CommandHandler';
import type { CommandService } from '../services/CommandService';

/**
 * Send command - Executes commands on the server
 */
export class SendCommand extends CommandHandler {
  private commandService: CommandService;

  constructor(commandService: CommandService) {
    super();
    this.commandService = commandService;
  }

  name(): string {
    return 'send';
  }

  description(): string {
    return 'Sends a command to the server for execution';
  }

  async execute(parameters: any[]): Promise<any> {
    if (parameters.length === 0) {
      console.error('Usage: send <command> [args...]');
      console.error('Example: send say "Hello, world!"');
      return { error: 'No command specified' };
    }

    const cmd = parameters[0].toString();
    const args = parameters.slice(1);

    console.log(`Sending command to server: ${cmd}`, args.length > 0 ? args : '');

    try {
      // Send command to server and wait for result
      const result = await this.commandService.sendCommandToServer(cmd, args, (message) => {
        // Handle intermediate messages
        console.log(`[Server] ${message}`);
      });

      // Display result
      if (result.rc === 0) {
        console.log(`✓ Command successful: ${result.message}`);
        return result;
      } else {
        console.error(`✗ Command failed (rc=${result.rc}): ${result.message}`);
        return result;
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error(`✗ Command error: ${errorMessage}`);
      throw error;
    }
  }
}
