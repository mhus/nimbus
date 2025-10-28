/**
 * Ping/Pong messages for connection keepalive and latency measurement
 */

import type { BaseMessage } from '../BaseMessage';

/**
 * Ping message (Client -> Server)
 * Server responds with same message ID
 *
 * Timeout: pingInterval + 10 seconds buffer
 * deadline = lastPingAt + pingInterval*1000 + 10000
 */
export interface PingMessage extends BaseMessage<undefined> {
  i: string;
}

/**
 * Pong message (Server -> Client)
 * Response to ping with same message ID in 'r' field
 */
export interface PongMessage extends BaseMessage<undefined> {
  r: string;
}
