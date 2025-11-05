/**
 * ShaderService - Manages shader effects for blocks
 *
 * Provides shader effects based on BlockModifier.visibility.effect parameter.
 *
 * Implemented effects:
 * - wind: Wind animation shader with physical displacement
 *
 * Future effects:
 * - water: Water wave shader
 * - lava: Lava wave shader
 * - fog: Fog effect shader
 * - flipbox: Rotating box shader
 */

import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import {
  Material,
  ShaderMaterial,
  Scene,
  Effect,
  Texture,
  Vector2,
  Vector3,
} from '@babylonjs/core';
import type { EnvironmentService, WindParameters } from './EnvironmentService';

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
 * Creates and manages shader materials for special block effects.
 * Integrates with EnvironmentService for wind parameters.
 */
export class ShaderService {
  private effects: Map<string, ShaderEffect> = new Map();
  private scene?: Scene;
  private environmentService?: EnvironmentService;

  // Wind shader materials for automatic parameter updates
  private windMaterials: ShaderMaterial[] = [];

  constructor(private appContext: AppContext) {
    logger.info('ShaderService initialized');
  }

  /**
   * Initialize shader service with scene
   * Must be called after scene is created
   */
  initialize(scene: Scene): void {
    this.scene = scene;

    // Register wind shader
    this.registerWindShader();

    // Register flipbox shader
    this.registerFlipboxShader();

    logger.debug('ShaderService initialized with scene');
  }

  /**
   * Set EnvironmentService for automatic wind parameter updates
   */
  setEnvironmentService(environmentService: EnvironmentService): void {
    this.environmentService = environmentService;
    logger.debug('EnvironmentService connected for automatic wind updates');

    // Update existing wind materials with current parameters
    if (this.windMaterials.length > 0) {
      const params = environmentService.getWindParameters();
      logger.debug('Initial wind parameters', params);
      this.updateWindMaterials(params);
    }

    // Setup automatic updates every frame
    if (this.scene) {
      this.scene.onBeforeRenderObservable.add(() => {
        if (this.environmentService && this.windMaterials.length > 0) {
          const params = this.environmentService.getWindParameters();
          this.updateWindMaterials(params);
        }
      });
    }
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
    this.windMaterials = [];
    logger.info('Shader effects cleared');
  }

  // ============================================
  // Wind Shader Implementation
  // ============================================

  /**
   * Register wind shader effect
   */
  private registerWindShader(): void {
    // Register wind shaders with Babylon.js
    this.registerWindShaderCode();

    // Register wind effect
    const windEffect: ShaderEffect = {
      name: 'wind',
      createMaterial: (params?: Record<string, any>) => {
        return this.createWindMaterial(params?.texture, params?.name);
      },
    };

    this.registerEffect(windEffect);
  }

