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
    return 'Lists all available server commands or shows details for a specific command (help [commandName])';
  }

  execute(context: CommandContext, args: any[]): CommandResult {
    const handlers = this.commandService.getHandlers();

    if (handlers.size === 0) {
      return {
        rc: 0,
        message: 'No commands available',
      };
    }

    // If a command name is provided, show details for that command
    if (args.length > 0) {
      const commandName = args[0].toString();
      const handler = handlers.get(commandName);

      if (!handler) {
        return {
          rc: 1,
          message: `Command '${commandName}' not found. Use 'help' to see all available commands.`,
        };
      }

      // Show detailed help for this command
      const lines: string[] = [];
      lines.push(`Command: ${commandName}`);
      lines.push('');
      lines.push(`Description:`);
      lines.push(`  ${handler.description()}`);
      lines.push('');
      lines.push(`Usage:`);
      lines.push(`  send ${commandName} [args...]`);

      const output = lines.join('\n');

      return {
        rc: 0,
        message: output,
      };
    }

    // Build formatted output - list all commands
    const lines: string[] = [];
    lines.push('Available Server Commands:');
    lines.push('');

    for (const [name, handler] of handlers) {
      lines.push(`  ${name.padEnd(20)} - ${handler.description()}`);
    }

    lines.push('');
    lines.push('Use "help <commandName>" for detailed information about a specific command.');
    lines.push('Usage: send <command> [args...]');

    const output = lines.join('\n');

    return {
      rc: 0,
      message: output,
    };
  }
}
