/**
 * ShadowsSimpleTestCommand - Isolated shadow test with separate light + shadow generator
 *
 * Usage: shadowsSimpleTest
 * Creates a completely isolated shadow system:
 * - Own DirectionalLight
 * - Own ShadowGenerator
 * - 1 sphere (red) casting shadows
 * - 6 white walls receiving shadows
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger } from '@nimbus/shared';
import {
  MeshBuilder,
  StandardMaterial,
  Color3,
  Vector3,
} from '@babylonjs/core';

const logger = getLogger('ShadowsSimpleTestCommand');

export class ShadowsSimpleTestCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'shadowsSimpleTest';
  }

  description(): string {
    return 'Shadow test with sphere inside cube (6 walls receive shadows)';
  }

  async execute(parameters: any[]): Promise<string> {
    const engineService = this.appContext.services.engine;

    if (!engineService) {
      return 'Engine service not available';
    }

    const scene = engineService.getScene();
    if (!scene) {
      return 'Scene not available';
    }

    // Clean up old test objects
    scene.getMeshByName('shadowTestGround')?.dispose();
    scene.getMeshByName('shadowTestBox')?.dispose();
    scene.getLightByName('shadowTestLight')?.dispose();

    // USE EXISTING ENVIRONMENTSERVICE SHADOW GENERATOR
    const envService = this.appContext.services.environment;
    if (!envService) {
      return 'EnvironmentService not available';
    }

    const shadowGenerator = envService.getShadowGenerator();
    if (!shadowGenerator) {
      return 'Shadow generator not initialized. Run: shadowsEnable true';
    }

    // REDUCE AMBIENT LIGHT to make shadows visible
    const allLights = scene.lights;
    for (const light of allLights) {
      if (light.name === 'ambientLight') {
        (light as any).intensity = 0.1; // Very low
        logger.info('Reduced ambientLight intensity for shadow visibility');
      }
    }

    const playerService = this.appContext.services.player;
    const playerPos = playerService?.getPosition() || new Vector3(0, 70, 0);

    const groundSize = 100;
    const boxWidth = 5;
    const boxHeight = 20;
    const boxDepth = 5;
    const groundY = playerPos.y - 20;
    const boxPos = new Vector3(playerPos.x, playerPos.y, playerPos.z + 15);

    logger.info('Using EnvironmentService shadow generator', {
      type: shadowGenerator.constructor.name,
    });

    // ===== CREATE GROUND (receives shadows) =====
    // EXACTLY like working example: large ground BELOW the sphere
    const ground = MeshBuilder.CreateGround('shadowTestGround', {
      width: groundSize,
      height: groundSize,
    }, scene);

    ground.position.y = groundY;
    ground.position.x = playerPos.x;
    ground.position.z = playerPos.z + 15;
    ground.receiveShadows = true; // RECEIVE SHADOWS

    const groundMat = new StandardMaterial('shadowTestGroundMat', scene);
    groundMat.diffuseColor = new Color3(0.8, 0.8, 0.8); // Light gray
    groundMat.specularColor = new Color3(0.1, 0.1, 0.1);
    ground.material = groundMat;
    ground.renderingGroupId = 2;

    // ===== CREATE BOX ABOVE GROUND (casts shadows) =====
    // Like in working example: tall box (pole)
    const box = MeshBuilder.CreateBox('shadowTestBox', {
      width: boxWidth,
      height: boxHeight,
      depth: boxDepth,
    }, scene);

    box.position = boxPos.clone();

    const boxMat = new StandardMaterial('shadowTestBoxMat', scene);
    boxMat.diffuseColor = new Color3(0.6, 0.4, 0.2); // Brown wood-like
    boxMat.specularColor = new Color3(0.1, 0.1, 0.1);
    box.material = boxMat;

    // Use the EXACT method from working example: push to renderList
    shadowGenerator.getShadowMap()!.renderList!.push(box);
    box.receiveShadows = false; // Casters don't receive their own shadows
    box.renderingGroupId = 2;

    // VOXEL CHUNKS: Register based on metadata property
    let voxelReceiverCount = 0;
    let voxelCasterCount = 0;
    const renderService = this.appContext.services.render;
    if (renderService) {
      const chunkMeshes = (renderService as any).chunkMeshes;
      if (chunkMeshes) {
        for (const meshMap of chunkMeshes.values()) {
          for (const mesh of meshMap.values()) {
            mesh.receiveShadows = true;  // All voxels receive
            voxelReceiverCount++;

            // Check if mesh should cast shadows (via metadata)
            if ((mesh as any).castsShadows === true) {
              shadowGenerator.getShadowMap()!.renderList!.push(mesh);
              voxelCasterCount++;
            }
          }
        }
        logger.info('Voxel chunks shadow registration', {
          receivers: voxelReceiverCount,
          casters: voxelCasterCount,
        });
      }
    }

    // Call splitFrustum if it's a CascadedShadowGenerator
    if ((shadowGenerator as any).splitFrustum) {
      (shadowGenerator as any).splitFrustum();
      logger.info('Called splitFrustum on CascadedShadowGenerator');
    }

    logger.info('Shadow test created using EnvironmentService generator', {
      groundPos: ground.position,
      boxPos: box.position,
      shadowCasters: shadowGenerator.getShadowMap()?.renderList?.length || 0,
      voxelReceivers: voxelReceiverCount,
      voxelCasters: voxelCasterCount,
    });

    return `SHADOW TEST (using EnvironmentService generator)!

SHADOW GENERATOR:
  Type: ${shadowGenerator.constructor.name}
  Total Casters: ${shadowGenerator.getShadowMap()?.renderList?.length || 0}
  Voxel Receivers: ${voxelReceiverCount}
  Voxel Casters: ${voxelCasterCount}

GROUND (gray, receives shadows):
  Position: (${ground.position.x.toFixed(1)}, ${ground.position.y.toFixed(1)}, ${ground.position.z.toFixed(1)})
  Size: ${groundSize}x${groundSize}

BOX/POLE (brown, casts shadows):
  Position: (${box.position.x.toFixed(1)}, ${box.position.y.toFixed(1)}, ${box.position.z.toFixed(1)})
  Size: ${boxWidth}x${boxHeight}x${boxDepth}

AmbientLight reduced to 0.1 for shadow visibility.
Look DOWN to see shadows!`;
  }
}
