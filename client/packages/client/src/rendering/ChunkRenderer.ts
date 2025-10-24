/**
 * Chunk Renderer - Creates meshes from chunk data using texture atlas
 */

import {
  Mesh,
  VertexData,
  VertexBuffer,
  Scene,
  Matrix
} from '@babylonjs/core';
import type { ChunkData } from '@nimbus-client/core';
import type { TextureAtlas } from './TextureAtlas';
import type { BlockType } from '@nimbus-client/core';
import type { ClientRegistry } from '../registry/ClientRegistry';
import type { MaterialManager, MaterialConfig } from './MaterialManager';
import {
  unpackMetadata,
  getBlockEdgeOffsets,
  getBlockModifier,
  applyModifier,
  BlockShape
} from '@nimbus-client/core';
import {
  ShapeRenderer,
  CubeRenderer,
  GlassRenderer,
  CrossRenderer,
  HashRenderer,
  FlatRenderer,
  SphereRenderer,
  ColumnRenderer,
  RoundCubeRenderer,
  StepsRenderer,
  StairRenderer,
  BillboardRenderer,
  SpriteRenderer,
  FlameRenderer,
  type BlockRenderContext,
  type MaterialMeshData
} from './shapes';
import type { SpriteManagerRegistry } from './SpriteManagerRegistry';

/**
 * Renders chunks as Babylon.js meshes using texture atlas
 */
export class ChunkRenderer {
  private scene: Scene;
  private chunkSize = 32;
  private atlas: TextureAtlas;
  private registry: ClientRegistry;
  private materialManager: MaterialManager;
  private spriteManagerRegistry: SpriteManagerRegistry;

  // Shape renderers
  private shapeRenderers: Map<BlockShape, ShapeRenderer>;

  constructor(scene: Scene, atlas: TextureAtlas, registry: ClientRegistry, materialManager: MaterialManager, spriteManagerRegistry: SpriteManagerRegistry) {
    this.scene = scene;
    this.atlas = atlas;
    this.registry = registry;
    this.materialManager = materialManager;
    this.spriteManagerRegistry = spriteManagerRegistry;

    // Initialize shape renderers
    this.shapeRenderers = new Map<BlockShape, ShapeRenderer>();
    this.shapeRenderers.set(BlockShape.CUBE, new CubeRenderer());
    this.shapeRenderers.set(BlockShape.GLASS, new GlassRenderer());
    this.shapeRenderers.set(BlockShape.CROSS, new CrossRenderer());
    this.shapeRenderers.set(BlockShape.HASH, new HashRenderer());
    this.shapeRenderers.set(BlockShape.FLAT, new FlatRenderer());
    this.shapeRenderers.set(BlockShape.SPHERE, new SphereRenderer());
    this.shapeRenderers.set(BlockShape.COLUMN, new ColumnRenderer());
    this.shapeRenderers.set(BlockShape.ROUND_CUBE, new RoundCubeRenderer());
    this.shapeRenderers.set(BlockShape.STEPS, new StepsRenderer());
    this.shapeRenderers.set(BlockShape.STAIR, new StairRenderer());
    this.shapeRenderers.set(BlockShape.BILLBOARD, new BillboardRenderer());
    this.shapeRenderers.set(BlockShape.SPRITE, new SpriteRenderer());
    this.shapeRenderers.set(BlockShape.FLAME, new FlameRenderer());

    console.log('[ChunkRenderer] Initialized with texture atlas and shape renderers');
  }


  /**
   * Create or get material mesh data container
   */
  private getOrCreateMaterialData(
    materialMeshes: Map<string, MaterialMeshData>,
    materialKey: string,
    needsWindAttributes: boolean
  ): MaterialMeshData {
    if (!materialMeshes.has(materialKey)) {
      const data: MaterialMeshData = {
        positions: [],
        indices: [],
        normals: [],
        uvs: [],
        colors: [],
        vertexIndex: 0,
      };

      // Add wind attributes if needed
      if (needsWindAttributes) {
        data.windLeafiness = [];
        data.windStability = [];
        data.windLeverUp = [];
        data.windLeverDown = [];
      }

      materialMeshes.set(materialKey, data);
    }

    return materialMeshes.get(materialKey)!;
  }

