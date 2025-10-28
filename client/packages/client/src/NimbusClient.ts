/**
 * NimbusClient - Main entry point for Nimbus 3D Voxel Engine Client
 *
 * Build variants:
 * - Viewer: Read-only 3D engine for viewing worlds
 * - Editor: Full 3D engine + editor functions + console
 *
 * Unreachable code is eliminated by the bundler based on __EDITOR__ and __VIEWER__ flags
 */

import {
  SHARED_VERSION,
  getLogger,
  LoggerFactory,
  LogLevel,
  FileLogTransport,
  ExceptionHandler,
} from '@nimbus/shared';

const CLIENT_VERSION = '2.0.0';

// Initialize logger
const logger = getLogger('NimbusClient');

// Configure logging from environment
LoggerFactory.configureFromEnv();

// Set appropriate log level for build mode
if (import.meta.env.PROD) {
  // Production: only warnings and errors
  LoggerFactory.setDefaultLevel(LogLevel.WARN);
} else if (__EDITOR__) {
  // Editor development: verbose logging
  LoggerFactory.setDefaultLevel(LogLevel.DEBUG);
} else {
  // Viewer development: normal logging
  LoggerFactory.setDefaultLevel(LogLevel.INFO);
}

// Development: Optional file logging (prompts user for file location)
if (import.meta.env.DEV && FileLogTransport.isFileSystemAPISupported()) {
  // Uncomment to enable file logging in development:
  /*
  const fileTransport = new FileLogTransport({
    filename: __EDITOR__ ? 'nimbus-editor.log' : 'nimbus-viewer.log',
    maxSizeMB: 5,
    flushIntervalMs: 1000,
  });

  fileTransport
    .initialize()
    .then(() => {
      LoggerFactory.configure({
        transports: [fileTransport.transport],
      });
      logger.info('File logging enabled');
    })
    .catch((error) => {
      ExceptionHandler.handle(error, 'NimbusClient.fileTransportInit');
      logger.warn('File logging not available', error);
    });
  */
}

// Build mode info
const buildMode = __EDITOR__ ? 'Editor' : 'Viewer';
logger.info(`Nimbus Client v${CLIENT_VERSION} (${buildMode} Build)`);
logger.info(`Shared Library v${SHARED_VERSION}`);
logger.debug(`Build Mode: ${__BUILD_MODE__}`);

// TODO: Initialize AppContext
// TODO: Initialize Services
// TODO: Show StartScreen
// TODO: After world selection, initialize Engine

// Editor-specific initialization (tree-shaken in viewer build)
if (__EDITOR__) {
  logger.info('Editor mode: Initializing editor functions...');
  // TODO: Initialize EditorService
  // TODO: Initialize CommandConsole
  // TODO: Load editor UI components
}

// Main initialization
try {
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

  logger.info('Nimbus Client initialized successfully');
} catch (error) {
  ExceptionHandler.handle(error, 'NimbusClient.init');
  logger.fatal('Failed to initialize client', undefined, error as Error);
  throw error;
}
