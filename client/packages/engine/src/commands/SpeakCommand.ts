/**
 * SpeakCommand - Play speech/narration audio from server
 */

import { getLogger } from '@nimbus/shared';
import { CommandHandler } from './CommandHandler';
import type { AppContext } from '../AppContext';

const logger = getLogger('SpeakCommand');

/**
 * Speak command
 *
 * Usage:
 *   /speak <streamPath> [volume]
 *
 * Examples:
 *   /speak welcome 1.0
 *   /speak tutorial/intro 0.8
 *   /speak announcement
 *
 * Note: Command waits until speech finishes or is stopped
 */
export class SpeakCommand extends CommandHandler {
  private appContext: AppContext;

  constructor(appContext: AppContext) {
    super();
    this.appContext = appContext;
  }

  name(): string {
    return 'speak';
  }

  description(): string {
    return 'Play speech/narration from server (/speak <streamPath> [volume])';
  }

  async execute(parameters: any[]): Promise<any> {
    const audioService = this.appContext.services.audio;

    if (!audioService) {
      console.error('AudioService not available');
      return { error: 'AudioService not available' };
    }

    // Require at least streamPath parameter
    if (parameters.length === 0) {
      console.error('Usage: /speak <streamPath> [volume]');
      console.log('Example: /speak welcome 1.0');
      console.log('Example: /speak tutorial/intro');
      return { error: 'Missing streamPath parameter' };
    }

    // Parse parameters
    const streamPath = String(parameters[0]);
    const volume = parameters.length > 1 ? parseFloat(String(parameters[1])) : 1.0;

    // Validate volume
    if (isNaN(volume) || volume < 0 || volume > 1) {
      console.error('Invalid volume. Must be between 0.0 and 1.0');
      return { error: 'Invalid volume parameter' };
    }

    // Play speech and wait for completion
    try {
      console.log(`🎙️ Playing speech: ${streamPath}`);
      console.log(`  Volume: ${volume}, Speech Volume: ${audioService.getSpeechVolume()}`);
      console.log('  Waiting for speech to finish...');

      // Wait for speech to complete
      await audioService.speak(streamPath, volume);

      console.log(`✓ Speech completed: ${streamPath}`);
      return { status: 'completed', streamPath, volume };
    } catch (error) {
      console.error('Failed to play speech', error);
      return { error: 'Failed to play speech' };
    }
  }
}
