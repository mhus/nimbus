/**
 * BackdropService - Manages backdrop rendering from chunk data
 *
 * Backdrops are pseudo-walls rendered at chunk edges based on chunk data.
 * They prevent sun from shining into tunnels and provide far-away rendering with alpha fading.
 *
 * Key features:
 * - Renders backdrops from chunk data (not dynamically calculated)
 * - Each backdrop side has its own mesh
 * - Reacts to chunk load/unload events
 * - Tracks and disposes unused backdrops
 * - Manages separate material cache via BackdropMaterialManager
 */

import { Mesh, VertexData, type Scene } from '@babylonjs/core';
import {
  getLogger,
  ExceptionHandler,
  type Backdrop,
} from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { ChunkService } from './ChunkService';
import { BackdropMaterialManager } from './BackdropMaterialManager';
import { getChunkKey } from '../utils/ChunkUtils';

const logger = getLogger('BackdropService');

/**
 * Backdrop mesh key format: "{direction}{cx},{cz}"
 * Examples: "n3,4" (north backdrop for chunk 3,4), "e0,-1", "s2,5", "w-1,3"
 */
type BackdropKey = string;

/**
 * BackdropService - Manages backdrop rendering from chunk data
 */
export class BackdropService {
  private scene: Scene;
  private chunkService: ChunkService;
  private materialManager: BackdropMaterialManager;

  /** Backdrop meshes by key (format: "cx,cz:direction") */
  private backdropMeshes = new Map<BackdropKey, Mesh>();

  constructor(scene: Scene, appContext: AppContext) {
    this.scene = scene;
    this.chunkService = appContext.services.chunk as ChunkService;
    this.materialManager = new BackdropMaterialManager(scene, appContext);

    this.setupEventListeners();

    logger.info('BackdropService initialized');
  }

  /**
   * Setup chunk event listeners
   */
  private setupEventListeners(): void {
    this.chunkService.on('chunk:loaded', () => {
      this.updateBackdrops();
    });

    this.chunkService.on('chunk:unloaded', () => {
      this.updateBackdrops();
    });

    logger.debug('Chunk event listeners registered');
  }

  /**
   * Update backdrops based on currently loaded chunks
   *
   * This is called whenever chunks are loaded or unloaded.
   * For each chunk, check if neighbors exist. If not, draw backdrop on that side.
   */
  private async updateBackdrops(): Promise<void> {
    try {
      logger.debug('Updating backdrops');

      const allChunks = this.chunkService.getAllChunks();

      // Build set of loaded chunk coordinates for fast lookup
      const loadedCoords = new Set<string>();
      for (const chunk of allChunks) {
        const cx = chunk.data.transfer.cx;
        const cz = chunk.data.transfer.cz;
        loadedCoords.add(getChunkKey(cx, cz));
      }

      // Collect all needed backdrops by checking each chunk's neighbors
      const neededBackdrops = new Set<BackdropKey>();

      // Check each loaded chunk for missing neighbors
      for (const chunk of allChunks) {
        const cx = chunk.data.transfer.cx;
        const cz = chunk.data.transfer.cz;

        // Check North neighbor (cz+1)
        if (!loadedCoords.has(getChunkKey(cx, cz + 1))) {
          neededBackdrops.add(this.getBackdropKey(cx, cz, 'n'));
        }

        // Check South neighbor (cz-1)
        if (!loadedCoords.has(getChunkKey(cx, cz - 1))) {
          neededBackdrops.add(this.getBackdropKey(cx, cz, 's'));
        }

        // Check East neighbor (cx+1)
        if (!loadedCoords.has(getChunkKey(cx + 1, cz))) {
          neededBackdrops.add(this.getBackdropKey(cx, cz, 'e'));
        }

        // Check West neighbor (cx-1)
        if (!loadedCoords.has(getChunkKey(cx - 1, cz))) {
          neededBackdrops.add(this.getBackdropKey(cx, cz, 'w'));
        }
      }

      logger.debug('Collected needed backdrops', {
        count: neededBackdrops.size,
      });

      // Dispose backdrops that are no longer needed
      this.disposeUnneededBackdrops(neededBackdrops);

      // Create new backdrops where needed
      await this.createNeededBackdrops(allChunks, neededBackdrops);

      logger.debug('Backdrops updated successfully', {
        renderedBackdrops: this.backdropMeshes.size,
      });
    } catch (error) {
      ExceptionHandler.handle(error, 'BackdropService.updateBackdrops');
    }
  }

