/**
 * EnvironmentAmbientAudioCommand - Set environment ambient audio
 */

import { getLogger } from '@nimbus/shared';
import { CommandHandler } from './CommandHandler';
import type { AppContext } from '../AppContext';

const logger = getLogger('EnvironmentAmbientAudioCommand');

/**
 * Environment ambient audio command
 *
 * Usage:
 *   /environmentAmbientAudio <soundPath>  - Set environment ambient music
 *   /environmentAmbientAudio ""            - Clear environment ambient music
 *
 * Examples:
 *   /environmentAmbientAudio audio/music/ambient1.ogg
 *   /environmentAmbientAudio audio/music/forest.mp3
 *   /environmentAmbientAudio ""
 *
 * Note: Uses ModifierStack priority 50 (environment level)
 */
export class EnvironmentAmbientAudioCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'environmentAmbientAudio';
  }

  description(): string {
    return 'Set environment ambient audio (/environmentAmbientAudio <path>)';
  }

  async execute(parameters: any[]): Promise<any> {
    const environmentService = this.appContext.services.environment;

    if (!environmentService) {
      console.error('EnvironmentService not available');
      return { error: 'EnvironmentService not available' };
    }

    // Require soundPath parameter
    if (parameters.length === 0) {
      console.error('Usage: /environmentAmbientAudio <soundPath>');
      console.log('Example: /environmentAmbientAudio audio/music/ambient1.ogg');
      console.log('Clear ambient: /environmentAmbientAudio ""');
      return { error: 'Missing soundPath parameter' };
    }

    // Parse soundPath parameter
    const soundPath = String(parameters[0]);

    try {
      // Set environment ambient audio (uses priority 50 modifier internally)
      environmentService.setEnvironmentAmbientAudio(soundPath);

      if (soundPath.trim() === '') {
        console.log('✓ Environment ambient music cleared');
        return { status: 'cleared' };
      } else {
        console.log(`✓ Environment ambient music set: ${soundPath}`);
        console.log(`  Priority: 50 (environment level)`);
        return { status: 'set', soundPath };
      }
    } catch (error) {
      console.error('Failed to set environment ambient music', error);
      return { error: 'Failed to set environment ambient music' };
    }
  }
}
