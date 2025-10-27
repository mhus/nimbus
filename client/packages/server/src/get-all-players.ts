#!/usr/bin/env node
/**
 * Quick script to get all players via IPC
 */

import { IPCClient } from './ipc/IPCClient.js';

const client = new IPCClient();

async function main() {
  try {
    await client.connect();
    console.log('[GetPlayers] Connected to VoxelServer via IPC');

    const allPlayers = await client.getAllPlayers();
    console.log('[GetPlayers] All Players:');
    console.log(JSON.stringify(allPlayers, null, 2));

    const worldInfo = await client.getWorldInfo('main');
    console.log('\n[GetPlayers] World Info (main):');
    console.log(JSON.stringify(worldInfo, null, 2));

    const entityCount = await client.getEntityCount({ worldName: 'main' });
    console.log('\n[GetPlayers] Entity Count (main):');
    console.log(JSON.stringify(entityCount, null, 2));

    client.disconnect();
  } catch (error: any) {
    console.error('[GetPlayers] Error:', error.message);
    process.exit(1);
  }
}

main();
