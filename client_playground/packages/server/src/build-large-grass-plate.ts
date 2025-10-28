#!/usr/bin/env node
/**
 * Build a large 80x80 grass plate below player
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    await ipcClient.connect();
    console.log('[GrassPlate] Connected to VoxelServer via IPC');

    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[GrassPlate] No player found in world "main"');
      process.exit(1);
    }

    const baseX = Math.floor(playerData.position.x);
    const baseY = Math.floor(playerData.position.y);
    const baseZ = Math.floor(playerData.position.z);

    console.log('[GrassPlate] Building 80x80 grass plate at:', { baseX, baseY, baseZ });

    const GRASS = 1;
    const changes: any[] = [];

    const width = 80;  // X direction
    const depth = 80;  // Z direction
    const plateY = baseY - 1;  // 1 block below player

    console.log('[GrassPlate] Creating 80x80 grass plate at y=' + plateY);

    // Create plate centered on player X and Z position
    for (let x = -width/2; x < width/2; x++) {
      for (let z = -depth/2; z < depth/2; z++) {
        changes.push({
          x: baseX + x,
          y: plateY,
          z: baseZ + z,
          blockId: GRASS,
        });
      }
    }

    console.log(`[GrassPlate] Total blocks to place: ${changes.length}`);

    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[GrassPlate] Connected to WebSocket server');
        console.log('[GrassPlate] Sending grass plate construction...');

        const message = {
          type: 'apply_block_changes',
          changes: changes,
        };

        ws.send(JSON.stringify(message));
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());

        if (message.type === 'block_changes_applied') {
          console.log(`[GrassPlate] ✅ Grass plate built successfully!`);
          console.log(`[GrassPlate] Placed ${message.count} blocks`);
          console.log(`[GrassPlate] Updated ${message.affectedChunks} chunks`);
          console.log(`[GrassPlate] Plate is at Y=${plateY}, 1 block below you`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[GrassPlate] WebSocket error:', error);
        reject(error);
      });

      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 30000);
    });

    ipcClient.disconnect();
    console.log('[GrassPlate] Done! 80x80 grass plate is ready.');
  } catch (error: any) {
    console.error('[GrassPlate] Error:', error.message);
    process.exit(1);
  }
}

main();
