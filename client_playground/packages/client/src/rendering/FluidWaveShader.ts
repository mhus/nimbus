/**
 * Fluid Wave Shader
 * Manages wave effects for water and lava using custom shaders
 */
import { ShaderMaterial, Scene, Effect, Color3 } from '@babylonjs/core';

export interface WaveParameters {
  speed: number;        // Wave animation speed
  amplitude: number;    // Wave height
  frequency: number;    // Wave frequency
  directionX: number;   // Wave direction X component
  directionZ: number;   // Wave direction Z component
}

/**
 * Default wave parameters for water
 */
const DEFAULT_WATER_WAVES: WaveParameters = {
  speed: 1.0,
  amplitude: 0.05,
  frequency: 2.0,
  directionX: 1.0,
  directionZ: 0.5,
};

/**
 * Default wave parameters for lava
 */
const DEFAULT_LAVA_WAVES: WaveParameters = {
  speed: 0.5,
  amplitude: 0.15,
  frequency: 1.5,
  directionX: 0.7,
  directionZ: 0.7,
};

/**
 * Manages wave shaders for fluids
 */
export class FluidWaveShader {
  private scene: Scene;
  private waterParams: WaveParameters;
  private lavaParams: WaveParameters;
  private waterMaterials: ShaderMaterial[] = [];
  private lavaMaterials: ShaderMaterial[] = [];

  constructor(scene: Scene) {
    this.scene = scene;
    this.waterParams = { ...DEFAULT_WATER_WAVES };
    this.lavaParams = { ...DEFAULT_LAVA_WAVES };

    this.registerShaders();
  }

  /**
   * Register custom shaders with Babylon.js
   */
  private registerShaders(): void {
    // Vertex shader with configurable amplitude, frequency, and speed
    Effect.ShadersStore['fluidWaveVertexShader'] = `
      precision highp float;
      attribute vec3 position;
      uniform mat4 worldViewProjection;
      uniform float time;
      uniform float waveAmplitude;
      uniform float waveFrequency;
      uniform float waveSpeed;

      varying vec3 vPosition;

      void main(void) {
        vec3 pos = position;

        // Wave with configurable amplitude, frequency, and speed
        float wave = sin(pos.x * waveFrequency + time * waveSpeed) * cos(pos.z * waveFrequency + time * waveSpeed) * waveAmplitude;
        pos.y += wave;

        gl_Position = worldViewProjection * vec4(pos, 1.0);
        vPosition = position;
      }
    `;

    // Fragment shader for water - semi-transparent blue with moving dark spots
    Effect.ShadersStore['waterWaveFragmentShader'] = `
      precision highp float;

      uniform float time;
      varying vec3 vPosition;

      void main(void) {
        // Base water color
        vec3 waterColor = vec3(0.2, 0.5, 0.8);

        // Create moving dark spots using sine waves
        vec2 flowPos = vPosition.xz + vec2(time * 0.3, time * 0.2);
        float pattern = sin(flowPos.x * 3.0) * sin(flowPos.y * 3.0);
        pattern = pattern * 0.5 + 0.5; // Normalize to 0-1

        // Darken the water where pattern is low
        float darkness = mix(0.7, 1.0, pattern);
        waterColor *= darkness;

        gl_FragColor = vec4(waterColor, 0.7);
      }
    `;

    // Fragment shader for lava - semi-transparent orange/red with moving dark spots
    Effect.ShadersStore['lavaWaveFragmentShader'] = `
      precision highp float;

      uniform float time;
      varying vec3 vPosition;

      void main(void) {
        // Base lava color
        vec3 lavaColor = vec3(1.0, 0.3, 0.0);

        // Create moving dark spots using sine waves (slower than water)
        vec2 flowPos = vPosition.xz + vec2(time * 0.15, time * 0.1);
        float pattern = sin(flowPos.x * 2.5) * sin(flowPos.y * 2.5);
        pattern = pattern * 0.5 + 0.5; // Normalize to 0-1

        // Darken the lava where pattern is low
        float darkness = mix(0.6, 1.0, pattern);
        lavaColor *= darkness;

        gl_FragColor = vec4(lavaColor, 0.9);
      }
    `;

  }

  /**
   * Create wave material for water
   */
  createWaterMaterial(texture?: any): ShaderMaterial {
    const material = new ShaderMaterial(
      'waterWaveMaterial',
      this.scene,
      {
        vertex: 'fluidWave',
        fragment: 'waterWave',
      },
      {
        attributes: ['position'],
        uniforms: ['worldViewProjection', 'time', 'waveAmplitude', 'waveFrequency', 'waveSpeed'],
        varyings: ['vPosition'],
      }
    );

    // Configure transparency
    material.backFaceCulling = false;
    material.transparencyMode = ShaderMaterial.MATERIAL_ALPHABLEND;
    material.alphaMode = 2; // ALPHA_COMBINE

    // Set initial parameters
    material.setFloat('waveAmplitude', this.waterParams.amplitude);
    material.setFloat('waveFrequency', this.waterParams.frequency);
    material.setFloat('waveSpeed', this.waterParams.speed);

    // Store material reference for later updates
    this.waterMaterials.push(material);

    // Update time uniform every frame
    let totalTime = 0;
    this.scene.onBeforeRenderObservable.add(() => {
      totalTime += this.scene.getEngine().getDeltaTime() / 1000.0;
      material.setFloat('time', totalTime);
    });

    return material;
  }

