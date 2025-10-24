/**
 * Block Editor UI Helper Functions
 * Reusable UI component creation functions for BlockEditor
 */
import {
  TextBlock,
  Control,
  StackPanel,
} from '@babylonjs/gui';

/**
 * Add a label text block
 */
export function addLabel(panel: StackPanel, text: string): void {
  const label = new TextBlock();
  label.text = text;
  label.color = '#AAAAAA';
  label.fontSize = 13;
  label.height = '20px';
  label.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
  label.paddingTop = 5;
  panel.addControl(label);
}

/**
 * Add a section title (larger, green text)
 */
export function addSectionTitle(panel: StackPanel, text: string): void {
  const title = new TextBlock();
  title.text = text;
  title.color = '#00FF00';
  title.fontSize = 16;
  title.height = '30px';
  title.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
  title.paddingTop = 5;
  panel.addControl(title);
}

/**
 * Add a property row (label: value format)
 */
export function addProperty(panel: StackPanel, label: string, value: string): void {
  const text = new TextBlock();
  text.text = `  ${label}: ${value}`;
  text.color = '#FFFFFF';
  text.fontSize = 14;
  text.height = '25px';
  text.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
  text.textWrapping = true;
  text.resizeToFit = true;
  panel.addControl(text);
}

/**
 * Add simple text block
 */
export function addText(
  panel: StackPanel,
  text: string,
  color: string = '#FFFFFF',
  fontSize: number = 14
): void {
  const textBlock = new TextBlock();
  textBlock.text = text;
  textBlock.color = color;
  textBlock.fontSize = fontSize;
  textBlock.height = '25px';
  textBlock.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
  textBlock.textWrapping = true;
  textBlock.resizeToFit = true;
  panel.addControl(textBlock);
}

/**
 * Add vertical spacer
 */
export function addSpacer(panel: StackPanel): void {
  const spacer = new TextBlock();
  spacer.text = '';
  spacer.height = '10px';
  panel.addControl(spacer);
}
