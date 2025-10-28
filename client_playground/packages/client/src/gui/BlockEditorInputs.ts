/**
 * Block Editor Input Components
 * Complex input creation functions for BlockEditor (text fields, dropdowns, checkboxes, specialized rows)
 */
import {
  AdvancedDynamicTexture,
  Rectangle,
  TextBlock,
  Control,
  StackPanel,
  Button,
  InputText,
  Checkbox,
  ColorPicker,
} from '@babylonjs/gui';
import { Color3 } from '@babylonjs/core';
import { DropdownMenu } from './DropdownMenu';

/**
 * Add input field with change tracking (on Enter or blur only)
 */
export function addInputField(
  panel: StackPanel,
  value: string,
  onChange?: (newValue: string) => void
): InputText {
  const input = new InputText();
  input.width = 1;
  input.height = '30px';
  input.color = '#FFFFFF';
  input.background = '#333333';
  input.text = value;
  input.fontSize = 13;
  input.thickness = 1;
  input.paddingLeft = 5;

  // Track the last committed value
  let lastValue = value;

  // Add change listener if provided (trigger on Enter or blur only)
  if (onChange) {
    // Handle Enter key
    input.onKeyboardEventProcessedObservable.add((eventData) => {
      if (eventData.key === 'Enter' && input.text !== lastValue) {
        lastValue = input.text;
        onChange(input.text);
      }
    });

    // Handle blur (focus lost)
    input.onBlurObservable.add(() => {
      if (input.text !== lastValue) {
        lastValue = input.text;
        onChange(input.text);
      }
    });
  }

  panel.addControl(input);
  return input;
}

/**
 * Add dropdown selector
 */
export function addDropdown(
  panel: StackPanel,
  advancedTexture: AdvancedDynamicTexture,
  options: string[],
  selectedValue: string,
  onChange: (newValue: string) => void
): void {
  // Create dropdown menu
  const dropdown = new DropdownMenu(panel, {
    width: 400,
    height: 30,
    color: '#FFFFFF',
    background: '#333333',
    advancedTexture: advancedTexture
  });

  // Set selected value
  dropdown.setValue(selectedValue);

  // Add all options
  dropdown.setOptions(options, selectedValue);

  // Set change handler
  dropdown.onChange(onChange);
}

/**
 * Add checkbox
 */
export function addCheckbox(
  panel: StackPanel,
  label: string,
  checked: boolean,
  onChange: (value: boolean) => void
): Checkbox {
  const container = new StackPanel();
  container.isVertical = false;
  container.width = 1;
  container.height = '30px';

  const checkbox = new Checkbox();
  checkbox.width = '20px';
  checkbox.height = '20px';
  checkbox.isChecked = checked;
  checkbox.color = '#FFFFFF';
  checkbox.background = '#333333';
  checkbox.onIsCheckedChangedObservable.add((value) => onChange(value));

  const labelText = new TextBlock();
  labelText.text = label;
  labelText.color = '#FFFFFF';
  labelText.fontSize = 13;
  labelText.width = 0.9;
  labelText.height = '30px';
  labelText.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
  labelText.paddingLeft = 10;

  container.addControl(checkbox);
  container.addControl(labelText);
  panel.addControl(container);

  return checkbox;
}

/**
 * Add collapsible section header
 */
export function addCollapsibleHeader(
  panel: StackPanel,
  title: string,
  isExpanded: boolean,
  onToggle: () => void
): void {
  const header = Button.CreateSimpleButton('header_' + title, (isExpanded ? '▼ ' : '▶ ') + title);
  header.width = 1;
  header.height = '35px';
  header.color = '#FFFF00';
  header.background = '#004400';
  header.thickness = 1;
  header.fontSize = 14;
  header.fontWeight = 'bold';
  header.horizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
  header.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
  header.paddingLeft = 10;
  header.onPointerClickObservable.add(onToggle);
  panel.addControl(header);
}

/**
 * Add number input (for floats/ints with range)
 */
