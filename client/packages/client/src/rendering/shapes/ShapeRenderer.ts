/**
 * Shape Renderer Base Class
 * Abstract base class for rendering different block shapes
 * Each shape type extends this class and implements the render method
 */

import { Matrix, Vector3 } from '@babylonjs/core';
import type { BlockRenderContext, MaterialMeshData } from './BlockRenderContext';
import type { AtlasUV } from '../TextureAtlas';

/**
 * Abstract base class for shape renderers
 * Provides common functionality for rendering block faces with rotation, UVs, and colors
 */
export abstract class ShapeRenderer {
  /**
   * Render a block using the provided context
   * @param context - All data needed to render the block
   * @returns Number of vertices added to the geometry arrays (or Promise for async renderers)
   */
  abstract render(context: BlockRenderContext): number | Promise<number>;

  /**
   * Get or create material mesh data for a specific material
   * Helper method for multi-material blocks
   */
  protected getMaterialData(
    context: BlockRenderContext,
    materialKeyOrBlock?: string | { transparent?: boolean; fluid?: boolean; material?: string }
  ): MaterialMeshData {
    // If no material specified, use the context's default material
    if (!materialKeyOrBlock) {
      return {
        positions: context.positions,
        indices: context.indices,
        normals: context.normals,
        uvs: context.uvs,
        colors: context.colors,
        vertexIndex: context.vertexIndex,
        windLeafiness: context.windLeafiness,
        windStability: context.windStability,
        windLeverUp: context.windLeverUp,
        windLeverDown: context.windLeverDown,
      };
    }

    // If string, use it directly as material key
    if (typeof materialKeyOrBlock === 'string') {
      const data = context.materialMeshes.get(materialKeyOrBlock);
      if (!data) {
        throw new Error(`[ShapeRenderer] Material '${materialKeyOrBlock}' not found in materialMeshes`);
      }
      return data;
    }

    // If block-like object, get material config from MaterialManager
    const materialConfig = context.materialManager.getMaterialConfig(
      materialKeyOrBlock as any,
      context.modifier
    );
    const data = context.materialMeshes.get(materialConfig.key);
    if (!data) {
      throw new Error(`[ShapeRenderer] Material '${materialConfig.key}' not found in materialMeshes`);
    }
    return data;
  }

  /**
   * Add a single face to the geometry arrays
   * Handles rotation, UV mapping, and vertex colors
   */
  protected addFace(
    v1: number[],
    v2: number[],
    v3: number[],
    v4: number[],
    normal: number[],
    atlasUV: AtlasUV,
    rotationMatrix: Matrix | null,
    centerX: number,
    centerY: number,
    centerZ: number,
    blockColor: [number, number, number, number],
    positions: number[],
    indices: number[],
    normals: number[],
    uvs: number[],
    colors: number[],
    vertexIndex: number
  ): void {
    // Apply rotation if matrix exists
    const vertices = [v1, v2, v3, v4];
    const rotatedVertices = vertices.map((v) => {
      if (rotationMatrix) {
        // Translate to origin, rotate, translate back
        const vec = Vector3.FromArray([v[0] - centerX, v[1] - centerY, v[2] - centerZ]);
        const rotated = Vector3.TransformCoordinates(vec, rotationMatrix);
        return [rotated.x + centerX, rotated.y + centerY, rotated.z + centerZ];
      }
      return v;
    });

    // Positions
    positions.push(
      ...rotatedVertices[0],
      ...rotatedVertices[1],
      ...rotatedVertices[2],
      ...rotatedVertices[3]
    );

    // Rotate normals if needed
    let finalNormal = normal;
    if (rotationMatrix) {
      const normalVec = Vector3.FromArray(normal);
      const rotatedNormal = Vector3.TransformNormal(normalVec, rotationMatrix);
      finalNormal = [rotatedNormal.x, rotatedNormal.y, rotatedNormal.z];
    }

    // Normals (same for all 4 vertices)
    for (let i = 0; i < 4; i++) {
      normals.push(...finalNormal);
    }

    // UVs (texture coordinates from atlas)
    uvs.push(
      atlasUV.u0,
      atlasUV.v0, // Bottom-left
      atlasUV.u1,
      atlasUV.v0, // Bottom-right
      atlasUV.u1,
      atlasUV.v1, // Top-right
      atlasUV.u0,
      atlasUV.v1 // Top-left
    );

    // Colors (RGBA for all 4 vertices)
    for (let i = 0; i < 4; i++) {
      colors.push(...blockColor); // R, G, B, A
    }

    // Indices (2 triangles)
    indices.push(
      vertexIndex,
      vertexIndex + 1,
      vertexIndex + 2,
      vertexIndex,
      vertexIndex + 2,
      vertexIndex + 3
    );
  }

  /**
   * Add wind properties for a number of vertices
   * Called by shape renderers to populate wind arrays when rendering transparent blocks
   */
  protected addWindProperties(
    context: BlockRenderContext,
    vertexCount: number
  ): void {
    // Only add wind properties if arrays are provided (transparent blocks)
    if (context.windLeafiness && context.windStability && context.windLeverUp && context.windLeverDown) {
      // Get wind values from BOTH block and modifier (modifier takes precedence)
      // Default values optimized for leaves/foliage:
      // - windLeafiness: 0.5 (moderate organic movement)
      // - windStability: 0.3 (slightly unstable, allows good movement)
      // - windLeverUp: 1.0 if wind is enabled (full movement amplitude for upper vertices)
      // - windLeverDown: 0.0 (no movement for lower vertices by default)
      const windLeafiness = context.modifier?.windLeafiness ?? context.block.windLeafiness ?? 0.5;
      const windStability = context.modifier?.windStability ?? context.block.windStability ?? 0.3;
      const windLeverUp = context.modifier?.windLeverUp ?? context.block.windLeverUp ?? 1.0;
      const windLeverDown = context.modifier?.windLeverDown ?? context.block.windLeverDown ?? 0.0;

      // Add same wind values for all vertices of this block
      for (let i = 0; i < vertexCount; i++) {
        context.windLeafiness.push(windLeafiness);
        context.windStability.push(windStability);
        context.windLeverUp.push(windLeverUp);
        context.windLeverDown.push(windLeverDown);
      }
    }
  }
}
