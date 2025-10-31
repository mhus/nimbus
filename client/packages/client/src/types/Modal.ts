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
 * Size and position preset (combined)
 */
export interface ModalSizePositionPreset {
  /** Size configuration */
  size: ModalSize;

  /** Position configuration */
  position: ModalPosition;
}

/**
 * Size presets for common modal sizes and positions
 */
export enum ModalSizePreset {
  /** Left side panel (50% width, full height) */
  LEFT = 'left',

  /** Right side panel (50% width, full height) */
  RIGHT = 'right',

  /** Top panel (full width, 50% height) */
  TOP = 'top',

  /** Bottom panel (full width, 50% height) */
  BOTTOM = 'bottom',

  /** Center small (600x400px) */
  CENTER_SMALL = 'center_small',

  /** Center medium (800x600px) */
  CENTER_MEDIUM = 'center_medium',

  /** Center large (90vw x 90vh) */
  CENTER_LARGE = 'center_large',

  /** Top-left quadrant (50% x 50%) */
  LEFT_TOP = 'left_top',

  /** Bottom-left quadrant (50% x 50%) */
  LEFT_BOTTOM = 'left_bottom',

  /** Top-right quadrant (50% x 50%) */
  RIGHT_TOP = 'right_top',

  /** Bottom-right quadrant (50% x 50%) */
  RIGHT_BOTTOM = 'right_bottom',
}

/**
 * Modal flags (bitflags for options)
 */
export enum ModalFlags {
  /** No flags */
  NONE = 0,

  /** Modal can be closed by user */
  CLOSEABLE = 1 << 0, // 1

  /** Minimal borders (no title, no close button, thin border) */
  NO_BORDERS = 1 << 1, // 2

  /** Show break-out button to open in new window */
  BREAK_OUT = 1 << 2, // 4
}

/**
 * Modal options
 */
export interface ModalOptions {
  /** Modal size (preset or custom) */
  size?: ModalSizePreset | ModalSize;

  /** Modal position ('center' or custom coordinates) */
  position?: 'center' | ModalPosition;

  /** Modal behavior flags (bitflags) */
  flags?: number;

  /** Close modal when clicking backdrop (default: true, overridden by flags) */
  closeOnBackdrop?: boolean;

  /** Close modal when pressing ESC key (default: true, overridden by flags) */
  closeOnEsc?: boolean;

  /** Reference key for this modal (reuse existing modal with same key) */
  referenceKey?: string;
}

/**
 * Modal reference returned from openModal()
 */
export interface ModalReference {
  /** Unique modal ID */
  id: string;

  /** Reference key (if provided) */
  referenceKey?: string;

  /** Root DOM element of the modal */
  element: HTMLElement;

  /** IFrame element */
  iframe: HTMLIFrameElement;

  /** Close this modal */
  close: (reason?: string) => void;

  /** Change position of this modal */
  changePosition: (preset: ModalSizePreset) => void;
}

/**
 * Message types for IFrame <-> Parent communication
 */
export enum IFrameMessageType {
  /** IFrame is ready */
  IFRAME_READY = 'IFRAME_READY',

  /** IFrame requests to be closed */
  REQUEST_CLOSE = 'REQUEST_CLOSE',

  /** IFrame requests position change */
  REQUEST_POSITION_CHANGE = 'REQUEST_POSITION_CHANGE',

  /** IFrame sends notification */
  NOTIFICATION = 'NOTIFICATION',
}

/**
 * IFrame message from child to parent
 */
export type IFrameMessageFromChild =
  | { type: IFrameMessageType.IFRAME_READY }
  | { type: IFrameMessageType.REQUEST_CLOSE; reason?: string }
  | { type: IFrameMessageType.REQUEST_POSITION_CHANGE; preset: ModalSizePreset }
  | {
      type: IFrameMessageType.NOTIFICATION;
      notificationType: string;
      from: string;
      message: string;
    };
