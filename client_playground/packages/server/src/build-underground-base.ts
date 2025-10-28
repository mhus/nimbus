#!/usr/bin/env node
/**
 * Build an underground base with entrance at player's position
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    // Connect to running server via IPC
    await ipcClient.connect();
    console.log('[UndergroundBase] Connected to VoxelServer via IPC');

    // Get player position
    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[UndergroundBase] No player found in world "main"');
      process.exit(1);
    }

    const baseX = Math.floor(playerData.position.x);
    const baseY = Math.floor(playerData.position.y);
    const baseZ = Math.floor(playerData.position.z);

    console.log('[UndergroundBase] Building underground base at:', { baseX, baseY, baseZ });

    // Block IDs
    const STONE = 2;     // Stone for walls
    const AIR = 0;       // Air for hollowing out
    const PLANKS = 4;    // Wood planks for floors
    const GLASS = 20;    // Glass for windows/lights

    const changes: any[] = [];

    // Base configuration
    const mainRoomWidth = 15;
    const mainRoomDepth = 15;
    const mainRoomHeight = 5;
    const baseDepth = 10; // How deep underground

    console.log('[UndergroundBase] Creating entrance stairway...');

    // Entrance stairway (going down)
    const stairLength = baseDepth;
    for (let i = 0; i < stairLength; i++) {
      // Clear 3x3 stairway
      for (let x = -1; x <= 1; x++) {
        for (let z = -1; z <= 1; z++) {
          for (let y = 0; y < 3; y++) {
            changes.push({
              x: baseX + x,
              y: baseY - i + y,
              z: baseZ + z + i,
              blockId: AIR,
            });
          }
        }
      }

      // Add step
      changes.push({
        x: baseX,
        y: baseY - i - 1,
        z: baseZ + i,
        blockId: STONE,
      });
    }

    console.log('[UndergroundBase] Hollowing out main room...');

    // Main room position (at bottom of stairs)
    const roomX = baseX;
    const roomY = baseY - baseDepth;
    const roomZ = baseZ + stairLength;

    // Hollow out main room
    for (let x = -mainRoomWidth/2; x < mainRoomWidth/2; x++) {
      for (let z = 0; z < mainRoomDepth; z++) {
        for (let y = 0; y < mainRoomHeight; y++) {
          changes.push({
            x: roomX + x,
            y: roomY + y,
            z: roomZ + z,
            blockId: AIR,
          });
        }
      }
    }

    console.log('[UndergroundBase] Building walls and ceiling...');

    // Build stone walls
    for (let x = -mainRoomWidth/2 - 1; x <= mainRoomWidth/2; x++) {
      for (let z = -1; z <= mainRoomDepth; z++) {
        // Floor
        changes.push({
          x: roomX + x,
          y: roomY - 1,
          z: roomZ + z,
          blockId: STONE,
        });

        // Ceiling
        changes.push({
          x: roomX + x,
          y: roomY + mainRoomHeight,
          z: roomZ + z,
          blockId: STONE,
        });

        // Walls
        for (let y = 0; y < mainRoomHeight; y++) {
          // Front and back walls
          if (z === -1 || z === mainRoomDepth) {
            changes.push({
              x: roomX + x,
              y: roomY + y,
              z: roomZ + z,
              blockId: STONE,
            });
          }

          // Side walls
          if (x === -mainRoomWidth/2 - 1 || x === mainRoomWidth/2) {
            changes.push({
              x: roomX + x,
              y: roomY + y,
              z: roomZ + z,
              blockId: STONE,
            });
          }
        }
      }
    }

    console.log('[UndergroundBase] Creating side rooms...');

    // Left side room
    const sideRoomWidth = 8;
    const sideRoomDepth = 8;
    const leftRoomX = roomX - mainRoomWidth/2 - sideRoomWidth;
    const leftRoomZ = roomZ + mainRoomDepth/2 - sideRoomDepth/2;

    // Hollow out left room
    for (let x = 0; x < sideRoomWidth; x++) {
      for (let z = 0; z < sideRoomDepth; z++) {
        for (let y = 0; y < mainRoomHeight; y++) {
          changes.push({
            x: leftRoomX + x,
            y: roomY + y,
            z: leftRoomZ + z,
            blockId: AIR,
          });
        }
      }
    }

    // Left room walls
    for (let x = -1; x <= sideRoomWidth; x++) {
      for (let z = -1; z <= sideRoomDepth; z++) {
        // Floor and ceiling
        changes.push({
          x: leftRoomX + x,
          y: roomY - 1,
          z: leftRoomZ + z,
          blockId: STONE,
        });
        changes.push({
          x: leftRoomX + x,
          y: roomY + mainRoomHeight,
          z: leftRoomZ + z,
          blockId: STONE,
        });

        // Walls
        for (let y = 0; y < mainRoomHeight; y++) {
          if (x === -1 || x === sideRoomWidth || z === -1 || z === sideRoomDepth) {
            // Skip doorway to main room
            if (!(x === sideRoomWidth && z >= sideRoomDepth/2 - 1 && z <= sideRoomDepth/2 + 1 && y < 3)) {
              changes.push({
                x: leftRoomX + x,
                y: roomY + y,
                z: leftRoomZ + z,
                blockId: STONE,
              });
            }
          }
        }
      }
    }

    // Right side room (mirror of left)
    const rightRoomX = roomX + mainRoomWidth/2 + 1;
    const rightRoomZ = roomZ + mainRoomDepth/2 - sideRoomDepth/2;

    // Hollow out right room
    for (let x = 0; x < sideRoomWidth; x++) {
      for (let z = 0; z < sideRoomDepth; z++) {
        for (let y = 0; y < mainRoomHeight; y++) {
          changes.push({
            x: rightRoomX + x,
            y: roomY + y,
            z: rightRoomZ + z,
            blockId: AIR,
          });
        }
      }
    }

    // Right room walls
    for (let x = -1; x <= sideRoomWidth; x++) {
      for (let z = -1; z <= sideRoomDepth; z++) {
        // Floor and ceiling
        changes.push({
          x: rightRoomX + x,
          y: roomY - 1,
          z: rightRoomZ + z,
          blockId: STONE,
        });
        changes.push({
          x: rightRoomX + x,
          y: roomY + mainRoomHeight,
          z: rightRoomZ + z,
          blockId: STONE,
        });

        // Walls
        for (let y = 0; y < mainRoomHeight; y++) {
          if (x === -1 || x === sideRoomWidth || z === -1 || z === sideRoomDepth) {
            // Skip doorway to main room
            if (!(x === -1 && z >= sideRoomDepth/2 - 1 && z <= sideRoomDepth/2 + 1 && y < 3)) {
              changes.push({
                x: rightRoomX + x,
                y: roomY + y,
                z: rightRoomZ + z,
                blockId: STONE,
              });
            }
          }
        }
      }
    }

    console.log('[UndergroundBase] Adding decorative elements...');

    // Add some glass blocks as "lights" in ceiling
    const lightPositions = [
      { x: 0, z: mainRoomDepth/4 },
      { x: 0, z: mainRoomDepth/2 },
      { x: 0, z: 3*mainRoomDepth/4 },
      { x: -mainRoomWidth/4, z: mainRoomDepth/2 },
      { x: mainRoomWidth/4, z: mainRoomDepth/2 },
    ];

    for (const pos of lightPositions) {
      changes.push({
        x: roomX + pos.x,
        y: roomY + mainRoomHeight - 1,
        z: roomZ + pos.z,
        blockId: GLASS,
      });
    }

    console.log(`[UndergroundBase] Total blocks to place: ${changes.length}`);

    // Connect to WebSocket and send all changes
    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[UndergroundBase] Connected to WebSocket server');
        console.log('[UndergroundBase] Sending underground base construction...');

        const message = {
          type: 'apply_block_changes',
          changes: changes,
        };

        ws.send(JSON.stringify(message));
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());

        if (message.type === 'block_changes_applied') {
          console.log(`[UndergroundBase] ✅ Underground base built successfully!`);
          console.log(`[UndergroundBase] Placed ${message.count} blocks`);
          console.log(`[UndergroundBase] Updated ${message.affectedChunks} chunks`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[UndergroundBase] WebSocket error:', error);
        reject(error);
      });

      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 30000);
    });

    ipcClient.disconnect();
    console.log('[UndergroundBase] Done! Your underground base is ready!');
    console.log('[UndergroundBase] Features:');
    console.log('[UndergroundBase] - Entrance stairway at your position');
    console.log('[UndergroundBase] - Large main room (15x15x5)');
    console.log('[UndergroundBase] - Two side rooms (8x8x5 each)');
    console.log('[UndergroundBase] - Stone walls, floor, and ceiling');
    console.log('[UndergroundBase] - Glass lighting blocks in ceiling');
    console.log('[UndergroundBase] - 10 blocks deep underground');
  } catch (error: any) {
    console.error('[UndergroundBase] Error:', error.message);
    process.exit(1);
  }
}

main();
