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
 * Wind parameters for environment
 */
export interface WindParameters {
  /** Wind direction as a 2D vector (x, z) - normalized */
  windDirection: { x: number; z: number };

  /** Base wind strength (0-1) */
  windStrength: number;

  /** Wind gust strength (0-1) - additional random wind impulses */
  windGustStrength: number;

  /** Wind sway factor (0-2) - multiplier for how much blocks sway */
  windSwayFactor: number;

  /** Current time for wind animation */
  time: number;
}

/**
 * EnvironmentService - Manages environment rendering
 *
 * Features:
 * - Hemispheric lighting
 * - Background color
 * - Wind parameters for wind-affected blocks
 * - Future: Sky, fog, weather effects
 */
export class EnvironmentService {
  private scene: Scene;
  private appContext: AppContext;

  private light?: HemisphericLight;

  // Wind parameters
  private windParameters: WindParameters;

  // Ambient audio modifier (priority 50)
  private ambientAudioModifier?: any; // Modifier<string>

  constructor(scene: Scene, appContext: AppContext) {
    this.scene = scene;
    this.appContext = appContext;

    // Initialize wind parameters with defaults
    this.windParameters = {
      windDirection: { x: 1, z: 0 }, // Default: wind from west (positive X)
      windStrength: 0.3, // 30% base wind strength
      windGustStrength: 0.15, // 15% gust strength
      windSwayFactor: 1.0, // 100% sway factor (neutral)
      time: 0, // Initialize time
    };

    this.initializeEnvironment();
    this.initializeAmbientAudioModifier();

    logger.info('EnvironmentService initialized', {
      windParameters: this.windParameters,
    });
  }

  /**
   * Initialize ambient audio modifier
   * Environment can set ambient music at priority 50
   */
  private initializeAmbientAudioModifier(): void {
    const modifierService = this.appContext.services.modifier;
    if (!modifierService) {
      logger.warn('ModifierService not available, ambient audio modifier not created');
      return;
    }

    const stack = modifierService.getModifierStack<string>('ambientAudio');
    if (stack) {
      // Create environment modifier (priority 50)
      this.ambientAudioModifier = stack.addModifier('', 50);
      this.ambientAudioModifier.setEnabled(false); // Disabled by default
      logger.info('Environment ambient audio modifier created', { prio: 50 });
    }
  }

  /**
   * Set environment ambient audio
   * @param soundPath Path to ambient music (empty to clear)
   */
  setEnvironmentAmbientAudio(soundPath: string): void {
    if (!this.ambientAudioModifier) {
      logger.warn('Ambient audio modifier not initialized');
      return;
    }

    this.ambientAudioModifier.setValue(soundPath);
    this.ambientAudioModifier.setEnabled(soundPath.trim() !== '');

    logger.info('Environment ambient audio set', { soundPath, enabled: soundPath.trim() !== '' });
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

  // ============================================
  // Wind Parameter Management
  // ============================================

  /**
   * Get current wind parameters
   */
  getWindParameters(): WindParameters {
    return { ...this.windParameters };
  }

  /**
   * Set wind direction (normalizes the vector)
   * @param x X component of wind direction
   * @param z Z component of wind direction
   */
  setWindDirection(x: number, z: number): void {
    // Normalize the direction vector
    const length = Math.sqrt(x * x + z * z);
    if (length > 0) {
      this.windParameters.windDirection.x = x / length;
      this.windParameters.windDirection.z = z / length;
    } else {
      // Default to east if zero vector provided
      this.windParameters.windDirection.x = 1;
      this.windParameters.windDirection.z = 0;
    }

    logger.debug('Wind direction set', {
      x: this.windParameters.windDirection.x.toFixed(2),
      z: this.windParameters.windDirection.z.toFixed(2),
    });
  }

  /**
   * Get wind direction
   */
  getWindDirection(): { x: number; z: number } {
    return { ...this.windParameters.windDirection };
  }

  /**
   * Set wind strength (clamped to 0-1)
   * @param strength Wind strength (0-1)
   */
  setWindStrength(strength: number): void {
    this.windParameters.windStrength = Math.max(0, Math.min(1, strength));
    logger.debug('Wind strength set', {
      strength: this.windParameters.windStrength.toFixed(2),
    });
  }

  /**
   * Get wind strength
   */
  getWindStrength(): number {
    return this.windParameters.windStrength;
  }

  /**
   * Set wind gust strength (clamped to 0-1)
   * @param strength Gust strength (0-1)
   */
  setWindGustStrength(strength: number): void {
    this.windParameters.windGustStrength = Math.max(0, Math.min(1, strength));
    logger.debug('Wind gust strength set', {
      gustStrength: this.windParameters.windGustStrength.toFixed(2),
    });
  }

  /**
   * Get wind gust strength
   */
  getWindGustStrength(): number {
    return this.windParameters.windGustStrength;
  }

  /**
   * Set wind sway factor (clamped to 0-2)
   * @param factor Sway factor (0-2)
   */
  setWindSwayFactor(factor: number): void {
    this.windParameters.windSwayFactor = Math.max(0, Math.min(2, factor));
    logger.debug('Wind sway factor set', {
      swayFactor: this.windParameters.windSwayFactor.toFixed(2),
    });
  }

  /**
   * Get wind sway factor
   */
  getWindSwayFactor(): number {
    return this.windParameters.windSwayFactor;
  }

  /**
   * Dispose environment
   */
  dispose(): void {
    this.light?.dispose();
    logger.info('Environment disposed');
  }
}
