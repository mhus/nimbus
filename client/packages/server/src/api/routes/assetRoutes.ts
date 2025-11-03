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
import type { WorldManager } from '../../world/WorldManager';

const logger = getLogger('AssetRoutes');

export function createAssetRoutes(worldManager: WorldManager): Router {
  const router = express.Router();

  // GET /api/worlds/:worldId/assets - List/search assets
  router.get('/:worldId/assets', async (req, res) => {
    const worldId = req.params.worldId;
    const world = worldManager.getWorld(worldId);

    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    const query = req.query.query as string | undefined;
    const assetManager = worldManager.getAssetManager();

    try {
      if (query) {
        // Search assets
        const assets = await assetManager.searchAssets(query);
        return res.json({ assets });
      } else {
        // Get all assets
        const assets = await assetManager.getAllAssets();
        return res.json({ assets });
      }
    } catch (error) {
      logger.error('Failed to get assets', { worldId, query }, error as Error);
      return res.status(500).json({ error: 'Failed to get assets' });
    }
  });

  // POST /api/worlds/:worldId/assets/* - Create new asset
  router.post('/:worldId/assets/*', express.raw({ type: '*/*', limit: '10mb' }), async (req, res) => {
    const worldId = req.params.worldId;
    const world = worldManager.getWorld(worldId);

    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    try {
      // Extract asset path after /assets/
      const fullPath = req.path;
      const assetsIndex = fullPath.indexOf('/assets/');
      const assetPath = assetsIndex !== -1 ? fullPath.substring(assetsIndex + '/assets/'.length) : '';

      if (!assetPath) {
        return res.status(400).json({ error: 'Asset path is required' });
      }

      // Get asset data from request body
      const assetData = req.body as Buffer;

      if (!assetData || assetData.length === 0) {
        return res.status(400).json({ error: 'Asset data is required' });
      }

      const assetManager = worldManager.getAssetManager();
      const createdAsset = await assetManager.createAsset(assetPath, assetData);

      if (!createdAsset) {
        return res.status(400).json({ error: 'Failed to create asset (may already exist)' });
      }

      return res.status(201).json(createdAsset);
    } catch (error) {
      logger.error('Failed to create asset', { worldId }, error as Error);
      return res.status(500).json({ error: 'Failed to create asset' });
    }
  });

  // PUT /api/worlds/:worldId/assets/* - Update existing asset
  router.put('/:worldId/assets/*', express.raw({ type: '*/*', limit: '10mb' }), async (req, res) => {
    const worldId = req.params.worldId;
    const world = worldManager.getWorld(worldId);

    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    try {
      // Extract asset path after /assets/
      const fullPath = req.path;
      const assetsIndex = fullPath.indexOf('/assets/');
      const assetPath = assetsIndex !== -1 ? fullPath.substring(assetsIndex + '/assets/'.length) : '';

      if (!assetPath) {
        return res.status(400).json({ error: 'Asset path is required' });
      }

      // Get asset data from request body
      const assetData = req.body as Buffer;

      if (!assetData || assetData.length === 0) {
        return res.status(400).json({ error: 'Asset data is required' });
      }

      const assetManager = worldManager.getAssetManager();
      const updatedAsset = await assetManager.updateAsset(assetPath, assetData);

      if (!updatedAsset) {
        return res.status(404).json({ error: 'Asset not found' });
      }

      return res.json(updatedAsset);
    } catch (error) {
      logger.error('Failed to update asset', { worldId }, error as Error);
      return res.status(500).json({ error: 'Failed to update asset' });
    }
  });

  // DELETE /api/worlds/:worldId/assets/* - Delete asset
  router.delete('/:worldId/assets/*', async (req, res) => {
    const worldId = req.params.worldId;
    const world = worldManager.getWorld(worldId);

    if (!world) {
      return res.status(404).json({ error: 'World not found' });
    }

    try {
      // Extract asset path after /assets/
      const fullPath = req.path;
      const assetsIndex = fullPath.indexOf('/assets/');
      const assetPath = assetsIndex !== -1 ? fullPath.substring(assetsIndex + '/assets/'.length) : '';

      if (!assetPath) {
        return res.status(400).json({ error: 'Asset path is required' });
      }

      const assetManager = worldManager.getAssetManager();
      const deleted = await assetManager.deleteAsset(assetPath);

      if (!deleted) {
        return res.status(404).json({ error: 'Asset not found' });
      }

      return res.status(204).send();
    } catch (error) {
      logger.error('Failed to delete asset', { worldId }, error as Error);
      return res.status(500).json({ error: 'Failed to delete asset' });
    }
  });

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

  // GET /api/worlds/:worldId/assets/*.info - Get asset info file
  router.get('/:worldId/assets/*.info', async (req, res): Promise<void> => {
    const worldId = req.params.worldId;
    const world = worldManager.getWorld(worldId);

    if (!world) {
      res.status(404).json({ error: 'World not found' });
      return;
    }

    try {
      // Extract asset path (remove .info suffix)
      const fullPath = req.path;
      const assetsIndex = fullPath.indexOf('/assets/');
      const infoPath = assetsIndex !== -1 ? fullPath.substring(assetsIndex + '/assets/'.length) : '';

      if (!infoPath || !infoPath.endsWith('.info')) {
        res.status(400).json({ error: 'Invalid .info file path' });
        return;
      }

      // Construct file path for .info file
      const filePath = path.join(process.cwd(), 'files', 'assets', infoPath);

      // Security check: Ensure path doesn't escape the assets directory
      const normalizedPath = path.normalize(filePath);
      const assetsDir = path.join(process.cwd(), 'files', 'assets');
      if (!normalizedPath.startsWith(assetsDir)) {
        logger.warn('Attempt to access file outside assets directory', { infoPath, worldId });
        res.status(403).json({ error: 'Access denied' });
        return;
      }

      // Check if file exists
      try {
        await fs.access(filePath);
      } catch {
        // Return empty object with description if .info file doesn't exist
        logger.debug('Info file not found, returning empty', { infoPath, worldId });
        res.json({ description: '' });
        return;
      }

      // Read and parse JSON file
      const fileContent = await fs.readFile(filePath, 'utf-8');
      const info = JSON.parse(fileContent);

      // Ensure description field exists
      if (!info.description) {
        info.description = '';
      }

      res.json(info);
      logger.debug('Served asset info', { infoPath, worldId });
    } catch (error) {
      logger.error('Error serving asset info', { worldId }, error as Error);
      res.status(500).json({ error: 'Failed to serve asset info' });
    }
  });

  // PUT /api/worlds/:worldId/assets/*.info - Create/update asset info file
  router.put('/:worldId/assets/*.info', express.json(), async (req, res): Promise<void> => {
    const worldId = req.params.worldId;
    const world = worldManager.getWorld(worldId);

    if (!world) {
      res.status(404).json({ error: 'World not found' });
      return;
    }

    try {
      // Extract asset path (remove .info suffix)
      const fullPath = req.path;
      const assetsIndex = fullPath.indexOf('/assets/');
      const infoPath = assetsIndex !== -1 ? fullPath.substring(assetsIndex + '/assets/'.length) : '';

      if (!infoPath || !infoPath.endsWith('.info')) {
        res.status(400).json({ error: 'Invalid .info file path' });
        return;
      }

      // Validate request body
      const info = req.body;
      if (!info || typeof info !== 'object') {
        res.status(400).json({ error: 'Invalid JSON body' });
        return;
      }

      // Ensure description field exists
      if (!info.description || typeof info.description !== 'string') {
        res.status(400).json({ error: 'description field is required' });
        return;
      }

      // Construct file path for .info file
      const filePath = path.join(process.cwd(), 'files', 'assets', infoPath);
      const dirPath = path.dirname(filePath);

      // Security check: Ensure path doesn't escape the assets directory
      const normalizedPath = path.normalize(filePath);
      const assetsDir = path.join(process.cwd(), 'files', 'assets');
      if (!normalizedPath.startsWith(assetsDir)) {
        logger.warn('Attempt to access file outside assets directory', { infoPath, worldId });
        res.status(403).json({ error: 'Access denied' });
        return;
      }

      // Create directory if it doesn't exist
      await fs.mkdir(dirPath, { recursive: true });

      // Write JSON file
      await fs.writeFile(filePath, JSON.stringify(info, null, 2), 'utf-8');

      logger.info('Saved asset info', { infoPath, worldId });
      res.json({ success: true });
    } catch (error) {
      logger.error('Error saving asset info', { worldId }, error as Error);
      res.status(500).json({ error: 'Failed to save asset info' });
    }
  });

  // DELETE /api/worlds/:worldId/assets/*.info - Delete asset info file
  router.delete('/:worldId/assets/*.info', async (req, res): Promise<void> => {
    const worldId = req.params.worldId;
    const world = worldManager.getWorld(worldId);

    if (!world) {
      res.status(404).json({ error: 'World not found' });
      return;
    }

    try {
      // Extract asset path (remove .info suffix)
      const fullPath = req.path;
      const assetsIndex = fullPath.indexOf('/assets/');
      const infoPath = assetsIndex !== -1 ? fullPath.substring(assetsIndex + '/assets/'.length) : '';

      if (!infoPath || !infoPath.endsWith('.info')) {
        res.status(400).json({ error: 'Invalid .info file path' });
        return;
      }

      // Construct file path for .info file
      const filePath = path.join(process.cwd(), 'files', 'assets', infoPath);

      // Security check: Ensure path doesn't escape the assets directory
      const normalizedPath = path.normalize(filePath);
      const assetsDir = path.join(process.cwd(), 'files', 'assets');
      if (!normalizedPath.startsWith(assetsDir)) {
        logger.warn('Attempt to access file outside assets directory', { infoPath, worldId });
        res.status(403).json({ error: 'Access denied' });
        return;
      }

      // Check if file exists
      try {
        await fs.access(filePath);
      } catch {
        logger.debug('Info file not found', { infoPath, worldId });
        res.status(404).json({ error: 'Info file not found' });
        return;
      }

      // Delete file
      await fs.unlink(filePath);

      logger.info('Deleted asset info', { infoPath, worldId });
      res.status(204).send();
    } catch (error) {
      logger.error('Error deleting asset info', { worldId }, error as Error);
      res.status(500).json({ error: 'Failed to delete asset info' });
    }
  });

  logger.info('Asset routes initialized');
  return router;
}
