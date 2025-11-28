/**
 * ShadowsVisualizeCommand - Visualize the shadow map as a plane
 *
 * Usage: shadowsVisualize
 * Creates a plane that shows the shadow map texture
 * This helps debug if the shadow map is actually being rendered
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger } from '@nimbus/shared';
import { MeshBuilder, StandardMaterial, Vector3, Color3 } from '@babylonjs/core';

const logger = getLogger('ShadowsVisualizeCommand');

export class ShadowsVisualizeCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'shadowsVisualize';
  }

  description(): string {
    return 'Visualize shadow map texture on a plane';
  }

  async execute(parameters: any[]): Promise<string> {
    const envService = this.appContext.services.environment;
    const engineService = this.appContext.services.engine;

    if (!envService || !engineService) {
      return 'Services not available';
    }

    const shadowGen = envService.getShadowGenerator();
    if (!shadowGen) {
      return 'Shadow generator not initialized';
    }

    const scene = engineService.getScene();
    if (!scene) {
      return 'Scene not available';
    }

    // Get shadow map texture
    const shadowMap = shadowGen.getShadowMap();
    if (!shadowMap) {
      return 'Shadow map not available';
    }

    // Remove existing visualizer
    scene.getMeshByName('shadowMapVisualizer')?.dispose();

    // Get player position
    const playerService = this.appContext.services.player;
    const playerPos = playerService?.getPosition() || new Vector3(0, 70, 0);

    // Create plane to show shadow map
    const plane = MeshBuilder.CreatePlane('shadowMapVisualizer', {
      size: 10,
    }, scene);

    // Position in front of player
    plane.position = new Vector3(
      playerPos.x,
      playerPos.y,
      playerPos.z + 10
    );

    // Create BRIGHT GREEN material first (to verify plane is visible)
    const mat = new StandardMaterial('shadowMapVisualizerMat', scene);

    // Start with bright green to see if plane renders at all
    mat.diffuseColor = new Color3(0, 1, 0); // GREEN
    mat.emissiveColor = new Color3(0, 1, 0); // Emissive green (glows!)

    // ALSO try to show shadow map
    mat.diffuseTexture = shadowMap;
    mat.emissiveTexture = shadowMap;

    mat.disableLighting = true; // Don't apply lighting
    mat.backFaceCulling = false; // Show both sides
    plane.material = mat;
    plane.renderingGroupId = 4; // SELECTION_OVERLAY - render on top
    plane.isVisible = true;
    plane.isPickable = true;
    plane.billboardMode = 7; // Billboard to always face camera

    logger.info('Shadow map visualizer created', {
      position: plane.position,
      shadowMapSize: shadowMap.getSize(),
      renderListLength: shadowMap.renderList?.length || 0,
    });

    return `Shadow map visualizer created!

Position: (${plane.position.x.toFixed(1)}, ${plane.position.y.toFixed(1)}, ${plane.position.z.toFixed(1)})
Size: 10x10 blocks
Shows: Raw shadow map texture

You should see a plane in front of you showing the shadow map.
- If it's BLACK: Shadow map is not rendering
- If it shows WHITE/GRAY shapes: Shadow map IS rendering!
  (The shapes are the depth values from the light's perspective)

This tells us if the problem is:
a) Shadow map not rendering → ShadowGenerator issue
b) Shadow map rendering but not applied → Material/Shader issue`;
  }
}
