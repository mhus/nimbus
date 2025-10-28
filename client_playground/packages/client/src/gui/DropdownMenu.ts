/**
 * Dropdown Menu Component for Babylon.js GUI
 * Uses a modal dialog with scrollable button options
 */
import {
  AdvancedDynamicTexture,
  Container,
  Button,
  StackPanel,
  Control,
  Rectangle,
  ScrollViewer
} from '@babylonjs/gui';

export interface DropdownOptions {
  width?: number;
  height?: number;
  color?: string;
  background?: string;
  align?: number;
  valign?: number;
  advancedTexture?: AdvancedDynamicTexture;
}

export class DropdownMenu {
  private advancedTexture: AdvancedDynamicTexture;
  private container: Container;
  private button: Button;
  private modalOverlay: Rectangle;
  private modalDialog: Rectangle;
  private modalScrollViewer: ScrollViewer;
  private optionsPanel: StackPanel;
  private height: string;
  private color: string;
  private background: string;
  private selectedValue: string;
  private onChangeCallback?: (value: string) => void;

  constructor(parentPanel: Container, options: DropdownOptions = {}) {
    if (!options.advancedTexture) {
      throw new Error('DropdownMenu requires advancedTexture in options');
    }
    const width = (options.width || 180) + 'px';
    this.height = (options.height || 40) + 'px';
    this.color = options.color || 'white';
    this.background = options.background || '#333333';
    this.selectedValue = '';
    this.advancedTexture = options.advancedTexture;

    // Container - only needs button height
    this.container = new Container();
    this.container.width = width;
    this.container.height = this.height;
    this.container.verticalAlignment = options.align || Control.VERTICAL_ALIGNMENT_TOP;
    this.container.horizontalAlignment = options.valign || Control.HORIZONTAL_ALIGNMENT_LEFT;

    // Primary button
    this.button = Button.CreateSimpleButton('dropdownButton', 'Please Select');
    this.button.width = 1;
    this.button.height = 1;
    this.button.background = this.background;
    this.button.color = this.color;
    this.button.thickness = 1;
    this.button.fontSize = 13;

    // Create modal overlay (fullscreen dark background)
    this.modalOverlay = new Rectangle();
    this.modalOverlay.width = 1;
    this.modalOverlay.height = 1;
    this.modalOverlay.background = 'black';
    this.modalOverlay.alpha = 0.5; // Semi-transparent
    this.modalOverlay.isVisible = false;
    this.modalOverlay.zIndex = 999; // Behind dialog but above everything else

    // Close modal when clicking overlay
    this.modalOverlay.onPointerClickObservable.add(() => {
      this.closeModal();
    });

    // Create modal dialog (centered)
    this.modalDialog = new Rectangle();
    this.modalDialog.width = '400px';
    this.modalDialog.height = '300px';
    this.modalDialog.background = this.background;
    this.modalDialog.color = this.color;
    this.modalDialog.thickness = 2;
    this.modalDialog.isVisible = false;
    this.modalDialog.zIndex = 1000; // High z-index to appear on top
    this.modalDialog.horizontalAlignment = Control.HORIZONTAL_ALIGNMENT_CENTER;
    this.modalDialog.verticalAlignment = Control.VERTICAL_ALIGNMENT_CENTER;

    // Create ScrollViewer for scrollable options
    this.modalScrollViewer = new ScrollViewer();
    this.modalScrollViewer.width = 1;
    this.modalScrollViewer.height = 1;
    this.modalScrollViewer.thickness = 0;
    this.modalScrollViewer.barSize = 20;
    this.modalScrollViewer.barColor = '#666666';
    this.modalScrollViewer.barBackground = '#222222';

    // Options panel inside ScrollViewer
    this.optionsPanel = new StackPanel();
    this.optionsPanel.width = 1;
    this.optionsPanel.isVertical = true;
    this.optionsPanel.adaptHeightToChildren = true;

    // Add optionsPanel to ScrollViewer
    this.modalScrollViewer.addControl(this.optionsPanel);

    // Add ScrollViewer to modal dialog
    this.modalDialog.addControl(this.modalScrollViewer);

    // Open modal on button click
    this.button.onPointerUpObservable.add(() => {
      this.openModal();
    });

    // Add controls to container
    this.container.addControl(this.button);

    // Add button container to parent panel
    parentPanel.addControl(this.container);

    // Add modal overlay and dialog to advancedTexture (fullscreen layer)
    this.advancedTexture.addControl(this.modalOverlay);
    this.advancedTexture.addControl(this.modalDialog);
  }

  /**
   * Open modal dialog
   */
  private openModal(): void {
    this.modalOverlay.isVisible = true;
    this.modalDialog.isVisible = true;
    console.log(`[DropdownMenu] Modal opened, Options count: ${this.optionsPanel.children.length}`);
  }

  /**
   * Close modal dialog
   */
  private closeModal(): void {
    this.modalOverlay.isVisible = false;
    this.modalDialog.isVisible = false;
    console.log('[DropdownMenu] Modal closed');
  }

  /**
   * Set the button text
   */
  setText(text: string): void {
    if (this.button.textBlock) {
      this.button.textBlock.text = text;
    }
  }

  /**
   * Get current selected value
   */
  getValue(): string {
    return this.selectedValue;
  }

  /**
   * Set selected value and update button text
   */
  setValue(value: string): void {
    this.selectedValue = value;
    this.setText(value);
  }

  /**
   * Set change callback
   */
  onChange(callback: (value: string) => void): void {
    this.onChangeCallback = callback;
  }

  /**
   * Add option to dropdown
   */
  addOption(text: string, callback?: () => void): void {
    const button = Button.CreateSimpleButton('option_' + text, text);
    button.width = 1; // Full width
    button.height = this.height;
    button.paddingTop = '5px';
    button.paddingBottom = '5px';
    button.background = this.background;
    button.color = this.color;
    button.alpha = 1.0;
    button.thickness = 1;
    button.fontSize = 13;
    button.horizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
    button.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;

    // Handle option click
    button.onPointerClickObservable.add(() => {
      this.selectedValue = text;
      this.setText(text);
      console.log(`[DropdownMenu] Selected: ${text}`);

      // Close modal
      this.closeModal();

      // Call custom callback if provided
      if (callback) {
        callback();
      }

      // Call onChange callback
      if (this.onChangeCallback) {
        this.onChangeCallback(text);
      }
    });

    this.optionsPanel.addControl(button);
  }

  /**
   * Clear all options
   */
  clearOptions(): void {
    this.optionsPanel.clearControls();
  }

  /**
   * Set options from array
   */
  setOptions(optionsList: string[], selectedValue?: string): void {
    this.clearOptions();

    for (const option of optionsList) {
      this.addOption(option);
    }

    if (selectedValue) {
      this.setValue(selectedValue);
    }
  }

  /**
   * Get container top position
   */
  get top(): string | number {
    return this.container.top;
  }

  /**
   * Set container top position
   */
  set top(value: string | number) {
    this.container.top = value;
  }

  /**
   * Get container left position
   */
  get left(): string | number {
    return this.container.left;
  }

  /**
   * Set container left position
   */
  set left(value: string | number) {
    this.container.left = value;
  }

  /**
   * Dispose the dropdown
   */
  dispose(): void {
    this.container.dispose();
  }
}
