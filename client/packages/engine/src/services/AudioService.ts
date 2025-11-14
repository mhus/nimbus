/**
 * AudioService - Manages audio loading, caching, and playback
 *
 * Responsible for:
 * - Loading audio files from the server
 * - Caching Babylon.js Sound objects
 * - Managing audio playback
 */

import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import { Sound, Scene } from '@babylonjs/core';
import type { NetworkService } from './NetworkService';

const logger = getLogger('AudioService');

/**
 * Audio cache entry
 */
interface AudioCacheEntry {
  /** Babylon.js Sound object */
  sound: Sound;
  /** Asset path */
  path: string;
  /** Load timestamp */
  loadedAt: number;
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
  private audioEnabled: boolean = true;

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
        if (entry.sound.isPlaying) {
          entry.sound.stop();
        }
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
   * Initialize audio service with scene
   * Must be called after scene is created
   */
  initialize(scene: Scene): void {
    this.scene = scene;
    this.networkService = this.appContext.services.network;

    if (!this.networkService) {
      logger.error('NetworkService not available in AppContext');
      return;
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
  ): Promise<Sound | null> {
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

      // Create Babylon.js Sound object
      const sound = new Sound(
        assetPath, // Name
        audioUrl,  // URL
        this.scene,
        () => {
          logger.debug('Audio loaded successfully', { assetPath });
        },
        {
          loop: options?.loop ?? false,
          autoplay: options?.autoplay ?? false,
          volume: options?.volume ?? 1.0,
          spatialSound: options?.spatialSound ?? false,
        }
      );

      // Cache the sound
      this.audioCache.set(assetPath, {
        sound,
        path: assetPath,
        loadedAt: Date.now(),
      });

      logger.info('Audio loaded and cached', { assetPath });
      return sound;
    } catch (error) {
      logger.error('Failed to load audio', { assetPath }, error as Error);
      return null;
    }
  }

  /**
   * Apply options to existing Sound object
   */
  private applySoundOptions(sound: Sound, options: {
    volume?: number;
    loop?: boolean;
    autoplay?: boolean;
    spatialSound?: boolean;
  }): void {
    if (options.volume !== undefined) {
      sound.setVolume(options.volume);
    }
    if (options.loop !== undefined) {
      sound.loop = options.loop;
    }
    if (options.spatialSound !== undefined) {
      sound.spatialSound = options.spatialSound;
    }
    if (options.autoplay && !sound.isPlaying) {
      sound.play();
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
  ): Promise<Sound | null> {
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
  }
}
