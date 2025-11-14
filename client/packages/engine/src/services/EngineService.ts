/**
 * EngineService - Core 3D engine service
 *
 * Initializes and manages the Babylon.js engine, scene, and rendering pipeline.
 * Coordinates all rendering-related sub-services.
 */

import { Engine, Scene } from '@babylonjs/core';
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import { TextureAtlas } from '../rendering/TextureAtlas';
import { MaterialService } from './MaterialService';
import { ModelService } from './ModelService';
import { EntityRenderService } from './EntityRenderService';
import { CameraService } from './CameraService';
import { EnvironmentService } from './EnvironmentService';
import { PlayerService } from './PlayerService';
import { RenderService } from './RenderService';
import { InputService } from './InputService';
import { PhysicsService } from './PhysicsService';
import { SelectService, SelectMode } from './SelectService';
import { BackdropService } from './BackdropService';
import { WebInputController } from '../input/WebInputController';

const logger = getLogger('EngineService');

/**
 * EngineService - Manages Babylon.js engine and scene
 *
 * Features:
 * - Babylon.js Engine initialization
 * - Scene management
 * - Render loop
 * - Sub-service coordination (Camera, Environment, Render, Player, Input)
 */
export class EngineService {
  private appContext: AppContext;
  private canvas: HTMLCanvasElement;

  // Babylon.js core
  private engine?: Engine;
  private scene?: Scene;

  // Rendering services
  private textureAtlas?: TextureAtlas;
  private materialService?: MaterialService;
  private modelService?: ModelService;
  private entityRenderService?: EntityRenderService;

  // Sub-services
  private cameraService?: CameraService;
  private environmentService?: EnvironmentService;
  private renderService?: RenderService;
  private physicsService?: PhysicsService;
  private playerService?: PlayerService;
  private inputService?: InputService;
  private selectService?: SelectService;
  private backdropService?: BackdropService;

  private isInitialized: boolean = false;
  private isRunning: boolean = false;

  constructor(appContext: AppContext, canvas: HTMLCanvasElement) {
    this.appContext = appContext;
    this.canvas = canvas;

    logger.info('EngineService created');
  }

