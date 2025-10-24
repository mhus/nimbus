/**
 * Block Render Context
 * Transfer object containing all data needed to render a block
 * This makes method signatures cleaner and easier to extend
 */

import type { Matrix, Mesh, Scene, Sprite } from '@babylonjs/core';
import type { BlockType, BlockModifier } from '@nimbus-client/core';
import type { BlockFaceUVs } from '../TextureAtlas';
import type { MaterialManager } from '../MaterialManager';
import type { SpriteManagerRegistry } from '../SpriteManagerRegistry';

/**
 * Material mesh data container
 * Holds geometry data for all blocks of a specific material type
 */
export interface MaterialMeshData {
  positions: number[];
  indices: number[];
  normals: number[];
  uvs: number[];
  colors: number[];
  vertexIndex: number;
  // Custom attributes for wind shader
  windLeafiness?: number[];
  windStability?: number[];
  windLeverUp?: number[];
  windLeverDown?: number[];
}

export interface BlockRenderContext {
  // World position
  x: number;
  y: number;
  z: number;

  // Block data
  block: BlockType;
  modifier?: BlockModifier;
  blockUVs: BlockFaceUVs;
  blockColor: [number, number, number, number];

  // Transform data
  rotationMatrix: Matrix | null;
  edgeOffsets: number[] | null;

  // Default material arrays (for backwards compatibility and simple blocks)
  positions: number[];
  indices: number[];
  normals: number[];
  uvs: number[];
  colors: number[];
  vertexIndex: number;

  // Wind properties (optional - only for blocks with wind effects)
  windLeafiness?: number[];
  windStability?: number[];
  windLeverUp?: number[];
  windLeverDown?: number[];

  // Multi-material support
  // Shape-Renderers can access all material containers for complex blocks
  materialMeshes: Map<string, MaterialMeshData>;
  materialManager: MaterialManager;

  // Scene and separate meshes support
  // For blocks that need to be rendered as separate meshes (billboards, sprites)
  scene: Scene;
  separateMeshes: Mesh[];

  // Sprite manager registry for SPRITE blocks
  spriteManagerRegistry: SpriteManagerRegistry;

  // Array to collect sprites created during rendering
  // These will be stored in the chunk mesh metadata for proper disposal
  sprites: Sprite[];
}
