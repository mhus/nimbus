/**
 * CommandService - Manages command registration and execution
 *
 * The CommandService is a singleton service that:
 * - Registers command handlers
 * - Executes commands by name
 * - Exposes commands to browser console (EDITOR mode only)
 */

import {
  getLogger,
  ExceptionHandler,
  MessageType,
  RequestMessage,
  CommandData,
  CommandResultData,
} from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { CommandHandler } from '../commands/CommandHandler';

const logger = getLogger('CommandService');

/**
 * Pending server command request
 */
interface PendingServerCommand {
  resolve: (result: CommandResultData) => void;
  reject: (error: Error) => void;
  timeout: NodeJS.Timeout;
  onMessage?: (message: string) => void;
}

/**
 * CommandService - Central service for command management
 */
export class CommandService {
  private appContext: AppContext;
  private handlers: Map<string, CommandHandler> = new Map();
  private pendingServerCommands: Map<string, PendingServerCommand> = new Map();

  constructor(appContext: AppContext) {
    this.appContext = appContext;
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
   * Execute a command by name
   * @param name Command name
   * @param parameters Command parameters
   * @returns Command result
   */
  async executeCommand(name: string, parameters: string[]): Promise<any> {
    try {
      const handler = this.handlers.get(name);

      if (!handler) {
        const error = new Error(`Command '${name}' not found`);
        logger.error(`Command not found: ${name}`, { availableCommands: Array.from(this.handlers.keys()) });
        throw error;
      }

      logger.debug(`Executing command '${name}'`, { parameters });

      const result = await handler.execute(parameters);

      logger.debug(`Command '${name}' executed successfully`, { result });

      return result;
    } catch (error) {
      ExceptionHandler.handle(error, 'CommandService.executeCommand', { name, parameters });
      throw error; // Re-throw so caller can handle it
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
   * Get list of all command names
   * @returns Array of command names
   */
  getCommandNames(): string[] {
    return Array.from(this.handlers.keys());
  }

  /**
   * Expose commands to browser console as do* functions
   * This should only be called in EDITOR mode
   *
   * For each registered command handler with name "foo", creates:
   * window.doFoo = (...params: string[]) => executeCommand('foo', params)
   */
  exposeToBrowserConsole(): void {
    try {
      logger.info('Exposing commands to browser console...');

      let exposedCount = 0;

      for (const [name, handler] of this.handlers) {
        // Generate function name: do + Capitalize(name)
        const functionName = 'do' + name.charAt(0).toUpperCase() + name.slice(1);

        // Create function that calls executeCommand
        (window as any)[functionName] = (...params: string[]) => {
          this.executeCommand(name, params)
            .then((result) => {
              // Log result to console
              if (result !== undefined && result !== null) {
                console.log(`✓ ${name}:`, result);
              } else {
                console.log(`✓ ${name}: Command executed successfully`);
              }
            })
            .catch((error) => {
              // Log error to console
              console.error(`✗ ${name}:`, error.message || error);
            });
        };

        logger.debug(`Exposed command '${name}' as window.${functionName}()`, {
          description: handler.description(),
        });

        exposedCount++;
      }

      logger.info(`${exposedCount} commands exposed to browser console`, {
        commands: Array.from(this.handlers.keys()),
      });

      // Log available commands to console
      console.log('=== Nimbus Commands ===');
      console.log('Available commands:');
      for (const [name, handler] of this.handlers) {
        const functionName = 'do' + name.charAt(0).toUpperCase() + name.slice(1);
        console.log(`  ${functionName}() - ${handler.description()}`);
      }
      console.log('=======================');
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'CommandService.exposeToBrowserConsole'
      );
    }
  }

  /**
   * Send a command to the server for execution
   * Waits for response with 30 second timeout
   * Intermediate messages (cmd.msg) are logged to console
   *
   * @param cmd Command string
   * @param args Command arguments
   * @returns Promise that resolves with command result
   */
  async sendCommandToServer(
    cmd: string,
    args: string[] = [],
    onMessage?: (message: string) => void
  ): Promise<CommandResultData> {
    const networkService = this.appContext.services.network;

    if (!networkService) {
      throw new Error('NetworkService not available');
    }

    if (!networkService.isConnected()) {
      throw new Error('Not connected to server');
    }

    // Generate message ID
    const messageId = networkService.generateMessageId();

    logger.debug(`Sending command to server: ${cmd}`, { messageId, args });

    // Create command message
    const message: RequestMessage<CommandData> = {
      i: messageId,
      t: MessageType.CMD,
      d: {
        cmd,
        args,
      },
    };

    // Create promise for response
    return new Promise<CommandResultData>((resolve, reject) => {
      // Set 30 second timeout
      const timeout = setTimeout(() => {
        this.pendingServerCommands.delete(messageId);
        reject(new Error(`Command timeout: ${cmd}`));
      }, 30000);

      // Store pending request
      this.pendingServerCommands.set(messageId, {
        resolve,
        reject,
        timeout,
        onMessage,
      });

      // Send message
      try {
        networkService.send(message);
      } catch (error) {
        this.pendingServerCommands.delete(messageId);
        clearTimeout(timeout);
        throw error;
      }
    });
  }

  /**
   * Handle intermediate command message from server (cmd.msg)
   * Called by CommandMessageHandler
   *
   * @param requestId Original request ID (r field)
   * @param message Message text
   */
  handleCommandMessage(requestId: string, message: string): void {
    const pending = this.pendingServerCommands.get(requestId);

    if (!pending) {
      logger.warn(`Received cmd.msg for unknown request: ${requestId}`);
      return;
    }

    logger.debug(`Command message from server: ${message}`, { requestId });

    // Call onMessage callback if provided
    if (pending.onMessage) {
      pending.onMessage(message);
    } else {
      // Default: log to console
      console.log(`[Server] ${message}`);
    }
  }

  /**
   * Handle command result from server (cmd.rs)
   * Called by CommandResultHandler
   *
   * @param requestId Original request ID (r field)
   * @param result Command result data
   */
  handleCommandResult(requestId: string, result: CommandResultData): void {
    const pending = this.pendingServerCommands.get(requestId);

    if (!pending) {
      logger.warn(`Received cmd.rs for unknown request: ${requestId}`);
      return;
    }

    // Clear timeout and remove from pending
    clearTimeout(pending.timeout);
    this.pendingServerCommands.delete(requestId);

    logger.debug(`Command result from server`, { requestId, rc: result.rc, message: result.message });

    // Resolve or reject based on return code
    if (result.rc === 0) {
      pending.resolve(result);
    } else {
      pending.reject(new Error(`Command failed (rc=${result.rc}): ${result.message}`));
    }
  }
}
