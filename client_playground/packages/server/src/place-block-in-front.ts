#!/usr/bin/env node
/**
 * Place a block in front of the player
 */

import { IPCClient } from './ipc/IPCClient.js';
import { VoxelServer } from './VoxelServer.js';

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

    // Load world and place block
    const config = {
      port: 3003,
      worldName: 'main',
      worldSeed: 12345,
      generator: 'flat' as const,
    };

    const voxelServer = new VoxelServer(config);
    const world = await voxelServer.worldManager.load('main');

    if (!world) {
      console.error('[PlaceBlock] Failed to load world');
      process.exit(1);
    }

    // Place a stone block (ID 2) at the target position
    await world.setBlock([targetX, targetY, targetZ], 2);

    console.log(`[PlaceBlock] ✅ Placed stone block at (${targetX}, ${targetY}, ${targetZ})`);
    console.log('[PlaceBlock] The block should appear in front of you!');

    ipcClient.disconnect();
  } catch (error: any) {
    console.error('[PlaceBlock] Error:', error.message);
    process.exit(1);
  }
}

main();
