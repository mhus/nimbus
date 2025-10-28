/**
 * Chunk-related messages
 *
 * Chunks are always columns with X and Z coordinates.
 * Y-direction is always delivered and rendered completely.
 */

import type { BaseMessage } from '../BaseMessage';
import type { Block } from '../../types/Block';
import type { AreaData } from '../../types/AreaData';
import type { EntityData } from '../../types/EntityData';

/**
 * Chunk coordinates (XZ only, Y is complete column)
 */
export interface ChunkCoordinate {
  x: number;
  z: number;
}

/**
 * Height data for chunk
 * [maxHeight, minHeight, groundLevel, waterHeight]
 */
export type HeightData = [number, number, number, number];

/**
 * Chunk data transfer object
 */
export interface ChunkDataTransferObject {
  /** Chunk X coordinate */
  c: number;

  /** Chunk Z coordinate */
  z: number;

  /** Block data */
  b: Block[];

  /** Height data */
  h: HeightData[];

  /** Area data with effects */
  a?: AreaData[];

  /** Entity data */
  e?: EntityData[];
}

/**
 * Chunk registration (Client -> Server)
 * Client registers chunks it wants to receive
 * All non-listed chunks will no longer be sent by server
 */
export interface ChunkRegisterData {
  c: ChunkCoordinate[];
}

export type ChunkRegisterMessage = BaseMessage<ChunkRegisterData>;

/**
 * Chunk query/request (Client -> Server)
 * Client explicitly requests specific chunks
 */
export interface ChunkQueryData {
  c: ChunkCoordinate[];
}

export type ChunkQueryMessage = BaseMessage<ChunkQueryData>;

/**
 * Chunk update (Server -> Client)
 * Server sends requested or updated chunks
 */
export type ChunkUpdateMessage = BaseMessage<ChunkDataTransferObject[]>;
