/**
 * ClientChunk - Client-side chunk representation
 *
 * Contains the chunk data with resolved references, caches, and rendering state.
 * This type is NOT transmitted over network - it's only used client-side.
 *
 * Analog to ClientBlock for chunks.
 */

import type { ChunkData } from './ChunkData';
import type { ClientBlock } from './ClientBlock';

/**
 * Chunk rendering state
 */
export enum ChunkRenderState {
  /** Not yet rendered */
  NOT_RENDERED = 0,

  /** Mesh generation in progress */
  GENERATING_MESH = 1,

  /** Mesh ready, not yet added to scene */
  MESH_READY = 2,

  /** Rendered and visible in scene */
  RENDERED = 3,

  /** Needs re-rendering (data changed) */
  DIRTY = 4,

  /** Being unloaded */
  UNLOADING = 5,
}

/**
 * Client-side chunk instance with caches and rendering state
 */
export interface ClientChunk {
  /** Original chunk data */
  chunkData: ChunkData;

  /** Chunk coordinates (same as chunkData.cx, cz for convenience) */
  chunk: { cx: number; cz: number };

  // Rendering state

  /**
   * Current render state
   */
  renderState: ChunkRenderState;

  /**
   * Babylon.js mesh reference (if rendered)
   */
  mesh?: any; // BABYLON.Mesh

  /**
   * Babylon.js material reference (if rendered)
   */
  material?: any; // BABYLON.Material

  /**
   * Mesh generation timestamp
   */
  meshGeneratedAt?: number;

  /**
   * Last render timestamp
   */
  lastRendered?: number;

  /**
   * Is chunk currently visible (frustum culling, distance)
   */
  isVisible: boolean;

  /**
   * Distance from camera (for LOD and priority)
   */
  distanceFromCamera?: number;

  /**
   * Level of detail (0 = highest, 3 = lowest)
   */
  lod: number;

  // Caches

  /**
   * Cached ClientBlock instances for non-air blocks in this chunk
   * Map: blockIndex → ClientBlock
   */
  blocks?: Map<number, ClientBlock>;

  /**
   * Cached neighbor chunks (for face culling)
   */
  neighbors?: {
    north?: ClientChunk;
    south?: ClientChunk;
    east?: ClientChunk;
    west?: ClientChunk;
  };

  /**
   * Has water blocks (for water shader optimization)
   */
  hasWater?: boolean;

  /**
   * Has transparent blocks (for rendering order)
   */
  hasTransparency?: boolean;

  /**
   * Bounding box (for culling)
   */
  boundingBox?: {
    min: { x: number; y: number; z: number };
    max: { x: number; y: number; z: number };
  };

  // Metadata

  /**
   * Chunk priority for loading/rendering
   * Higher = more important
   */
  priority: number;

  /**
   * Last update timestamp (for change detection)
   */
  lastUpdate: number;

  /**
   * Dirty flag (needs mesh regeneration)
   */
  isDirty: boolean;

  /**
   * Vertex count (for performance monitoring)
   */
  vertexCount?: number;

  /**
   * Triangle count (for performance monitoring)
   */
  triangleCount?: number;
}

/**
 * Helper functions for ClientChunk
 */
export namespace ClientChunkHelper {
  /**
   * Create ClientChunk from ChunkData
   * @param chunkData Internal chunk data
   * @returns New ClientChunk
   */
  export function create(chunkData: ChunkData): ClientChunk {
    return {
      chunkData,
      chunk: { cx: chunkData.cx, cz: chunkData.cz },
      renderState: ChunkRenderState.NOT_RENDERED,
      isVisible: false,
      lod: 0,
      priority: 0,
      lastUpdate: Date.now(),
      isDirty: true,
    };
  }

  /**
   * Mark chunk as dirty (needs re-render)
   * @param chunk Client chunk
   */
  export function markDirty(chunk: ClientChunk): void {
    chunk.isDirty = true;
    chunk.renderState = ChunkRenderState.DIRTY;
    chunk.lastUpdate = Date.now();
  }

  /**
   * Check if chunk needs rendering
   * @param chunk Client chunk
   * @returns True if needs rendering
   */
  export function needsRendering(chunk: ClientChunk): boolean {
    return (
      chunk.isDirty ||
      chunk.renderState === ChunkRenderState.NOT_RENDERED ||
      chunk.renderState === ChunkRenderState.DIRTY
    );
  }

