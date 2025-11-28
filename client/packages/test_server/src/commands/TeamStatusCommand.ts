/**
 * TeamStatusCommand - Update team member status
 */

import { CommandHandler, CommandContext, CommandResult } from './CommandHandler';
import { toNumber, toString, type TeamStatusUpdate } from '@nimbus/shared';

/**
 * Team status command - Sends team member status updates to client
 */
export class TeamStatusCommand extends CommandHandler {
  name(): string {
    return 'teamStatus';
  }

  description(): string {
    return 'Update team member status. Usage: teamStatus <teamId> <playerId> <health> <status>';
  }

  async execute(context: CommandContext, args: any[]): Promise<CommandResult> {
    const teamId = toString(args[0]);
    const playerId = toString(args[1]);
    const health = args[2] !== undefined ? toNumber(args[2]) : undefined;
    const status = args[3] !== undefined ? (toNumber(args[3]) as 0 | 1 | 2) : undefined;

    const update: TeamStatusUpdate = {
      id: teamId,
      ms: [
        {
          id: playerId,
          h: health,
          st: status,
        },
      ],
    };

    // Send to client
    context.session.ws.send(
      JSON.stringify({
        t: 't.s',
        d: update,
      })
    );

    return {
      rc: 0,
      message: `Status updated for player ${playerId}`,
    };
  }
}
