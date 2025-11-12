/**
 * Server-specific types
 */

import type WebSocket from 'ws';
import type { ServerChunk } from './ServerChunk';
import type { WorldGenerator } from '../world/generators/WorldGenerator';
import type { WorldInfo, EditAction, EntityPathway } from '@nimbus/shared';

/**
 * Client session data
 */
export interface ClientSession {
  /** Unique session ID */
  sessionId: string;

  /** User ID (after login) */
  userId?: string;

  /** Username */
  username?: string;

  /** Display name */
  displayName?: string;

  /** Current world ID */
  worldId?: string;

  /** WebSocket connection */
  ws: WebSocket;

  /** Registered chunk coordinates (cx, cz) */
  registeredChunks: Set<string>;

  /** Last ping timestamp */
  lastPingAt: number;

  /** Session created timestamp */
  createdAt: number;

  /** Is authenticated */
  isAuthenticated: boolean;

  /** Selected edit block position (for editing operations) */
  selectedEditBlock?: { x: number; y: number; z: number } | null;

  /** Marked edit block position (for copy/move operations) */
  markedEditBlock?: { x: number; y: number; z: number } | null;

  /** Current edit action (determines behavior when setting selectedEditBlock) */
  editAction?: EditAction;

  /** Entity pathway queue (pathways to send to client) */
  entityPathwayQueue: EntityPathway[];
}

/**
 * Chunk key helper
 */
export function getChunkKey(cx: number, cz: number): string {
  return `${cx},${cz}`;
}

/**
 * Parse chunk key
 */
export function parseChunkKey(key: string): { cx: number; cz: number } {
  const [cx, cz] = key.split(',').map(Number);
  return { cx, cz };
}

/**
 * World instance data
 */
export interface WorldInstance {
  /** World ID */
  worldId: string;

  /** World name */
  name: string;

  /** World description */
  description?: string;

  /** Chunk size */
  chunkSize: number;

  /** World dimensions */
  dimensions: {
    minX: number;
    maxX: number;
    minY: number;
    maxY: number;
    minZ: number;
    maxZ: number;
  };

  /** Sea level */
  seaLevel: number;

  /** Ground level */
  groundLevel: number;

  /** World status */
  status: number;

  /** Loaded chunks */
  chunks: Map<string, ServerChunk>;

  /** Terrain generator for this world */
  generator?: WorldGenerator;

  /** Created timestamp */
  createdAt: string;

  /** Updated timestamp */
  updatedAt: string;

  /** Shared WorldInfo for client transmission */
  worldInfo: WorldInfo;
}
