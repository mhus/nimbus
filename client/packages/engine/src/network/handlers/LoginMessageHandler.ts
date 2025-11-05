/**
 * LoginMessageHandler - Handles login response messages
 *
 * Processes login responses from server and updates AppContext
 * with world information.
 */

import {
  ResponseMessage,
  MessageType,
  LoginResponseData,
  LoginErrorData,
  getLogger,
} from '@nimbus/shared';
import { MessageHandler } from '../MessageHandler';
import type { AppContext } from '../../AppContext';
import type { NetworkService } from '../../services/NetworkService';

const logger = getLogger('LoginMessageHandler');

/**
 * Handles LOGIN_RESPONSE messages from server
 */
export class LoginMessageHandler extends MessageHandler<LoginResponseData | LoginErrorData> {
  readonly messageType = MessageType.LOGIN_RESPONSE;

  constructor(
    private appContext: AppContext,
    private networkService: NetworkService
  ) {
    super();
  }

  handle(message: ResponseMessage<LoginResponseData | LoginErrorData>): void {
    const data = message.d;

    if (!data) {
      logger.error('Login response has no data');
      this.networkService.emit('login:error', new Error('No data in login response'));
      return;
    }

    if (data.success) {
      // Success response
      const successData = data as LoginResponseData;

      // Debug log the response structure
      logger.debug('Login response data', { data: successData });

      // Validate worldInfo exists
      if (!successData.worldInfo) {
        const error = 'Server error: Login response missing worldInfo. Please check server configuration.';
        logger.error(error, { data: successData });
        this.networkService.emit('login:error', new Error(error));
        return;
      }

      // Update AppContext with WorldInfo and sessionId from server
      this.appContext.worldInfo = successData.worldInfo;
      this.appContext.sessionId = successData.sessionId;

      logger.info('Login successful', {
        userId: successData.userId,
        displayName: successData.displayName,
        worldId: successData.worldInfo.worldId,
        worldName: successData.worldInfo.name,
        sessionId: successData.sessionId,
      });

      // Log complete WorldInfo for debugging
      logger.info('Received WorldInfo from server', {
        worldInfo: successData.worldInfo,
      });

      // Emit event for other services
      this.networkService.emit('login:success', successData);
    } else {
      // Error response
      const errorData = data as LoginErrorData;

      logger.error('Login failed', {
        errorCode: errorData.errorCode,
        errorMessage: errorData.errorMessage,
      });

      // Emit error event
      this.networkService.emit('login:error', new Error(errorData.errorMessage));
    }
  }
}
