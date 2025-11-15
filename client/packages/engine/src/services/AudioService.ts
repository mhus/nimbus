/**
 * AudioService - Manages audio loading, caching, and playback
 *
 * Responsible for:
 * - Loading audio files from the server
 * - Caching Babylon.js Sound objects
 * - Managing audio playback
 * - Handling gameplay sound playback (step sounds, swim sounds, etc.)
 */

import { getLogger, type AudioDefinition } from '@nimbus/shared';
import { Vector3 } from '@babylonjs/core';
import type { AppContext } from '../AppContext';
import { Scene, Sound, Engine, CreateAudioEngineAsync, CreateSoundAsync } from '@babylonjs/core';
import type { AudioEngine, StaticSound } from '@babylonjs/core';
import type { NetworkService } from './NetworkService';
import type { PhysicsService } from './PhysicsService';
import type { ClientBlock } from '../types/ClientBlock';

const logger = getLogger('AudioService');

// Constants for spatial audio
const DEFAULT_MAX_DISTANCE = 15;
const DEFAULT_INITIAL_POOL_SIZE = 1;
const STEP_SOUND_INITIAL_POOL_SIZE = 3;
const POOL_MAX_SIZE = 10; // Maximum pool size per sound

/**
 * Step over event data
 */
interface StepOverEvent {
  entityId: string;
  block: ClientBlock; // For swim mode: contains position but no audioSteps
  movementType: string;
}

/**
 * Audio cache entry (legacy, for non-spatial sounds)
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
 * AudioPoolItem - Manages a single sound instance in the pool
 * Handles blocking, spatial configuration, and auto-release
 */
class AudioPoolItem {
  public sound: any; // StaticSound
  public inUse: boolean = false;
  public blockedAt: number = 0; // Timestamp when blocked
  public lastUsed: number = 0; // Timestamp when last used

  constructor(sound: any) {
    this.sound = sound;
  }

  /**
   * Blocks the instance for playback
   * Sets spatial parameters and registers onEndedObservable for auto-release
   * @param position 3D position for spatial sound
   * @param maxDistance Maximum hearing distance
   * @param onReleaseCallback Callback when released
   */
  public block(
    position: Vector3,
    maxDistance: number,
    onReleaseCallback: () => void
  ): void {
    this.inUse = true;
    this.blockedAt = Date.now();

    // Spatial configuration
    this.configureSpatial(position, maxDistance);

    // Auto-release via onEndedObservable (Babylon.js Observable pattern)
    if (this.sound.onEndedObservable) {
      this.sound.onEndedObservable.addOnce(() => {
        logger.debug('onEndedObservable fired, releasing item');
        this.release();
        onReleaseCallback();
      });
      logger.debug('onEndedObservable registered');
    } else {
      // Fallback: release after 1 second if Observable not available
      logger.warn('onEndedObservable not available, using timeout fallback');
      setTimeout(() => {
        logger.debug('Timeout fallback fired, releasing item');
        this.release();
        onReleaseCallback();
      }, 1000);
    }
  }

  /**
   * Releases the instance back to the pool (available)
   */
  public release(): void {
    this.inUse = false;
    this.lastUsed = Date.now();
    this.blockedAt = 0;
  }

  /**
   * Configures spatial audio parameters
   * StaticSound uses sound.spatial object for configuration
   * Omnidirectional sound (equal in all directions from position)
   */
  private configureSpatial(position: Vector3, maxDistance: number): void {
    // StaticSound has a spatial property
    if (this.sound.spatial) {
      this.sound.spatial.position = position;
      this.sound.spatial.maxDistance = maxDistance;
      this.sound.spatial.distanceModel = 'exponential';
      this.sound.spatial.refDistance = 1;
      this.sound.spatial.rolloffFactor = 1;

      // Omnidirectional sound (no cone, equal in all directions)
      this.sound.spatial.coneInnerAngle = 2 * Math.PI; // 360 degrees
      this.sound.spatial.coneOuterAngle = 2 * Math.PI; // 360 degrees
      this.sound.spatial.coneOuterGain = 1.0; // Full volume in all directions

      logger.debug('Spatial audio configured (StaticSound API)', {
        position: { x: position.x, y: position.y, z: position.z },
        maxDistance,
        distanceModel: 'exponential',
        omnidirectional: true
      });
    } else {
      // Fallback for regular Sound (if used)
      this.sound.spatialSound = true;
      this.sound.distanceModel = 'exponential';
      this.sound.maxDistance = maxDistance;
      this.sound.refDistance = 1;
      this.sound.rolloffFactor = 1;

      if (typeof this.sound.setPosition === 'function') {
        this.sound.setPosition(position);
      }

      logger.debug('Spatial audio configured (legacy Sound API)', {
        position: { x: position.x, y: position.y, z: position.z },
        maxDistance
      });
    }
  }

