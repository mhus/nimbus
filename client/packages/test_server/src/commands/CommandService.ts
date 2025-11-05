/**
 * CommandService - Server-side command registration and execution
 *
 * Manages command handlers and executes commands from clients.
 * Sends responses using the command protocol (cmd.msg, cmd.rs).
 */

import { getLogger, ExceptionHandler, MessageType } from '@nimbus/shared';
import type { CommandHandler, CommandContext, CommandResult } from './CommandHandler';
import type { ClientSession } from '../types/ServerTypes';

const logger = getLogger('CommandService');

/**
 * CommandService - Central service for server command management
 */
export class CommandService {
  private handlers: Map<string, CommandHandler> = new Map();

  constructor() {
    logger.info('CommandService initialized');
  }

  /**
   * Register a command handler
   * @param handler Command handler to register
   */
  registerHandler(handler: CommandHandler): void {
    try {
      const name = handler.name();

      if (this.handlers.has(name)) {
        logger.warn(`Command handler '${name}' already registered, overwriting`, {
          oldHandler: this.handlers.get(name)?.constructor.name,
          newHandler: handler.constructor.name,
        });
      }

      this.handlers.set(name, handler);
      logger.debug(`Command handler '${name}' registered`, {
        handler: handler.constructor.name,
        description: handler.description(),
      });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'CommandService.registerHandler',
        { handler: handler.constructor.name }
      );
    }
  }

  /**
   * Execute a command from a client
   *
   * @param session Client session
   * @param requestId Request ID from client (i field)
   * @param cmd Command name
   * @param args Command arguments
   */
  async executeCommand(
    session: ClientSession,
    requestId: string,
    cmd: string,
    args: string[] = []
  ): Promise<void> {
    try {
      const handler = this.handlers.get(cmd);

      // Command not found
      if (!handler) {
        logger.warn(`Command not found: ${cmd}`, {
          sessionId: session.sessionId,
          username: session.username,
        });

        this.sendResult(session, requestId, {
          rc: -1, // Command not found
          message: `Command '${cmd}' not found. Use 'help' to list available commands.`,
        });
        return;
      }

      logger.debug(`Executing command '${cmd}'`, {
        sessionId: session.sessionId,
        username: session.username,
        args,
      });

      // Create command context
      const context: CommandContext = {
        session,
        sendMessage: (message: string) => {
          this.sendMessage(session, requestId, message);
        },
      };

      // Execute command
      const result = await handler.execute(context, args);

      logger.debug(`Command '${cmd}' executed`, {
        sessionId: session.sessionId,
        rc: result.rc,
      });

      // Send result
      this.sendResult(session, requestId, result);
    } catch (error) {
      ExceptionHandler.handle(error, 'CommandService.executeCommand', {
        cmd,
        args,
        sessionId: session.sessionId,
      });

      // Send error result
      this.sendResult(session, requestId, {
        rc: -4, // Internal error
        message: `Internal error: ${error instanceof Error ? error.message : 'Unknown error'}`,
      });
    }
  }

  /**
   * Get all registered command handlers
   * @returns Map of command name to handler
   */
  getHandlers(): Map<string, CommandHandler> {
    return new Map(this.handlers);
  }

  /**
   * Send intermediate message to client (cmd.msg)
   * @param session Client session
   * @param requestId Original request ID
   * @param message Message text
   */
  private sendMessage(session: ClientSession, requestId: string, message: string): void {
    try {
      const response = {
        r: requestId,
        t: MessageType.CMD_MESSAGE,
        d: {
          message,
        },
      };

      session.ws.send(JSON.stringify(response));
      logger.debug('Sent command message', { sessionId: session.sessionId, message });
    } catch (error) {
      ExceptionHandler.handle(error, 'CommandService.sendMessage', {
        requestId,
        sessionId: session.sessionId,
      });
    }
  }

  /**
   * Send final result to client (cmd.rs)
   * @param session Client session
   * @param requestId Original request ID
   * @param result Command result
   */
  private sendResult(session: ClientSession, requestId: string, result: CommandResult): void {
    try {
      const response = {
        r: requestId,
        t: MessageType.CMD_RESULT,
        d: {
          rc: result.rc,
          message: result.message,
        },
      };

      session.ws.send(JSON.stringify(response));
      logger.debug('Sent command result', {
        sessionId: session.sessionId,
        rc: result.rc,
      });
    } catch (error) {
      ExceptionHandler.handle(error, 'CommandService.sendResult', {
        requestId,
        sessionId: session.sessionId,
      });
    }
  }
}
