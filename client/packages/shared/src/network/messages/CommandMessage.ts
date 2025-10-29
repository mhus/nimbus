/**
 * Command-related messages
 *
 * Commands are bidirectional messages that allow execution of commands
 * on the server from the client (e.g., console commands, editor commands).
 * The server can also send command-related messages back to the client.
 */

import type { BaseMessage } from '../BaseMessage';

/**
 * Command severity level
 */
export enum CommandSeverity {
  INFO = 'info',
  SUCCESS = 'success',
  WARNING = 'warning',
  ERROR = 'error',
}

/**
 * Command execution status
 */
export enum CommandStatus {
  PENDING = 'pending',
  EXECUTING = 'executing',
  SUCCESS = 'success',
  ERROR = 'error',
  CANCELLED = 'cancelled',
}

/**
 * Command data (Client -> Server)
 * Client sends command to execute on server
 */
export interface CommandData {
  /** Command string to execute */
  cmd: string;

  /** Command arguments */
  args?: string[];

  /** Additional context data */
  ctx?: Record<string, any>;
}

export type CommandMessage = BaseMessage<CommandData>;

/**
 * Command message data (Server -> Client)
 * Server sends informational messages during command execution
 */
export interface CommandMessageData {
  /** Message text */
  msg: string;

  /** Message severity */
  severity?: CommandSeverity;

  /** Additional data */
  data?: any;
}

export type CommandMessageMessage = BaseMessage<CommandMessageData>;

/**
 * Command result data (Server -> Client)
 * Server sends final result of command execution
 */
export interface CommandResultData {
  /** Execution status */
  status: CommandStatus;

  /** Result message */
  msg?: string;

  /** Result data */
  result?: any;

  /** Error information (if status is ERROR) */
  error?: {
    message: string;
    code?: string;
    details?: any;
  };

  /** Execution time in milliseconds */
  executionTime?: number;
}

export type CommandResultMessage = BaseMessage<CommandResultData>;
