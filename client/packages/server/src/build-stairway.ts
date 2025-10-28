#!/usr/bin/env node
/**
 * Build a stairway from player position upward
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    // Connect to running server via IPC
    await ipcClient.connect();
    console.log('[Stairway] Connected to VoxelServer via IPC');

    // Get player position
    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[Stairway] No player found in world "main"');
      process.exit(1);
    }

    const baseX = Math.floor(playerData.position.x);
    const baseY = Math.floor(playerData.position.y);
    const baseZ = Math.floor(playerData.position.z);

    console.log('[Stairway] Building stairway at:', { baseX, baseY, baseZ });

    // Block IDs
    const STONE = 2;
    const AIR = 0;

    const changes: any[] = [];

    // Stairway configuration
    const stairHeight = 10;  // 10 blocks high

    console.log('[Stairway] Creating stairway going up 10 blocks...');

    // Build stairway going up in Z direction
    for (let step = 0; step < stairHeight; step++) {
      const stepY = baseY + step;
      const stepZ = baseZ + step;

      // Place the step block
      changes.push({
        x: baseX,
        y: stepY,
        z: stepZ,
        blockId: STONE,
      });

      // Also place blocks to left and right for wider stairs (3 blocks wide)
      changes.push({
        x: baseX - 1,
        y: stepY,
        z: stepZ,
        blockId: STONE,
      });
      changes.push({
        x: baseX + 1,
        y: stepY,
        z: stepZ,
        blockId: STONE,
      });

      // Clear air above the stairs (3 blocks high clearance)
      for (let clearY = 1; clearY <= 3; clearY++) {
        changes.push({
          x: baseX,
          y: stepY + clearY,
          z: stepZ,
          blockId: AIR,
        });
        changes.push({
          x: baseX - 1,
          y: stepY + clearY,
          z: stepZ,
          blockId: AIR,
        });
        changes.push({
          x: baseX + 1,
          y: stepY + clearY,
          z: stepZ,
          blockId: AIR,
        });
      }
    }

    console.log(`[Stairway] Total blocks to place: ${changes.length}`);

    // Connect to WebSocket and send all changes
    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[Stairway] Connected to WebSocket server');
        console.log('[Stairway] Sending stairway construction...');

        const message = {
          type: 'apply_block_changes',
          changes: changes,
        };

        ws.send(JSON.stringify(message));
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());

        if (message.type === 'block_changes_applied') {
          console.log(`[Stairway] ✅ Stairway built successfully!`);
          console.log(`[Stairway] Placed ${message.count} blocks`);
          console.log(`[Stairway] Updated ${message.affectedChunks} chunks`);
          console.log(`[Stairway] Stairway goes from Y=${baseY} to Y=${baseY + stairHeight}`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[Stairway] WebSocket error:', error);
        reject(error);
      });

      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 30000);
    });

    ipcClient.disconnect();
    console.log('[Stairway] Done! Stairway is ready.');
  } catch (error: any) {
    console.error('[Stairway] Error:', error.message);
    process.exit(1);
  }
}

main();
