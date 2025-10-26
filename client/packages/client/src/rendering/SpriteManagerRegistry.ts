/**
 * Sprite Manager Registry
 * Manages Babylon.js SpriteManager instances for SPRITE block shapes
 *
 * Creates one SpriteManager per unique texture to optimize rendering.
 * Each SpriteManager can render many sprite instances efficiently.
 */

import { SpriteManager, Scene, Texture, Sprite } from '@babylonjs/core';
import type { TextureAtlas } from './TextureAtlas';
import type { WindManager } from '../wind/WindManager';

/**
 * Configuration for a sprite manager
 */
interface SpriteManagerConfig {
  texturePath: string;
  capacity: number; // Maximum number of sprites this manager can handle
}

/**
 * Sprite instance data for wind animation
 */
interface SpriteData {
  sprite: Sprite;
  baseX: number; // Original X position
  baseY: number; // Original Y position
  baseZ: number; // Original Z position
  phaseOffset: number; // Random phase offset for varied animation
  windLeafiness: number; // Per-sprite wind leafiness (0-1, 0 = no wind effect)
  windStability: number; // Per-sprite wind stability (0-1, 0 = maximum movement)
}

/**
 * Registry for managing SpriteManager instances by texture
 */
export class SpriteManagerRegistry {
  private scene: Scene;
  private textureAtlas: TextureAtlas;
  private windManager: WindManager | null = null;

  // Map: texturePath -> SpriteManager
  private managers: Map<string, SpriteManager> = new Map();

  // Map: texturePath -> texture dimensions { width, height }
  private textureDimensions: Map<string, { width: number; height: number }> = new Map();

  // Array of all sprite data for wind animation
  private spriteData: SpriteData[] = [];

  // Default capacity per manager (can be increased if needed)
  private readonly DEFAULT_CAPACITY = 10000;

  // Animation state
  private totalTime = 0;

  constructor(scene: Scene, textureAtlas: TextureAtlas) {
    this.scene = scene;
    this.textureAtlas = textureAtlas;

    // Setup animation loop
    this.setupAnimationLoop();

    console.log('[SpriteManagerRegistry] Initialized with wind animation');
  }

  /**
   * Set WindManager for accessing wind parameters
   */
  setWindManager(windManager: WindManager): void {
    this.windManager = windManager;
    console.log('[SpriteManagerRegistry] WindManager set');
  }

  /**
   * Get or create a SpriteManager for a given texture
   * @param texturePath Path to the texture (e.g., "grass.png" or "foods.png:0,0,16,16")
   * @param capacity Optional capacity override (default: 10000)
   * @returns Promise<SpriteManager> instance
   */
  async getManager(texturePath: string, capacity?: number): Promise<SpriteManager> {
    // Check if manager already exists
    if (this.managers.has(texturePath)) {
      return this.managers.get(texturePath)!;
    }

    // Create new manager (await texture loading)
    const actualCapacity = capacity ?? this.DEFAULT_CAPACITY;
    const manager = await this.createManager(texturePath, actualCapacity);

    // Cache manager
    this.managers.set(texturePath, manager);
    console.log(`[SpriteManagerRegistry] Created manager for texture: ${texturePath} (capacity: ${actualCapacity})`);

    return manager;
  }

