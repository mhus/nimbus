/**
 * CommandService - Manages command registration and execution
 *
 * The CommandService is a singleton service that:
 * - Registers command handlers
 * - Executes commands by name
 * - Exposes commands to browser console (EDITOR mode only)
 */

import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { CommandHandler } from '../commands/CommandHandler';

const logger = getLogger('CommandService');

/**
 * CommandService - Central service for command management
 */
export class CommandService {
  private appContext: AppContext;
  private handlers: Map<string, CommandHandler> = new Map();

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
}
