#!/usr/bin/env node
/**
 * Remove a block in front of the player by placing air (blockId 0)
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    // Connect to running server via IPC
    await ipcClient.connect();
    console.log('[RemoveBlock] Connected to VoxelServer via IPC');

    // Get player position
    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[RemoveBlock] No player found in world "main"');
      process.exit(1);
    }

    console.log('[RemoveBlock] Player position:', playerData.position);
    console.log('[RemoveBlock] Player rotation:', playerData.rotation);

    // Calculate position in front of player based on rotation
    const distance = 3; // 3 blocks in front
    const yaw = playerData.rotation.y;

    // Calculate direction vector from yaw
    const dx = Math.sin(yaw) * distance;
    const dz = Math.cos(yaw) * distance;

    // Target position (round to block coordinates)
    const targetX = Math.floor(playerData.position.x + dx);
    const targetY = Math.floor(playerData.position.y);
    const targetZ = Math.floor(playerData.position.z + dz);

    console.log(`[RemoveBlock] Removing block at (${targetX}, ${targetY}, ${targetZ})`);

    // Connect to the WebSocket server directly to send block changes
    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[RemoveBlock] Connected to WebSocket server');

        // Send block change (air = 0)
        const blockChange = {
          type: 'apply_block_changes',
          changes: [
            {
              x: targetX,
              y: targetY,
              z: targetZ,
              blockId: 0, // Air block (removes block)
            },
          ],
        };

        ws.send(JSON.stringify(blockChange));
        console.log('[RemoveBlock] Sent block removal to server');
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());

        if (message.type === 'block_changes_applied') {
          console.log(`[RemoveBlock] ✅ Block removed successfully!`);
          console.log(`[RemoveBlock] ${message.affectedChunks} chunks were updated and broadcast to all clients`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[RemoveBlock] WebSocket error:', error);
        reject(error);
      });

      // Timeout after 5 seconds
      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 5000);
    });

    ipcClient.disconnect();
    console.log('[RemoveBlock] Done! The block should now be removed from your client.');
  } catch (error: any) {
    console.error('[RemoveBlock] Error:', error.message);
    process.exit(1);
  }
}

main();
