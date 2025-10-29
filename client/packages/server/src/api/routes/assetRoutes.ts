/**
 * Asset Routes - Serves static assets (textures, models, etc.)
 *
 * Provides lazy loading of assets from the filesystem.
 * Assets are served from files/assets/ directory.
 */

import express, { Router } from 'express';
import path from 'path';
import { promises as fs } from 'fs';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('AssetRoutes');

export function createAssetRoutes(): Router {
  const router = express.Router();

  /**
   * GET /api/worlds/:worldId/assets/*
   *
   * Serves asset files from files/assets/ directory.
   * Example: /api/worlds/test-world-1/assets/textures/block/basic/stone.png
   *
   * The worldId parameter is validated but currently all worlds share the same assets.
   * In the future, per-world asset overrides could be implemented.
   */
  router.get('/:worldId/assets/*', async (req, res): Promise<void> => {
    const worldId = req.params.worldId;
    try {
      // Extract path after /assets/
      const fullPath = req.path; // e.g., "/test-world-1/assets/textures/block/basic/stone.png"
      const assetsIndex = fullPath.indexOf('/assets/');
      const assetPath = assetsIndex !== -1 ? fullPath.substring(assetsIndex + '/assets/'.length) : '';

      if (!assetPath) {
        res.status(400).json({ error: 'Asset path is required' });
        return;
      }

      // Construct file path
      // Server files/assets/ directory maps to /assets/ in URL
      const filePath = path.join(process.cwd(), 'files', 'assets', assetPath);

      // Security check: Ensure path doesn't escape the assets directory
      const normalizedPath = path.normalize(filePath);
      const assetsDir = path.join(process.cwd(), 'files', 'assets');
      if (!normalizedPath.startsWith(assetsDir)) {
        logger.warn('Attempt to access file outside assets directory', { assetPath, worldId });
        res.status(403).json({ error: 'Access denied' });
        return;
      }

      // Check if file exists
      try {
        await fs.access(filePath);
      } catch {
        logger.debug('Asset not found', { assetPath, worldId });
        res.status(404).json({ error: 'Asset not found' });
        return;
      }

      // Get file stats for content-length
      const stats = await fs.stat(filePath);
      if (!stats.isFile()) {
        res.status(400).json({ error: 'Path is not a file' });
        return;
      }

      // Set content type based on file extension
      const ext = path.extname(filePath).toLowerCase();
      const contentTypes: Record<string, string> = {
        '.png': 'image/png',
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.gif': 'image/gif',
        '.webp': 'image/webp',
        '.svg': 'image/svg+xml',
        '.json': 'application/json',
        '.obj': 'text/plain',
        '.mtl': 'text/plain',
      };

      const contentType = contentTypes[ext] || 'application/octet-stream';
      res.setHeader('Content-Type', contentType);
      res.setHeader('Content-Length', stats.size);

      // Enable CORS for cross-origin requests
      res.setHeader('Access-Control-Allow-Origin', '*');
      res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
      res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

      // Cache headers (textures don't change often)
      res.setHeader('Cache-Control', 'public, max-age=86400'); // 24 hours

      // Send file
      res.sendFile(filePath);

      logger.debug('Served asset', { assetPath, worldId, size: stats.size });
    } catch (error) {
      logger.error('Error serving asset', { worldId }, error as Error);
      res.status(500).json({ error: 'Failed to serve asset' });
    }
  });

  // Handle OPTIONS requests for CORS preflight
  router.options('/:worldId/assets/*', (_req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
    res.status(204).send();
  });

  logger.info('Asset routes initialized');
  return router;
}