  /**
   * Create wave material for lava
   */
  createLavaMaterial(texture?: any): ShaderMaterial {
    const material = new ShaderMaterial(
      'lavaWaveMaterial',
      this.scene,
      {
        vertex: 'fluidWave',
        fragment: 'lavaWave',
      },
      {
        attributes: ['position'],
        uniforms: ['worldViewProjection', 'time', 'waveAmplitude', 'waveFrequency', 'waveSpeed'],
        varyings: ['vPosition'],
      }
    );

    // Configure transparency
    material.backFaceCulling = false;
    material.transparencyMode = ShaderMaterial.MATERIAL_ALPHABLEND;
    material.alphaMode = 2; // ALPHA_COMBINE

    // Set initial parameters
    material.setFloat('waveAmplitude', this.lavaParams.amplitude);
    material.setFloat('waveFrequency', this.lavaParams.frequency);
    material.setFloat('waveSpeed', this.lavaParams.speed);

    // Store material reference for later updates
    this.lavaMaterials.push(material);

    // Update time uniform every frame
    let totalTime = 0;
    this.scene.onBeforeRenderObservable.add(() => {
      totalTime += this.scene.getEngine().getDeltaTime() / 1000.0;
      material.setFloat('time', totalTime);
    });

    return material;
  }

  /**
   * Update water material with current parameters
   */
  private updateWaterMaterial(material: ShaderMaterial): void {
    material.setFloat('waveSpeed', this.waterParams.speed);
    material.setFloat('waveAmplitude', this.waterParams.amplitude);
    material.setFloat('waveFrequency', this.waterParams.frequency);
    material.setVector2('waveDirection', this.waterParams.directionX, this.waterParams.directionZ);
  }

  /**
   * Update lava material with current parameters
   */
  private updateLavaMaterial(material: ShaderMaterial): void {
    material.setFloat('waveSpeed', this.lavaParams.speed);
    material.setFloat('waveAmplitude', this.lavaParams.amplitude);
    material.setFloat('waveFrequency', this.lavaParams.frequency);
    material.setVector2('waveDirection', this.lavaParams.directionX, this.lavaParams.directionZ);
  }

  /**
   * Set water wave parameters
   */
  setWaterWaves(params: Partial<WaveParameters>): void {
    this.waterParams = { ...this.waterParams, ...params };

    // Update all existing water materials
    for (const material of this.waterMaterials) {
      if (params.amplitude !== undefined) {
        material.setFloat('waveAmplitude', this.waterParams.amplitude);
      }
      if (params.frequency !== undefined) {
        material.setFloat('waveFrequency', this.waterParams.frequency);
      }
      if (params.speed !== undefined) {
        material.setFloat('waveSpeed', this.waterParams.speed);
      }
    }
  }

  /**
   * Set lava wave parameters
   */
  setLavaWaves(params: Partial<WaveParameters>): void {
    this.lavaParams = { ...this.lavaParams, ...params };

    // Update all existing lava materials
    for (const material of this.lavaMaterials) {
      if (params.amplitude !== undefined) {
        material.setFloat('waveAmplitude', this.lavaParams.amplitude);
      }
      if (params.frequency !== undefined) {
        material.setFloat('waveFrequency', this.lavaParams.frequency);
      }
      if (params.speed !== undefined) {
        material.setFloat('waveSpeed', this.lavaParams.speed);
      }
    }
  }

  /**
   * Get current water wave parameters
   */
  getWaterWaves(): WaveParameters {
    return { ...this.waterParams };
  }

  /**
   * Get current lava wave parameters
   */
  getLavaWaves(): WaveParameters {
    return { ...this.lavaParams };
  }

  /**
   * Reset water waves to default
   */
  resetWaterWaves(): void {
    this.waterParams = { ...DEFAULT_WATER_WAVES };
    // Update all materials with default values
    for (const material of this.waterMaterials) {
      material.setFloat('waveAmplitude', this.waterParams.amplitude);
      material.setFloat('waveFrequency', this.waterParams.frequency);
      material.setFloat('waveSpeed', this.waterParams.speed);
    }
  }

  /**
   * Reset lava waves to default
   */
  resetLavaWaves(): void {
    this.lavaParams = { ...DEFAULT_LAVA_WAVES };
    // Update all materials with default values
    for (const material of this.lavaMaterials) {
      material.setFloat('waveAmplitude', this.lavaParams.amplitude);
      material.setFloat('waveFrequency', this.lavaParams.frequency);
      material.setFloat('waveSpeed', this.lavaParams.speed);
    }
  }
}
