/**
 * Modal types and interfaces
 */

/**
 * Modal size configuration
 */
export interface ModalSize {
  /** Width in CSS units (px, %, em, etc.) */
  width: string;

  /** Height in CSS units (px, %, em, etc.) */
  height: string;
}

/**
 * Modal position configuration
 */
export interface ModalPosition {
  /** Horizontal position ('center' or CSS value like '100px', '10%') */
  x: string | 'center';

  /** Vertical position ('center' or CSS value like '50px', '5%') */
  y: string | 'center';
}

/**
 * Size presets for common modal sizes
 */
export type ModalSizePreset = 'small' | 'medium' | 'large';

/**
 * Modal options
 */
export interface ModalOptions {
  /** Modal size (preset or custom) */
  size?: ModalSizePreset | ModalSize;

  /** Modal position ('center' or custom coordinates) */
  position?: 'center' | ModalPosition;

  /** Close modal when clicking backdrop (default: true) */
  closeOnBackdrop?: boolean;

  /** Close modal when pressing ESC key (default: true) */
  closeOnEsc?: boolean;
}

/**
 * Modal reference returned from openModal()
 */
export interface ModalReference {
  /** Unique modal ID */
  id: string;

  /** Root DOM element of the modal */
  element: HTMLElement;

  /** Close this modal */
  close: () => void;
}
