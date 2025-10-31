/**
 * Server Command-related messages
 *
 * Server commands allow the server to execute commands on the client.
 * This is the inverse of client commands (cmd).
 *
 * Message types:
 * - "scmd" (Server -> Client): Server command execution request
 * - "scmd.rs" (Client -> Server): Server command result/response
 *
 * Return codes (rc):
 * - Negative rc are system errors:
 *   * -1 = Command not found
 *   * -3 = Invalid arguments
 *   * -4 = Internal error
 *
 * - Positive rc are command-specific:
 *   * 0 = OK / true
 *   * 1 = Error / false
 *   * Other positive values are command-specific
 */

import type { RequestMessage, ResponseMessage } from '../BaseMessage';

/**
 * Server command data (Server -> Client)
 * Message type: "scmd"
 *
 * Server sends command to execute on client
 */
export interface ServerCommandData {
  /** Command string to execute */
  cmd: string;

  /** Command arguments */
  args?: string[];
}

/**
 * Server command message (Server -> Client)
 * Message type: "scmd"
 */
export type ServerCommandMessage = RequestMessage<ServerCommandData>;

/**
 * Server command result data (Client -> Server)
 * Message type: "scmd.rs"
 *
 * Client sends final result of server command execution
 *
 * Return codes:
 * - Negative: System errors (-1 = not found, -3 = invalid args, -4 = internal error)
 * - Zero: Success / OK
 * - Positive: Command-specific (1 = error/false, others are command-specific)
 */
export interface ServerCommandResultData {
  /** Return code */
  rc: number;

  /** Result/error message */
  message: string;
}

/**
 * Server command result message (Client -> Server)
 * Message type: "scmd.rs"
 */
export type ServerCommandResultMessage = ResponseMessage<ServerCommandResultData>;
