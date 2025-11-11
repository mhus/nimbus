/**
 * Backdrop Routes - Serves backdrop type configurations
 *
 * Backdrops are served from files/backdrops/{id}.json
 * These define visual properties for backdrop rendering at chunk boundaries
 */

import { Router } from 'express';
import path from 'path';
import { promises as fs } from 'fs';
import { getLogger, ExceptionHandler } from '@nimbus/shared';

const logger = getLogger('BackdropRoutes');

export function createBackdropRoutes(): Router {
  const router = Router();

  /**
   * GET /api/backdrop/:id
   *
   * Serves backdrop configuration from files/backdrops/{id}.json
   * Example: GET /api/backdrop/stone -> files/backdrops/stone.json
   *
   * Returns BackdropConfig JSON with texture path, alpha, color, etc.
   */
  router.get('/:id', async (req, res) => {
    const backdropId = req.params.id;

    try {
      // Sanitize ID (alphanumeric, dash, underscore only)
      if (!/^[a-zA-Z0-9_-]+$/.test(backdropId)) {
        logger.warn('Invalid backdrop ID requested', { backdropId });
        return res.status(400).json({ error: 'Invalid backdrop ID' });
      }

      const filePath = path.join(
        process.cwd(),
        'files',
        'backdrops',
        `${backdropId}.json`
      );

      // Security check: ensure path is within backdrops directory
      const normalizedPath = path.normalize(filePath);
      const backdropsDir = path.join(process.cwd(), 'files', 'backdrops');

      if (!normalizedPath.startsWith(backdropsDir)) {
        logger.warn('Attempt to access file outside backdrops directory', {
          backdropId,
        });
        return res.status(403).json({ error: 'Access denied' });
      }

      // Check if file exists
      try {
        await fs.access(filePath);
      } catch {
        logger.debug('Backdrop not found', { backdropId });
        return res.status(404).json({ error: 'Backdrop not found' });
      }

      // Read and parse JSON
      const fileContent = await fs.readFile(filePath, 'utf-8');
      const backdrop = JSON.parse(fileContent);

      // Set headers
      res.setHeader('Content-Type', 'application/json');
      res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate'); // No cache during development

      return res.json(backdrop);
    } catch (error) {
      ExceptionHandler.handle(error, 'BackdropRoutes.get', { backdropId });
      return res.status(500).json({ error: 'Failed to serve backdrop' });
    }
  });

  logger.info('Backdrop routes initialized');
  return router;
}
