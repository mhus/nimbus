/**
 * ShadowsSystemDebugCommand - Check if shadows are blocked system-wide
 *
 * Usage: shadowsSystemDebug
 * Checks scene settings, engine settings, and other factors that might prevent shadows
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('ShadowsSystemDebugCommand');

export class ShadowsSystemDebugCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'shadowsSystemDebug';
  }

  description(): string {
    return 'Check for system-wide shadow blockers';
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

    const engine = engineService.getEngine();
    if (!engine) {
      return 'Engine not available';
    }

    // Check all scene properties that might affect shadows
    const sceneChecks = {
      'shadowsEnabled': (scene as any).shadowsEnabled,
      'postProcessesEnabled': scene.postProcessesEnabled,
      'renderTargetsEnabled': scene.renderTargetsEnabled,
      'probesEnabled': scene.probesEnabled,
      'texturesEnabled': scene.texturesEnabled,
      'lightsEnabled': scene.lightsEnabled,
      'particlesEnabled': scene.particlesEnabled,
      'spritesEnabled': scene.spritesEnabled,
      'lensFlaresEnabled': scene.lensFlaresEnabled,
      'proceduralTexturesEnabled': scene.proceduralTexturesEnabled,
      'constantlyUpdateMeshUnderPointer': scene.constantlyUpdateMeshUnderPointer,
    };

    // Check engine capabilities
    const engineChecks = {
      'webGLVersion': engine.webGLVersion,
      'isWebGL2': engine.isWebGL2,
      'hardwareScalingLevel': engine.getHardwareScalingLevel(),
      'isStencilEnable': engine.isStencilEnable,
    };

    // Check all lights in scene
    const lights = scene.lights;
    const lightInfo = lights.map(light => ({
      name: light.name,
      type: light.getClassName(),
      enabled: light.isEnabled(),
      intensity: light.intensity,
      shadowEnabled: (light as any).shadowEnabled,
    }));

    // Check for render targets
    const renderTargets = scene.customRenderTargets;

    let output = `SHADOW SYSTEM DEBUG
===================

SCENE SETTINGS:
`;

    for (const [key, value] of Object.entries(sceneChecks)) {
      const status = value === undefined ? 'undefined' :
                     value === true ? 'true' :
                     value === false ? 'FALSE ⚠️' :
                     String(value);
      output += `  ${key}: ${status}\n`;
    }

    output += `\nENGINE:
`;
    for (const [key, value] of Object.entries(engineChecks)) {
      output += `  ${key}: ${value}\n`;
    }

    output += `\nLIGHTS (${lights.length} total):
`;
    if (lights.length === 0) {
      output += '  (none)\n';
    } else {
      for (const info of lightInfo) {
        output += `  ${info.name} (${info.type}):\n`;
        output += `    enabled: ${info.enabled}\n`;
        output += `    intensity: ${info.intensity}\n`;
        output += `    shadowEnabled: ${info.shadowEnabled !== undefined ? info.shadowEnabled : 'undefined'}\n`;
      }
    }

    output += `\nCUSTOM RENDER TARGETS: ${renderTargets.length}
`;

    // Try to check if there's a global shadow disable flag
    const globalShadowCheck = (scene as any).shadowsEnabled;
    if (globalShadowCheck === false) {
      output += `\n⚠️  WARNING: scene.shadowsEnabled is FALSE!
This might be blocking all shadows in the scene.
Try: scene.shadowsEnabled = true
`;
    }

    logger.info('Shadow system debug', {
      sceneShadowsEnabled: sceneChecks.shadowsEnabled,
      webGL2: engine.isWebGL2,
      lightsCount: lights.length,
    });

    return output;
  }
}
