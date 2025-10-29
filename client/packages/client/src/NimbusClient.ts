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
import { NetworkService } from './services/NetworkService';
import { BlockTypeService } from './services/BlockTypeService';
import { ShaderService } from './services/ShaderService';
import { ChunkService } from './services/ChunkService';
import { EngineService } from './services/EngineService';

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

/**
 * Initialize core services (Network, BlockType, Shader, Chunk)
 */
async function initializeCoreServices(appContext: AppContext): Promise<void> {
  try {
    logger.info('Initializing core services...');

    // Initialize NetworkService
    logger.info('Initializing NetworkService...');
    const networkService = new NetworkService(appContext);
    appContext.services.network = networkService;

    // Connect to server
    logger.info('Connecting to server...');
    await networkService.connect();
    logger.info('Connected to server');

    // Wait for login response and world info
    await new Promise<void>((resolve) => {
      networkService.once('login:success', () => {
        logger.info('Login successful');
        resolve();
      });
    });

    // Initialize BlockTypeService
    logger.info('Initializing BlockTypeService...');
    const blockTypeService = new BlockTypeService(appContext);
    appContext.services.blockType = blockTypeService;

    // Load block types
    logger.info('Loading block types...');
    await blockTypeService.loadBlockTypes();
    logger.info('Block types loaded', { count: blockTypeService.getBlockTypeCount() });

    // Initialize ShaderService
    logger.info('Initializing ShaderService...');
    const shaderService = new ShaderService(appContext);
    appContext.services.shader = shaderService;

    // Initialize ChunkService
    logger.info('Initializing ChunkService...');
    const chunkService = new ChunkService(networkService, appContext);
    appContext.services.chunk = chunkService;

    logger.info('Core services initialized');
  } catch (error) {
    throw ExceptionHandler.handleAndRethrow(error, 'NimbusClient.initializeCoreServices');
  }
}

/**
 * Initialize 3D engine
 */
async function initializeEngine(appContext: AppContext, canvas: HTMLCanvasElement): Promise<void> {
  try {
    logger.info('Initializing 3D Engine...');

    // Create EngineService
    const engineService = new EngineService(appContext, canvas);
    appContext.services.engine = engineService;

    // Initialize engine (loads textures, creates scene, etc.)
    await engineService.initialize();
    logger.info('Engine initialized');

    // Start render loop
    engineService.startRenderLoop();
    logger.info('Render loop started');

    // Register some chunks around player spawn
    const chunkService = appContext.services.chunk;
    if (chunkService) {
      const playerService = engineService.getPlayerService();
      const playerPos = playerService?.getPosition();

      if (playerPos) {
        logger.info('Registering chunks around player', {
          x: playerPos.x,
          y: playerPos.y,
          z: playerPos.z
        });
        chunkService.updateChunksAroundPosition(playerPos.x, playerPos.z);
      }
    }

    logger.info('3D Engine ready');
  } catch (error) {
    throw ExceptionHandler.handleAndRethrow(error, 'NimbusClient.initializeEngine');
  }
}

// Initialize application
const appContextPromise = initializeApp();

// Main initialization
appContextPromise
  .then(async (appContext) => {
    logger.info('AppContext ready', {
      hasConfig: !!appContext.config,
      hasClientService: !!appContext.services.client,
    });

    // Get canvas
    const canvas = document.getElementById('renderCanvas') as HTMLCanvasElement;
    if (!canvas) {
      throw new Error('Canvas element not found');
    }

    // Show loading message
    showLoadingMessage(canvas, 'Connecting to server...');

    try {
      // Initialize core services (Network, BlockType, Chunk)
      await initializeCoreServices(appContext);

      // Show progress
      showLoadingMessage(canvas, 'Initializing 3D engine...');

      // Initialize 3D engine
      await initializeEngine(appContext, canvas);

      // Hide loading screen
      const loadingElement = document.getElementById('loading');
      if (loadingElement) {
        loadingElement.classList.add('hidden');
      }

      logger.info('Nimbus Client ready!');
    } catch (error) {
      throw error; // Re-throw to outer catch
    }

    // Editor-specific initialization (tree-shaken in viewer build)
    if (__EDITOR__) {
      logger.info('Editor mode active');
      // TODO: Initialize EditorService
      // TODO: Initialize CommandConsole
      // TODO: Load editor UI components
    }

    logger.info('Nimbus Client initialized successfully');
  })
  .catch((error) => {
    ExceptionHandler.handle(error, 'NimbusClient.main');
    logger.fatal('Failed to initialize client', undefined, error as Error);

    // Show error on canvas
    const canvas = document.getElementById('renderCanvas') as HTMLCanvasElement;
    if (canvas) {
      showErrorMessage(canvas, error instanceof Error ? error.message : 'Unknown error');
    }
  });

/**
 * Show loading message on canvas
 */
function showLoadingMessage(canvas: HTMLCanvasElement, message: string): void {
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
    ctx.fillStyle = '#4a9eff';
    ctx.fillText(message, canvas.width / 2, canvas.height / 2 + 10);
  }
}

/**
 * Show error message on canvas
 */
function showErrorMessage(canvas: HTMLCanvasElement, message: string): void {
  const ctx = canvas.getContext('2d');
  if (ctx) {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    ctx.fillStyle = '#1a1a1a';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#ff4444';
    ctx.font = '24px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('Initialization Error', canvas.width / 2, canvas.height / 2 - 20);
    ctx.font = '16px sans-serif';
    ctx.fillStyle = '#ffaaaa';
    ctx.fillText(message, canvas.width / 2, canvas.height / 2 + 10);
  }
}
