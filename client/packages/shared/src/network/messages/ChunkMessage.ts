/**
 * Chunk-related messages
 *
 * Chunks are always columns with X and Z coordinates.
 * Y-direction is always delivered and rendered completely.
 */

import type { BaseMessage } from '../BaseMessage';
import type { Block } from '../../types/Block';
import type { AreaData } from '../../types/AreaData';
import type { HeightData } from '../../types/ChunkData';
import type { Backdrop } from '../../types/Backdrop';

/**
 * Chunk coordinates (XZ only, Y is complete column)
 */
export interface ChunkCoordinate {
  cx: number;
  cz: number;
}

/**
 * Chunk data transfer object
 */
export interface ChunkDataTransferObject {
  /** Chunk X coordinate */
  cx: number;

  /** Chunk Z coordinate */
  cz: number;

  /** Block data */
  b: Block[];

  /** Height data, maximum height */
  h?: HeightData[];

  /** Area data with effects */
  a?: AreaData[];

  /** Backdrop data for chunk edges */
  backdrop?: {
    /** North side backdrop items */
    n?: Array<Backdrop>;
    /** East side backdrop items */
    e?: Array<Backdrop>;
    /** South side backdrop items */
    s?: Array<Backdrop>;
    /** West side backdrop items */
    w?: Array<Backdrop>;
  };
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
