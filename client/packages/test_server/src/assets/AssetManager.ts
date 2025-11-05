/**
 * AssetManager - Manages assets (textures, models, sounds, etc.)
 *
 * Assets are stored in the filesystem under files/assets/
 * Each asset is identified by its relative path (e.g., "textures/block/basic/stone.png")
 */

import fs from 'fs';
import path from 'path';
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const logger = getLogger('AssetManager');

/**
 * Asset metadata
 */
export interface Asset {
  /** Relative path (e.g., "textures/block/basic/stone.png") */
  path: string;

  /** File size in bytes */
  size: number;

  /** MIME type */
  mimeType: string;

  /** Last modified timestamp */
  lastModified: Date;

  /** File extension */
  extension: string;

  /** Category (derived from path) */
  category: string;
}

export class AssetManager {
  private assetsDir: string;

  constructor() {
    this.assetsDir = path.join(__dirname, '../../files/assets');
    logger.info('AssetManager initialized');
  }

  /**
   * Get absolute file path for asset
   */
  private getAssetFilePath(relativePath: string): string {
    return path.join(this.assetsDir, relativePath);
  }

  /**
   * Get MIME type from file extension
   */
  private getMimeType(ext: string): string {
    const mimeTypes: Record<string, string> = {
      '.png': 'image/png',
      '.jpg': 'image/jpeg',
      '.jpeg': 'image/jpeg',
      '.gif': 'image/gif',
      '.webp': 'image/webp',
      '.svg': 'image/svg+xml',
      '.json': 'application/json',
      '.obj': 'model/obj',
      '.mtl': 'model/mtl',
      '.wav': 'audio/wav',
      '.mp3': 'audio/mpeg',
      '.ogg': 'audio/ogg',
    };

    return mimeTypes[ext.toLowerCase()] || 'application/octet-stream';
  }

  /**
   * Get category from path
   */
  private getCategory(relativePath: string): string {
    const parts = relativePath.split('/');
    return parts.length > 0 ? parts[0] : 'other';
  }

  /**
   * Check if path is safe (doesn't escape assets directory)
   */
  private isSafePath(relativePath: string): boolean {
    const fullPath = this.getAssetFilePath(relativePath);
    const normalized = path.normalize(fullPath);
    return normalized.startsWith(this.assetsDir);
  }

  /**
   * Get asset metadata
   */
  async getAsset(relativePath: string): Promise<Asset | undefined> {
    try {
      if (!this.isSafePath(relativePath)) {
        logger.warn('Unsafe asset path', { relativePath });
        return undefined;
      }

      const filePath = this.getAssetFilePath(relativePath);

      if (!fs.existsSync(filePath)) {
        logger.debug('Asset not found', { relativePath });
        return undefined;
      }

      const stats = fs.statSync(filePath);

      if (!stats.isFile()) {
        logger.debug('Path is not a file', { relativePath });
        return undefined;
      }

      const ext = path.extname(relativePath);

      return {
        path: relativePath,
        size: stats.size,
        mimeType: this.getMimeType(ext),
        lastModified: stats.mtime,
        extension: ext,
        category: this.getCategory(relativePath),
      };
    } catch (error) {
      ExceptionHandler.handle(error, 'AssetManager.getAsset', { relativePath });
      return undefined;
    }
  }

  /**
   * Get all assets recursively
   */
  async getAllAssets(): Promise<Asset[]> {
    try {
      const assets: Asset[] = [];
      const scanDirectory = (dir: string, baseDir: string) => {
        const entries = fs.readdirSync(dir, { withFileTypes: true });

        for (const entry of entries) {
          const fullPath = path.join(dir, entry.name);
          const relativePath = path.relative(baseDir, fullPath);

          if (entry.isDirectory()) {
            scanDirectory(fullPath, baseDir);
          } else if (entry.isFile()) {
            // Skip .info files - they should not appear in asset search results
            if (entry.name.endsWith('.info')) {
              continue;
            }

            const stats = fs.statSync(fullPath);
            const ext = path.extname(entry.name);

            assets.push({
              path: relativePath.replace(/\\/g, '/'), // Normalize path separators
              size: stats.size,
              mimeType: this.getMimeType(ext),
              lastModified: stats.mtime,
              extension: ext,
              category: this.getCategory(relativePath),
            });
          }
        }
      };

      scanDirectory(this.assetsDir, this.assetsDir);
      logger.info(`Found ${assets.length} assets`);
      return assets;
    } catch (error) {
      ExceptionHandler.handle(error, 'AssetManager.getAllAssets');
      return [];
    }
  }

