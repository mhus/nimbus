/**
 * Flame Renderer
 * Renders FLAME shaped blocks using separate Babylon.js meshes (vertical planes)
 * Creates animated flame effects with flickering and wind movement
 *
 * Block/Modifier Properties:
 * - edgeOffsets[0-2] (corner 0): Adjusts flame center position (X, Y, Z)
 * - scale: Flame size multiplier (default: [1, 1, 1])
 * - color: Flame color tint (default: orange [255, 200, 100])
 * - texture: Optional texture(s) for FireMaterial:
 *   - Single string: diffuse texture
 *   - Array[0]: diffuse texture (main flame texture)
 *   - Array[1]: distortion texture (optional)
 *   - Array[2]: opacity texture (optional)
 * - windStability: Flame strength/animation speed (default: 1.0)
 * - windLeafiness: Flicker intensity (default: 0.5)
 * - windLeverUp: Upward movement amplitude (default: 0.3)
 * - windLeverDown: Downward movement amplitude (default: 0.1)
 *
 * Material Selection:
 * - With texture: Uses Babylon.js FireMaterial for realistic animated flames
 * - Without texture: Uses StandardMaterial with emissive glow
 *
 * Important: Flames NEVER use WindShader, but use wind parameters for movement
 */

import { Mesh, MeshBuilder, StandardMaterial, Color3, Texture } from '@babylonjs/core';
import { FireMaterial } from '@babylonjs/materials';
import { ShapeRenderer } from './ShapeRenderer';
import type { BlockRenderContext } from './BlockRenderContext';

export class FlameRenderer extends ShapeRenderer {
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
    } = context;

    // FLAME blocks don't add geometry to chunk mesh
    // Instead, they create separate Mesh objects

    if (!scene) {
      console.warn('[FlameRenderer] No scene provided, cannot render flames');
      return 0;
    }

    // Get scale from modifier (default: [1, 1, 1])
    const scale = modifier?.scale || [1, 1, 1];

    // Get color from modifier (default: orange [255, 200, 100])
    const color = modifier?.color || [255, 200, 100];

    // Get center offset from edgeOffsets (corner 0: X, Y, Z)
    // Normalize from -127..128 to roughly -1..1
    const centerOffsetX = edgeOffsets ? edgeOffsets[0] / 127.0 : 0;
    const centerOffsetY = edgeOffsets ? edgeOffsets[1] / 127.0 : 0;
    const centerOffsetZ = edgeOffsets ? edgeOffsets[2] / 127.0 : 0;

    // Get wind/flame properties from modifier or block (modifier takes precedence)
    // windStability controls flame strength
    // windLeafiness controls flicker intensity
    const flameStrength = modifier?.windStability ?? block.windStability ?? 1.0;
    const flicker = modifier?.windLeafiness ?? block.windLeafiness ?? 0.5;
    const windLeverUp = modifier?.windLeverUp ?? block.windLeverUp ?? 0.3;
    const windLeverDown = modifier?.windLeverDown ?? block.windLeverDown ?? 0.1;

    // Create plane mesh (vertical quad)
    const plane = MeshBuilder.CreatePlane(
      `flame_${x}_${y}_${z}`,
      {
        width: scale[0],
        height: scale[1],
        sideOrientation: Mesh.DOUBLESIDE, // Visible from both sides
      },
      scene
    );

    // Position at block center + offsets
    plane.position.set(
      x + 0.5 + centerOffsetX,
      y + 0.5 + centerOffsetY,
      z + 0.5 + centerOffsetZ
    );

    // Enable billboard mode - rotate around Y axis to face camera
    plane.billboardMode = Mesh.BILLBOARDMODE_Y;

    // Get texture from modifier or block
    // Texture can be a string or array of strings
    const textureSource = modifier?.texture ?? block.texture;
    const hasTexture = textureSource && (Array.isArray(textureSource) ? textureSource[0] : textureSource);

    // Create material - prefer FireMaterial when textures are available
    let material: FireMaterial | StandardMaterial;

    if (hasTexture) {
      // Use FireMaterial with textures
      const fireMaterial = new FireMaterial(`flameMat_${x}_${y}_${z}`, scene);

      // Get texture paths array
      // Handle semicolon-separated texture paths (e.g., "fire/fire;fire/distortion;fire/candleopacity")
      let texturePaths: string[];
      if (Array.isArray(textureSource)) {
        texturePaths = textureSource;
      } else if (typeof textureSource === 'string' && textureSource.includes(';')) {
        // Split semicolon-separated paths
        texturePaths = textureSource.split(';');
      } else {
        texturePaths = [textureSource as string];
      }

      // Build proper texture URLs using the asset server
      const baseUrl = 'http://localhost:3004/assets/textures/block/';

      // Set diffuse texture (main flame texture)
      if (texturePaths[0]) {
        const textureUrl = baseUrl + texturePaths[0] + '.png';
        fireMaterial.diffuseTexture = new Texture(textureUrl, scene);
      }

      // Set distortion texture if second texture is provided
      if (texturePaths[1]) {
        const textureUrl = baseUrl + texturePaths[1] + '.png';
        fireMaterial.distortionTexture = new Texture(textureUrl, scene);
      }

      // Set opacity texture if third texture is provided
      if (texturePaths[2]) {
        const textureUrl = baseUrl + texturePaths[2] + '.png';
        fireMaterial.opacityTexture = new Texture(textureUrl, scene);
      }

      // Apply color tint to fire
      fireMaterial.fireColors = [
        new Color3(color[0] / 255.0, color[1] / 255.0, color[2] / 255.0), // Main color
        new Color3(1.0, 0.8, 0.4), // Secondary color (yellowish)
        new Color3(0.8, 0.2, 0.0), // Dark orange
      ];

      // Use wind parameters to control fire animation
      fireMaterial.speed = flameStrength * 2.0; // Flame strength affects animation speed

      material = fireMaterial;
    } else {
      // Fallback to simple emissive material when no texture
      const standardMaterial = new StandardMaterial(`flameMat_${x}_${y}_${z}`, scene);

      // Convert color from 0-255 to 0.0-1.0
      standardMaterial.emissiveColor = new Color3(
        color[0] / 255.0,
        color[1] / 255.0,
        color[2] / 255.0
      );

      // Make flame semi-transparent
      standardMaterial.alpha = 0.8;
      standardMaterial.useAlphaFromDiffuseTexture = false;

      material = standardMaterial;
    }

    // Common material properties
    material.backFaceCulling = false; // Visible from both sides

    // Store wind/flame parameters in material metadata for later shader/animation access
    // These will be used for additional flickering and movement effects
    material.metadata = {
      flameStrength,
      flicker,
      windLeverUp,
      windLeverDown,
      startTime: Date.now(),
    };

    plane.material = material;
    plane.isPickable = false; // Optimization: flames don't need picking

    // Add to separate meshes array for chunk management
    separateMeshes.push(plane);

    // Return 0 vertices (flames don't contribute to chunk mesh geometry)
    return 0;
  }
}
