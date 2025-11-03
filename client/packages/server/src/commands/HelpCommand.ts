/**
 * HelpCommand - Lists all available server commands
 */

import { CommandHandler, CommandContext, CommandResult } from './CommandHandler';
import type { CommandService } from './CommandService';

/**
 * Help command - Lists all available commands with descriptions
 */
export class HelpCommand extends CommandHandler {
  private commandService: CommandService;

  constructor(commandService: CommandService) {
    super();
    this.commandService = commandService;
  }

  name(): string {
    return 'help';
  }

  description(): string {
    return 'Lists all available server commands with descriptions';
  }

  execute(context: CommandContext, args: any[]): CommandResult {
    const handlers = this.commandService.getHandlers();

    if (handlers.size === 0) {
      return {
        rc: 0,
        message: 'No commands available',
      };
    }

    // Build formatted output
    const lines: string[] = [];
    lines.push('Available Server Commands:');
    lines.push('');

    for (const [name, handler] of handlers) {
      lines.push(`  ${name.padEnd(20)} - ${handler.description()}`);
    }

    lines.push('');
    lines.push('Usage: send <command> [args...]');

    const output = lines.join('\n');

    return {
      rc: 0,
      message: output,
    };
  }
}
