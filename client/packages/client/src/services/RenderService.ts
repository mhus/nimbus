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

  // Chunk meshes: Map<chunkKey, Mesh>
  private chunkMeshes: Map<string, Mesh> = new Map();

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

      // Build mesh data
      const faceData: FaceData = {
        positions: [],
        indices: [],
        uvs: [],
        normals: [],
      };

      let vertexOffset = 0;

      // Check if block types are loaded
      if (!this.blockTypeService.isLoaded()) {
        logger.error('BlockTypes not loaded yet, cannot render chunk', { cx: chunk.cx, cz: chunk.cz });
        return;
      }

      // Render each ClientBlock from the Map
      for (const clientBlock of clientBlocksMap.values()) {
        const block = clientBlock.block;

        // Validate block data
        if (!block || typeof block.blockTypeId === 'undefined' || !block.position) {
          logger.warn('Invalid block data in ClientBlock', { block });
          continue;
        }

        // Use cached references from ClientBlock
        const blockType = clientBlock.blockType;
        const modifier = clientBlock.currentModifier; // ← Already merged!

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
          vertexOffset = await this.cubeRenderer.render(
            this,
            clientBlock,
            block.position.x,
            block.position.y,
            block.position.z,
            faceData,
            vertexOffset
          );
        } else {
          logger.debug('Unsupported shape, skipping', {
            shape,
            blockTypeId: block.blockTypeId,
          });
        }
      }

      // Create mesh if we have any geometry
      if (faceData.positions.length > 0) {
        const mesh = this.createMesh(chunkKey, faceData);
        this.chunkMeshes.set(chunkKey, mesh);

        logger.debug('Chunk rendered', {
          cx: chunk.cx,
          cz: chunk.cz,
          vertices: faceData.positions.length / 3,
          faces: faceData.indices.length / 3,
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
   * Create a mesh from face data
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

    // Set material
    mesh.material = this.materialService.getAtlasMaterial();

    return mesh;
  }

  /**
   * Unload a chunk and dispose its mesh
   */
  private unloadChunk(cx: number, cz: number): void {
    const chunkKey = this.getChunkKey(cx, cz);
    const mesh = this.chunkMeshes.get(chunkKey);

    if (mesh) {
      mesh.dispose();
      this.chunkMeshes.delete(chunkKey);

      logger.debug('Chunk mesh disposed', { cx, cz });
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
   */
  getChunkMeshes(): Map<string, Mesh> {
    return new Map(this.chunkMeshes);
  }

  /**
   * Get statistics
   */
  getStats(): { renderedChunks: number; totalVertices: number; totalFaces: number } {
    let totalVertices = 0;
    let totalFaces = 0;

    for (const mesh of this.chunkMeshes.values()) {
      totalVertices += mesh.getTotalVertices();
      totalFaces += mesh.getTotalIndices() / 3;
    }

    return {
      renderedChunks: this.chunkMeshes.size,
      totalVertices,
      totalFaces,
    };
  }

  /**
   * Dispose all chunks and resources
   */
  dispose(): void {
    for (const mesh of this.chunkMeshes.values()) {
      mesh.dispose();
    }

    this.chunkMeshes.clear();

    logger.info('RenderService disposed');
  }
}
