/**
 * Wind Manager
 * Manages global wind parameters for the voxel environment
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
}

export class WindManager {
  private parameters: WindParameters;

  constructor() {
    // Initialize with sensible default values
    this.parameters = {
      windDirection: { x: 1, z: 0 },  // Default: wind from west (positive X direction)
      windStrength: 0.3,               // 30% base wind strength
      windGustStrength: 0.15,          // 15% gust strength (half of base)
      windSwayFactor: 1.0,             // 100% sway factor (neutral)
    };

    console.log('[WindManager] Initialized with default parameters:', this.parameters);
  }

  /**
   * Get current wind parameters
   */
  getParameters(): WindParameters {
    return { ...this.parameters };
  }

  /**
   * Set wind direction (normalizes the vector)
   */
  setWindDirection(x: number, z: number): void {
    // Normalize the direction vector
    const length = Math.sqrt(x * x + z * z);
    if (length > 0) {
      this.parameters.windDirection.x = x / length;
      this.parameters.windDirection.z = z / length;
    } else {
      // Default to east if zero vector provided
      this.parameters.windDirection.x = 1;
      this.parameters.windDirection.z = 0;
    }
    console.log(`[WindManager] Wind direction set to (${this.parameters.windDirection.x.toFixed(2)}, ${this.parameters.windDirection.z.toFixed(2)})`);
  }

  /**
   * Set wind strength (clamped to 0-1)
   */
  setWindStrength(strength: number): void {
    this.parameters.windStrength = Math.max(0, Math.min(1, strength));
    console.log(`[WindManager] Wind strength set to ${this.parameters.windStrength.toFixed(2)}`);
  }

  /**
   * Set wind gust strength (clamped to 0-1)
   */
  setWindGustStrength(strength: number): void {
    this.parameters.windGustStrength = Math.max(0, Math.min(1, strength));
    console.log(`[WindManager] Wind gust strength set to ${this.parameters.windGustStrength.toFixed(2)}`);
  }

  /**
   * Set wind sway factor (clamped to 0-2)
   */
  setWindSwayFactor(factor: number): void {
    this.parameters.windSwayFactor = Math.max(0, Math.min(2, factor));
    console.log(`[WindManager] Wind sway factor set to ${this.parameters.windSwayFactor.toFixed(2)}`);
  }

  /**
   * Get wind direction
   */
  getWindDirection(): { x: number; z: number } {
    return { ...this.parameters.windDirection };
  }

  /**
   * Get wind strength
   */
  getWindStrength(): number {
    return this.parameters.windStrength;
  }

  /**
   * Get wind gust strength
   */
  getWindGustStrength(): number {
    return this.parameters.windGustStrength;
  }

  /**
   * Get wind sway factor
   */
  getWindSwayFactor(): number {
    return this.parameters.windSwayFactor;
  }
}
