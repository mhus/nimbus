/**
 * Main VoxelClient class
 */

import { Engine, Scene, FreeCamera, Vector3, HemisphericLight, SpotLight, DirectionalLight, MeshBuilder } from '@babylonjs/core';
import { AdvancedDynamicTexture } from '@babylonjs/gui';
import { MainMenu, type ServerInfo } from './gui/MainMenu';
import { GameConsole } from './gui/GameConsole';
import { BlockEditor } from './gui/BlockEditor';
import { FluidWaveShader } from './rendering/FluidWaveShader';
import { WindShader } from './rendering/WindShader';
import { MaterialManager } from './rendering/MaterialManager';
import { WebSocketClient } from './network/WebSocketClient';
import { ChunkManager } from './world/ChunkManager';
import { PlayerController } from './player/PlayerController';
import { BlockSelector } from './player/BlockSelector';
import { ClientRegistry } from './registry/ClientRegistry';
import { ClientAssetManager } from './assets/ClientAssetManager';
import { TextureAtlas } from './rendering/TextureAtlas';
import { createDefaultAtlasConfig } from './rendering/defaultAtlasConfig';
import { RegistryMessageType, AssetMessageType, createRegistryAckMessage } from '@voxel-02/protocol';
import type { RegistryMessage, AssetMessage } from '@voxel-02/protocol';
import { CommandController } from './commands/CommandController';
import { HelpCommand } from './commands/builtin/HelpCommand';
import { PositionCommand, TeleportCommand, StartCommand, FlightCommand, WalkCommand, OrbitDistanceCommand } from './commands/builtin/PlayerCommands';
import { SelectCommand } from './commands/builtin/SelectCommand';
import { GridCommand, CollisionCommand, EditorCommand } from './commands/builtin/ViewCommands';
import { WaterWaveCommand, LavaWaveCommand } from './commands/builtin/FluidWaveCommands';
import { RechunkCommand, RechunkAllCommand } from './commands/builtin/ChunkCommands';
import { WindManager } from './wind/WindManager';
import { WindDirectionCommand, WindStrengthCommand, WindGustStrengthCommand, WindSwayFactorCommand } from './commands/WindCommands';
import { SkyManager } from './rendering/SkyManager';

/**
 * Main client class for VoxelSrv
 */
export class VoxelClient {
  private canvas: HTMLCanvasElement;
  private engine?: Engine;
  private scene?: Scene;
  private camera?: FreeCamera;
  private mainMenu?: MainMenu;
  private socket?: WebSocketClient;
  private chunkManager?: ChunkManager;
  private playerController?: PlayerController;
  private blockSelector?: BlockSelector;
  private registry: ClientRegistry;
  private assetManager: ClientAssetManager;
  private atlas?: TextureAtlas;
  private connected = false;

