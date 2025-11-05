/**
 * NotificationService - Manages notification display across 4 areas
 *
 * Four areas:
 * - System (bottom-left): Info, errors, command results (auto-hide 5s, max 5)
 * - Chat (bottom-right): Chat messages (no auto-hide, max 5, clearable)
 * - Overlay (center): Big messages (auto-hide 2s, max 1)
 * - Quest (top-right): Quest info (no auto-hide, max 2)
 */

import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type {
  Notification,
  NotificationType,
  NotificationAreaConfig,
} from '../types/Notification';
import {
  NotificationArea,
  NOTIFICATION_AREA_CONFIGS,
  NOTIFICATION_TYPE_TO_AREA,
  NOTIFICATION_TYPE_STYLES,
} from '../types/Notification';

const logger = getLogger('NotificationService');

/**
 * NotificationService - Manages notifications across different areas
 */
export class NotificationService {
  private appContext: AppContext;
  private notifications: Map<NotificationArea, Notification[]> = new Map();
  private nextNotificationId: number = 1;
  private hideTimers: Map<string, number> = new Map();
  private visible: boolean = true;

  // Container elements
  private containers: Map<NotificationArea, HTMLElement> = new Map();

  constructor(appContext: AppContext) {
    this.appContext = appContext;

    // Initialize notification storage
    this.notifications.set(NotificationArea.SYSTEM, []);
    this.notifications.set(NotificationArea.CHAT, []);
    this.notifications.set(NotificationArea.OVERLAY, []);
    this.notifications.set(NotificationArea.QUEST, []);

    // Get container elements
    this.initializeContainers();

    logger.info('NotificationService initialized');
  }