  /**
   * Search assets by query
   */
  async searchAssets(query: string): Promise<Asset[]> {
    const allAssets = await this.getAllAssets();
    const lowerQuery = query.toLowerCase();

    return allAssets.filter(asset => {
      // Search in path
      if (asset.path.toLowerCase().includes(lowerQuery)) {
        return true;
      }

      // Search in category
      if (asset.category.toLowerCase().includes(lowerQuery)) {
        return true;
      }

      return false;
    });
  }

  /**
   * Create/upload a new asset
   */
  async createAsset(relativePath: string, data: Buffer): Promise<Asset | undefined> {
    try {
      if (!this.isSafePath(relativePath)) {
        logger.error('Unsafe asset path', { relativePath });
        return undefined;
      }

      const filePath = this.getAssetFilePath(relativePath);
      const dirPath = path.dirname(filePath);

      // Create directory if it doesn't exist
      if (!fs.existsSync(dirPath)) {
        fs.mkdirSync(dirPath, { recursive: true });
      }

      // Check if asset already exists
      if (fs.existsSync(filePath)) {
        logger.error('Asset already exists', { relativePath });
        return undefined;
      }

      // Write file
      fs.writeFileSync(filePath, data);

      logger.info('Created asset', { relativePath, size: data.length });

      // Return asset metadata
      return await this.getAsset(relativePath);
    } catch (error) {
      ExceptionHandler.handle(error, 'AssetManager.createAsset', { relativePath });
      return undefined;
    }
  }

  /**
   * Update an existing asset
   */
  async updateAsset(relativePath: string, data: Buffer): Promise<Asset | undefined> {
    try {
      if (!this.isSafePath(relativePath)) {
        logger.error('Unsafe asset path', { relativePath });
        return undefined;
      }

      const filePath = this.getAssetFilePath(relativePath);

      // Check if asset exists
      if (!fs.existsSync(filePath)) {
        logger.error('Asset not found', { relativePath });
        return undefined;
      }

      // Write file
      fs.writeFileSync(filePath, data);

      logger.info('Updated asset', { relativePath, size: data.length });

      // Return asset metadata
      return await this.getAsset(relativePath);
    } catch (error) {
      ExceptionHandler.handle(error, 'AssetManager.updateAsset', { relativePath });
      return undefined;
    }
  }

  /**
   * Delete an asset
   */
  async deleteAsset(relativePath: string): Promise<boolean> {
    try {
      if (!this.isSafePath(relativePath)) {
        logger.error('Unsafe asset path', { relativePath });
        return false;
      }

      const filePath = this.getAssetFilePath(relativePath);

      // Check if asset exists
      if (!fs.existsSync(filePath)) {
        logger.error('Asset not found', { relativePath });
        return false;
      }

      // Delete file
      fs.unlinkSync(filePath);

      logger.info('Deleted asset', { relativePath });
      return true;
    } catch (error) {
      ExceptionHandler.handle(error, 'AssetManager.deleteAsset', { relativePath });
      return false;
    }
  }

  /**
   * Rename/move an asset
   */
  async renameAsset(oldPath: string, newPath: string): Promise<Asset | undefined> {
    try {
      if (!this.isSafePath(oldPath) || !this.isSafePath(newPath)) {
        logger.error('Unsafe asset path', { oldPath, newPath });
        return undefined;
      }

      const oldFilePath = this.getAssetFilePath(oldPath);
      const newFilePath = this.getAssetFilePath(newPath);

      // Check if old asset exists
      if (!fs.existsSync(oldFilePath)) {
        logger.error('Asset not found', { oldPath });
        return undefined;
      }

      // Check if new path already exists
      if (fs.existsSync(newFilePath)) {
        logger.error('Target path already exists', { newPath });
        return undefined;
      }

      // Create directory for new path if needed
      const newDirPath = path.dirname(newFilePath);
      if (!fs.existsSync(newDirPath)) {
        fs.mkdirSync(newDirPath, { recursive: true });
      }

      // Rename file
      fs.renameSync(oldFilePath, newFilePath);

      logger.info('Renamed asset', { oldPath, newPath });

      // Return new asset metadata
      return await this.getAsset(newPath);
    } catch (error) {
      ExceptionHandler.handle(error, 'AssetManager.renameAsset', { oldPath, newPath });
      return undefined;
    }
  }
}
