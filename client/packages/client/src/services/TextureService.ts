/**
 * TextureService - Manages texture loading and caching
 *
 * Provides lazy loading of textures from the server asset endpoint.
 * Textures are cached to prevent duplicate downloads.
 */

import { getLogger, ExceptionHandler } from '@nimbus/shared';

const logger = getLogger('TextureService');

export interface TextureServiceConfig {
  /** Asset server base URL (e.g., "http://localhost:3000/api/worlds/test-world-1/assets") */
  assetServerUrl: string;

  /** Optional timeout for texture loading in milliseconds */
  timeout?: number;
}

/**
 * TextureService - Handles texture loading and caching
 *
 * Key features:
 * - Lazy loading: Textures are loaded on-demand, not at startup
 * - Caching: Each texture is only loaded once, subsequent requests return cached image
 * - Path normalization: Handles various path formats
 * - Error handling: Provides fallback for missing textures
 */
export class TextureService {
  private imageCache: Map<string, HTMLImageElement> = new Map();
  private loadingPromises: Map<string, Promise<HTMLImageElement>> = new Map();
  private config: Required<TextureServiceConfig>;

  constructor(config: TextureServiceConfig) {
    this.config = {
      timeout: 10000, // 10 seconds default
      ...config,
    };

    logger.info('TextureService initialized', { assetServerUrl: this.config.assetServerUrl });
  }

  /**
   * Load a texture from the server
   *
   * @param path - Asset path (e.g., "textures/block/basic/stone.png" or "assets/textures/block/basic/stone.png")
   * @returns Promise resolving to HTMLImageElement
   */
  async loadTexture(path: string): Promise<HTMLImageElement> {
    try {
      // Normalize path
      const normalizedPath = this.normalizePath(path);

      // Check cache first
      const cached = this.imageCache.get(normalizedPath);
      if (cached) {
        logger.debug('Texture loaded from cache', { path: normalizedPath });
        return cached;
      }

      // Check if already loading
      const existingPromise = this.loadingPromises.get(normalizedPath);
      if (existingPromise) {
        logger.debug('Texture already loading, waiting', { path: normalizedPath });
        return existingPromise;
      }

      // Start loading
      const loadPromise = this.loadTextureInternal(normalizedPath);
      this.loadingPromises.set(normalizedPath, loadPromise);

      try {
        const image = await loadPromise;
        this.imageCache.set(normalizedPath, image);
        return image;
      } finally {
        this.loadingPromises.delete(normalizedPath);
      }
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'TextureService.loadTexture',
        { path }
      );
    }
  }

  /**
   * Internal texture loading implementation
   */
  private loadTextureInternal(normalizedPath: string): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const image = new Image();
      const url = `${this.config.assetServerUrl}/${normalizedPath}`;

      // Set up timeout
      const timeoutId = setTimeout(() => {
        image.src = ''; // Cancel loading
        reject(new Error(`Texture loading timeout: ${normalizedPath}`));
      }, this.config.timeout);

      image.onload = () => {
        clearTimeout(timeoutId);
        logger.debug('Texture loaded successfully', {
          path: normalizedPath,
          width: image.width,
          height: image.height,
        });
        resolve(image);
      };

      image.onerror = () => {
        clearTimeout(timeoutId);
        const error = new Error(`Failed to load texture: ${normalizedPath}`);
        logger.error('Texture loading failed', { path: normalizedPath, url }, error);
        reject(error);
      };

      // Start loading
      image.crossOrigin = 'anonymous'; // Enable CORS
      image.src = url;
    });
  }

  /**
   * Normalize asset path
   *
   * Handles various path formats:
   * - "assets/textures/block/basic/stone.png" → "textures/block/basic/stone.png"
   * - "textures/block/basic/stone.png" → "textures/block/basic/stone.png"
   * - "stone" → "textures/block/basic/stone.png"
   *
   * @param path - Input path
   * @returns Normalized path without "assets/" prefix
   */
  private normalizePath(path: string): string {
    let normalized = path;

    // Remove "assets/" prefix if present
    if (normalized.startsWith('assets/')) {
      normalized = normalized.substring('assets/'.length);
    }

    // Add .png extension if missing
    if (!normalized.match(/\.\w+$/)) {
      normalized += '.png';
    }

    // If it's just a filename, assume it's in textures/block/basic/
    if (!normalized.includes('/')) {
      normalized = `textures/block/basic/${normalized}`;
    }

    return normalized;
  }

  /**
   * Check if texture is already loaded
   *
   * @param path - Asset path
   * @returns True if texture is in cache
   */
  isLoaded(path: string): boolean {
    const normalizedPath = this.normalizePath(path);
    return this.imageCache.has(normalizedPath);
  }

  /**
   * Get cached texture if available
   *
   * @param path - Asset path
   * @returns Cached image or undefined
   */
  getCached(path: string): HTMLImageElement | undefined {
    const normalizedPath = this.normalizePath(path);
    return this.imageCache.get(normalizedPath);
  }

  /**
   * Preload multiple textures
   *
   * @param paths - Array of asset paths
   * @returns Promise resolving when all textures are loaded
   */
  async preloadTextures(paths: string[]): Promise<void> {
    try {
      logger.info('Preloading textures', { count: paths.length });

      const promises = paths.map((path) => this.loadTexture(path));
      await Promise.all(promises);

      logger.info('Textures preloaded successfully', { count: paths.length });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'TextureService.preloadTextures',
        { count: paths.length }
      );
    }
  }

  /**
   * Clear texture cache
   *
   * @param path - Optional specific path to clear, or clear all if omitted
   */
  clearCache(path?: string): void {
    if (path) {
      const normalizedPath = this.normalizePath(path);
      this.imageCache.delete(normalizedPath);
      logger.debug('Cleared texture from cache', { path: normalizedPath });
    } else {
      const count = this.imageCache.size;
      this.imageCache.clear();
      logger.info('Cleared entire texture cache', { count });
    }
  }

  /**
   * Get cache statistics
   */
  getCacheStats(): { cachedCount: number; loadingCount: number } {
    return {
      cachedCount: this.imageCache.size,
      loadingCount: this.loadingPromises.size,
    };
  }

  /**
   * Update asset server URL (e.g., when switching worlds)
   */
  setAssetServerUrl(assetServerUrl: string): void {
    this.config.assetServerUrl = assetServerUrl;
    logger.info('Asset server URL updated', { assetServerUrl });
  }
}
