/**
 * RenderService - Renders chunks as meshes
 *
 * Manages chunk rendering, mesh generation, and cleanup.
 * Listens to ChunkService events to render/unload chunks.
 */

import { Mesh, VertexData, Scene } from '@babylonjs/core';
import { getLogger, ExceptionHandler, Shape, BlockShader } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { ClientChunk } from '../types/ClientChunk';
import type { ClientBlock } from '../types/ClientBlock';
import type { MaterialService } from './MaterialService';
import type { BlockTypeService } from './BlockTypeService';
import { BlockRenderer } from '../rendering/BlockRenderer';
import { CubeRenderer } from '../rendering/CubeRenderer';
import { FlipboxRenderer } from '../rendering/FlipboxRenderer';
import { BillboardRenderer } from '../rendering/BillboardRenderer';
import { SpriteRenderer } from '../rendering/SpriteRenderer';
import { ThinInstancesRenderer } from '../rendering/ThinInstancesRenderer';
import { FlameRenderer } from '../rendering/FlameRenderer';
import { OceanRenderer } from '../rendering/OceanRenderer';
import { DisposableResources } from '../rendering/DisposableResources';
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
  resourcesToDispose: DisposableResources;
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
  public appContext: AppContext; // Public for renderer access to services
  public materialService: MaterialService;
  private blockTypeService: BlockTypeService;
  private textureAtlas: TextureAtlas;

  // Renderers
  private cubeRenderer: CubeRenderer;
  private flipboxRenderer: FlipboxRenderer;
  private billboardRenderer: BillboardRenderer;
  private spriteRenderer: SpriteRenderer;
  private thinInstancesRenderer: ThinInstancesRenderer;
  private flameRenderer: FlameRenderer;
  private oceanRenderer: OceanRenderer;

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
    this.flipboxRenderer = new FlipboxRenderer();
    this.billboardRenderer = new BillboardRenderer();
    this.spriteRenderer = new SpriteRenderer();
    this.thinInstancesRenderer = new ThinInstancesRenderer();
    this.flameRenderer = new FlameRenderer();
    this.oceanRenderer = new OceanRenderer();

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

      // Dispose old resources if they exist (e.g., during chunk update)
      if (clientChunk.data.resourcesToDispose) {
        const oldStats = clientChunk.data.resourcesToDispose.getStats();
        clientChunk.data.resourcesToDispose.dispose();
        logger.debug('Disposed old chunk resources before re-render', {
          cx: chunk.cx,
          cz: chunk.cz,
          oldResources: oldStats.total,
        });
      }

      // Create new DisposableResources for this chunk
      const resourcesToDispose = new DisposableResources();
      clientChunk.data.resourcesToDispose = resourcesToDispose;

      // Separate blocks into chunk mesh blocks vs separate mesh blocks
      const { chunkMeshBlocks, separateMeshBlocks } = this.separateBlocksByRenderType(clientChunk);

      logger.debug('Blocks separated by render type', {
        cx: chunk.cx,
        cz: chunk.cz,
        chunkMeshBlocks: chunkMeshBlocks.length,
        separateMeshBlocks: separateMeshBlocks.length,
      });

      // 1. Render chunk mesh blocks (batched by material)
      const materialGroups = this.groupBlocksByMaterial(clientChunk, chunkMeshBlocks);

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
          resourcesToDispose,
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

      // Store chunk meshes
      if (meshMap.size > 0) {
        this.chunkMeshes.set(chunkKey, meshMap);

        logger.debug('Chunk meshes rendered', {
          cx: chunk.cx,
          cz: chunk.cz,
          meshCount: meshMap.size,
        });
      } else {
        logger.debug('Chunk has no chunk mesh blocks', { cx: chunk.cx, cz: chunk.cz });
      }

      // 2. Render separate mesh blocks (individual meshes)
      for (const clientBlock of separateMeshBlocks) {
        await this.renderSeparateMeshBlock(clientBlock, chunkKey, resourcesToDispose);
      }

      logger.debug('Chunk fully rendered', {
        cx: chunk.cx,
        cz: chunk.cz,
        chunkMeshes: meshMap.size,
        separateMeshes: separateMeshBlocks.length,
      });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'RenderService.renderChunk', {
        cx: chunk.cx,
        cz: chunk.cz,
      });
    }
  }

  /**
   * Get appropriate renderer for a block
   *
   * Routes blocks to the correct renderer based on shader or shape.
   *
   * @param clientBlock Block to get renderer for
   * @returns BlockRenderer instance or null if not supported
   */
  private getRenderer(clientBlock: ClientBlock): BlockRenderer | null {
    const modifier = clientBlock.currentModifier;
    if (!modifier || !modifier.visibility) {
      return null;
    }

    const shape = modifier.visibility.shape ?? Shape.CUBE;
    const shader = this.getBlockShader(modifier);

    // Check shader first
    if (shader === BlockShader.FLIPBOX) {
      return this.flipboxRenderer;
    }

    // Then check shape
    switch (shape) {
      case Shape.CUBE:
        return this.cubeRenderer;
      case Shape.BILLBOARD:
        return this.billboardRenderer;
      case Shape.SPRITE:
        return this.spriteRenderer;
      case Shape.THIN_INSTANCES:
        return this.thinInstancesRenderer;
      case Shape.FLAME:
        return this.flameRenderer;
      case Shape.OCEAN:
        return this.oceanRenderer;
      default:
        return null;
    }
  }

  /**
   * Separate blocks into chunk mesh blocks vs separate mesh blocks
   *
   * Uses BlockRenderer.needsSeparateMesh() to determine separation.
   * This is the clean Strategy Pattern approach - each renderer knows its requirements.
   *
   * @param clientChunk Chunk with all blocks
   * @returns Separated block lists
   */
  private separateBlocksByRenderType(clientChunk: ClientChunk): {
    chunkMeshBlocks: ClientBlock[];
    separateMeshBlocks: ClientBlock[];
  } {
    const chunkMeshBlocks: ClientBlock[] = [];
    const separateMeshBlocks: ClientBlock[] = [];

    for (const clientBlock of clientChunk.data.data.values()) {
      const renderer = this.getRenderer(clientBlock);

      if (!renderer) {
        continue; // Skip blocks without renderer
      }

      // Use renderer's needsSeparateMesh() to determine grouping
      if (renderer.needsSeparateMesh()) {
        separateMeshBlocks.push(clientBlock);
      } else {
        chunkMeshBlocks.push(clientBlock);
      }
    }

    return { chunkMeshBlocks, separateMeshBlocks };
  }

  /**
   * Get shader from modifier
   * Checks texture-level shader first, then visibility-level shader
   */
  private getBlockShader(modifier: any): BlockShader {
    // Check texture-level shader (highest priority)
    if (modifier.visibility?.textures) {
      for (const texture of Object.values(modifier.visibility.textures)) {
        if (typeof texture === 'object' && texture !== null && 'shader' in texture) {
          return (texture as any).shader ?? BlockShader.NONE;
        }
      }
    }

    // Check visibility-level shader
    return modifier.visibility?.shader ?? BlockShader.NONE;
  }

  /**
   * Group blocks by their material key
   * Blocks with the same material properties will be grouped together
   *
   * @param clientChunk Chunk with all blocks
   * @param blocksToGroup Specific blocks to group (allows filtering)
   * @returns Grouped blocks by material key
   */
  private groupBlocksByMaterial(
    clientChunk: ClientChunk,
    blocksToGroup?: ClientBlock[]
  ): Map<string, ClientBlock[]> {
    const groups = new Map<string, ClientBlock[]>();
    const blocks = blocksToGroup || Array.from(clientChunk.data.data.values());

    for (const clientBlock of blocks) {
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
   * Render a single block with separate mesh
   *
   * Used for blocks that need individual meshes (FLIPBOX, BILLBOARD, etc.)
   *
   * @param clientBlock Block to render
   * @param chunkKey Parent chunk key for tracking
   */
  private async renderSeparateMeshBlock(
    clientBlock: ClientBlock,
    chunkKey: string,
    resourcesToDispose: DisposableResources
  ): Promise<void> {
    const block = clientBlock.block;
    const modifier = clientBlock.currentModifier;

    if (!modifier || !modifier.visibility) {
      return;
    }

    try {
      // Get appropriate renderer using getRenderer()
      const renderer = this.getRenderer(clientBlock);
      if (!renderer) {
        logger.warn('No renderer found for separate mesh block', { position: block.position });
        return;
      }

      const renderContext: RenderContext = {
        renderService: this,
        faceData: { positions: [], indices: [], uvs: [], normals: [] }, // Not used by separate renderers
        vertexOffset: 0,
        resourcesToDispose,
      };

      // Render using appropriate renderer
      // Renderer will add created meshes/sprites to resourcesToDispose
      await renderer.render(renderContext, clientBlock);

      logger.debug('Separate mesh rendered', {
        position: `${block.position.x},${block.position.y},${block.position.z}`,
        renderer: renderer.constructor.name,
      });
    } catch (error) {
      ExceptionHandler.handle(error, 'RenderService.renderSeparateMeshBlock', {
        position: `${block.position.x},${block.position.y},${block.position.z}`,
      });
    }
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
   * Unload a chunk and dispose all its resources
   *
   * Disposes:
   * - Chunk meshes (batched material groups)
   * - Separate meshes/sprites via DisposableResources (including thin instances)
   */
  private unloadChunk(cx: number, cz: number): void {
    const chunkKey = this.getChunkKey(cx, cz);

    // Dispose chunk meshes (batched material groups)
    const meshMap = this.chunkMeshes.get(chunkKey);
    let chunkMeshCount = 0;

    if (meshMap) {
      for (const [materialKey, mesh] of meshMap) {
        mesh.dispose();
        chunkMeshCount++;
      }
      this.chunkMeshes.delete(chunkKey);
    }

    // Dispose separate meshes/sprites via DisposableResources
    // (includes thin instances via ThinInstanceDisposable)
    const chunkService = this.appContext.services.chunk;
    if (chunkService) {
      const clientChunk = chunkService.getChunk(cx, cz);
      if (clientChunk?.data.resourcesToDispose) {
        const stats = clientChunk.data.resourcesToDispose.getStats();
        clientChunk.data.resourcesToDispose.dispose();

        logger.debug('Chunk resources disposed', {
          cx,
          cz,
          chunkMeshes: chunkMeshCount,
          totalResources: stats.total,
          namedResources: stats.named,
        });
        return;
      }
    }

    logger.debug('Chunk unloaded', { cx, cz, chunkMeshes: chunkMeshCount });
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
   *
   * Note: Separate meshes/sprites are disposed via ClientChunk.resourcesToDispose
   * when chunks are unloaded. This method only disposes chunk meshes.
   */
  dispose(): void {
    // Dispose chunk meshes
    for (const meshMap of this.chunkMeshes.values()) {
      for (const mesh of meshMap.values()) {
        mesh.dispose();
      }
    }
    this.chunkMeshes.clear();

    logger.info('RenderService disposed');
  }
}
