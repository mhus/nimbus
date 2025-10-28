/**
 * NimbusServer - Test server for Nimbus Client development
 *
 * This is a simple server implementation to provide world data to the client.
 * The focus is on client development, not server features.
 */

import { SHARED_VERSION } from '@nimbus/shared';

const SERVER_VERSION = '2.0.0';
const PORT = process.env.PORT || 3000;

console.log(`Nimbus Server v${SERVER_VERSION} (Shared v${SHARED_VERSION})`);
console.log('Starting test server...');

// TODO: Initialize Express app
// TODO: Setup REST API endpoints (/worlds, /login, etc.)
// TODO: Setup WebSocket server
// TODO: Initialize world generation
// TODO: Implement chunk management

console.log(`Server will listen on port ${PORT}`);
console.log('Server structure created - Ready for implementation');

// Keep process alive
process.on('SIGINT', () => {
  console.log('\nShutting down server...');
  process.exit(0);
});