  /**
   * Create mesh from chunk data using dynamic texture atlas
   */
  async createChunkMesh(chunk: ChunkData): Promise<Mesh> {
    console.log(`[ChunkRenderer] Starting to create chunk mesh ${chunk.chunkX},${chunk.chunkZ}`);

    const data = chunk.data;
    const height = chunk.height || 256;

    // Map to hold material-specific mesh data
    const materialMeshes = new Map<string, MaterialMeshData>();

    // Array to hold separate meshes (for billboards, sprites, etc.)
    const separateMeshes: Mesh[] = [];

    // Array to collect all sprites created during rendering
    const sprites: any[] = [];

    // Index formula must match server: x + y * chunkSize + z * chunkSize * height
    for (let x = 0; x < this.chunkSize; x++) {
      for (let z = 0; z < this.chunkSize; z++) {
        for (let y = 0; y < height; y++) {
          const index = x + y * this.chunkSize + z * this.chunkSize * height;
          const blockId = data[index];

          // Skip air blocks (id 0)
          if (blockId === 0) continue;

          // Get base block type from registry
          const baseBlock = this.registry.getBlockByID(blockId);
          if (!baseBlock) {
            console.warn(`[ChunkRenderer] Unknown block ID: ${blockId}`);
            continue;
          }

          // Check for modifier and apply if present
          const modifier = getBlockModifier(chunk, x, y, z, this.chunkSize);
          const block = applyModifier(baseBlock, modifier);

          // Get UV mapping for this block (loads texture into atlas if needed)
          // Use modifier.texture if present, otherwise use block.texture
          const blockUVs = await this.atlas.getBlockUVs(block, modifier?.texture);

          // Get metadata if available
          const packedMetadata = chunk.metadata?.[index] || 0;
          const metadata = packedMetadata ? unpackMetadata(packedMetadata) : null;

          // Get edge offsets if available
          const edgeOffsets = getBlockEdgeOffsets(chunk, x, y, z, this.chunkSize);

          // Calculate rotation matrix from modifier
          let rotationMatrix: Matrix | null = null;

          // Check for facing first (determines primary orientation)
          if (modifier?.facing !== undefined) {
            rotationMatrix = this.getFacingRotationMatrix(modifier.facing);
          }

          // Apply rotationX (vertical tilt/pitch) if specified
          const rotX = modifier?.rotationX;
          if (rotX !== undefined) {
            const angleRadians = (rotX * Math.PI) / 180;
            const rotationXMatrix = Matrix.RotationX(angleRadians);
            rotationMatrix = rotationMatrix ? rotationMatrix.multiply(rotationXMatrix) : rotationXMatrix;
          }

          // Apply rotationY (horizontal rotation) if specified
          // Support both new rotationY and deprecated rotation for backward compatibility
          const rotY = modifier?.rotationY ?? modifier?.rotation;
          if (rotY !== undefined) {
            const angleRadians = (rotY * Math.PI) / 180;
            const rotationYMatrix = Matrix.RotationY(angleRadians);
            rotationMatrix = rotationMatrix ? rotationMatrix.multiply(rotationYMatrix) : rotationYMatrix;
          }

          // World position
          const wx = chunk.chunkX * this.chunkSize + x;
          const wy = y;
          const wz = chunk.chunkZ * this.chunkSize + z;

          // Extract color from modifier (RGB 0-255 -> normalized 0.0-1.0)
          // Default to white (1, 1, 1) if no color specified
          const blockColor: [number, number, number, number] = modifier?.color
            ? [
                modifier.color[0] / 255.0,
                modifier.color[1] / 255.0,
                modifier.color[2] / 255.0,
                1.0 // Alpha always 1.0
              ]
            : [1.0, 1.0, 1.0, 1.0]; // White tint (no color change)

          // Get the block shape
          const shape = block.shape ?? BlockShape.CUBE;

          // Check if this shape creates separate meshes (doesn't contribute to chunk mesh)
          // These shapes manage their own materials and don't need MaterialManager
          const createsSeparateMesh =
            shape === BlockShape.BILLBOARD ||
            shape === BlockShape.SPRITE ||
            shape === BlockShape.FLAME;

          // For shapes that create separate meshes, skip material creation
          if (createsSeparateMesh) {
            // Get the appropriate shape renderer
            const renderer = this.shapeRenderers.get(shape) || this.shapeRenderers.get(BlockShape.CUBE)!;

            // Create minimal render context for separate mesh shapes
            const context: BlockRenderContext = {
              x: wx,
              y: wy,
              z: wz,
              block,
              modifier,
              blockUVs,
              blockColor,
              rotationMatrix,
              edgeOffsets,
              // Separate mesh shapes don't use chunk mesh arrays
              positions: [],
              indices: [],
              normals: [],
              uvs: [],
              colors: [],
              vertexIndex: 0,
              // Multi-material support (not used by separate mesh shapes)
              materialMeshes,
              materialManager: this.materialManager,
              // Separate meshes support
              scene: this.scene,
              separateMeshes,
              // Sprite manager registry
              spriteManagerRegistry: this.spriteManagerRegistry,
              // Sprite collection array
              sprites,
            };

            // Render the block (will create separate mesh)
            await renderer.render(context);

            // Skip the rest of the chunk mesh logic
            continue;
          }

          // Normal chunk mesh blocks: Get material configuration from MaterialManager
          const materialConfig = this.materialManager.getMaterialConfig(block, modifier);

          // Debug: Log material classification for blocks with wind properties
          if (modifier?.windLeafiness || modifier?.windStability || modifier?.windLeverUp || modifier?.windLeverDown ||
              block.windLeafiness || block.windStability || block.windLeverUp || block.windLeverDown) {
            console.log(`[ChunkRenderer] ⚠️ WIND BLOCK DETECTED: ${block.name} at (${wx},${wy},${wz})`);
            console.log(`[ChunkRenderer] Material config:`, {
              materialKey: materialConfig.key,
              needsWindAttributes: materialConfig.needsWindAttributes,
              blockTransparent: block.transparent,
              blockOptionsTransparent: block.options?.transparent,
              modifierWindLeafiness: modifier?.windLeafiness,
              modifierWindStability: modifier?.windStability,
              modifierWindLeverUp: modifier?.windLeverUp,
              modifierWindLeverDown: modifier?.windLeverDown,
              blockWindLeafiness: block.windLeafiness,
              blockWindStability: block.windStability,
              blockWindLeverUp: block.windLeverUp,
              blockWindLeverDown: block.windLeverDown
            });
          }

          const materialData = this.getOrCreateMaterialData(
            materialMeshes,
            materialConfig.key,
            materialConfig.needsWindAttributes
          );

          // Get references to the arrays for this material
          const positions = materialData.positions;
          const indices = materialData.indices;
          const normals = materialData.normals;
          const uvs = materialData.uvs;
          const colors = materialData.colors;
          const vertexIndex = materialData.vertexIndex;

          // Render based on shape
          let verticesAdded = 0;

          // Get the appropriate shape renderer
          const renderer = this.shapeRenderers.get(shape) || this.shapeRenderers.get(BlockShape.CUBE)!;

          // Create render context with full multi-material support
          const context: BlockRenderContext = {
            x: wx,
            y: wy,
            z: wz,
            block,
            modifier,
            blockUVs,
            blockColor,
            rotationMatrix,
            edgeOffsets,
            // Default material arrays (backwards compatibility)
            positions,
            indices,
            normals,
            uvs,
            colors,
            vertexIndex,
            // Multi-material support
            materialMeshes,
            materialManager: this.materialManager,
            // Separate meshes support (for billboards, sprites)
            scene: this.scene,
            separateMeshes,
            // Sprite manager registry (for SPRITE blocks)
            spriteManagerRegistry: this.spriteManagerRegistry,
            // Sprite collection array
            sprites,
          };

          // Add wind properties to context for blocks that need them
          if (materialConfig.needsWindAttributes) {
            context.windLeafiness = materialData.windLeafiness;
            context.windStability = materialData.windStability;
            context.windLeverUp = materialData.windLeverUp;
            context.windLeverDown = materialData.windLeverDown;
          }

          // Render the block using the appropriate renderer (await if async)
          verticesAdded = await renderer.render(context);

          // Update vertex index in material data
          materialData.vertexIndex += verticesAdded;
        }
      }
    }

    // Create parent mesh to hold all material-specific meshes
    const parentMesh = new Mesh(`chunk_${chunk.chunkX}_${chunk.chunkZ}`, this.scene);

    // Create meshes for each material type that has blocks
    for (const [materialKey, materialData] of materialMeshes) {
      if (materialData.positions.length === 0) {
        continue; // Skip empty materials
      }

      const mesh = new Mesh(`chunk_${chunk.chunkX}_${chunk.chunkZ}_${materialKey}`, this.scene);

      const vertexData = new VertexData();
      vertexData.positions = materialData.positions;
      vertexData.indices = materialData.indices;
      vertexData.normals = materialData.normals;
      vertexData.uvs = materialData.uvs;
      vertexData.colors = materialData.colors;

      vertexData.applyToMesh(mesh);

      // Set wind properties as custom vertex attributes if present
      if (materialData.windLeafiness && materialData.windStability && materialData.windLeverUp && materialData.windLeverDown) {
        // Custom attributes must be set using VertexBuffer, not setVerticesData
        const engine = this.scene.getEngine();

        const windLeafinessBuffer = new VertexBuffer(
          engine,
          materialData.windLeafiness,
          'windLeafiness',
          false,
          false,
          1, // stride (1 float per vertex)
          false,
          0, // offset
          1  // size (1 component)
        );
        mesh.setVerticesBuffer(windLeafinessBuffer);

        const windStabilityBuffer = new VertexBuffer(
          engine,
          materialData.windStability,
          'windStability',
          false,
          false,
          1,
          false,
          0,
          1
        );
        mesh.setVerticesBuffer(windStabilityBuffer);

        const windLeverUpBuffer = new VertexBuffer(
          engine,
          materialData.windLeverUp,
          'windLeverUp',
          false,
          false,
          1,
          false,
          0,
          1
        );
        mesh.setVerticesBuffer(windLeverUpBuffer);

        const windLeverDownBuffer = new VertexBuffer(
          engine,
          materialData.windLeverDown,
          'windLeverDown',
          false,
          false,
          1,
          false,
          0,
          1
        );
        mesh.setVerticesBuffer(windLeverDownBuffer);

        console.log(`[ChunkRenderer] Set custom wind attributes for ${materialKey} mesh`);
      }

      // Get material from MaterialManager
      // Extract base material type from materialKey, preserving wind information
      // Examples: "solid_wind_texture.png" -> "solid_wind", "transparent_wind" -> "transparent_wind"
      let materialType: 'solid' | 'solid_wind' | 'transparent' | 'transparent_wind' | 'water' | 'lava';

      if (materialKey.startsWith('solid_wind')) {
        materialType = 'solid_wind';
      } else if (materialKey.startsWith('transparent_wind')) {
        materialType = 'transparent_wind';
      } else if (materialKey.startsWith('transparent')) {
        materialType = 'transparent';
      } else if (materialKey.startsWith('water')) {
        materialType = 'water';
      } else if (materialKey.startsWith('lava')) {
        materialType = 'lava';
      } else {
        materialType = 'solid';
      }

      const material = this.materialManager.getMaterialByType(materialType);
      mesh.material = material;

      mesh.parent = parentMesh;

      console.log(
        `[ChunkRenderer] Created ${materialKey} mesh for chunk ${chunk.chunkX},${chunk.chunkZ} with ${materialData.vertexIndex} vertices`
      );
    }

    // Attach separate meshes (billboards, sprites, etc.) to parent
    for (const separateMesh of separateMeshes) {
      separateMesh.parent = parentMesh;
    }

    if (separateMeshes.length > 0) {
      console.log(`[ChunkRenderer] Attached ${separateMeshes.length} separate meshes to chunk ${chunk.chunkX},${chunk.chunkZ}`);
    }

    // Store sprites in parent mesh metadata for proper disposal later
    if (sprites.length > 0) {
      parentMesh.metadata = parentMesh.metadata || {};
      parentMesh.metadata.sprites = sprites;
      console.log(`[ChunkRenderer] Stored ${sprites.length} sprites in chunk ${chunk.chunkX},${chunk.chunkZ} metadata for disposal`);
    }

    console.log(`[ChunkRenderer] Created chunk ${chunk.chunkX},${chunk.chunkZ} with ${materialMeshes.size} material types`);

    return parentMesh;
  }

  /**
   * Get rotation matrix for facing direction
   * This is kept in ChunkRenderer since rotation is calculated before passing to shape renderers
   */
  private getFacingRotationMatrix(facing: number): Matrix {
    // Import BlockFacing enum values for the switch
    const BlockFacing = { NORTH: 0, EAST: 1, SOUTH: 2, WEST: 3, UP: 4, DOWN: 5 };

    switch (facing) {
      case BlockFacing.NORTH: // 0
        return Matrix.Identity(); // No rotation needed

      case BlockFacing.EAST: // 1
        return Matrix.RotationY(Math.PI / 2); // 90° clockwise

      case BlockFacing.SOUTH: // 2
        return Matrix.RotationY(Math.PI); // 180°

      case BlockFacing.WEST: // 3
        return Matrix.RotationY((3 * Math.PI) / 2); // 270° or -90°

      case BlockFacing.UP: // 4
        return Matrix.RotationX(-Math.PI / 2); // Pitch up 90°

      case BlockFacing.DOWN: // 5
        return Matrix.RotationX(Math.PI / 2); // Pitch down 90°

      default:
        return Matrix.Identity(); // Fallback to no rotation
    }
  }
}
