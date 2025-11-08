/**
 * Model Renderer
 * Renders MODEL shaped blocks by loading external 3D models (.babylon files)
 * Supports scaling, rotateX, rotateY and edgeOffset for positioning
 */

import { Mesh, SceneLoader, Vector3 } from '@babylonjs/core';
import '@babylonjs/loaders'; // Required for .babylon file format
import { ShapeRenderer } from './ShapeRenderer';
import type { BlockRenderContext } from './BlockRenderContext';

export class ModelRenderer extends ShapeRenderer {
  // Cache loaded models to avoid reloading the same model multiple times
  private static modelCache = new Map<string, Mesh>();

  async render(context: BlockRenderContext): Promise<number> {
    const {
      x,
      y,
      z,
      block,
      modifier,
      scene,
      separateMeshes,
      edgeOffsets,
      rotationMatrix,
      materialManager,
      textureAtlas,
    } = context;

    // Get model path from block texture field (reuse texture field for model path)
    // Example: block.texture = "models/skull.babylon"
    const modelPath = modifier?.texture || block.texture;

    if (!modelPath || typeof modelPath !== 'string') {
      console.warn(`[ModelRenderer] No model path specified for block at ${x},${y},${z}`);
      return 0;
    }

    // Normalize model path - ensure it starts with textures/ to match asset server structure
    // If path doesn't start with 'textures/', prepend 'textures/block/'
    let normalizedPath = modelPath;
    if (!normalizedPath.startsWith('textures/')) {
      normalizedPath = `textures/block/${modelPath}`;
    }

    // Get asset server URL
    const assetServerUrl = textureAtlas.getAssetServerUrl();
    const fullModelUrl = `${assetServerUrl}/${normalizedPath}`;

    console.log(`[ModelRenderer] Model path: ${modelPath} -> normalized: ${normalizedPath}`);
    console.log(`[ModelRenderer] Loading model from: ${fullModelUrl}`);

    // Get scaling from modifier (default 1.0)
    const scaleX = modifier?.scale?.[0] ?? 1.0;
    const scaleY = modifier?.scale?.[1] ?? 1.0;
    const scaleZ = modifier?.scale?.[2] ?? 1.0;

    // Get position offset from edgeOffsets (first 3 values: XYZ)
    let offsetX = 0;
    let offsetY = 0;
    let offsetZ = 0;

    if (edgeOffsets) {
      offsetX = edgeOffsets[0] / 127.0;
      offsetY = edgeOffsets[1] / 127.0;
      offsetZ = edgeOffsets[2] / 127.0;
    }

    // Calculate final position (block center + offset)
    const posX = x + 0.5 + offsetX;
    const posY = y + 0.5 + offsetY;
    const posZ = z + 0.5 + offsetZ;

    try {
      // Load or get cached model
      let modelMesh: Mesh;

      if (ModelRenderer.modelCache.has(modelPath)) {
        // Clone cached model
        const cachedMesh = ModelRenderer.modelCache.get(modelPath)!;
        modelMesh = cachedMesh.clone(`model_${x}_${y}_${z}`, null)!;
      } else {
        // Load new model
        console.log(`[ModelRenderer] Loading model from asset server: ${fullModelUrl}`);

        // Parse full URL to get root path and filename
        // fullModelUrl format: "http://localhost:3001/models/skull.babylon"
        const lastSlashIndex = fullModelUrl.lastIndexOf('/');
        const rootUrl = lastSlashIndex !== -1 ? fullModelUrl.substring(0, lastSlashIndex + 1) : fullModelUrl;
        const filename = lastSlashIndex !== -1 ? fullModelUrl.substring(lastSlashIndex + 1) : 'model.babylon';

        console.log(`[ModelRenderer] SceneLoader params - rootUrl: ${rootUrl}, filename: ${filename}`);

        // Load the model
        const result = await SceneLoader.ImportMeshAsync(
          '', // Load all meshes
          rootUrl,
          filename,
          scene
        );

        if (!result.meshes || result.meshes.length === 0) {
          console.error(`[ModelRenderer] No meshes found in model: ${fullModelUrl}`);
          console.error(`[ModelRenderer] Make sure the model file exists at the specified path in the assets directory`);
          return 0;
        }

        console.log(`[ModelRenderer] Successfully loaded ${result.meshes.length} meshes from ${modelPath}`);

        // Get root mesh (first mesh is usually the root)
        const rootMesh = result.meshes[0];

        // Convert to Mesh if needed
        if (!(rootMesh instanceof Mesh)) {
          console.error(`[ModelRenderer] Root mesh is not a Mesh instance`);
          return 0;
        }

        // Merge all meshes into one for better performance
        const allMeshes = result.meshes.filter(m => m instanceof Mesh) as Mesh[];
        if (allMeshes.length > 1) {
          console.log(`[ModelRenderer] Merging ${allMeshes.length} meshes into one`);
          const merged = Mesh.MergeMeshes(allMeshes, true, true, undefined, false, true);
          if (!merged) {
            console.error(`[ModelRenderer] Failed to merge meshes`);
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
        console.log(`[ModelRenderer] Cached model: ${modelPath}`);

        // Clone for this instance
        modelMesh = modelMesh.clone(`model_${x}_${y}_${z}`, null)!;
        modelMesh.setEnabled(true); // Enable the clone
      }

      // Calculate bounding box to determine automatic scaling
      modelMesh.computeWorldMatrix(true);
      const boundingInfo = modelMesh.getBoundingInfo();
      const boundingBox = boundingInfo.boundingBox;
      const modelSize = boundingBox.extendSize.scale(2); // extendSize is half the size

      console.log(`[ModelRenderer] Model size: ${modelSize.x}, ${modelSize.y}, ${modelSize.z}`);

      // Calculate automatic scale factor to fit model into 1 block (1 unit = 1 block)
      // Use the largest dimension to ensure model fits within block
      const maxDimension = Math.max(modelSize.x, modelSize.y, modelSize.z);
      const autoScale = maxDimension > 0 ? 1.0 / maxDimension : 1.0;

      console.log(`[ModelRenderer] Auto scale factor: ${autoScale} (max dimension: ${maxDimension})`);

      // Apply user scaling on top of automatic scaling
      const finalScaleX = scaleX * autoScale;
      const finalScaleY = scaleY * autoScale;
      const finalScaleZ = scaleZ * autoScale;

      // Position the model
      modelMesh.position = new Vector3(posX, posY, posZ);

      // Apply final scaling
      modelMesh.scaling = new Vector3(finalScaleX, finalScaleY, finalScaleZ);

      console.log(`[ModelRenderer] Final scale: ${finalScaleX}, ${finalScaleY}, ${finalScaleZ}`);

      // Apply rotation from modifier
      if (modifier?.rotationX !== undefined) {
        modelMesh.rotation.x = (modifier.rotationX * Math.PI) / 180;
      }

      if (modifier?.rotationY !== undefined || modifier?.rotation !== undefined) {
        const rotY = modifier.rotationY ?? modifier.rotation;
        if (rotY !== undefined) {
          modelMesh.rotation.y = (rotY * Math.PI) / 180;
        }
      }

      // Apply rotation matrix if provided (from facing or other sources)
      if (rotationMatrix) {
        // Extract rotation from matrix and apply
        const rotation = rotationMatrix.decompose();
        if (rotation && rotation[1]) { // rotation[1] is the quaternion
          modelMesh.rotationQuaternion = rotation[1];
        }
      }

      // Apply material from MaterialManager if available
      if (materialManager) {
        const materialConfig = materialManager.getMaterialConfig(block, modifier);
        const material = materialManager.getMaterialByType(
          materialConfig.key as 'solid' | 'transparent' | 'transparent_wind' | 'water' | 'lava'
        );
        modelMesh.material = material;
      }

      // Ensure mesh is visible
      modelMesh.isVisible = true;
      modelMesh.visibility = 1.0;

      // Add to separate meshes array
      separateMeshes.push(modelMesh);

      console.log(`[ModelRenderer] Successfully rendered model at ${x},${y},${z}`);

      // Return 0 vertices since we're not adding to the chunk mesh
      return 0;

    } catch (error) {
      console.error(`[ModelRenderer] Failed to load model ${modelPath}:`, error);
      console.error(`[ModelRenderer] Full URL: ${fullModelUrl}`);
      console.error(`[ModelRenderer] Error details:`, {
        message: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined
      });

      // Suggestions for common issues
      console.error(`[ModelRenderer] Common issues:`);
      console.error(`  1. Make sure the model file exists in the assets directory on the server`);
      console.error(`  2. Check that the file path is correct (e.g., "models/skull.babylon")`);
      console.error(`  3. Verify the model file format is valid .babylon JSON`);
      console.error(`  4. Check AssetServer is serving files correctly at ${assetServerUrl}`);

      return 0;
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
  }
}