  /**
   * Register custom wind shader code with Babylon.js
   */
  private registerWindShaderCode(): void {
    // Vertex shader with lighting and physical wind animation
    Effect.ShadersStore['windVertexShader'] = `
      precision highp float;

      // Attributes
      attribute vec3 position;
      attribute vec3 normal;
      attribute vec2 uv;
      attribute vec4 color;

      // Wind properties (per-vertex)
      attribute float windLeafiness;
      attribute float windStability;
      attribute float windLeverUp;
      attribute float windLeverDown;

      // Uniforms
      uniform mat4 worldViewProjection;
      uniform mat4 world;
      uniform float time;
      uniform vec2 windDirection;
      uniform float windStrength;
      uniform float windGustStrength;
      uniform float windSwayFactor;

      // Varyings to fragment shader
      varying vec2 vUV;
      varying vec4 vColor;
      varying vec3 vNormal;

      void main(void) {
        vec3 pos = position;

        // Get world position for phase shift
        vec4 worldPos = world * vec4(position, 1.0);

        // Base sway wave (smooth sinusoidal)
        float baseWave = sin(time * windSwayFactor + worldPos.x * 0.01 + worldPos.z * 0.01) * windStrength;

        // Gust effect (faster, irregular pulses)
        float gustWave = sin(time * windSwayFactor * 2.3 + worldPos.x * 0.01) * windGustStrength;
        gustWave *= sin(time * windSwayFactor * 0.7); // Modulate gust intensity

        // Secondary wave for more organic movement (leafiness effect)
        float leafiness = max(windLeafiness, 0.5);
        float leafWave = sin(time * windSwayFactor * 1.7 + worldPos.y * 0.01) * leafiness;

        // Combine all waves
        float totalWave = baseWave + gustWave * 0.5 + leafWave * 0.3;

        // Stability reduces movement (1.0 = stable/no movement, 0.0 = unstable/full movement)
        float stabilityFactor = 1.0 - windStability;

        // Shear direction (normalized wind direction in XZ plane)
        vec3 shearDir = vec3(windDirection.x, 0.0, windDirection.y);
        if (length(shearDir) > 0.01) {
          shearDir = normalize(shearDir);
        }

        // Pivot Y: base of the lever
        float pivotY = -windLeverDown;

        // Block height
        float blockHeight = 1.0;

        // Normalized height within block (0.0 at bottom, 1.0 at top)
        float h = clamp((position.y - pivotY) / (pivotY + blockHeight + windLeverUp), 0.0, 1.0);

        // Total height from pivot to top of this vertex
        float heightFromPivot = position.y - pivotY;

        // Calculate lever length for this vertex
        float leverAtThisHeight = mix(windLeverDown, windLeverUp, h);

        // Horizontal displacement at this height
        float horizontalDisp = totalWave * leverAtThisHeight * stabilityFactor * 0.05;

        // Apply horizontal shearing
        pos += shearDir * horizontalDisp * h;

        // Vertical compression (physics: when bent horizontally, becomes shorter)
        float horizontalDispTotal = abs(horizontalDisp * h);
        float compressionFactor = (horizontalDispTotal * horizontalDispTotal) / (2.0 * max(heightFromPivot, 0.1));
        pos.y -= compressionFactor;

        // Vertical leafiness movement (additional up/down movement for leaves)
        float verticalLeafWave = sin(time * windSwayFactor * 2.1 + worldPos.x * 0.015 + worldPos.z * 0.015) * leafiness;
        verticalLeafWave += cos(time * windSwayFactor * 1.3 + worldPos.z * 0.02) * leafiness * 0.5;
        pos.y += verticalLeafWave * 0.02 * h;

        // Transform to clip space
        gl_Position = worldViewProjection * vec4(pos, 1.0);

        // Pass to fragment shader
        vUV = uv;
        vColor = color;
        vNormal = normalize((world * vec4(normal, 0.0)).xyz);
      }
    `;

    // Fragment shader with proper lighting and alpha handling
    Effect.ShadersStore['windFragmentShader'] = `
      precision highp float;

      // Varyings from vertex shader
      varying vec2 vUV;
      varying vec4 vColor;
      varying vec3 vNormal;

      // Uniforms
      uniform sampler2D textureSampler;
      uniform vec3 lightDirection;

      void main(void) {
        // Sample texture
        vec4 texColor = texture2D(textureSampler, vUV);

        // Alpha test: discard transparent pixels
        if (texColor.a < 0.5) {
          discard;
        }

        // Apply vertex color tint
        vec4 finalColor = texColor * vColor;

        // Simple directional lighting
        float lightIntensity = max(dot(vNormal, lightDirection), 0.3);
        finalColor.rgb *= lightIntensity;

        // Output final color with full opacity
        gl_FragColor = vec4(finalColor.rgb, 1.0);
      }
    `;

    logger.debug('Wind shader code registered with Babylon.js');
  }

