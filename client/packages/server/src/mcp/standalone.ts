#!/usr/bin/env node
/**
 * Standalone Nimbus MCP Server
 * Can be run independently to provide MCP tools without starting the full Voxel server
 * Now supports IPC connection to live VoxelServer for real-time player data
 */

import { NimbusMCPServer } from './NimbusMCPServer.js';
import { VoxelServer } from '../VoxelServer.js';
import { IPCClient } from '../ipc/IPCClient.js';

// Create a minimal Voxel server instance for the MCP server to use
const config = {
  port: 3000, // Won't actually listen, just for initialization
  worldName: 'mcp_world',
  worldSeed: 12345,
  generator: 'flat' as const,
};

const voxelServer = new VoxelServer(config);

// Load or create main world
let world = await voxelServer.worldManager.load('main');

if (!world) {
  world = await voxelServer.worldManager.create({
    name: 'main',
    seed: config.worldSeed,
    generator: config.generator,
    chunkSize: 32,
    worldHeight: 256,
  });
}

if (!world) {
  console.error('[MCP Standalone] Failed to load or create world');
  process.exit(1);
}

console.log('[MCP Standalone] Initialized Nimbus MCP server');
console.log('[MCP Standalone] Registry has', voxelServer.registry.getAllBlocks().size, 'blocks');

// Try to connect to live VoxelServer via IPC
const ipcClient = new IPCClient();
try {
  await ipcClient.connect();
  console.log('[MCP Standalone] Connected to live VoxelServer via IPC');
  console.log('[MCP Standalone] Will use live player data from running server');
} catch (error) {
  console.log('[MCP Standalone] No live VoxelServer found, running in standalone mode');
  console.log('[MCP Standalone] Player position will use default values');
}

// Create and start MCP server (pass IPC client for live data access)
const mcpServer = new NimbusMCPServer(voxelServer, ipcClient.isConnected() ? ipcClient : undefined);

mcpServer.start().catch((error) => {
  console.error('[MCP Standalone] Failed to start:', error);
  process.exit(1);
});

// Handle shutdown
process.on('SIGINT', async () => {
  console.log('\n[MCP Standalone] Shutting down...');
  await mcpServer.stop();
  if (ipcClient.isConnected()) {
    ipcClient.disconnect();
  }
  process.exit(0);
});

process.on('SIGTERM', async () => {
  console.log('\n[MCP Standalone] Shutting down...');
  await mcpServer.stop();
  if (ipcClient.isConnected()) {
    ipcClient.disconnect();
  }
  process.exit(0);
});
