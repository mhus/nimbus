/**
 * ChunkStorage - File-based chunk persistence
 *
 * Stores ChunkData as JSON files on disk.
 * ChunkData is stored in serialized format, ready for network transfer.
 */

import { promises as fs } from 'fs';
import path from 'path';
import type { ChunkData } from '@nimbus/shared';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('ChunkStorage');

export class ChunkStorage {
  private basePath: string;

  constructor(worldId: string, basePath = './data/worlds') {
    this.basePath = path.join(basePath, worldId, 'chunks');
  }

  /**
   * Initialize storage (create directories)
   */
  async initialize(): Promise<void> {
    try {
      await fs.mkdir(this.basePath, { recursive: true });
      logger.info('ChunkStorage initialized', { basePath: this.basePath });
    } catch (error) {
      logger.error('Failed to initialize ChunkStorage', { basePath: this.basePath }, error as Error);
      throw error;
    }
  }

  /**
   * Get chunk file path
   */
  private getChunkPath(cx: number, cz: number): string {
    return path.join(this.basePath, `chunk_${cx}_${cz}.json`);
  }

  /**
   * Check if chunk exists on disk
   */
  async exists(cx: number, cz: number): Promise<boolean> {
    try {
      const chunkPath = this.getChunkPath(cx, cz);
      await fs.access(chunkPath);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Load chunk from disk
   */
  async load(cx: number, cz: number): Promise<ChunkData | null> {
    try {
      const chunkPath = this.getChunkPath(cx, cz);
      const data = await fs.readFile(chunkPath, 'utf-8');
      const chunkData = JSON.parse(data) as ChunkData;

      logger.debug('Loaded chunk from disk', { cx, cz, blockCount: chunkData.blocks.length });
      return chunkData;
    } catch (error) {
      if ((error as any).code === 'ENOENT') {
        return null; // Chunk doesn't exist
      }
      logger.error('Failed to load chunk', { cx, cz }, error as Error);
      throw error;
    }
  }

  /**
   * Save chunk to disk
   */
  async save(chunkData: ChunkData): Promise<void> {
    try {
      const chunkPath = this.getChunkPath(chunkData.cx, chunkData.cz);
      const data = JSON.stringify(chunkData, null, 2);
      await fs.writeFile(chunkPath, data, 'utf-8');

      logger.debug('Saved chunk to disk', {
        cx: chunkData.cx,
        cz: chunkData.cz,
        blockCount: chunkData.blocks.length,
      });
    } catch (error) {
      logger.error('Failed to save chunk', { cx: chunkData.cx, cz: chunkData.cz }, error as Error);
      throw error;
    }
  }

  /**
   * Delete chunk from disk
   */
  async delete(cx: number, cz: number): Promise<boolean> {
    try {
      const chunkPath = this.getChunkPath(cx, cz);
      await fs.unlink(chunkPath);
      logger.debug('Deleted chunk from disk', { cx, cz });
      return true;
    } catch (error) {
      if ((error as any).code === 'ENOENT') {
        return false; // Chunk doesn't exist
      }
      logger.error('Failed to delete chunk', { cx, cz }, error as Error);
      throw error;
    }
  }

  /**
   * List all stored chunks
   */
  async listChunks(): Promise<Array<{ cx: number; cz: number }>> {
    try {
      const files = await fs.readdir(this.basePath);
      const chunks: Array<{ cx: number; cz: number }> = [];

      for (const file of files) {
        const match = file.match(/^chunk_(-?\d+)_(-?\d+)\.json$/);
        if (match) {
          chunks.push({
            cx: parseInt(match[1], 10),
            cz: parseInt(match[2], 10),
          });
        }
      }

      return chunks;
    } catch (error) {
      logger.error('Failed to list chunks', {}, error as Error);
      return [];
    }
  }

  /**
   * Get storage statistics
   */
  async getStats(): Promise<{
    chunkCount: number;
    totalSize: number;
  }> {
    try {
      const files = await fs.readdir(this.basePath);
      let totalSize = 0;
      let chunkCount = 0;

      for (const file of files) {
        if (file.endsWith('.json')) {
          const filePath = path.join(this.basePath, file);
          const stats = await fs.stat(filePath);
          totalSize += stats.size;
          chunkCount++;
        }
      }

      return { chunkCount, totalSize };
    } catch (error) {
      logger.error('Failed to get storage stats', {}, error as Error);
      return { chunkCount: 0, totalSize: 0 };
    }
  }
}
