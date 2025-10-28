/**
 * NimbusClient - Main entry point for Nimbus 3D Voxel Engine Client (Viewer)
 *
 * This is the viewer build - read-only 3D engine for viewing worlds
 */

import { SHARED_VERSION } from '@nimbus/shared';

const CLIENT_VERSION = '2.0.0';

console.log(`Nimbus Client v${CLIENT_VERSION} (Shared v${SHARED_VERSION})`);
console.log('Initializing viewer...');

// TODO: Initialize AppContext
// TODO: Initialize Services
// TODO: Show StartScreen
// TODO: After world selection, initialize Engine

// Hide loading screen
const loadingElement = document.getElementById('loading');
if (loadingElement) {
  loadingElement.classList.add('hidden');
}

// Show info message
const canvas = document.getElementById('renderCanvas') as HTMLCanvasElement;
if (canvas) {
  const ctx = canvas.getContext('2d');
  if (ctx) {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    ctx.fillStyle = '#1a1a1a';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#ffffff';
    ctx.font = '24px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(
      `Nimbus Client v${CLIENT_VERSION}`,
      canvas.width / 2,
      canvas.height / 2 - 20
    );
    ctx.font = '16px sans-serif';
    ctx.fillStyle = '#888888';
    ctx.fillText(
      'Client structure created - Ready for implementation',
      canvas.width / 2,
      canvas.height / 2 + 20
    );
  }
}
