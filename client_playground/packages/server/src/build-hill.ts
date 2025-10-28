#!/usr/bin/env node
/**
 * Build a hill/terrain under the mansion so it sits on elevated ground
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    // Connect to running server via IPC
    await ipcClient.connect();
    console.log('[BuildHill] Connected to VoxelServer via IPC');

    // Get player position to calculate mansion location
    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[BuildHill] No player found in world "main"');
      process.exit(1);
    }

    // Mansion location (same as in build-mansion.ts)
    const mansionBaseX = Math.floor(playerData.position.x) + 10;
    const mansionBaseY = Math.floor(playerData.position.y);
    const mansionBaseZ = Math.floor(playerData.position.z);

    const mansionWidth = 40;
    const mansionDepth = 40;

    console.log('[BuildHill] Building hill under mansion at:', {
      x: mansionBaseX,
      y: mansionBaseY,
      z: mansionBaseZ
    });

    // Block IDs
    const DIRT = 3;
    const GRASS = 1; // Grass block

    const changes: any[] = [];

    // Find the ground level (assuming flat terrain around y=64)
    const groundLevel = 64;
    const buildHeight = mansionBaseY - 1; // Up to the mansion foundation

    console.log('[BuildHill] Filling terrain from y=' + groundLevel + ' to y=' + buildHeight);

    // Create a gradual hill that extends beyond the mansion
    const hillExtension = 10; // Extend hill 10 blocks beyond mansion on each side
    const totalWidth = mansionWidth + hillExtension * 2;
    const totalDepth = mansionDepth + hillExtension * 2;

    for (let x = -hillExtension; x < mansionWidth + hillExtension; x++) {
      for (let z = -hillExtension; z < mansionDepth + hillExtension; z++) {
        // Calculate distance from mansion center for slope
        const centerX = mansionWidth / 2;
        const centerZ = mansionDepth / 2;
        const distX = Math.abs(x - centerX);
        const distZ = Math.abs(z - centerZ);
        const maxDist = Math.max(distX, distZ);

        // Calculate how high this column should be (slope from edges)
        const maxDistance = Math.max(mansionWidth, mansionDepth) / 2 + hillExtension;
        const heightFactor = 1 - (maxDist / maxDistance);
        const targetHeight = Math.floor(groundLevel + (buildHeight - groundLevel) * Math.max(0, heightFactor));

        // Fill from ground level to target height
        for (let y = groundLevel; y <= targetHeight; y++) {
          const blockId = (y === targetHeight) ? GRASS : DIRT;

          changes.push({
            x: mansionBaseX + x,
            y: y,
            z: mansionBaseZ + z,
            blockId: blockId,
          });
        }
      }
    }

    console.log(`[BuildHill] Total blocks to place: ${changes.length}`);

    // Connect to WebSocket and send all changes
    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[BuildHill] Connected to WebSocket server');
        console.log('[BuildHill] Sending terrain construction...');

        const message = {
          type: 'apply_block_changes',
          changes: changes,
        };

        ws.send(JSON.stringify(message));
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());

        if (message.type === 'block_changes_applied') {
          console.log(`[BuildHill] ✅ Hill built successfully!`);
          console.log(`[BuildHill] Placed ${message.count} blocks`);
          console.log(`[BuildHill] Updated ${message.affectedChunks} chunks`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[BuildHill] WebSocket error:', error);
        reject(error);
      });

      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 30000);
    });

    ipcClient.disconnect();
    console.log('[BuildHill] Done! Your mansion now sits on a nice hill!');
  } catch (error: any) {
    console.error('[BuildHill] Error:', error.message);
    process.exit(1);
  }
}

main();
