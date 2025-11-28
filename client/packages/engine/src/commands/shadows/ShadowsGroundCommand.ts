/**
 * ShadowsGroundCommand - Create a simple ground plane to receive shadows
 *
 * Usage: shadowsGround
 * Creates a large white ground plane that should receive shadows
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger } from '@nimbus/shared';
import { MeshBuilder, StandardMaterial, Color3, Vector3 } from '@babylonjs/core';

const logger = getLogger('ShadowsGroundCommand');

export class ShadowsGroundCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'shadowsGround';
  }

  description(): string {
    return 'Create test ground plane to receive shadows';
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

    // Remove existing ground if present
    const existingGround = scene.getMeshByName('shadowTestGround');
    if (existingGround) {
      existingGround.dispose();
    }

    // Get player position
    const playerService = this.appContext.services.player;
    const playerPos = playerService?.getPosition() || new Vector3(0, 70, 0);

    // Create a LARGE ground plane below player
    const ground = MeshBuilder.CreateGround('shadowTestGround', {
      width: 40,
      height: 40,
      subdivisions: 4,
    }, scene);

    // Position at player Y minus 10
    ground.position = new Vector3(
      playerPos.x,
      playerPos.y - 10, // 10 blocks below player
      playerPos.z
    );

    // Create BRIGHT WHITE material with lighting enabled
    const material = new StandardMaterial('shadowTestGroundMaterial', scene);
    material.diffuseColor = new Color3(1, 1, 1); // White
    material.specularColor = new Color3(0.5, 0.5, 0.5);
    material.disableLighting = false; // IMPORTANT: lighting must be enabled for shadows!
    ground.material = material;

    // Set rendering group
    ground.renderingGroupId = 2; // RENDERING_GROUPS.WORLD

    // CRITICAL: Enable shadow receiving
    ground.receiveShadows = true;

    logger.info('Shadow test ground created', {
      position: ground.position,
      size: '40x40',
      receiveShadows: ground.receiveShadows,
      materialLighting: !material.disableLighting,
    });

    return `Shadow test ground plane created!
  Position: (${ground.position.x.toFixed(1)}, ${ground.position.y.toFixed(1)}, ${ground.position.z.toFixed(1)})
  Size: 40x40 blocks
  Color: WHITE
  receiveShadows: ${ground.receiveShadows}
  Material lighting: ${!material.disableLighting}

Now run: shadowsTest
The red sphere should cast a DARK shadow on the white ground!`;
  }
}