  /**
   * Initialize container references
   */
  private initializeContainers(): void {
    try {
      const systemContainer = document.getElementById('notifications-system');
      const chatContainer = document.getElementById('notifications-chat');
      const overlayContainer = document.getElementById('notifications-overlay');
      const questContainer = document.getElementById('notifications-quest');

      if (!systemContainer || !chatContainer || !overlayContainer || !questContainer) {
        throw new Error('Notification containers not found in DOM');
      }

      this.containers.set(NotificationArea.SYSTEM, systemContainer);
      this.containers.set(NotificationArea.CHAT, chatContainer);
      this.containers.set(NotificationArea.OVERLAY, overlayContainer);
      this.containers.set(NotificationArea.QUEST, questContainer);

      logger.debug('Notification containers initialized');
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'NotificationService.initializeContainers'
      );
    }
  }

  /**
   * Create a new notification
   *
   * @param type Notification type (0-31)
   * @param from Sender name (null for system messages)
   * @param message Notification message
   */
  newNotification(type: NotificationType, from: string | null, message: string): void {
    try {
      // Don't show if notifications are suppressed
      if (!this.visible) {
        logger.debug('Notification suppressed', { type, from, message });
        return;
      }

      // Determine area from type
      const area = NOTIFICATION_TYPE_TO_AREA[type];
      if (!area) {
        logger.warn('Unknown notification type', { type });
        return;
      }

      // Get area config
      const config = NOTIFICATION_AREA_CONFIGS[area];

      // Create notification
      const notification: Notification = {
        id: `notification-${this.nextNotificationId++}`,
        type,
        from,
        message,
        timestamp: Date.now(),
        area,
      };

      // Add to storage
      const notifications = this.notifications.get(area)!;
      notifications.push(notification);

      // Enforce max count
      while (notifications.length > config.maxCount) {
        const removed = notifications.shift()!;
        this.removeNotificationElement(removed);
      }

      // Render notification
      this.renderNotification(notification, config);

      // Setup auto-hide if configured
      if (config.autoHideMs !== null) {
        this.setupAutoHide(notification, config.autoHideMs);
      }

      logger.debug('Notification created', {
        id: notification.id,
        type,
        area,
        from,
      });
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.newNotification', {
        type,
        from,
        message,
      });
    }
  }

  /**
   * Control notification visibility
   *
   * @param visible If false, suppress all notifications
   */
  notificationsVisible(visible: boolean): void {
    try {
      this.visible = visible;

      if (!visible) {
        logger.debug('Notifications suppressed');
      } else {
        logger.debug('Notifications enabled');
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.notificationsVisible', {
        visible,
      });
    }
  }

  /**
   * Clear all chat notifications
   */
  clearChatNotifications(): void {
    try {
      const chatNotifications = this.notifications.get(NotificationArea.CHAT)!;

      // Remove all chat notification elements
      chatNotifications.forEach((notification) => {
        this.removeNotificationElement(notification);
      });

      // Clear storage
      chatNotifications.length = 0;

      logger.debug('Chat notifications cleared');
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.clearChatNotifications');
    }
  }

  /**
   * Render a notification to the DOM
   */
  private renderNotification(
    notification: Notification,
    config: NotificationAreaConfig
  ): void {
    try {
      const container = this.containers.get(notification.area);
      if (!container) {
        logger.warn('Container not found for area', { area: notification.area });
        return;
      }

      // Get style config
      const style = NOTIFICATION_TYPE_STYLES[notification.type];

      // Create notification element
      const element = document.createElement('div');
      element.className = `notification color-${style.color} size-${style.size}`;
      element.id = notification.id;

      // Add from line if present
      if (notification.from) {
        const fromElement = document.createElement('div');
        fromElement.className = 'notification-from';
        fromElement.textContent = notification.from;
        element.appendChild(fromElement);
      }

      // Add message
      const messageElement = document.createElement('div');
      messageElement.className = 'notification-message';
      messageElement.textContent = notification.message;
      element.appendChild(messageElement);

      // Add to container
      container.appendChild(element);

      logger.debug('Notification rendered', { id: notification.id });
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.renderNotification', {
        notification,
      });
    }
  }

  /**
   * Setup auto-hide timer for notification
   */
  private setupAutoHide(notification: Notification, delayMs: number): void {
    try {
      const timerId = window.setTimeout(() => {
        this.hideNotification(notification);
      }, delayMs);

      this.hideTimers.set(notification.id, timerId);

      logger.debug('Auto-hide timer set', { id: notification.id, delayMs });
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.setupAutoHide', {
        notification,
        delayMs,
      });
    }
  }

  /**
   * Hide a notification with animation
   */
  private hideNotification(notification: Notification): void {
    try {
      const element = document.getElementById(notification.id);
      if (!element) {
        return;
      }

      // Add hiding class for animation
      element.classList.add('hiding');

      // Remove after animation completes
      setTimeout(() => {
        this.removeNotificationElement(notification);
      }, 300); // Match CSS animation duration

      logger.debug('Notification hidden', { id: notification.id });
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.hideNotification', {
        notification,
      });
    }
  }

  /**
   * Remove notification element from DOM and storage
   */
  private removeNotificationElement(notification: Notification): void {
    try {
      // Clear timer if exists
      const timerId = this.hideTimers.get(notification.id);
      if (timerId !== undefined) {
        window.clearTimeout(timerId);
        this.hideTimers.delete(notification.id);
      }

      // Remove from DOM
      const element = document.getElementById(notification.id);
      if (element && element.parentNode) {
        element.parentNode.removeChild(element);
      }

      // Remove from storage
      const notifications = this.notifications.get(notification.area);
      if (notifications) {
        const index = notifications.findIndex((n) => n.id === notification.id);
        if (index !== -1) {
          notifications.splice(index, 1);
        }
      }

      logger.debug('Notification removed', { id: notification.id });
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.removeNotificationElement', {
        notification,
      });
    }
  }

  /**
   * Dispose service and clean up
   */
  dispose(): void {
    try {
      // Clear all timers
      this.hideTimers.forEach((timerId) => {
        window.clearTimeout(timerId);
      });
      this.hideTimers.clear();

      // Clear all notifications
      this.notifications.forEach((notifications, area) => {
        notifications.forEach((notification) => {
          this.removeNotificationElement(notification);
        });
        notifications.length = 0;
      });

      logger.info('NotificationService disposed');
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.dispose');
    }
  }
}
