/**
 * HelpCommand - Lists all available commands with descriptions
 */

import { CommandHandler } from './CommandHandler';
import type { CommandService } from '../services/CommandService';

/**
 * Help command - Lists all available commands
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
    return 'Lists all available commands with descriptions';
  }

  execute(parameters: any[]): any {
    const handlers = this.commandService.getHandlers();

    if (handlers.size === 0) {
      return 'No commands available';
    }

    // Build formatted output
    const lines: string[] = [];
    lines.push('Available Commands:');
    lines.push('');

    for (const [name, handler] of handlers) {
      // Generate function name for display
      const functionName = 'do' + name.charAt(0).toUpperCase() + name.slice(1);
      lines.push(`  ${functionName.padEnd(20)} - ${handler.description()}`);
    }

    const output = lines.join('\n');

    // Return as both string and log to console
    console.log(output);

    return output;
  }
}
