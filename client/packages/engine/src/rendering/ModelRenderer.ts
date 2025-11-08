/**
 * ModelRenderer - Renders 3D model files as blocks
 *
 * Loads external 3D models (.babylon files) and renders them as blocks.
 * Supports scaling, rotation, offsets, and automatic size normalization.
 * Each model gets a separate mesh (not part of chunk mesh).
 */

import { Mesh, SceneLoader, Vector3, Matrix } from '@babylonjs/core';
import '@babylonjs/loaders'; // Required for .babylon file format
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { ClientBlock } from '../types';
import { BlockRenderer } from './BlockRenderer';
import type { RenderContext } from '../services/RenderService';

const logger = getLogger('ModelRenderer');

/**
 * ModelRenderer - Renders blocks as 3D models
 *
 * Features:
 * - Loads .babylon model files
 * - Caches loaded models for reuse
 * - Automatic scaling to fit block size
 * - Supports offset, scaling, rotation transformations
 * - Each block gets its own mesh instance
 */
export class ModelRenderer extends BlockRenderer {
  // Static cache: shared across all instances
  // Key: model path, Value: template mesh
  private static modelCache = new Map<string, Mesh>();

  /**
   * MODEL blocks need separate meshes
   */
  needsSeparateMesh(): boolean {
    return true;
  }

