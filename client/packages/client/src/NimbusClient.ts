/**
 * NimbusClient - Main entry point for Nimbus 3D Voxel Engine Client
 *
 * Build variants:
 * - Viewer: Read-only 3D engine for viewing worlds
 * - Editor: Full 3D engine + editor functions + console
 *
 * Unreachable code is eliminated by the bundler based on __EDITOR__ and __VIEWER__ flags
 */

import { SHARED_VERSION } from '@nimbus/shared';

const CLIENT_VERSION = '2.0.0';

// Build mode info
const buildMode = __EDITOR__ ? 'Editor' : 'Viewer';
console.log(`Nimbus Client v${CLIENT_VERSION} (${buildMode} Build)`);
console.log(`Shared Library v${SHARED_VERSION}`);
console.log(`Build Mode: ${__BUILD_MODE__}`);

// TODO: Initialize AppContext
// TODO: Initialize Services
// TODO: Show StartScreen
// TODO: After world selection, initialize Engine

// Editor-specific initialization (tree-shaken in viewer build)
if (__EDITOR__) {
  console.log('Editor mode: Initializing editor functions...');
  // TODO: Initialize EditorService
  // TODO: Initialize CommandConsole
  // TODO: Load editor UI components
}

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
      `Nimbus Client v${CLIENT_VERSION} (${buildMode})`,
      canvas.width / 2,
      canvas.height / 2 - 40
    );
    ctx.font = '16px sans-serif';
    ctx.fillStyle = '#888888';
    ctx.fillText(
      'Client structure created - Ready for implementation',
      canvas.width / 2,
      canvas.height / 2 - 10
    );
    ctx.fillStyle = '#4a9eff';
    ctx.fillText(
      `Build Mode: ${__BUILD_MODE__}`,
      canvas.width / 2,
      canvas.height / 2 + 20
    );
    if (__EDITOR__) {
      ctx.fillStyle = '#44ff44';
      ctx.fillText(
        'Editor features enabled',
        canvas.width / 2,
        canvas.height / 2 + 50
      );
    }
  }
}