  /**
   * Create wind shader material
   */
  private createWindMaterial(texture: Texture | undefined, name: string = 'windMaterial'): ShaderMaterial | null {
    if (!this.scene) {
      logger.error('Cannot create wind material: Scene not initialized');
      return null;
    }

    logger.debug('Creating wind material', { name });

    const material = new ShaderMaterial(
      name,
      this.scene,
      {
        vertex: 'wind',
        fragment: 'wind',
      },
      {
        attributes: [
          'position',
          'normal',
          'uv',
          'color',
          'windLeafiness',
          'windStability',
          'windLeverUp',
          'windLeverDown',
        ],
        uniforms: [
          'worldViewProjection',
          'world',
          'time',
          'windDirection',
          'windStrength',
          'windGustStrength',
          'windSwayFactor',
          'textureSampler',
          'lightDirection',
        ],
        samplers: ['textureSampler'],
      }
    );

    // Error handling for shader compilation
    material.onError = (effect, errors) => {
      logger.error('Shader compilation error', { name, errors });
    };

    material.onCompiled = () => {
      logger.debug('Shader compiled successfully', { name });
    };

    // Set texture if provided
    if (texture) {
      material.setTexture('textureSampler', texture);
    }

    // Set default wind parameters (will be updated from EnvironmentService)
    material.setVector2('windDirection', new Vector2(1.0, 0.0));
    material.setFloat('windStrength', 0.8);
    material.setFloat('windGustStrength', 0.4);
    material.setFloat('windSwayFactor', 2.0);

    // Set light direction (from above-front)
    material.setVector3('lightDirection', new Vector3(0.5, 1.0, 0.5));

    // Configure material properties
    material.backFaceCulling = false; // Transparent blocks visible from both sides

    // Store material for automatic updates
    this.windMaterials.push(material);

    // Update time uniform every frame
    let totalTime = 0;
    const scene = this.scene; // Capture scene reference for closure
    scene.onBeforeRenderObservable.add(() => {
      totalTime += scene.getEngine().getDeltaTime() / 1000.0;
      material.setFloat('time', totalTime);
    });

    // If EnvironmentService is already connected, update this material immediately
    if (this.environmentService) {
      const params = this.environmentService.getWindParameters();
      this.updateSingleWindMaterial(material, params);
    }

    return material;
  }

  /**
   * Update all wind materials with new wind parameters
   */
  private updateWindMaterials(params: WindParameters): void {
    for (const material of this.windMaterials) {
      this.updateSingleWindMaterial(material, params);
    }
  }

  /**
   * Update a single wind material with wind parameters
   */
  private updateSingleWindMaterial(material: ShaderMaterial, params: WindParameters): void {
    material.setVector2('windDirection', new Vector2(params.windDirection.x, params.windDirection.z));
    material.setFloat('windStrength', params.windStrength);
    material.setFloat('windGustStrength', params.windGustStrength);
    material.setFloat('windSwayFactor', params.windSwayFactor);
  }

  // ============================================
  // Flipbox Shader Implementation
  // ============================================

  /**
   * Register flipbox shader effect
   */
  private registerFlipboxShader(): void {
    // Register flipbox shaders with Babylon.js
    this.registerFlipboxShaderCode();

    // Register flipbox effect
    const flipboxEffect: ShaderEffect = {
      name: 'flipbox',
      createMaterial: (params?: Record<string, any>) => {
        return this.createFlipboxMaterial(params?.texture, params?.shaderParameters, params?.name);
      },
    };

    this.registerEffect(flipboxEffect);
  }

