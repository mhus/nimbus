/**
 * ShadowsMarkCasterCommand - Mark specific chunks/meshes to cast shadows
 *
 * Usage: shadowsMarkCaster <chunkX> <chunkZ> <true|false>
 * Marks all meshes in a chunk to cast shadows or not
 */

import { CommandHandler } from '../CommandHandler';
import type { AppContext } from '../../AppContext';
import { getLogger, toNumber, toBoolean } from '@nimbus/shared';

const logger = getLogger('ShadowsMarkCasterCommand');

export class ShadowsMarkCasterCommand extends CommandHandler {
  constructor(private appContext: AppContext) {
    super();
  }

  name(): string {
    return 'shadowsMarkCaster';
  }

  description(): string {
    return 'Mark chunk meshes to cast shadows (cx, cz, enabled)';
  }

  async execute(parameters: any[]): Promise<string> {
    if (parameters.length < 3) {
      return 'Usage: shadowsMarkCaster <chunkX> <chunkZ> <true|false>';
    }

    const cx = toNumber(parameters[0]);
    const cz = toNumber(parameters[1]);
    const enabled = toBoolean(parameters[2]);

    const renderService = this.appContext.services.render;
    if (!renderService) {
      return 'RenderService not available';
    }

    const chunkMeshes = (renderService as any).chunkMeshes;
    if (!chunkMeshes) {
      return 'No chunk meshes available';
    }

    const chunkKey = `chunk_${cx}_${cz}`;
    const meshMap = chunkMeshes.get(chunkKey);

    if (!meshMap) {
      return `Chunk ${cx},${cz} not found (not loaded or wrong coordinates)`;
    }

    // Mark all meshes in this chunk
    for (const mesh of meshMap.values()) {
      if (!mesh.metadata) {
        mesh.metadata = {};
      }
      mesh.metadata.castsShadows = enabled;
    }

    logger.info('Marked chunk for shadow casting', {
      cx,
      cz,
      enabled,
      meshCount: meshMap.size,
    });

    return `Chunk ${cx},${cz}: Shadow casting ${enabled ? 'ENABLED' : 'DISABLED'} for ${meshMap.size} meshes.

Now run shadowsRefresh or reload chunks to apply.`;
  }
}
