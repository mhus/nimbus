/**
 * ToggleEntityPathwaysCommand - Toggle entity pathway visualization
 *
 * Usage: toggleEntityPathways [on|off]
 *
 * Shows or hides green pathway lines for debugging entity movement.
 */

import { CommandHandler } from './CommandHandler';
import type { AppContext } from '../AppContext';

/**
 * ToggleEntityPathways command - Toggle entity pathway lines
 */
export class ToggleEntityPathwaysCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'toggleEntityPathways';
  }

  description(): string {
    return 'Toggle entity pathway visualization (green lines). Usage: toggleEntityPathways [on|off]';
  }

  execute(parameters: any[]): any {
    const engineService = this.appContext.services.engine;
    if (!engineService) {
      console.error('EngineService not available');
      return { error: 'EngineService not available' };
    }

    const entityRenderService = engineService.getEntityRenderService();
    if (!entityRenderService) {
      console.error('EntityRenderService not available');
      return { error: 'EntityRenderService not available' };
    }

    // Parse parameter
    let newValue: boolean;

    if (parameters.length === 0) {
      // No parameter - toggle current value
      newValue = !entityRenderService.showPathways;
    } else {
      const param = parameters[0].toLowerCase();
      if (param === 'on' || param === 'true' || param === '1') {
        newValue = true;
      } else if (param === 'off' || param === 'false' || param === '0') {
        newValue = false;
      } else {
        console.error('Usage: toggleEntityPathways [on|off]');
        return {
          error: 'Invalid parameter',
          usage: 'toggleEntityPathways [on|off]',
          example: 'toggleEntityPathways on',
        };
      }
    }

    // Set new value
    entityRenderService.showPathways = newValue;

    const status = newValue ? 'ON' : 'OFF';
    console.log(`Entity pathway visualization: ${status}`);

    if (newValue) {
      console.log('Green lines will be shown along entity movement paths');
    } else {
      console.log('Pathway lines hidden');
    }

    return {
      showPathways: newValue,
      status,
    };
  }
}
