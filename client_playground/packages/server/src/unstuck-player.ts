#!/usr/bin/env node
/**
 * Unstuck player by teleporting them up if they're stuck in a block
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    // Connect to running server via IPC
    await ipcClient.connect();
    console.log('[Unstuck] Connected to VoxelServer via IPC');

    // Get player position
    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[Unstuck] No player found in world "main"');
      process.exit(1);
    }

    const currentX = playerData.position.x;
    const currentY = playerData.position.y;
    const currentZ = playerData.position.z;

    console.log('[Unstuck] Current player position:', playerData.position);

    // Move player up by removing blocks above them
    // We'll clear a 2-block tall column above the player for 5 blocks up
    const changes: any[] = [];

    const blockX = Math.floor(currentX);
    const blockY = Math.floor(currentY);
    const blockZ = Math.floor(currentZ);

    console.log('[Unstuck] Clearing blocks above player position...');

    // Clear 5 blocks straight up (air blocks)
    for (let y = 0; y < 10; y++) {
      changes.push({
        x: blockX,
        y: blockY + y,
        z: blockZ,
        blockId: 0, // Air
      });
    }

    console.log(`[Unstuck] Clearing ${changes.length} blocks above you`);

    // Connect to WebSocket and send changes
    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[Unstuck] Connected to WebSocket server');

        const message = {
          type: 'apply_block_changes',
          changes: changes,
        };

        ws.send(JSON.stringify(message));
        console.log('[Unstuck] Sent block removal');
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());

        if (message.type === 'block_changes_applied') {
          console.log(`[Unstuck] ✅ Blocks cleared!`);
          console.log(`[Unstuck] You should now be able to move upwards.`);
          console.log(`[Unstuck] Try jumping or looking up to get out of the block.`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[Unstuck] WebSocket error:', error);
        reject(error);
      });

      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 5000);
    });

    ipcClient.disconnect();
  } catch (error: any) {
    console.error('[Unstuck] Error:', error.message);
    process.exit(1);
  }
}

main();