  /**
   * Sets volume and starts playback
   */
  public play(volume: number): void {
    this.sound.volume = volume;

    try {
      this.sound.play();
    } catch (error) {
      logger.warn('Failed to play sound', { error: (error as Error).message });
      this.release(); // Release on error
    }
  }

  /**
   * Checks if instance is available
   */
  public isAvailable(): boolean {
    return !this.inUse;
  }

  /**
   * Cleanup - dispose sound
   */
  public dispose(): void {
    this.sound?.dispose();
  }
}

/**
 * AudioPool - Manages pool of AudioPoolItems for a sound path
 */
class AudioPool {
  public path: string;
  public audioUrl: string; // URL for creating new instances
  public items: AudioPoolItem[] = [];
  public loadedAt: number;

  constructor(path: string, audioUrl: string, initialSounds: any[]) {
    this.path = path;
    this.audioUrl = audioUrl;
    this.loadedAt = Date.now();

    // Add initial sounds to pool
    for (const sound of initialSounds) {
      this.items.push(new AudioPoolItem(sound));
    }

    logger.debug('AudioPool created', { path, initialSize: initialSounds.length });
  }

  /**
   * Gets available item from pool or creates new via CreateSoundAsync
   * Returns null if pool is at max capacity and no items are available
   */
  public async getAvailableItem(): Promise<AudioPoolItem | null> {
    // Find free item
    let item = this.items.find(item => item.isAvailable());

    // No free item → check if we can grow pool
    if (!item) {
      // Check max pool size
      if (this.items.length >= POOL_MAX_SIZE) {
        // Before giving up, check for stuck items (blocked >1 second)
        const now = Date.now();
        const stuckItems = this.items.filter(
          item => item.inUse && item.blockedAt > 0 && (now - item.blockedAt) > 1000
        );

        if (stuckItems.length > 0) {
          logger.warn('Found stuck items in pool, releasing them', {
            path: this.path,
            stuckCount: stuckItems.length,
            totalItems: this.items.length
          });

          // Force release stuck items
          stuckItems.forEach(stuckItem => {
            stuckItem.release();
          });

          // Try to find free item again
          item = this.items.find(item => item.isAvailable());

          if (item) {
            logger.info('Recovered stuck item from pool', { path: this.path });
            return item;
          }
        }

        // Still no free item after cleanup
        logger.warn('Pool at maximum capacity, sound skipped', {
          path: this.path,
          maxSize: POOL_MAX_SIZE,
          available: this.getAvailableCount()
        });
        return null; // Pool is full, skip this sound
      }

      // Grow pool
      logger.debug('Pool full, creating new instance', { path: this.path });
      const newSound = await CreateSoundAsync(this.path, this.audioUrl);
      item = new AudioPoolItem(newSound);
      this.items.push(item);
      logger.debug('Pool grown', { path: this.path, newSize: this.items.length });
    }

    return item;
  }

  /**
   * Returns number of available items
   */
  public getAvailableCount(): number {
    return this.items.filter(item => item.isAvailable()).length;
  }

  /**
   * Cleanup - dispose all items
   */
  public dispose(): void {
    this.items.forEach(item => item.dispose());
    this.items = [];
  }
}

/**
 * AudioService - Manages audio resources
 *
 * Loads audio files as Babylon.js Sound objects and caches them for reuse.
 * Integrates with NetworkService to fetch audio assets.
 */
export class AudioService {
  private audioCache: Map<string, AudioCacheEntry> = new Map(); // Legacy cache for non-spatial sounds
  private soundPools: Map<string, AudioPool> = new Map(); // Pool system for spatial sounds
  private scene?: Scene;
  private networkService?: NetworkService;
  private physicsService?: PhysicsService;
  private audioEnabled: boolean = true;
  private audioEngine?: any; // AudioEngineV2
  private stepVolume: number = 1.0; // Default step sound volume multiplier
  private ambientVolume: number = 0.5; // Default ambient music volume multiplier
  private speechVolume: number = 1.0; // Default speech volume multiplier

