/**
 * ThinInstancesService - Manages thin instance rendering for grass-like objects
 *
 * Uses Babylon.js Thin Instances for extreme performance with Y-axis billboard shader.
 * Supports GPU-based wind animation and per-block instance configuration.
 */

import { Mesh, MeshBuilder, Scene, Matrix, StandardMaterial, Texture } from '@babylonjs/core';
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { ShaderService } from './ShaderService';

const logger = getLogger('ThinInstancesService');

/**
 * Configuration for a thin instances group
 */
interface ThinInstanceConfig {
  texturePath: string;
  instanceCount: number;
  blockPosition: { x: number; y: number; z: number };
}

/**
 * Thin instances group data
 */
interface ThinInstanceGroup {
  mesh: Mesh;
  matricesData: Float32Array;
  instanceCount: number;
  chunkKey: string;
}

export class ThinInstancesService {
  private scene: Scene;
  private appContext: AppContext;
  private shaderService?: ShaderService;

  // Map: chunkKey -> ThinInstanceGroup[]
  private instanceGroups: Map<string, ThinInstanceGroup[]> = new Map();

  // Base mesh template (will be cloned for each texture)
  private baseMesh?: Mesh;

  // Material cache: texturePath -> Material
  private materialCache: Map<string, StandardMaterial> = new Map();

  constructor(scene: Scene, appContext: AppContext) {
    this.scene = scene;
    this.appContext = appContext;

    this.createBaseMesh();

    logger.info('ThinInstancesService initialized');
  }

  /**
   * Set shader service
   */
  setShaderService(shaderService: ShaderService): void {
    this.shaderService = shaderService;
    logger.debug('ShaderService set');
  }

  /**
   * Create base mesh template (vertical quad)
   */
  private createBaseMesh(): void {
    // Create a simple vertical quad (2x2 ground rotated 90 degrees)
    const mesh = MeshBuilder.CreateGround('thinInstanceBase', { width: 2, height: 2 }, this.scene);
    mesh.rotation.x = Math.PI * 0.5; // Rotate to vertical
    mesh.bakeCurrentTransformIntoVertices(); // Bake rotation into vertices
    mesh.isVisible = false; // Template is invisible

    this.baseMesh = mesh;

    logger.debug('Base mesh template created');
  }

  /**
   * Create thin instances for a block
   *
   * @param config Instance configuration
   * @param chunkKey Parent chunk key
   * @returns Created mesh with thin instances
   */
  async createInstances(config: ThinInstanceConfig, chunkKey: string): Promise<Mesh> {
    if (!this.baseMesh) {
      throw new Error('Base mesh not created');
    }

    logger.info('🌿 Creating thin instances', {
      position: config.blockPosition,
      count: config.instanceCount,
      texturePath: config.texturePath,
    });

    // Clone base mesh for this group
    const mesh = this.baseMesh.clone(`thinInstances_${config.blockPosition.x}_${config.blockPosition.y}_${config.blockPosition.z}`);
    mesh.isVisible = true;

    logger.info('📦 Base mesh cloned', { meshName: mesh.name });

    // Get or create material
    const material = await this.getMaterial(config.texturePath);
    mesh.material = material;

    logger.info('🎨 Material applied', {
      materialName: material.name,
      hasTexture: material.diffuseTexture !== null,
    });

    // Create matrices for instances
    const matricesData = new Float32Array(16 * config.instanceCount);
    const m = Matrix.Identity();

    let index = 0;
    const blockX = config.blockPosition.x;
    const blockY = config.blockPosition.y;
    const blockZ = config.blockPosition.z;

    // Distribute instances randomly within block bounds
    for (let i = 0; i < config.instanceCount; i++) {
      // Random position within block (0.8 = 80% of block size for margin)
      const offsetX = (Math.random() - 0.5) * 0.8;
      const offsetZ = (Math.random() - 0.5) * 0.8;

      // Set instance position in matrix
      m.m[12] = blockX + 0.5 + offsetX;
      m.m[13] = blockY; // Base at block bottom
      m.m[14] = blockZ + 0.5 + offsetZ;

      // Copy matrix to buffer
      m.copyToArray(matricesData, index * 16);
      index++;
    }

    // Set thin instance buffer
    mesh.thinInstanceSetBuffer('matrix', matricesData, 16);

    logger.info('✅ Thin instance buffer set', {
      instanceCount: config.instanceCount,
      bufferSize: matricesData.length,
    });

    // Store group data
    const group: ThinInstanceGroup = {
      mesh,
      matricesData,
      instanceCount: config.instanceCount,
      chunkKey,
    };

    // Add to chunk groups
    if (!this.instanceGroups.has(chunkKey)) {
      this.instanceGroups.set(chunkKey, []);
    }
    this.instanceGroups.get(chunkKey)!.push(group);

    logger.info('✅ Thin instances fully created', {
      position: config.blockPosition,
      count: config.instanceCount,
      chunkKey,
      meshVisible: mesh.isVisible,
      totalVertices: mesh.getTotalVertices(),
    });

    return mesh;
  }

