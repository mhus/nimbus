/**
 * CommandHandler - Abstract base class for server command handlers
 *
 * Command handlers implement specific commands that can be executed
 * by clients via the command protocol (MessageType.CMD).
 */

import type { ClientSession } from '../types/ServerTypes';

/**
 * Command execution context
 * Provides access to session and server state
 */
export interface CommandContext {
  /** Client session executing the command */
  session: ClientSession;

  /** Send intermediate message to client (cmd.msg) */
  sendMessage: (message: string) => void;
}

/**
 * Abstract base class for command handlers
 *
 * Subclasses must implement:
 * - name(): string - Command name (e.g., "help", "tp")
 * - description(): string - Short description
 * - execute(context, args): Promise<CommandResult> - Command logic
 */
export abstract class CommandHandler {
  /**
   * Get the command name
   * @returns Command name (lowercase, no spaces)
   */
  abstract name(): string;

  /**
   * Get a short description of what this command does
   * @returns Command description
   */
  abstract description(): string;

  /**
   * Execute the command
   * Can be synchronous or asynchronous
   *
   * @param context Command execution context
   * @param args Command arguments
   * @returns Command result (rc=0 for success, non-zero for error)
   * @throws Error if command execution fails
   */
  abstract execute(
    context: CommandContext,
    args: string[]
  ): Promise<CommandResult> | CommandResult;
}

/**
 * Command execution result
 */
export interface CommandResult {
  /** Return code (0 = success, negative = system error, positive = command error) */
  rc: number;

  /** Result message */
  message: string;
}