  // Track last swim sound time per entity to prevent overlapping
  private lastSwimSoundTime: Map<string, number> = new Map();

  // Ambient music
  private currentAmbientSound?: any; // Current ambient music sound
  private currentAmbientPath?: string; // Current ambient music path
  private ambientFadeInterval?: number; // Fade in/out interval ID

  // Speech/narration
  private currentSpeech?: any; // Current speech sound
  private currentSpeechPath?: string; // Current speech stream path

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
   * Set ambient music volume multiplier
   * @param volume Volume multiplier (0.0 = silent, 1.0 = full volume)
   */
  setAmbientVolume(volume: number): void {
    this.ambientVolume = Math.max(0, Math.min(1, volume)); // Clamp between 0 and 1
    logger.info('Ambient volume set to ' + this.ambientVolume);

    // Update current ambient sound volume if playing
    if (this.currentAmbientSound && this.ambientVolume > 0) {
      this.currentAmbientSound.volume = this.ambientVolume;
    } else if (this.currentAmbientSound && this.ambientVolume <= 0) {
      // Stop ambient if volume is 0 or below
      this.stopAmbientSound();
    }
  }

  /**
   * Get current ambient music volume multiplier
   */
  getAmbientVolume(): number {
    return this.ambientVolume;
  }

  /**
   * Set speech volume multiplier
   * @param volume Volume multiplier (0.0 = silent, 1.0 = full volume)
   */
  setSpeechVolume(volume: number): void {
    this.speechVolume = Math.max(0, Math.min(1, volume)); // Clamp between 0 and 1
    logger.info('Speech volume set to ' + this.speechVolume);

    // Update current speech volume if playing
    if (this.currentSpeech) {
      this.currentSpeech.volume = this.speechVolume;
    }
  }

