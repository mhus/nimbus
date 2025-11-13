/**
 * ItemRenderer - Renders items as Y-axis billboards
 *
 * Items are special billboard blocks optimized for pickable objects (tools, weapons, etc.).
 * They always use Y-axis locked billboards and have item-specific defaults.
 *
 * Default settings:
 * - scalingX: 0.5 (half block width)
 * - scalingY: 0.5 (half block height)
 *
 * Enforced settings (always applied):
 * - transparencyMode: ALPHA_TEST (sharp cutout edges, cannot be overridden)
 */

import { Vector3, Mesh, VertexData, Texture } from '@babylonjs/core';
import { getLogger, TextureHelper, TransparencyMode } from '@nimbus/shared';
import type { ClientBlock } from '../types';
import { BlockRenderer } from './BlockRenderer';
import type { RenderContext } from '../services/RenderService';

const logger = getLogger('ItemRenderer');

/**
 * Default scaling for items (half block size)
 */
const DEFAULT_ITEM_SCALING = 0.5;

/**
 * ItemRenderer - Renders item billboards with item-specific defaults
 *
 * Items are Y-axis locked billboards optimized for pickable objects.
 * Default size: 0.5 units wide, height determined by texture aspect ratio.
 */
export class ItemRenderer extends BlockRenderer {
  /**
   * ItemRenderer needs separate mesh per block
   * (cannot be batched, needs camera-facing behavior)
   */
  needsSeparateMesh(): boolean {
    return true;
  }

  /**
   * Render an ITEM block
   *
   * Creates a vertical quad with camera-facing behavior and item-specific defaults.
   * Size: 0.5 units wide by default, height = width * texture aspect ratio.
   *
   * @param renderContext Render context (not used - separate mesh)
   * @param clientBlock Block to render
   */
  async render(renderContext: RenderContext, clientBlock: ClientBlock): Promise<void> {
    const block = clientBlock.block;
    const modifier = clientBlock.currentModifier;

    if (!modifier || !modifier.visibility) {
      logger.warn('ItemRenderer: No visibility modifier', { block });
      return;
    }

    // Get first texture (TextureKey.ALL = 0, or TextureKey.TOP = 1)
    const firstTexture = modifier.visibility.textures?.[0] || modifier.visibility.textures?.[1];
    if (!firstTexture) {
      logger.warn('ItemRenderer: No texture defined', { block });
      return;
    }

    // Normalize texture
    const textureDef = TextureHelper.normalizeTexture(firstTexture);

    // Items ALWAYS use ALPHA_TEST for sharp cutout edges
    textureDef.transparencyMode = TransparencyMode.ALPHA_TEST;

    // Get transformations with item-specific defaults
    const scalingX = modifier.visibility.scalingX ?? DEFAULT_ITEM_SCALING;
    const scalingY = modifier.visibility.scalingY ?? DEFAULT_ITEM_SCALING;
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

    // Create separate mesh for this item (aspect ratio will be calculated from loaded texture)
    await this.createSeparateMesh(
      clientBlock,
      new Vector3(centerX, centerY, centerZ),
      scalingX,
      scalingY,
      renderContext
    );
  }

  /**
   * Create item geometry (vertical quad)
   *
   * Creates a vertical quad facing forward (towards negative Z).
   * Size: scalingX * 1.0 wide, scalingY * (1.0 / aspectRatio) high.
   *
   * @param scalingX X-axis scaling (width)
   * @param scalingY Y-axis scaling (height multiplier)
   * @param aspectRatio Texture aspect ratio (width / height)
   * @returns Face geometry data
   */
  private createItemGeometry(
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
   * Create separate mesh for this ITEM block
   *
   * Creates a new mesh with billboard behavior and material from MaterialService.
   * Items are always Y-axis locked billboards.
   *
   * @param clientBlock Block to create mesh for
   * @param centerPosition World position for item center
   * @param scalingX X-axis scaling
   * @param scalingY Y-axis scaling
   * @param renderContext Render context with resourcesToDispose
   */
  private async createSeparateMesh(
    clientBlock: ClientBlock,
    centerPosition: Vector3,
    scalingX: number,
    scalingY: number,
    renderContext: RenderContext
  ): Promise<void> {
    const block = clientBlock.block;
    const scene = renderContext.renderService.materialService.scene;

    // Create mesh name (include item ID if available)
    const itemId = block.metadata?.id || 'unknown';
    const meshName = `item_${itemId}_${block.position.x}_${block.position.y}_${block.position.z}`;

    // Get material first to extract texture aspect ratio
    const material = await renderContext.renderService.materialService.createOriginalTextureMaterial(
      meshName,
      clientBlock.currentModifier,
      0 // TextureKey.ALL
    );

    // Calculate aspect ratio from loaded texture
    let aspectRatio = 1.0;
    const diffuseTexture = material.diffuseTexture as Texture;
    if (diffuseTexture) {
      // Wait for texture to be ready
      await new Promise<void>((resolve) => {
        if (diffuseTexture.isReady()) {
          resolve();
        } else {
          diffuseTexture.onLoadObservable.addOnce(() => resolve());
        }
      });

      const size = diffuseTexture.getSize();
      if (size && size.width > 0 && size.height > 0) {
        aspectRatio = size.width / size.height;
        logger.debug('Item aspect ratio calculated', {
          itemId,
          width: size.width,
          height: size.height,
          aspectRatio,
        });
      }
    }

    // Create item geometry with calculated aspect ratio
    const faceData = this.createItemGeometry(scalingX, scalingY, aspectRatio);

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
    mesh.isPickable = false; // Items should not be pickable for block selection (use custom item picking)

    // Prevent frustum culling issues
    mesh.alwaysSelectAsActiveMesh = true;

    // Force mesh to render
    mesh.refreshBoundingInfo();
    mesh.computeWorldMatrix(true);

    // Apply material (already created above to get aspect ratio)
    mesh.material = material;

    // Register mesh for automatic disposal when chunk is unloaded
    renderContext.resourcesToDispose.addMesh(mesh);

    logger.debug('Item mesh created', {
      meshName,
      itemId,
      displayName: block.metadata?.displayName,
      position: centerPosition,
      aspectRatio,
    });
  }
}
