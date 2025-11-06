/**
 * FlipboxRenderer - Renders animated sprite-sheet blocks
 *
 * Renders only the TOP face of a block with FLIPBOX shader for sprite-sheet animation.
 * Each block gets its own separate mesh with original texture (not atlas).
 *
 * Shader Parameters Format: "frameCount,delayMs[,mode]"
 * - frameCount: Number of frames in horizontal sprite-sheet (e.g., 4)
 * - delayMs: Milliseconds between frame transitions (e.g., 100)
 * - mode: "rotate" (default) or "bumerang" (optional)
 *   - rotate: 0,1,2,3,0,1,2,3...
 *   - bumerang: 0,1,2,3,2,1,0,1,2,3,2,1...
 *
 * Example: "4,100" or "4,100,bumerang"
 */

import { Vector3, Matrix, Mesh, VertexData } from '@babylonjs/core';
import { getLogger, BlockShader } from '@nimbus/shared';
import type { ClientBlock } from '../types';
import { BlockRenderer } from './BlockRenderer';
import type { RenderContext } from '../services/RenderService';

const logger = getLogger('FlipboxRenderer');

/**
 * FlipboxRenderer - Renders animated sprite-sheet blocks
 *
 * Features:
 * - Renders only TOP face (horizontal surface)
 * - Uses FLIPBOX shader from ShaderService
 * - Each block gets separate mesh with original texture
 * - Supports offset, scaling, rotation transformations
 */
export class FlipboxRenderer extends BlockRenderer {
  /**
   * FlipboxRenderer needs separate mesh per block
   * (cannot use atlas, needs original texture for sprite-sheet)
   */
  needsSeparateMesh(): boolean {
    return true;
  }

  /**
   * Render a FLIPBOX block
   *
   * Creates geometry for TOP face only with transformations applied.
   * Material is created by MaterialService with FLIPBOX shader.
   *
   * @param renderContext Render context (not used - separate mesh)
   * @param clientBlock Block to render
   */
  async render(renderContext: RenderContext, clientBlock: ClientBlock): Promise<void> {
    const block = clientBlock.block;
    const modifier = clientBlock.currentModifier;

    if (!modifier || !modifier.visibility) {
      logger.warn('FlipboxRenderer: No visibility modifier', { block });
      return;
    }

    // Validate shader is FLIPBOX
    const shader = modifier.visibility.shader;
    if (shader !== BlockShader.FLIPBOX) {
      logger.warn('FlipboxRenderer: Not a FLIPBOX shader', { shader, block });
      return;
    }

    // Get texture for TOP face (TextureKey.TOP = 1)
    const topTexture = modifier.visibility.textures?.[1]; // TOP face
    if (!topTexture) {
      logger.warn('FlipboxRenderer: No TOP texture defined', { block });
      return;
    }

    // Get transformations
    const scalingX = modifier.visibility.scalingX ?? 1.0;
    const scalingY = modifier.visibility.scalingY ?? 1.0;
    const scalingZ = modifier.visibility.scalingZ ?? 1.0;
    const rotationX = modifier.visibility.rotationX ?? 0;
    const rotationY = modifier.visibility.rotationY ?? 0;

    // Block position
    const pos = block.position;

    // Create TOP face geometry
    const faceData = this.createTopFaceGeometry(
      new Vector3(pos.x, pos.y, pos.z),
      scalingX,
      scalingY,
      scalingZ,
      rotationX,
      rotationY,
      modifier.visibility.offsets
    );

    // Create separate mesh for this block
    await this.createSeparateMesh(
      clientBlock,
      faceData,
      renderContext.renderService
    );
  }

