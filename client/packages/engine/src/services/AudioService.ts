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
        this.release();
        onReleaseCallback();
      });
    } else {
      // Fallback: release after 1 second if Observable not available
      logger.warn('onEndedObservable not available, using timeout fallback');
      setTimeout(() => {
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

      logger.info('Spatial audio configured (StaticSound API)', {
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

      logger.info('Spatial audio configured (legacy Sound API)', {
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

    // Debug logging
    logger.info('Playing sound with config', {
      volume,
      hasSpatial: !!this.sound.spatial,
      spatialPosition: this.sound.spatial?.position,
      maxDistance: this.sound.spatial?.maxDistance,
      distanceModel: this.sound.spatial?.distanceModel
    });

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
        logger.warn('Pool at maximum capacity, sound skipped', {
          path: this.path,
          maxSize: POOL_MAX_SIZE
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

      logger.info('Sound loaded into pool', { path, initialPoolSize });
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

    // Dispose legacy cache
    this.clearCache();

    // Dispose all sound pools
    this.soundPools.forEach(pool => pool.dispose());
    this.soundPools.clear();

    // Clear swim sound throttle
    this.lastSwimSoundTime.clear();
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
   */
  private async playSwimSound(entityId: string, block: ClientBlock): Promise<void> {
    const swimSoundPath = 'audio/liquid/swim1.ogg';

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
      logger.warn('Failed to get swim sound from pool');
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