  /**
   * Calculate chunk priority based on distance and state
   * @param chunk Client chunk
   * @param cameraX Camera world X position
   * @param cameraZ Camera world Z position
   * @param chunkSize Chunk size
   * @returns Priority value (higher = more important)
   */
  export function calculatePriority(
    chunk: ClientChunk,
    cameraX: number,
    cameraZ: number,
    chunkSize: number
  ): number {
    // Calculate distance from camera to chunk center
    const chunkCenterX = chunk.chunk.cx * chunkSize + chunkSize / 2;
    const chunkCenterZ = chunk.chunk.cz * chunkSize + chunkSize / 2;

    const dx = chunkCenterX - cameraX;
    const dz = chunkCenterZ - cameraZ;
    const distance = Math.sqrt(dx * dx + dz * dz);

    // Store distance
    chunk.distanceFromCamera = distance;

    // Higher priority for closer chunks
    let priority = 1000 - distance;

    // Boost priority if dirty
    if (chunk.isDirty) {
      priority += 500;
    }

    // Boost priority if not yet rendered
    if (chunk.renderState === ChunkRenderState.NOT_RENDERED) {
      priority += 1000;
    }

    chunk.priority = priority;
    return priority;
  }

  /**
   * Calculate appropriate LOD level
   * @param chunk Client chunk
   * @param maxDistance Maximum render distance
   * @returns LOD level (0-3)
   */
  export function calculateLOD(
    chunk: ClientChunk,
    maxDistance: number
  ): number {
    if (!chunk.distanceFromCamera) {
      return 0;
    }

    const distance = chunk.distanceFromCamera;
    const normalizedDistance = distance / maxDistance;

    if (normalizedDistance < 0.25) {
      return 0; // High detail
    } else if (normalizedDistance < 0.5) {
      return 1; // Medium detail
    } else if (normalizedDistance < 0.75) {
      return 2; // Low detail
    } else {
      return 3; // Very low detail
    }
  }

  /**
   * Update neighbors references
   * @param chunk Client chunk
   * @param chunks Map of all client chunks
   * @param chunkSize Chunk size
   */
  export function updateNeighbors(
    chunk: ClientChunk,
    chunks: Map<string, ClientChunk>
  ): void {
    const { cx, cz } = chunk.chunk;

    chunk.neighbors = {
      north: chunks.get(`${cx},${cz - 1}`),
      south: chunks.get(`${cx},${cz + 1}`),
      east: chunks.get(`${cx + 1},${cz}`),
      west: chunks.get(`${cx - 1},${cz}`),
    };
  }

  /**
   * Dispose chunk resources (meshes, materials, etc.)
   * @param chunk Client chunk
   */
  export function dispose(chunk: ClientChunk): void {
    if (chunk.mesh) {
      chunk.mesh.dispose();
      chunk.mesh = undefined;
    }

    if (chunk.material) {
      chunk.material.dispose();
      chunk.material = undefined;
    }

    chunk.blocks?.clear();
    chunk.neighbors = undefined;
    chunk.renderState = ChunkRenderState.UNLOADING;
  }

  /**
   * Get chunk key for mapping
   * @param chunk Client chunk
   * @returns String key "cx,cz"
   */
  export function getKey(chunk: ClientChunk): string {
    return `${chunk.chunk.cx},${chunk.chunk.cz}`;
  }

  /**
   * Check if chunk is empty (no non-air blocks)
   * @param chunk Client chunk
   * @returns True if empty
   */
  export function isEmpty(chunk: ClientChunk): boolean {
    return (
      !chunk.blocks || chunk.blocks.size === 0 || chunk.vertexCount === 0
    );
  }

  /**
   * Clone chunk (shallow copy, doesn't clone mesh/material)
   * @param chunk Client chunk
   * @returns Cloned chunk
   */
  export function clone(chunk: ClientChunk): ClientChunk {
    return {
      ...chunk,
      chunkData: { ...chunk.chunkData },
      chunk: { ...chunk.chunk },
      neighbors: chunk.neighbors ? { ...chunk.neighbors } : undefined,
      blocks: chunk.blocks ? new Map(chunk.blocks) : undefined,
      // Don't clone mesh/material references
    };
  }

  /**
   * Get render state name for debugging
   * @param state Render state
   * @returns State name
   */
  export function getRenderStateName(state: ChunkRenderState): string {
    const names: Record<ChunkRenderState, string> = {
      [ChunkRenderState.NOT_RENDERED]: 'not_rendered',
      [ChunkRenderState.GENERATING_MESH]: 'generating_mesh',
      [ChunkRenderState.MESH_READY]: 'mesh_ready',
      [ChunkRenderState.RENDERED]: 'rendered',
      [ChunkRenderState.DIRTY]: 'dirty',
      [ChunkRenderState.UNLOADING]: 'unloading',
    };
    return names[state] ?? 'unknown';
  }

  /**
   * Convert to string for debugging
   * @param chunk Client chunk
   * @returns Debug string
   */
  export function toString(chunk: ClientChunk): string {
    const state = getRenderStateName(chunk.renderState);
    const blocks = chunk.blocks?.size ?? 0;
    const vertices = chunk.vertexCount ?? 0;

    return `ClientChunk(${chunk.chunk.cx},${chunk.chunk.cz}, ${state}, ${blocks} blocks, ${vertices} vertices)`;
  }
}
