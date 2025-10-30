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
  ModalSizePreset,
} from '../types/Modal';

const logger = getLogger('ModalService');

/**
 * Default modal size presets
 */
const SIZE_PRESETS: Record<ModalSizePreset, ModalSize> = {
  small: { width: '600px', height: '400px' },
  medium: { width: '800px', height: '600px' },
  large: { width: '90%', height: '90%' },
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
 */
export class ModalService {
  private appContext: AppContext;
  private modals: Map<string, ModalReference> = new Map();
  private nextModalId: number = 1;
  private baseZIndex: number = 10000;

  constructor(appContext: AppContext) {
    this.appContext = appContext;

    logger.info('ModalService initialized');
  }

  /**
   * Open a modal with IFrame content
   *
   * @param title Modal title
   * @param url URL to load in IFrame
   * @param options Modal options (size, position, behavior)
   * @returns Modal reference for closing
   */
  openModal(title: string, url: string, options?: ModalOptions): ModalReference {
    try {
      // Generate unique modal ID
      const id = `modal-${this.nextModalId++}`;

      // Resolve options with defaults
      const resolvedOptions: Required<ModalOptions> = {
        size: options?.size ?? 'medium',
        position: options?.position ?? 'center',
        closeOnBackdrop: options?.closeOnBackdrop ?? true,
        closeOnEsc: options?.closeOnEsc ?? true,
      };

      // Create modal DOM structure
      const { backdrop, modalContainer } = this.createModalDOM(
        id,
        title,
        url,
        resolvedOptions
      );

      // Create modal reference
      const modalRef: ModalReference = {
        id,
        element: backdrop,
        close: () => this.closeModal(modalRef),
      };

      // Store reference
      this.modals.set(id, modalRef);

      // Setup event handlers
      this.setupEventHandlers(modalRef, resolvedOptions);

      // Add to DOM
      document.body.appendChild(backdrop);

      logger.debug('Modal opened', { id, title, url, options: resolvedOptions });

      return modalRef;
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'ModalService.openModal', {
        title,
        url,
        options,
      });
    }
  }

  /**
   * Close a specific modal
   *
   * @param ref Modal reference to close
   */
  closeModal(ref: ModalReference): void {
    try {
      // Remove from DOM
      if (ref.element.parentNode) {
        ref.element.parentNode.removeChild(ref.element);
      }

      // Remove from tracking
      this.modals.delete(ref.id);

      logger.debug('Modal closed', { id: ref.id });
    } catch (error) {
      ExceptionHandler.handle(error, 'ModalService.closeModal', { ref });
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
    options: Required<ModalOptions>
  ): { backdrop: HTMLElement; modalContainer: HTMLElement } {
    // Calculate z-index (each modal gets higher z-index)
    const zIndex = this.baseZIndex + this.modals.size * 2;

    // Create backdrop
    const backdrop = document.createElement('div');
    backdrop.className = 'nimbus-modal-backdrop';
    backdrop.id = `${id}-backdrop`;
    backdrop.style.zIndex = zIndex.toString();

    // Determine if centered or absolute positioning
    const isCentered = options.position === 'center';

    if (isCentered) {
      backdrop.classList.add('nimbus-modal-centered');
    }

    // Create modal container
    const modalContainer = document.createElement('div');
    modalContainer.className = 'nimbus-modal-container';
    modalContainer.id = id;

    // Apply size
    const size = this.resolveSize(options.size);
    modalContainer.style.width = size.width;
    modalContainer.style.height = size.height;

    // Apply position (only for non-centered modals)
    if (!isCentered) {
      const position = options.position as ModalPosition;
      this.applyPosition(modalContainer, position);
    }

    // Create header
    const header = document.createElement('div');
    header.className = 'nimbus-modal-header';

    // Title
    const titleElement = document.createElement('h2');
    titleElement.className = 'nimbus-modal-title';
    titleElement.textContent = title;

    // Close button
    const closeButton = document.createElement('button');
    closeButton.className = 'nimbus-modal-close';
    closeButton.innerHTML = '&times;';
    closeButton.setAttribute('aria-label', 'Close modal');

    header.appendChild(titleElement);
    header.appendChild(closeButton);

    // Create IFrame
    const iframe = document.createElement('iframe');
    iframe.className = 'nimbus-modal-iframe';
    iframe.src = url;
    iframe.setAttribute('sandbox', 'allow-same-origin allow-scripts allow-forms allow-popups');

    // Assemble structure
    modalContainer.appendChild(header);
    modalContainer.appendChild(iframe);
    backdrop.appendChild(modalContainer);

    return { backdrop, modalContainer };
  }

  /**
   * Resolve size from preset or custom
   */
  private resolveSize(size: ModalSizePreset | ModalSize): ModalSize {
    if (typeof size === 'string') {
      return SIZE_PRESETS[size];
    }
    return size;
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
  private setupEventHandlers(
    modalRef: ModalReference,
    options: Required<ModalOptions>
  ): void {
    const backdrop = modalRef.element;

    // Close button click
    const closeButton = backdrop.querySelector('.nimbus-modal-close');
    if (closeButton) {
      closeButton.addEventListener('click', () => {
        modalRef.close();
      });
    }

    // Backdrop click (close if enabled)
    if (options.closeOnBackdrop) {
      backdrop.addEventListener('click', (e) => {
        // Only close if clicking directly on backdrop, not on modal content
        if (e.target === backdrop) {
          modalRef.close();
        }
      });
    }

    // ESC key (close if enabled)
    if (options.closeOnEsc) {
      const escHandler = (e: KeyboardEvent) => {
        if (e.key === 'Escape') {
          // Only close top-most modal
          const topModal = this.getTopModal();
          if (topModal && topModal.id === modalRef.id) {
            modalRef.close();
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
   * Dispose service and close all modals
   */
  dispose(): void {
    try {
      this.closeAll();

      // Remove any remaining event listeners
      this.modals.forEach((modalRef) => {
        const escHandler = (modalRef as any)._escHandler;
        if (escHandler) {
          document.removeEventListener('keydown', escHandler);
        }
      });

      this.modals.clear();

      logger.info('ModalService disposed');
    } catch (error) {
      ExceptionHandler.handle(error, 'ModalService.dispose');
    }
  }
}
