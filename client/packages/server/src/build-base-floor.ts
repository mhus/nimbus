#!/usr/bin/env node
/**
 * Build a base floor plate 10 blocks below player
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    // Connect to running server via IPC
    await ipcClient.connect();
    console.log('[BaseFloor] Connected to VoxelServer via IPC');

    // Get player position
    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[BaseFloor] No player found in world "main"');
      process.exit(1);
    }

    const baseX = Math.floor(playerData.position.x);
    const baseY = Math.floor(playerData.position.y);
    const baseZ = Math.floor(playerData.position.z);

    console.log('[BaseFloor] Building base floor at:', { baseX, baseY, baseZ });

    // Block IDs
    const STONE = 2;

    const changes: any[] = [];

    // Floor dimensions
    const width = 40;  // X direction
    const depth = 30;  // Z direction
    const floorY = baseY - 10;  // 10 blocks below player

    console.log('[BaseFloor] Creating 40x30 stone floor at y=' + floorY);

    // Create floor centered on player X and Z position
    for (let x = -width/2; x < width/2; x++) {
      for (let z = -depth/2; z < depth/2; z++) {
        changes.push({
          x: baseX + x,
          y: floorY,
          z: baseZ + z,
          blockId: STONE,
        });
      }
    }

    console.log(`[BaseFloor] Total blocks to place: ${changes.length}`);

    // Connect to WebSocket and send all changes
    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[BaseFloor] Connected to WebSocket server');
        console.log('[BaseFloor] Sending floor construction...');

        const message = {
          type: 'apply_block_changes',
          changes: changes,
        };

        ws.send(JSON.stringify(message));
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());

        if (message.type === 'block_changes_applied') {
          console.log(`[BaseFloor] ✅ Base floor built successfully!`);
          console.log(`[BaseFloor] Placed ${message.count} blocks`);
          console.log(`[BaseFloor] Updated ${message.affectedChunks} chunks`);
          console.log(`[BaseFloor] Floor is at Y=${floorY}, 10 blocks below you`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[BaseFloor] WebSocket error:', error);
        reject(error);
      });

      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 30000);
    });

    ipcClient.disconnect();
    console.log('[BaseFloor] Done! Base floor is ready.');
  } catch (error: any) {
    console.error('[BaseFloor] Error:', error.message);
    process.exit(1);
  }
}

main();
