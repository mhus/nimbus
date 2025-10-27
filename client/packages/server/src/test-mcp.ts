#!/usr/bin/env node
/**
 * Test script to create a test block near the player
 */

import { VoxelServer } from './VoxelServer.js';

async function main() {
  console.log('[Test] Initializing Voxel server...');

  // Create a minimal Voxel server instance
  const config = {
    port: 3000,
    worldName: 'test_world',
    worldSeed: 12345,
    generator: 'flat' as const,
  };

  const voxelServer = new VoxelServer(config);

  // Load the existing world
  console.log('[Test] Loading existing world "main"...');
  const world = await voxelServer.worldManager.load('main');

  if (!world) {
    console.error('[Test] Failed to load world "main"');
    console.log('[Test] Available worlds:', Array.from(voxelServer.worldManager.getAllWorlds().keys()));
    process.exit(1);
  }

  console.log('[Test] World loaded successfully');
  console.log('[Test] Registry has', voxelServer.registry.getAllBlocks().size, 'blocks');

  // Step 1: Get player position
  console.log('\n[Step 1] Getting player position...');
  const entities = voxelServer.entityManager.getAll();
  let playerEntity = null;

  for (const entity of entities.values()) {
    if (entity.type === 'player') {
      playerEntity = entity;
      break;
    }
  }

  if (!playerEntity) {
    console.warn('[Test] No player found, using default position (0, 65, 0)');
    playerEntity = {
      data: {
        position: [0, 65, 0],
        rotation: 0,
        pitch: 0,
      }
    };
  } else {
    console.log('[Test] Player found at position:', playerEntity.data.position);
  }

  const [px, py, pz] = playerEntity.data.position;

  // Step 2: Create test block type
  console.log('\n[Step 2] Creating test block type...');
  const testBlockName = 'test_glowing_cube';
  const testBlockDisplayName = 'Test Glowing Cube';

  voxelServer.registry.addBlock({
    name: testBlockName,
    displayName: testBlockDisplayName,
    shape: 0, // CUBE
    texture: 'stone.png',
    solid: true,
    transparent: false,
    hardness: 1.5,
    miningtime: 1500,
    tool: 'any',
    unbreakable: false,
  });

  // Finalize registry to assign block IDs
  voxelServer.registry.finalize();

  const blockType = voxelServer.registry.getBlock(testBlockName);
  console.log('[Test] Created block:', blockType?.displayName);
  console.log('[Test] Block ID after finalize:', blockType?.id);

  // Step 3: Place test block near player
  console.log('\n[Step 3] Placing test block near player...');

  // Place block 3 blocks in front of player (assuming player faces +Z)
  const blockX = Math.floor(px + 3);
  const blockY = Math.floor(py);
  const blockZ = Math.floor(pz);

  const blockId = voxelServer.registry.getBlockID(testBlockName);

  if (blockId === undefined) {
    console.error('[Test] Failed to get block ID');
    process.exit(1);
  }

  await world.setBlock([blockX, blockY, blockZ], blockId);

  console.log(`[Test] Placed ${testBlockDisplayName} at (${blockX}, ${blockY}, ${blockZ})`);
  console.log('[Test] Block ID:', blockId);

  // Verify placement
  const placedBlockId = await world.getBlock([blockX, blockY, blockZ]);
  const placedBlockType = voxelServer.registry.getBlockByID(placedBlockId);

  console.log('\n[Verification]');
  console.log('Block at position:', placedBlockType?.displayName || 'Unknown');
  console.log('Block ID:', placedBlockId);

  console.log('\n[Test] Success! Test block created near player.');
}

main().catch((error) => {
  console.error('[Test] Error:', error);
  process.exit(1);
});
