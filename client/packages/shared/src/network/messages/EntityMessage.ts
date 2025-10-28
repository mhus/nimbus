/**
 * Entity-related messages
 */

import type { BaseMessage } from '../BaseMessage';
import type { EntityData } from '../../types/EntityData';

/**
 * Entity update (Server -> Client)
 * Server sends entity changes to client
 */
export type EntityUpdateMessage = BaseMessage<EntityData[]>;