  /**
   * Initialize the engine
   *
   * Creates Babylon.js Engine, Scene, and initializes all sub-services
   */
  async initialize(): Promise<void> {
    if (this.isInitialized) {
      logger.warn('Engine already initialized');
      return;
    }

    try {
      logger.info('Initializing 3D engine');

      // Check WebGL support
      const gl = this.canvas.getContext('webgl') || this.canvas.getContext('webgl2');
      if (!gl) {
        throw new Error('WebGL not supported by this browser. Please use a modern browser with WebGL support.');
      }

      logger.debug('WebGL supported');

      // Create Babylon.js Engine
      this.engine = new Engine(this.canvas, true, {
        preserveDrawingBuffer: true,
        stencil: true,
        antialias: false, // Disable for better performance
      });

      if (!this.engine) {
        throw new Error('Failed to create Babylon.js Engine');
      }

      logger.debug('Babylon.js Engine created');

      // Create Scene
      this.scene = new Scene(this.engine);
      logger.debug('Scene created');

      // Initialize texture atlas
      this.textureAtlas = new TextureAtlas(this.scene, this.appContext);
      await this.textureAtlas.load();
      logger.debug('TextureAtlas initialized');

      // Initialize material service
      this.materialService = new MaterialService(this.scene, this.appContext);
      this.materialService.setTextureAtlas(this.textureAtlas);
      logger.debug('MaterialService initialized');

      // Initialize model service
      this.modelService = new ModelService(this.scene, this.appContext);
      logger.debug('ModelService initialized');

      // Initialize camera
      this.cameraService = new CameraService(this.scene, this.appContext);
      this.appContext.services.camera = this.cameraService;
      logger.debug('CameraService initialized');

      // Initialize environment
      this.environmentService = new EnvironmentService(this.scene, this.appContext);
      logger.debug('EnvironmentService initialized');

      // Initialize ShaderService with scene and connect to EnvironmentService
      const shaderService = this.appContext.services.shader;
      if (shaderService) {
        shaderService.initialize(this.scene);
        shaderService.setEnvironmentService(this.environmentService);

        // Connect MaterialService with ShaderService
        this.materialService.setShaderService(shaderService);

        logger.debug('ShaderService initialized with scene and connected to EnvironmentService');
        logger.debug('MaterialService connected to ShaderService');
      }

      // Initialize AudioService with scene
      const audioService = this.appContext.services.audio;
      if (audioService) {
        audioService.initialize(this.scene);
        logger.debug('AudioService initialized with scene');
      }

      // Initialize SpriteService with scene and connect to EnvironmentService
      const spriteService = new (await import('./SpriteService')).SpriteService(this.scene, this.appContext);
      this.appContext.services.sprite = spriteService;
      spriteService.setEnvironmentService(this.environmentService);
      logger.debug('SpriteService initialized with scene and connected to EnvironmentService');

      // Initialize ThinInstancesService with scene and shader service
      const thinInstancesService = new (await import('./ThinInstancesService')).ThinInstancesService(this.scene, this.appContext);
      this.appContext.services.thinInstances = thinInstancesService;
      if (shaderService) {
        thinInstancesService.setShaderService(shaderService);
      }
      logger.debug('ThinInstancesService initialized with scene and ShaderService');

      // Store environment service reference for access
      this.appContext.services.environment = this.environmentService;

      // Initialize physics
      this.physicsService = new PhysicsService(this.appContext);
      this.appContext.services.physics = this.physicsService;

      // Connect physics with chunk service for collision detection
      if (this.appContext.services.chunk) {
        this.physicsService.setChunkService(this.appContext.services.chunk);
      }

      logger.debug('PhysicsService initialized and registered');

      // Initialize player
      this.playerService = new PlayerService(this.appContext, this.cameraService);
      this.playerService.setPhysicsService(this.physicsService);
      this.appContext.services.player = this.playerService;
      logger.debug('PlayerService initialized');

      // Disable physics during initial chunk loading to prevent falling
      this.physicsService.disablePhysics();
      logger.debug('Physics disabled for initial spawn');

      // Connect CameraService with PlayerService for turnSpeed updates
      this.cameraService.setPlayerService(this.playerService);
      logger.debug('CameraService connected to PlayerService');

      // Initialize render service
      this.renderService = new RenderService(
        this.scene,
        this.appContext,
        this.materialService,
        this.textureAtlas
      );
      logger.debug('RenderService initialized');

      // Initialize entity render service (requires EntityService and ModelService)
      const entityService = this.appContext.services.entity;
      if (entityService && this.modelService) {
        this.entityRenderService = new EntityRenderService(
          this.scene,
          this.appContext,
          entityService,
          this.modelService
        );

        // Connect EntityRenderService to PlayerService for player avatar rendering
        if (this.playerService) {
          this.playerService.setEntityRenderService(this.entityRenderService);
          logger.debug('EntityRenderService connected to PlayerService');
        }

        logger.debug('EntityRenderService initialized');
      } else {
        logger.warn('EntityRenderService not initialized: missing EntityService or ModelService');
      }

      // Initialize backdrop service (requires scene and appContext with ChunkService)
      if (this.scene && this.appContext.services.chunk) {
        this.backdropService = new BackdropService(this.scene, this.appContext);
        logger.debug('BackdropService initialized');
      } else {
        logger.warn('BackdropService not initialized: missing Scene or ChunkService');
      }

      // Initialize input service
      this.inputService = new InputService(this.appContext, this.playerService);
      this.appContext.services.input = this.inputService; // Register in AppContext
      const webInputController = new WebInputController(this.canvas, this.playerService, this.appContext);
      this.inputService.setController(webInputController);
      logger.debug('InputService initialized');

      // Initialize select service (requires ChunkService and PlayerService)
      if (this.appContext.services.chunk && this.playerService && this.scene) {
        this.selectService = new SelectService(
          this.appContext,
          this.appContext.services.chunk,
          this.playerService,
          this.scene,
          this.appContext.services.entity // Add EntityService for entity selection
        );
        this.appContext.services.select = this.selectService;
        logger.debug('SelectService initialized');

        // Set auto-select mode based on build mode
        if (__EDITOR__) {
          this.selectService.autoSelectMode = SelectMode.BLOCK;
          logger.debug('Auto-select mode set to BLOCK (Editor build)');
        } else if (__VIEWER__) {
          this.selectService.autoSelectMode = SelectMode.INTERACTIVE;
          logger.debug('Auto-select mode set to INTERACTIVE (Viewer build)');
        }
      } else {
        logger.warn('SelectService not initialized: missing ChunkService, PlayerService, or Scene');
      }

      // Listen to player position changes to update chunks
      this.playerService.on('position:changed', (position) => {
        const chunkService = this.appContext.services.chunk;
        if (chunkService) {
          chunkService.updateChunksAroundPosition(position.x, position.z);
        }
      });
      logger.debug('Player position listener registered');

      // Handle window resize
      window.addEventListener('resize', this.onResize);

      this.isInitialized = true;

      logger.info('3D engine initialized successfully');
    } catch (error) {
      this.isInitialized = false;
      throw ExceptionHandler.handleAndRethrow(error, 'EngineService.initialize');
    }
  }

