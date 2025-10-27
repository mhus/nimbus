/**
 * VoxelSrv Server Entry Point
 */

import { VoxelServer } from './VoxelServer.js';

const server = new VoxelServer({
  port: 3003,
  worldName: 'main',
  worldSeed: 12345,
  generator: 'flat',  // 'flat' or 'normal'
  enableMCP: true,  // Enable MCP server integration
  enableIPC: true,  // Enable IPC server for external communication
});

server.start().catch((error) => {
  console.error('[Server] Failed to start:', error);
  process.exit(1);
});

// Graceful shutdown
process.on('SIGINT', async () => {
  console.log('\n[Server] Shutting down...');
  await server.stop();
  process.exit(0);
});

process.on('SIGTERM', async () => {
  console.log('\n[Server] Shutting down...');
  await server.stop();
  process.exit(0);
});
