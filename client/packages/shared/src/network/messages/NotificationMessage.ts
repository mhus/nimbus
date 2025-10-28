/**
 * Notification messages
 */

import type { BaseMessage } from '../BaseMessage';
import type { NotificationType } from '../MessageTypes';

/**
 * Notification data
 */
export interface NotificationData {
  /** Notification type */
  t: NotificationType;

  /** From (optional, e.g., for chat messages) */
  f?: string;

  /** Message text */
  m: string;

  /** UTC timestamp */
  ts: number;
}

/**
 * Server notification (Server -> Client)
 * Server sends notifications (system messages, chat, warnings, etc.)
 */
export type NotificationMessage = BaseMessage<NotificationData>;
