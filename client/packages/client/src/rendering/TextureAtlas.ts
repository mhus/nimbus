/**
 * Dynamic Texture Atlas System
 *
 * Loads individual textures from the asset server and builds a dynamic texture atlas
 * at runtime for efficient rendering.
 */

import { DynamicTexture, StandardMaterial, Color3, Scene, RawTexture } from '@babylonjs/core';
import type { BlockType } from '@nimbus-client/core';
import type { ClientAssetManager } from '../assets/ClientAssetManager';

/**
 * UV coordinates for a texture in the atlas
 */
export interface AtlasUV {
  u0: number;  // Left
  v0: number;  // Top
  u1: number;  // Right
  v1: number;  // Bottom
}

/**
 * Parsed texture specification with optional atlas coordinates
 */
export interface TextureSpec {
  path: string;
  atlasCoords?: {
    x: number;
    y: number;
    width: number;
    height: number;
  };
}

/**
 * Face-specific UV mapping for blocks with different textures per face
 */
export interface BlockFaceUVs {
  top: AtlasUV;
  bottom: AtlasUV;
  sides: AtlasUV;
  north?: AtlasUV;  // Optional individual face UVs
  south?: AtlasUV;
  east?: AtlasUV;
  west?: AtlasUV;
}

/**
 * Configuration for the texture atlas
 */
export interface AtlasConfig {
  /** Base URL for asset server */
  assetServerUrl: string;

  /** Asset manager instance */
  assetManager?: ClientAssetManager;

  /** Size of individual textures in pixels (default: 16) */
  textureSize?: number;

  /** Maximum atlas size in pixels (default: 2048) */
  maxAtlasSize?: number;
}

/**
 * Dynamic Texture Atlas - Builds runtime atlas from server textures
 */
export class TextureAtlas {
  private scene: Scene;
  private config: AtlasConfig;
  private assetManager?: ClientAssetManager;

  // Atlas configuration
  private textureSize: number;
  private maxAtlasSize: number;
  private texturesPerRow: number;
  private uvSize: number;

  // Dynamic atlas canvas and texture
  private atlasCanvas?: HTMLCanvasElement;
  private atlasContext?: CanvasRenderingContext2D;
  private atlasTexture?: DynamicTexture;
  private material?: StandardMaterial;

  // Texture tracking
  private textureMap: Map<string, { x: number; y: number }> = new Map();
  private nextSlot = 0;
  private textureLoaded: Set<string> = new Set();

  // Image cache: Map<path, HTMLImageElement> to avoid loading same image multiple times
  private imageCache: Map<string, HTMLImageElement> = new Map();

  // Cache
  private blockUVCache: Map<string, BlockFaceUVs> = new Map();

  constructor(scene: Scene, config: AtlasConfig) {
    this.scene = scene;
    this.config = config;
    this.assetManager = config.assetManager;

    this.textureSize = config.textureSize || 16;
    this.maxAtlasSize = config.maxAtlasSize || 2048;
    this.texturesPerRow = Math.floor(this.maxAtlasSize / this.textureSize);
    this.uvSize = 1.0 / this.texturesPerRow;

    console.log(`[TextureAtlas] Initialized dynamic atlas: ${this.texturesPerRow}x${this.texturesPerRow} slots (${this.maxAtlasSize}x${this.maxAtlasSize}px)`);
  }

  /**
   * Set asset manager (if not provided in constructor)
   */
  setAssetManager(assetManager: ClientAssetManager): void {
    this.assetManager = assetManager;
  }