  // Console system
  private advancedTexture?: AdvancedDynamicTexture;
  private gameConsole?: GameConsole;
  private blockEditor?: BlockEditor;
  private fluidWaveShader?: FluidWaveShader;
  private windShader?: WindShader;
  private commandController: CommandController;
  private serverCommandController?: CommandController;
  private windManager: WindManager;
  private skyManager?: SkyManager;

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas;
    this.registry = new ClientRegistry();
    this.assetManager = new ClientAssetManager();
    this.commandController = new CommandController();
    this.windManager = new WindManager();
  }

  /**
   * Initialize the client
   */
  async init(): Promise<void> {
    console.log('[Client] Initializing...');

    // Create Babylon.js engine
    this.engine = new Engine(this.canvas, true, {
      preserveDrawingBuffer: true,
      stencil: true,
    });

    // Create scene
    this.scene = new Scene(this.engine);
    this.scene.clearColor.set(0.5, 0.7, 1.0, 1.0);  // Sky blue

    // Create camera (detached initially, will be positioned on spawn)
    this.camera = new FreeCamera('camera', new Vector3(0, 80, 0), this.scene);
    this.camera.setTarget(new Vector3(10, 79, 10));

    // Set near clipping plane to prevent seeing through blocks when standing close
    // Lower value = render geometry closer to camera (prevents clipping when near walls)
    this.camera.minZ = 0.01;

    // Create light
    const light = new HemisphericLight('light', new Vector3(0, 1, 0), this.scene);
    // const light = new DirectionalLight("DirectionalLight", new Vector3(0, -1, 0), this.scene);
    //const light = new SpotLight("spotLight", new Vector3(0, 30, -10), new Vector3(0, -1, 0), Math.PI / 3, 2, this.scene);
    light.intensity = 0.7;


    // Initialize texture atlas (will be configured with server URL later)
    // Note: Asset server URL will be set when connecting to server
    console.log('[Client] Initializing texture system...');
    this.atlas = new TextureAtlas(this.scene, createDefaultAtlasConfig());
    this.atlas.setAssetManager(this.assetManager);
    await this.atlas.load();
    console.log('[Client] Texture system initialized');

    // Initialize fluid wave shader
    this.fluidWaveShader = new FluidWaveShader(this.scene);
    console.log('[Client] Fluid wave shader initialized');

    // Initialize wind shader
    this.windShader = new WindShader(this.scene);
    console.log('[Client] Wind shader initialized');

    // Connect WindManager to WindShader for automatic parameter updates
    this.windShader.setWindManager(this.windManager);
    console.log('[Client] WindManager connected to WindShader');

    // Resize handler
    window.addEventListener('resize', () => {
      this.engine?.resize();
    });

    // Initialize GUI
    this.advancedTexture = AdvancedDynamicTexture.CreateFullscreenUI('UI', true, this.scene);

    // Initialize game console (but don't show it yet)
    this.gameConsole = new GameConsole(
      this.scene,
      this.advancedTexture,
      this.commandController,
      this.serverCommandController
    );

    // Ensure console is hidden on startup
    this.gameConsole.hide();

    // Register basic commands
    this.registerCommands();

    // Setup console toggle key
    this.setupConsoleToggle();

    // Setup block editor hotkeys
    this.setupBlockEditorKeys();

    // Start render loop
    this.engine.runRenderLoop(() => {
      // Update block selector
      this.blockSelector?.update();

      // Update block editor (checks if selection changed)
      this.blockEditor?.update();

      this.scene?.render();
    });

    // Show main menu
    this.showMainMenu();

    console.log('[Client] Initialized successfully');
  }

  /**
   * Show main menu
   */
  private showMainMenu(): void {
    if (!this.scene) return;

    this.mainMenu = new MainMenu(this.scene);
    this.mainMenu.show((serverInfo: ServerInfo) => {
      this.connectToServer(serverInfo);
    });
  }

  /**
   * Register client commands
   */
  private registerCommands(): void {
    // Help command
    this.commandController.registerCommand(new HelpCommand(this.commandController));

    // Player commands
    this.commandController.registerCommand(new PositionCommand(this));
    this.commandController.registerCommand(new TeleportCommand(this));
    this.commandController.registerCommand(new StartCommand(this));
    this.commandController.registerCommand(new FlightCommand(this));
    this.commandController.registerCommand(new WalkCommand(this));
    this.commandController.registerCommand(new OrbitDistanceCommand(this));

    // Selection command
    this.commandController.registerCommand(new SelectCommand(this));

    // View commands
    this.commandController.registerCommand(new GridCommand(this));
    this.commandController.registerCommand(new CollisionCommand(this));
    this.commandController.registerCommand(new EditorCommand(this));

    // Fluid wave commands
    this.commandController.registerCommand(new WaterWaveCommand(this));
    this.commandController.registerCommand(new LavaWaveCommand(this));

    // Chunk commands
    this.commandController.registerCommand(new RechunkCommand(this));
    this.commandController.registerCommand(new RechunkAllCommand(this));

    // Wind commands
    this.commandController.registerCommand(new WindDirectionCommand(this.windManager));
    this.commandController.registerCommand(new WindStrengthCommand(this.windManager));
    this.commandController.registerCommand(new WindGustStrengthCommand(this.windManager));
    this.commandController.registerCommand(new WindSwayFactorCommand(this.windManager));

    console.log('[Client] Registered client commands');
  }

  /**
   * Setup console toggle with '/' key
   */
  private setupConsoleToggle(): void {
    document.addEventListener('keydown', (event) => {
      // Only handle '/' key
      if (event.key !== '/') {
        return;
      }

      // Don't open console if already visible
      if (this.gameConsole?.getIsVisible()) {
        return;
      }

      // Don't trigger shortcuts if no pointer lock (user might be typing in editor or console)
      if (!document.pointerLockElement && (this.blockEditor?.getIsVisible() || this.gameConsole?.getIsVisible())) {
        return;
      }

      // Open console with '/' key
      event.preventDefault();
      this.gameConsole?.show();

      // Release pointer lock when console opens
      if (document.pointerLockElement) {
        document.exitPointerLock();
      }
    });

    console.log('[Client] Console setup (press "/" to open, ESC to close)');
  }

  /**
   * Setup block editor hotkeys ('.' for edit mode, ',' for new block)
   */
  private setupBlockEditorKeys(): void {
    document.addEventListener('keydown', (event) => {
      // Don't trigger if console is visible
      if (this.gameConsole?.getIsVisible()) {
        return;
      }

      // Don't trigger shortcuts if no pointer lock (user might be typing in editor or console)
      if (!document.pointerLockElement && (this.blockEditor?.getIsVisible() || this.gameConsole?.getIsVisible())) {
        return;
      }

      // '.' key behavior:
      if (event.key === '.') {
        event.preventDefault();

        if (this.blockEditor?.getIsVisible()) {
          // Editor is already visible -> activate edit mode
          this.blockEditor.activateEditMode();

          // Release pointer lock when entering edit mode
          if (document.pointerLockElement) {
            document.exitPointerLock();
          }

          console.log('[Client] Edit mode activated');
        } else {
          // Editor is not visible -> enable select mode and show editor
          if (this.blockSelector) {
            this.blockSelector.enable();
            console.log('[Client] Block selection enabled');
          }
          if (this.blockEditor) {
            this.blockEditor.show();
            console.log('[Client] Block editor shown');
          }
        }
      }

      // ',' key to create new block (when editor is visible)
      if (event.key === ',' && this.blockEditor?.getIsVisible()) {
        event.preventDefault();
        this.blockEditor.createNewBlock();
        console.log('[Client] Creating new block');
      }
    });

    console.log('[Client] Block editor hotkeys setup ("." for editor/edit mode, "," for new block)');
  }

  /**
   * Connect to server
   */
  private async connectToServer(serverInfo: ServerInfo): Promise<void> {
    console.log('[Client] Connecting to server:', serverInfo);

    if (!this.scene) {
      console.error('[Client] Scene not initialized');
      return;
    }

    if (serverInfo.address === 'embedded') {
      console.log('[Client] Starting singleplayer (embedded server)...');
      // TODO: Start embedded server
    } else {
      console.log(`[Client] Connecting to ${serverInfo.address}:${serverInfo.port}...`);

      try {
        // Create WebSocket client
        this.socket = new WebSocketClient();
        const serverUrl = `ws://${serverInfo.address}:${serverInfo.port}`;

        // Update atlas with asset server URL (port + 1)
        const assetServerPort = serverInfo.port + 1;
        const assetServerUrl = `http://${serverInfo.address}:${assetServerPort}/assets`;
        console.log(`[Client] Asset server URL: ${assetServerUrl}`);

        if (!this.atlas) {
          throw new Error('Texture atlas not initialized');
        }

        // Update atlas config with correct asset server URL
        const config = this.atlas.getConfig();
        config.assetServerUrl = assetServerUrl;

        // Create skybox now that we have the asset server URL
        this.skyManager = new SkyManager(this.scene);
        const skyboxUrl = `${assetServerUrl}/textures/skybox/skybox`;
        this.skyManager.createSkybox(skyboxUrl);
        console.log('[Client] Skybox initialized');

        // Create material manager with shaders
        const materialManager = new MaterialManager(this.scene, this.atlas, this.windShader, this.fluidWaveShader);

        // Create chunk manager with material manager
        this.chunkManager = new ChunkManager(this.socket, this.scene, this.atlas, this.registry, materialManager);

        // Set render distances from server info
        this.chunkManager.setRenderDistance(serverInfo.renderDistance);
        this.chunkManager.setUnloadDistance(serverInfo.unloadDistance);
        console.log(`[Client] Terrain distances set: render=${serverInfo.renderDistance}, unload=${serverInfo.unloadDistance}`);

        // Set WindManager for sprite wind animation
        this.chunkManager.setWindManager(this.windManager);

        // Apply wireframe mode if enabled
        if (serverInfo.wireframeMode) {
          console.log('[Client] Enabling wireframe mode');
          const material = this.atlas.getMaterial();
          if (material) {
            material.wireframe = true;
          }
        }

        // Setup message handlers
        this.setupAssetHandler();
        this.setupRegistryHandler();

        // Connect to server
        await this.socket.connect(serverUrl);

        console.log('[Client] Connected successfully, waiting for registry sync...');

        // Create player controller for physics and collision
        this.playerController = new PlayerController(this.scene, this.camera, this.chunkManager, this.registry);

        // Create block selector for highlighting blocks
        this.blockSelector = new BlockSelector(this.scene, this.camera, this.chunkManager, this.registry);

        // Create block editor (requires blockSelector, registry, and chunkManager)
        if (this.advancedTexture && this.blockSelector && this.chunkManager) {
          this.blockEditor = new BlockEditor(this.scene, this.advancedTexture, this.blockSelector, this.registry, this.chunkManager, this);
        }

        // Setup debug key
        this.setupDebugKey();

        // Listen for disconnect
        this.socket.on('PlayerKick', (data) => {
          console.log('[Client] Disconnected:', data.reason);
          alert(`Disconnected: ${data.reason}`);
          this.disconnect();
        });

      } catch (error) {
        console.error('[Client] Connection failed:', error);
        alert('Failed to connect to server');
        this.showMainMenu();
        return;
      }
    }

    // Enable camera mouse look only (movement handled by PlayerController)
    this.camera?.attachControl(this.canvas, true);
    this.camera!.angularSensibility = 2000;

    // Disable default keyboard controls (PlayerController handles movement)
    this.camera!.keysUp = [];
    this.camera!.keysDown = [];
    this.camera!.keysLeft = [];
    this.camera!.keysRight = [];
    this.camera!.keysUpward = [];
    this.camera!.keysDownward = [];

    this.connected = true;

    // Setup pointer lock on canvas click (must be after connection is established)
    this.setupPointerLock();
  }

  /**
   * Setup asset manifest handler
   */
  private setupAssetHandler(): void {
    if (!this.socket) return;

    // Listen for asset manifest
    this.socket.on(AssetMessageType.ASSET_MANIFEST, async (message: AssetMessage) => {
      if (message.type === AssetMessageType.ASSET_MANIFEST) {
        console.log('[Client] Received asset manifest from server');

        // Load manifest
        await this.assetManager.loadManifest(message.data);

        console.log('[Client] Asset manifest loaded');
        // Note: Texture atlas is already loaded during init()
      }
    });
  }

  /**
   * Setup registry message handler
   */
  private setupRegistryHandler(): void {
    if (!this.socket) return;

    // Listen for registry sync message
    this.socket.on(RegistryMessageType.REGISTRY_SYNC, (message: RegistryMessage) => {
      if (message.type === RegistryMessageType.REGISTRY_SYNC) {
        console.log('[Client] Received registry sync from server');

        // Load registry data
        this.registry.loadFromServer(
          message.data.blocks,
          message.data.items,
          message.data.entities,
          message.data.version
        );

        // Send acknowledgement
        const ackMessage = createRegistryAckMessage(
          true,
          message.data.version,
          {
            blocks: message.data.blocks.length,
            items: message.data.items.length,
            entities: message.data.entities.length,
          }
        );

        this.socket!.send(JSON.stringify(ackMessage));

        // Now request chunks (registry is loaded)
        console.log('[Client] Registry synced, requesting chunks...');
        // Use render distance from server info (stored in ChunkManager)
        const renderDistance = this.chunkManager?.getRenderDistance() || 3;
        this.chunkManager?.requestChunksAround(0, 0, renderDistance);
      }
    });

    // Listen for registry updates
    this.socket.on(RegistryMessageType.REGISTRY_UPDATE, (message: RegistryMessage) => {
      if (message.type === RegistryMessageType.REGISTRY_UPDATE) {
        console.log('[Client] Received registry update from server');
        this.registry.applyUpdate(message.data);
      }
    });
  }

  /**
   * Setup debug key (backslash) for world dump
   */
  private setupDebugKey(): void {
    if (!this.scene) return;

    document.addEventListener('keydown', (event) => {
      if (event.key === '\\') {
        this.dumpWorldDebugInfo();
      }
    });
  }

  /**
   * Dump world debug information to console
   */
  private dumpWorldDebugInfo(): void {
    console.log('\n========================================');
    console.log('🌍 WORLD DEBUG DUMP');
    console.log('========================================\n');

    // Camera/Player position
    if (this.camera) {
      const pos = this.camera.position;
      const blockPos = {
        x: Math.floor(pos.x),
        y: Math.floor(pos.y),
        z: Math.floor(pos.z),
      };
      console.log('📍 Player Position:');
      console.log(`   World: (${pos.x.toFixed(2)}, ${pos.y.toFixed(2)}, ${pos.z.toFixed(2)})`);
      console.log(`   Block: (${blockPos.x}, ${blockPos.y}, ${blockPos.z})`);
      console.log(`   Chunk: (${Math.floor(blockPos.x / 32)}, ${Math.floor(blockPos.z / 32)})`);
    }

    // Player controller info
    if (this.playerController) {
      const mode = this.playerController.getMode();
      console.log(`\n🎮 Player Mode: ${mode.toUpperCase()}`);
    }

    // Chunk manager info
    if (this.chunkManager) {
      const chunkCount = this.chunkManager.getLoadedChunksCount();
      const renderDistance = this.chunkManager.getRenderDistance();
      const playerChunk = this.chunkManager.getPlayerChunk();

      console.log(`\n📦 Chunk Loading:`);
      console.log(`   Loaded Chunks: ${chunkCount}`);
      console.log(`   Render Distance: ${renderDistance} chunks (${renderDistance * 2 + 1}×${renderDistance * 2 + 1} grid)`);
      console.log(`   Player Chunk: (${playerChunk.x}, ${playerChunk.z})`);

      // Access internal chunk data
      const chunks = (this.chunkManager as any).chunks as Map<string, any>;

      console.log('\n📋 Chunk Details:');
      let totalBlocks = 0;
      let totalNonAirBlocks = 0;

      const chunkArray = Array.from(chunks.entries());
      chunkArray.forEach(([key, chunk], index) => {
        const nonAirBlocks = Array.from(chunk.data).filter((id: number) => id !== 0).length;
        totalBlocks += chunk.data.length;
        totalNonAirBlocks += nonAirBlocks;

        if (index < 10) { // Show first 10 chunks
          console.log(`   Chunk ${key}:`);
          console.log(`      Total blocks: ${chunk.data.length}`);
          console.log(`      Non-air blocks: ${nonAirBlocks} (${(nonAirBlocks / chunk.data.length * 100).toFixed(1)}%)`);

          // Count blocks by type
          const blockCounts: Map<number, number> = new Map();
          for (let i = 0; i < chunk.data.length; i++) {
            const blockId = chunk.data[i];
            blockCounts.set(blockId, (blockCounts.get(blockId) || 0) + 1);
          }

          console.log(`      Block types:`);
          blockCounts.forEach((count, blockId) => {
            if (blockId !== 0) {
              const blockName = this.getBlockName(blockId);
              console.log(`         ${blockName} (ID ${blockId}): ${count} blocks`);
            }
          });
        }
      });

      if (chunkArray.length > 10) {
        console.log(`   ... and ${chunkArray.length - 10} more chunks`);
      }

      console.log(`\n📊 Total Statistics:`);
      console.log(`   Total blocks: ${totalBlocks}`);
      console.log(`   Non-air blocks: ${totalNonAirBlocks}`);
      console.log(`   Air blocks: ${totalBlocks - totalNonAirBlocks}`);
      console.log(`   Fill ratio: ${(totalNonAirBlocks / totalBlocks * 100).toFixed(1)}%`);
    }

    // Scene info
    if (this.scene) {
      const meshCount = this.scene.meshes.length;
      const materialCount = this.scene.materials.length;
      const textureCount = this.scene.textures.length;

      console.log(`\n🎨 Rendering Info:`);
      console.log(`   Meshes: ${meshCount}`);
      console.log(`   Materials: ${materialCount}`);
      console.log(`   Textures: ${textureCount}`);

      // Count triangles
      let totalTriangles = 0;
      this.scene.meshes.forEach(mesh => {
        if (mesh.getTotalVertices && mesh.getIndices) {
          const indices = mesh.getIndices();
          if (indices) {
            totalTriangles += indices.length / 3;
          }
        }
      });

      console.log(`   Total triangles: ${totalTriangles.toLocaleString()}`);
    }

    // Performance info
    if (this.engine) {
      const fps = this.engine.getFps();
      console.log(`\n⚡ Performance:`);
      console.log(`   FPS: ${fps.toFixed(1)}`);
    }

    // Connection info
    if (this.socket) {
      const isConnected = this.socket.isConnected();
      const serverUrl = (this.socket as any).server || 'unknown';
      console.log(`\n🌐 Connection:`);
      console.log(`   Status: ${isConnected ? 'Connected' : 'Disconnected'}`);
      console.log(`   Server: ${serverUrl}`);
    }

    // Selected Block Info
    if (this.blockSelector) {
      console.log(`\n🎯 Selected Block:`);
      const selection = this.blockSelector.getSelectedBlock();

      if (selection) {
        const { blockX, blockY, blockZ, blockId, distance } = selection;
        const chunkX = Math.floor(blockX / 32);
        const chunkZ = Math.floor(blockZ / 32);

        console.log(`   Position: (${blockX}, ${blockY}, ${blockZ})`);
        console.log(`   Chunk: (${chunkX}, ${chunkZ})`);
        console.log(`   Distance: ${distance.toFixed(2)} blocks`);
        console.log(`   Block ID: ${blockId}`);
        console.log(`   Block Name: ${this.getBlockName(blockId)}`);

        // Get block from chunk for additional information
        if (this.chunkManager) {
          const chunk = this.chunkManager.getChunk(chunkX, chunkZ);
          if (chunk) {
            // Calculate local position within chunk
            const localX = blockX - chunkX * 32;
            const localZ = blockZ - chunkZ * 32;
            const localY = blockY;

            // Get index
            const index = localX + localY * 32 + localZ * 32 * (chunk.height || 256);

            if (blockId !== 0) {
              // Get BlockType from registry
              const blockType = this.registry.getBlockByID(blockId);
              if (blockType) {
                console.log(`\n   📋 BlockType Configuration:`);
                console.log(`      ID: ${blockType.id}`);
                console.log(`      Name: ${blockType.name}`);
                console.log(`      Shape: ${blockType.shape || 'CUBE'}`);
                console.log(`      Transparent: ${blockType.transparent || false}`);

                // Wind parameters from BlockType
                const hasWindParams = blockType.windLeafiness !== undefined ||
                                     blockType.windStability !== undefined ||
                                     blockType.windLeverUp !== undefined ||
                                     blockType.windLeverDown !== undefined;
                if (hasWindParams) {
                  console.log(`\n      Wind Parameters (BlockType):`);
                  console.log(`         windLeafiness: ${blockType.windLeafiness ?? 'undefined'}`);
                  console.log(`         windStability: ${blockType.windStability ?? 'undefined'}`);
                  console.log(`         windLeverUp: ${blockType.windLeverUp ?? 'undefined'}`);
                  console.log(`         windLeverDown: ${blockType.windLeverDown ?? 'undefined'}`);
                }

                if (blockType.options) {
                  console.log(`\n      Options:`);
                  Object.entries(blockType.options).forEach(([key, value]) => {
                    console.log(`         ${key}: ${JSON.stringify(value)}`);
                  });
                }

                if (blockType.textures) {
                  console.log(`\n      Textures:`);
                  Object.entries(blockType.textures).forEach(([key, value]) => {
                    console.log(`         ${key}: ${value}`);
                  });
                }

                // Get modifier to check for wind properties
                let modifier = null;
                if (chunk.modifiers) {
                  modifier = chunk.modifiers instanceof Map
                    ? chunk.modifiers.get(index)
                    : chunk.modifiers[index];
                }

                // Determine material type
                const isFluid = blockType.options?.fluid || false;
                const fluidMaterial = blockType.options?.material;
                const isTransparent = blockType.transparent || blockType.options?.transparent || false;

                // Check for wind properties in both blockType AND modifier
                const hasWind =
                  (blockType.windLeafiness && blockType.windLeafiness > 0) ||
                  (blockType.windStability && blockType.windStability > 0) ||
                  (blockType.windLeverUp && blockType.windLeverUp > 0) ||
                  (blockType.windLeverDown && blockType.windLeverDown > 0) ||
                  (modifier?.windLeafiness && modifier.windLeafiness > 0) ||
                  (modifier?.windStability && modifier.windStability > 0) ||
                  (modifier?.windLeverUp && modifier.windLeverUp > 0) ||
                  (modifier?.windLeverDown && modifier.windLeverDown > 0);

                let materialType = 'solid';
                if (isFluid && fluidMaterial === 'water') {
                  materialType = 'water';
                } else if (isFluid && fluidMaterial === 'lava') {
                  materialType = 'lava';
                } else if (isTransparent && hasWind) {
                  materialType = 'transparent_wind';
                } else if (isTransparent) {
                  materialType = 'transparent';
                }

                console.log(`\n   🎨 Rendering Configuration:`);
                console.log(`      Material Type: ${materialType}`);
                console.log(`      Uses Shader: ${materialType === 'water' || materialType === 'lava' || materialType === 'transparent_wind' ? 'Yes' : 'No'}`);

                // Get modifier if available
                if (chunk.modifiers) {
                  const modifier = chunk.modifiers instanceof Map
                    ? chunk.modifiers.get(index)
                    : chunk.modifiers[index];

                  if (modifier) {
                    console.log(`\n   🔧 Block Modifier:`);
                    console.log(`      Has Modifier: Yes`);

                    // Show all modifier properties
                    Object.entries(modifier).forEach(([key, value]) => {
                      if (key === 'color' && Array.isArray(value)) {
                        console.log(`      ${key}: [${value.join(', ')}]`);
                      } else {
                        console.log(`      ${key}: ${JSON.stringify(value)}`);
                      }
                    });

                    // Wind parameters from Modifier
                    const hasModifierWindParams = modifier.windLeafiness !== undefined ||
                                                 modifier.windStability !== undefined ||
                                                 modifier.windLeverUp !== undefined ||
                                                 modifier.windLeverDown !== undefined;
                    if (hasModifierWindParams) {
                      console.log(`\n      Wind Parameters (Modifier):`);
                      console.log(`         windLeafiness: ${modifier.windLeafiness ?? 'undefined'}`);
                      console.log(`         windStability: ${modifier.windStability ?? 'undefined'}`);
                      console.log(`         windLeverUp: ${modifier.windLeverUp ?? 'undefined'}`);
                      console.log(`         windLeverDown: ${modifier.windLeverDown ?? 'undefined'}`);
                    }
                  }
                }

                // Also check metadata
                const metadata = chunk.metadata?.[index];
                if (metadata) {
                  console.log(`\n   📊 Block Metadata:`);
                  console.log(`      Packed value: ${metadata}`);
                }
              } else {
                console.log(`   ⚠️ BlockType not found in registry!`);
              }
            } else {
              console.log(`   (Air block)`);
            }
          } else {
            console.log(`   ⚠️ Chunk not loaded`);
          }
        }
      } else {
        console.log(`   No block selected (look at a block to select it)`);
      }
    }

    console.log('\n========================================');
    console.log('End of debug dump');
    console.log('========================================\n');
  }

  /**
   * Get block name from ID (using registry)
   */
  private getBlockName(blockId: number): string {
    if (blockId === 0) return 'Air';

    const block = this.registry.getBlockByID(blockId);
    return block ? block.name : `Unknown (${blockId})`;
  }

  /**
   * Get client registry (for debugging)
   */
  getRegistry(): ClientRegistry {
    return this.registry;
  }

  /**
   * Get wind manager
   */
  getWindManager(): WindManager {
    return this.windManager;
  }

  /**
   * Setup pointer lock for mouse control
   */
  private setupPointerLock(): void {
    if (!this.canvas || !this.scene) return;

    // Request pointer lock on canvas click
    const clickHandler = (event: MouseEvent) => {
      // Don't request pointer lock if BlockEditor is in edit mode
      if (this.blockEditor?.isInEditMode()) {
        console.log('[Client] Pointer lock skipped (BlockEditor in edit mode)');
        return;
      }

      // Don't request pointer lock if BlockEditor wants to ignore this request
      if (this.blockEditor?.shouldIgnorePointerLock()) {
        console.log('[Client] Pointer lock skipped (BlockEditor ignoreNextPointerLock flag)');
        return;
      }

      // Don't request pointer lock if click was on a GUI element (not on canvas directly)
      if (event.target !== this.canvas) {
        console.log('[Client] Pointer lock skipped (click was not on canvas)');
        return;
      }

      if (!document.pointerLockElement) {
        this.canvas.requestPointerLock()
          .catch((err) => {
            console.warn('[Client] Pointer lock request failed:', err.message);
          });
      }
    };

    this.canvas.addEventListener('click', clickHandler);

    // Handle pointer lock change
    const lockChangeHandler = () => {
      const isLocked = document.pointerLockElement === this.canvas;
      console.log(`[Client] Pointer lock: ${isLocked ? 'enabled' : 'disabled'}`);
    };

    document.addEventListener('pointerlockchange', lockChangeHandler);

    // ESC key to exit pointer lock
    const keyHandler = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && document.pointerLockElement) {
        document.exitPointerLock();
      }
    };

    document.addEventListener('keydown', keyHandler);
  }

  /**
   * Disconnect from server
   */
  private disconnect(): void {
    if (this.socket) {
      this.socket.close();
      this.socket = undefined;
    }

    if (this.chunkManager) {
      this.chunkManager.dispose();
      this.chunkManager = undefined;
    }

    this.connected = false;

    // Exit pointer lock
    if (document.pointerLockElement) {
      document.exitPointerLock();
    }

    this.showMainMenu();
  }

  /**
   * Get the Babylon.js scene
   */
  getScene(): Scene | undefined {
    return this.scene;
  }

  /**
   * Get the Babylon.js engine
   */
  getEngine(): Engine | undefined {
    return this.engine;
  }

  /**
   * Get the camera
   */
  getCamera(): FreeCamera | undefined {
    return this.camera;
  }

  /**
   * Get the canvas
   */
  getCanvas(): HTMLCanvasElement {
    return this.canvas;
  }

  /**
   * Re-initialize camera controls (after pointer lock is re-enabled)
   * This must match the exact sequence from connectToServer() lines 348-357
   */
  reinitializeCameraControls(): void {
    if (!this.camera || !this.canvas) {
      console.warn('[Client] Cannot reinitialize camera controls: camera or canvas not available');
      return;
    }

    console.log('[Client] Re-initializing camera controls...');

    // IMPORTANT: Exact same sequence as initial setup (no detach, no timeout)
    // Enable camera mouse look only (movement handled by PlayerController)
    this.camera.attachControl(this.canvas, true);
    this.camera.angularSensibility = 2000;

    // Disable default keyboard controls (PlayerController handles movement)
    this.camera.keysUp = [];
    this.camera.keysDown = [];
    this.camera.keysLeft = [];
    this.camera.keysRight = [];
    this.camera.keysUpward = [];
    this.camera.keysDownward = [];

    console.log('[Client] Camera controls reinitialized - angularSensibility:', this.camera.angularSensibility);
  }

  /**
   * Request pointer lock and reinitialize camera (called by BlockEditor)
   */
  requestPointerLockWithReinit(): Promise<void> {
    if (!this.canvas) {
      return Promise.reject(new Error('Canvas not available'));
    }

    console.log('[Client] Requesting pointer lock with camera reinit...');

    return this.canvas.requestPointerLock().then(() => {
      console.log('[Client] Pointer lock enabled, reinitializing camera...');
      // Wait a bit for pointer lock to be fully active
      setTimeout(() => {
        this.reinitializeCameraControls();
      }, 50);
    });
  }

  /**
   * Shutdown the client
   */
  dispose(): void {
    this.scene?.dispose();
    this.engine?.dispose();
  }
}