export function addNumberInput(
  panel: StackPanel,
  label: string,
  value: number | undefined,
  min: number,
  max: number,
  onChange: (value: number | undefined) => void
): void {
  // Add label
  const labelBlock = new TextBlock();
  labelBlock.text = label;
  labelBlock.color = '#AAAAAA';
  labelBlock.fontSize = 13;
  labelBlock.height = '20px';
  labelBlock.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
  labelBlock.paddingTop = 5;
  panel.addControl(labelBlock);

  // Add input field
  addInputField(
    panel,
    value !== undefined ? value.toString() : '',
    (newValue) => {
      if (newValue === '') {
        onChange(undefined);
      } else {
        const num = parseFloat(newValue);
        if (!isNaN(num)) {
          const clamped = Math.max(min, Math.min(max, num));
          onChange(clamped);
        }
      }
    }
  );
}

/**
 * Add edge offset row (X, Y, Z in horizontal layout)
 */
export function addEdgeOffsetRow(
  panel: StackPanel,
  x: number,
  y: number,
  z: number,
  onChange: (x: number | undefined, y: number | undefined, z: number | undefined) => void
): void {
  // Create horizontal container
  const container = new StackPanel();
  container.isVertical = false;
  container.width = 1;
  container.height = '30px';

  // Store current values
  let currentX = x;
  let currentY = y;
  let currentZ = z;

  // Helper function to create a small labeled input
  const createSmallInput = (label: string, value: number, onChange: (value: number | undefined) => void) => {
    // Label
    const labelText = new TextBlock();
    labelText.text = label;
    labelText.color = '#AAAAAA';
    labelText.fontSize = 12;
    labelText.width = '25px';
    labelText.height = '30px';
    labelText.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_RIGHT;
    labelText.paddingRight = 5;
    container.addControl(labelText);

    // Input
    const input = new InputText();
    input.width = '60px';
    input.height = '25px';
    input.color = '#FFFFFF';
    input.background = '#333333';
    input.text = value.toString();
    input.fontSize = 12;
    input.thickness = 1;
    input.paddingLeft = 5;

    // Track the last committed value
    let lastValue = value.toString();

    // Handle Enter key
    input.onKeyboardEventProcessedObservable.add((eventData) => {
      if (eventData.key === 'Enter' && input.text !== lastValue) {
        lastValue = input.text;
        if (input.text === '') {
          onChange(undefined);
        } else {
          const num = parseInt(input.text);
          if (!isNaN(num)) {
            const clamped = Math.max(-127, Math.min(128, num));
            onChange(clamped);
          }
        }
      }
    });

    // Handle blur (focus lost)
    input.onBlurObservable.add(() => {
      if (input.text !== lastValue) {
        lastValue = input.text;
        if (input.text === '') {
          onChange(undefined);
        } else {
          const num = parseInt(input.text);
          if (!isNaN(num)) {
            const clamped = Math.max(-127, Math.min(128, num));
            onChange(clamped);
          }
        }
      }
    });

    container.addControl(input);

    // Spacer
    const spacer = new Rectangle('spacer');
    spacer.width = '10px';
    spacer.height = '30px';
    spacer.thickness = 0;
    container.addControl(spacer);
  };

  // Create inputs for X, Y, Z
  createSmallInput('X:', x, (value) => {
    currentX = value ?? 0;
    onChange(currentX, currentY, currentZ);
  });

  createSmallInput('Y:', y, (value) => {
    currentY = value ?? 0;
    onChange(currentX, currentY, currentZ);
  });

  createSmallInput('Z:', z, (value) => {
    currentZ = value ?? 0;
    onChange(currentX, currentY, currentZ);
  });

  // Add container to content panel
  panel.addControl(container);
}

/**
 * Add scale row (X, Y, Z floats in horizontal layout)
 */
