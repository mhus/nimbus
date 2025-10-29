/**
 * ShaderService - Manages shader effects for blocks
 *
 * Provides shader effects based on BlockModifier.visibility.effect parameter.
 * Initial implementation is a placeholder for future shader system.
 *
 * Future effects will include:
 * - water: Water wave shader
 * - lava: Lava wave shader
 * - wind: Wind animation shader
 * - fog: Fog effect shader
 * - flipbox: Rotating box shader
 */

import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { Material } from '@babylonjs/core';

const logger = getLogger('ShaderService');

/**
 * Shader effect definition
 */
export interface ShaderEffect {
  /** Effect name */
  name: string;

  /** Create material for this effect */
  createMaterial: (params?: Record<string, any>) => Material | null;
}

/**
 * ShaderService - Manages shader effects
 *
 * This is a placeholder service for future shader implementation.
 * Shaders will be implemented later based on BlockModifier.visibility.effect parameter.
 */
export class ShaderService {
  private effects: Map<string, ShaderEffect> = new Map();

  constructor(private appContext: AppContext) {
    logger.info('ShaderService initialized');
  }

  /**
   * Register a shader effect
   *
   * @param effect Shader effect to register
   */
  registerEffect(effect: ShaderEffect): void {
    this.effects.set(effect.name, effect);
    logger.debug('Registered shader effect', { name: effect.name });
  }

  /**
   * Get a shader effect by name
   *
   * @param name Effect name (from BlockModifier.visibility.effect)
   * @returns Shader effect or undefined if not found
   */
  getEffect(name: string): ShaderEffect | undefined {
    return this.effects.get(name);
  }

  /**
   * Check if an effect is registered
   *
   * @param name Effect name
   * @returns True if effect is registered
   */
  hasEffect(name: string): boolean {
    return this.effects.has(name);
  }

  /**
   * Create a material for an effect
   *
   * @param effectName Effect name (from BlockModifier.visibility.effect)
   * @param params Effect-specific parameters (from BlockModifier.visibility.effectParameters)
   * @returns Material or null if effect not found
   */
  createMaterial(effectName: string, params?: Record<string, any>): Material | null {
    const effect = this.effects.get(effectName);
    if (!effect) {
      logger.debug('Shader effect not found', { effectName });
      return null;
    }

    try {
      return effect.createMaterial(params);
    } catch (error) {
      logger.error('Failed to create material for effect', { effectName }, error as Error);
      return null;
    }
  }

  /**
   * Get all registered effect names
   *
   * @returns Array of effect names
   */
  getEffectNames(): string[] {
    return Array.from(this.effects.keys());
  }

  /**
   * Clear all registered effects
   *
   * Useful for testing or when switching worlds
   */
  clear(): void {
    this.effects.clear();
    logger.info('Shader effects cleared');
  }
}
