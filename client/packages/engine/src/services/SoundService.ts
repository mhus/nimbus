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

  // Track currently playing step sounds per entity
  private playingStepSounds: Map<string, boolean> = new Map();

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

    logger.info('Step over event received', {
      entityId,
      blockTypeId: block.blockType.id,
      hasAudioSteps: !!block.audioSteps,
      audioStepsCount: block.audioSteps?.length || 0,
    });

    // Check if audio is enabled
    const audioService = this.appContext.services.audio;
    if (!audioService || !audioService.isAudioEnabled()) {
      logger.info('Audio disabled, skipping step sound');
      return; // Audio disabled
    }

    // Check if already playing step sound for this entity
    if (this.playingStepSounds.get(entityId)) {
      logger.info('Step sound already playing for entity, skipping', { entityId });
      return;
    }

    // Check if block has step audio
    if (!block.audioSteps || block.audioSteps.length === 0) {
      logger.info('No step audio for this block', { blockTypeId: block.blockType.id });
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

    // Mark as playing for this entity
    this.playingStepSounds.set(entityId, true);

    // Set spatial sound position to block position
    if (sound.spatialSound) {
      const pos = block.block.position;
      if (typeof sound.setPosition === 'function') {
        sound.setPosition(new Vector3(pos.x, pos.y, pos.z));
      }
    }

    // Set volume - StaticSound uses .volume property, not setVolume()
    if (typeof sound.setVolume === 'function') {
      sound.setVolume(definition.volume);
    } else {
      sound.volume = definition.volume;
    }

    try {
      sound.play();
      logger.info('sound.play() called', {
        audioPath: definition.path,
        volume: definition.volume,
        isPlaying: sound.isPlaying !== undefined ? sound.isPlaying : 'unknown',
      });
    } catch (error) {
      logger.info('Error calling sound.play()', {
        audioPath: definition.path,
        error: (error as Error).message,
      });
      this.playingStepSounds.delete(entityId);
      return;
    }

    logger.info('Playing step sound', {
      entityId,
      blockTypeId: block.blockType.id,
      audioPath: definition.path,
      volume: definition.volume,
      movementType,
      soundIsPlaying: sound.isPlaying,
    });

    // Remove playing flag when sound ends
    // Use Babylon.js Observable instead of DOM onended
    if (sound.onEndedObservable) {
      logger.info('Registering onEndedObservable callback', { audioPath: definition.path });
      const observer = sound.onEndedObservable.addOnce(() => {
        this.playingStepSounds.delete(entityId);
        logger.info('Step sound ended (onEndedObservable fired)', {
          entityId,
          audioPath: definition.path,
          stillInMap: this.playingStepSounds.has(entityId),
        });
      });
    } else {
      logger.info('No onEndedObservable, using timeout fallback');
      // Fallback: Remove flag after estimated duration
      // This shouldn't happen but provides safety
      setTimeout(() => {
        this.playingStepSounds.delete(entityId);
        logger.info('Step sound ended (timeout fallback)', { entityId });
      }, 1000);
    }
  }

  /**
   * Stop all playing sounds
   */
  stopAllSounds(): void {
    this.playingStepSounds.clear();
    logger.debug('Stopped all sounds');
  }

  /**
   * Dispose service
   */
  dispose(): void {
    this.stopAllSounds();
    logger.info('SoundService disposed');
  }
}
