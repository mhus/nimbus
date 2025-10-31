/**
 * ModalService - Manages HTML IFrame modals
 *
 * Provides modal dialogs with IFrame content, configurable size and position.
 */

import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type {
  ModalOptions,
  ModalReference,
  ModalSize,
  ModalPosition,
  ModalSizePositionPreset,
  IFrameMessageFromChild,
} from '../types/Modal';
import { ModalFlags, IFrameMessageType, ModalSizePreset } from '../types/Modal';

const logger = getLogger('ModalService');

/**
 * Default modal size and position presets
 * Margins: 20px on all sides
 */
const SIZE_PRESETS: Record<ModalSizePreset, ModalSizePositionPreset> = {
  // Side panels (full height, with margins)
  [ModalSizePreset.LEFT]: {
    size: { width: 'calc(50% - 30px)', height: 'calc(100vh - 40px)' },
    position: { x: '20px', y: '20px' },
  },
  [ModalSizePreset.RIGHT]: {
    size: { width: 'calc(50% - 30px)', height: 'calc(100vh - 40px)' },
    position: { x: 'calc(50% + 10px)', y: '20px' },
  },

  // Top/Bottom panels (full width, with margins)
  [ModalSizePreset.TOP]: {
    size: { width: 'calc(100vw - 40px)', height: 'calc(50% - 30px)' },
    position: { x: '20px', y: '20px' },
  },
  [ModalSizePreset.BOTTOM]: {
    size: { width: 'calc(100vw - 40px)', height: 'calc(50% - 30px)' },
    position: { x: '20px', y: 'calc(50% + 10px)' },
  },

  // Center (small, medium, large)
  [ModalSizePreset.CENTER_SMALL]: {
    size: { width: '600px', height: '400px' },
    position: { x: 'center', y: 'center' },
  },
  [ModalSizePreset.CENTER_MEDIUM]: {
    size: { width: '800px', height: '600px' },
    position: { x: 'center', y: 'center' },
  },
  [ModalSizePreset.CENTER_LARGE]: {
    size: { width: '90vw', height: '90vh' },
    position: { x: 'center', y: 'center' },
  },

  // Quadrants (with margins)
  [ModalSizePreset.LEFT_TOP]: {
    size: { width: 'calc(50% - 30px)', height: 'calc(50% - 30px)' },
    position: { x: '20px', y: '20px' },
  },
  [ModalSizePreset.LEFT_BOTTOM]: {
    size: { width: 'calc(50% - 30px)', height: 'calc(50% - 30px)' },
    position: { x: '20px', y: 'calc(50% + 10px)' },
  },
  [ModalSizePreset.RIGHT_TOP]: {
    size: { width: 'calc(50% - 30px)', height: 'calc(50% - 30px)' },
    position: { x: 'calc(50% + 10px)', y: '20px' },
  },
  [ModalSizePreset.RIGHT_BOTTOM]: {
    size: { width: 'calc(50% - 30px)', height: 'calc(50% - 30px)' },
    position: { x: 'calc(50% + 10px)', y: 'calc(50% + 10px)' },
  },
};

/**
 * ModalService - Manages modal dialogs with IFrame content
 *
 * Features:
 * - IFrame-based modals for displaying websites
 * - Configurable size (presets or custom)
 * - Configurable position (center or absolute)
 * - Close via X button, ESC key, or backdrop click
 * - Multiple modals support with z-index management
 * - Reference key support for reusing modals
 * - Bitflags for options (CLOSEABLE, NO_BORDERS, BREAK_OUT)
 * - PostMessage communication with IFrame content
 */
export class ModalService {
  private appContext: AppContext;
  private modals: Map<string, ModalReference> = new Map();
  private modalsByReferenceKey: Map<string, ModalReference> = new Map();
  private nextModalId: number = 1;
  private baseZIndex: number = 10000;
  private messageHandler: ((event: MessageEvent<IFrameMessageFromChild>) => void) | null = null;

  constructor(appContext: AppContext) {
    this.appContext = appContext;

    // Setup postMessage listener for IFrame communication
    this.setupPostMessageListener();

    logger.info('ModalService initialized');
  }

