/**
 * World - World information and configuration
 */

import type { Vector3 } from './Vector3';

/**
 * World information
 * Contains all metadata and configuration for a world
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
