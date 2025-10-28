/**
 * Notification messages
 */

import type { BaseMessage } from '../BaseMessage';
import { NotificationType } from '../MessageTypes';

/**
 * Notification data
 */
export interface NotificationData {
  /** Notification type (numeric) */
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

/**
 * Helper to get notification type name
 */
export function getNotificationTypeName(type: NotificationType): string {
  const names: Record<NotificationType, string> = {
    [NotificationType.SYSTEM]: 'system',
    [NotificationType.CHAT]: 'chat',
    [NotificationType.WARNING]: 'warning',
    [NotificationType.ERROR]: 'error',
    [NotificationType.INFO]: 'info',
  };
  return names[type] ?? 'unknown';
}
