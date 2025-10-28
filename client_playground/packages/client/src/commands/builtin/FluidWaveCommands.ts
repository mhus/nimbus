/**
 * Fluid Wave Commands
 * Commands for controlling water and lava wave effects
 */
import type { Command } from '../Command';
import type { CmdExecutionContext } from '../CmdExecutionContext';
import type { VoxelClient } from '../../VoxelClient';

/**
 * Water Wave Command - Control water wave parameters
 */
export class WaterWaveCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'waterwave';
  }

  getDescription(): string {
    return 'Control water wave parameters';
  }

  getHelp(): string {
    return `Usage: waterwave <parameter> <value>

Controls wave animation for water blocks.

Parameters:
  speed <value>      - Wave animation speed (default: 1.0)
  amplitude <value>  - Wave height (default: 0.05)
  frequency <value>  - Wave frequency (default: 2.0)
  reset              - Reset to default values
  show               - Show current values

Examples:
  waterwave speed 2.0        - Double wave speed
  waterwave amplitude 0.2    - Increase wave height
  waterwave reset            - Reset to defaults
  waterwave show             - Show current settings`;
  }

  execute(context: CmdExecutionContext): void {
    const fluidWaveShader = (this.client as any).fluidWaveShader;

    if (!fluidWaveShader) {
      context.writeError('Fluid wave shader not available');
      return;
    }

    const param = context.getParameter(0)?.toLowerCase();
    const value = context.getParameter(1);

    if (!param) {
      context.writeError('Missing parameter. Use "waterwave help" for usage');
      return;
    }

    if (param === 'reset') {
      fluidWaveShader.resetWaterWaves();
      context.writeLine('Water waves reset to default');
      return;
    }

    if (param === 'show') {
      const waves = fluidWaveShader.getWaterWaves();
      context.writeLine('Water Wave Parameters:');
      context.writeLine(`  Speed:      ${waves.speed}`);
      context.writeLine(`  Amplitude:  ${waves.amplitude}`);
      context.writeLine(`  Frequency:  ${waves.frequency}`);
      return;
    }

    if (!value) {
      context.writeError(`Missing value for parameter "${param}"`);
      return;
    }

    const numValue = parseFloat(value);
    if (isNaN(numValue)) {
      context.writeError(`Invalid value "${value}". Must be a number`);
      return;
    }

    switch (param) {
      case 'speed':
        fluidWaveShader.setWaterWaves({ speed: numValue });
        context.writeLine(`Water wave speed set to ${numValue}`);
        break;
      case 'amplitude':
        fluidWaveShader.setWaterWaves({ amplitude: numValue });
        context.writeLine(`Water wave amplitude set to ${numValue}`);
        break;
      case 'frequency':
        fluidWaveShader.setWaterWaves({ frequency: numValue });
        context.writeLine(`Water wave frequency set to ${numValue}`);
        break;
      default:
        context.writeError(`Unknown parameter "${param}". Use "waterwave help" for available parameters`);
    }
  }
}

/**
 * Lava Wave Command - Control lava wave parameters
 */
export class LavaWaveCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'lavawave';
  }

  getDescription(): string {
    return 'Control lava wave parameters';
  }

  getHelp(): string {
    return `Usage: lavawave <parameter> <value>

Controls wave animation for lava blocks.

Parameters:
  speed <value>      - Wave animation speed (default: 0.5)
  amplitude <value>  - Wave height (default: 0.15)
  frequency <value>  - Wave frequency (default: 1.5)
  reset              - Reset to default values
  show               - Show current values

Examples:
  lavawave speed 1.0        - Increase wave speed
  lavawave amplitude 0.3    - Increase wave height
  lavawave reset            - Reset to defaults
  lavawave show             - Show current settings`;
  }

  execute(context: CmdExecutionContext): void {
    const fluidWaveShader = (this.client as any).fluidWaveShader;

    if (!fluidWaveShader) {
      context.writeError('Fluid wave shader not available');
      return;
    }

    const param = context.getParameter(0)?.toLowerCase();
    const value = context.getParameter(1);

    if (!param) {
      context.writeError('Missing parameter. Use "lavawave help" for usage');
      return;
    }

    if (param === 'reset') {
      fluidWaveShader.resetLavaWaves();
      context.writeLine('Lava waves reset to default');
      return;
    }

    if (param === 'show') {
      const waves = fluidWaveShader.getLavaWaves();
      context.writeLine('Lava Wave Parameters:');
      context.writeLine(`  Speed:      ${waves.speed}`);
      context.writeLine(`  Amplitude:  ${waves.amplitude}`);
      context.writeLine(`  Frequency:  ${waves.frequency}`);
      return;
    }

    if (!value) {
      context.writeError(`Missing value for parameter "${param}"`);
      return;
    }

    const numValue = parseFloat(value);
    if (isNaN(numValue)) {
      context.writeError(`Invalid value "${value}". Must be a number`);
      return;
    }

    switch (param) {
      case 'speed':
        fluidWaveShader.setLavaWaves({ speed: numValue });
        context.writeLine(`Lava wave speed set to ${numValue}`);
        break;
      case 'amplitude':
        fluidWaveShader.setLavaWaves({ amplitude: numValue });
        context.writeLine(`Lava wave amplitude set to ${numValue}`);
        break;
      case 'frequency':
        fluidWaveShader.setLavaWaves({ frequency: numValue });
        context.writeLine(`Lava wave frequency set to ${numValue}`);
        break;
      default:
        context.writeError(`Unknown parameter "${param}". Use "lavawave help" for available parameters`);
    }
  }
}
