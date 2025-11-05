/**
 * RenderService - Renders chunks as meshes
 *
 * Manages chunk rendering, mesh generation, and cleanup.
 * Listens to ChunkService events to render/unload chunks.
 */

import { Mesh, VertexData, Scene } from '@babylonjs/core';
import { getLogger, ExceptionHandler, Shape } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { ClientChunk } from '../types/ClientChunk';
import type { ClientBlock } from '../types/ClientBlock';
import type { MaterialService } from './MaterialService';
import type { BlockTypeService } from './BlockTypeService';
import { CubeRenderer } from '../rendering/CubeRenderer';
import type { TextureAtlas } from '../rendering/TextureAtlas';

const logger = getLogger('RenderService');

/**
 * Face data for mesh building
 */
interface FaceData {
  positions: number[];
  indices: number[];
  uvs: number[];
  normals: number[];
}

/**
 * Render context passed to block renderers
 */
export interface RenderContext {
  renderService: RenderService;
  faceData: FaceData;
  vertexOffset: number;
}

/**
 * RenderService - Manages chunk rendering
 *
 * Features:
 * - Renders chunks as optimized meshes
 * - Uses CubeRenderer for cube blocks
 * - Skips INVISIBLE blocks
 * - Listens to ChunkService events
 * - Manages mesh lifecycle
 */
export class RenderService {
  private scene: Scene;
  private appContext: AppContext;
  public materialService: MaterialService;
  private blockTypeService: BlockTypeService;
  private textureAtlas: TextureAtlas;

  // Renderers
  private cubeRenderer: CubeRenderer;

  // Chunk meshes: Map<chunkKey, Map<materialKey, Mesh>>
  // Each chunk can have multiple meshes (one per material type)
  private chunkMeshes: Map<string, Map<string, Mesh>> = new Map();

  constructor(
    scene: Scene,
    appContext: AppContext,
    materialService: MaterialService,
    textureAtlas: TextureAtlas
  ) {
    this.scene = scene;
    this.appContext = appContext;
    this.materialService = materialService;
    this.textureAtlas = textureAtlas;

    const blockTypeService = appContext.services.blockType;
    if (!blockTypeService) {
      throw new Error('BlockTypeService not available');
    }
    this.blockTypeService = blockTypeService;

    // Initialize renderers
    this.cubeRenderer = new CubeRenderer(textureAtlas);

    // Listen to chunk events
    this.setupChunkEventListeners();

    logger.info('RenderService initialized');
  }

  /**
   * Setup chunk event listeners
   */
  private setupChunkEventListeners(): void {
    const chunkService = this.appContext.services.chunk;
    if (!chunkService) {
      logger.warn('ChunkService not available, cannot listen to chunk events');
      return;
    }

    chunkService.on('chunk:loaded', (chunk: any) => {
      this.onChunkLoaded(chunk);
    });

    chunkService.on('chunk:updated', (chunk: any) => {
      this.onChunkUpdated(chunk);
    });

    chunkService.on('chunk:unloaded', (coord: { cx: number; cz: number }) => {
      this.onChunkUnloaded(coord);
    });
  }

  /**
   * Handle single chunk loaded event
   */
  private onChunkLoaded(clientChunk: ClientChunk): void {
    const cx = clientChunk.data.transfer.cx;
    const cz = clientChunk.data.transfer.cz;

    logger.debug('Chunk loaded, rendering', { cx, cz });

    this.renderChunk(clientChunk).catch((error) => {
      ExceptionHandler.handle(error, 'RenderService.onChunkLoaded', {
        cx,
        cz,
      });
    });
  }

  /**
   * Handle chunk updated event (blocks changed via b.u message)
   */
  private onChunkUpdated(clientChunk: ClientChunk): void {
    const cx = clientChunk.data.transfer.cx;
    const cz = clientChunk.data.transfer.cz;

    logger.info('🔵 RenderService: Chunk updated, re-rendering', {
      cx,
      cz,
      blockCount: clientChunk.data.data.size,
    });

    // Remove old mesh first
    this.unloadChunk(cx, cz);

    // Re-render chunk with updated ClientBlocks
    this.renderChunk(clientChunk).catch((error) => {
      ExceptionHandler.handle(error, 'RenderService.onChunkUpdated', {
        cx,
        cz,
      });
    });

    logger.info('🔵 Chunk re-render complete', { cx, cz });
  }

