/**
 * Command Interface
 * Base interface for all console commands
 */
import type { CmdExecutionContext } from './CmdExecutionContext';

export interface Command {
  /**
   * Get command name (used for invocation)
   */
  getName(): string;

  /**
   * Get command description
   */
  getDescription(): string;

  /**
   * Get help text with usage examples
   */
  getHelp(): string;

  /**
   * Execute the command
   */
  execute(context: CmdExecutionContext): void | Promise<void>;
}