  /**
   * Initialize the dynamic texture atlas
   */
  async load(): Promise<void> {
    console.log('[TextureAtlas] Creating dynamic texture atlas...');

    // Create our own canvas for drawing
    this.atlasCanvas = document.createElement('canvas');
    this.atlasCanvas.width = this.maxAtlasSize;
    this.atlasCanvas.height = this.maxAtlasSize;
    this.atlasContext = this.atlasCanvas.getContext('2d', { willReadFrequently: true })!;

    console.log('[TextureAtlas] Canvas dimensions:', this.atlasCanvas.width, 'x', this.atlasCanvas.height);

    // Fill with transparent background
    this.atlasContext.fillStyle = 'rgba(0, 0, 0, 0)';
    this.atlasContext.fillRect(0, 0, this.maxAtlasSize, this.maxAtlasSize);

    // Create dynamic texture from our canvas
    this.atlasTexture = new DynamicTexture('dynamicAtlas', this.maxAtlasSize, this.scene, false);

    // Initial update from canvas
    this.updateAtlasTexture();

    // Create material
    this.material = new StandardMaterial('atlasMaterial', this.scene);
    this.material.diffuseTexture = this.atlasTexture;
    this.material.specularColor = new Color3(0, 0, 0);
    this.material.backFaceCulling = true;

    console.log('[TextureAtlas] Dynamic atlas created');
    console.log('[TextureAtlas] Material:', this.material);
    console.log('[TextureAtlas] Texture:', this.atlasTexture);
    console.log('[TextureAtlas] Texture is ready:', this.atlasTexture.isReady());
  }

  /**
   * Update the Babylon.js texture from canvas
   */
  private updateAtlasTexture(): void {
    if (!this.atlasTexture || !this.atlasCanvas || !this.atlasContext) return;

    // Get the DynamicTexture's context
    const textureContext = this.atlasTexture.getContext();

    // Clear the texture context
    textureContext.clearRect(0, 0, this.maxAtlasSize, this.maxAtlasSize);

    // Draw our canvas onto the texture's canvas
    textureContext.drawImage(this.atlasCanvas, 0, 0);

    // Tell Babylon.js to update the GPU texture with invertY = false
    this.atlasTexture.update(false);

    // Force mark the material as dirty so Babylon.js knows to re-render with the new texture
    if (this.material) {
      this.material.markDirty();
    }
  }

  /**
   * Load a texture (or sub-texture) into the dynamic atlas
   * Supports atlas coordinates for loading part of a texture
   */
  async loadTextureIntoAtlas(textureSpec: string | TextureSpec): Promise<{ x: number; y: number }> {
    // Parse texture spec if string
    const spec: TextureSpec = typeof textureSpec === 'string'
      ? { path: textureSpec }
      : textureSpec;

    // Create cache key that includes atlas coordinates if present
    const cacheKey = spec.atlasCoords
      ? `${spec.path}:${spec.atlasCoords.x},${spec.atlasCoords.y},${spec.atlasCoords.width},${spec.atlasCoords.height}`
      : spec.path;

    // Check if already loaded
    if (this.textureMap.has(cacheKey)) {
      return this.textureMap.get(cacheKey)!;
    }

    // Check if we have space
    const maxSlots = this.texturesPerRow * this.texturesPerRow;
    if (this.nextSlot >= maxSlots) {
      console.warn(`[TextureAtlas] Atlas full! Cannot load texture: ${cacheKey}`);
      return { x: 0, y: 0 };
    }

    // Calculate slot position
    const slotX = this.nextSlot % this.texturesPerRow;
    const slotY = Math.floor(this.nextSlot / this.texturesPerRow);
    this.nextSlot++;

    try {
      // Check if image is already cached (by path only, not including atlas coords)
      let img: HTMLImageElement;
      if (this.imageCache.has(spec.path)) {
        img = this.imageCache.get(spec.path)!;
      } else {
        // Load image from server (only if not cached)
        const url = `${this.config.assetServerUrl}/${spec.path}`;
        img = await this.loadImage(url);
        // Cache the loaded image for future use
        this.imageCache.set(spec.path, img);
        console.log(`[TextureAtlas] Loaded and cached image: ${spec.path}`);
      }

      // Draw into atlas canvas
      const pixelX = slotX * this.textureSize;
      const pixelY = slotY * this.textureSize;

      if (spec.atlasCoords) {
        // Draw sub-texture from source image
        this.atlasContext!.drawImage(
          img,
          spec.atlasCoords.x, spec.atlasCoords.y, spec.atlasCoords.width, spec.atlasCoords.height, // Source
          pixelX, pixelY, this.textureSize, this.textureSize // Destination
        );
      } else {
        // Draw full texture
        this.atlasContext!.drawImage(img, pixelX, pixelY, this.textureSize, this.textureSize);
      }

      // Update dynamic texture from canvas
      this.updateAtlasTexture();

      // Cache position
      const position = { x: slotX, y: slotY };
      this.textureMap.set(cacheKey, position);
      this.textureLoaded.add(cacheKey);

      return position;
    } catch (error) {
      console.error(`[TextureAtlas] Failed to load texture ${cacheKey}:`, error);
      return { x: 0, y: 0 };
    }
  }

