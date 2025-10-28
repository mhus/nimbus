/**
 * Login messages
 */

import type { RequestMessage, ResponseMessage } from '../BaseMessage';
import type { ClientType } from '../MessageTypes';
import type { WorldInfo } from '../../types/World';

/**
 * Login request with username/password
 */
export interface LoginRequestData {
  username?: string;
  password?: string;
  token?: string;
  worldId: string;
  clientType: ClientType;
  sessionId?: string;
}

/**
 * Login response data (success)
 */
export interface LoginResponseData {
  success: true;
  userId: string;
  displayName: string;
  worldInfo: WorldInfo;
  sessionId: string;
}

/**
 * Login response data (failure)
 */
export interface LoginErrorData {
  success: false;
  errorCode: number;
  errorMessage: string;
}

/**
 * Login request message
 */
export type LoginMessage = RequestMessage<LoginRequestData>;

/**
 * Login response message
 */
export type LoginResponseMessage = ResponseMessage<
  LoginResponseData | LoginErrorData
>;
