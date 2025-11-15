/**
 * SoundService - Handles gameplay sound playback
 *
 * Listens to physics events and plays appropriate sounds:
 * - Step sounds when player walks over blocks
 * - Jump sounds
 * - Landing sounds
 */

import { getLogger } from '@nimbus/shared';
import { Vector3 } from '@babylonjs/core';
import type { AppContext } from '../AppContext';
import type { PhysicsService } from './PhysicsService';
import type { ClientBlock } from '../types/ClientBlock';

const logger = getLogger('SoundService');

/**
 * Step over event data
 */
interface StepOverEvent {
  entityId: string;
  block: ClientBlock;
  movementType: string;
}

/**
 * SoundService - Manages gameplay sound playback
 *
 * Subscribes to PhysicsService events and plays sounds based on block audio definitions.
 */
export class SoundService {
  private physicsService?: PhysicsService;

  constructor(private appContext: AppContext) {
    logger.info('SoundService created');
  }

  /**
   * Initialize sound service
   * Subscribes to PhysicsService events
   */
  initialize(): void {
    this.physicsService = this.appContext.services.physics;

    if (!this.physicsService) {
      logger.error('PhysicsService not available in AppContext');
      return;
    }

    // Subscribe to step over events
    this.physicsService.on('step:over', (event: StepOverEvent) => {
      this.onStepOver(event);
    });

    logger.info('SoundService initialized and subscribed to PhysicsService events');
  }

  /**
   * Handle step over event
   * Plays random step sound from block's audioSteps
   */
  private onStepOver(event: StepOverEvent): void {
    const { entityId, block, movementType } = event;

    // Check if audio is enabled
    const audioService = this.appContext.services.audio;
    if (!audioService || !audioService.isAudioEnabled()) {
      return; // Audio disabled
    }

    // Check if block has step audio
    if (!block.audioSteps || block.audioSteps.length === 0) {
      return; // No step audio for this block
    }

    // Select random step audio
    const randomIndex = Math.floor(Math.random() * block.audioSteps.length);
    const audioEntry = block.audioSteps[randomIndex];

    if (!audioEntry || !audioEntry.sound) {
      logger.warn('Invalid audio entry', { blockTypeId: block.blockType.id });
      return;
    }

    const { sound, definition } = audioEntry;

    // Set spatial sound position to block position
    if (sound.spatialSound) {
      const pos = block.block.position;
      if (typeof sound.setPosition === 'function') {
        sound.setPosition(new Vector3(pos.x, pos.y, pos.z));
      }
    }

    // Set volume - StaticSound uses .volume property, not setVolume()
    // Apply stepVolume multiplier from AudioService
    let stepVolume = audioService.getStepVolume();

    // CROUCH mode: reduce volume to 50%
    if (movementType === 'crouch') {
      stepVolume *= 0.5;
    }

    const finalVolume = definition.volume * stepVolume;

    if (typeof sound.setVolume === 'function') {
      sound.setVolume(finalVolume);
    } else if ('volume' in sound) {
      sound.volume = finalVolume;
    }

    try {
      sound.play();
    } catch (error) {
      logger.warn('Failed to play step sound', {
        audioPath: definition.path,
        error: (error as Error).message,
      });
      return;
    }
  }

  /**
   * Stop all playing sounds
   */
  stopAllSounds(): void {
    // No longer tracking playing sounds - nothing to clear
  }

  /**
   * Dispose service
   */
  dispose(): void {
    this.stopAllSounds();
    logger.info('SoundService disposed');
  }
}
