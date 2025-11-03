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
import { DEFAULT_PLAYER_INFO } from './config/DefaultPlayerInfo';
import { ClientService } from './services/ClientService';
import { createAppContext } from './AppContext';
import type { AppContext } from './AppContext';
import { NetworkService } from './services/NetworkService';
import { BlockTypeService } from './services/BlockTypeService';
import { ShaderService } from './services/ShaderService';
import { ChunkService } from './services/ChunkService';
import { EngineService } from './services/EngineService';
import { ModalService } from './services/ModalService';
import { NotificationService } from './services/NotificationService';
import { CommandService } from './services/CommandService';
import { LoginMessageHandler } from './network/handlers/LoginMessageHandler';
import { ChunkMessageHandler } from './network/handlers/ChunkMessageHandler';
import { BlockUpdateHandler } from './network/handlers/BlockUpdateHandler';
import { PingMessageHandler } from './network/handlers/PingMessageHandler';
import { CommandMessageHandler } from './network/handlers/CommandMessageHandler';
import { CommandResultHandler } from './network/handlers/CommandResultHandler';
import { ServerCommandHandler } from './network/handlers/ServerCommandHandler';
import { HelpCommand } from './commands/HelpCommand';
import { InfoCommand } from './commands/InfoCommand';
import { ClearCommand } from './commands/ClearCommand';
import { SendCommand } from './commands/SendCommand';
import { NotificationCommand } from './commands/NotificationCommand';
import { SetPlayerInfoCommand } from './commands/SetPlayerInfoCommand';
import { OpenComponentCommand } from './commands/OpenComponentCommand';
import { SetSelectedEditBlockCommand } from './commands/SetSelectedEditBlockCommand';
import { GetSelectedEditBlockCommand } from './commands/GetSelectedEditBlockCommand';
import {
  WindDirectionCommand,
  WindStrengthCommand,
  WindGustStrengthCommand,
  WindSwayFactorCommand,
} from './commands/wind';

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

    // Initialize PlayerInfo with defaults
    // This can later be overridden by server configuration
    appContext.playerInfo = { ...DEFAULT_PLAYER_INFO };
    logger.info('PlayerInfo initialized', {
      displayName: appContext.playerInfo.displayName,
      baseWalkSpeed: appContext.playerInfo.baseWalkSpeed,
      effectiveWalkSpeed: appContext.playerInfo.effectiveWalkSpeed,
    });

    // Initialize ModalService (no dependencies, UI-only)
    logger.info('Initializing ModalService...');
    const modalService = new ModalService(appContext);
    appContext.services.modal = modalService;
    logger.debug('ModalService initialized');

    // Initialize NotificationService (no dependencies, UI-only)
    logger.info('Initializing NotificationService...');
    const notificationService = new NotificationService(appContext);
    appContext.services.notification = notificationService;
    logger.debug('NotificationService initialized');

    // Initialize CommandService (available in both EDITOR and VIEWER modes)
    logger.info('Initializing CommandService...');
    const commandService = new CommandService(appContext);
    appContext.services.command = commandService;

    // Register command handlers
    commandService.registerHandler(new HelpCommand(commandService));
    commandService.registerHandler(new InfoCommand(appContext));
    commandService.registerHandler(new ClearCommand());
    commandService.registerHandler(new SendCommand(commandService));
    commandService.registerHandler(new NotificationCommand(appContext));
    commandService.registerHandler(new SetPlayerInfoCommand(appContext));
    commandService.registerHandler(new OpenComponentCommand(appContext));
    commandService.registerHandler(new SetSelectedEditBlockCommand(appContext));
    commandService.registerHandler(new GetSelectedEditBlockCommand(appContext));

    // Register wind commands
    commandService.registerHandler(new WindDirectionCommand(appContext));
    commandService.registerHandler(new WindStrengthCommand(appContext));
    commandService.registerHandler(new WindGustStrengthCommand(appContext));
    commandService.registerHandler(new WindSwayFactorCommand(appContext));

    logger.debug('CommandService initialized with commands');

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

    // Register message handlers BEFORE connecting
    logger.debug('Registering message handlers...');
    const loginHandler = new LoginMessageHandler(appContext, networkService);
    networkService.registerHandler(loginHandler);

    const pingHandler = new PingMessageHandler(networkService, appContext);
    networkService.registerHandler(pingHandler);

    // Add error handler to prevent unhandled errors
    networkService.on('error', (error) => {
      logger.error('Network error', undefined, error);
    });

    // Connect to server
    logger.info('Connecting to server...');
    await networkService.connect();
    logger.info('Connected to server');

    // Wait for login response and world info
    await new Promise<void>((resolve, reject) => {
      // Add error handler
      networkService.once('login:error', (error) => {
        logger.error('Login failed', undefined, error);
        reject(error);
      });

      networkService.once('login:success', () => {
        logger.info('Login successful');

        // Start ping interval after successful login
        const pingInterval = appContext.worldInfo?.settings?.pingInterval || 30;
        pingHandler.startPingInterval(pingInterval);
        logger.info('Ping interval started', { intervalSeconds: pingInterval });

        resolve();
      });

      // Add timeout
      setTimeout(() => {
        reject(new Error('Login timeout'));
      }, 30000);
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

    // Register ChunkMessageHandler
    const chunkHandler = new ChunkMessageHandler(chunkService);
    networkService.registerHandler(chunkHandler);

    // Register BlockUpdateHandler
    const blockUpdateHandler = new BlockUpdateHandler(chunkService);
    networkService.registerHandler(blockUpdateHandler);
    logger.info('🔵 BlockUpdateHandler registered for message type: b.u');

    // Register CommandMessageHandler and CommandResultHandler
    const commandService = appContext.services.command;
    if (commandService) {
      const commandMessageHandler = new CommandMessageHandler(commandService);
      networkService.registerHandler(commandMessageHandler);
      logger.debug('CommandMessageHandler registered for message type: cmd.msg');

      const commandResultHandler = new CommandResultHandler(commandService);
      networkService.registerHandler(commandResultHandler);
      logger.debug('CommandResultHandler registered for message type: cmd.rs');

      // Register ServerCommandHandler for server -> client commands
      const serverCommandHandler = new ServerCommandHandler(commandService);
      networkService.registerHandler(serverCommandHandler);
      logger.debug('ServerCommandHandler registered for message type: scmd');
    }

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

      // Clear canvas and prepare for WebGL
      // BabylonJS needs a fresh canvas without existing 2D context
      const parent = canvas.parentElement;
      if (parent) {
        const newCanvas = document.createElement('canvas');
        newCanvas.id = 'renderCanvas';
        newCanvas.width = window.innerWidth;
        newCanvas.height = window.innerHeight;
        newCanvas.style.width = '100%';
        newCanvas.style.height = '100%';
        parent.replaceChild(newCanvas, canvas);

        logger.debug('Canvas replaced for WebGL initialization');

        // Initialize 3D engine with new canvas
        await initializeEngine(appContext, newCanvas);
      } else {
        throw new Error('Canvas has no parent element');
      }

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

      // Expose commands to browser console
      const commandService = appContext.services.command;
      if (commandService) {
        commandService.exposeToBrowserConsole();
      }

      // TODO: Initialize EditorService
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

/**
 * Test functions for NotificationService
 * Call from browser console:
 * - testNotifications() - Test all notification types
 * - testSystemNotifications() - Test system area
 * - testChatNotifications() - Test chat area
 * - testOverlayNotifications() - Test overlay area
 * - testQuestNotifications() - Test quest area
 */

// Make test functions globally available
(window as any).testNotifications = () => {
  appContextPromise.then((appContext) => {
    const ns = appContext.services.notification;
    if (!ns) {
      console.error('NotificationService not initialized');
      return;
    }

    logger.info('Testing all notification types...');

    // System notifications
    ns.newNotification(0, null, 'System Info: Client initialized');
    setTimeout(() => ns.newNotification(1, null, 'System Error: Connection failed'), 500);
    setTimeout(() => ns.newNotification(3, null, 'Command Result: Build successful'), 1000);

    // Chat notifications
    setTimeout(() => ns.newNotification(10, null, 'Player joined the game'), 1500);
    setTimeout(() => ns.newNotification(11, 'Max', 'Hello everyone!'), 2000);
    setTimeout(() => ns.newNotification(12, 'Anna', 'Hi there!'), 2500);

    // Overlay notifications
    setTimeout(() => ns.newNotification(20, null, 'LEVEL UP!'), 3000);
    setTimeout(() => ns.newNotification(21, null, 'Achievement unlocked'), 5500);

    // Quest notifications
    setTimeout(() => ns.newNotification(30, null, 'Quest: Find the Crystal'), 6000);
    setTimeout(() => ns.newNotification(31, null, 'Target: Search the cave (0/5)'), 6500);

    console.log('Test sequence started. Notifications will appear over 7 seconds.');
  });
};

(window as any).testSystemNotifications = () => {
  appContextPromise.then((appContext) => {
    const ns = appContext.services.notification;
    if (!ns) return;
    ns.newNotification(0, null, 'System Info Message');
    ns.newNotification(1, null, 'System Error Message');
    ns.newNotification(3, null, 'Command Result Message');
  });
};

(window as any).testChatNotifications = () => {
  appContextPromise.then((appContext) => {
    const ns = appContext.services.notification;
    if (!ns) return;
    ns.newNotification(10, null, 'Player joined');
    ns.newNotification(11, 'GroupChat', 'This is a group message');
    ns.newNotification(12, 'PrivateUser', 'This is a private message');
  });
};

(window as any).testOverlayNotifications = () => {
  appContextPromise.then((appContext) => {
    const ns = appContext.services.notification;
    if (!ns) return;
    ns.newNotification(20, null, 'BIG OVERLAY MESSAGE');
    setTimeout(() => ns.newNotification(21, null, 'Small overlay message'), 2500);
  });
};

(window as any).testQuestNotifications = () => {
  appContextPromise.then((appContext) => {
    const ns = appContext.services.notification;
    if (!ns) return;
    ns.newNotification(30, null, 'Quest: Explore the Dungeon');
    ns.newNotification(31, null, 'Kill 10 monsters (3/10)');
  });
};

(window as any).clearChat = () => {
  appContextPromise.then((appContext) => {
    const ns = appContext.services.notification;
    if (!ns) return;
    ns.clearChatNotifications();
    console.log('Chat notifications cleared');
  });
};

(window as any).toggleNotifications = (visible: boolean) => {
  appContextPromise.then((appContext) => {
    const ns = appContext.services.notification;
    if (!ns) return;
    ns.notificationsVisible(visible);
    console.log(`Notifications ${visible ? 'enabled' : 'disabled'}`);
  });
};

console.log('=== Notification Test Functions ===');
console.log('testNotifications() - Test all types');
console.log('testSystemNotifications() - System area');
console.log('testChatNotifications() - Chat area');
console.log('testOverlayNotifications() - Overlay area');
console.log('testQuestNotifications() - Quest area');
console.log('clearChat() - Clear chat notifications');
console.log('toggleNotifications(true/false) - Enable/disable');
console.log('===================================');
