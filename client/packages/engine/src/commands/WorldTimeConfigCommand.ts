/**
 * WorldTimeConfigCommand - Configure World Time settings
 */

import { CommandHandler } from './CommandHandler';
import type { AppContext } from '../AppContext';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('WorldTimeConfigCommand');

/**
 * WorldTimeConfig command - Configure World Time settings
 * Usage: worldTimeConfig <minuteScaling> <minutesPerHour> <hoursPerDay> <daysPerMonth> <monthsPerYear> <yearsPerEra>
 */
export class WorldTimeConfigCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'worldTimeConfig';
  }

  description(): string {
    return 'Configure World Time settings (minuteScaling, minutesPerHour, hoursPerDay, daysPerMonth, monthsPerYear, yearsPerEra)';
  }

  execute(parameters: any[]): any {
    const environmentService = this.appContext.services.environment;
    if (!environmentService) {
      console.error('EnvironmentService not available');
      return { error: 'EnvironmentService not available' };
    }

    // Parse parameters
    if (parameters.length !== 6) {
      console.error(
        'Usage: worldTimeConfig <minuteScaling> <minutesPerHour> <hoursPerDay> <daysPerMonth> <monthsPerYear> <yearsPerEra>'
      );
      return {
        error: 'Invalid parameters',
        usage:
          'worldTimeConfig <minuteScaling> <minutesPerHour> <hoursPerDay> <daysPerMonth> <monthsPerYear> <yearsPerEra>',
      };
    }

    const minuteScaling = parseFloat(parameters[0]);
    const minutesPerHour = parseInt(parameters[1], 10);
    const hoursPerDay = parseInt(parameters[2], 10);
    const daysPerMonth = parseInt(parameters[3], 10);
    const monthsPerYear = parseInt(parameters[4], 10);
    const yearsPerEra = parseInt(parameters[5], 10);

    // Validate parameters
    if (
      isNaN(minuteScaling) ||
      isNaN(minutesPerHour) ||
      isNaN(hoursPerDay) ||
      isNaN(daysPerMonth) ||
      isNaN(monthsPerYear) ||
      isNaN(yearsPerEra)
    ) {
      console.error('All parameters must be valid numbers');
      return { error: 'All parameters must be valid numbers' };
    }

    // Set configuration
    environmentService.setWorldTimeConfig(
      minuteScaling,
      minutesPerHour,
      hoursPerDay,
      daysPerMonth,
      monthsPerYear,
      yearsPerEra
    );

    const config = environmentService.getWorldTimeConfig();

    console.log('=== World Time Config Updated ===');
    console.log(`  @Minute Scaling    : ${config.minuteScaling} (world minutes per real minute)`);
    console.log(`  @Minutes per Hour  : ${config.minutesPerHour}`);
    console.log(`  @Hours per Day     : ${config.hoursPerDay}`);
    console.log(`  @Days per Month    : ${config.daysPerMonth}`);
    console.log(`  @Months per Year   : ${config.monthsPerYear}`);
    console.log(`  @Years per Era     : ${config.yearsPerEra}`);
    console.log('=================================');

    return { success: true, config };
  }
}
