/**
 * SetSelectedEditBlockCommand - Sets the selected edit block (green highlight)
 *
 * Usage: setSelectedEditBlock [x] [y] [z]
 * Example: setSelectedEditBlock 10 64 5   (selects block at position)
 * Example: setSelectedEditBlock           (clears selection)
 */

import { CommandHandler } from './CommandHandler';
import type { AppContext } from '../AppContext';

/**
 * SetSelectedEditBlock command - Sets or clears the selected edit block
 */
export class SetSelectedEditBlockCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'setSelectedEditBlock';
  }

  description(): string {
    return 'Sets or clears the selected edit block (setSelectedEditBlock [x] [y] [z])';
  }

  execute(parameters: any[]): any {
    const selectService = this.appContext.services.select;

    if (!selectService) {
      console.error('SelectService not available');
      return { error: 'SelectService not available' };
    }

    // No parameters = clear selection
    if (parameters.length === 0) {
      selectService.setSelectedEditBlock();
      console.log('✓ Edit block selection cleared');
      return { success: true, cleared: true };
    }

    // Need exactly 3 parameters for position
    if (parameters.length !== 3) {
      console.error('Usage: setSelectedEditBlock [x] [y] [z]');
      console.error('');
      console.error('Parameters:');
      console.error('  No parameters: Clears the selection');
      console.error('  x, y, z: World coordinates of the block to select');
      console.error('');
      console.error('Examples:');
      console.error('  setSelectedEditBlock           (clear selection)');
      console.error('  setSelectedEditBlock 10 64 5   (select block at x=10, y=64, z=5)');
      return { error: 'Invalid arguments' };
    }

    // Parse coordinates
    const x = parseFloat(parameters[0]);
    const y = parseFloat(parameters[1]);
    const z = parseFloat(parameters[2]);

    if (isNaN(x) || isNaN(y) || isNaN(z)) {
      console.error('✗ Coordinates must be valid numbers');
      return { error: 'Invalid coordinates' };
    }

    // Set selection
    try {
      selectService.setSelectedEditBlock(x, y, z);

      const result = {
        success: true,
        position: { x, y, z },
      };

      console.log('✓ Edit block selected with green highlight:');
      console.log(`  Position: (${x}, ${y}, ${z})`);

      return result;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      console.error(`✗ Failed to set selected edit block: ${errorMessage}`);
      return { error: errorMessage };
    }
  }
}
