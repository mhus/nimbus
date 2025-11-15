/**
 * AudioService - Manages audio loading, caching, and playback
 *
 * Responsible for:
 * - Loading audio files from the server
 * - Caching Babylon.js Sound objects
 * - Managing audio playback
 * - Handling gameplay sound playback (step sounds, swim sounds, etc.)
 */

import { getLogger } from '@nimbus/shared';
import { Vector3 } from '@babylonjs/core';
import type { AppContext } from '../AppContext';
import { Scene, Sound, Engine, CreateAudioEngineAsync, CreateSoundAsync } from '@babylonjs/core';
import type { AudioEngine, StaticSound } from '@babylonjs/core';
import type { NetworkService } from './NetworkService';
import type { PhysicsService } from './PhysicsService';
import type { ClientBlock } from '../types/ClientBlock';

const logger = getLogger('AudioService');

/**
 * Step over event data
 */
interface StepOverEvent {
  entityId: string;
  block: ClientBlock; // For swim mode: contains position but no audioSteps
  movementType: string;
}

/**
 * Audio cache entry
 */
interface AudioCacheEntry {
  /** Babylon.js Sound object (Sound or StaticSound) */
  sound: any; // Can be Sound or StaticSound
  /** Asset path */
  path: string;
  /** Load timestamp */
  loadedAt: number;
  /** Is sound ready to play (set in ready callback) */
  isReady: boolean;
}

/**
 * AudioService - Manages audio resources
 *
 * Loads audio files as Babylon.js Sound objects and caches them for reuse.
 * Integrates with NetworkService to fetch audio assets.
 */
export class AudioService {
  private audioCache: Map<string, AudioCacheEntry> = new Map();
  private scene?: Scene;
  private networkService?: NetworkService;
  private physicsService?: PhysicsService;
  private audioEnabled: boolean = true;
  private audioEngine?: any; // AudioEngineV2
  private stepVolume: number = 0.5; // Default step sound volume multiplier

  // Track last swim sound time per entity to prevent overlapping
  private lastSwimSoundTime: Map<string, number> = new Map();

  constructor(private appContext: AppContext) {
    logger.info('AudioService created');
  }

  /**
   * Enable or disable audio playback
   * Audio files are still loaded/cached but not played when disabled
   */
  setAudioEnabled(enabled: boolean): void {
    this.audioEnabled = enabled;
    logger.info('Audio playback ' + (enabled ? 'enabled' : 'disabled'));

    // Stop all playing audio when disabling
    if (!enabled) {
      this.audioCache.forEach(entry => {
        // StaticSound has stop() but not isPlaying, so just call stop()
        entry.sound.stop();
      });
    }
  }

  /**
   * Get current audio enabled state
   */
  isAudioEnabled(): boolean {
    return this.audioEnabled;
  }

  /**
   * Set step sound volume multiplier
   * @param volume Volume multiplier (0.0 = silent, 1.0 = full volume)
   */
  setStepVolume(volume: number): void {
    this.stepVolume = Math.max(0, Math.min(1, volume)); // Clamp between 0 and 1
    logger.info('Step volume set to ' + this.stepVolume);
  }

  /**
   * Get current step sound volume multiplier
   */
  getStepVolume(): number {
    return this.stepVolume;
  }

  /**
   * Initialize audio service with scene
   * Must be called after scene is created
   * Also subscribes to PhysicsService events for gameplay sounds
   */
  async initialize(scene: Scene): Promise<void> {
    this.scene = scene;
    this.networkService = this.appContext.services.network;
    this.physicsService = this.appContext.services.physics;

    if (!this.networkService) {
      logger.error('NetworkService not available in AppContext');
      return;
    }

    // Create audio engine using async API
    try {
      this.audioEngine = await CreateAudioEngineAsync();

      // Unlock audio engine (waits for user interaction if needed)
      if (this.audioEngine && !this.audioEngine.unlocked) {
        logger.info('Audio engine locked - waiting for user interaction');

        // Unlock in background - don't block initialization
        this.audioEngine.unlockAsync().then(() => {
          logger.info('Audio engine unlocked and ready');
        }).catch((error: any) => {
          logger.error('Failed to unlock audio engine', {}, error);
        });
      } else if (this.audioEngine) {
        logger.info('Audio engine ready');
      }
    } catch (error) {
      logger.error('Failed to create audio engine', {}, error as Error);
    }

    // Subscribe to PhysicsService events for gameplay sounds
    if (this.physicsService) {
      this.physicsService.on('step:over', (event: StepOverEvent) => {
        this.onStepOver(event);
      });
      logger.info('AudioService subscribed to PhysicsService events');
    } else {
      logger.warn('PhysicsService not available - gameplay sounds will not work');
    }

    logger.info('AudioService initialized with scene');
  }