  /**
   * Render a MODEL block
   *
   * @param renderContext - Render context
   * @param block - The block to render
   */
  async render(renderContext: RenderContext, block: ClientBlock): Promise<void> {
    const worldX = block.block.position.x;
    const worldY = block.block.position.y;
    const worldZ = block.block.position.z;

    const modifier = block.currentModifier;

    if (!modifier || !modifier.visibility) {
      logger.debug('Block has no visibility modifier', { blockTypeId: block.blockType.id });
      return;
    }

    // Get model path from VisibilityModifier.path
    const modelPath = modifier.visibility.path;

    if (!modelPath) {
      logger.warn('No model path found in visibility.path', {
        blockTypeId: block.blockType.id,
        position: { x: worldX, y: worldY, z: worldZ }
      });
      return;
    }

    // Get full model URL via NetworkService
    // No path normalization - use path as-is (e.g., "models/skull.babylon")
    const networkService = renderContext.renderService.appContext.services.network;
    if (!networkService) {
      logger.error('NetworkService not available', {
        blockTypeId: block.blockType.id,
        position: { x: worldX, y: worldY, z: worldZ }
      });
      return;
    }

    const fullModelUrl = networkService.getAssetUrl(modelPath);

    logger.debug('Loading model', {
      modelPath,
      fullModelUrl,
      position: { x: worldX, y: worldY, z: worldZ }
    });

    // Get transformations
    const scalingX = modifier.visibility?.scalingX ?? 1.0;
    const scalingY = modifier.visibility?.scalingY ?? 1.0;
    const scalingZ = modifier.visibility?.scalingZ ?? 1.0;

    // Get position offset from offsets (first 3 values: XYZ)
    const offsets = modifier.visibility.offsets;
    let offsetX = 0;
    let offsetY = 0;
    let offsetZ = 0;

    if (offsets && offsets.length >= 3) {
      offsetX = (offsets[0] ?? 0) / 127.0;
      offsetY = (offsets[1] ?? 0) / 127.0;
      offsetZ = (offsets[2] ?? 0) / 127.0;
    }

    // Calculate final position (block center + offset)
    const posX = worldX + 0.5 + offsetX;
    const posY = worldY + 0.5 + offsetY;
    const posZ = worldZ + 0.5 + offsetZ;

    try {
      // Load or get cached model
      let modelMesh: Mesh;

      if (ModelRenderer.modelCache.has(modelPath)) {
        // Clone cached model
        const cachedMesh = ModelRenderer.modelCache.get(modelPath)!;
        modelMesh = cachedMesh.clone(`model_${worldX}_${worldY}_${worldZ}`, null)!;
        logger.debug('Cloned cached model', { modelPath });
      } else {
        // Load new model
        logger.info('Loading new model from asset server', { fullModelUrl });

        // Parse URL to get root path and filename
        const lastSlashIndex = fullModelUrl.lastIndexOf('/');
        const rootUrl = fullModelUrl.substring(0, lastSlashIndex + 1);
        const filename = fullModelUrl.substring(lastSlashIndex + 1);

        logger.debug('SceneLoader parameters', { rootUrl, filename });

        // Load the model
        const result = await SceneLoader.ImportMeshAsync(
          '', // Load all meshes
          rootUrl,
          filename,
          renderContext.renderService.scene
        );

        if (!result.meshes || result.meshes.length === 0) {
          throw new Error(`No meshes found in model: ${fullModelUrl}`);
        }

        logger.info('Model loaded successfully', {
          modelPath,
          meshCount: result.meshes.length
        });

        // Merge all meshes into one for better performance
        const allMeshes = result.meshes.filter(m => m instanceof Mesh) as Mesh[];

        if (allMeshes.length === 0) {
          throw new Error('No Mesh instances found in loaded model');
        }

        if (allMeshes.length > 1) {
          logger.debug('Merging meshes', { count: allMeshes.length });
          const merged = Mesh.MergeMeshes(
            allMeshes,
            true,  // disposeSource
            true,  // allow32BitsIndices
            undefined,
            false, // subdivideWithSubMeshes
            true   // multiMultiMaterials
          );

          if (!merged) {
            logger.warn('Failed to merge meshes, using first mesh', { modelPath });
            modelMesh = allMeshes[0];
          } else {
            modelMesh = merged;
            modelMesh.name = `model_template_${modelPath}`;
          }
        } else {
          modelMesh = allMeshes[0];
        }

        // Cache the loaded model (make it invisible, it's just a template)
        ModelRenderer.modelCache.set(modelPath, modelMesh);
        modelMesh.setEnabled(false); // Disable template mesh so it's not rendered
        logger.info('Model cached', { modelPath });

        // Clone for this instance
        modelMesh = modelMesh.clone(`model_${worldX}_${worldY}_${worldZ}`, null)!;
        modelMesh.setEnabled(true); // Enable the clone
      }

      // Calculate bounding box for automatic scaling
      modelMesh.computeWorldMatrix(true);
      const boundingInfo = modelMesh.getBoundingInfo();
      const boundingBox = boundingInfo.boundingBox;
      const modelSize = boundingBox.extendSize.scale(2); // extendSize is half-size

      // Calculate automatic scale factor to fit model into 1 block
      const maxDimension = Math.max(modelSize.x, modelSize.y, modelSize.z);
      const autoScale = maxDimension > 0 ? 1.0 / maxDimension : 1.0;

      logger.debug('Model dimensions', {
        size: { x: modelSize.x, y: modelSize.y, z: modelSize.z },
        maxDimension,
        autoScale
      });

      // Apply transformations
      modelMesh.position = new Vector3(posX, posY, posZ);

      // Combine user scaling with auto-scaling
      modelMesh.scaling = new Vector3(
        scalingX * autoScale,
        scalingY * autoScale,
        scalingZ * autoScale
      );

      // Apply rotation
      const rotationX = modifier.visibility?.rotationX ?? 0;
      const rotationY = modifier.visibility?.rotationY ?? 0;

      if (rotationX !== 0) {
        modelMesh.rotation.x = (rotationX * Math.PI) / 180;
      }

      if (rotationY !== 0) {
        modelMesh.rotation.y = (rotationY * Math.PI) / 180;
      }

      // Ensure mesh is visible
      modelMesh.isVisible = true;
      modelMesh.visibility = 1.0;

      // Register mesh for automatic disposal when chunk is unloaded
      renderContext.resourcesToDispose.addMesh(modelMesh);

      logger.debug('Model rendered', {
        blockTypeId: block.blockType.id,
        position: { x: worldX, y: worldY, z: worldZ },
        modelPath,
        finalScaling: modelMesh.scaling,
        rotation: { x: modelMesh.rotation.x, y: modelMesh.rotation.y }
      });

    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(
        error,
        'ModelRenderer.render',
        {
          modelPath,
          fullModelUrl,
          position: { x: worldX, y: worldY, z: worldZ },
          blockTypeId: block.blockType.id,
          troubleshooting: [
            'Make sure the model file exists in the assets directory on the server',
            'Check that the file path is correct (e.g., "models/skull.babylon")',
            'Verify the model file format is valid .babylon JSON',
            'Check that NetworkService.getAssetUrl() returns correct URL',
            'Verify worldInfo.assetPath is configured correctly'
          ]
        }
      );
    }
  }

  /**
   * Clear the model cache (useful for hot-reloading during development)
   */
  static clearCache(): void {
    for (const mesh of ModelRenderer.modelCache.values()) {
      mesh.dispose();
    }
    ModelRenderer.modelCache.clear();
    logger.info('Model cache cleared', { cachedModels: ModelRenderer.modelCache.size });
  }
}
