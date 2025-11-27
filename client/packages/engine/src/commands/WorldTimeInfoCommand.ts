/**
 * WorldTimeInfoCommand - Show World Time information
 */

import { CommandHandler } from './CommandHandler';
import type { AppContext } from '../AppContext';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('WorldTimeInfoCommand');

/**
 * WorldTimeInfo command - Show World Time information
 * Usage: worldTimeInfo
 */
export class WorldTimeInfoCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'worldTimeInfo';
  }

  description(): string {
    return 'Show World Time information and status';
  }

  execute(parameters: any[]): any {
    const environmentService = this.appContext.services.environment;
    if (!environmentService) {
      console.error('EnvironmentService not available');
      return { error: 'EnvironmentService not available' };
    }

    const isRunning = environmentService.isWorldTimeRunning();
    const config = environmentService.getWorldTimeConfig();

    console.log('=== World Time Info ===');
    console.log(`  Status         : ${isRunning ? 'Running' : 'Stopped'}`);

    if (isRunning) {
      const currentTime = environmentService.getWorldTimeCurrentAsString();
      const currentMinute = environmentService.getWorldTimeCurrent();
      const daySection = environmentService.getWorldDayTimeSection();

      console.log(`  Current Time   : ${currentTime}`);
      console.log(`  World Minute   : ${currentMinute.toFixed(2)}`);
      console.log(`  Day Section    : ${daySection}`);
    }

    console.log('');
    console.log('=== Configuration ===');
    console.log(`  Minute Scaling : ${config.minuteScaling} (world minutes per real minute)`);
    console.log(`  Minutes/Hour   : ${config.minutesPerHour}`);
    console.log(`  Hours/Day      : ${config.hoursPerDay}`);
    console.log(`  Days/Month     : ${config.daysPerMonth}`);
    console.log(`  Months/Year    : ${config.monthsPerYear}`);
    console.log(`  Years/Era      : ${config.yearsPerEra}`);
    console.log('=====================');

    return {
      success: true,
      running: isRunning,
      currentTime: isRunning ? environmentService.getWorldTimeCurrentAsString() : null,
      currentMinute: isRunning ? environmentService.getWorldTimeCurrent() : null,
      daySection: isRunning ? environmentService.getWorldDayTimeSection() : null,
      config,
    };
  }
}
