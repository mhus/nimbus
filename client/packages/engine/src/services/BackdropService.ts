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

import { Mesh, VertexData, MeshBuilder, Color3, Vector3, type Scene } from '@babylonjs/core';
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

    // Initial update in case chunks are already loaded
    setTimeout(() => {
      logger.info('Running initial backdrop update...');
      this.updateBackdrops();
    }, 1000);
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
      const allChunks = this.chunkService.getAllChunks();

      logger.info('Updating backdrops', { loadedChunks: allChunks.length });

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

      logger.info('Collected needed backdrops', {
        count: neededBackdrops.size,
        backdrops: Array.from(neededBackdrops),
      });

      // Dispose backdrops that are no longer needed
      this.disposeUnneededBackdrops(neededBackdrops);

      // Create new backdrops where needed
      await this.createNeededBackdrops(allChunks, neededBackdrops);

      logger.info('Backdrops updated successfully', {
        renderedBackdrops: this.backdropMeshes.size,
        meshKeys: Array.from(this.backdropMeshes.keys()),
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
      if (!chunk) {
        logger.warn('Chunk not found for backdrop', { cx, cz, key });
        continue;
      }

      if (!chunk.data.backdrop) {
        logger.warn('Chunk has no backdrop data', { cx, cz, key });
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

      logger.info('Creating backdrop', { cx, cz, direction, backdropConfig: backdrops[0] });

      // Use first backdrop for now (TODO: support multiple backdrops per side)
      const backdropConfig = backdrops[0];

      // Create mesh (temporarily lines for debugging)
      const mesh = this.createBackdropMesh(cx, cz, direction, backdropConfig);

      if (mesh) {
        // TEMPORARY: Skip material for lines debugging
        // const material = await this.materialManager.getBackdropMaterial(backdropConfig);
        // mesh.material = material;

        // Rendering settings
        mesh.renderingGroupId = 1; // Render after blocks but before transparent objects
        mesh.name = `backdrop_${key}`;

        // Store mesh
        this.backdropMeshes.set(key, mesh);

        // Debug: Check scene hierarchy
        const sceneRootNodes = this.scene.rootNodes.map(n => n.name);
        const meshesInScene = this.scene.meshes.length;

        logger.info('Backdrop LINES created successfully', {
          key,
          meshName: mesh.name,
          meshType: mesh.getClassName(),
          position: mesh.position,
          absolutePosition: mesh.getAbsolutePosition(),
          boundingBox: {
            min: mesh.getBoundingInfo().boundingBox.minimumWorld,
            max: mesh.getBoundingInfo().boundingBox.maximumWorld
          },
          sceneInfo: {
            totalMeshesInScene: meshesInScene,
            rootNodeCount: sceneRootNodes.length
          }
        });
      } else {
        logger.warn('Failed to create backdrop mesh', { key });
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

      // Calculate world coordinates of chunk origin
      const worldX = cx * chunkSize;
      const worldZ = cz * chunkSize;

      // Get local coordinates (0-16 within chunk)
      const left = config.left ?? 0;
      const width = config.width ?? chunkSize;
      const right = left + width;

      // Calculate Y coordinates
      let yBase: number;
      if (config.yBase !== undefined) {
        yBase = config.yBase;
      } else {
        // Calculate groundLevel at this edge
        yBase = this.calculateGroundLevelAtEdge(cx, cz, direction, left, right);
      }

      const height = config.height ?? 60;
      const yUp = yBase + height;

      logger.info('Creating backdrop mesh', {
        chunk: { cx, cz },
        direction,
        worldPos: { worldX, worldZ },
        chunkSize,
        config: { left, width, right, yBase, height, yUp }
      });

      // Create mesh based on direction
      // left and width are local coordinates (0-16)
      let x1: number, z1: number, x2: number, z2: number;

      switch (direction) {
        case 'n':
          // North edge (positive Z direction, upper edge of chunk)
          // z = worldZ + chunkSize (e.g., chunk 1,1 -> z=32)
          // x goes from left to (left + width)
          x1 = worldX + left;
          z1 = worldZ + chunkSize;
          x2 = worldX + right;
          z2 = worldZ + chunkSize;
          break;

        case 's':
          // South edge (negative Z direction, lower edge of chunk)
          // z = worldZ (e.g., chunk 1,1 -> z=16)
          // x goes from left to (left + width)
          x1 = worldX + left;
          z1 = worldZ;
          x2 = worldX + right;
          z2 = worldZ;
          break;

        case 'e':
          // East edge (positive X direction, right edge of chunk)
          // x = worldX + chunkSize (e.g., chunk 1,1 -> x=32)
          // z goes from left to (left + width)
          x1 = worldX + chunkSize;
          z1 = worldZ + left;
          x2 = worldX + chunkSize;
          z2 = worldZ + right;
          break;

        case 'w':
          // West edge (negative X direction, left edge of chunk)
          // x = worldX (e.g., chunk 1,1 -> x=16)
          // z goes from left to (left + width)
          x1 = worldX;
          z1 = worldZ + left;
          x2 = worldX;
          z2 = worldZ + right;
          break;

        default:
          logger.warn('Unknown backdrop direction', { direction });
          return null;
      }

      logger.info('Backdrop mesh coordinates calculated', {
        direction,
        chunk: { cx, cz },
        localCoords: { left, width, right },
        worldCoords: {
          x1, z1, x2, z2,
          yBase, yUp
        },
        corners: {
          bottomLeft: { x: x1, z: z1, y: yBase },
          bottomRight: { x: x2, z: z2, y: yBase },
          topRight: { x: x2, z: z2, y: yUp },
          topLeft: { x: x1, z: z1, y: yUp }
        }
      });

      // Create vertical plane mesh
      const mesh = this.createVerticalPlane(x1, z1, x2, z2, yBase, yUp);

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
   * Calculate ground level at the edge of a chunk
   */
  private calculateGroundLevelAtEdge(
    cx: number,
    cz: number,
    direction: string,
    left: number,
    right: number
  ): number {
    try {
      const chunk = this.chunkService.getChunk(cx, cz);
      if (!chunk || !chunk.data.hightData) {
        return 0; // Default if no height data
      }

      const heightData = chunk.data.hightData;
      let minGroundLevel = Infinity;

      // Sample ground level along the edge
      const samples = Math.max(3, Math.ceil(right - left)); // At least 3 samples
      for (let i = 0; i <= samples; i++) {
        const t = i / samples;
        const localCoord = Math.floor(left + (right - left) * t);

        let x: number, z: number;
        switch (direction) {
          case 'n':
          case 's':
            x = localCoord;
            z = direction === 'n' ? 0 : 15;
            break;
          case 'e':
          case 'w':
            x = direction === 'w' ? 0 : 15;
            z = localCoord;
            break;
          default:
            continue;
        }

        const key = `${x},${z}`;
        const height = heightData.get(key);
        if (height) {
          const groundLevel = height[4]; // groundLevel is at index 4
          if (groundLevel < minGroundLevel) {
            minGroundLevel = groundLevel;
          }
        }
      }

      const result = minGroundLevel === Infinity ? 0 : minGroundLevel;
      logger.info('Calculated ground level at edge', {
        chunk: { cx, cz },
        direction,
        edge: { left, right },
        groundLevel: result
      });

      return result;
    } catch (error) {
      logger.warn('Failed to calculate ground level, using 0', {
        cx, cz, direction, error
      });
      return 0;
    }
  }

  /**
   * Create a vertical plane mesh
   *
   * TEMPORARY DEBUG: Drawing lines instead of mesh to verify coordinates
   */
  private createVerticalPlane(
    x1: number,
    z1: number,
    x2: number,
    z2: number,
    yStart: number,
    yEnd: number
  ): Mesh {
    logger.info('Creating vertical plane with LINES', {
      x1, z1, x2, z2, yStart, yEnd,
      width: Math.sqrt((x2-x1)**2 + (z2-z1)**2),
      height: yEnd - yStart
    });

    // Define the 4 corners of the rectangle in ABSOLUTE WORLD COORDINATES
    const bottomLeft = new Vector3(x1, yStart, z1);
    const bottomRight = new Vector3(x2, yStart, z2);
    const topRight = new Vector3(x2, yEnd, z2);
    const topLeft = new Vector3(x1, yEnd, z1);

    logger.info('Rectangle corners', {
      bottomLeft: { x: x1, y: yStart, z: z1 },
      bottomRight: { x: x2, y: yStart, z: z2 },
      topRight: { x: x2, y: yEnd, z: z2 },
      topLeft: { x: x1, y: yEnd, z: z1 }
    });

    // Create lines for the rectangle outline (4 edges)
    const points = [
      // Bottom edge
      bottomLeft,
      bottomRight,
      // Right edge
      bottomRight,
      topRight,
      // Top edge
      topRight,
      topLeft,
      // Left edge
      topLeft,
      bottomLeft
    ];

    // Create lines with MeshBuilder
    const linesMesh = MeshBuilder.CreateLines(
      'backdrop_lines',
      {
        points: points,
      },
      this.scene
    );

    // Make lines red and thick for visibility
    linesMesh.color = Color3.Red();
    linesMesh.isPickable = false;

    logger.info('Lines created', {
      linesMeshPosition: linesMesh.position,
      parent: linesMesh.parent ? 'HAS PARENT!' : 'no parent',
      absolutePosition: linesMesh.getAbsolutePosition()
    });

    return linesMesh;
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
