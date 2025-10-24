/**
 * Command Controller
 * Parses input and executes registered commands
 */
import type { Command } from './Command';
import { CmdExecutionContext } from './CmdExecutionContext';

export class CommandController {
  private commands: Map<string, Command> = new Map();

  /**
   * Register a command
   */
  registerCommand(command: Command): void {
    const name = command.getName().toLowerCase();
    this.commands.set(name, command);
    console.log(`[CommandController] Registered command: ${name}`);
  }

  /**
   * Unregister a command
   */
  unregisterCommand(commandName: string): void {
    this.commands.delete(commandName.toLowerCase());
  }

  /**
   * Get all registered commands
   */
  getCommands(): Command[] {
    return Array.from(this.commands.values());
  }

  /**
   * Get command by name
   */
  getCommand(name: string): Command | undefined {
    return this.commands.get(name.toLowerCase());
  }

  /**
   * Parse input string into command name and parameters
   * Supports quoted strings for parameters with spaces
   */
  private parseInput(input: string): { command: string; parameters: string[] } {
    const parts: string[] = [];
    let current = '';
    let inQuote = false;
    let quoteChar = '';

    for (let i = 0; i < input.length; i++) {
      const char = input[i];

      if ((char === '"' || char === "'") && !inQuote) {
        inQuote = true;
        quoteChar = char;
      } else if (char === quoteChar && inQuote) {
        inQuote = false;
        quoteChar = '';
      } else if (char === ' ' && !inQuote) {
        if (current.length > 0) {
          parts.push(current);
          current = '';
        }
      } else {
        current += char;
      }
    }

    if (current.length > 0) {
      parts.push(current);
    }

    const command = parts[0] || '';
    const parameters = parts.slice(1);

    return { command, parameters };
  }

  /**
   * Execute command from input string
   * Returns the execution context with output
   */
  async executeCommand(input: string): Promise<CmdExecutionContext> {
    const trimmed = input.trim();
    const { command: commandName, parameters } = this.parseInput(trimmed);

    const context = new CmdExecutionContext(trimmed, parameters);

    if (!commandName) {
      context.writeError('No command specified');
      return context;
    }

    const command = this.getCommand(commandName);

    if (!command) {
      context.writeError(`Unknown command: ${commandName}`);
      context.writeLine('Type "help" for a list of available commands');
      return context;
    }

    try {
      await command.execute(context);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      context.writeError(`Command execution failed: ${errorMessage}`);
    }

    return context;
  }
}
