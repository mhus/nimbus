/**
 * Game Console
 * In-game console with scrollable output and input field
 */
import { AdvancedDynamicTexture, Rectangle, TextBlock, InputText, ScrollViewer, Control } from '@babylonjs/gui';
import type { Scene } from '@babylonjs/core';
import type { CommandController } from '../commands/CommandController';

export class GameConsole {
  private scene: Scene;
  private advancedTexture: AdvancedDynamicTexture;
  private commandController: CommandController;
  private serverCommandController?: CommandController;

  private container!: Rectangle;
  private outputScroller!: ScrollViewer;
  private outputText!: TextBlock;
  private inputField!: InputText;

  private isVisible = false;
  private outputLines: string[] = [];
  private maxOutputLines = 1000;

  constructor(
    scene: Scene,
    advancedTexture: AdvancedDynamicTexture,
    commandController: CommandController,
    serverCommandController?: CommandController
  ) {
    this.scene = scene;
    this.advancedTexture = advancedTexture;
    this.commandController = commandController;
    this.serverCommandController = serverCommandController;

    this.createUI();
    this.hide();

    console.log('[GameConsole] Initialized');
  }

  /**
   * Create console UI
   */
  private createUI(): void {
    // Main container
    this.container = new Rectangle('consoleContainer');
    this.container.width = 0.8;
    this.container.height = 0.5;
    this.container.thickness = 2;
    this.container.color = '#FFFFFF';
    this.container.background = '#000000AA';
    this.container.verticalAlignment = Control.VERTICAL_ALIGNMENT_TOP;
    this.container.top = 10;
    this.container.isVisible = false; // Start hidden

    // Output scroll viewer
    this.outputScroller = new ScrollViewer('consoleOutput');
    this.outputScroller.width = 1;
    this.outputScroller.height = '90%';
    this.outputScroller.thickness = 0;
    this.outputScroller.verticalAlignment = Control.VERTICAL_ALIGNMENT_TOP;

    // Output text
    this.outputText = new TextBlock('consoleOutputText');
    this.outputText.text = 'Console ready. Type "help" for available commands.';
    this.outputText.color = '#00FF00';
    this.outputText.fontSize = 14;
    this.outputText.textHorizontalAlignment = Control.HORIZONTAL_ALIGNMENT_LEFT;
    this.outputText.textVerticalAlignment = Control.VERTICAL_ALIGNMENT_TOP;
    this.outputText.textWrapping = true;
    this.outputText.resizeToFit = true;
    this.outputText.paddingLeft = 10;
    this.outputText.paddingTop = 10;

    this.outputScroller.addControl(this.outputText);

    // Input field
    this.inputField = new InputText('consoleInput');
    this.inputField.width = 1;
    this.inputField.height = '10%';
    this.inputField.color = '#FFFFFF';
    this.inputField.background = '#333333';
    this.inputField.placeholderText = 'Enter command...';
    this.inputField.placeholderColor = '#888888';
    this.inputField.fontSize = 14;
    this.inputField.verticalAlignment = Control.VERTICAL_ALIGNMENT_BOTTOM;
    this.inputField.thickness = 1;
    this.inputField.paddingLeft = 10;

    // Handle input submission and ESC key
    this.inputField.onKeyboardEventProcessedObservable.add((eventData) => {
      if (eventData.key === 'Enter') {
        this.submitInput();
        // Keep focus in input field after submit
        setTimeout(() => {
          this.inputField.focus();
        }, 10);
      } else if (eventData.key === 'Escape') {
        // Close console on ESC
        this.hide();
      }
    });

    // Add to container
    this.container.addControl(this.outputScroller);
    this.container.addControl(this.inputField);

    // Add to advanced texture
    this.advancedTexture.addControl(this.container);
  }

  /**
   * Submit input command
   */
  private async submitInput(): Promise<void> {
    const input = this.inputField.text.trim();

    if (!input) {
      return;
    }

    // Clear input field
    this.inputField.text = '';

    // Add command to output
    this.writeLine(`> ${input}`);

    // Determine which controller to use
    let controller = this.commandController;

    if (input.startsWith('/') && this.serverCommandController) {
      // Server command - remove leading '/'
      controller = this.serverCommandController;
      const serverInput = input.substring(1);

      // Execute on server controller
      const context = await controller.executeCommand(serverInput);

      // Display output
      const output = context.getOutput();
      output.forEach(line => this.writeLine(line));
    } else {
      // Client command
      const context = await controller.executeCommand(input);

      // Display output
      const output = context.getOutput();
      output.forEach(line => this.writeLine(line));
    }

    // Scroll to bottom
    this.scrollToBottom();
  }

  /**
   * Write line to console output
   */
  writeLine(text: string): void {
    this.outputLines.push(text);

    // Limit output lines
    if (this.outputLines.length > this.maxOutputLines) {
      this.outputLines.shift();
    }

    // Update output text
    this.outputText.text = this.outputLines.join('\n');
  }

  /**
   * Clear console output
   */
  clear(): void {
    this.outputLines = [];
    this.outputText.text = '';
  }

  /**
   * Scroll to bottom of output
   */
  private scrollToBottom(): void {
    // Small delay to ensure text is rendered
    setTimeout(() => {
      this.outputScroller.verticalBar.value = 1;
    }, 10);
  }

  /**
   * Show console
   */
  show(): void {
    if (this.isVisible) return;

    this.container.isVisible = true;
    this.isVisible = true;

    // Focus input field
    this.inputField.focus();

    console.log('[GameConsole] Shown');
  }

  /**
   * Hide console
   */
  hide(): void {
    if (!this.isVisible) return;

    this.container.isVisible = false;
    this.isVisible = false;

    // Blur input field
    this.inputField.blur();

    console.log('[GameConsole] Hidden');
  }

  /**
   * Toggle console visibility
   */
  toggle(): void {
    if (this.isVisible) {
      this.hide();
    } else {
      this.show();
    }
  }

  /**
   * Check if console is visible
   */
  getIsVisible(): boolean {
    return this.isVisible;
  }

  /**
   * Dispose console
   */
  dispose(): void {
    this.container.dispose();
  }
}
