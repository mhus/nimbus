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

  // ============================================
  // Shortcut UI Display
  // ============================================

  /** Shortcut display modes */
  private shortcutModes = ['keys', 'clicks', 'slots0', 'slots1', 'off'] as const;
  private currentShortcutMode: typeof this.shortcutModes[number] = 'off';
  private shortcutContainer: HTMLElement | null = null;

  /**
   * Toggle shortcuts display
   *
   * Cycles through: keys -> clicks -> slots0 -> slots1 (if available) -> off
   */
  toggleShowShortcuts(): void {
    try {
      const currentIndex = this.shortcutModes.indexOf(this.currentShortcutMode);
      let nextIndex = (currentIndex + 1) % this.shortcutModes.length;

      // Skip slots1 if no shortcuts in that range
      if (this.shortcutModes[nextIndex] === 'slots1') {
        if (!this.hasShortcutsInRange(10, 19)) {
          nextIndex = (nextIndex + 1) % this.shortcutModes.length;
        }
      }

      this.currentShortcutMode = this.shortcutModes[nextIndex];
      this.updateShortcutDisplay();

      logger.debug('Shortcut display toggled', { mode: this.currentShortcutMode });
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.toggleShowShortcuts');
    }
  }

  /**
   * Check if shortcuts exist in range
   */
  private hasShortcutsInRange(start: number, end: number): boolean {
    const playerInfo = this.appContext.playerInfo;
    if (!playerInfo?.shortcuts) return false;

    for (let i = start; i <= end; i++) {
      if (playerInfo.shortcuts[`slot${i}`]) {
        return true;
      }
    }
    return false;
  }

  /**
   * Update shortcut display
   */
  private async updateShortcutDisplay(): Promise<void> {
    try {
      // Remove existing display
      if (this.shortcutContainer) {
        this.shortcutContainer.remove();
        this.shortcutContainer = null;
      }

      // If mode is 'off', don't create display
      if (this.currentShortcutMode === 'off') {
        return;
      }

      // Create shortcut container
      this.shortcutContainer = document.createElement('div');
      this.shortcutContainer.id = 'shortcuts-container';
      this.shortcutContainer.style.cssText = `
        position: fixed;
        bottom: 80px;
        left: 50%;
        transform: translateX(-50%);
        display: flex;
        gap: 8px;
        padding: 8px;
        background: rgba(0, 0, 0, 0.7);
        border-radius: 8px;
        z-index: 900;
        font-family: 'Courier New', monospace;
        font-size: 12px;
        color: white;
      `;

      // Get shortcuts to display
      const shortcuts = this.getShortcutsForMode(this.currentShortcutMode);

      // Create slots
      for (const [key, shortcut] of Object.entries(shortcuts)) {
        const slot = await this.createShortcutSlot(key, shortcut);
        this.shortcutContainer.appendChild(slot);
      }

      document.body.appendChild(this.shortcutContainer);
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.updateShortcutDisplay');
    }
  }

  /**
   * Get shortcuts for current mode
   */
  private getShortcutsForMode(mode: typeof this.shortcutModes[number]): Record<string, any> {
    const playerInfo = this.appContext.playerInfo;
    if (!playerInfo?.shortcuts) return {};

    const shortcuts: Record<string, any> = {};

    switch (mode) {
      case 'keys':
        // Keys 1-0
        for (let i = 1; i <= 9; i++) {
          shortcuts[`key${i}`] = playerInfo.shortcuts[`key${i}`] || null;
        }
        shortcuts['key0'] = playerInfo.shortcuts['key0'] || null;
        break;

      case 'clicks':
        // Clicks 1-3
        shortcuts['click1'] = playerInfo.shortcuts['click1'] || null;
        shortcuts['click2'] = playerInfo.shortcuts['click2'] || null;
        shortcuts['click3'] = playerInfo.shortcuts['click3'] || null;
        break;

      case 'slots0':
        // Slots 0-9
        for (let i = 0; i <= 9; i++) {
          shortcuts[`slot${i}`] = playerInfo.shortcuts[`slot${i}`] || null;
        }
        break;

      case 'slots1':
        // Slots 10-19
        for (let i = 10; i <= 19; i++) {
          shortcuts[`slot${i}`] = playerInfo.shortcuts[`slot${i}`] || null;
        }
        break;
    }

    return shortcuts;
  }

  /**
   * Create shortcut slot element
   */
  private async createShortcutSlot(key: string, shortcut: any): Promise<HTMLElement> {
    const slot = document.createElement('div');
    slot.style.cssText = `
      width: 48px;
      height: 48px;
      background: rgba(50, 50, 50, 0.9);
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      cursor: pointer;
    `;

    // Add key label
    const label = document.createElement('div');
    label.style.cssText = `
      position: absolute;
      top: 2px;
      right: 4px;
      font-size: 10px;
      color: rgba(255, 255, 255, 0.7);
    `;
    label.textContent = this.formatKeyLabel(key);
    slot.appendChild(label);

    if (shortcut) {
      // Load item and display texture
      await this.loadShortcutItem(slot, shortcut);

      // Add hover tooltip
      this.addShortcutTooltip(slot, shortcut);
    }

    return slot;
  }

  /**
   * Format key label for display
   */
  private formatKeyLabel(key: string): string {
    if (key.startsWith('key')) {
      return key.replace('key', '');
    }
    if (key.startsWith('click')) {
      return 'C' + key.replace('click', '');
    }
    if (key.startsWith('slot')) {
      return 'S' + key.replace('slot', '');
    }
    return key;
  }

  /**
   * Load item for shortcut and display texture
   */
  private async loadShortcutItem(slot: HTMLElement, shortcut: any): Promise<void> {
    try {
      const itemService = this.appContext.services.item;
      if (!itemService) {
        logger.warn('ItemService not available');
        return;
      }

      // Get item ID from shortcut definition
      const itemId = shortcut.itemId || shortcut.id;
      if (!itemId) {
        logger.debug('No itemId in shortcut', { shortcut });
        return;
      }

      // Load item from server
      const item = await itemService.getItem(itemId);
      if (!item) {
        logger.debug('Item not found', { itemId });
        return;
      }

      // Get texture URL
      const textureUrl = itemService.getTextureUrl(item);
      if (!textureUrl) {
        logger.debug('No texture for item', { itemId });
        return;
      }

      // Create image element
      const img = document.createElement('img');
      img.src = textureUrl;
      img.style.cssText = `
        width: 36px;
        height: 36px;
        object-fit: contain;
        image-rendering: pixelated;
      `;
      img.onerror = () => {
        logger.warn('Failed to load item texture', { textureUrl });
      };

      slot.appendChild(img);
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.loadShortcutItem', { shortcut });
    }
  }

  /**
   * Add tooltip to shortcut slot
   */
  private addShortcutTooltip(slot: HTMLElement, shortcut: any): void {
    let tooltip: HTMLElement | null = null;

    slot.addEventListener('mouseenter', () => {
      // Create tooltip
      tooltip = document.createElement('div');
      tooltip.style.cssText = `
        position: absolute;
        bottom: 100%;
        left: 50%;
        transform: translateX(-50%);
        margin-bottom: 8px;
        padding: 8px 12px;
        background: rgba(0, 0, 0, 0.95);
        border: 1px solid rgba(255, 255, 255, 0.3);
        border-radius: 4px;
        white-space: nowrap;
        pointer-events: none;
        z-index: 1000;
      `;

      // Add title
      const title = document.createElement('div');
      title.style.cssText = 'font-weight: bold; margin-bottom: 4px;';
      title.textContent = shortcut.name || shortcut.displayName || 'Unnamed';
      tooltip.appendChild(title);

      // Add description if available
      if (shortcut.description) {
        const desc = document.createElement('div');
        desc.style.cssText = 'font-size: 11px; color: rgba(255, 255, 255, 0.8);';
        desc.textContent = shortcut.description;
        tooltip.appendChild(desc);
      }

      slot.appendChild(tooltip);
    });

    slot.addEventListener('mouseleave', () => {
      if (tooltip) {
        tooltip.remove();
        tooltip = null;
      }
    });
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

      // Remove shortcut container
      if (this.shortcutContainer) {
        this.shortcutContainer.remove();
        this.shortcutContainer = null;
      }

      logger.info('NotificationService disposed');
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.dispose');
    }
  }
}
