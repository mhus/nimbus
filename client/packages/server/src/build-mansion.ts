#!/usr/bin/env node
/**
 * Build a large mansion (40x40) with 2 floors, windows, rooms, and stairs
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    // Connect to running server via IPC
    await ipcClient.connect();
    console.log('[BuildMansion] Connected to VoxelServer via IPC');

    // Get player position
    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[BuildMansion] No player found in world "main"');
      process.exit(1);
    }

    // Build mansion next to the small house (offset by 10 blocks)
    const baseX = Math.floor(playerData.position.x) + 10;
    const baseY = Math.floor(playerData.position.y);
    const baseZ = Math.floor(playerData.position.z);

    console.log('[BuildMansion] Building 40x40 mansion at:', { baseX, baseY, baseZ });

    // Mansion dimensions
    const width = 40;
    const depth = 40;
    const floorHeight = 6; // Height of each floor
    const totalHeight = floorHeight * 2; // 2 floors

    // Block IDs
    const STONE = 2;
    const PLANKS = 4;
    const GLASS = 20; // Glass for windows
    const STONE_BRICK = 48; // Stone bricks for walls
    const AIR = 0;

    const changes: any[] = [];

    console.log('[BuildMansion] Building foundation...');
    // Foundation (stone)
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

    console.log('[BuildMansion] Building exterior walls...');
    // Build exterior walls for both floors
    for (let floor = 0; floor < 2; floor++) {
      const floorY = baseY + (floor * floorHeight);

      for (let y = 0; y < floorHeight; y++) {
        const currentY = floorY + y;

        for (let x = 0; x < width; x++) {
          for (let z = 0; z < depth; z++) {
            // Only build on the perimeter
            if (x === 0 || x === width - 1 || z === 0 || z === depth - 1) {
              // Add windows (glass) at specific intervals
              const isWindow = (y === 2 || y === 3) &&
                               ((x % 5 === 2 && (z === 0 || z === depth - 1)) ||
                                (z % 5 === 2 && (x === 0 || x === width - 1)));

              // Main entrance (front wall, middle, ground floor only)
              const isMainDoor = floor === 0 && z === 0 &&
                                 x >= width / 2 - 1 && x <= width / 2 + 1 &&
                                 y < 3;

              if (isMainDoor) {
                // Leave door opening
                continue;
              } else if (isWindow) {
                changes.push({
                  x: baseX + x,
                  y: currentY,
                  z: baseZ + z,
                  blockId: GLASS,
                });
              } else {
                changes.push({
                  x: baseX + x,
                  y: currentY,
                  z: baseZ + z,
                  blockId: STONE_BRICK,
                });
              }
            }
          }
        }
      }

      // Floor ceiling/separator (planks)
      if (floor === 0) {
        console.log('[BuildMansion] Building floor separator...');
        for (let x = 1; x < width - 1; x++) {
          for (let z = 1; z < depth - 1; z++) {
            changes.push({
              x: baseX + x,
              y: floorY + floorHeight,
              z: baseZ + z,
              blockId: PLANKS,
            });
          }
        }
      }
    }

    console.log('[BuildMansion] Building interior rooms...');
    // Interior walls to create rooms (ground floor)
    const roomSize = 12;

    // Vertical interior walls (ground floor)
    for (let x = roomSize; x < width - 1; x += roomSize) {
      for (let z = 1; z < depth - 1; z++) {
        for (let y = 0; y < floorHeight - 1; y++) {
          // Add doorways every roomSize blocks
          if (!(z % roomSize === roomSize / 2 && y < 3)) {
            changes.push({
              x: baseX + x,
              y: baseY + y,
              z: baseZ + z,
              blockId: STONE_BRICK,
            });
          }
        }
      }
    }

    // Horizontal interior walls (ground floor)
    for (let z = roomSize; z < depth - 1; z += roomSize) {
      for (let x = 1; x < width - 1; x++) {
        for (let y = 0; y < floorHeight - 1; y++) {
          // Add doorways every roomSize blocks
          if (!(x % roomSize === roomSize / 2 && y < 3)) {
            changes.push({
              x: baseX + x,
              y: baseY + y,
              z: baseZ + z,
              blockId: STONE_BRICK,
            });
          }
        }
      }
    }

    // Same for second floor
    for (let x = roomSize; x < width - 1; x += roomSize) {
      for (let z = 1; z < depth - 1; z++) {
        for (let y = 0; y < floorHeight - 1; y++) {
          if (!(z % roomSize === roomSize / 2 && y < 3)) {
            changes.push({
              x: baseX + x,
              y: baseY + floorHeight + y,
              z: baseZ + z,
              blockId: STONE_BRICK,
            });
          }
        }
      }
    }

    for (let z = roomSize; z < depth - 1; z += roomSize) {
      for (let x = 1; x < width - 1; x++) {
        for (let y = 0; y < floorHeight - 1; y++) {
          if (!(x % roomSize === roomSize / 2 && y < 3)) {
            changes.push({
              x: baseX + x,
              y: baseY + floorHeight + y,
              z: baseZ + z,
              blockId: STONE_BRICK,
            });
          }
        }
      }
    }

    console.log('[BuildMansion] Building central staircase...');
    // Central spiral staircase
    const stairCenterX = width / 2;
    const stairCenterZ = depth / 2;

    for (let step = 0; step < totalHeight; step++) {
      const angle = (step / totalHeight) * Math.PI * 2 * 2; // 2 full rotations
      const radius = 3;
      const sx = Math.floor(stairCenterX + Math.cos(angle) * radius);
      const sz = Math.floor(stairCenterZ + Math.sin(angle) * radius);

      // Stair block
      changes.push({
        x: baseX + sx,
        y: baseY + step,
        z: baseZ + sz,
        blockId: PLANKS,
      });

      // Support blocks around the stair
      for (let dx = -1; dx <= 1; dx++) {
        for (let dz = -1; dz <= 1; dz++) {
          if (dx === 0 && dz === 0) continue;
          changes.push({
            x: baseX + sx + dx,
            y: baseY + step,
            z: baseZ + sz + dz,
            blockId: PLANKS,
          });
        }
      }
    }

    console.log('[BuildMansion] Building roof...');
    // Flat roof
    const roofY = baseY + totalHeight;
    for (let x = 0; x < width; x++) {
      for (let z = 0; z < depth; z++) {
        changes.push({
          x: baseX + x,
          y: roofY,
          z: baseZ + z,
          blockId: PLANKS,
        });
      }
    }

    console.log(`[BuildMansion] Total blocks to place: ${changes.length}`);

    // Connect to WebSocket and send all changes
    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[BuildMansion] Connected to WebSocket server');
        console.log('[BuildMansion] Sending mansion construction (this may take a moment)...');

        const message = {
          type: 'apply_block_changes',
          changes: changes,
        };

        ws.send(JSON.stringify(message));
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());

        if (message.type === 'block_changes_applied') {
          console.log(`[BuildMansion] ✅ Mansion built successfully!`);
          console.log(`[BuildMansion] Placed ${message.count} blocks`);
          console.log(`[BuildMansion] Updated ${message.affectedChunks} chunks`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[BuildMansion] WebSocket error:', error);
        reject(error);
      });

      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 30000); // 30 second timeout for large structure
    });

    ipcClient.disconnect();
    console.log('[BuildMansion] Done! Your 40x40 mansion is ready to explore!');
    console.log('[BuildMansion] Features:');
    console.log('[BuildMansion] - 2 floors with separate rooms');
    console.log('[BuildMansion] - Windows on all sides');
    console.log('[BuildMansion] - Central spiral staircase');
    console.log('[BuildMansion] - Multiple rooms per floor');
    console.log('[BuildMansion] - Grand entrance');
  } catch (error: any) {
    console.error('[BuildMansion] Error:', error.message);
    process.exit(1);
  }
}

main();
