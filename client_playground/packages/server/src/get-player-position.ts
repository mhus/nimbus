#!/usr/bin/env node
/**
 * Quick script to get player position via IPC
 */

import { IPCClient } from './ipc/IPCClient.js';

const client = new IPCClient();

async function main() {
  try {
    await client.connect();
    console.log('[GetPosition] Connected to VoxelServer via IPC');

    const playerData = await client.getPlayerPosition('main');
    console.log('[GetPosition] Player Position Data:');
    console.log(JSON.stringify(playerData, null, 2));

    client.disconnect();
  } catch (error: any) {
    console.error('[GetPosition] Error:', error.message);
    process.exit(1);
  }
}

main();
