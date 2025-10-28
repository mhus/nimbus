/**
 * Login messages
 */

import type { RequestMessage, ResponseMessage } from '../BaseMessage';
import type { ClientType } from '../MessageTypes';
import type { Vector3 } from '../../types/Vector3';

/**
 * World information
 */
export interface WorldInfo {
  worldId: string;
  name: string;
  description?: string;
  start: Vector3;
  stop: Vector3;
  chunkSize: number;
  assetPath: string;
  assetPort?: number;
  worldGroupId?: string;
  status: number;
  createdAt: string;
  updatedAt: string;
  owner: {
    user: string;
    displayName: string;
    email?: string;
  };
  settings: {
    maxPlayers: number;
    allowGuests: boolean;
    pvpEnabled: boolean;
    pingInterval: number;
  };
  license?: {
    type: string;
    expiresAt?: string;
  };
  startArea?: {
    x: number;
    y: number;
    z: number;
    radius: number;
    rotation: number;
  };
}

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
