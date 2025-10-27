#!/usr/bin/env node
/**
 * Build a house at the player's position
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    // Connect to running server via IPC
    await ipcClient.connect();
    console.log('[BuildHouse] Connected to VoxelServer via IPC');

    // Get player position
    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[BuildHouse] No player found in world "main"');
      process.exit(1);
    }

    const baseX = Math.floor(playerData.position.x);
    const baseY = Math.floor(playerData.position.y);
    const baseZ = Math.floor(playerData.position.z);

    console.log('[BuildHouse] Building house at player position:', { baseX, baseY, baseZ });

    // House dimensions
    const width = 5;
    const depth = 5;
    const height = 4;

    // Block IDs
    const STONE = 2;
    const PLANKS = 4; // Oak planks for roof
    const AIR = 0;

    const changes: any[] = [];

    // Build floor (stone)
    console.log('[BuildHouse] Building floor...');
    for (let x = 0; x < width; x++) {
      for (let z = 0; z < depth; z++) {
        changes.push({
          x: baseX + x,
          y: baseY - 1,
          z: baseZ + z,
          blockId: STONE,
        });
      }
    }

    // Build walls (stone), but leave door opening
    console.log('[BuildHouse] Building walls...');
    for (let y = 0; y < height; y++) {
      for (let x = 0; x < width; x++) {
        for (let z = 0; z < depth; z++) {
          // Only build on the perimeter
          if (x === 0 || x === width - 1 || z === 0 || z === depth - 1) {
            // Leave door opening on one side (front wall, middle)
            if (!(z === 0 && x === Math.floor(width / 2) && y < 2)) {
              changes.push({
                x: baseX + x,
                y: baseY + y,
                z: baseZ + z,
                blockId: STONE,
              });
            }
          }
        }
      }
    }

    // Build pyramidal roof (planks)
    console.log('[BuildHouse] Building roof...');
    const roofStartY = baseY + height;

    // Roof layer 1 (full coverage)
    for (let x = 0; x < width; x++) {
      for (let z = 0; z < depth; z++) {
        changes.push({
          x: baseX + x,
          y: roofStartY,
          z: baseZ + z,
          blockId: PLANKS,
        });
      }
    }

    // Roof layer 2 (smaller)
    for (let x = 1; x < width - 1; x++) {
      for (let z = 1; z < depth - 1; z++) {
        changes.push({
          x: baseX + x,
          y: roofStartY + 1,
          z: baseZ + z,
          blockId: PLANKS,
        });
      }
    }

    // Roof layer 3 (even smaller)
    for (let x = 2; x < width - 2; x++) {
      for (let z = 2; z < depth - 2; z++) {
        changes.push({
          x: baseX + x,
          y: roofStartY + 2,
          z: baseZ + z,
          blockId: PLANKS,
        });
      }
    }

    // Roof top (single block)
    changes.push({
      x: baseX + Math.floor(width / 2),
      y: roofStartY + 3,
      z: baseZ + Math.floor(depth / 2),
      blockId: PLANKS,
    });

    console.log(`[BuildHouse] Total blocks to place: ${changes.length}`);

    // Connect to WebSocket and send all changes
    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[BuildHouse] Connected to WebSocket server');

        const message = {
          type: 'apply_block_changes',
          changes: changes,
        };

        ws.send(JSON.stringify(message));
        console.log('[BuildHouse] Sent house construction to server');
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());

        if (message.type === 'block_changes_applied') {
          console.log(`[BuildHouse] ✅ House built successfully!`);
          console.log(`[BuildHouse] Placed ${message.count} blocks`);
          console.log(`[BuildHouse] Updated ${message.affectedChunks} chunks`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[BuildHouse] WebSocket error:', error);
        reject(error);
      });

      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 10000);
    });

    ipcClient.disconnect();
    console.log('[BuildHouse] Done! Your house should now be visible.');
  } catch (error: any) {
    console.error('[BuildHouse] Error:', error.message);
    process.exit(1);
  }
}

main();
