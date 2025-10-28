#!/usr/bin/env node
/**
 * Build a large statue of Donald Trump at player position
 */

import { IPCClient } from './ipc/IPCClient.js';
import { WebSocket } from 'ws';

const ipcClient = new IPCClient();

async function main() {
  try {
    await ipcClient.connect();
    console.log('[TrumpStatue] Connected to VoxelServer via IPC');

    const playerData = await ipcClient.getPlayerPosition('main');

    if (!playerData) {
      console.error('[TrumpStatue] No player found in world "main"');
      process.exit(1);
    }

    const baseX = Math.floor(playerData.position.x);
    const baseY = Math.floor(playerData.position.y);
    const baseZ = Math.floor(playerData.position.z);

    console.log('[TrumpStatue] Building Trump statue at:', { baseX, baseY, baseZ });

    // Block IDs
    const STONE = 2;      // Base/pedestal
    const PLANKS = 4;     // Suit (body)
    const GOLD = 41;      // Hair (if available, otherwise use planks)
    const WOOL_ORANGE = 35; // Skin tone (orange wool if available)
    const WOOL_RED = 14;  // Tie
    const WOOL_WHITE = 0; // Shirt (or use stone)
    const GLASS = 20;     // Eyes

    const changes: any[] = [];

    // Use available blocks - adjust based on what's in registry
    const HAIR = GOLD;     // Golden hair
    const SKIN = PLANKS;   // Light wood for skin
    const SUIT = 2;        // Stone for dark suit
    const TIE = 14;        // Red wool for tie (if available, else stone)
    const EYES = GLASS;    // Glass for eyes

    console.log('[TrumpStatue] Building pedestal...');

    // Pedestal (5x5 base, 3 blocks tall)
    for (let y = 0; y < 3; y++) {
      for (let x = -2; x <= 2; x++) {
        for (let z = -2; z <= 2; z++) {
          changes.push({
            x: baseX + x,
            y: baseY + y,
            z: baseZ + z,
            blockId: STONE,
          });
        }
      }
    }

    const statueBaseY = baseY + 3;

    console.log('[TrumpStatue] Building legs...');

    // Legs (2 columns, 4 blocks tall)
    for (let y = 0; y < 4; y++) {
      // Left leg
      changes.push({
        x: baseX - 1,
        y: statueBaseY + y,
        z: baseZ,
        blockId: SUIT,
      });
      // Right leg
      changes.push({
        x: baseX + 1,
        y: statueBaseY + y,
        z: baseZ,
        blockId: SUIT,
      });
    }

    console.log('[TrumpStatue] Building body...');

    const bodyY = statueBaseY + 4;

    // Body/Torso (3x2x5 blocks)
    for (let y = 0; y < 5; y++) {
      for (let x = -1; x <= 1; x++) {
        for (let z = -1; z <= 0; z++) {
          // Suit body
          if (x === -1 || x === 1 || z === -1) {
            changes.push({
              x: baseX + x,
              y: bodyY + y,
              z: baseZ + z,
              blockId: SUIT,
            });
          }
          // Tie in center front
          else if (x === 0 && z === 0 && y < 4) {
            changes.push({
              x: baseX + x,
              y: bodyY + y,
              z: baseZ + z,
              blockId: 14, // Red tie
            });
          }
        }
      }
    }

    console.log('[TrumpStatue] Building arms...');

    const armY = bodyY + 2;

    // Left arm (extending outward)
    for (let x = -2; x >= -4; x--) {
      changes.push({
        x: baseX + x,
        y: armY,
        z: baseZ,
        blockId: SUIT,
      });
      changes.push({
        x: baseX + x,
        y: armY + 1,
        z: baseZ,
        blockId: SUIT,
      });
    }

    // Right arm (extending outward)
    for (let x = 2; x <= 4; x++) {
      changes.push({
        x: baseX + x,
        y: armY,
        z: baseZ,
        blockId: SUIT,
      });
      changes.push({
        x: baseX + x,
        y: armY + 1,
        z: baseZ,
        blockId: SUIT,
      });
    }

    // Hands
    changes.push({
      x: baseX - 4,
      y: armY - 1,
      z: baseZ,
      blockId: SKIN,
    });
    changes.push({
      x: baseX + 4,
      y: armY - 1,
      z: baseZ,
      blockId: SKIN,
    });

    console.log('[TrumpStatue] Building neck...');

    const neckY = bodyY + 5;

    // Neck (1 block)
    changes.push({
      x: baseX,
      y: neckY,
      z: baseZ,
      blockId: SKIN,
    });

    console.log('[TrumpStatue] Building head...');

    const headY = neckY + 1;

    // Head (3x3x3 blocks)
    for (let y = 0; y < 3; y++) {
      for (let x = -1; x <= 1; x++) {
        for (let z = -1; z <= 1; z++) {
          // Face (front side)
          if (z === 0 && y < 2) {
            // Eyes
            if (y === 1 && (x === -1 || x === 1)) {
              changes.push({
                x: baseX + x,
                y: headY + y,
                z: baseZ + z,
                blockId: EYES,
              });
            }
            // Nose
            else if (y === 1 && x === 0) {
              changes.push({
                x: baseX + x,
                y: headY + y,
                z: baseZ + z + 1,
                blockId: SKIN,
              });
            }
            // Rest of face
            else {
              changes.push({
                x: baseX + x,
                y: headY + y,
                z: baseZ + z,
                blockId: SKIN,
              });
            }
          }
          // Back and sides of head
          else if (!(z === 0 && y < 2)) {
            changes.push({
              x: baseX + x,
              y: headY + y,
              z: baseZ + z,
              blockId: SKIN,
            });
          }
        }
      }
    }

    console.log('[TrumpStatue] Building hair...');

    const hairY = headY + 2;

    // Iconic hair (swept forward style)
    for (let x = -2; x <= 2; x++) {
      for (let z = -2; z <= 1; z++) {
        // Top of head hair
        changes.push({
          x: baseX + x,
          y: hairY,
          z: baseZ + z,
          blockId: HAIR,
        });

        // Extra hair volume
        if (Math.abs(x) <= 1 && z >= -1) {
          changes.push({
            x: baseX + x,
            y: hairY + 1,
            z: baseZ + z,
            blockId: HAIR,
          });
        }

        // Swept forward
        if (x === 0 && z === 1) {
          changes.push({
            x: baseX + x,
            y: hairY + 1,
            z: baseZ + z + 1,
            blockId: HAIR,
          });
        }
      }
    }

    console.log(`[TrumpStatue] Total blocks to place: ${changes.length}`);

    const ws = new WebSocket('ws://localhost:3003');

    await new Promise((resolve, reject) => {
      ws.on('open', () => {
        console.log('[TrumpStatue] Connected to WebSocket server');
        console.log('[TrumpStatue] Sending statue construction...');

        const message = {
          type: 'apply_block_changes',
          changes: changes,
        };

        ws.send(JSON.stringify(message));
      });

      ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());

        if (message.type === 'block_changes_applied') {
          console.log(`[TrumpStatue] ✅ Trump statue built successfully!`);
          console.log(`[TrumpStatue] Placed ${message.count} blocks`);
          console.log(`[TrumpStatue] Updated ${message.affectedChunks} chunks`);
          ws.close();
          resolve(true);
        }
      });

      ws.on('error', (error) => {
        console.error('[TrumpStatue] WebSocket error:', error);
        reject(error);
      });

      setTimeout(() => {
        reject(new Error('Timeout waiting for server response'));
      }, 30000);
    });

    ipcClient.disconnect();
    console.log('[TrumpStatue] Done! The Trump statue is complete!');
    console.log('[TrumpStatue] Features:');
    console.log('[TrumpStatue] - Stone pedestal');
    console.log('[TrumpStatue] - Suit and tie');
    console.log('[TrumpStatue] - Extended arms in power pose');
    console.log('[TrumpStatue] - Characteristic hair style');
    console.log('[TrumpStatue] - Approximately 25 blocks tall');
  } catch (error: any) {
    console.error('[TrumpStatue] Error:', error.message);
    process.exit(1);
  }
}

main();
