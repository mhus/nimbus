/**
 * ShadowsSceneDebugCommand - List all meshes in the scene
 *
 * Usage: shadowsSceneDebug
 * Shows all meshes with their positions, visibility, and rendering properties
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('ShadowsSceneDebugCommand');

export class ShadowsSceneDebugCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'shadowsSceneDebug';
  }

  description(): string {
    return 'List all meshes in scene with their properties';
  }

  async execute(parameters: any[]): Promise<string> {
    const engineService = this.appContext.services.engine;
    const playerService = this.appContext.services.player;

    if (!engineService) {
      return 'Engine service not available';
    }

    const scene = engineService.getScene();
    if (!scene) {
      return 'Scene not available';
    }

    const playerPos = playerService?.getPosition();
    const allMeshes = scene.meshes;

    // Filter for test meshes
    const testMeshes = allMeshes.filter(m =>
      m.name.includes('shadowCube') ||
      m.name.includes('simpleTest') ||
      m.name.includes('shadowMap')
    );

    let output = `Scene Debug Info
================

Total meshes: ${allMeshes.length}
Test meshes: ${testMeshes.length}

Player position: ${playerPos ? `(${playerPos.x.toFixed(1)}, ${playerPos.y.toFixed(1)}, ${playerPos.z.toFixed(1)})` : 'unknown'}

TEST MESHES:
`;

    if (testMeshes.length === 0) {
      output += '  (none found)\n';
    } else {
      for (const mesh of testMeshes) {
        const pos = mesh.position;
        const distance = playerPos ?
          Math.sqrt(
            Math.pow(pos.x - playerPos.x, 2) +
            Math.pow(pos.y - playerPos.y, 2) +
            Math.pow(pos.z - playerPos.z, 2)
          ) : 0;

        output += `
  ${mesh.name}:
    Position: (${pos.x.toFixed(1)}, ${pos.y.toFixed(1)}, ${pos.z.toFixed(1)})
    Distance from player: ${distance.toFixed(1)}
    Visible: ${mesh.isVisible}
    Enabled: ${mesh.isEnabled()}
    Pickable: ${mesh.isPickable}
    Rendering group: ${mesh.renderingGroupId}
    Has material: ${mesh.material ? 'YES' : 'NO'}
    Material name: ${mesh.material?.name || 'none'}
    Receives shadows: ${mesh.receiveShadows}
    Rotation: (${mesh.rotation.x.toFixed(2)}, ${mesh.rotation.y.toFixed(2)}, ${mesh.rotation.z.toFixed(2)})
    Scaling: (${mesh.scaling.x.toFixed(2)}, ${mesh.scaling.y.toFixed(2)}, ${mesh.scaling.z.toFixed(2)})
`;
      }
    }

    // Check shadow generator
    const envService = this.appContext.services.environment;
    if (envService && envService.getShadowGenerator) {
      const shadowGen = envService.getShadowGenerator();
      if (shadowGen) {
        const shadowMap = shadowGen.getShadowMap();
        const renderList = shadowMap?.renderList || [];

        output += `\nSHADOW CASTERS IN RENDER LIST:
`;
        if (renderList.length === 0) {
          output += '  (none)\n';
        } else {
          for (const mesh of renderList) {
            output += `  - ${mesh.name}\n`;
          }
        }
      }
    }

    logger.info('Scene debug info', { testMeshCount: testMeshes.length });

    return output;
  }
}
