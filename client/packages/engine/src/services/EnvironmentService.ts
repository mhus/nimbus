/**
 * EnvironmentService - Manages scene environment
 *
 * Handles lighting, sky, fog, and other environmental effects.
 */

import { HemisphericLight, DirectionalLight, Vector3, Color3, Scene } from '@babylonjs/core';
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { ScriptActionDefinition } from '@nimbus/shared';

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
 * Environment script definition
 * Scripts are stored by name and can be executed in groups
 */
export interface EnvironmentScript {
  /** Script name (key) */
  name: string;

  /** Script group (e.g., 'environment', 'weather', 'daytime') */
  group: string;

  /** Script action definition */
  script: ScriptActionDefinition;
}

/**
 * Running environment script information
 */
interface RunningEnvironmentScript {
  /** Script name */
  name: string;

  /** Script group */
  group: string;

  /** Executor ID from ScrawlService */
  executorId: string;

  /** Start time (timestamp) */
  startTime: number;
}

/**
 * World Time configuration
 */
export interface WorldTimeConfig {
  /** @Minute scaling: How many world minutes pass per real minute */
  minuteScaling: number;
  /** @Hour: How many @Minutes in one @Hour */
  minutesPerHour: number;
  /** @Day: How many @Hours in one @Day */
  hoursPerDay: number;
  /** @Month: How many @Days in one @Month */
  daysPerMonth: number;
  /** @Year: How many @Months in one @Year */
  monthsPerYear: number;
  /** @Era: How many @Years in one @Era */
  yearsPerEra: number;
}

/**
 * Day section definitions
 */
export interface DaySectionConfig {
  /** Morning start @Hour */
  morningStart: number;
  /** Day start @Hour */
  dayStart: number;
  /** Evening start @Hour */
  eveningStart: number;
  /** Night start @Hour */
  nightStart: number;
}

/**
 * Day section type
 */
export type DaySection = 'morning' | 'day' | 'evening' | 'night';

/**
 * World Time state
 */
interface WorldTimeState {
  /** Is world time running */
  running: boolean;
  /** Start world time in @Minutes */
  startWorldMinute: number;
  /** Real time when world time was started (timestamp in ms) */
  startRealTime: number;
}

/**
 * Celestial bodies configuration
 */
export interface CelestialBodiesConfig {
  /** Enable automatic sun/moon position updates */
  enabled: boolean;
  /** Update interval in seconds */
  updateIntervalSeconds: number;
  /** Number of active moons (0-3) */
  activeMoons: number;
  /** Full rotation time for sun in @Hours */
  sunRotationHours: number;
  /** Full rotation time for moon 0 in @Hours */
  moon0RotationHours: number;
  /** Full rotation time for moon 1 in @Hours */
  moon1RotationHours: number;
  /** Full rotation time for moon 2 in @Hours */
  moon2RotationHours: number;
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

  private ambientLight?: HemisphericLight;
  private sunLight?: DirectionalLight;

  // Wind parameters
  private windParameters: WindParameters;

  // Ambient audio modifier (priority 50)
  private ambientAudioModifier?: any; // Modifier<string>

  // Environment script management
  private environmentScripts: Map<string, EnvironmentScript> = new Map();
  private runningScripts: Map<string, RunningEnvironmentScript> = new Map();

  // World Time management
  private worldTimeConfig: WorldTimeConfig;
  private daySectionConfig: DaySectionConfig;
  private worldTimeState: WorldTimeState;
  private currentDaySection: DaySection | null = null;

  // Celestial bodies automatic update
  private celestialBodiesConfig: CelestialBodiesConfig;
  private celestialUpdateTimer: number = 0;
  private lastCelestialUpdateTime: number = 0;
  private sunPositionModifier?: any;
  private moonPositionModifiers: Map<number, any> = new Map();

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

    // Initialize World Time configuration with defaults
    this.worldTimeConfig = this.loadWorldTimeConfigFromWorldInfo();
    this.daySectionConfig = this.loadDaySectionConfigFromWorldInfo();
    this.celestialBodiesConfig = this.loadCelestialBodiesConfigFromWorldInfo();
    this.worldTimeState = {
      running: false,
      startWorldMinute: 0,
      startRealTime: 0,
    };