  /**
   * Load audio file and return Babylon.js Sound object
   * Uses cache if audio was previously loaded
   *
   * @param assetPath Path to audio asset (e.g., "audio/step/grass.ogg")
   * @param options Optional Babylon.js Sound options
   * @returns Sound object or null if loading failed
   */
  async loadAudio(
    assetPath: string,
    options?: {
      volume?: number;
      loop?: boolean;
      autoplay?: boolean;
      spatialSound?: boolean;
    }
  ): Promise<any> {
    if (!this.scene) {
      logger.error('Scene not initialized');
      return null;
    }

    if (!this.networkService) {
      logger.error('NetworkService not available');
      return null;
    }

    // Check cache
    const cached = this.audioCache.get(assetPath);
    if (cached) {
      logger.debug('Audio loaded from cache', { assetPath });

      // Apply options to cached sound
      if (options) {
        this.applySoundOptions(cached.sound, options);
      }

      return cached.sound;
    }

    // Load new audio
    try {
      const audioUrl = this.networkService.getAssetUrl(assetPath);

      logger.debug('Loading audio', { assetPath, audioUrl });

      // Create Babylon.js Sound object using async API
      const sound = await CreateSoundAsync(assetPath, audioUrl);

      // Apply options - StaticSound uses direct properties
      if (options?.loop !== undefined) {
        sound.loop = options.loop;
      }
      if (options?.volume !== undefined) {
        sound.volume = options.volume;
      }
      if (options?.autoplay) {
        sound.play();
      }

      logger.debug('Sound loaded', { assetPath });

      // Cache the sound immediately (it will load in background)
      this.audioCache.set(assetPath, {
        sound,
        path: assetPath,
        loadedAt: Date.now(),
        isReady: true, // Set to true - Babylon.js handles loading, we can call play() anytime
      });

      logger.debug('Audio cached', { assetPath });
      return sound;
    } catch (error) {
      logger.warn('Failed to load audio', { assetPath, error: (error as Error).message });
      return null;
    }
  }

  /**
   * Apply options to existing Sound object
   * Works with both Sound and StaticSound
   */
  private applySoundOptions(sound: any, options: {
    volume?: number;
    loop?: boolean;
    autoplay?: boolean;
    spatialSound?: boolean;
  }): void {
    if (options.volume !== undefined) {
      // StaticSound uses .volume property, Sound uses setVolume()
      if (typeof sound.setVolume === 'function') {
        sound.setVolume(options.volume);
      } else {
        sound.volume = options.volume;
      }
    }
    if (options.loop !== undefined) {
      sound.loop = options.loop;
    }
    if (options.spatialSound !== undefined && 'spatialSound' in sound) {
      sound.spatialSound = options.spatialSound;
    }
    if (options.autoplay) {
      // StaticSound doesn't have isPlaying property
      const shouldPlay = !sound.isPlaying || sound.isPlaying === undefined;
      if (shouldPlay) {
        sound.play();
      }
    }
  }

  /**
   * Play audio by asset path
   * Loads audio if not already cached
   * Respects audioEnabled flag
   *
   * @param assetPath Path to audio asset
   * @param options Playback options
   * @returns Sound object or null if loading failed or audio disabled
   */
  async playAudio(
    assetPath: string,
    options?: {
      volume?: number;
      loop?: boolean;
    }
  ): Promise<any> {
    // Check if audio is enabled
    if (!this.audioEnabled) {
      logger.debug('Audio playback disabled, skipping', { assetPath });
      return null;
    }

    const sound = await this.loadAudio(assetPath, {
      ...options,
      autoplay: false, // Don't autoplay, we control it below
    });

    if (sound && !sound.isPlaying && this.audioEnabled) {
      sound.play();
    }

    return sound;
  }

