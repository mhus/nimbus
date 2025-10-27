#!/usr/bin/env node
/**
 * Check all worlds for player entities
 */

import { IPCClient } from './ipc/IPCClient.js';

const client = new IPCClient();

async function main() {
  try {
    await client.connect();
    console.log('[CheckWorlds] Connected to VoxelServer via IPC\n');

    // Check both worlds
    const worlds = ['main', 'world'];

    for (const worldName of worlds) {
      console.log(`=== Checking world: ${worldName} ===`);

      const worldInfo = await client.getWorldInfo(worldName);
      console.log('World Info:', JSON.stringify(worldInfo, null, 2));

      const entityCount = await client.getEntityCount({ worldName });
      console.log('Entity Count:', entityCount);

      const playerData = await client.getPlayerPosition(worldName);
      console.log('Player Position:', JSON.stringify(playerData, null, 2));

      console.log('');
    }

    const allPlayers = await client.getAllPlayers();
    console.log('All Players across all worlds:', JSON.stringify(allPlayers, null, 2));

    client.disconnect();
  } catch (error: any) {
    console.error('[CheckWorlds] Error:', error.message);
    process.exit(1);
  }
}

main();