  /**
   * Register custom flipbox shader code with Babylon.js
   */
  private registerFlipboxShaderCode(): void {
    // Vertex shader - standard pass-through with UV
    Effect.ShadersStore['flipboxVertexShader'] = `
      precision highp float;

      // Attributes
      attribute vec3 position;
      attribute vec3 normal;
      attribute vec2 uv;

      // Uniforms
      uniform mat4 worldViewProjection;
      uniform mat4 world;

      // Varyings to fragment shader
      varying vec2 vUV;
      varying vec3 vNormal;

      void main(void) {
        gl_Position = worldViewProjection * vec4(position, 1.0);
        vUV = uv;
        vNormal = normalize((world * vec4(normal, 0.0)).xyz);
      }
    `;

    // Fragment shader - sprite-sheet frame animation
    Effect.ShadersStore['flipboxFragmentShader'] = `
      precision highp float;

      // Varyings from vertex shader
      varying vec2 vUV;
      varying vec3 vNormal;

      // Uniforms
      uniform sampler2D textureSampler;
      uniform vec3 lightDirection;
      uniform int currentFrame;
      uniform int frameCount;

      void main(void) {
        // Calculate frame offset in UV space
        float frameWidth = 1.0 / float(frameCount);
        float frameOffset = float(currentFrame) * frameWidth;

        // Adjust UV to sample current frame
        vec2 frameUV = vec2(vUV.x * frameWidth + frameOffset, vUV.y);

        // Sample texture
        vec4 texColor = texture2D(textureSampler, frameUV);

        // Alpha test: discard transparent pixels
        if (texColor.a < 0.5) {
          discard;
        }

        // Simple directional lighting
        float lightIntensity = max(dot(vNormal, lightDirection), 0.3);
        vec3 finalColor = texColor.rgb * lightIntensity;

        // Output final color
        gl_FragColor = vec4(finalColor, 1.0);
      }
    `;

    logger.debug('Flipbox shader code registered with Babylon.js');
  }

  /**
   * Create flipbox shader material
   *
   * @param texture Original texture (not atlas) with sprite-sheet frames
   * @param shaderParameters Format: "frameCount,delayMs" (e.g., "4,100")
   * @param name Material name
   */
  private createFlipboxMaterial(
    texture: Texture | undefined,
    shaderParameters: string | undefined,
    name: string = 'flipboxMaterial'
  ): ShaderMaterial | null {
    if (!this.scene) {
      logger.error('Cannot create flipbox material: Scene not initialized');
      return null;
    }

    if (!shaderParameters) {
      logger.error('Cannot create flipbox material: shaderParameters required (format: "frameCount,delayMs")');
      return null;
    }

    // Parse shader parameters: "frameCount,delayMs"
    const parts = shaderParameters.split(',');
    if (parts.length !== 2) {
      logger.error('Invalid flipbox shaderParameters format (expected "frameCount,delayMs")', { shaderParameters });
      return null;
    }

    const frameCount = parseInt(parts[0], 10);
    const delayMs = parseInt(parts[1], 10);

    if (isNaN(frameCount) || isNaN(delayMs) || frameCount < 1 || delayMs < 1) {
      logger.error('Invalid flipbox shaderParameters values', { frameCount, delayMs });
      return null;
    }

    logger.debug('Creating flipbox material', { name, frameCount, delayMs });

    const material = new ShaderMaterial(
      name,
      this.scene,
      {
        vertex: 'flipbox',
        fragment: 'flipbox',
      },
      {
        attributes: ['position', 'normal', 'uv'],
        uniforms: [
          'worldViewProjection',
          'world',
          'currentFrame',
          'frameCount',
          'textureSampler',
          'lightDirection',
        ],
        samplers: ['textureSampler'],
      }
    );

    // Error handling for shader compilation
    material.onError = (effect, errors) => {
      logger.error('Flipbox shader compilation error', { name, errors });
    };

    material.onCompiled = () => {
      logger.debug('Flipbox shader compiled successfully', { name });
    };

    // Set texture if provided
    if (texture) {
      material.setTexture('textureSampler', texture);
    }

    // Set frame count
    material.setInt('frameCount', frameCount);

    // Set light direction (from above-front)
    material.setVector3('lightDirection', new Vector3(0.5, 1.0, 0.5));

    // Configure material properties
    material.backFaceCulling = true; // Default backface culling for flipbox

    // Setup frame animation
    let currentFrame = 0;
    let lastFrameTime = Date.now();
    const scene = this.scene; // Capture scene reference for closure

    scene.onBeforeRenderObservable.add(() => {
      const now = Date.now();
      if (now - lastFrameTime >= delayMs) {
        currentFrame = (currentFrame + 1) % frameCount;
        material.setInt('currentFrame', currentFrame);
        lastFrameTime = now;
      }
    });

    return material;
  }
}
