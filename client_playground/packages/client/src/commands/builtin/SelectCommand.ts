/**
 * Select Command - Toggle block selection mode
 */
import type { Command } from '../Command';
import type { CmdExecutionContext } from '../CmdExecutionContext';
import type { VoxelClient } from '../../VoxelClient';

export class SelectCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'select';
  }

  getDescription(): string {
    return 'Toggle block selection mode';
  }

  getHelp(): string {
    return `Usage: select [on|off]

Toggles block selection mode. When enabled, the block you're looking at
(within 5 blocks) will be highlighted with a white wireframe.

Examples:
  select        - Toggle selection mode
  select on     - Enable selection mode
  select off    - Disable selection mode`;
  }

  execute(context: CmdExecutionContext): void {
    const blockSelector = (this.client as any).blockSelector;

    if (!blockSelector) {
      context.writeError('Block selector not available');
      return;
    }

    const param = context.getParameter(0)?.toLowerCase();

    if (param === 'on') {
      blockSelector.enable();
      context.writeLine('Block selection enabled');
    } else if (param === 'off') {
      blockSelector.disable();
      context.writeLine('Block selection disabled');
    } else {
      // Toggle
      blockSelector.toggle();
      const isEnabled = blockSelector.isEnabled();
      context.writeLine(`Block selection ${isEnabled ? 'enabled' : 'disabled'}`);
    }

    // Show selected block info if enabled
    if (blockSelector.isEnabled()) {
      const selected = blockSelector.getSelectedBlock();
      if (selected) {
        const blockName = this.getBlockName(selected.blockId);
        context.writeLine(`Looking at: ${blockName} at (${selected.blockX}, ${selected.blockY}, ${selected.blockZ})`);
      }
    }
  }

  /**
   * Get block name from ID
   */
  private getBlockName(blockId: number): string {
    const registry = this.client.getRegistry();
    const block = registry.getBlockByID(blockId);
    return block ? block.displayName || block.name : `Unknown (${blockId})`;
  }
}
