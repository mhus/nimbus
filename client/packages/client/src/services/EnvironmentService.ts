/**
 * EnvironmentService - Manages scene environment
 *
 * Handles lighting, sky, fog, and other environmental effects.
 */

import { HemisphericLight, Vector3, Color3, Scene } from '@babylonjs/core';
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';

const logger = getLogger('EnvironmentService');

/**
 * EnvironmentService - Manages environment rendering
 *
 * Features:
 * - Hemispheric lighting
 * - Background color
 * - Future: Sky, fog, weather effects
 */
export class EnvironmentService {
  private scene: Scene;
  private appContext: AppContext;

  private light?: HemisphericLight;

  constructor(scene: Scene, appContext: AppContext) {
    this.scene = scene;
    this.appContext = appContext;

    this.initializeEnvironment();

    logger.info('EnvironmentService initialized');
  }

  /**
   * Initialize environment
   */
  private initializeEnvironment(): void {
    try {
      // Set background color (light blue sky)
      this.scene.clearColor = new Color3(0.5, 0.7, 1.0).toColor4();

      // Create hemispheric light
      this.light = new HemisphericLight('environmentLight', new Vector3(0, 1, 0), this.scene);

      // Set light properties
      this.light.intensity = 1.0;
      this.light.diffuse = new Color3(1, 1, 1); // White light
      this.light.specular = new Color3(0, 0, 0); // No specular
      this.light.groundColor = new Color3(0.3, 0.3, 0.3); // Dim ground light

      logger.debug('Environment initialized', {
        lightIntensity: this.light.intensity,
        backgroundColor: this.scene.clearColor,
      });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'EnvironmentService.initializeEnvironment');
    }
  }

  /**
   * Get the main light
   */
  getLight(): HemisphericLight | undefined {
    return this.light;
  }

  /**
   * Set light intensity
   *
   * @param intensity Light intensity (0-1 for normal, can go higher)
   */
  setLightIntensity(intensity: number): void {
    if (!this.light) {
      logger.warn('Cannot set light intensity: light not initialized');
      return;
    }

    this.light.intensity = intensity;
  }

  /**
   * Get light intensity
   */
  getLightIntensity(): number {
    return this.light?.intensity ?? 0;
  }

  /**
   * Set background color
   *
   * @param r Red (0-1)
   * @param g Green (0-1)
   * @param b Blue (0-1)
   */
  setBackgroundColor(r: number, g: number, b: number): void {
    this.scene.clearColor = new Color3(r, g, b).toColor4();
  }

  /**
   * Update environment (called each frame if needed)
   *
   * @param deltaTime Time since last frame in seconds
   */
  update(deltaTime: number): void {
    // Future: Add time-of-day lighting, weather effects, etc.
  }

  /**
   * Dispose environment
   */
  dispose(): void {
    this.light?.dispose();
    logger.info('Environment disposed');
  }
}
