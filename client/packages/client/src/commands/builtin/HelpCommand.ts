/**
 * Help Command
 * Lists all available commands
 */
import type { Command } from '../Command';
import type { CmdExecutionContext } from '../CmdExecutionContext';
import type { CommandController } from '../CommandController';

export class HelpCommand implements Command {
  private commandController: CommandController;

  constructor(commandController: CommandController) {
    this.commandController = commandController;
  }

  getName(): string {
    return 'help';
  }

  getDescription(): string {
    return 'Lists all available commands';
  }

  getHelp(): string {
    return `Usage: help [command]

Lists all available commands or shows help for a specific command.

Examples:
  help              - Show all commands
  help teleport     - Show help for teleport command`;
  }

  execute(context: CmdExecutionContext): void {
    const commandName = context.getParameter(0);

    if (commandName) {
      // Show help for specific command
      const command = this.commandController.getCommand(commandName);

      if (!command) {
        context.writeError(`Unknown command: ${commandName}`);
        return;
      }

      context.writeLine(`Command: ${command.getName()}`);
      context.writeLine(`Description: ${command.getDescription()}`);
      context.writeLine('');
      context.writeLine(command.getHelp());
    } else {
      // Show all commands
      const commands = this.commandController.getCommands();

      context.writeLine('Available commands:');
      context.writeLine('');

      commands.forEach(cmd => {
        context.writeLine(`  ${cmd.getName().padEnd(15)} - ${cmd.getDescription()}`);
      });

      context.writeLine('');
      context.writeLine('Type "help <command>" for more information about a specific command');
    }
  }
}
