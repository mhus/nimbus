/**
 * SkyBoxEnableCommand - Enable/disable skybox
 *
 * Usage: skyBoxEnable [on|off]
 * - Without parameters: Shows usage information
 * - With "on": Enables skybox visibility
 * - With "off": Disables skybox visibility
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('SkyBoxEnableCommand');

export class SkyBoxEnableCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'skyBoxEnable';
  }

  description(): string {
    return 'Enable/disable skybox (on|off)';
  }

  async execute(parameters: string[]): Promise<string> {
    const skyBoxService = this.appContext.services.skyBox;

    if (!skyBoxService) {
      return 'SkyBoxService not available';
    }

    // Show usage if no parameters
    if (parameters.length === 0) {
      return 'Usage: skyBoxEnable [on|off]\nExample: skyBoxEnable on';
    }

    const param = parameters[0].toLowerCase();

    if (param === 'on') {
      skyBoxService.setEnabled(true);
      logger.info('SkyBox enabled via command');
      return 'SkyBox enabled';
    } else if (param === 'off') {
      skyBoxService.setEnabled(false);
      logger.info('SkyBox disabled via command');
      return 'SkyBox disabled';
    } else {
      return 'Invalid parameter. Use "on" or "off".';
    }
  }
}
