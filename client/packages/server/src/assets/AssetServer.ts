/**
 * Asset HTTP Server
 *
 * Serves assets via HTTP alongside the WebSocket server
 */

import * as http from 'http';
import * as fs from 'fs';
import * as path from 'path';
import type { AssetManager } from './AssetManager.js';
import type { WorldManager } from '../world/WorldManager.js';

export class AssetServer {
  private server: http.Server;
  private assetManager: AssetManager;
  private worldManager?: WorldManager;
  private port: number;

  constructor(assetManager: AssetManager, port: number, worldManager?: WorldManager) {
    this.assetManager = assetManager;
    this.worldManager = worldManager;
    this.port = port;

    // Create HTTP server
    this.server = http.createServer((req, res) => {
      this.handleRequest(req, res);
    });
  }

  /**
   * Start asset server
   */
  async start(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.server.listen(this.port, () => {
        console.log(`[AssetServer] Asset HTTP server started on port ${this.port}`);
        resolve();
      });

      this.server.on('error', reject);
    });
  }

  /**
   * Stop asset server
   */
  async stop(): Promise<void> {
    return new Promise((resolve) => {
      this.server.close(() => {
        console.log('[AssetServer] Asset HTTP server stopped');
        resolve();
      });
    });
  }

  /**
   * Handle HTTP request
   */
  private handleRequest(req: http.IncomingMessage, res: http.ServerResponse): void {
    const url = req.url || '/';

    // Enable CORS for all requests
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    // Handle OPTIONS (CORS preflight)
    if (req.method === 'OPTIONS') {
      res.writeHead(204);
      res.end();
      return;
    }

    // Only allow GET requests
    if (req.method !== 'GET') {
      res.writeHead(405, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Method not allowed' }));
      return;
    }

    // Route requests
    if (url === '/manifest' || url === '/manifest.json') {
      this.serveManifest(res);
    } else if (url === '/worlds' || url === '/worlds.json') {
      this.serveWorlds(res);
    } else if (url.startsWith('/assets/')) {
      this.serveAsset(url.substring(8), res); // Remove '/assets/' prefix
    } else if (url === '/' || url === '/health') {
      this.serveHealth(res);
    } else {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Not found' }));
    }
  }

  /**
   * Serve asset manifest
   */
  private serveManifest(res: http.ServerResponse): void {
    try {
      const manifest = this.assetManager.getManifest();

      res.writeHead(200, {
        'Content-Type': 'application/json',
        'Cache-Control': 'public, max-age=3600', // Cache for 1 hour
      });

      res.end(JSON.stringify(manifest));
    } catch (error) {
      console.error('[AssetServer] Error serving manifest:', error);
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Internal server error' }));
    }
  }

  /**
   * Serve asset file
   */
  private serveAsset(assetPath: string, res: http.ServerResponse): void {
    try {
      // Decode URI component and normalize path
      const decodedPath = decodeURIComponent(assetPath);

      // Find asset by path
      const manifest = this.assetManager.getManifest();
      const asset = manifest.assets.find(a => a.path === decodedPath);

      if (!asset) {
        res.writeHead(404, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Asset not found' }));
        return;
      }

      const filePath = this.assetManager.getAssetPath(asset.id);
      if (!filePath || !fs.existsSync(filePath)) {
        res.writeHead(404, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Asset file not found' }));
        return;
      }

      // Read and serve file
      const stats = fs.statSync(filePath);
      const fileStream = fs.createReadStream(filePath);

      res.writeHead(200, {
        'Content-Type': asset.mimeType,
        'Content-Length': stats.size,
        'Cache-Control': 'public, max-age=31536000', // Cache for 1 year (immutable)
        'ETag': asset.hash || '',
      });

      fileStream.pipe(res);

      fileStream.on('error', (error) => {
        console.error('[AssetServer] Error streaming file:', error);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Error reading file' }));
      });
    } catch (error) {
      console.error('[AssetServer] Error serving asset:', error);
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Internal server error' }));
    }
  }

  /**
   * Serve worlds list
   */
  private serveWorlds(res: http.ServerResponse): void {
    try {
      if (!this.worldManager) {
        res.writeHead(503, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'World manager not available' }));
        return;
      }

      // Get loaded worlds
      const loadedWorlds = this.worldManager.getAllWorlds();
      const loadedWorldNames = new Set(Array.from(loadedWorlds.keys()));

      // Get all worlds from disk
      const worldsDir = './worlds';
      const worldList: Array<{ name: string; loaded: boolean }> = [];

      if (fs.existsSync(worldsDir)) {
        const entries = fs.readdirSync(worldsDir, { withFileTypes: true });

        for (const entry of entries) {
          if (entry.isDirectory()) {
            const worldPath = path.join(worldsDir, entry.name);
            const metadataPath = path.join(worldPath, 'world.json');

            // Check if it's a valid world (has world.json)
            if (fs.existsSync(metadataPath)) {
              worldList.push({
                name: entry.name,
                loaded: loadedWorldNames.has(entry.name),
              });
            }
          }
        }
      }

      // Sort worlds: loaded first, then alphabetically
      worldList.sort((a, b) => {
        if (a.loaded === b.loaded) {
          return a.name.localeCompare(b.name);
        }
        return a.loaded ? -1 : 1;
      });

      res.writeHead(200, {
        'Content-Type': 'application/json',
        'Cache-Control': 'no-cache', // Don't cache world list
      });

      res.end(JSON.stringify({
        worlds: worldList,
        defaultWorld: worldList.length > 0 ? worldList[0].name : null,
      }));
    } catch (error) {
      console.error('[AssetServer] Error serving worlds:', error);
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Internal server error' }));
    }
  }

  /**
   * Serve health check
   */
  private serveHealth(res: http.ServerResponse): void {
    const manifest = this.assetManager.getManifest();

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      assetCount: manifest.assets.length,
      version: manifest.version,
    }));
  }

  /**
   * Get server port
   */
  getPort(): number {
    return this.port;
  }
}
