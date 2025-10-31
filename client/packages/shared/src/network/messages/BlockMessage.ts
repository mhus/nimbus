/**
 * Block-related messages
 */

import type { BaseMessage } from '../BaseMessage';
import type { Block } from '../../types/Block';
import type { AnimationData } from '../../types/AnimationData';

/**
 * Block update (Server -> Client)
 * Server sends block changes to client
 */
export type BlockUpdateMessage = BaseMessage<Block[]>;

/**
 * Block status update data
 */
export interface BlockStatusUpdate {
  /** Block X position */
  x: number;

  /** Block Y position */
  y: number;

  /** Block Z position */
  z: number;

  /** New status value */
  s: number;

  /** Optional animations before status change */
  aa?: AnimationData[];

  /** Optional animations after status change */
  ab?: AnimationData[];
}

/**
 * Block status update (Server -> Client)
 * Server sends block status changes (e.g., for animations, effects)
 */
export type BlockStatusUpdateMessage = BaseMessage<BlockStatusUpdate[]>;
