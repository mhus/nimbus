#!/usr/bin/env node
/**
 * Build a tree at the player's position
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    // Connect to running server via IPC
    await ipcClient.connect();
    console.log('[BuildTree] Connected to VoxelServer via IPC');

    // Get player position
    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[BuildTree] No player found in world "main"');
      process.exit(1);
    }

    const baseX = Math.floor(playerData.position.x);
    const baseY = Math.floor(playerData.position.y);
    const baseZ = Math.floor(playerData.position.z);

    console.log('[BuildTree] Building tree at player position:', { baseX, baseY, baseZ });

    // Block IDs
    const WOOD = 5; // Oak wood/log
    const LEAVES = 6; // Oak leaves

    const changes: any[] = [];

    // Tree trunk (5 blocks tall)
    const trunkHeight = 5;
    console.log('[BuildTree] Building trunk...');
    for (let y = 0; y < trunkHeight; y++) {
      changes.push({
        x: baseX,
        y: baseY + y,
        z: baseZ,
        blockId: WOOD,
      });
    }

    // Leaves (crown) - 3 layers
    const crownStartY = baseY + trunkHeight - 1;

    console.log('[BuildTree] Building leaf crown...');

    // Bottom layer of leaves (5x5)
    for (let x = -2; x <= 2; x++) {
      for (let z = -2; z <= 2; z++) {
        // Skip corners and center (trunk)
        if ((Math.abs(x) === 2 && Math.abs(z) === 2) || (x === 0 && z === 0)) {
          continue;
        }
        changes.push({
          x: baseX + x,
          y: crownStartY,
          z: baseZ + z,
          blockId: LEAVES,
        });
      }
    }

    // Middle layer of leaves (5x5)
    for (let x = -2; x <= 2; x++) {
      for (let z = -2; z <= 2; z++) {
        // Skip far corners
        if (Math.abs(x) === 2 && Math.abs(z) === 2) {
          continue;
        }
        changes.push({
          x: baseX + x,
          y: crownStartY + 1,
          z: baseZ + z,
          blockId: LEAVES,
        });
      }
    }

    // Top layer of leaves (3x3)
    for (let x = -1; x <= 1; x++) {
      for (let z = -1; z <= 1; z++) {
        changes.push({
          x: baseX + x,
          y: crownStartY + 2,
          z: baseZ + z,
          blockId: LEAVES,
        });
      }
    }

    // Top single leaf block
    changes.push({
      x: baseX,
      y: crownStartY + 3,
      z: baseZ,
      blockId: LEAVES,
    });

    console.log(`[BuildTree] Total blocks to place: ${changes.length}`);

    // Connect to WebSocket and send all changes
    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[BuildTree] Connected to WebSocket server');

        const message = {
          type: 'apply_block_changes',
          changes: changes,
        };

        ws.send(JSON.stringify(message));
        console.log('[BuildTree] Sent tree construction to server');
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());

        if (message.type === 'block_changes_applied') {
          console.log(`[BuildTree] ✅ Tree built successfully!`);
          console.log(`[BuildTree] Placed ${message.count} blocks`);
          console.log(`[BuildTree] Updated ${message.affectedChunks} chunks`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[BuildTree] WebSocket error:', error);
        reject(error);
      });

      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 10000);
    });

    ipcClient.disconnect();
    console.log('[BuildTree] Done! Your tree should now be visible.');
  } catch (error: any) {
    console.error('[BuildTree] Error:', error.message);
    process.exit(1);
  }
}

main();