  /**
   * Handle single chunk unloaded event
   */
  private onChunkUnloaded(coord: { cx: number; cz: number }): void {
    logger.debug('Chunk unloaded, cleaning up', { cx: coord.cx, cz: coord.cz });
    this.unloadChunk(coord.cx, coord.cz);
  }

  /**
   * Render a chunk
   *
   * NEW IMPLEMENTATION: Groups blocks by material properties and creates
   * separate meshes for each material group.
   *
   * @param clientChunk Client-side chunk with processed ClientBlocks
   */
  async renderChunk(clientChunk: ClientChunk): Promise<void> {
    const chunk = clientChunk.data.transfer; // Get transfer object for chunk coordinates
    try {
      const chunkKey = this.getChunkKey(chunk.cx, chunk.cz);

      // Check if already rendered
      if (this.chunkMeshes.has(chunkKey)) {
        logger.debug('Chunk already rendered, skipping', { cx: chunk.cx, cz: chunk.cz });
        return;
      }

      const clientBlocksMap = clientChunk.data.data;
      const blockCount = clientBlocksMap.size;

      logger.debug('Rendering chunk from ClientBlocks', {
        cx: chunk.cx,
        cz: chunk.cz,
        blockCount,
      });

      // Group blocks by material key
      const materialGroups = this.groupBlocksByMaterial(clientChunk);

      logger.debug('Material groups created', {
        cx: chunk.cx,
        cz: chunk.cz,
        groupCount: materialGroups.size,
        groups: Array.from(materialGroups.keys()),
      });

      // Create mesh for each material group
      const meshMap = new Map<string, Mesh>();

      for (const [materialKey, blocks] of materialGroups) {
        const faceData: FaceData = {
          positions: [],
          indices: [],
          uvs: [],
          normals: [],
        };

        const renderContext: RenderContext = {
          renderService: this,
          faceData,
          vertexOffset: 0,
        };

        // Render all blocks in this material group
        for (const clientBlock of blocks) {
          const block = clientBlock.block;

          // Validate block data
          if (!block || typeof block.blockTypeId === 'undefined' || !block.position) {
            logger.warn('Invalid block data in ClientBlock', { block });
            continue;
          }

          const modifier = clientBlock.currentModifier;
          if (!modifier || !modifier.visibility) {
            continue;
          }

          const shape = modifier.visibility.shape ?? Shape.CUBE;

          // Skip invisible blocks
          if (shape === Shape.INVISIBLE) {
            continue;
          }

          // Only render cubes for now
          if (shape === Shape.CUBE) {
            await this.cubeRenderer.render(renderContext, clientBlock);
          } else {
            logger.debug('Unsupported shape, skipping', {
              shape,
              blockTypeId: block.blockTypeId,
            });
          }
        }

        // Create mesh if we have any geometry
        if (faceData.positions.length > 0) {
          const meshName = `${chunkKey}_${materialKey}`;
          const mesh = this.createMesh(meshName, faceData);

          // Get and apply material
          const material = await this.materialService.getMaterial(
            blocks[0].currentModifier, // Use first block's modifier to get material
            0 // textureIndex - doesn't matter for property-based keys
          );
          mesh.material = material;

          meshMap.set(materialKey, mesh);

          logger.debug('Material group mesh created', {
            cx: chunk.cx,
            cz: chunk.cz,
            materialKey,
            vertices: faceData.positions.length / 3,
            faces: faceData.indices.length / 3,
            backFaceCulling: material.backFaceCulling,
          });
        }
      }

      // Store meshes for this chunk
      if (meshMap.size > 0) {
        this.chunkMeshes.set(chunkKey, meshMap);

        logger.debug('Chunk rendered', {
          cx: chunk.cx,
          cz: chunk.cz,
          meshCount: meshMap.size,
        });
      } else {
        logger.debug('Chunk has no renderable blocks', { cx: chunk.cx, cz: chunk.cz });
      }
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'RenderService.renderChunk', {
        cx: chunk.cx,
        cz: chunk.cz,
      });
    }
  }

  /**
   * Group blocks by their material key
   * Blocks with the same material properties will be grouped together
   */
  private groupBlocksByMaterial(clientChunk: ClientChunk): Map<string, ClientBlock[]> {
    const groups = new Map<string, ClientBlock[]>();

    for (const clientBlock of clientChunk.data.data.values()) {
      const modifier = clientBlock.currentModifier;
      if (!modifier || !modifier.visibility) {
        continue;
      }

      // Get material key (based on properties, not texture)
      // Use textureIndex 0 as placeholder - actual texture determined by UVs
      const materialKey = this.materialService.getMaterialKey(modifier, 0);

      if (!groups.has(materialKey)) {
        groups.set(materialKey, []);
      }

      groups.get(materialKey)!.push(clientBlock);
    }

    return groups;
  }

  /**
   * Create a mesh from face data
   * Note: Material is NOT set here - caller must assign it
   */
  private createMesh(name: string, faceData: FaceData): Mesh {
    const mesh = new Mesh(name, this.scene);

    // Create vertex data
    const vertexData = new VertexData();
    vertexData.positions = faceData.positions;
    vertexData.indices = faceData.indices;
    vertexData.uvs = faceData.uvs;
    vertexData.normals = faceData.normals;

    // Apply to mesh
    vertexData.applyToMesh(mesh);

    // Material will be set by caller (renderChunk)
    // mesh.material = ... (assigned after creation)

    return mesh;
  }

  /**
   * Unload a chunk and dispose all its meshes
   */
  private unloadChunk(cx: number, cz: number): void {
    const chunkKey = this.getChunkKey(cx, cz);
    const meshMap = this.chunkMeshes.get(chunkKey);

    if (meshMap) {
      // Dispose all meshes for this chunk
      for (const [materialKey, mesh] of meshMap) {
        mesh.dispose();
      }
      this.chunkMeshes.delete(chunkKey);

      logger.debug('Chunk meshes disposed', { cx, cz, count: meshMap.size });
    }
  }

  /**
   * Get chunk key for map storage
   */
  private getChunkKey(cx: number, cz: number): string {
    return `chunk_${cx}_${cz}`;
  }

  /**
   * Get all rendered chunk meshes
   * Returns flattened map of all meshes (chunkKey_materialKey -> Mesh)
   */
  getChunkMeshes(): Map<string, Mesh> {
    const flatMap = new Map<string, Mesh>();
    for (const [chunkKey, meshMap] of this.chunkMeshes) {
      for (const [materialKey, mesh] of meshMap) {
        flatMap.set(`${chunkKey}_${materialKey}`, mesh);
      }
    }
    return flatMap;
  }

  /**
   * Get statistics
   */
  getStats(): { renderedChunks: number; totalVertices: number; totalFaces: number; totalMeshes: number } {
    let totalVertices = 0;
    let totalFaces = 0;
    let totalMeshes = 0;

    for (const meshMap of this.chunkMeshes.values()) {
      for (const mesh of meshMap.values()) {
        totalVertices += mesh.getTotalVertices();
        totalFaces += mesh.getTotalIndices() / 3;
        totalMeshes++;
      }
    }

    return {
      renderedChunks: this.chunkMeshes.size,
      totalMeshes,
      totalVertices,
      totalFaces,
    };
  }

  /**
   * Dispose all chunks and resources
   */
  dispose(): void {
    for (const meshMap of this.chunkMeshes.values()) {
      for (const mesh of meshMap.values()) {
        mesh.dispose();
      }
    }

    this.chunkMeshes.clear();

    logger.info('RenderService disposed');
  }
}
