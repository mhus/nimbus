/**
 * SunSizeCommand - Set sun size
 *
 * Usage: sunSize [value]
 * - Without parameters: Shows current sun size
 * - With parameter: Sets sun billboard size (10-500)
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('SunSizeCommand');

export class SunSizeCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'sunSize';
  }

  description(): string {
    return 'Set sun size (10-500)';
  }

  async execute(parameters: string[]): Promise<string> {
    const sunService = this.appContext.services.sun;

    if (!sunService) {
      return 'SunService not available';
    }

    // Show current size if no parameters
    if (parameters.length === 0) {
      return 'Usage: sunSize [value] (10-500)';
    }

    // Parse and validate parameter
    const size = parseFloat(parameters[0]);

    if (isNaN(size)) {
      return 'Invalid parameter. Value must be a number (10-500).';
    }

    if (size < 10 || size > 500) {
      return 'Value out of bounds. Sun size must be between 10 and 500.';
    }

    // Set sun size
    sunService.setSunSize(size);

    logger.info('Sun size set', { size });

    return `Sun size set to ${size}`;
  }
}
