/**
 * SunEnableCommand - Enable or disable sun visibility
 *
 * Usage: sunEnable [true|false]
 * - Without parameters: Shows current sun enabled status
 * - With parameter: Enables or disables sun visibility
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('SunEnableCommand');

export class SunEnableCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'sunEnable';
  }

  description(): string {
    return 'Enable or disable sun visibility (true|false)';
  }

  async execute(parameters: string[]): Promise<string> {
    const sunService = this.appContext.services.sun;

    if (!sunService) {
      return 'SunService not available';
    }

    // Show current status if no parameters
    if (parameters.length === 0) {
      return 'Usage: sunEnable [true|false]';
    }

    // Parse parameter
    const value = parameters[0].toLowerCase();

    if (value !== 'true' && value !== 'false') {
      return 'Invalid parameter. Value must be "true" or "false".';
    }

    const enabled = value === 'true';

    // Set sun visibility
    sunService.setEnabled(enabled);

    logger.info('Sun visibility changed', { enabled });

    return `Sun ${enabled ? 'enabled' : 'disabled'}`;
  }
}