  /**
   * Stop audio by asset path
   *
   * @param assetPath Path to audio asset
   */
  stopAudio(assetPath: string): void {
    const cached = this.audioCache.get(assetPath);
    if (cached && cached.sound.isPlaying) {
      cached.sound.stop();
      logger.debug('Audio stopped', { assetPath });
    }
  }

  /**
   * Stop all playing audio
   */
  stopAllAudio(): void {
    let stoppedCount = 0;
    this.audioCache.forEach((entry) => {
      if (entry.sound.isPlaying) {
        entry.sound.stop();
        stoppedCount++;
      }
    });

    if (stoppedCount > 0) {
      logger.debug('Stopped all audio', { count: stoppedCount });
    }
  }

  /**
   * Check if audio is ready to play
   * Uses cache entry flag instead of sound.isReady() for reliability
   */
  isAudioReady(assetPath: string): boolean {
    const cached = this.audioCache.get(assetPath);
    return cached?.isReady ?? false;
  }

  /**
   * Get cached audio count
   */
  getCacheSize(): number {
    return this.audioCache.size;
  }

  /**
   * Clear audio cache
   * Disposes all cached Sound objects
   */
  clearCache(): void {
    logger.info('Clearing audio cache', { count: this.audioCache.size });

    this.audioCache.forEach((entry) => {
      entry.sound.dispose();
    });

    this.audioCache.clear();
  }

  /**
   * Dispose service and cleanup resources
   */
  dispose(): void {
    logger.info('Disposing AudioService');
    this.clearCache();
    this.lastSwimSoundTime.clear();
  }

  // ========================================
  // Gameplay Sound Methods (from SoundService)
  // ========================================

  /**
   * Handle step over event
   * Plays random step sound from block's audioSteps
   */
  private onStepOver(event: StepOverEvent): void {
    const { entityId, block, movementType } = event;

    // Check if audio is enabled
    if (!this.audioEnabled) {
      return; // Audio disabled
    }

    // SWIM mode: play special swim sound
    if (movementType === 'swim') {
      this.playSwimSound(entityId, block);
      return;
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
    let stepVolume = this.stepVolume;

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
   * Play swim sound at player position
   * Prevents overlapping by checking if sound was played recently
   */
  private async playSwimSound(entityId: string, block: ClientBlock): Promise<void> {
    const swimSoundPath = 'audio/liquid/swim1.ogg';

    // Check if swim sound was played recently (prevent overlapping)
    // Swim sounds are typically 500-1000ms long, so wait at least 500ms
    const now = Date.now();
    const lastPlayTime = this.lastSwimSoundTime.get(entityId);
    if (lastPlayTime && now - lastPlayTime < 500) {
      return; // Still playing from last time
    }

    // Load swim sound
    const sound = await this.loadAudio(swimSoundPath, {
      spatialSound: true,
      loop: false,
      autoplay: false,
    });

    if (!sound) {
      return; // Failed to load
    }

    // Set position to player/entity position (from block.block.position)
    if (sound.spatialSound && block.block?.position) {
      const pos = block.block.position;
      if (typeof sound.setPosition === 'function') {
        sound.setPosition(new Vector3(pos.x, pos.y, pos.z));
      }
    }

    // Apply stepVolume multiplier
    const stepVolume = this.stepVolume;
    const finalVolume = 1.0 * stepVolume; // Full volume for swim sounds

    if (typeof sound.setVolume === 'function') {
      sound.setVolume(finalVolume);
    } else if ('volume' in sound) {
      sound.volume = finalVolume;
    }

    // Update last play time
    this.lastSwimSoundTime.set(entityId, now);

    try {
      sound.play();
    } catch (error) {
      logger.warn('Failed to play swim sound', {
        audioPath: swimSoundPath,
        error: (error as Error).message,
      });
    }
  }
}
