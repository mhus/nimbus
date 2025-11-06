/**
 * BillboardRenderer - Renders camera-facing billboard sprites
 *
 * Creates a vertical quad that always faces the camera (Y-axis rotation only).
 * Each billboard gets its own separate mesh with original texture.
 *
 * Features:
 * - Automatically faces camera (Babylon.js billboardMode)
 * - Aspect ratio from texture dimensions (1 width : ratio height)
 * - Pivot point at block center (offset[0] shifts this point)
 * - Supports scaling, rotation transformations
 */

import { Vector3, Mesh, VertexData, Texture } from '@babylonjs/core';
import { getLogger, TextureHelper } from '@nimbus/shared';
import type { ClientBlock } from '../types';
import { BlockRenderer } from './BlockRenderer';
import type { RenderContext } from '../services/RenderService';

const logger = getLogger('BillboardRenderer');

/**
 * BillboardRenderer - Renders camera-facing billboards
 *
 * Billboards are vertical quads that rotate to face the camera.
 * Default size: 1 unit wide, height determined by texture aspect ratio.
 */
export class BillboardRenderer extends BlockRenderer {
  /**
   * BillboardRenderer needs separate mesh per block
   * (cannot be batched, needs camera-facing behavior)
   */
  needsSeparateMesh(): boolean {
    return true;
  }

  /**
   * Render a BILLBOARD block
   *
   * Creates a vertical quad with camera-facing behavior.
   * Size: 1 unit wide by default, height = width * texture aspect ratio.
   *
   * @param renderContext Render context (not used - separate mesh)
   * @param clientBlock Block to render
   */
  async render(renderContext: RenderContext, clientBlock: ClientBlock): Promise<void> {
    const block = clientBlock.block;
    const modifier = clientBlock.currentModifier;

    if (!modifier || !modifier.visibility) {
      logger.warn('BillboardRenderer: No visibility modifier', { block });
      return;
    }

    // Get first texture (TextureKey.ALL = 0, or TextureKey.TOP = 1)
    const firstTexture = modifier.visibility.textures?.[0] || modifier.visibility.textures?.[1];
    if (!firstTexture) {
      logger.warn('BillboardRenderer: No texture defined', { block });
      return;
    }

    // Normalize texture
    const textureDef = TextureHelper.normalizeTexture(firstTexture);

    // Get transformations
    const scalingX = modifier.visibility.scalingX ?? 1.0;
    const scalingY = modifier.visibility.scalingY ?? 1.0;
    const scalingZ = modifier.visibility.scalingZ ?? 1.0;
    const rotationX = modifier.visibility.rotationX ?? 0;
    const rotationY = modifier.visibility.rotationY ?? 0;

    // Get pivot offset (offset[0] shifts the pivot point)
    let pivotOffsetX = 0;
    let pivotOffsetY = 0;
    let pivotOffsetZ = 0;

    if (modifier.visibility.offsets && modifier.visibility.offsets.length >= 3) {
      pivotOffsetX = modifier.visibility.offsets[0] || 0;
      pivotOffsetY = modifier.visibility.offsets[1] || 0;
      pivotOffsetZ = modifier.visibility.offsets[2] || 0;
    }

    // Block position
    const pos = block.position;

    // Calculate billboard center (block center + pivot offset)
    // IMPORTANT: Center must be above block (Y + 0.5) so billboard stands ON the block
    const centerX = pos.x + 0.5 + pivotOffsetX;
    const centerY = pos.y + 0.5 + pivotOffsetY;
    const centerZ = pos.z + 0.5 + pivotOffsetZ;

    // Get texture aspect ratio
    const aspectRatio = await this.getTextureAspectRatio(textureDef.path, renderContext.renderService);

    // Create billboard geometry
    const faceData = this.createBillboardGeometry(
      scalingX,
      scalingY,
      aspectRatio
    );

    // Create separate mesh for this billboard
    await this.createSeparateMesh(
      clientBlock,
      faceData,
      new Vector3(centerX, centerY, centerZ),
      renderContext.renderService
    );
  }

