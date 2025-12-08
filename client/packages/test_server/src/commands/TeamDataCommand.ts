/**
 * TeamDataCommand - Send team data to client
 */

import { CommandHandler, CommandContext, CommandResult } from './CommandHandler';
import { toString, type TeamData } from '@nimbus/shared';

/**
 * Team data command - Sends complete team data to client
 */
export class TeamDataCommand extends CommandHandler {
  name(): string {
    return 'teamData';
  }

  description(): string {
    return 'Send team data to client. Usage: teamData <teamName> <player1> <player2> ...';
  }

  async execute(context: CommandContext, args: any[]): Promise<CommandResult> {
    const teamName = toString(args[0] || 'DefaultTeam');
    const playerNames = args.slice(1);

    if (playerNames.length === 0) {
      return { rc: 1, message: 'At least one player name required' };
    }

    const teamData: TeamData = {
      name: teamName,
      id: `team_${Date.now()}`,
      members: playerNames.map((name, i) => ({
        playerId: `player_${i}`,
        name: toString(name),
        status: 1, // alive
        health: 100,
      })),
    };

    // Send to client
    context.session.ws.send(
      JSON.stringify({
        t: 't.d ',
        d: teamData,
      })
    );

    return {
      rc: 0,
      message: `Team "${teamName}" sent with ${playerNames.length} members`,
    };
  }
}