export function addScaleRow(
  panel: StackPanel,
  x: number,
  y: number,
  z: number,
  onChange: (x: number | undefined, y: number | undefined, z: number | undefined) => void
): void {
  // Create horizontal container
  const container = new StackPanel();
  container.isVertical = false;
  container.width = 1;
  container.height = '30px';

  // Store current values
  let currentX = x;
  let currentY = y;
  let currentZ = z;

  // Helper function to create a small labeled input for floats
  const createSmallFloatInput = (label: string, value: number, onChange: (value: number | undefined) => void) => {
    // Label
    const labelText = new TextBlock();
    labelText.text = label;
    labelText.color = '#AAAAAA';
    labelText.fontSize = 12;
    labelText.width = '25px';
    labelText.height = '30px';
    labelText.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_RIGHT;
    labelText.paddingRight = 5;
    container.addControl(labelText);

    // Input (configured like addInputField for better input handling)
    const input = new InputText();
    input.width = '80px';  // Wider for better decimal input
    input.height = '30px';  // Same height as addInputField
    input.color = '#FFFFFF';
    input.background = '#333333';
    input.text = value !== undefined ? value.toString() : '';  // Allow empty
    input.fontSize = 13;  // Same fontSize as addInputField
    input.thickness = 1;
    input.paddingLeft = 5;
    input.autoStretchWidth = false;  // Explicitly set

    // Track the last committed value
    let lastValue = value !== undefined ? value.toString() : '';

    // Handle Enter key
    input.onKeyboardEventProcessedObservable.add((eventData) => {
      if (eventData.key === 'Enter' && input.text !== lastValue) {
        lastValue = input.text;
        if (input.text === '') {
          onChange(1); // default to 1 if empty
        } else {
          const num = parseFloat(input.text);
          if (!isNaN(num)) {
            const clamped = Math.max(0.001, Math.min(10, num));
            onChange(clamped);
          }
        }
      }
    });

    // Handle blur (focus lost)
    input.onBlurObservable.add(() => {
      if (input.text !== lastValue) {
        lastValue = input.text;
        if (input.text === '') {
          onChange(1); // default to 1 if empty
        } else {
          const num = parseFloat(input.text);
          if (!isNaN(num)) {
            const clamped = Math.max(0.001, Math.min(10, num));
            onChange(clamped);
          }
        }
      }
    });

    container.addControl(input);

    // Spacer
    const spacer = new Rectangle('spacer');
    spacer.width = '10px';
    spacer.height = '30px';
    spacer.thickness = 0;
    container.addControl(spacer);
  };

  // Create inputs for X, Y, Z
  createSmallFloatInput('X:', x, (value) => {
    currentX = value ?? 1;
    onChange(currentX, currentY, currentZ);
  });

  createSmallFloatInput('Y:', y, (value) => {
    currentY = value ?? 1;
    onChange(currentX, currentY, currentZ);
  });

  createSmallFloatInput('Z:', z, (value) => {
    currentZ = value ?? 1;
    onChange(currentX, currentY, currentZ);
  });

  // Add container to content panel
  panel.addControl(container);
}

/**
 * Add rotation row (X and Y rotation in horizontal layout, 0-360 degrees)
 */