  /**
   * Generate backdrop key from chunk coordinates and direction
   * Format: "{direction}{cx},{cz}" (e.g., "n3,4", "s0,-1", "e2,5", "w-1,3")
   */
  private getBackdropKey(cx: number, cz: number, direction: string): BackdropKey {
    return `${direction}${cx},${cz}`;
  }


  /**
   * Dispose backdrops that are no longer needed
   */
  private disposeUnneededBackdrops(neededBackdrops: Set<BackdropKey>): void {
    // Find and dispose unneeded backdrops
    for (const [key, mesh] of this.backdropMeshes) {
      if (!neededBackdrops.has(key)) {
        logger.debug('Disposing unneeded backdrop', { key });
        mesh.dispose();
        this.backdropMeshes.delete(key);
      }
    }
  }

  /**
   * Create backdrops where needed
   */
  private async createNeededBackdrops(
    allChunks: any[],
    neededBackdrops: Set<BackdropKey>
  ): Promise<void> {
    // Build a map of chunks by coordinate for fast lookup
    const chunkMap = new Map<string, any>();
    for (const chunk of allChunks) {
      const cx = chunk.data.transfer.cx;
      const cz = chunk.data.transfer.cz;
      chunkMap.set(getChunkKey(cx, cz), chunk);
    }

    // Create each needed backdrop
    for (const key of neededBackdrops) {
      // Skip if already exists
      if (this.backdropMeshes.has(key)) {
        continue;
      }

      // Parse key to get chunk coordinates and direction
      // Format: "n3,4" -> direction='n', cx=3, cz=4
      const direction = key[0];
      const coords = key.substring(1);
      const [cx, cz] = coords.split(',').map(Number);

      // Get the chunk
      const chunk = chunkMap.get(getChunkKey(cx, cz));
      if (!chunk || !chunk.data.backdrop) {
        continue;
      }

      // Get backdrop data for this direction
      const backdrop = chunk.data.backdrop;
      let backdropData: Array<Backdrop> | undefined;

      switch (direction) {
        case 'n':
          backdropData = backdrop.n;
          break;
        case 's':
          backdropData = backdrop.s;
          break;
        case 'e':
          backdropData = backdrop.e;
          break;
        case 'w':
          backdropData = backdrop.w;
          break;
      }

      if (!backdropData || backdropData.length === 0) {
        continue;
      }

      // Create the backdrop
      await this.createBackdrop(cx, cz, direction, backdropData);
    }
  }

  /**
   * Create backdrop mesh for a specific chunk side
   */
  private async createBackdrop(
    cx: number,
    cz: number,
    direction: string,
    backdrops: Array<Backdrop>
  ): Promise<void> {
    try {
      const key = this.getBackdropKey(cx, cz, direction);

      logger.debug('Creating backdrop', { cx, cz, direction });

      // Use first backdrop for now (TODO: support multiple backdrops per side)
      const backdropConfig = backdrops[0];

      // Get material
      const material = await this.materialManager.getBackdropMaterial(backdropConfig);

      // Create mesh
      const mesh = this.createBackdropMesh(cx, cz, direction, backdropConfig);

      if (mesh) {
        mesh.material = material;
        mesh.renderingGroupId = 0; // Render first (background)
        mesh.name = `backdrop_${key}`;

        // Store mesh
        this.backdropMeshes.set(key, mesh);

        logger.debug('Backdrop created', { key });
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'BackdropService.createBackdrop', {
        cx,
        cz,
        direction,
      });
    }
  }