  /**
   * Get texture aspect ratio (width / height)
   *
   * Loads texture to read dimensions and calculate aspect ratio.
   * If loading fails, returns 1.0 (square).
   *
   * @param texturePath Path to texture
   * @param renderService RenderService for material access
   * @returns Aspect ratio (width / height)
   */
  private async getTextureAspectRatio(
    texturePath: string,
    renderService: any
  ): Promise<number> {
    try {
      // Load texture to get dimensions
      const texture = await renderService.materialService.loadTexture(texturePath) as Texture;

      if (!texture) {
        logger.warn('Could not load texture for aspect ratio', { texturePath });
        return 1.0;
      }

      const size = texture.getSize();
      const aspectRatio = size.width / size.height;

      logger.debug('Billboard texture aspect ratio', {
        texturePath,
        width: size.width,
        height: size.height,
        aspectRatio,
      });

      return aspectRatio;
    } catch (error) {
      logger.warn('Failed to get texture aspect ratio, using 1.0', { texturePath, error });
      return 1.0;
    }
  }

  /**
   * Create billboard geometry (vertical quad)
   *
   * Creates a vertical quad facing forward (towards negative Z).
   * Size: scalingX * 1.0 wide, scalingY * (1.0 / aspectRatio) high.
   *
   * @param scalingX X-axis scaling (width)
   * @param scalingY Y-axis scaling (height multiplier)
   * @param aspectRatio Texture aspect ratio (width / height)
   * @returns Face geometry data
   */
  private createBillboardGeometry(
    scalingX: number,
    scalingY: number,
    aspectRatio: number
  ): { positions: number[]; indices: number[]; uvs: number[]; normals: number[] } {
    // Calculate dimensions
    // Width: 1 unit * scalingX
    // Height: (1 unit / aspectRatio) * scalingY
    const halfWidth = 0.5 * scalingX;
    const halfHeight = (0.5 / aspectRatio) * scalingY;

    // Create vertical quad facing forward (towards -Z)
    // Quad is centered at origin (will be positioned by mesh.position)
    const positions = [
      -halfWidth, -halfHeight, 0, // 0: left-bottom
      +halfWidth, -halfHeight, 0, // 1: right-bottom
      +halfWidth, +halfHeight, 0, // 2: right-top
      -halfWidth, +halfHeight, 0, // 3: left-top
    ];

    // Indices for two triangles (quad)
    const indices = [
      0, 1, 2, // First triangle
      0, 2, 3, // Second triangle
    ];

    // UV coordinates (full texture)
    const uvs = [
      0, 1, // left-bottom
      1, 1, // right-bottom
      1, 0, // right-top
      0, 0, // left-top
    ];

    // Normals (pointing forward)
    const normals = [
      0, 0, 1, // Vertex 0
      0, 0, 1, // Vertex 1
      0, 0, 1, // Vertex 2
      0, 0, 1, // Vertex 3
    ];

    return { positions, indices, uvs, normals };
  }

  /**
   * Create separate mesh for this BILLBOARD block
   *
   * Creates a new mesh with billboard behavior and material from MaterialService.
   *
   * @param clientBlock Block to create mesh for
   * @param faceData Geometry data
   * @param centerPosition World position for billboard center
   * @param renderService RenderService instance for material access
   */
  private async createSeparateMesh(
    clientBlock: ClientBlock,
    faceData: { positions: number[]; indices: number[]; uvs: number[]; normals: number[] },
    centerPosition: Vector3,
    renderService: any // RenderService type
  ): Promise<void> {
    const block = clientBlock.block;
    const scene = renderService.materialService.scene;

    // Create mesh name
    const meshName = `billboard_${block.position.x}_${block.position.y}_${block.position.z}`;

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

    // Position the mesh at center
    mesh.position.copyFrom(centerPosition);

    // Enable billboard mode for camera-facing (Y-axis only, no up/down tilt)
    mesh.billboardMode = Mesh.BILLBOARDMODE_Y;

    // Ensure mesh is visible
    mesh.isVisible = true;
    mesh.visibility = 1.0;

    // Prevent frustum culling issues
    mesh.alwaysSelectAsActiveMesh = true;

    // Get material from MaterialService
    // MaterialService will use original texture (not atlas) for billboards
    const material = await renderService.materialService.getMaterial(
      clientBlock.currentModifier,
      0 // TextureKey.ALL
    );

    // Disable backface culling for billboards (visible from both sides)
    material.backFaceCulling = false;

    mesh.material = material;

    // Store mesh in RenderService separate mesh tracking
    // TODO: RenderService needs to track separate meshes
    // For now, just create the mesh - it will be in the scene

    logger.debug('BILLBOARD separate mesh created', {
      meshName,
      position: centerPosition,
      vertices: faceData.positions.length / 3,
      material: material.name,
    });
  }
}
