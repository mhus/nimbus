/**
 * Wind Commands
 * Commands to control global wind parameters
 */
import type { Command } from './Command';
import type { CmdExecutionContext } from './CmdExecutionContext';
import type { WindManager } from '../wind/WindManager';

/**
 * windDirection command - Set wind direction
 * Usage: windDirection <x> <z>
 */
export class WindDirectionCommand implements Command {
  constructor(private windManager: WindManager) {}

  getName(): string {
    return 'windDirection';
  }

  getDescription(): string {
    return 'Set wind direction (x, z)';
  }

  getHelp(): string {
    return 'Usage: windDirection <x> <z>\nSet the wind direction as a 2D vector.\nExample: windDirection 1 0  (wind from west)';
  }

  execute(context: CmdExecutionContext): void {
    const params = context.getParameters();

    if (params.length === 0) {
      // Show current wind direction
      const dir = this.windManager.getWindDirection();
      context.writeLine(`Current wind direction: (${dir.x.toFixed(2)}, ${dir.z.toFixed(2)})`);
      return;
    }

    if (params.length < 2) {
      context.writeError('Missing parameters. Usage: windDirection <x> <z>');
      return;
    }

    const x = parseFloat(params[0]);
    const z = parseFloat(params[1]);

    if (isNaN(x) || isNaN(z)) {
      context.writeError('Invalid parameters. Both x and z must be numbers.');
      return;
    }

    this.windManager.setWindDirection(x, z);
    const dir = this.windManager.getWindDirection();
    context.writeLine(`Wind direction set to (${dir.x.toFixed(2)}, ${dir.z.toFixed(2)})`);
  }
}

/**
 * windStrength command - Set wind strength
 * Usage: windStrength <value>
 */
export class WindStrengthCommand implements Command {
  constructor(private windManager: WindManager) {}

  getName(): string {
    return 'windStrength';
  }

  getDescription(): string {
    return 'Set wind strength (0-10)';
  }

  getHelp(): string {
    return 'Usage: windStrength <value>\nSet the base wind strength (0-10).\nExample: windStrength 0.5  (50% wind strength)';
  }

  execute(context: CmdExecutionContext): void {
    const params = context.getParameters();

    if (params.length === 0) {
      // Show current wind strength
      const strength = this.windManager.getWindStrength();
      context.writeLine(`Current wind strength: ${strength.toFixed(2)}`);
      return;
    }

    const value = parseFloat(params[0]);

    if (isNaN(value)) {
      context.writeError('Invalid parameter. Value must be a number (0-10).');
      return;
    }

    if (value < 0 || value > 10) {
      context.writeError('Value out of bounds. Wind strength must be between 0 and 10.');
      return;
    }

    this.windManager.setWindStrength(value);
    const strength = this.windManager.getWindStrength();
    context.writeLine(`Wind strength set to ${strength.toFixed(2)}`);
  }
}

/**
 * windGustStrength command - Set wind gust strength
 * Usage: windGustStrength <value>
 */
export class WindGustStrengthCommand implements Command {
  constructor(private windManager: WindManager) {}

  getName(): string {
    return 'windGustStrength';
  }

  getDescription(): string {
    return 'Set wind gust strength (0-10)';
  }

  getHelp(): string {
    return 'Usage: windGustStrength <value>\nSet the wind gust strength (0-10).\nExample: windGustStrength 0.2  (20% gust strength)';
  }

  execute(context: CmdExecutionContext): void {
    const params = context.getParameters();

    if (params.length === 0) {
      // Show current wind gust strength
      const strength = this.windManager.getWindGustStrength();
      context.writeLine(`Current wind gust strength: ${strength.toFixed(2)}`);
      return;
    }

    const value = parseFloat(params[0]);

    if (isNaN(value)) {
      context.writeError('Invalid parameter. Value must be a number (0-10).');
      return;
    }

    if (value < 0 || value > 10) {
      context.writeError('Value out of bounds. Wind gust strength must be between 0 and 10.');
      return;
    }

    this.windManager.setWindGustStrength(value);
    const strength = this.windManager.getWindGustStrength();
    context.writeLine(`Wind gust strength set to ${strength.toFixed(2)}`);
  }
}

/**
 * windSwayFactor command - Set wind sway factor
 * Usage: windSwayFactor <value>
 */
export class WindSwayFactorCommand implements Command {
  constructor(private windManager: WindManager) {}

  getName(): string {
    return 'windSwayFactor';
  }

  getDescription(): string {
    return 'Set wind sway factor (0-5)';
  }

  getHelp(): string {
    return 'Usage: windSwayFactor <value>\nSet the wind sway factor (0-5). This is a multiplier for how much blocks sway.\nExample: windSwayFactor 1.5  (150% sway)';
  }

  execute(context: CmdExecutionContext): void {
    const params = context.getParameters();

    if (params.length === 0) {
      // Show current wind sway factor
      const factor = this.windManager.getWindSwayFactor();
      context.writeLine(`Current wind sway factor: ${factor.toFixed(2)}`);
      return;
    }

    const value = parseFloat(params[0]);

    if (isNaN(value)) {
      context.writeError('Invalid parameter. Value must be a number (0-5).');
      return;
    }

    if (value < 0 || value > 5) {
      context.writeError('Value out of bounds. Wind sway factor must be between 0 and 5.');
      return;
    }

    this.windManager.setWindSwayFactor(value);
    const factor = this.windManager.getWindSwayFactor();
    context.writeLine(`Wind sway factor set to ${factor.toFixed(2)}`);
  }
}