  /**
   * Create a single backdrop mesh for one direction
   */
  private createBackdropMesh(
    cx: number,
    cz: number,
    direction: string,
    config: Backdrop
  ): Mesh | null {
    try {
      // Get chunk size from world info
      const chunkSize = this.chunkService['appContext'].worldInfo?.chunkSize || 16;

      // Calculate world coordinates
      const worldX = cx * chunkSize;
      const worldZ = cz * chunkSize;

      // Get position range from config (with defaults)
      const la = config.la ?? 0;
      const lb = config.lb ?? chunkSize;
      const yStart = config.ya ?? 0;
      const yEnd = config.yb ?? (config.ya ?? 0) + 60;

      // Create mesh based on direction
      let x1: number, z1: number, x2: number, z2: number;

      switch (direction) {
        case 'n':
          // North edge (negative Z)
          x1 = worldX + la;
          z1 = worldZ;
          x2 = worldX + lb;
          z2 = worldZ;
          break;

        case 'e':
          // East edge (positive X)
          x1 = worldX + chunkSize;
          z1 = worldZ + la;
          x2 = worldX + chunkSize;
          z2 = worldZ + lb;
          break;

        case 's':
          // South edge (positive Z)
          x1 = worldX + lb;
          z1 = worldZ + chunkSize;
          x2 = worldX + la;
          z2 = worldZ + chunkSize;
          break;

        case 'w':
          // West edge (negative X)
          x1 = worldX;
          z1 = worldZ + lb;
          x2 = worldX;
          z2 = worldZ + la;
          break;

        default:
          logger.warn('Unknown backdrop direction', { direction });
          return null;
      }

      // Create vertical plane mesh
      const mesh = this.createVerticalPlane(x1, z1, x2, z2, yStart, yEnd);

      return mesh;
    } catch (error) {
      ExceptionHandler.handle(error, 'BackdropService.createBackdropMesh', {
        cx,
        cz,
        direction,
      });
      return null;
    }
  }

  /**
   * Create a vertical plane mesh
   */
  private createVerticalPlane(
    x1: number,
    z1: number,
    x2: number,
    z2: number,
    yStart: number,
    yEnd: number
  ): Mesh {
    const mesh = new Mesh('backdrop_plane', this.scene);

    // Create quad vertices
    const positions = [
      x1,
      yStart,
      z1, // Bottom left
      x2,
      yStart,
      z2, // Bottom right
      x2,
      yEnd,
      z2, // Top right
      x1,
      yEnd,
      z1, // Top left
    ];

    const indices = [
      0,
      1,
      2, // First triangle
      0,
      2,
      3, // Second triangle
    ];

    const uvs = [
      0,
      1, // Bottom left
      1,
      1, // Bottom right
      1,
      0, // Top right
      0,
      0, // Top left
    ];

    // Calculate normals (perpendicular to plane, facing inward)
    const dx = x2 - x1;
    const dz = z2 - z1;
    const len = Math.sqrt(dx * dx + dz * dz);

    // Normal perpendicular to edge, pointing inward to loaded chunks
    const nx = -dz / len;
    const nz = dx / len;

    const normals = [nx, 0, nz, nx, 0, nz, nx, 0, nz, nx, 0, nz];

    // Apply vertex data
    const vertexData = new VertexData();
    vertexData.positions = positions;
    vertexData.indices = indices;
    vertexData.uvs = uvs;
    vertexData.normals = normals;

    vertexData.applyToMesh(mesh);

    return mesh;
  }


  /**
   * Get statistics
   */
  getStats(): {
    backdropCount: number;
    materialCacheSize: number;
  } {
    return {
      backdropCount: this.backdropMeshes.size,
      materialCacheSize: this.materialManager.getStats().cachedMaterials,
    };
  }

  /**
   * Dispose all backdrops and cleanup resources
   */
  dispose(): void {
    logger.info('Disposing BackdropService', {
      backdropCount: this.backdropMeshes.size,
    });

    // Dispose all meshes
    for (const mesh of this.backdropMeshes.values()) {
      mesh.dispose();
    }

    this.backdropMeshes.clear();

    // Dispose materials
    this.materialManager.dispose();

    logger.info('BackdropService disposed');
  }
}