  /**
   * Create a new SpriteManager instance (async - waits for texture to load)
   * @param texturePath Path to the texture
   * @param capacity Maximum number of sprites
   * @returns Promise<SpriteManager> instance
   */
  private async createManager(texturePath: string, capacity: number): Promise<SpriteManager> {
    // Parse texture path (may include atlas coordinates like "foods.png:0,0,16,16")
    const textureSpec = this.parseTexturePath(texturePath);

    // Normalize the texture path (add proper directory structure and extension)
    const normalizedPath = this.normalizeTexturePath(textureSpec.path);

    // Create manager name
    const managerName = `spriteManager_${texturePath.replace(/[^a-zA-Z0-9]/g, '_')}`;

    // Get full texture URL from asset server
    const assetServerUrl = this.textureAtlas.getAssetServerUrl();
    const textureUrl = `${assetServerUrl}/${normalizedPath}`;

    console.log(`[SpriteManagerRegistry] Loading texture: ${textureUrl}`);

    // Load texture and WAIT for it to be ready
    const texture = new Texture(textureUrl, this.scene, false, false);

    // Wait for texture to load
    await new Promise<void>((resolve) => {
      if (texture.isReady()) {
        resolve();
      } else {
        texture.onLoadObservable.addOnce(() => {
          resolve();
        });
      }
    });

    // NOW get the actual texture dimensions
    const size = texture.getSize();
    const cellWidth = size.width;
    const cellHeight = size.height;

    console.log(`[SpriteManagerRegistry] Texture loaded: ${textureUrl} (${cellWidth}x${cellHeight})`);

    // Store dimensions for later retrieval
    this.textureDimensions.set(texturePath, { width: cellWidth, height: cellHeight });

    // Dispose the temporary texture (SpriteManager will load its own)
    texture.dispose();

    // Create SpriteManager with ACTUAL texture dimensions
    // SpriteManager parameters: name, imgUrl, capacity, cellSize, scene
    const manager = new SpriteManager(
      managerName,
      textureUrl,
      capacity,
      { width: cellWidth, height: cellHeight },
      this.scene
    );

    // Configure transparency immediately and after texture loads
    // Set blend mode for proper alpha blending (2 = ALPHA_COMBINE)
    manager.blendMode = 2; // Babylon.SpriteManager.ALPHA_COMBINE

    // Apply transparency settings to texture
    if (manager.texture) {
      manager.texture.hasAlpha = true;
      manager.texture.getAlphaFromRGB = false;

      // Also set after texture loads (to be sure)
      manager.texture.onLoadObservable.addOnce(() => {
        console.log(`[SpriteManagerRegistry] Manager texture loaded, reapplying transparency settings`);
        if (manager.texture) {
          manager.texture.hasAlpha = true;
          manager.texture.getAlphaFromRGB = false;
          // Force texture update
          manager.texture.updateSamplingMode(8); // NEAREST_NEAREST
        }
      });
    }

    console.log(`[SpriteManagerRegistry] Created SpriteManager: ${managerName} with dimensions ${cellWidth}x${cellHeight} and transparency (blendMode: 2)`);

    return manager;
  }

  /**
   * Parse texture path into components
   * Handles formats like:
   * - "grass.png" -> { path: "grass.png" }
   * - "foods.png:0,0,16,16" -> { path: "foods.png", atlas: [0, 0, 16, 16] }
   */
  private parseTexturePath(texturePath: string): { path: string; atlas?: number[] } {
    if (!texturePath.includes(':')) {
      return { path: texturePath };
    }

    const [path, coords] = texturePath.split(':');
    const atlasCoords = coords.split(',').map(Number);

    return {
      path,
      atlas: atlasCoords,
    };
  }

  /**
   * Normalize texture path (add .png extension if missing and proper directory structure)
   * Matches TextureAtlas.normalizeTexturePath() behavior
   */
  private normalizeTexturePath(path: string): string {
    // If path starts with 'block/', prepend 'textures/'
    if (path.startsWith('block/')) {
      path = `textures/${path}`;
    }
    // If path doesn't start with 'textures/', prepend 'textures/block/'
    else if (!path.startsWith('textures/')) {
      path = `textures/block/${path}`;
    }

    // Add .png extension if missing
    if (!path.endsWith('.png')) {
      path = `${path}.png`;
    }

    return path;
  }

  /**
   * Get asset server URL from TextureAtlas
   */
  private getAssetServerUrl(): string {
    return this.textureAtlas.getAssetServerUrl();
  }

  /**
   * Get texture dimensions for a given texture path
   * @param texturePath Path to the texture
   * @returns Texture dimensions { width, height } or default { width: 16, height: 16 }
   */
  getTextureDimensions(texturePath: string): { width: number; height: number } {
    return this.textureDimensions.get(texturePath) || { width: 16, height: 16 };
  }

