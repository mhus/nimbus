/**
 * Billboard Renderer
 * Renders BILLBOARD shaped blocks as separate camera-facing meshes
 * Uses Babylon.js billboardMode for automatic rotation towards camera (Y-axis only)
 */

import { Mesh, VertexData } from '@babylonjs/core';
import { ShapeRenderer } from './ShapeRenderer';
import type { BlockRenderContext } from './BlockRenderContext';

export class BillboardRenderer extends ShapeRenderer {
  render(context: BlockRenderContext): number {
    const {
      x,
      y,
      z,
      block,
      blockUVs,
      modifier,
      scene,
      separateMeshes,
      materialManager,
    } = context;

    // Get scale from modifier (default 1.0)
    const scaleX = modifier?.scale?.[0] ?? 1.0;
    const scaleY = modifier?.scale?.[1] ?? 1.0;

    // Calculate quad half-dimensions
    const halfWidth = 0.5 * scaleX;
    const halfHeight = 0.5 * scaleY;

    // Get center offset from edgeOffsets (first 3 values: XYZ)
    let centerOffsetX = 0;
    let centerOffsetY = 0;
    let centerOffsetZ = 0;

    if (context.edgeOffsets) {
      centerOffsetX = context.edgeOffsets[0] / 127.0;
      centerOffsetY = context.edgeOffsets[1] / 127.0;
      centerOffsetZ = context.edgeOffsets[2] / 127.0;
    }

    // Calculate billboard center (block center + offset)
    const centerX = x + 0.5 + centerOffsetX;
    const centerY = y + 0.5 + centerOffsetY;
    const centerZ = z + 0.5 + centerOffsetZ;

    // Create a quad mesh in world coordinates (facing north, towards negative Z)
    const positions = [
      -halfWidth, -halfHeight, 0, // 0: left-bottom
      halfWidth, -halfHeight, 0, // 1: right-bottom
      halfWidth, halfHeight, 0, // 2: right-top
      -halfWidth, halfHeight, 0, // 3: left-top
    ];

    const indices = [
      0, 1, 2, // First triangle
      0, 2, 3, // Second triangle
    ];

    const normals = [
      0, 0, 1, // Normal pointing forward
      0, 0, 1,
      0, 0, 1,
      0, 0, 1,
    ];

    // Get texture UVs from block (use top texture)
    const { u0, v0, u1, v1 } = blockUVs.top;

    // Map UVs to quad vertices to stretch texture across entire quad
    const uvs = [
      u0, v1, // left-bottom
      u1, v1, // right-bottom
      u1, v0, // right-top
      u0, v0, // left-top
    ];

    // Create mesh
    const mesh = new Mesh(`billboard_${x}_${y}_${z}`, scene);

    const vertexData = new VertexData();
    vertexData.positions = positions;
    vertexData.indices = indices;
    vertexData.normals = normals;
    vertexData.uvs = uvs;

    vertexData.applyToMesh(mesh);

    // Position the mesh
    mesh.position.set(centerX, centerY, centerZ);

    // Enable billboard mode for camera-facing (Y-axis only, no up/down tilt)
    mesh.billboardMode = Mesh.BILLBOARDMODE_Y;

    // Ensure mesh is visible
    mesh.isVisible = true;
    mesh.visibility = 1.0;

    // Prevent frustum culling
    mesh.alwaysSelectAsActiveMesh = true;

    // Get material from MaterialManager
    const materialConfig = materialManager!.getMaterialConfig(block, modifier);
    const material = materialManager!.getMaterialByType(
      materialConfig.key as 'solid' | 'transparent' | 'transparent_wind' | 'water' | 'lava'
    );
    material.backFaceCulling = false;

    mesh.material = material;

    // Add mesh to separateMeshes array
    separateMeshes.push(mesh);

    // Return 0 vertices since we're not adding to the material meshes
    return 0;
  }
}