  /**
   * Start the render loop
   */
  startRenderLoop(): void {
    if (!this.isInitialized) {
      throw new Error('Engine not initialized');
    }

    if (this.isRunning) {
      logger.warn('Render loop already running');
      return;
    }

    logger.info('Starting render loop');

    let lastTime = performance.now();

    this.engine!.runRenderLoop(() => {
      try {
        // Calculate delta time
        const currentTime = performance.now();
        const deltaTime = (currentTime - lastTime) / 1000; // Convert to seconds
        lastTime = currentTime;

        // Update services
        this.inputService?.update(deltaTime);
        this.physicsService?.update(deltaTime); // Physics before player
        this.playerService?.update(deltaTime);
        this.cameraService?.update(deltaTime);
        this.selectService?.update(deltaTime); // Update block selection and highlighting
        this.environmentService?.update(deltaTime);

        // Render scene
        this.scene!.render();
      } catch (error) {
        ExceptionHandler.handle(error, 'EngineService.renderLoop');
      }
    });

    this.isRunning = true;
  }

  /**
   * Stop the render loop
   */
  stopRenderLoop(): void {
    if (!this.isRunning) {
      return;
    }

    logger.info('Stopping render loop');

    this.engine?.stopRenderLoop();
    this.isRunning = false;
  }

  /**
   * Handle window resize
   */
  private onResize = (): void => {
    this.engine?.resize();
  };

  /**
   * Get the Babylon.js engine
   */
  getEngine(): Engine | undefined {
    return this.engine;
  }

  /**
   * Get the Babylon.js scene
   */
  getScene(): Scene | undefined {
    return this.scene;
  }

  /**
   * Get the texture atlas
   */
  getTextureAtlas(): TextureAtlas | undefined {
    return this.textureAtlas;
  }

  /**
   * Get the material service
   */
  getMaterialService(): MaterialService | undefined {
    return this.materialService;
  }

  /**
   * Get the model service
   */
  getModelService(): ModelService | undefined {
    return this.modelService;
  }

  /**
   * Get the entity render service
   */
  getEntityRenderService(): EntityRenderService | undefined {
    return this.entityRenderService;
  }

  /**
   * Get the camera service
   */
  getCameraService(): CameraService | undefined {
    return this.cameraService;
  }

  /**
   * Get the environment service
   */
  getEnvironmentService(): EnvironmentService | undefined {
    return this.environmentService;
  }

  /**
   * Get the physics service
   */
  getPhysicsService(): PhysicsService | undefined {
    return this.physicsService;
  }

  /**
   * Get the player service
   */
  getPlayerService(): PlayerService | undefined {
    return this.playerService;
  }

  /**
   * Get the render service
   */
  getRenderService(): RenderService | undefined {
    return this.renderService;
  }

  /**
   * Get the input service
   */
  getInputService(): InputService | undefined {
    return this.inputService;
  }

  /**
   * Check if engine is initialized
   */
  isReady(): boolean {
    return (
      this.isInitialized &&
      (this.textureAtlas?.isReady() ?? false) &&
      (this.materialService?.isReady() ?? false)
    );
  }

  /**
   * Set wireframe mode for all materials
   *
   * @param enabled true to enable wireframe mode, false to disable
   */
  setWireframeMode(enabled: boolean): void {
    if (!this.scene) {
      logger.warn('Cannot set wireframe mode: scene not initialized');
      return;
    }

    // Set wireframe mode on all materials in the scene
    this.scene.materials.forEach((material) => {
      material.wireframe = enabled;
    });

    logger.info(`Wireframe mode ${enabled ? 'enabled' : 'disabled'}`, {
      materialCount: this.scene.materials.length,
    });
  }

  /**
   * Dispose engine and all resources
   */
  dispose(): void {
    logger.info('Disposing engine');

    this.stopRenderLoop();

    // Remove event listeners
    window.removeEventListener('resize', this.onResize);

    // Dispose sub-services
    this.inputService?.dispose();
    this.selectService?.dispose();
    this.backdropService?.dispose();
    this.renderService?.dispose();
    this.entityRenderService?.dispose();
    this.playerService?.dispose();
    this.physicsService?.dispose();
    this.environmentService?.dispose();
    this.cameraService?.dispose();
    this.modelService?.dispose();
    this.materialService?.dispose();

    // Dispose Babylon.js resources
    this.scene?.dispose();
    this.engine?.dispose();

    this.isInitialized = false;
    this.isRunning = false;

    logger.info('Engine disposed');
  }
}
