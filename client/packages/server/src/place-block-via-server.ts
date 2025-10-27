#!/usr/bin/env node
/**
 * Place a block in front of the player by sending block changes to the running server
 * This ensures the server broadcasts the update to all connected clients
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    // Connect to running server via IPC
    await ipcClient.connect();
    console.log('[PlaceBlock] Connected to VoxelServer via IPC');

    // Get player position
    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[PlaceBlock] No player found in world "main"');
      process.exit(1);
    }

    console.log('[PlaceBlock] Player position:', playerData.position);
    console.log('[PlaceBlock] Player rotation:', playerData.rotation);

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

    console.log(`[PlaceBlock] Placing block at (${targetX}, ${targetY}, ${targetZ})`);

    // Connect to the WebSocket server directly to send block changes
    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[PlaceBlock] Connected to WebSocket server');

        // Send block change
        const blockChange = {
          type: 'apply_block_changes',
          changes: [
            {
              x: targetX,
              y: targetY,
              z: targetZ,
              blockId: 2, // Stone block
            },
          ],
        };

        ws.send(JSON.stringify(blockChange));
        console.log('[PlaceBlock] Sent block change to server');
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());
        console.log('[PlaceBlock] Received from server:', message);

        if (message.type === 'block_changes_applied') {
          console.log(`[PlaceBlock] ✅ Block placed successfully!`);
          console.log(`[PlaceBlock] ${message.affectedChunks} chunks were updated and broadcast to all clients`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[PlaceBlock] WebSocket error:', error);
        reject(error);
      });

      // Timeout after 5 seconds
      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 5000);
    });

    ipcClient.disconnect();
    console.log('[PlaceBlock] Done! The block should now be visible in your client.');
  } catch (error: any) {
    console.error('[PlaceBlock] Error:', error.message);
    process.exit(1);
  }
}

main();