  /**
   * Register a sprite for wind animation
   * @param sprite The sprite instance
   * @param windLeafiness Per-sprite wind leafiness (0-1, default 0 = no wind)
   * @param windStability Per-sprite wind stability (0-1, default 0 = no wind)
   */
  registerSprite(sprite: Sprite, windLeafiness: number = 0, windStability: number = 0): void {
    const spriteData: SpriteData = {
      sprite,
      baseX: sprite.position.x,
      baseY: sprite.position.y,
      baseZ: sprite.position.z,
      phaseOffset: Math.random() * Math.PI * 2, // Random phase for variation
      windLeafiness: Math.max(0, Math.min(1, windLeafiness)), // Clamp to 0-1
      windStability: Math.max(0, Math.min(1, windStability)), // Clamp to 0-1
    };
    this.spriteData.push(spriteData);
  }

  /**
   * Setup animation loop for wind effects
   */
  private setupAnimationLoop(): void {
    this.scene.onBeforeRenderObservable.add(() => {
      // Update time
      this.totalTime += this.scene.getEngine().getDeltaTime() / 1000.0;

      // Skip if no wind manager or no sprites
      if (!this.windManager || this.spriteData.length === 0) {
        return;
      }

      // Get wind parameters
      const windStrength = this.windManager.getWindStrength();
      const windGustStrength = this.windManager.getWindGustStrength();
      const windSwayFactor = this.windManager.getWindSwayFactor();
      const windDirection = this.windManager.getWindDirection();

      // Animate each sprite
      for (const data of this.spriteData) {
        // Base sway wave (smooth sinusoidal)
        const baseWave = Math.sin(this.totalTime * windSwayFactor + data.phaseOffset) * windStrength;

        // Gust effect (faster, irregular pulses)
        const gustWave = Math.sin(this.totalTime * windSwayFactor * 2.3 + data.phaseOffset * 0.7) * windGustStrength;
        const gustModulation = Math.sin(this.totalTime * windSwayFactor * 0.7);

        // Secondary wave for more organic movement (leafiness effect) - per sprite
        const leafWave = Math.sin(this.totalTime * windSwayFactor * 1.7 + data.phaseOffset) * data.windLeafiness;

        // Combine waves
        const totalWave = (baseWave + gustWave * 0.5 * gustModulation + leafWave * 0.3) * 0.15; // Scale down amplitude

        // Apply stability (reduces movement) - per sprite
        const stabilityFactor = 1.0 - data.windStability;

        // Apply wind direction with stability
        const offsetX = windDirection.x * totalWave * stabilityFactor;
        const offsetZ = windDirection.z * totalWave * stabilityFactor;

        // Update sprite position (add offset to base position)
        data.sprite.position.x = data.baseX + offsetX;
        data.sprite.position.z = data.baseZ + offsetZ;
      }
    });
  }

  /**
   * Clear all managers (cleanup)
   */
  clearAll(): void {
    for (const [texturePath, manager] of this.managers) {
      manager.dispose();
      console.log(`[SpriteManagerRegistry] Disposed manager: ${texturePath}`);
    }
    this.managers.clear();
    this.textureDimensions.clear();
    this.spriteData = []; // Clear sprite data
  }

  /**
   * Remove sprites that have been disposed
   * Call this when chunks are unloaded
   */
  removeDisposedSprites(): void {
    // Filter out sprites that are no longer valid
    const beforeCount = this.spriteData.length;
    this.spriteData = this.spriteData.filter(data => {
      // Check if sprite still exists and is not disposed
      try {
        return data.sprite && !data.sprite.isDisposed();
      } catch {
        return false;
      }
    });
    const removed = beforeCount - this.spriteData.length;
    if (removed > 0) {
      console.log(`[SpriteManagerRegistry] Removed ${removed} disposed sprites from animation`);
    }
  }

  /**
   * Get statistics about managed sprite managers
   */
  getStats(): { managerCount: number; textures: string[]; spriteCount: number } {
    return {
      managerCount: this.managers.size,
      textures: Array.from(this.managers.keys()),
      spriteCount: this.spriteData.length,
    };
  }
}