  /**
   * Get or create material with optional Y-axis billboard shader
   */
  private async getMaterial(texturePath: string): Promise<StandardMaterial> {
    // Check cache
    if (this.materialCache.has(texturePath)) {
      logger.debug('Using cached material', { texturePath });
      return this.materialCache.get(texturePath)!;
    }

    logger.info('🎨 Creating material for thin instances', { texturePath });

    // Try to use shader service first
    if (this.shaderService && typeof (this.shaderService as any).createThinInstanceMaterial === 'function') {
      try {
        const material = await (this.shaderService as any).createThinInstanceMaterial(texturePath);
        if (material) {
          this.materialCache.set(texturePath, material);
          logger.info('✅ Shader material created', { texturePath });
          return material;
        }
      } catch (error) {
        logger.warn('Failed to create shader material, using fallback', { error });
      }
    }

    // Fallback: standard material with texture from NetworkService
    logger.info('Creating standard material with NetworkService texture', { texturePath });

    const networkService = this.appContext.services.network;
    if (!networkService) {
      logger.error('🚨 NetworkService not available', { texturePath });

      // Ultimate fallback: create material without texture
      const material = new StandardMaterial(`thinInstance_${texturePath}`, this.scene);
      material.backFaceCulling = false;
      material.specularColor.set(0, 0, 0);
      this.materialCache.set(texturePath, material);
      return material;
    }

    // Create material
    const material = new StandardMaterial(`thinInstance_${texturePath}`, this.scene);

    // Load texture directly via NetworkService
    const url = networkService.getAssetUrl(texturePath);
    logger.info('📥 Loading texture from NetworkService', { texturePath, url });

    const texture = new Texture(url, this.scene);

    // Configure texture
    texture.hasAlpha = true;
    texture.getAlphaFromRGB = false;

    material.diffuseTexture = texture;
    material.backFaceCulling = false;
    material.specularColor.set(0, 0, 0); // No specular

    // Cache material
    this.materialCache.set(texturePath, material);

    logger.info('✅ Material created with texture', {
      texturePath,
      url,
    });

    return material;
  }

  /**
   * Dispose instances for a chunk
   */
  disposeChunkInstances(chunkKey: string): void {
    const groups = this.instanceGroups.get(chunkKey);
    if (!groups) {
      return;
    }

    for (const group of groups) {
      group.mesh.dispose();
    }

    this.instanceGroups.delete(chunkKey);

    logger.debug('Chunk instances disposed', { chunkKey, groupCount: groups.length });
  }

  /**
   * Dispose all instances
   */
  dispose(): void {
    for (const groups of this.instanceGroups.values()) {
      for (const group of groups) {
        group.mesh.dispose();
      }
    }

    this.instanceGroups.clear();

    // Dispose materials
    for (const material of this.materialCache.values()) {
      material.dispose();
    }
    this.materialCache.clear();

    this.baseMesh?.dispose();

    logger.info('ThinInstancesService disposed');
  }

  /**
   * Get statistics
   */
  getStats(): { chunkCount: number; totalInstances: number; groupCount: number } {
    let totalInstances = 0;
    let groupCount = 0;

    for (const groups of this.instanceGroups.values()) {
      groupCount += groups.length;
      for (const group of groups) {
        totalInstances += group.instanceCount;
      }
    }

    return {
      chunkCount: this.instanceGroups.size,
      totalInstances,
      groupCount,
    };
  }
}
