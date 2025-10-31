/**
 * NotificationCommand - Creates notifications in the NotificationService
 *
 * Usage: notification <type> <from> <message>
 * Example: notification 0 null "System message"
 * Example: notification 11 "Player1" "Hello!"
 */

import { CommandHandler } from './CommandHandler';
import type { AppContext } from '../AppContext';
import type { NotificationType } from '../types/Notification';

/**
 * Notification command - Creates notifications
 */
export class NotificationCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'notification';
  }

  description(): string {
    return 'Creates a notification (notification <type> <from> <message>)';
  }

  execute(parameters: string[]): any {
    const notificationService = this.appContext.services.notification;

    if (!notificationService) {
      console.error('NotificationService not available');
      return { error: 'NotificationService not available' };
    }

    if (parameters.length < 3) {
      console.error('Usage: notification <type> <from> <message>');
      console.error('  type: 0-31 (0=system, 10-12=chat, 20-21=overlay, 30-31=quest)');
      console.error('  from: sender name or "null" for no sender');
      console.error('  message: notification message');
      console.error('');
      console.error('Examples:');
      console.error('  notification 0 null "System info"');
      console.error('  notification 11 "Player1" "Hello everyone!"');
      console.error('  notification 20 null "LEVEL UP!"');
      return { error: 'Invalid arguments' };
    }

    // Parse type
    const type = parseInt(parameters[0], 10);
    if (isNaN(type) || type < 0 || type > 31) {
      console.error('Invalid type: must be a number between 0 and 31');
      return { error: 'Invalid type' };
    }

    // Parse from (null or string)
    const fromParam = parameters[1];
    const from = fromParam.toLowerCase() === 'null' ? null : fromParam;

    // Parse message (join remaining parameters)
    const message = parameters.slice(2).join(' ');

    // Create notification
    try {
      notificationService.newNotification(type as NotificationType, from, message);

      const result = `Notification created: type=${type}, from=${from || 'null'}, message="${message}"`;
      console.log(`✓ ${result}`);
      return result;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error(`✗ Failed to create notification: ${errorMessage}`);
      throw error;
    }
  }
}