  /**
   * Load image from URL
   */
  private loadImage(url: string): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';

      img.onload = () => resolve(img);
      img.onerror = (e) => {
        console.error(`[TextureAtlas] Failed to load image: ${url}`, e);
        reject(new Error(`Failed to load image: ${url}`));
      };

      img.src = url;
    });
  }

  /**
   * Get UV coordinates for a texture in the atlas
   * Supports TextureSpec for atlas coordinates
   */
  async getTextureUV(textureSpec: string | TextureSpec): Promise<AtlasUV> {
    // Convert string to TextureSpec if needed
    const spec: TextureSpec = typeof textureSpec === 'string'
      ? { path: this.normalizeTexturePath(textureSpec) }
      : { ...textureSpec, path: this.normalizeTexturePath(textureSpec.path) };

    // Ensure texture is loaded into atlas
    const pos = await this.loadTextureIntoAtlas(spec);

    // Calculate UV coordinates
    const u0 = pos.x * this.uvSize;
    const v0 = pos.y * this.uvSize;
    const u1 = u0 + this.uvSize;
    const v1 = v0 + this.uvSize;

    return { u0, v0, u1, v1 };
  }

  /**
   * Get UV mapping for a block (with face-specific textures)
   * Loads textures into atlas dynamically
   * Supports custom texture override via customTexture parameter
   */
  async getBlockUVs(block: BlockType, customTexture?: string | string[]): Promise<BlockFaceUVs> {
    // Use custom texture if provided, otherwise use block's texture
    const textureToUse = customTexture ?? block.texture;

    // Create cache key that includes custom texture if present
    const cacheKey = customTexture
      ? `${block.name}:${typeof customTexture === 'string' ? customTexture : customTexture.join(';')}`
      : block.name;

    // Check cache
    if (this.blockUVCache.has(cacheKey)) {
      return this.blockUVCache.get(cacheKey)!;
    }

    console.log(`[TextureAtlas] Loading textures for block: ${block.name}`, textureToUse);

    let faceUVs: BlockFaceUVs;

    if (typeof textureToUse === 'string') {
      // Single texture for all faces - parse and load
      const specs = TextureAtlas.parseTextureString(textureToUse);
      const uv = await this.getTextureUV(specs[0]);
      faceUVs = {
        top: uv,
        bottom: uv,
        sides: uv,
      };
    } else if (Array.isArray(textureToUse)) {
      if (block.texture.length === 1) {
        // Single texture
        const uv = await this.getTextureUV(block.texture[0]);
        faceUVs = {
          top: uv,
          bottom: uv,
          sides: uv,
        };
      } else if (block.texture.length === 2) {
        // Top/bottom, sides
        const topUV = await this.getTextureUV(block.texture[0]);
        const sideUV = await this.getTextureUV(block.texture[1]);
        faceUVs = {
          top: topUV,
          bottom: topUV,
          sides: sideUV,
        };
      } else if (block.texture.length === 3) {
        // Top, bottom, sides
        const topUV = await this.getTextureUV(block.texture[0]);
        const bottomUV = await this.getTextureUV(block.texture[1]);
        const sideUV = await this.getTextureUV(block.texture[2]);
        faceUVs = {
          top: topUV,
          bottom: bottomUV,
          sides: sideUV,
        };
      } else {
        // Top, bottom, north, south, east, west
        const topUV = await this.getTextureUV(block.texture[0]);
        const bottomUV = await this.getTextureUV(block.texture[1]);
        const northUV = await this.getTextureUV(block.texture[2]);
        const southUV = await this.getTextureUV(block.texture[3] || block.texture[2]);
        const eastUV = await this.getTextureUV(block.texture[4] || block.texture[2]);
        const westUV = await this.getTextureUV(block.texture[5] || block.texture[2]);
        faceUVs = {
          top: topUV,
          bottom: bottomUV,
          sides: northUV,
          north: northUV,
          south: southUV,
          east: eastUV,
          west: westUV,
        };
      }
    } else {
      // Default fallback
      const defaultUV = await this.getTextureUV('stone');
      faceUVs = {
        top: defaultUV,
        bottom: defaultUV,
        sides: defaultUV,
      };
    }

    // Cache result using the correct cache key (includes custom texture if present)
    this.blockUVCache.set(cacheKey, faceUVs);

    return faceUVs;
  }

  /**
   * Normalize texture path (add .png extension if missing)
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
   * Get the atlas material
   */
  getMaterial(): StandardMaterial | undefined {
    return this.material;
  }

  /**
   * Get the dynamic atlas texture
   */
  getTexture(): DynamicTexture | undefined {
    return this.atlasTexture;
  }

  /**
   * Check if texture system is ready
   */
  isReady(): boolean {
    return this.atlasTexture !== undefined && this.material !== undefined;
  }

  /**
   * Clear caches (call when blocks change)
   */
  clearCache(): void {
    this.blockUVCache.clear();
    console.log('[TextureAtlas] UV cache cleared');
  }

  /**
   * Get atlas configuration
   */
  getConfig(): AtlasConfig {
    return this.config;
  }

  /**
   * Get asset server URL
   */
  getAssetServerUrl(): string {
    return this.config.assetServerUrl;
  }

  /**
   * Get atlas statistics
   */
  getStats(): { loadedTextures: number; totalSlots: number; usedSlots: number } {
    const totalSlots = this.texturesPerRow * this.texturesPerRow;
    return {
      loadedTextures: this.textureLoaded.size,
      totalSlots,
      usedSlots: this.nextSlot,
    };
  }

  /**
   * Parse texture string into array of texture specifications
   * Supports:
   * - Single texture: "stone.png"
   * - Multiple textures: "top.png;side.png;bottom.png"
   * - Atlas coordinates: "foods.png:0,0,16,16"
   * - Combined: "top.png:0,0,16,16;side.png:16,0,16,16"
   */
  static parseTextureString(textureString: string): TextureSpec[] {
    // Split by semicolon to get individual texture specs
    const parts = textureString.split(';').map(s => s.trim()).filter(s => s.length > 0);

    return parts.map(part => {
      // Check for atlas coordinates: "path:x,y,w,h"
      const colonIndex = part.indexOf(':');
      if (colonIndex === -1) {
        // No atlas coordinates, just path
        return { path: part };
      }

      // Split path and coordinates
      const path = part.substring(0, colonIndex);
      const coordsStr = part.substring(colonIndex + 1);

      // Parse coordinates: "x,y,w,h"
      const coords = coordsStr.split(',').map(s => parseInt(s.trim(), 10));

      if (coords.length !== 4 || coords.some(isNaN)) {
        console.warn(`[TextureAtlas] Invalid atlas coordinates in "${part}", using full texture`);
        return { path: part }; // Fallback to full string as path
      }

      return {
        path,
        atlasCoords: {
          x: coords[0],
          y: coords[1],
          width: coords[2],
          height: coords[3],
        },
      };
    });
  }

  /**
   * Convert texture string or array to TextureSpec array
   */
  static normalizeTextureInput(texture: string | string[]): TextureSpec[] {
    if (Array.isArray(texture)) {
      // Array of texture strings - parse each one
      return texture.flatMap(t => TextureAtlas.parseTextureString(t));
    }
    // Single string - parse it
    return TextureAtlas.parseTextureString(texture);
  }
}
