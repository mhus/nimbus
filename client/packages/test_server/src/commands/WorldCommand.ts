/**
 * WorldCommand - Manage world properties
 *
 * Commands:
 * - /world status <value> - Set world status
 * - /world season <seasonStatus> - Set season status
 * - /world seasonProgress <value> - Set season progress (0.0-1.0)
 * - /world info - Show current world info
 *
 * When status or season changes, broadcasts 'reloadWorldConfig' to all clients
 */

import { CommandHandler, type CommandContext, type CommandResult } from './CommandHandler';
import type { WorldManager } from '../world/WorldManager';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('WorldCommand');

type BroadcastCallback = (worldId: string | null, cmd: string, args?: any[]) => void;

export class WorldCommand extends CommandHandler {
  private readonly usage = '/world <status|season|seasonProgress|info> [args...]';

  constructor(
    private worldManager: WorldManager,
    private broadcastCommand: BroadcastCallback
  ) {
    super();
  }

  name(): string {
    return 'world';
  }

  description(): string {
    return 'Manage world properties (status, season)';
  }

  async execute(context: CommandContext, args: any[]): Promise<CommandResult> {
    if (args.length === 0) {
      return {
        rc: 1,
        message: `Usage: ${this.usage}\n\nSubcommands:\n  status <value> - Set world status\n  season <seasonStatus> - Set season status\n  seasonProgress <value> - Set season progress (0.0-1.0)\n  info - Show current world info`,
      };
    }

    const subcommand = String(args[0]).toLowerCase();

    switch (subcommand) {
      case 'status':
        return this.handleStatus(args.slice(1), context);
      case 'season':
        return this.handleSeason(args.slice(1), context);
      case 'seasonprogress':
        return this.handleSeasonProgress(args.slice(1), context);
      case 'info':
        return this.handleInfo(context);
      default:
        return {
          rc: 1,
          message: `Unknown subcommand: ${subcommand}\nUse /world for help`,
        };
    }
  }

  /**
   * Handle /world status <value>
   */
  private async handleStatus(args: any[], context: CommandContext): Promise<CommandResult> {
    if (args.length < 1) {
      return {
        rc: 1,
        message: 'Usage: /world status <value>',
      };
    }

    const worldId = context.session.worldId;
    if (!worldId) {
      return {
        rc: 1,
        message: 'Not in a world',
      };
    }

    const status = parseInt(String(args[0]), 10);

    if (isNaN(status)) {
      return {
        rc: 1,
        message: 'Invalid status. Must be an integer.',
      };
    }

    try {
      // Update WorldInfo
      await this.worldManager.updateWorldInfo(worldId, { status });

      // Broadcast reload command to all clients in this world
      this.broadcastCommand(worldId, 'reloadWorldConfig');

      logger.info('World status updated and broadcast to clients', { worldId, status });

      return {
        rc: 0,
        message: `World status set to ${status}. Clients are reloading configuration.`,
      };
    } catch (error) {
      logger.error('Failed to update world status', { worldId, status }, error as Error);
      return {
        rc: -1,
        message: `Failed to update world status: ${(error as Error).message}`,
      };
    }
  }

  /**
   * Handle /world season <seasonStatus>
   */
  private async handleSeason(args: any[], context: CommandContext): Promise<CommandResult> {
    if (args.length < 1) {
      return {
        rc: 1,
        message: 'Usage: /world season <seasonStatus>\nExamples: spring, summer, autumn, winter',
      };
    }

    const worldId = context.session.worldId;
    if (!worldId) {
      return {
        rc: 1,
        message: 'Not in a world',
      };
    }

    const seasonStatus = Number(args[0]);

    try {
      // Update WorldInfo
      await this.worldManager.updateWorldInfo(worldId, { seasonStatus });

      // Broadcast reload command to all clients in this world
      this.broadcastCommand(worldId, 'reloadWorldConfig');

      logger.info('World season updated and broadcast to clients', { worldId, seasonStatus });

      return {
        rc: 0,
        message: `World season set to "${seasonStatus}". Clients are reloading configuration.`,
      };
    } catch (error) {
      logger.error('Failed to update world season', { worldId, seasonStatus }, error as Error);
      return {
        rc: -1,
        message: `Failed to update world season: ${(error as Error).message}`,
      };
    }
  }

  /**
   * Handle /world seasonProgress <value>
   */
  private async handleSeasonProgress(args: any[], context: CommandContext): Promise<CommandResult> {
    if (args.length < 1) {
      return {
        rc: 1,
        message: 'Usage: /world seasonProgress <value>\nValue must be between 0.0 and 1.0',
      };
    }

    const worldId = context.session.worldId;
    if (!worldId) {
      return {
        rc: 1,
        message: 'Not in a world',
      };
    }

    const seasonProgress = parseFloat(String(args[0]));

    if (isNaN(seasonProgress) || seasonProgress < 0 || seasonProgress > 1) {
      return {
        rc: 1,
        message: 'Invalid season progress. Must be a number between 0.0 and 1.0.',
      };
    }

    try {
      // Update WorldInfo
      await this.worldManager.updateWorldInfo(worldId, { seasonProgress });

      // Broadcast reload command to all clients in this world
      this.broadcastCommand(worldId, 'reloadWorldConfig');

      logger.info('World season progress updated and broadcast to clients', {
        worldId,
        seasonProgress,
      });

      return {
        rc: 0,
        message: `World season progress set to ${seasonProgress}. Clients are reloading configuration.`,
      };
    } catch (error) {
      logger.error('Failed to update world season progress', { worldId, seasonProgress }, error as Error);
      return {
        rc: -1,
        message: `Failed to update world season progress: ${(error as Error).message}`,
      };
    }
  }

  /**
   * Handle /world info
   */
  private async handleInfo(context: CommandContext): Promise<CommandResult> {
    const worldId = context.session.worldId;
    if (!worldId) {
      return {
        rc: 1,
        message: 'Not in a world',
      };
    }

    const world = this.worldManager.getWorld(worldId);
    if (!world) {
      return {
        rc: 1,
        message: `World not found: ${worldId}`,
      };
    }

    const info = [
      `World Info for "${world.worldInfo.name}" (${worldId}):`,
      `  Description: ${world.worldInfo.description || 'N/A'}`,
      `  Status: ${world.worldInfo.status ?? 'N/A'}`,
      `  Season Status: ${world.worldInfo.seasonStatus || 'N/A'}`,
      `  Season Progress: ${world.worldInfo.seasonProgress?.toFixed(2) ?? 'N/A'}`,
      `  Chunk Size: ${world.worldInfo.chunkSize || 16}`,
    ];

    if (world.worldInfo.start && world.worldInfo.stop) {
      info.push(`  Dimensions: (${world.worldInfo.start.x}, ${world.worldInfo.start.y}, ${world.worldInfo.start.z}) to (${world.worldInfo.stop.x}, ${world.worldInfo.stop.y}, ${world.worldInfo.stop.z})`);
    }

    return {
      rc: 0,
      message: info.join('\n'),
    };
  }
}