  /**
   * Create geometry for TOP face
   *
   * Creates a horizontal quad at the top of the block position.
   * Applies scaling, rotation, and corner offsets.
   *
   * @param position Block world position
   * @param scalingX X-axis scaling
   * @param scalingY Y-axis scaling (affects height)
   * @param scalingZ Z-axis scaling
   * @param rotationX X-axis rotation (degrees)
   * @param rotationY Y-axis rotation (degrees)
   * @param offsets Corner offsets (24 values: 8 corners × 3 axes)
   * @returns Face geometry data
   */
  private createTopFaceGeometry(
    position: Vector3,
    scalingX: number,
    scalingY: number,
    scalingZ: number,
    rotationX: number,
    rotationY: number,
    offsets?: number[]
  ): { positions: number[]; indices: number[]; uvs: number[]; normals: number[] } {
    // TOP face vertices (Y = +0.5, at top of block)
    // Counter-clockwise from top-left
    const baseVertices = [
      [-0.5, +0.5, -0.5], // 0: Top-Left
      [+0.5, +0.5, -0.5], // 1: Top-Right
      [+0.5, +0.5, +0.5], // 2: Bottom-Right
      [-0.5, +0.5, +0.5], // 3: Bottom-Left
    ];

    // Apply corner offsets if provided
    if (offsets && offsets.length >= 24) {
      // TOP face uses corners: 4, 5, 6, 7 (top 4 corners of cube)
      const topCornerIndices = [4, 5, 6, 7];
      for (let i = 0; i < 4; i++) {
        const cornerIndex = topCornerIndices[i];
        const offsetBase = cornerIndex * 3;
        baseVertices[i][0] += offsets[offsetBase] || 0;
        baseVertices[i][1] += offsets[offsetBase + 1] || 0;
        baseVertices[i][2] += offsets[offsetBase + 2] || 0;
      }
    }

    // Apply scaling
    const scaledVertices = baseVertices.map(([x, y, z]) => [
      x * scalingX,
      y * scalingY,
      z * scalingZ,
    ]);

    // Apply rotation
    const rotationMatrix = Matrix.RotationYawPitchRoll(
      (rotationY * Math.PI) / 180, // Yaw (Y-axis)
      (rotationX * Math.PI) / 180, // Pitch (X-axis)
      0 // Roll (Z-axis, not used in visibility)
    );

    const transformedVertices = scaledVertices.map(([x, y, z]) => {
      const vec = Vector3.TransformCoordinates(new Vector3(x, y, z), rotationMatrix);
      return [vec.x, vec.y, vec.z];
    });

    // Translate to world position
    const positions: number[] = [];
    for (const [x, y, z] of transformedVertices) {
      positions.push(position.x + x, position.y + y, position.z + z);
    }

    // Indices for two triangles (quad)
    const indices = [
      0, 1, 2, // First triangle
      0, 2, 3, // Second triangle
    ];

    // UV coordinates (full texture, will be animated by shader)
    const uvs = [
      0, 0, // Top-Left
      1, 0, // Top-Right
      1, 1, // Bottom-Right
      0, 1, // Bottom-Left
    ];

    // Normals (pointing up for TOP face)
    const normals = [
      0, 1, 0, // Vertex 0
      0, 1, 0, // Vertex 1
      0, 1, 0, // Vertex 2
      0, 1, 0, // Vertex 3
    ];

    return { positions, indices, uvs, normals };
  }

  /**
   * Create separate mesh for this FLIPBOX block
   *
   * Creates a new mesh with FLIPBOX shader material from MaterialService.
   *
   * @param clientBlock Block to create mesh for
   * @param faceData Geometry data
   * @param renderService RenderService instance for material access
   */
  private async createSeparateMesh(
    clientBlock: ClientBlock,
    faceData: { positions: number[]; indices: number[]; uvs: number[]; normals: number[] },
    renderService: any // RenderService type
  ): Promise<void> {
    const block = clientBlock.block;
    const scene = renderService.materialService.scene;

    // Create mesh name
    const meshName = `flipbox_${block.position.x}_${block.position.y}_${block.position.z}`;

    // Create mesh
    const mesh = new Mesh(meshName, scene);

    // Create vertex data
    const vertexData = new VertexData();
    vertexData.positions = faceData.positions;
    vertexData.indices = faceData.indices;
    vertexData.uvs = faceData.uvs;
    vertexData.normals = faceData.normals;

    // Apply to mesh
    vertexData.applyToMesh(mesh);

    // Get FLIPBOX shader material from MaterialService
    // MaterialService will detect shader field and create FLIPBOX material with original texture
    const material = await renderService.materialService.getMaterial(
      clientBlock.currentModifier,
      1 // TextureKey.TOP
    );

    mesh.material = material;

    // Store mesh in RenderService separate mesh tracking
    // TODO: RenderService needs to track separate meshes
    // For now, just create the mesh - it will be in the scene

    logger.debug('FLIPBOX separate mesh created', {
      meshName,
      position: block.position,
      vertices: faceData.positions.length / 3,
      material: material.name,
    });
  }
}
