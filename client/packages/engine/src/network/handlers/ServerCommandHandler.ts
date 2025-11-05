/**
 * ServerCommandHandler - Handles server command requests
 *
 * Message type: "scmd"
 * Direction: Server -> Client
 *
 * Handles server commands (scmd) by executing them via CommandService
 * and sending back the result (scmd.rs).
 */

import {
  ServerCommandMessage,
  MessageType,
  getLogger,
  ExceptionHandler,
} from '@nimbus/shared';
import type { MessageHandler } from '../MessageHandler';
import type { CommandService } from '../../services/CommandService';

const logger = getLogger('ServerCommandHandler');

/**
 * Handler for server command messages (scmd)
 */
export class ServerCommandHandler implements MessageHandler {
  readonly messageType = MessageType.SCMD;

  constructor(private commandService: CommandService) {
    logger.debug('ServerCommandHandler created');
  }

  handle(message: ServerCommandMessage): void {
    try {
      if (!message.i) {
        logger.error('Received scmd without request ID (i)');
        return;
      }

      if (!message.d) {
        logger.error('Received scmd without data');
        return;
      }

      const { cmd, args } = message.d;

      if (!cmd) {
        logger.error('Received scmd without cmd field');
        return;
      }

      logger.debug('Received server command', {
        requestId: message.i,
        cmd,
        args,
      });

      // Route to CommandService for execution
      this.commandService.handleServerCommand(message.i, cmd, args || []);
    } catch (error) {
      ExceptionHandler.handle(error, 'ServerCommandHandler.handle', { message });
    }
  }
}
