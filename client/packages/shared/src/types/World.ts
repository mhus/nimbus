/**
 * World - World information and configuration
 */

import type { Vector3 } from './Vector3';

/**
 * World information
 * Contains all metadata and configuration for a world
 *
 * Only worldId and name are required. All other fields are optional
 * to allow flexible usage across different contexts (server, client, editor).
 */
export interface WorldInfo {
  /** Unique world identifier (required) */
  worldId: string;

  /** World display name (required) */
  name: string;

  /** World description */
  description?: string;

  /** Start position (min coordinates) */
  start?: Vector3;

  /** Stop position (max coordinates) */
  stop?: Vector3;

  /** Chunk size (default: 16) */
  chunkSize?: number;

  /** Asset path for resources */
  assetPath?: string;

  /** Asset server port */
  assetPort?: number;

  /** World group identifier */
  worldGroupId?: string;

  /** World status (0=active, 1=inactive, etc.) */
  status?: number;

  /** Creation timestamp */
  createdAt?: string;

  /** Last update timestamp */
  updatedAt?: string;

  /** World owner information */
  owner?: {
    user: string;
    displayName: string;
    email?: string;
  };

  /** World settings */
  settings?: {
    maxPlayers: number;
    allowGuests: boolean;
    pvpEnabled: boolean;
    pingInterval: number;

    /** Audio settings */
    /** Ambient music when player dies (optional) */
    deadAmbientAudio?: string;

    /** Step sound when swimming (optional, default: 'audio/liquid/swim1.ogg') */
    swimStepAudio?: string;
  };

  /** License information */
  license?: {
    type: string;
    expiresAt?: string;
  };

  /** Start area for new players */
  startArea?: {
    x: number;
    y: number;
    z: number;
    radius: number;
    rotation: number;
  };

  /** Editor URL for block editing */
  editorUrl?: string;
}