export function addRotationRow(
  panel: StackPanel,
  x: number,
  y: number,
  onChange: (x: number | undefined, y: number | undefined) => void
): void {
  // Create horizontal container
  const container = new StackPanel();
  container.isVertical = false;
  container.width = 1;
  container.height = '30px';

  // Store current values
  let currentX = x;
  let currentY = y;

  // Helper function to create a labeled input for rotation (0-360 degrees)
  const createRotationInput = (label: string, value: number, onChange: (value: number | undefined) => void) => {
    // Label
    const labelText = new TextBlock();
    labelText.text = label;
    labelText.color = '#AAAAAA';
    labelText.fontSize = 12;
    labelText.width = '25px';
    labelText.height = '30px';
    labelText.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_RIGHT;
    labelText.paddingRight = 5;
    container.addControl(labelText);

    // Input
    const input = new InputText();
    input.width = '80px';
    input.height = '25px';
    input.color = '#FFFFFF';
    input.background = '#333333';
    input.text = value.toString();
    input.fontSize = 12;
    input.thickness = 1;
    input.paddingLeft = 5;

    // Track the last committed value
    let lastValue = value.toString();

    // Handle Enter key
    input.onKeyboardEventProcessedObservable.add((eventData) => {
      if (eventData.key === 'Enter' && input.text !== lastValue) {
        lastValue = input.text;
        if (input.text === '') {
          onChange(0); // Default to 0 if empty
        } else {
          const num = parseFloat(input.text);
          if (!isNaN(num)) {
            // Clamp to 0-360 range
            let clamped = num % 360;
            if (clamped < 0) clamped += 360;
            onChange(clamped);
          }
        }
      }
    });

    // Handle blur (focus lost)
    input.onBlurObservable.add(() => {
      if (input.text !== lastValue) {
        lastValue = input.text;
        if (input.text === '') {
          onChange(0); // Default to 0 if empty
        } else {
          const num = parseFloat(input.text);
          if (!isNaN(num)) {
            // Clamp to 0-360 range
            let clamped = num % 360;
            if (clamped < 0) clamped += 360;
            onChange(clamped);
          }
        }
      }
    });

    container.addControl(input);

    // Spacer
    const spacer = new Rectangle('spacer');
    spacer.width = '15px';
    spacer.height = '30px';
    spacer.thickness = 0;
    container.addControl(spacer);
  };

  // Create inputs for X and Y rotation
  createRotationInput('X:', x, (value) => {
    currentX = value ?? 0;
    onChange(currentX, currentY);
  });

  createRotationInput('Y:', y, (value) => {
    currentY = value ?? 0;
    onChange(currentX, currentY);
  });

  // Add container to content panel
  panel.addControl(container);
}

/**
 * Add color picker row (Hex input + Pick Color button)
 */
export function addColorPickerRow(
  panel: StackPanel,
  advancedTexture: AdvancedDynamicTexture,
  initialHex: string,
  onChange: (hexValue: string) => void
): void {
  // Create horizontal container
  const container = new StackPanel();
  container.isVertical = false;
  container.width = 1;
  container.height = '30px';

  // Hex input field
  const hexInput = new InputText();
  hexInput.width = '300px';
  hexInput.height = '25px';
  hexInput.color = '#FFFFFF';
  hexInput.background = '#333333';
  hexInput.text = initialHex;
  hexInput.fontSize = 13;
  hexInput.thickness = 1;
  hexInput.paddingLeft = 5;
  hexInput.placeholderText = '#FF0000';

  // Track the last committed value
  let lastValue = initialHex;

  // Handle Enter key
  hexInput.onKeyboardEventProcessedObservable.add((eventData) => {
    if (eventData.key === 'Enter' && hexInput.text !== lastValue) {
      lastValue = hexInput.text;
      onChange(hexInput.text);
    }
  });

  // Handle blur (focus lost)
  hexInput.onBlurObservable.add(() => {
    if (hexInput.text !== lastValue) {
      lastValue = hexInput.text;
      onChange(hexInput.text);
    }
  });

  container.addControl(hexInput);

  // Spacer
  const spacer = new Rectangle('spacer');
  spacer.width = '10px';
  spacer.height = '30px';
  spacer.thickness = 0;
  container.addControl(spacer);

  // Pick Color button
  const pickButton = Button.CreateSimpleButton('pickColor', 'Pick Color');
  pickButton.width = '90px';
  pickButton.height = '25px';
  pickButton.color = '#FFFFFF';
  pickButton.background = '#0066AA';
  pickButton.thickness = 1;
  pickButton.fontSize = 12;

  pickButton.onPointerClickObservable.add(() => {
    openColorPickerDialog(advancedTexture, hexInput.text, (newHexValue) => {
      hexInput.text = newHexValue;
      onChange(newHexValue);
    });
  });

  container.addControl(pickButton);

  // Add container to content panel
  panel.addControl(container);
}

/**
 * Open color picker dialog
 */
