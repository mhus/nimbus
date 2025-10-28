#!/usr/bin/env node
/**
 * Add extra bottom step at player position
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    await ipcClient.connect();
    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('No player found');
      process.exit(1);
    }

    const baseX = Math.floor(playerData.position.x);
    const baseY = Math.floor(playerData.position.y);
    const baseZ = Math.floor(playerData.position.z);

    console.log('[ExtraStep] Adding extra step at:', { baseX, baseY, baseZ });

    const STONE = 2;
    const changes: any[] = [];

    // Add step one block lower (Y-1) and 2 blocks forward (Z-2)
    for (let x = -1; x <= 1; x++) {
      changes.push({
        x: baseX + x,
        y: baseY - 1,
        z: baseZ - 2,
        blockId: STONE,
      });
    }

    console.log(`[ExtraStep] Placing ${changes.length} blocks`);

    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        ws.send(JSON.stringify({
          type: 'apply_block_changes',
          changes: changes,
        }));
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());
        if (message.type === 'block_changes_applied') {
          console.log(`✅ Extra step added at y=${baseY - 1}`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('Error:', error);
        reject(error);
      });

      setTimeout(() => {
        reject(new Error('Timeout'));
      }, 10000);
    });

    ipcClient.disconnect();
  } catch (error: any) {
    console.error('Error:', error.message);
    process.exit(1);
  }
}

main();
