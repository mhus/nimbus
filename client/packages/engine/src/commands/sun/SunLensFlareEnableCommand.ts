/**
 * SunLensFlareEnableCommand - Enable or disable sun lens flare effect
 *
 * Usage: sunLensFlareEnable [true|false]
 * - Without parameters: Shows usage information
 * - With parameter: Enables or disables sun lens flare effect
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('SunLensFlareEnableCommand');

export class SunLensFlareEnableCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'sunLensFlareEnable';
  }

  description(): string {
    return 'Enable or disable sun lens flare effect (true|false)';
  }

  async execute(parameters: string[]): Promise<string> {
    const sunService = this.appContext.services.sun;

    if (!sunService) {
      return 'SunService not available';
    }

    // Show usage if no parameters
    if (parameters.length === 0) {
      return 'Usage: sunLensFlareEnable [true|false]';
    }

    // Parse parameter
    const value = parameters[0].toLowerCase();

    if (value !== 'true' && value !== 'false') {
      return 'Invalid parameter. Value must be "true" or "false".';
    }

    const enabled = value === 'true';

    // Set lens flare visibility
    sunService.setSunLensFlareEnabled(enabled);

    logger.info('Lens flare visibility changed', { enabled });

    return `Lens flare ${enabled ? 'enabled' : 'disabled'}`;
  }
}