  /**
   * Get current speech volume multiplier
   */
  getSpeechVolume(): number {
    return this.speechVolume;
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

      // Attach audio listener to active camera for spatial audio
      if (this.audioEngine && this.audioEngine.listener && scene.activeCamera) {
        this.audioEngine.listener.spatial.attach(scene.activeCamera);
        logger.info('Audio listener attached to camera', {
          cameraName: scene.activeCamera.name
        });
      } else {
        logger.warn('Could not attach audio listener to camera', {
          hasEngine: !!this.audioEngine,
          hasListener: !!this.audioEngine?.listener,
          hasCamera: !!scene.activeCamera
        });
      }

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
   * Load sound into pool with initial pool size (forecast)
   * @param path Audio asset path
   * @param initialPoolSize Initial number of instances (default: 1)
   */
  async loadSoundIntoPool(path: string, initialPoolSize = DEFAULT_INITIAL_POOL_SIZE): Promise<void> {
    // Already loaded?
    if (this.soundPools.has(path)) {
      logger.debug('Sound already in pool', { path });
      return;
    }

    if (!this.networkService) {
      logger.error('NetworkService not available - cannot load sound into pool', { path });
      return;
    }

    if (!this.scene) {
      logger.error('Scene not initialized - cannot load sound into pool', { path });
      return;
    }

    try {
      // Get audio URL
      const audioUrl = this.networkService.getAssetUrl(path);
      logger.debug('Loading sound into pool', { path, audioUrl, initialPoolSize });

      // Load initial sound instances
      const initialSounds: any[] = [];
      for (let i = 0; i < initialPoolSize; i++) {
        const sound = await CreateSoundAsync(path, audioUrl);
        initialSounds.push(sound);
        logger.debug('Sound instance created', { path, instance: i + 1, total: initialPoolSize });
      }

      // Create AudioPool with pre-loaded sounds
      const pool = new AudioPool(path, audioUrl, initialSounds);
      this.soundPools.set(path, pool);

      logger.debug('Sound loaded into pool', { path, initialPoolSize });
    } catch (error) {
      logger.error('Failed to load sound into pool', {
        path,
        error: (error as Error).message,
        stack: (error as Error).stack
      });
    }
  }

  /**
   * Get blocked AudioPoolItem from pool
   * @param path Audio asset path
   * @param position 3D position for spatial sound
   * @param maxDistance Maximum hearing distance
   * @returns AudioPoolItem or null if failed
   */
  async getBlockedSoundFromPool(
    path: string,
    position: Vector3,
    maxDistance: number
  ): Promise<AudioPoolItem | null> {
    // Pool doesn't exist → lazy load with default size
    if (!this.soundPools.has(path)) {
      logger.debug('Pool does not exist, loading sound', { path });
      await this.loadSoundIntoPool(path, DEFAULT_INITIAL_POOL_SIZE);

      // Check if pool was created successfully
      if (!this.soundPools.has(path)) {
        logger.warn('Failed to create pool after loading attempt', { path });
        return null;
      }
    }

    const pool = this.soundPools.get(path);
    if (!pool) {
      logger.error('Failed to get pool (should not happen)', { path });
      return null;
    }

    // Get available item from pool (async now)
    const item = await pool.getAvailableItem();

    // Pool at max capacity, no available items
    if (!item) {
      logger.debug('No available items in pool', {
        path,
        poolSize: pool.items.length,
        available: pool.getAvailableCount()
      });
      return null;
    }

    // Block item with onRelease callback
    const onReleaseCallback = () => {
      logger.debug('AudioPoolItem released', { path });
    };

    item.block(position, maxDistance, onReleaseCallback);

    return item;
  }

  /**
   * Load audio file and return Babylon.js Sound object
   * Uses cache if audio was previously loaded
   * NOTE: Legacy method for non-spatial sounds (UI, music, etc.)
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
   * Creates a permanent (non-cached) spatial sound for a block
   * Used for ambient sounds that play continuously while the block is visible
   * Audio is streamed and looped automatically (Babylon.js handles streaming for large files)
   *
   * @param block Block to attach sound to
   * @param audioDef Audio definition with path, volume, loop, etc.
   * @returns Babylon.js Sound object (implements IDisposable)
   */
  async createPermanentSoundForBlock(block: ClientBlock, audioDef: AudioDefinition): Promise<any> {
    if (!this.scene) {
      logger.error('Scene not initialized');
      return null;
    }

    if (!this.networkService) {
      logger.error('NetworkService not available');
      return null;
    }

    try {
      const audioUrl = this.networkService.getAssetUrl(audioDef.path);
      const blockPos = block.block.position;
      const blockPosition = new Vector3(blockPos.x, blockPos.y, blockPos.z);

      logger.debug('Creating permanent sound for block', {
        path: audioDef.path,
        position: blockPosition,
        volume: audioDef.volume,
        loop: audioDef.loop
      });

      // Create spatial Sound directly (not StaticSound via CreateSoundAsync)
      // Sound class supports spatial audio with setPosition()
      const sound = new Sound(
        audioDef.path,
        audioUrl,
        this.scene,
        null, // Ready callback
        {
          loop: audioDef.loop !== false, // Default to true for permanent sounds
          autoplay: false,
          spatialSound: true,
          maxDistance: audioDef.maxDistance || DEFAULT_MAX_DISTANCE,
          distanceModel: 'linear',
          rolloffFactor: 1,
        }
      );

      // Set position and volume
      sound.setPosition(blockPosition);
      sound.setVolume(audioDef.volume);

      logger.debug('Permanent sound created', { path: audioDef.path, position: blockPosition });

      return sound;
    } catch (error) {
      logger.warn('Failed to create permanent sound for block', {
        path: audioDef.path,
        blockPos: block.block.position,
        error: (error as Error).message
      });
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

    // Stop ambient music
    this.stopAmbientSound();

    // Dispose legacy cache
    this.clearCache();

    // Dispose all sound pools
    this.soundPools.forEach(pool => pool.dispose());
    this.soundPools.clear();

    // Clear swim sound throttle
    this.lastSwimSoundTime.clear();
  }

  // ========================================
  // General Sound Playback Methods
  // ========================================

  /**
   * Play sound directly (non-spatial, non-looping)
   * Useful for UI sounds, notifications, or sounds that should play at player position
   * @param soundPath Path to sound file
   * @param stream Whether to stream the audio (default: false for small sounds)
   * @param volume Volume (0.0 - 1.0)
   */
  async playSound(
    soundPath: string,
    stream: boolean = false,
    volume: number = 1.0
  ): Promise<void> {
    // Validate volume
    if (volume < 0 || volume > 1) {
      logger.warn('Invalid volume, clamping to 0-1 range', { volume });
      volume = Math.max(0, Math.min(1, volume));
    }

    // Check if audio is enabled
    if (!this.audioEnabled) {
      logger.debug('Audio disabled, skipping playSound', { soundPath });
      return;
    }

    try {
      // Load sound (non-spatial, non-looping, one-shot)
      const sound = await this.loadAudio(soundPath, {
        volume,
        loop: false,
        autoplay: true, // Play immediately
        spatialSound: false, // Non-spatial (plays directly at listener)
      });

      if (!sound) {
        logger.warn('Failed to load sound', { soundPath });
        return;
      }

      logger.info('Playing non-spatial sound', { soundPath, volume, stream });
    } catch (error) {
      logger.error('Failed to play sound', {
        soundPath,
        error: (error as Error).message
      });
    }
  }

  /**
   * Play sound at specific world position (spatial, non-looping)
   * @param soundPath Path to sound file
   * @param x World X coordinate
   * @param y World Y coordinate
   * @param z World Z coordinate
   * @param volume Volume (0.0 - 1.0)
   */
  async playSoundAtPosition(
    soundPath: string,
    x: number,
    y: number,
    z: number,
    volume: number = 1.0
  ): Promise<void> {
    // Validate volume
    if (volume < 0 || volume > 1) {
      logger.warn('Invalid volume, clamping to 0-1 range', { volume });
      volume = Math.max(0, Math.min(1, volume));
    }

    // Check if audio is enabled
    if (!this.audioEnabled) {
      logger.debug('Audio disabled, skipping playSoundAtPosition', { soundPath });
      return;
    }

    // Position
    const position = new Vector3(x, y, z);

    // Get blocked sound from pool
    const item = await this.getBlockedSoundFromPool(
      soundPath,
      position,
      DEFAULT_MAX_DISTANCE
    );

    if (!item) {
      logger.warn('Failed to get sound from pool for playSoundAtPosition', { soundPath });
      return;
    }

    // Play sound (no loop, one-shot)
    item.play(volume);

    logger.info('Playing sound at position', {
      soundPath,
      position: { x, y, z },
      volume
    });
  }

  // ========================================
  // Ambient Music Methods
  // ========================================

  /**
   * Play ambient background music with fade in
   * @param soundPath Path to ambient music file (empty string stops ambient music)
   * @param stream Whether to stream the audio (default: true for large music files)
   * @param volume Volume (0.0 - 1.0), multiplied by ambientVolume
   */
  async playAmbientSound(soundPath: string, stream: boolean = true, volume: number = 1.0): Promise<void> {
    // Empty path → stop ambient music
    if (!soundPath || soundPath.trim() === '') {
      await this.stopAmbientSound();
      return;
    }

    // Check if ambientVolume is 0 or below → don't play
    if (this.ambientVolume <= 0) {
      logger.info('Ambient volume is 0 or below, not playing ambient music', { soundPath });
      return;
    }

    // Stop current ambient music if playing different track
    if (this.currentAmbientSound && this.currentAmbientPath !== soundPath) {
      await this.stopAmbientSound();
    }

    // Already playing this track → don't restart
    if (this.currentAmbientPath === soundPath && this.currentAmbientSound) {
      logger.info('Ambient music already playing', { soundPath });
      return;
    }

    try {
      logger.info('Loading ambient music', { soundPath, stream, volume });

      // Load ambient music (non-spatial, looping)
      const sound = await this.loadAudio(soundPath, {
        volume: 0, // Start at 0 for fade in
        loop: true, // Always loop ambient music
        autoplay: false,
        spatialSound: false, // Ambient music is non-spatial
      });

      if (!sound) {
        logger.error('Failed to load ambient music', { soundPath });
        return;
      }

      this.currentAmbientSound = sound;
      this.currentAmbientPath = soundPath;

      // Start playing
      sound.play();

      // Fade in
      const targetVolume = volume * this.ambientVolume;
      await this.fadeSound(sound, 0, targetVolume, 2000); // 2 second fade in

      logger.info('Ambient music playing', { soundPath, volume: targetVolume });
    } catch (error) {
      logger.error('Failed to play ambient music', { soundPath, error: (error as Error).message });
    }
  }

  /**
   * Stop ambient background music with fade out
   */
  async stopAmbientSound(): Promise<void> {
    if (!this.currentAmbientSound) {
      return; // No ambient music playing
    }

    logger.info('Stopping ambient music', { path: this.currentAmbientPath });

    // Fade out
    const currentVolume = this.currentAmbientSound.volume;
    await this.fadeSound(this.currentAmbientSound, currentVolume, 0, 1000); // 1 second fade out

    // Stop and dispose
    this.currentAmbientSound.stop();
    this.currentAmbientSound = undefined;
    this.currentAmbientPath = undefined;

    logger.info('Ambient music stopped');
  }

  /**
   * Fade sound volume from start to end over duration
   * @param sound Sound object
   * @param startVolume Starting volume (0.0 - 1.0)
   * @param endVolume Target volume (0.0 - 1.0)
   * @param duration Duration in milliseconds
   */
  private fadeSound(sound: any, startVolume: number, endVolume: number, duration: number): Promise<void> {
    return new Promise((resolve) => {
      const steps = 50; // Number of fade steps
      const stepDuration = duration / steps;
      const volumeStep = (endVolume - startVolume) / steps;

      let currentStep = 0;
      sound.volume = startVolume;

      // Clear any existing fade interval
      if (this.ambientFadeInterval) {
        clearInterval(this.ambientFadeInterval);
      }

      this.ambientFadeInterval = window.setInterval(() => {
        currentStep++;

        if (currentStep >= steps) {
          sound.volume = endVolume;
          clearInterval(this.ambientFadeInterval!);
          this.ambientFadeInterval = undefined;
          resolve();
        } else {
          sound.volume = startVolume + (volumeStep * currentStep);
        }
      }, stepDuration);
    });
  }

  // ========================================
  // Speech/Narration Methods
  // ========================================

  /**
   * Play speech/narration audio (streamed from server)
   * Only one speech can play at a time - new speech stops current
   * Returns promise that resolves when speech ends or is stopped
   *
   * @param streamPath Speech stream path (e.g., "welcome", "tutorial/intro")
   * @param volume Volume (0.0 - 1.0), multiplied by speechVolume
   * @returns Promise that resolves when speech ends
   */
  async speak(streamPath: string, volume: number = 1.0): Promise<void> {
    // Stop current speech if playing
    if (this.currentSpeech) {
      await this.stopSpeech();
    }

    // Check if speechVolume is 0 or below → don't play
    if (this.speechVolume <= 0) {
      logger.info('Speech volume is 0 or below, not playing speech', { streamPath });
      return;
    }

    if (!this.networkService) {
      logger.error('NetworkService not available - cannot get speech URL');
      return;
    }

    try {
      // Get speech URL from NetworkService
      const speechUrl = this.networkService.getSpeechUrl(streamPath);
      logger.info('Loading speech', { streamPath, speechUrl, volume });

      // Load speech (non-spatial, non-looping, streamed)
      const sound = await CreateSoundAsync(streamPath, speechUrl);

      if (!sound) {
        logger.error('Failed to load speech', { streamPath });
        return;
      }

      this.currentSpeech = sound;
      this.currentSpeechPath = streamPath;

      // Set volume
      const finalVolume = volume * this.speechVolume;
      sound.volume = finalVolume;

      // Return promise that resolves when speech ends
      return new Promise<void>((resolve) => {
        // Register onEnded callback
        if (sound.onEndedObservable) {
          sound.onEndedObservable.addOnce(() => {
            logger.info('Speech ended', { streamPath });
            this.currentSpeech = undefined;
            this.currentSpeechPath = undefined;
            resolve();
          });
        } else {
          // Fallback: assume speech ended after 60 seconds max
          logger.warn('onEndedObservable not available for speech, using 60s timeout');
          setTimeout(() => {
            logger.info('Speech timeout reached', { streamPath });
            this.currentSpeech = undefined;
            this.currentSpeechPath = undefined;
            resolve();
          }, 60000); // 60 second timeout
        }

        // Start playing
        sound.play();
        logger.info('Speech playing', { streamPath, volume: finalVolume });
      });
    } catch (error) {
      logger.error('Failed to play speech', { streamPath, error: (error as Error).message });
      this.currentSpeech = undefined;
      this.currentSpeechPath = undefined;
      throw error;
    }
  }

  /**
   * Stop current speech playback
   */
  async stopSpeech(): Promise<void> {
    if (!this.currentSpeech) {
      return; // No speech playing
    }

    logger.info('Stopping speech', { path: this.currentSpeechPath });

    // Stop immediately (no fade for speech)
    this.currentSpeech.stop();
    this.currentSpeech = undefined;
    this.currentSpeechPath = undefined;

    logger.info('Speech stopped');
  }

  // ========================================
  // Gameplay Sound Methods (from SoundService)
  // ========================================

  /**
   * Handle step over event
   * Plays random step sound from block's audioSteps using pool system
   */
  private async onStepOver(event: StepOverEvent): Promise<void> {
    const { entityId, block, movementType } = event;

    // Check if audio is enabled
    if (!this.audioEnabled) {
      return; // Audio disabled
    }

    // SWIM mode: play special swim sound
    if (movementType === 'swim') {
      await this.playSwimSound(entityId, block);
      return;
    }

    // Check if block has step audio
    if (!block.audioSteps || block.audioSteps.length === 0) {
      return; // No step audio for this block
    }

    // Select random step audio
    const randomIndex = Math.floor(Math.random() * block.audioSteps.length);
    const audioEntry = block.audioSteps[randomIndex];

    if (!audioEntry || !audioEntry.definition) {
      logger.warn('Invalid audio entry', { blockTypeId: block.blockType.id });
      return;
    }

    // Get configuration
    const maxDistance = audioEntry.definition.maxDistance ?? DEFAULT_MAX_DISTANCE;
    const position = new Vector3(
      block.block.position.x,
      block.block.position.y,
      block.block.position.z
    );

    // Get blocked sound from pool (auto-released via onEndedObservable)
    const item = await this.getBlockedSoundFromPool(
      audioEntry.definition.path,
      position,
      maxDistance
    );

    if (!item) {
      logger.warn('Failed to get sound from pool', { path: audioEntry.definition.path });
      return;
    }

    // Volume calculation
    let volumeMultiplier = this.stepVolume;

    // CROUCH mode: reduce volume to 50%
    if (movementType === 'crouch') {
      volumeMultiplier *= 0.5;
    }

    const finalVolume = audioEntry.definition.volume * volumeMultiplier;

    // Play sound (automatically released via onended callback in AudioPoolItem)
    item.play(finalVolume);
  }

  /**
   * Play swim sound at player position using pool system
   * Prevents overlapping by checking if sound was played recently
   * Sound path is read from WorldInfo.settings.swimStepAudio
   */
  private async playSwimSound(entityId: string, block: ClientBlock): Promise<void> {
    // Get swim sound path from WorldInfo settings (optional)
    const settings = this.appContext.worldInfo?.settings as any;
    const swimSoundPath = settings?.swimStepAudio || 'audio/liquid/swim1.ogg'; // Fallback to default

    // Skip if no swim sound configured
    if (!swimSoundPath || swimSoundPath.trim() === '') {
      return;
    }

    // Throttling: Check if swim sound was played recently (prevent overlapping)
    // Swim sounds are typically 500-1000ms long, so wait at least 500ms
    const now = Date.now();
    const lastPlayTime = this.lastSwimSoundTime.get(entityId);
    if (lastPlayTime && now - lastPlayTime < 500) {
      return; // Still too soon
    }

    // Position
    const position = new Vector3(
      block.block.position.x,
      block.block.position.y,
      block.block.position.z
    );

    // Get blocked sound from pool
    const item = await this.getBlockedSoundFromPool(
      swimSoundPath,
      position,
      DEFAULT_MAX_DISTANCE
    );

    if (!item) {
      logger.warn('Failed to get swim sound from pool', { swimSoundPath });
      return;
    }

    // Volume
    const finalVolume = 1.0 * this.stepVolume; // Full volume for swim sounds

    // Update throttle timestamp
    this.lastSwimSoundTime.set(entityId, now);

    // Play (automatically released via onended callback)
    item.play(finalVolume);
  }
}
