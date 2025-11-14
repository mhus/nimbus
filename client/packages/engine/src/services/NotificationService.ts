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
   * Initialize event subscriptions
   * Called after PlayerService is available
   */
  initializeEventSubscriptions(): void {
    const playerService = this.appContext.services.player;
    if (!playerService) {
      logger.warn('PlayerService not available for event subscriptions');
      return;
    }

    // Subscribe to PlayerInfo updates to refresh shortcut display
    playerService.on('playerInfo:updated', () => {
      // Refresh shortcut display if currently visible
      if (this.currentShortcutMode !== 'off') {
        this.updateShortcutDisplay();
      }
    });

    // Subscribe to shortcut highlight events
    playerService.on('shortcut:highlight', (shortcutKey: string) => {
      this.highlightShortcut(shortcutKey);
    });

    // Subscribe to status effects changes
    playerService.on('statusEffects:changed', (effects: any[]) => {
      this.updateStatusEffectsDisplay(effects);
    });

    // Subscribe to vitals changes
    playerService.on('vitals:changed', (vitals: any[]) => {
      this.updateVitalsDisplay(vitals);
    });

    logger.debug('NotificationService event subscriptions initialized');
  }

  /**
   * Highlight a shortcut slot
   *
   * Switches to the appropriate mode if needed and briefly highlights the slot.
   * If shortcuts were not visible before, they are hidden again after highlighting.
   * Example: highlightShortcut('click1') switches to 'clicks' mode and highlights click1
   *
   * @param shortcutKey Shortcut key (e.g., 'key1', 'click2', 'slot5')
   */
  private highlightShortcut(shortcutKey: string): void {
    try {
      // Remember if shortcuts were visible before
      const wasVisible = this.currentShortcutMode !== 'off';

      // Determine which mode this shortcut belongs to
      let targetMode: typeof this.shortcutModes[number] = 'off';

      if (shortcutKey.startsWith('key')) {
        targetMode = 'keys';
      } else if (shortcutKey.startsWith('click')) {
        targetMode = 'clicks';
      } else if (shortcutKey.startsWith('slot')) {
        const slotNum = parseInt(shortcutKey.replace('slot', ''), 10);
        if (!isNaN(slotNum)) {
          targetMode = slotNum >= 10 ? 'slots1' : 'slots0';
        }
      }

      // If not currently showing or showing different mode, switch mode
      if (this.currentShortcutMode !== targetMode) {
        this.currentShortcutMode = targetMode;
        this.updateShortcutDisplay().then(() => {
          this.highlightSlotElement(shortcutKey);

          // If was not visible, hide again after highlight duration (1.5s)
          if (!wasVisible) {
            setTimeout(() => {
              this.currentShortcutMode = 'off';
              this.updateShortcutDisplay();
            }, 1500);
          }
        });
      } else {
        // Already showing correct mode, just highlight
        this.highlightSlotElement(shortcutKey);
      }

      logger.debug('Shortcut highlighted', { shortcutKey, mode: targetMode, wasVisible });
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.highlightShortcut', { shortcutKey });
    }
  }

  /**
   * Highlight a specific slot element with animation
   */
  private highlightSlotElement(shortcutKey: string): void {
    if (!this.shortcutContainer) return;

    // Find the slot element by data attribute
    const slots = this.shortcutContainer.querySelectorAll('[data-shortcut-key]');
    for (const slot of Array.from(slots)) {
      if ((slot as HTMLElement).dataset.shortcutKey === shortcutKey) {
        const element = slot as HTMLElement;

        // Store original border
        const originalBorder = element.style.border;

        // Highlight with yellow border
        element.style.border = '2px solid rgba(255, 255, 0, 1)';
        element.style.boxShadow = '0 0 10px rgba(255, 255, 0, 0.8)';

        // Reset after 1 second
        setTimeout(() => {
          element.style.border = originalBorder;
          element.style.boxShadow = '';
        }, 1000);

        break;
      }
    }
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

  /** Status effects display */
  private statusEffectsContainer: HTMLElement | null = null;

  /** Vitals display */
  private vitalsContainer: HTMLElement | null = null;
  private showVitals: boolean = true; // Default: on

  /**
   * Get current shortcut mode
   *
   * @returns Current shortcut display mode
   */
  getCurrentShortcutMode(): typeof this.shortcutModes[number] {
    return this.currentShortcutMode;
  }

  /**
   * Map keyboard number (1-9, 0) to shortcut key based on current display mode
   *
   * @param keyNumber Keyboard number (1-9, 0 maps to index 0-9)
   * @returns Shortcut key to execute (e.g., 'click0', 'slot5', 'key1')
   */
  mapKeyboardNumberToShortcut(keyNumber: number): string | null {
    // If shortcuts not visible, return null (use default key mapping)
    if (this.currentShortcutMode === 'off') {
      return null;
    }

    // Map 1-9, 0 to indices 0-9
    const index = keyNumber === 0 ? 9 : keyNumber - 1;

    switch (this.currentShortcutMode) {
      case 'keys':
        // Default behavior: key1-key9, key0
        return null;

      case 'clicks':
        // Map to click0-9
        return `click${index}`;

      case 'slots0':
        // Map to slot0-9
        return `slot${index}`;

      case 'slots1':
        // Map to slot10-19
        return `slot${10 + index}`;

      default:
        return null;
    }
  }

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
   * Always returns exactly 10 slots (filled or empty)
   */
  private getShortcutsForMode(mode: typeof this.shortcutModes[number]): Record<string, any> {
    const playerInfo = this.appContext.playerInfo;
    const playerShortcuts = playerInfo?.shortcuts || {};

    const shortcuts: Record<string, any> = {};

    switch (mode) {
      case 'keys':
        // Keys 1-0 (always 10 slots)
        for (let i = 1; i <= 9; i++) {
          shortcuts[`key${i}`] = playerShortcuts[`key${i}`] || null;
        }
        shortcuts['key0'] = playerShortcuts['key0'] || null;
        break;

      case 'clicks':
        // Clicks 0-9 (always 10 slots, for mice with multiple buttons)
        for (let i = 0; i <= 9; i++) {
          shortcuts[`click${i}`] = playerShortcuts[`click${i}`] || null;
        }
        break;

      case 'slots0':
        // Slots 0-9 (always 10 slots)
        for (let i = 0; i <= 9; i++) {
          shortcuts[`slot${i}`] = playerShortcuts[`slot${i}`] || null;
        }
        break;

      case 'slots1':
        // Slots 10-19 (always 10 slots)
        for (let i = 10; i <= 19; i++) {
          shortcuts[`slot${i}`] = playerShortcuts[`slot${i}`] || null;
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
    slot.dataset.shortcutKey = key; // Add data attribute for highlighting
    slot.style.cssText = `
      width: 32px;
      height: 32px;
      background: ${shortcut ? 'rgba(50, 50, 50, 0.9)' : 'rgba(0, 0, 0, 0.9)'};
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      cursor: ${shortcut ? 'pointer' : 'default'};
      flex-shrink: 0;
    `;

    // Add key label
    const label = document.createElement('div');
    label.style.cssText = `
      position: absolute;
      top: 1px;
      right: 2px;
      font-size: 9px;
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
    if (key.startsWith('empty')) {
      return ''; // Empty slots have no label
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
        width: 28px;
        height: 28px;
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

  // ============================================
  // Status Effects UI Display
  // ============================================

  /**
   * Update status effects display
   *
   * @param effects Array of active status effects
   */
  private async updateStatusEffectsDisplay(effects: any[]): Promise<void> {
    try {
      // Remove existing display
      if (this.statusEffectsContainer) {
        this.statusEffectsContainer.remove();
        this.statusEffectsContainer = null;
      }

      // If no effects, don't create display
      if (effects.length === 0) {
        return;
      }

      // Create status effects container (above shortcuts)
      this.statusEffectsContainer = document.createElement('div');
      this.statusEffectsContainer.id = 'status-effects-container';
      this.statusEffectsContainer.style.cssText = `
        position: fixed;
        bottom: 130px;
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

      // Create effect slots
      for (const effect of effects) {
        const slot = await this.createStatusEffectSlot(effect);
        this.statusEffectsContainer.appendChild(slot);
      }

      document.body.appendChild(this.statusEffectsContainer);
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.updateStatusEffectsDisplay');
    }
  }

  /**
   * Create status effect slot element
   */
  private async createStatusEffectSlot(effect: any): Promise<HTMLElement> {
    const slot = document.createElement('div');
    slot.style.cssText = `
      width: 32px;
      height: 32px;
      background: rgba(139, 0, 0, 0.9);
      border: 2px solid rgba(255, 0, 0, 0.5);
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      cursor: pointer;
      flex-shrink: 0;
    `;

    // Load item and display texture
    await this.loadStatusEffectItem(slot, effect);

    // Add hover tooltip
    this.addStatusEffectTooltip(slot, effect);

    return slot;
  }

  /**
   * Load item for status effect and display texture
   */
  private async loadStatusEffectItem(slot: HTMLElement, effect: any): Promise<void> {
    try {
      const itemService = this.appContext.services.item;
      if (!itemService) {
        logger.warn('ItemService not available');
        return;
      }

      const itemId = effect.itemId;
      if (!itemId) {
        logger.debug('No itemId in effect', { effect });
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
        width: 28px;
        height: 28px;
        object-fit: contain;
        image-rendering: pixelated;
      `;
      img.onerror = () => {
        logger.warn('Failed to load effect texture', { textureUrl });
      };

      slot.appendChild(img);
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.loadStatusEffectItem', { effect });
    }
  }

  /**
   * Add tooltip to status effect slot
   */
  private async addStatusEffectTooltip(slot: HTMLElement, effect: any): Promise<void> {
    const itemService = this.appContext.services.item;
    if (!itemService) return;

    // Load ItemData for description
    const itemData = await itemService.getItemData(effect.itemId);

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
        border: 1px solid rgba(255, 0, 0, 0.5);
        border-radius: 4px;
        white-space: nowrap;
        pointer-events: none;
        z-index: 1000;
      `;

      // Add title (from ItemData or effect)
      const title = document.createElement('div');
      title.style.cssText = 'font-weight: bold; margin-bottom: 4px; color: rgba(255, 100, 100, 1);';
      title.textContent = itemData?.block.metadata?.displayName || effect.itemId;
      tooltip.appendChild(title);

      // Add description if available
      if (itemData?.description) {
        const desc = document.createElement('div');
        desc.style.cssText = 'font-size: 11px; color: rgba(255, 255, 255, 0.8); margin-bottom: 4px;';
        desc.textContent = itemData.description;
        tooltip.appendChild(desc);
      }

      // Add duration info if available
      if (effect.expiresAt) {
        const remaining = Math.max(0, effect.expiresAt - Date.now());
        const duration = document.createElement('div');
        duration.style.cssText = 'font-size: 10px; color: rgba(255, 200, 200, 0.8);';
        duration.textContent = `${(remaining / 1000).toFixed(1)}s remaining`;
        tooltip.appendChild(duration);
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

  // ============================================
  // Vitals UI Display
  // ============================================

  /**
   * Toggle vitals display
   */
  toggleShowVitals(): void {
    this.showVitals = !this.showVitals;
    const playerService = this.appContext.services.player;
    if (playerService) {
      this.updateVitalsDisplay(playerService.getVitals());
    }
    logger.debug('Vitals display toggled', { showVitals: this.showVitals });
  }

  /**
   * Update vitals display
   *
   * @param vitals Array of vitals data
   */
  private async updateVitalsDisplay(vitals: any[]): Promise<void> {
    try {
      // Remove existing display
      if (this.vitalsContainer) {
        this.vitalsContainer.remove();
        this.vitalsContainer = null;
      }

      // If showVitals is off, don't create display
      if (!this.showVitals) {
        return;
      }

      // Filter vitals that should be displayed (not full)
      const visibleVitals = vitals.filter((vital) => {
        const maxValue = vital.max + (vital.extended || 0);
        return vital.current < maxValue;
      });

      // If no vitals to show, don't create container
      if (visibleVitals.length === 0) {
        return;
      }

      // Sort by order
      visibleVitals.sort((a, b) => a.order - b.order);

      // Create vitals container
      this.vitalsContainer = document.createElement('div');
      this.vitalsContainer.id = 'vitals-container';
      this.vitalsContainer.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        display: flex;
        flex-direction: column;
        gap: 4px;
        z-index: 900;
        font-family: 'Courier New', monospace;
        font-size: 11px;
        color: white;
      `;

      // Create vital bars
      for (const vital of visibleVitals) {
        const bar = this.createVitalBar(vital);
        this.vitalsContainer.appendChild(bar);
      }

      document.body.appendChild(this.vitalsContainer);
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.updateVitalsDisplay');
    }
  }

  /**
   * Create vital bar element
   */
  private createVitalBar(vital: any): HTMLElement {
    const container = document.createElement('div');
    container.style.cssText = `
      position: relative;
      cursor: pointer;
    `;

    // Calculate dimensions
    const baseHeight = 10;
    const extendedPercent = vital.extended ? (vital.extended / vital.max) : 0;
    const totalHeight = Math.min(50, baseHeight + extendedPercent * baseHeight * 5); // Max 50px
    const width = 200;

    // Background bar (max + extended)
    const bgBar = document.createElement('div');
    bgBar.style.cssText = `
      width: ${width}px;
      height: ${totalHeight}px;
      background: rgba(50, 50, 50, 0.9);
      border: 1px solid rgba(255, 255, 255, 0.3);
      border-radius: 3px;
      position: relative;
      overflow: hidden;
    `;

    // Foreground bar (current value)
    const maxValue = vital.max + (vital.extended || 0);
    const fillPercent = maxValue > 0 ? (vital.current / maxValue) * 100 : 0;

    const fgBar = document.createElement('div');
    fgBar.style.cssText = `
      width: ${fillPercent}%;
      height: 100%;
      background: ${vital.color || '#ffffff'};
      transition: width 0.3s ease-out;
    `;
    bgBar.appendChild(fgBar);

    // Extended indicator (if present)
    if (vital.extended && vital.extended > 0) {
      const extendedBar = document.createElement('div');
      const extendedStartPercent = (vital.max / maxValue) * 100;
      extendedBar.style.cssText = `
        position: absolute;
        left: ${extendedStartPercent}%;
        top: 0;
        width: ${100 - extendedStartPercent}%;
        height: 100%;
        background: rgba(255, 255, 255, 0.2);
        border-left: 1px dashed rgba(255, 255, 255, 0.5);
      `;
      bgBar.appendChild(extendedBar);
    }

    // Value text overlay
    const text = document.createElement('div');
    text.style.cssText = `
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      font-size: 10px;
      font-weight: bold;
      color: white;
      text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8);
      pointer-events: none;
    `;
    text.textContent = `${Math.floor(vital.current)}/${vital.max}${vital.extended ? `+${vital.extended}` : ''}`;
    bgBar.appendChild(text);

    container.appendChild(bgBar);

    // Add tooltip
    this.addVitalTooltip(container, vital);

    return container;
  }

  /**
   * Add tooltip to vital bar
   */
  private addVitalTooltip(container: HTMLElement, vital: any): void {
    let tooltip: HTMLElement | null = null;

    container.addEventListener('mouseenter', () => {
      // Create tooltip
      tooltip = document.createElement('div');
      tooltip.style.cssText = `
        position: absolute;
        top: 0;
        right: 100%;
        margin-right: 8px;
        padding: 8px 12px;
        background: rgba(0, 0, 0, 0.95);
        border: 1px solid rgba(255, 255, 255, 0.3);
        border-radius: 4px;
        white-space: nowrap;
        pointer-events: none;
        z-index: 1000;
      `;

      // Title
      const title = document.createElement('div');
      title.style.cssText = 'font-weight: bold; margin-bottom: 4px;';
      title.textContent = vital.name || vital.type;
      tooltip.appendChild(title);

      // Current/Max
      const values = document.createElement('div');
      values.style.cssText = 'font-size: 11px; margin-bottom: 2px;';
      values.textContent = `${Math.floor(vital.current)} / ${vital.max}${vital.extended ? ` (+${vital.extended})` : ''}`;
      tooltip.appendChild(values);

      // Regen/Degen
      if (vital.regenRate > 0) {
        const regen = document.createElement('div');
        regen.style.cssText = 'font-size: 10px; color: rgba(100, 255, 100, 0.8);';
        regen.textContent = `+${vital.regenRate.toFixed(1)}/s`;
        tooltip.appendChild(regen);
      }

      if (vital.degenRate > 0) {
        const degen = document.createElement('div');
        degen.style.cssText = 'font-size: 10px; color: rgba(255, 100, 100, 0.8);';
        degen.textContent = `-${vital.degenRate.toFixed(1)}/s`;
        tooltip.appendChild(degen);
      }

      // Extended expiry
      if (vital.extendExpiry) {
        const remaining = Math.max(0, vital.extendExpiry - Date.now());
        const expiry = document.createElement('div');
        expiry.style.cssText = 'font-size: 9px; color: rgba(255, 255, 100, 0.8); margin-top: 2px;';
        expiry.textContent = `Extend expires in ${(remaining / 1000).toFixed(0)}s`;
        tooltip.appendChild(expiry);
      }

      container.appendChild(tooltip);
    });

    container.addEventListener('mouseleave', () => {
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

      // Remove status effects container
      if (this.statusEffectsContainer) {
        this.statusEffectsContainer.remove();
        this.statusEffectsContainer = null;
      }

      // Remove vitals container
      if (this.vitalsContainer) {
        this.vitalsContainer.remove();
        this.vitalsContainer = null;
      }

      logger.info('NotificationService disposed');
    } catch (error) {
      ExceptionHandler.handle(error, 'NotificationService.dispose');
    }
  }
}
