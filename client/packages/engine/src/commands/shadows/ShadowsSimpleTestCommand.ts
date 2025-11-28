/**
 * ShadowsSimpleTestCommand - Ultra-simple isolated shadow test
 *
 * Usage: shadowsSimpleTest
 * Creates a minimal test scene: 1 cube + 1 ground, both with fresh StandardMaterial
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger } from '@nimbus/shared';
import { MeshBuilder, StandardMaterial, Color3, Vector3 } from '@babylonjs/core';

const logger = getLogger('ShadowsSimpleTestCommand');

export class ShadowsSimpleTestCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'shadowsSimpleTest';
  }

  description(): string {
    return 'Ultra-simple shadow test (1 box + 1 ground)';
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

    // Clean up existing test objects
    scene.getMeshByName('simpleTestBox')?.dispose();
    scene.getMeshByName('simpleTestGround')?.dispose();

    const playerService = this.appContext.services.player;
    const playerPos = playerService?.getPosition() || new Vector3(0, 70, 0);

    // Create GROUND PLANE - pure white, receives shadows
    const ground = MeshBuilder.CreateGround('simpleTestGround', {
      width: 20,
      height: 20,
    }, scene);

    ground.position = new Vector3(playerPos.x, playerPos.y - 5, playerPos.z);

    const groundMat = new StandardMaterial('simpleGroundMat', scene);
    groundMat.diffuseColor = new Color3(1, 1, 1); // Pure white
    groundMat.specularColor = new Color3(0.1, 0.1, 0.1);
    groundMat.backFaceCulling = true;
    ground.material = groundMat;
    ground.receiveShadows = true; // RECEIVE SHADOWS
    ground.renderingGroupId = 2;

    // Create BOX - red, casts shadows
    const box = MeshBuilder.CreateBox('simpleTestBox', {
      size: 3,
    }, scene);

    box.position = new Vector3(
      playerPos.x,
      playerPos.y + 2, // 2 blocks above player
      playerPos.z
    );

    const boxMat = new StandardMaterial('simpleBoxMat', scene);
    boxMat.diffuseColor = new Color3(1, 0, 0); // Red
    boxMat.emissiveColor = new Color3(0.5, 0, 0); // Slightly glowing
    boxMat.backFaceCulling = true;
    box.material = boxMat;
    shadowGen.addShadowCaster(box); // CAST SHADOW
    box.renderingGroupId = 2;

    logger.info('Simple shadow test created', {
      groundPos: ground.position,
      boxPos: box.position,
      groundReceives: ground.receiveShadows,
      boxInRenderList: shadowGen.getShadowMap()?.renderList?.includes(box),
    });

    return `Simple shadow test created!

GROUND (white):
  Position: (${ground.position.x.toFixed(1)}, ${ground.position.y.toFixed(1)}, ${ground.position.z.toFixed(1)})
  receiveShadows: ${ground.receiveShadows}
  Material: StandardMaterial (white)

BOX (red):
  Position: (${box.position.x.toFixed(1)}, ${box.position.y.toFixed(1)}, ${box.position.z.toFixed(1)})
  In shadow caster list: ${shadowGen.getShadowMap()?.renderList?.includes(box)}
  Material: StandardMaterial (red)

You should see:
- Red box floating above you
- White ground plane below
- DARK SHADOW of box on white ground

If still no shadow, there's a fundamental BabylonJS config issue.`;
  }
}