    this.initializeEnvironment();
    this.initializeAmbientAudioModifier();
    this.loadEnvironmentScriptsFromWorldInfo();

    logger.info('EnvironmentService initialized', {
      windParameters: this.windParameters,
      worldTimeConfig: this.worldTimeConfig,
      daySectionConfig: this.daySectionConfig,
      celestialBodiesConfig: this.celestialBodiesConfig,
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
      // Set background color from WorldInfo or use default (light blue sky)
      const settings = this.appContext.worldInfo?.settings;
      const clearColor = settings?.clearColor
        ? new Color3(settings.clearColor.r, settings.clearColor.g, settings.clearColor.b)
        : new Color3(0.5, 0.7, 1.0); // Default sky blue

      this.scene.clearColor = clearColor.toColor4();

      // Create ambient hemispheric light
      this.ambientLight = new HemisphericLight('ambientLight', new Vector3(0, 1, 0), this.scene);

      // Set ambient light properties
      this.ambientLight.intensity = 1.0;
      this.ambientLight.diffuse = new Color3(1, 1, 1); // White light
      this.ambientLight.specular = new Color3(0, 0, 0); // No specular
      this.ambientLight.groundColor = new Color3(0.3, 0.3, 0.3); // Dim ground light

      // Create sun directional light
      this.sunLight = new DirectionalLight('sunLight', new Vector3(-1, -2, -1), this.scene);

      // Set sun light properties
      this.sunLight.intensity = 0.8;
      this.sunLight.diffuse = new Color3(1, 0.95, 0.9); // Warm sunlight
      this.sunLight.specular = new Color3(1, 1, 1); // White specular highlights

      logger.debug('Environment initialized', {
        ambientLightIntensity: this.ambientLight.intensity,
        sunLightIntensity: this.sunLight.intensity,
        backgroundColor: this.scene.clearColor,
      });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'EnvironmentService.initializeEnvironment');
    }
  }

  /**
   * Get the ambient light
   */
  getAmbientLight(): HemisphericLight | undefined {
    return this.ambientLight;
  }

  /**
   * Get the sun light
   */
  getSunLight(): DirectionalLight | undefined {
    return this.sunLight;
  }

  // ============================================
  // Ambient Light Management
  // ============================================

  /**
   * Set ambient light intensity
   *
   * @param intensity Light intensity (0-1 for normal, can go higher)
   */
  setAmbientLightIntensity(intensity: number): void {
    if (!this.ambientLight) {
      logger.warn('Cannot set ambient light intensity: light not initialized');
      return;
    }

    this.ambientLight.intensity = intensity;
    logger.info('Ambient light intensity set', { intensity });
  }

  /**
   * Get ambient light intensity
   */
  getAmbientLightIntensity(): number {
    return this.ambientLight?.intensity ?? 0;
  }

  /**
   * Set ambient light specular color
   *
   * @param color Specular color
   */
  setAmbientLightSpecularColor(color: Color3): void {
    if (!this.ambientLight) {
      logger.warn('Cannot set ambient light specular color: light not initialized');
      return;
    }

    this.ambientLight.specular = color;
    logger.info('Ambient light specular color set', { color });
  }

  /**
   * Set ambient light diffuse color
   *
   * @param color Diffuse color
   */
  setAmbientLightDiffuse(color: Color3): void {
    if (!this.ambientLight) {
      logger.warn('Cannot set ambient light diffuse color: light not initialized');
      return;
    }

    this.ambientLight.diffuse = color;
    logger.info('Ambient light diffuse color set', { color });
  }

  /**
   * Set ambient light ground color
   *
   * @param color Ground color
   */
  setAmbientLightGroundColor(color: Color3): void {
    if (!this.ambientLight) {
      logger.warn('Cannot set ambient light ground color: light not initialized');
      return;
    }

    this.ambientLight.groundColor = color;
    logger.info('Ambient light ground color set', { color });
  }

  // ============================================
  // Sun Light Management
  // ============================================

  /**
   * Set sun light intensity
   *
   * @param intensity Light intensity (0-1 for normal, can go higher)
   */
  setSunLightIntensity(intensity: number): void {
    if (!this.sunLight) {
      logger.warn('Cannot set sun light intensity: light not initialized');
      return;
    }

    this.sunLight.intensity = intensity;
    logger.info('Sun light intensity set', { intensity });
  }

  /**
   * Get sun light intensity
   */
  getSunLightIntensity(): number {
    return this.sunLight?.intensity ?? 0;
  }

  /**
   * Set sun light direction (vector will be normalized)
   *
   * @param x X component of direction
   * @param y Y component of direction
   * @param z Z component of direction
   */
  setSunLightDirection(x: number, y: number, z: number): void {
    if (!this.sunLight) {
      logger.warn('Cannot set sun light direction: light not initialized');
      return;
    }

    const direction = new Vector3(x, y, z).normalize();
    this.sunLight.direction = direction;
    logger.info('Sun light direction set', { x: direction.x, y: direction.y, z: direction.z });
  }

  /**
   * Get sun light direction
   */
  getSunLightDirection(): Vector3 {
    return this.sunLight?.direction ?? new Vector3(0, -1, 0);
  }

  /**
   * Set sun light diffuse color
   *
   * @param color Diffuse color
   */
  setSunLightDiffuse(color: Color3): void {
    if (!this.sunLight) {
      logger.warn('Cannot set sun light diffuse color: light not initialized');
      return;
    }

    this.sunLight.diffuse = color;
    logger.info('Sun light diffuse color set', { color });
  }

  /**
   * Set sun light specular color
   *
   * @param color Specular color
   */
  setSunLightSpecularColor(color: Color3): void {
    if (!this.sunLight) {
      logger.warn('Cannot set sun light specular color: light not initialized');
      return;
    }

    this.sunLight.specular = color;
    logger.info('Sun light specular color set', { color });
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
    this.updateWorldTime(deltaTime);
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

  // ============================================
  // Environment Script Management
  // ============================================

  /**
   * Load environment scripts from WorldInfo
   */
  private loadEnvironmentScriptsFromWorldInfo(): void {
    const worldInfo = this.appContext.worldInfo;
    if (!worldInfo?.settings?.environmentScripts) {
      logger.info('No environment scripts defined in WorldInfo');
      return;
    }

    const scripts = worldInfo.settings.environmentScripts;
    if (Array.isArray(scripts)) {
      for (const scriptDef of scripts) {
        if (scriptDef.name && scriptDef.group && scriptDef.script) {
          this.environmentScripts.set(scriptDef.name, scriptDef);
          logger.info('Loaded environment script from WorldInfo', {
            name: scriptDef.name,
            group: scriptDef.group,
          });
        }
      }
    }
    logger.info(`Loaded ${this.environmentScripts.size} environment scripts total`);
  }

  /**
   * Create/register an environment script
   *
   * @param name Script name (unique identifier)
   * @param group Script group (e.g., 'environment', 'weather')
   * @param script Script action definition
   */
  createEnvironmentScript(name: string, group: string, script: ScriptActionDefinition): void {
    const environmentScript: EnvironmentScript = {
      name,
      group,
      script,
    };

    this.environmentScripts.set(name, environmentScript);
    logger.info('Environment script created', { name, group });
  }

  /**
   * Delete an environment script
   *
   * @param name Script name
   */
  deleteEnvironmentScript(name: string): boolean {
    const existed = this.environmentScripts.delete(name);
    if (existed) {
      logger.info('Environment script deleted', { name });
    } else {
      logger.warn('Environment script not found for deletion', { name });
    }
    return existed;
  }

  /**
   * Get an environment script by name
   *
   * @param name Script name
   */
  getEnvironmentScript(name: string): EnvironmentScript | undefined {
    return this.environmentScripts.get(name);
  }

  /**
   * Get all environment scripts
   */
  getAllEnvironmentScripts(): EnvironmentScript[] {
    return Array.from(this.environmentScripts.values());
  }

  /**
   * Start an environment script
   * If a script is already running in the same group, it will be stopped first
   *
   * @param name Script name
   * @returns Executor ID or null if script not found or ScrawlService unavailable
   */
  async startEnvironmentScript(name: string): Promise<string | null> {
    const scriptDef = this.environmentScripts.get(name);
    if (!scriptDef) {
      logger.error('Environment script not found', { name });
      return null;
    }

    const scrawlService = this.appContext.services.scrawl;
    if (!scrawlService) {
      logger.error('ScrawlService not available');
      return null;
    }

    // Stop any running script in the same group
    await this.stopEnvironmentScriptByGroup(scriptDef.group);

    try {
      // Use script action definition directly (already has script field)
      // Ensure it's executed locally only
      const scriptAction: ScriptActionDefinition = {
        ...scriptDef.script,
        sendToServer: false, // Override: Always execute locally
      };

      logger.debug('Starting environment script', {
        name: scriptDef.name,
        group: scriptDef.group,
      });

      const executorId = await scrawlService.executeAction(scriptAction);

      const runningScript: RunningEnvironmentScript = {
        name: scriptDef.name,
        group: scriptDef.group,
        executorId,
        startTime: Date.now(),
      };

      this.runningScripts.set(scriptDef.group, runningScript);

      logger.info('Environment script started', {
        name: scriptDef.name,
        group: scriptDef.group,
        executorId,
      });

      return executorId;
    } catch (error: any) {
      logger.error('Failed to start environment script', {
        name,
        error: error.message,
      });
      return null;
    }
  }

  /**
   * Stop environment script by group
   *
   * @param group Script group
   */
  async stopEnvironmentScriptByGroup(group: string): Promise<boolean> {
    const runningScript = this.runningScripts.get(group);
    if (!runningScript) {
      logger.info('No running script in group', { group });
      return false;
    }

    const scrawlService = this.appContext.services.scrawl;
    if (!scrawlService) {
      logger.error('ScrawlService not available');
      return false;
    }

    const cancelled = scrawlService.cancelExecutor(runningScript.executorId);
    if (cancelled) {
      this.runningScripts.delete(group);

      logger.info('Environment script stopped', {
        name: runningScript.name,
        group: runningScript.group,
        executorId: runningScript.executorId,
      });

      return true;
    } else {
      logger.error('Failed to stop environment script', {
        group,
        executorId: runningScript.executorId,
      });
      return false;
    }
  }

  /**
   * Get current running script name for a group
   *
   * @param group Script group
   * @returns Script name or null if no script is running in the group
   */
  getCurrentEnvironmentScriptName(group: string): string | null {
    const runningScript = this.runningScripts.get(group);
    return runningScript?.name ?? null;
  }

  /**
   * Get all running scripts
   */
  getRunningEnvironmentScripts(): RunningEnvironmentScript[] {
    return Array.from(this.runningScripts.values());
  }

  // ============================================
  // World Time Management
  // ============================================

  /**
   * Load World Time configuration from WorldInfo
   */
  private loadWorldTimeConfigFromWorldInfo(): WorldTimeConfig {
    const worldTime = this.appContext.worldInfo?.settings?.worldTime;

    return {
      minuteScaling: worldTime?.minuteScaling ?? 10,
      minutesPerHour: worldTime?.minutesPerHour ?? 60,
      hoursPerDay: worldTime?.hoursPerDay ?? 24,
      daysPerMonth: worldTime?.daysPerMonth ?? 30,
      monthsPerYear: worldTime?.monthsPerYear ?? 12,
      yearsPerEra: worldTime?.yearsPerEra ?? 10000,
    };
  }

  /**
   * Load Day Section configuration from WorldInfo
   */
  private loadDaySectionConfigFromWorldInfo(): DaySectionConfig {
    const daySections = this.appContext.worldInfo?.settings?.worldTime?.daySections;

    return {
      morningStart: daySections?.morningStart ?? 6,
      dayStart: daySections?.dayStart ?? 12,
      eveningStart: daySections?.eveningStart ?? 18,
      nightStart: daySections?.nightStart ?? 0,
    };
  }

  /**
   * Load Celestial Bodies configuration from WorldInfo
   */
  private loadCelestialBodiesConfigFromWorldInfo(): CelestialBodiesConfig {
    const celestialBodies = this.appContext.worldInfo?.settings?.worldTime?.celestialBodies;

    return {
      enabled: celestialBodies?.enabled ?? false,
      updateIntervalSeconds: celestialBodies?.updateIntervalSeconds ?? 10,
      activeMoons: celestialBodies?.activeMoons ?? 0,
      sunRotationHours: celestialBodies?.sunRotationHours ?? 24,
      moon0RotationHours: celestialBodies?.moon0RotationHours ?? 672, // 28 days
      moon1RotationHours: celestialBodies?.moon1RotationHours ?? 504, // 21 days
      moon2RotationHours: celestialBodies?.moon2RotationHours ?? 336, // 14 days
    };
  }

  /**
   * Set World Time configuration
   * Command: worldTimeConfig <minuteScaling> <hoursPerDay> <daysPerMonth> <monthsPerYear> <yearsPerEra>
   */
  setWorldTimeConfig(
    minuteScaling: number,
    minutesPerHour: number,
    hoursPerDay: number,
    daysPerMonth: number,
    monthsPerYear: number,
    yearsPerEra: number
  ): void {
    this.worldTimeConfig = {
      minuteScaling: Math.max(0.1, minuteScaling),
      minutesPerHour: Math.max(1, minutesPerHour),
      hoursPerDay: Math.max(1, hoursPerDay),
      daysPerMonth: Math.max(1, daysPerMonth),
      monthsPerYear: Math.max(1, monthsPerYear),
      yearsPerEra: Math.max(1, yearsPerEra),
    };

    logger.info('World Time config updated', this.worldTimeConfig);
  }

  /**
   * Get World Time configuration
   */
  getWorldTimeConfig(): WorldTimeConfig {
    return { ...this.worldTimeConfig };
  }

  /**
   * Start World Time
   * Command: worldTimeStart <worldMinute>
   *
   * @param worldMinute Start time in @Minutes since @0.1.1.0000 00:00:00
   */
  startWorldTime(worldMinute: number): void {
    if (this.worldTimeState.running) {
      logger.warn('World Time is already running, stopping first');
      this.stopWorldTime();
    }

    this.worldTimeState = {
      running: true,
      startWorldMinute: worldMinute,
      startRealTime: Date.now(),
    };

    // Set initial day section
    this.currentDaySection = this.getWorldDayTimeSection();

    // Start script for initial day section
    const dayScriptName = `daytime_change_${this.currentDaySection}`;
    this.startEnvironmentScript(dayScriptName);

    // Start script for current season
    const seasonStatus = this.appContext.worldInfo?.seasonStatus;
    if (seasonStatus !== undefined && seasonStatus !== 0) {
      // Map season status to script name
      const seasonNames = ['', 'winter', 'spring', 'summer', 'autumn'];
      const seasonName = seasonNames[seasonStatus];
      if (seasonName) {
        const seasonScriptName = `season_${seasonName}`;
        this.startEnvironmentScript(seasonScriptName);
        logger.info('Started season script', { seasonStatus, seasonScriptName });
      }
    }

    logger.info('World Time started', {
      startWorldMinute: worldMinute,
      startWorldTime: this.getWorldTimeCurrentAsString(),
      daySection: this.currentDaySection,
      initialDayScript: dayScriptName,
      seasonStatus,
    });
  }

  /**
   * Stop World Time
   * Command: worldTimeStop
   */
  stopWorldTime(): void {
    if (!this.worldTimeState.running) {
      logger.warn('World Time is not running');
      return;
    }

    const currentWorldTime = this.getWorldTimeCurrent();

    this.worldTimeState = {
      running: false,
      startWorldMinute: 0,
      startRealTime: 0,
    };

    this.currentDaySection = null;

    logger.info('World Time stopped', {
      stoppedAt: currentWorldTime,
      stoppedAtFormatted: this.formatWorldTime(currentWorldTime),
    });
  }

  /**
   * Check if World Time is running
   */
  isWorldTimeRunning(): boolean {
    return this.worldTimeState.running;
  }

  /**
   * Get current World Time in @Minutes
   */
  getWorldTimeCurrent(): number {
    if (!this.worldTimeState.running) {
      return 0;
    }

    const realElapsedMs = Date.now() - this.worldTimeState.startRealTime;
    const realElapsedMinutes = realElapsedMs / (1000 * 60);
    const worldElapsedMinutes = realElapsedMinutes * this.worldTimeConfig.minuteScaling;

    return this.worldTimeState.startWorldMinute + worldElapsedMinutes;
  }

  /**
   * Get current World Time as formatted string
   * Format: @era, @year.@month.@day, @hour:@minute
   */
  getWorldTimeCurrentAsString(): string {
    const worldMinute = this.getWorldTimeCurrent();
    return this.formatWorldTime(worldMinute);
  }

  /**
   * Format world time in minutes to string
   * Format: @era, @year.@month.@day, @hour:@minute
   *
   * @param worldMinute World time in @Minutes
   */
  private formatWorldTime(worldMinute: number): string {
    const config = this.worldTimeConfig;

    // Calculate time components
    let remainingMinutes = Math.floor(worldMinute);

    const minute = remainingMinutes % config.minutesPerHour;
    remainingMinutes = Math.floor(remainingMinutes / config.minutesPerHour);

    const hour = remainingMinutes % config.hoursPerDay;
    remainingMinutes = Math.floor(remainingMinutes / config.hoursPerDay);

    const day = (remainingMinutes % config.daysPerMonth) + 1; // Days start at 1
    remainingMinutes = Math.floor(remainingMinutes / config.daysPerMonth);

    const month = (remainingMinutes % config.monthsPerYear) + 1; // Months start at 1
    remainingMinutes = Math.floor(remainingMinutes / config.monthsPerYear);

    const year = remainingMinutes % config.yearsPerEra;
    const era = Math.floor(remainingMinutes / config.yearsPerEra);

    return `@${era}, @${year}.${month}.${day}, ${hour}:${minute.toString().padStart(2, '0')}`;
  }

  /**
   * Get current day time section
   */
  getWorldDayTimeSection(): DaySection {
    const worldMinute = this.getWorldTimeCurrent();
    const config = this.worldTimeConfig;

    // Calculate current hour of the day
    const totalMinutesInDay = config.minutesPerHour * config.hoursPerDay;
    const minuteOfDay = worldMinute % totalMinutesInDay;
    const hourOfDay = Math.floor(minuteOfDay / config.minutesPerHour);

    // Determine day section
    const sections = this.daySectionConfig;

    if (hourOfDay >= sections.morningStart && hourOfDay < sections.dayStart) {
      return 'morning';
    } else if (hourOfDay >= sections.dayStart && hourOfDay < sections.eveningStart) {
      return 'day';
    } else if (hourOfDay >= sections.eveningStart && hourOfDay < config.hoursPerDay) {
      return 'evening';
    } else {
      return 'night';
    }
  }

  /**
   * Update World Time (called each frame)
   * Checks for day section changes and triggers scripts
   *
   * @param deltaTime Time since last frame in seconds
   */
  private updateWorldTime(deltaTime: number): void {
    if (!this.worldTimeState.running) {
      return;
    }

    const newDaySection = this.getWorldDayTimeSection();

    if (this.currentDaySection !== newDaySection) {
      logger.info('Day section changed', {
        from: this.currentDaySection,
        to: newDaySection,
        worldTime: this.getWorldTimeCurrentAsString(),
      });

      this.currentDaySection = newDaySection;

      // Start corresponding environment script
      const scriptName = `daytime_change_${newDaySection}`;
      this.startEnvironmentScript(scriptName);
    }

    // Update celestial bodies positions if enabled
    this.updateCelestialBodies(deltaTime);
  }

  /**
   * Update celestial bodies positions (sun and moons)
   * Called every frame but only updates at configured intervals
   *
   * @param deltaTime Time since last frame in seconds
   */
  private updateCelestialBodies(deltaTime: number): void {
    if (!this.celestialBodiesConfig.enabled) {
      return;
    }

    // Accumulate time
    this.celestialUpdateTimer += deltaTime;

    // Check if update interval has passed
    if (this.celestialUpdateTimer < this.celestialBodiesConfig.updateIntervalSeconds) {
      return;
    }

    // Reset timer
    this.celestialUpdateTimer = 0;

    // Get modifier service
    const modifierService = this.appContext.services.modifier;
    if (!modifierService) {
      return;
    }

    // Get current world time
    const currentWorldMinute = this.getWorldTimeCurrent();
    const currentWorldHour = currentWorldMinute / this.worldTimeConfig.minutesPerHour;

    // Update sun position
    this.updateSunPosition(modifierService, currentWorldHour);

    // Update moon positions
    for (let i = 0; i < this.celestialBodiesConfig.activeMoons && i < 3; i++) {
      this.updateMoonPosition(modifierService, i, currentWorldHour);
    }

    // Removed debug log for performance
  }

  /**
   * Update sun position based on world time
   */
  private updateSunPosition(modifierService: any, currentWorldHour: number): void {
    const sunRotationHours = this.celestialBodiesConfig.sunRotationHours;

    // Calculate sun position (0-360 degrees)
    // At hour 0, sun is at 0° (North)
    // Sun rotates 360° over sunRotationHours
    const sunAngle = (currentWorldHour / sunRotationHours) * 360;
    const sunAngleNormalized = sunAngle % 360;

    // Get sun position stack
    const sunPositionStack = modifierService.getModifierStack('sunPosition');
    if (sunPositionStack) {
      // Create modifier once and reuse it (priority 40, lower than manual scripts at 50+)
      if (!this.sunPositionModifier) {
        this.sunPositionModifier = sunPositionStack.addModifier(sunAngleNormalized, 40);
      } else {
        this.sunPositionModifier.setValue(sunAngleNormalized);
      }
      this.sunPositionModifier.setEnabled(true);
    }
  }

  /**
   * Update moon position based on world time
   */
  private updateMoonPosition(
    modifierService: any,
    moonIndex: number,
    currentWorldHour: number
  ): void {
    let moonRotationHours: number;
    let stackName: string;

    // Select rotation hours and stack name based on moon index
    switch (moonIndex) {
      case 0:
        moonRotationHours = this.celestialBodiesConfig.moon0RotationHours;
        stackName = 'moon0Position';
        break;
      case 1:
        moonRotationHours = this.celestialBodiesConfig.moon1RotationHours;
        stackName = 'moon1Position';
        break;
      case 2:
        moonRotationHours = this.celestialBodiesConfig.moon2RotationHours;
        stackName = 'moon2Position';
        break;
      default:
        return;
    }

    // Calculate moon position (0-360 degrees)
    // Moon rotates 360° over moonRotationHours
    const moonAngle = (currentWorldHour / moonRotationHours) * 360;
    const moonAngleNormalized = moonAngle % 360;

    // Get moon position stack
    const moonPositionStack = modifierService.getModifierStack(stackName);
    if (moonPositionStack) {
      // Create modifier once per moon and reuse it (priority 40, lower than manual scripts)
      let moonModifier = this.moonPositionModifiers.get(moonIndex);
      if (!moonModifier) {
        moonModifier = moonPositionStack.addModifier(moonAngleNormalized, 40);
        this.moonPositionModifiers.set(moonIndex, moonModifier);
      } else {
        moonModifier.setValue(moonAngleNormalized);
      }
      moonModifier.setEnabled(true);
    }
  }

  /**
   * Reset environment to default state
   * Clears clouds and stops precipitation
   */
  resetEnvironment(): void {
    const cloudService = this.appContext.services.clouds;
    if (cloudService) {
      cloudService.clearClouds(false);
      logger.info('Environment reset: clouds cleared');
    } else {
      logger.warn('CloudService not available for environment reset');
    }

    const precipitationService = this.appContext.services.precipitation;
    if (precipitationService) {
      precipitationService.setEnabled(false);
      logger.info('Environment reset: precipitation stopped');
    } else {
      logger.warn('PrecipitationService not available for environment reset');
    }

    logger.info('Environment reset completed');
  }

  /**
   * Dispose environment
   */
  dispose(): void {
    // Stop world time
    if (this.worldTimeState.running) {
      this.stopWorldTime();
    }

    // Stop all running scripts
    for (const group of this.runningScripts.keys()) {
      this.stopEnvironmentScriptByGroup(group);
    }

    this.ambientLight?.dispose();
    this.sunLight?.dispose();
    logger.info('Environment disposed');
  }
}
