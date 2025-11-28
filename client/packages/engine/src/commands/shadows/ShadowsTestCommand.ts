/**
 * ShadowsTestCommand - Create test objects to verify shadows are working
 *
 * Usage: shadowsTest
 * Creates a large test sphere that should cast a visible shadow
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger } from '@nimbus/shared';
import { MeshBuilder, StandardMaterial, Color3, Vector3 } from '@babylonjs/core';

const logger = getLogger('ShadowsTestCommand');

export class ShadowsTestCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'shadowsTest';
  }

  description(): string {
    return 'Create test sphere to verify shadows are working';
  }

  async execute(parameters: any[]): Promise<string> {
    const envService = this.appContext.services.environment;
    const engineService = this.appContext.services.engine;

    if (!envService || !engineService) {
      return 'Services not available';
    }

    const shadowGen = envService.getShadowGenerator();
    if (!shadowGen) {
      return 'Shadow generator not initialized. Enable shadows first: shadowsEnable true';
    }

    const scene = engineService.getScene();
    if (!scene) {
      return 'Scene not available';
    }

    // Check if test objects already exist and remove them
    scene.getMeshByName('shadowTestSphere')?.dispose();
    scene.getMeshByName('shadowTestGround')?.dispose();

    // Get player position
    const playerService = this.appContext.services.player;
    const playerPos = playerService?.getPosition() || new Vector3(9, 74, 0);

    // CREATE GROUND PLANE FIRST - white, receives shadows
    const ground = MeshBuilder.CreateGround('shadowTestGround', {
      width: 20,
      height: 20,
      subdivisions: 4,
    }, scene);

    ground.position = new Vector3(
      playerPos.x,
      playerPos.y - 8, // 8 blocks below player
      playerPos.z
    );

    const groundMat = new StandardMaterial('shadowTestGroundMat', scene);
    groundMat.diffuseColor = new Color3(1, 1, 1); // Pure white
    groundMat.specularColor = new Color3(0.2, 0.2, 0.2);
    groundMat.backFaceCulling = true;
    ground.material = groundMat;
    ground.receiveShadows = true; // CRITICAL: Receive shadows
    ground.renderingGroupId = 2;

    // CREATE SPHERE - red, casts shadows, floating above ground
    const sphere = MeshBuilder.CreateSphere('shadowTestSphere', {
      diameter: 6,
      segments: 16,
    }, scene);

    // Position between player and ground
    sphere.position = new Vector3(
      playerPos.x,
      playerPos.y - 2, // 2 blocks below player (above ground)
      playerPos.z
    );

    const sphereMat = new StandardMaterial('shadowTestSphereMat', scene);
    sphereMat.diffuseColor = new Color3(1, 0, 0); // Red
    sphereMat.emissiveColor = new Color3(0.8, 0, 0); // Glowing red
    sphereMat.specularColor = new Color3(1, 1, 1);
    sphereMat.backFaceCulling = true;
    sphere.material = sphereMat;
    sphere.receiveShadows = true;
    sphere.renderingGroupId = 2;

    // CRITICAL: Add to renderList with .push() like in working example!
    const shadowMap = shadowGen.getShadowMap();
    if (shadowMap && shadowMap.renderList) {
      // Clear and re-add to ensure it's fresh
      const index = shadowMap.renderList.indexOf(sphere);
      if (index === -1) {
        shadowMap.renderList.push(sphere);
      }
    }

    logger.info('Shadow test objects created', {
      spherePos: sphere.position,
      groundPos: ground.position,
      groundReceives: ground.receiveShadows,
      sphereInRenderList: shadowMap?.renderList?.includes(sphere),
      renderListLength: shadowMap?.renderList?.length || 0,
    });

    return `Shadow test scene created!

GROUND (white plane):
  Position: (${ground.position.x.toFixed(1)}, ${ground.position.y.toFixed(1)}, ${ground.position.z.toFixed(1)})
  Size: 20x20 blocks
  receiveShadows: ${ground.receiveShadows}

SPHERE (red):
  Position: (${sphere.position.x.toFixed(1)}, ${sphere.position.y.toFixed(1)}, ${sphere.position.z.toFixed(1)})
  Diameter: 6 blocks
  Casts shadow: ${shadowGen.getShadowMap()?.renderList?.includes(sphere)}

Look down! You should see:
- White ground plane below you
- Red glowing sphere between you and ground
- DARK SHADOW of sphere on white ground

If no shadow → fundamental BabylonJS issue!`;
  }
}
