/**
 * Test script to place a block via WebSocket and verify persistence
 */

import WebSocket from 'ws';

const WS_URL = 'ws://localhost:3011';
const API_URL = 'http://localhost:3011';
const WORLD_ID = 'main';
const USERNAME = 'test-script';

interface Block {
  position: { x: number; y: number; z: number };
  blockTypeId: number;
}

async function testBlockPlacement() {
  console.log('🔵 Connecting to server...');
  const ws = new WebSocket(WS_URL);

  await new Promise<void>((resolve, reject) => {
    ws.on('open', () => {
      console.log('✅ Connected to server');
      resolve();
    });
    ws.on('error', reject);
  });

  // Login
  console.log('🔵 Sending login...');
  const loginMessage = {
    t: 'login',
    i: 'login_1',
    d: {
      username: USERNAME,
      worldId: WORLD_ID,
    },
  };
  ws.send(JSON.stringify(loginMessage));

  // Wait for login response
  await new Promise<void>((resolve) => {
    ws.on('message', (data) => {
      const msg = JSON.parse(data.toString());
      if (msg.t === 'loginResponse') {
        console.log('✅ Login successful');
        resolve();
      }
    });
  });

  // Wait a bit for connection to stabilize
  await new Promise(resolve => setTimeout(resolve, 500));

  // Place a test block via REST API (easier than WebSocket)
  const testBlock: Block = {
    position: {
      x: Math.floor(Math.random() * 10) - 5, // Random x between -5 and 4
      y: 70,
      z: Math.floor(Math.random() * 10) - 5, // Random z between -5 and 4
    },
    blockTypeId: 471, // Some visible block type
  };

  console.log('🔵 Placing block via REST API...', testBlock);

  const response = await fetch(
    `${API_URL}/api/worlds/${WORLD_ID}/blocks/${testBlock.position.x}/${testBlock.position.y}/${testBlock.position.z}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Basic ' + Buffer.from('testuser:testpass').toString('base64'),
      },
      body: JSON.stringify({
        blockTypeId: testBlock.blockTypeId,
      }),
    }
  );

  if (response.ok) {
    console.log('✅ Block placed successfully');
    console.log('   Position:', testBlock.position);
    console.log('   BlockTypeId:', testBlock.blockTypeId);
  } else {
    console.error('❌ Failed to place block:', response.status, await response.text());
  }

  // Wait for broadcast message
  console.log('🔵 Waiting for block update broadcast (1 second)...');
  await new Promise(resolve => setTimeout(resolve, 1500));

  let receivedUpdate = false;
  ws.on('message', (data) => {
    const msg = JSON.parse(data.toString());
    if (msg.t === 'b.u') {
      console.log('✅ Received block update broadcast:', msg.d);
      receivedUpdate = true;
    }
  });

  // Wait for auto-save
  console.log('🔵 Waiting for auto-save (30 seconds)...');
  await new Promise(resolve => setTimeout(resolve, 30000));

  console.log('\n📊 Test Summary:');
  console.log('   Block placed:', testBlock.position);
  console.log('   Broadcast received:', receivedUpdate);
  console.log('\n💡 Next steps:');
  console.log('   1. Check server logs for "Block set" and "Saving dirty chunk"');
  console.log('   2. Stop the server (Ctrl+C)');
  console.log('   3. Check if chunk file contains the block:');
  console.log(`      grep -A 5 '"x": ${testBlock.position.x}' data/worlds/main/chunks/*.json`);
  console.log('   4. Restart server and verify block is loaded');

  ws.close();
  process.exit(0);
}

testBlockPlacement().catch((error) => {
  console.error('❌ Test failed:', error);
  process.exit(1);
});
