#!/usr/bin/env node
/**
 * Add a bottom step at player position
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    // Connect to running server via IPC
    await ipcClient.connect();
    console.log('[BottomStep] Connected to VoxelServer via IPC');

    // Get player position
    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[BottomStep] No player found in world "main"');
      process.exit(1);
    }

    const baseX = Math.floor(playerData.position.x);
    const baseY = Math.floor(playerData.position.y);
    const baseZ = Math.floor(playerData.position.z);

    console.log('[BottomStep] Adding bottom step at:', { baseX, baseY, baseZ });

    // Block IDs
    const STONE = 2;

    const changes: any[] = [];

    // Add 3 blocks wide step in front of player (Z-1)
    for (let x = -1; x <= 1; x++) {
      changes.push({
        x: baseX + x,
        y: baseY,
        z: baseZ - 1,
        blockId: STONE,
      });
    }

    console.log(`[BottomStep] Total blocks to place: ${changes.length}`);

    // Connect to WebSocket and send all changes
    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[BottomStep] Connected to WebSocket server');

        const message = {
          type: 'apply_block_changes',
          changes: changes,
        };

        ws.send(JSON.stringify(message));
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());

        if (message.type === 'block_changes_applied') {
          console.log(`[BottomStep] ✅ Bottom step added!`);
          console.log(`[BottomStep] Placed ${message.count} blocks`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[BottomStep] WebSocket error:', error);
        reject(error);
      });

      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 10000);
    });

    ipcClient.disconnect();
    console.log('[BottomStep] Done!');
  } catch (error: any) {
    console.error('[BottomStep] Error:', error.message);
    process.exit(1);
  }
}

main();