  /**
   * Open a modal with IFrame content
   *
   * @param referenceKey Reference key for reusing modals (optional)
   * @param title Modal title
   * @param url URL to load in IFrame
   * @param preset Size/position preset (optional, defaults to 'center_medium')
   * @param flags Behavior flags (optional, bitflags: CLOSEABLE, NO_BORDERS, BREAK_OUT)
   * @returns Modal reference for closing
   */
  openModal(
    referenceKey: string | null,
    title: string,
    url: string,
    preset?: ModalSizePreset,
    flags: number = ModalFlags.CLOSEABLE
  ): ModalReference {
    try {
      // Check if modal with this reference key already exists
      if (referenceKey) {
        const existingModal = this.modalsByReferenceKey.get(referenceKey);
        if (existingModal) {
          // Update existing modal with new URL
          logger.debug('Reusing existing modal', { referenceKey, url });
          this.updateModalURL(existingModal, url);
          return existingModal;
        }
      }

      // Generate unique modal ID
      const id = `modal-${this.nextModalId++}`;

      // Use preset or default
      const sizePreset = preset ?? ModalSizePreset.CENTER_MEDIUM;

      // Add embedded=true to URL
      const embeddedUrl = this.addEmbeddedParameter(url, true);

      // Create modal DOM structure
      const { backdrop, modalContainer, iframe } = this.createModalDOM(
        id,
        title,
        embeddedUrl,
        sizePreset,
        flags
      );

      // Create modal reference
      const modalRef: ModalReference = {
        id,
        referenceKey: referenceKey ?? undefined,
        element: backdrop,
        iframe,
        close: (reason?: string) => this.closeModal(modalRef, reason),
        changePosition: (newPreset: ModalSizePreset) =>
          this.changeModalPosition(modalRef, newPreset),
      };

      // Store reference
      this.modals.set(id, modalRef);
      if (referenceKey) {
        this.modalsByReferenceKey.set(referenceKey, modalRef);
      }

      // Setup event handlers
      this.setupEventHandlers(modalRef, flags);

      // Add to DOM
      document.body.appendChild(backdrop);

      logger.debug('Modal opened', { id, referenceKey, title, url, preset: sizePreset, flags });

      return modalRef;
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'ModalService.openModal', {
        referenceKey,
        title,
        url,
        preset,
        flags,
      });
    }
  }

  /**
   * Close a specific modal
   *
   * @param ref Modal reference to close
   * @param reason Optional reason for closing
   */
  closeModal(ref: ModalReference, reason?: string): void {
    try {
      // Remove from DOM
      if (ref.element.parentNode) {
        ref.element.parentNode.removeChild(ref.element);
      }

      // Remove from tracking
      this.modals.delete(ref.id);
      if (ref.referenceKey) {
        this.modalsByReferenceKey.delete(ref.referenceKey);
      }

      // Cleanup event listener
      const escHandler = (ref as any)._escHandler;
      if (escHandler) {
        document.removeEventListener('keydown', escHandler);
        delete (ref as any)._escHandler;
      }

      logger.debug('Modal closed', { id: ref.id, referenceKey: ref.referenceKey, reason });
    } catch (error) {
      ExceptionHandler.handle(error, 'ModalService.closeModal', { ref, reason });
    }
  }

  /**
   * Close all open modals
   */
  closeAll(): void {
    try {
      const modalRefs = Array.from(this.modals.values());

      modalRefs.forEach((ref) => {
        this.closeModal(ref);
      });

      logger.debug('All modals closed', { count: modalRefs.length });
    } catch (error) {
      ExceptionHandler.handle(error, 'ModalService.closeAll');
    }
  }

  /**
   * Get count of currently open modals
   */
  getOpenModalCount(): number {
    return this.modals.size;
  }

  /**
   * Create modal DOM structure
   */
  private createModalDOM(
    id: string,
    title: string,
    url: string,
    preset: ModalSizePreset,
    flags: number
  ): { backdrop: HTMLElement; modalContainer: HTMLElement; iframe: HTMLIFrameElement } {
    // Calculate z-index (each modal gets higher z-index)
    const zIndex = this.baseZIndex + this.modals.size * 2;

    // Get preset configuration
    const presetConfig = SIZE_PRESETS[preset];
    const size = presetConfig.size;
    const position = presetConfig.position;

    // Check flags
    const isCloseable = (flags & ModalFlags.CLOSEABLE) !== 0;
    const noBorders = (flags & ModalFlags.NO_BORDERS) !== 0;
    const hasBreakOut = (flags & ModalFlags.BREAK_OUT) !== 0;

    // Create backdrop
    const backdrop = document.createElement('div');
    backdrop.className = 'nimbus-modal-backdrop';
    backdrop.id = `${id}-backdrop`;
    backdrop.style.zIndex = zIndex.toString();

    // Determine if centered
    const isCentered = position.x === 'center' && position.y === 'center';
    if (isCentered) {
      backdrop.classList.add('nimbus-modal-centered');
    }

    // Create modal container
    const modalContainer = document.createElement('div');
    modalContainer.className = 'nimbus-modal-container';
    modalContainer.id = id;

    // Apply NO_BORDERS styling
    if (noBorders) {
      modalContainer.classList.add('nimbus-modal-no-borders');
    }

    // Apply size
    modalContainer.style.width = size.width;
    modalContainer.style.height = size.height;

    // Apply position
    this.applyPosition(modalContainer, position);

    // Create header (unless NO_BORDERS)
    if (!noBorders) {
      const header = document.createElement('div');
      header.className = 'nimbus-modal-header';

      // Title
      const titleElement = document.createElement('h2');
      titleElement.className = 'nimbus-modal-title';
      titleElement.textContent = title;
      header.appendChild(titleElement);

      // Buttons container
      const buttonsContainer = document.createElement('div');
      buttonsContainer.className = 'nimbus-modal-buttons';

      // Break-out button (if enabled)
      if (hasBreakOut) {
        const breakOutButton = document.createElement('button');
        breakOutButton.className = 'nimbus-modal-breakout';
        breakOutButton.innerHTML = '&#x2197;'; // ↗ arrow
        breakOutButton.setAttribute('aria-label', 'Open in new window');
        breakOutButton.setAttribute('data-url', url);
        buttonsContainer.appendChild(breakOutButton);
      }

      // Close button (if closeable)
      if (isCloseable) {
        const closeButton = document.createElement('button');
        closeButton.className = 'nimbus-modal-close';
        closeButton.innerHTML = '&times;';
        closeButton.setAttribute('aria-label', 'Close modal');
        buttonsContainer.appendChild(closeButton);
      }

      header.appendChild(buttonsContainer);
      modalContainer.appendChild(header);
    }

    // Create IFrame
    const iframe = document.createElement('iframe');
    iframe.className = 'nimbus-modal-iframe';
    iframe.src = url;
    iframe.setAttribute('sandbox', 'allow-same-origin allow-scripts allow-forms allow-popups');
    iframe.setAttribute('data-modal-id', id);

    // Assemble structure
    modalContainer.appendChild(iframe);
    backdrop.appendChild(modalContainer);

    return { backdrop, modalContainer, iframe };
  }

  /**
   * Apply position to modal container
   */
  private applyPosition(element: HTMLElement, position: ModalPosition): void {
    const xCentered = position.x === 'center';
    const yCentered = position.y === 'center';

    if (xCentered && yCentered) {
      // Both centered - use flex centering (handled by backdrop)
      element.style.position = 'relative';
    } else if (xCentered) {
      // Only X centered
      element.style.position = 'absolute';
      element.style.left = '50%';
      element.style.top = position.y;
      element.style.transform = 'translateX(-50%)';
    } else if (yCentered) {
      // Only Y centered
      element.style.position = 'absolute';
      element.style.left = position.x;
      element.style.top = '50%';
      element.style.transform = 'translateY(-50%)';
    } else {
      // Both absolute
      element.style.position = 'absolute';
      element.style.left = position.x;
      element.style.top = position.y;
    }
  }

  /**
   * Setup event handlers for modal
   */
  private setupEventHandlers(modalRef: ModalReference, flags: number): void {
    const backdrop = modalRef.element;
    const isCloseable = (flags & ModalFlags.CLOSEABLE) !== 0;
    const hasBreakOut = (flags & ModalFlags.BREAK_OUT) !== 0;

    // Close button click
    const closeButton = backdrop.querySelector('.nimbus-modal-close');
    if (closeButton && isCloseable) {
      closeButton.addEventListener('click', () => {
        modalRef.close('user_closed');
      });
    }

    // Break-out button click
    const breakOutButton = backdrop.querySelector('.nimbus-modal-breakout');
    if (breakOutButton && hasBreakOut) {
      breakOutButton.addEventListener('click', () => {
        const url = breakOutButton.getAttribute('data-url');
        if (url) {
          this.breakOutModal(modalRef, url);
        }
      });
    }

    // Backdrop click (only close if closeable)
    if (isCloseable) {
      backdrop.addEventListener('click', (e) => {
        // Only close if clicking directly on backdrop, not on modal content
        if (e.target === backdrop) {
          modalRef.close('backdrop_click');
        }
      });
    }

    // ESC key (only if closeable)
    if (isCloseable) {
      const escHandler = (e: KeyboardEvent) => {
        if (e.key === 'Escape') {
          // Only close top-most modal
          const topModal = this.getTopModal();
          if (topModal && topModal.id === modalRef.id) {
            modalRef.close('escape_key');
          }
        }
      };

      document.addEventListener('keydown', escHandler);

      // Store handler for cleanup
      (modalRef as any)._escHandler = escHandler;
    }
  }

  /**
   * Get top-most (highest z-index) modal
   */
  private getTopModal(): ModalReference | null {
    const modals = Array.from(this.modals.values());
    if (modals.length === 0) return null;

    // Last modal in the map is the most recent one
    return modals[modals.length - 1];
  }

  /**
   * Add embedded parameter to URL
   */
  private addEmbeddedParameter(url: string, embedded: boolean): string {
    try {
      const urlObj = new URL(url, window.location.href);
      urlObj.searchParams.set('embedded', embedded.toString());
      return urlObj.toString();
    } catch (error) {
      // If URL parsing fails, append as query string
      const separator = url.includes('?') ? '&' : '?';
      return `${url}${separator}embedded=${embedded}`;
    }
  }

  /**
   * Update modal URL (for reusing existing modals)
   */
  private updateModalURL(modalRef: ModalReference, url: string): void {
    try {
      const embeddedUrl = this.addEmbeddedParameter(url, true);
      modalRef.iframe.src = embeddedUrl;

      // Update break-out button URL if present
      const breakOutButton = modalRef.element.querySelector('.nimbus-modal-breakout');
      if (breakOutButton) {
        breakOutButton.setAttribute('data-url', embeddedUrl);
      }

      logger.debug('Modal URL updated', { id: modalRef.id, url: embeddedUrl });
    } catch (error) {
      ExceptionHandler.handle(error, 'ModalService.updateModalURL', { modalRef, url });
    }
  }

  /**
   * Change modal position to a new preset
   */
  private changeModalPosition(modalRef: ModalReference, preset: ModalSizePreset): void {
    try {
      const presetConfig = SIZE_PRESETS[preset];
      const size = presetConfig.size;
      const position = presetConfig.position;

      // Find modal container
      const modalContainer = modalRef.element.querySelector('.nimbus-modal-container') as HTMLElement;
      if (!modalContainer) {
        logger.warn('Modal container not found', { id: modalRef.id });
        return;
      }

      // Apply new size
      modalContainer.style.width = size.width;
      modalContainer.style.height = size.height;

      // Apply new position
      this.applyPosition(modalContainer, position);

      // Update backdrop centering class
      const isCentered = position.x === 'center' && position.y === 'center';
      if (isCentered) {
        modalRef.element.classList.add('nimbus-modal-centered');
      } else {
        modalRef.element.classList.remove('nimbus-modal-centered');
      }

      logger.debug('Modal position changed', { id: modalRef.id, preset });
    } catch (error) {
      ExceptionHandler.handle(error, 'ModalService.changeModalPosition', { modalRef, preset });
    }
  }

  /**
   * Break out modal to new window
   */
  private breakOutModal(modalRef: ModalReference, url: string): void {
    try {
      // Open URL in new window with embedded=false
      const breakOutUrl = this.addEmbeddedParameter(url, false);
      window.open(breakOutUrl, '_blank');

      // Close this modal
      modalRef.close('break_out');

      logger.debug('Modal broken out', { id: modalRef.id, url: breakOutUrl });
    } catch (error) {
      ExceptionHandler.handle(error, 'ModalService.breakOutModal', { modalRef, url });
    }
  }

  /**
   * Setup postMessage listener for IFrame communication
   */
  private setupPostMessageListener(): void {
    this.messageHandler = (event: MessageEvent<IFrameMessageFromChild>) => {
      try {
        // Security: Check if message is from one of our iframes
        const iframe = Array.from(this.modals.values())
          .map((ref) => ref.iframe)
          .find((iframe) => iframe.contentWindow === event.source);

        if (!iframe) {
          // Message not from our iframe, ignore
          return;
        }

        // Get modal reference
        const modalId = iframe.getAttribute('data-modal-id');
        if (!modalId) {
          logger.warn('IFrame missing data-modal-id attribute');
          return;
        }

        const modalRef = this.modals.get(modalId);
        if (!modalRef) {
          logger.warn('Modal not found for IFrame message', { modalId });
          return;
        }

        // Handle message
        this.handleIFrameMessage(modalRef, event.data);
      } catch (error) {
        ExceptionHandler.handle(error, 'ModalService.messageHandler', { event });
      }
    };

    window.addEventListener('message', this.messageHandler);
  }

  /**
   * Handle IFrame message
   */
  private handleIFrameMessage(modalRef: ModalReference, message: IFrameMessageFromChild): void {
    try {
      switch (message.type) {
        case IFrameMessageType.IFRAME_READY:
          logger.debug('IFrame ready', { id: modalRef.id });
          break;

        case IFrameMessageType.REQUEST_CLOSE:
          logger.debug('IFrame requests close', { id: modalRef.id, reason: message.reason });
          modalRef.close(message.reason ?? 'iframe_request');
          break;

        case IFrameMessageType.REQUEST_POSITION_CHANGE:
          logger.debug('IFrame requests position change', {
            id: modalRef.id,
            preset: message.preset,
          });
          modalRef.changePosition(message.preset);
          break;

        case IFrameMessageType.NOTIFICATION:
          logger.debug('IFrame notification', {
            id: modalRef.id,
            notificationType: message.notificationType,
            from: message.from,
            message: message.message,
          });

          // Forward to NotificationService if available
          if (this.appContext.services?.notification) {
            // Parse notificationType string to NotificationType enum
            const notificationType =
              parseInt(message.notificationType, 10) || 0; // Default to SYSTEM_INFO
            this.appContext.services.notification.newNotification(
              notificationType,
              message.from,
              message.message
            );
          }
          break;

        default:
          logger.warn('Unknown IFrame message type', { message });
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'ModalService.handleIFrameMessage', { modalRef, message });
    }
  }

  /**
   * Dispose service and close all modals
   */
  dispose(): void {
    try {
      this.closeAll();

      // Remove postMessage listener
      if (this.messageHandler) {
        window.removeEventListener('message', this.messageHandler);
        this.messageHandler = null;
      }

      // Remove any remaining event listeners
      this.modals.forEach((modalRef) => {
        const escHandler = (modalRef as any)._escHandler;
        if (escHandler) {
          document.removeEventListener('keydown', escHandler);
        }
      });

      this.modals.clear();
      this.modalsByReferenceKey.clear();

      logger.info('ModalService disposed');
    } catch (error) {
      ExceptionHandler.handle(error, 'ModalService.dispose');
    }
  }
}

/**
 * Export SIZE_PRESETS for external use
 */
export { SIZE_PRESETS };
