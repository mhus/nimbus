/**
 * PingMessageHandler - Handles ping/pong keep-alive
 *
 * Sends regular ping messages to server and handles pong responses
 * to maintain connection.
 */

import {
  BaseMessage,
  RequestMessage,
  MessageType,
  getLogger,
  ExceptionHandler,
} from '@nimbus/shared';
import { MessageHandler } from '../MessageHandler';
import type { AppContext } from '../../AppContext';
import type { NetworkService } from '../../services/NetworkService';

const logger = getLogger('PingMessageHandler');

/**
 * Handles ping/pong keep-alive mechanism
 */
export class PingMessageHandler extends MessageHandler {
  readonly messageType = MessageType.PING;

  private pingInterval?: NodeJS.Timeout;
  private lastPongAt: number = 0;
  private pingIntervalSeconds: number = 30; // Default 30 seconds

  constructor(
    private networkService: NetworkService,
    _appContext: AppContext
  ) {
    super();
  }

  /**
   * Start ping interval based on world settings
   */
  startPingInterval(intervalSeconds: number): void {
    this.stopPingInterval();

    this.pingIntervalSeconds = intervalSeconds;

    logger.info('Starting ping interval', { intervalSeconds });

    this.pingInterval = setInterval(() => {
      this.sendPing();
    }, intervalSeconds * 1000);

    // Send initial ping
    this.sendPing();
  }

  /**
   * Stop ping interval
   */
  stopPingInterval(): void {
    if (this.pingInterval) {
      clearInterval(this.pingInterval);
      this.pingInterval = undefined;
      logger.debug('Ping interval stopped');
    }
  }

  /**
   * Send ping message to server
   */
  private sendPing(): void {
    try {
      if (!this.networkService.isConnected()) {
        logger.debug('Not connected, skipping ping');
        return;
      }

      const pingMsg: RequestMessage<void> = {
        i: this.networkService.generateMessageId(),
        t: MessageType.PING,
      };

      this.networkService.send(pingMsg);

      logger.debug('Ping sent');

      // Check for timeout (no pong received for 2x ping interval)
      if (this.lastPongAt > 0) {
        const timeSinceLastPong = Date.now() - this.lastPongAt;
        const timeoutThreshold = this.pingIntervalSeconds * 2000;

        if (timeSinceLastPong > timeoutThreshold) {
          logger.warn('Ping timeout - no pong received', {
            timeSinceLastPong,
            timeoutThreshold,
          });
        }
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'PingMessageHandler.sendPing');
    }
  }

  /**
   * Handle pong response from server
   */
  handle(_message: BaseMessage): void {
    this.lastPongAt = Date.now();
    logger.debug('Pong received');
  }
}
