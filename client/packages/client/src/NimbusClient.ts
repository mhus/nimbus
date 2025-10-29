/**
 * NimbusClient - Main entry point for Nimbus 3D Voxel Engine Client
 *
 * Build variants:
 * - Viewer: Read-only 3D engine for viewing worlds
 * - Editor: Full 3D engine + editor functions + console
 *
 * Unreachable code is eliminated by the bundler based on __EDITOR__ and __VIEWER__ flags
 */

import { SHARED_VERSION, getLogger, ExceptionHandler } from '@nimbus/shared';
import { loadClientConfig } from './config/ClientConfig';
import { ClientService } from './services/ClientService';
import { createAppContext } from './AppContext';
import type { AppContext } from './AppContext';

const CLIENT_VERSION = '2.0.0';

// Initialize logger (basic setup before ClientService)
const logger = getLogger('NimbusClient');

// Build mode info
const buildMode = __EDITOR__ ? 'Editor' : 'Viewer';

/**
 * Initialize application
 */
async function initializeApp(): Promise<AppContext> {
  try {
    logger.info(`Nimbus Client v${CLIENT_VERSION} (${buildMode} Build)`);
    logger.info(`Shared Library v${SHARED_VERSION}`);
    logger.debug(`Build Mode: ${__BUILD_MODE__}`);

    // Load client configuration
    logger.info('Loading client configuration...');
    const config = loadClientConfig();

    // Create ClientService
    logger.info('Initializing ClientService...');
    const clientService = new ClientService(config);

    // Setup logger with proper transports
    clientService.setupLogger();

    // Create AppContext
    logger.info('Creating AppContext...');
    const appContext = createAppContext(config, clientService);

    logger.info('App initialization complete', {
      clientType: clientService.getClientType(),
      isEditor: clientService.isEditor(),
      isDevMode: clientService.isDevMode(),
    });

    return appContext;
  } catch (error) {
    throw ExceptionHandler.handleAndRethrow(error, 'NimbusClient.initializeApp');
  }
}

// Initialize application
const appContextPromise = initializeApp();

// TODO: Show StartScreen
// TODO: After world selection, initialize Engine

// Main initialization
appContextPromise
  .then((appContext) => {
    logger.info('AppContext ready', {
      hasConfig: !!appContext.config,
      hasClientService: !!appContext.services.client,
    });

    // Editor-specific initialization (tree-shaken in viewer build)
    if (__EDITOR__) {
      logger.info('Editor mode active');
      // TODO: Initialize EditorService
      // TODO: Initialize CommandConsole
      // TODO: Load editor UI components
    }

    // Hide loading screen
    const loadingElement = document.getElementById('loading');
    if (loadingElement) {
      loadingElement.classList.add('hidden');
    }

    // Show info message on canvas
    const canvas = document.getElementById(
      'renderCanvas'
    ) as HTMLCanvasElement;
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
          canvas.height / 2 - 60
        );
        ctx.font = '16px sans-serif';
        ctx.fillStyle = '#888888';
        ctx.fillText(
          'AppContext & ClientService initialized',
          canvas.width / 2,
          canvas.height / 2 - 30
        );
        ctx.fillStyle = '#4a9eff';
        ctx.fillText(
          `Build Mode: ${__BUILD_MODE__}`,
          canvas.width / 2,
          canvas.height / 2
        );
        ctx.fillStyle = '#66cc66';
        ctx.fillText(
          `Client Type: ${appContext.services.client.getClientType()}`,
          canvas.width / 2,
          canvas.height / 2 + 30
        );
        if (__EDITOR__) {
          ctx.fillStyle = '#44ff44';
          ctx.fillText(
            'Editor features enabled',
            canvas.width / 2,
            canvas.height / 2 + 60
          );
        }
      }
    }

    logger.info('Nimbus Client initialized successfully');
  })
  .catch((error) => {
    ExceptionHandler.handle(error, 'NimbusClient.main');
    logger.fatal('Failed to initialize client', undefined, error as Error);

    // Show error on canvas
    const canvas = document.getElementById(
      'renderCanvas'
    ) as HTMLCanvasElement;
    if (canvas) {
      const ctx = canvas.getContext('2d');
      if (ctx) {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
        ctx.fillStyle = '#1a1a1a';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = '#ff4444';
        ctx.font = '24px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(
          'Initialization Error',
          canvas.width / 2,
          canvas.height / 2 - 20
        );
        ctx.font = '16px sans-serif';
        ctx.fillStyle = '#ffaaaa';
        ctx.fillText(
          error instanceof Error ? error.message : 'Unknown error',
          canvas.width / 2,
          canvas.height / 2 + 10
        );
      }
    }
  });