export function openColorPickerDialog(
  advancedTexture: AdvancedDynamicTexture,
  currentHex: string,
  onColorSelected: (hexValue: string) => void
): void {
  // Create modal background
  const modalBg = new Rectangle('colorPickerModalBg');
  modalBg.width = 1;
  modalBg.height = 1;
  modalBg.background = '#000000';
  modalBg.alpha = 0.7;
  modalBg.thickness = 0;
  advancedTexture.addControl(modalBg);

  // Create dialog container
  const dialog = new Rectangle('colorPickerDialog');
  dialog.width = '450px';
  dialog.height = '250px';
  dialog.thickness = 2;
  dialog.color = '#FFFFFF';
  dialog.background = '#222222';
  dialog.horizontalAlignment = Control.HORIZONTAL_ALIGNMENT_CENTER;
  dialog.verticalAlignment = Control.VERTICAL_ALIGNMENT_CENTER;
  advancedTexture.addControl(dialog);

  // Title
  const titleText = new TextBlock('dialogTitle');
  titleText.text = 'Pick a Color';
  titleText.color = '#FFFFFF';
  titleText.fontSize = 16;
  titleText.height = '30px';
  titleText.verticalAlignment = Control.VERTICAL_ALIGNMENT_TOP;
  titleText.top = 10;
  dialog.addControl(titleText);

  // Parse current color
  let initialColor = new Color3(1, 1, 1);
  if (currentHex.startsWith('#') && currentHex.length === 7) {
    const r = parseInt(currentHex.substr(1, 2), 16) / 255;
    const g = parseInt(currentHex.substr(3, 2), 16) / 255;
    const b = parseInt(currentHex.substr(5, 2), 16) / 255;
    initialColor = new Color3(r, g, b);
  }

  // ColorPicker
  const colorPicker = new ColorPicker('colorPicker');
  colorPicker.value = initialColor;
  colorPicker.width = '400px';
  colorPicker.height = '150px';
  colorPicker.horizontalAlignment = Control.HORIZONTAL_ALIGNMENT_CENTER;
  colorPicker.verticalAlignment = Control.VERTICAL_ALIGNMENT_CENTER;
  colorPicker.top = 0;
  dialog.addControl(colorPicker);

  // Buttons container
  const buttonsContainer = new StackPanel('buttonsContainer');
  buttonsContainer.isVertical = false;
  buttonsContainer.height = '35px';
  buttonsContainer.width = '200px';
  buttonsContainer.horizontalAlignment = Control.HORIZONTAL_ALIGNMENT_CENTER;
  buttonsContainer.verticalAlignment = Control.VERTICAL_ALIGNMENT_BOTTOM;
  buttonsContainer.top = -10;
  dialog.addControl(buttonsContainer);

  // OK button
  const okButton = Button.CreateSimpleButton('okButton', 'OK');
  okButton.width = '90px';
  okButton.height = '30px';
  okButton.color = '#FFFFFF';
  okButton.background = '#00AA00';
  okButton.thickness = 1;
  okButton.fontSize = 13;
  okButton.onPointerClickObservable.add(() => {
    const color = colorPicker.value;
    const r = Math.round(color.r * 255).toString(16).padStart(2, '0');
    const g = Math.round(color.g * 255).toString(16).padStart(2, '0');
    const b = Math.round(color.b * 255).toString(16).padStart(2, '0');
    const hexValue = `#${r}${g}${b}`;

    onColorSelected(hexValue);

    // Close dialog
    advancedTexture.removeControl(dialog);
    advancedTexture.removeControl(modalBg);
    dialog.dispose();
    modalBg.dispose();
  });
  buttonsContainer.addControl(okButton);

  // Spacer
  const buttonSpacer = new Rectangle('buttonSpacer');
  buttonSpacer.width = '20px';
  buttonSpacer.height = '30px';
  buttonSpacer.thickness = 0;
  buttonsContainer.addControl(buttonSpacer);

  // Cancel button
  const cancelButton = Button.CreateSimpleButton('cancelButton', 'Cancel');
  cancelButton.width = '90px';
  cancelButton.height = '30px';
  cancelButton.color = '#FFFFFF';
  cancelButton.background = '#AA0000';
  cancelButton.thickness = 1;
  cancelButton.fontSize = 13;
  cancelButton.onPointerClickObservable.add(() => {
    // Close dialog without applying
    advancedTexture.removeControl(dialog);
    advancedTexture.removeControl(modalBg);
    dialog.dispose();
    modalBg.dispose();
  });
  buttonsContainer.addControl(cancelButton);
}
