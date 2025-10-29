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
import { CameraService } from './CameraService';
import { EnvironmentService } from './EnvironmentService';
import { PlayerService } from './PlayerService';
import { RenderService } from './RenderService';
import { InputService } from './InputService';
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

  // Sub-services
  private cameraService?: CameraService;
  private environmentService?: EnvironmentService;
  private renderService?: RenderService;
  private playerService?: PlayerService;
  private inputService?: InputService;

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

      // Create Babylon.js Engine
      this.engine = new Engine(this.canvas, true, {
        preserveDrawingBuffer: true,
        stencil: true,
      });

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

      // Initialize camera
      this.cameraService = new CameraService(this.scene, this.appContext);
      logger.debug('CameraService initialized');

      // Initialize environment
      this.environmentService = new EnvironmentService(this.scene, this.appContext);
      logger.debug('EnvironmentService initialized');

      // Initialize player
      this.playerService = new PlayerService(this.appContext, this.cameraService);
      logger.debug('PlayerService initialized');

      // Initialize render service
      this.renderService = new RenderService(
        this.scene,
        this.appContext,
        this.materialService,
        this.textureAtlas
      );
      logger.debug('RenderService initialized');

      // Initialize input service
      this.inputService = new InputService(this.appContext, this.playerService);
      const webInputController = new WebInputController(this.canvas, this.playerService);
      this.inputService.setController(webInputController);
      logger.debug('InputService initialized');

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
        this.playerService?.update(deltaTime);
        this.cameraService?.update(deltaTime);
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
    return this.isInitialized && this.textureAtlas?.isReady() && this.materialService?.isReady();
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
    this.renderService?.dispose();
    this.playerService?.dispose();
    this.environmentService?.dispose();
    this.cameraService?.dispose();
    this.materialService?.dispose();

    // Dispose Babylon.js resources
    this.scene?.dispose();
    this.engine?.dispose();

    this.isInitialized = false;
    this.isRunning = false;

    logger.info('Engine disposed');
  }
}
