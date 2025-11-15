/**
 * Speech Routes - Serves speech/narration audio files
 *
 * Provides streaming of speech audio files from the filesystem.
 * Files are served from files/speech/ directory.
 */

import express, { Router } from 'express';
import path from 'path';
import { promises as fs } from 'fs';
import { getLogger } from '@nimbus/shared';
import type { WorldManager } from '../../world/WorldManager';

const logger = getLogger('SpeechRoutes');

export function createSpeechRoutes(worldManager: WorldManager): Router {
  const router = express.Router();

  /**
   * GET /api/world/:worldId/speech/:streamPath
   *
   * Serves speech audio files from files/speech/ directory.
   * Example: /api/world/test-world-1/speech/welcome
   * File: files/speech/welcome.ogg
   *
   * Query Parameters:
   * - sessionId: Session ID for authentication (optional)
   * - authToken: Authentication token (optional)
   */
  router.get('/:worldId/speech/*', async (req, res): Promise<void> => {
    const worldId = req.params.worldId;
    const sessionId = req.query.sessionId as string | undefined;
    const authToken = req.query.authToken as string | undefined;

    // Extract stream path early for error logging
    const fullPath = req.path;
    const speechIndex = fullPath.indexOf('/speech/');
    const streamPath = speechIndex !== -1 ? fullPath.substring(speechIndex + '/speech/'.length) : '';

    try {
      // Validate world exists
      const world = worldManager.getWorld(worldId);
      if (!world) {
        res.status(404).json({ error: 'World not found' });
        return;
      }

      if (!streamPath) {
        res.status(400).json({ error: 'Stream path is required' });
        return;
      }

      logger.info('Speech request', { worldId, streamPath, sessionId, hasAuthToken: !!authToken });

      // TODO: Validate sessionId and authToken here if needed
      // For test server, we skip authentication

      // Construct file path
      // Server files/speech/ directory maps to /speech/ in URL
      // Always append .ogg extension
      const fileName = streamPath.endsWith('.ogg') ? streamPath : `${streamPath}.ogg`;
      const filePath = path.join(process.cwd(), 'files', 'speech', fileName);

      // Security check: Ensure path doesn't escape the speech directory
      const normalizedPath = path.normalize(filePath);
      const speechDir = path.join(process.cwd(), 'files', 'speech');
      if (!normalizedPath.startsWith(speechDir)) {
        logger.warn('Attempt to access file outside speech directory', { streamPath, worldId });
        res.status(403).json({ error: 'Access denied' });
        return;
      }

      // Check if file exists
      try {
        await fs.access(filePath);
      } catch {
        logger.warn('Speech file not found', { streamPath, worldId, filePath });
        res.status(404).json({ error: 'Speech file not found' });
        return;
      }

      // Get file stats
      const stats = await fs.stat(filePath);
      if (!stats.isFile()) {
        res.status(400).json({ error: 'Path is not a file' });
        return;
      }

      // Set headers for audio streaming
      res.setHeader('Content-Type', 'audio/ogg');
      res.setHeader('Content-Length', stats.size);
      res.setHeader('Accept-Ranges', 'bytes');

      // Enable CORS
      res.setHeader('Access-Control-Allow-Origin', '*');
      res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
      res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Range');

      // No cache for speech (may be dynamic/personalized)
      res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');

      // Send file
      res.sendFile(filePath);

      logger.info('Served speech file', { streamPath, worldId, size: stats.size });
    } catch (error) {
      logger.error('Error serving speech', { worldId, streamPath }, error as Error);
      res.status(500).json({ error: 'Failed to serve speech' });
    }
  });

  // Handle OPTIONS requests for CORS preflight
  router.options('/:worldId/speech/*', (_req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Range');
    res.status(204).send();
  });

  return router;
}
