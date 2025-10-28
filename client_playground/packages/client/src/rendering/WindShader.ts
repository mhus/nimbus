/**
 * Wind Shader
 * Applies wind displacement to blocks based on wind parameters
 */
import { ShaderMaterial, Scene, Effect, Texture, Vector2 } from '@babylonjs/core';

/**
 * Wind Shader Manager
 * Creates and manages shader materials for wind-affected blocks
 */
export class WindShader {
  private scene: Scene;
  private materials: ShaderMaterial[] = [];
  private windManager: any | null = null; // WindManager reference for automatic updates

  constructor(scene: Scene) {
    this.scene = scene;
    this.registerShaders();
  }

  /**
   * Set WindManager for automatic parameter updates
   */
  setWindManager(windManager: any): void {
    this.windManager = windManager;
    console.log('[WindShader] WindManager connected for automatic updates');
    console.log('[WindShader] Current materials count:', this.materials.length);

    // Immediately update materials with current wind parameters
    if (this.materials.length > 0) {
      const params = windManager.getParameters();
      console.log('[WindShader] Initial wind parameters:', params);
      this.updateWindParameters(
        params.windDirection,
        params.windStrength,
        params.windGustStrength,
        params.windSwayFactor
      );
    }

    // Setup automatic updates every frame
    let frameCount = 0;
    this.scene.onBeforeRenderObservable.add(() => {
      if (this.windManager && this.materials.length > 0) {
        frameCount++;
        // Log every 60 frames (roughly once per second at 60 FPS)
        if (frameCount % 60 === 0) {
          const params = this.windManager.getParameters();
          console.log('[WindShader] Frame update - materials:', this.materials.length, 'params:', params);
        }
        const params = this.windManager.getParameters();
        this.updateWindParameters(
          params.windDirection,
          params.windStrength,
          params.windGustStrength,
          params.windSwayFactor
        );
      }
    });
  }

  /**
   * Register custom wind shaders with Babylon.js
   */
  private registerShaders(): void {
    // Vertex shader with lighting and simple wind animation
    Effect.ShadersStore['windVertexShader'] = `
      precision highp float;

      // Attributes
      attribute vec3 position;
      attribute vec3 normal;
      attribute vec2 uv;
      attribute vec4 color;

      // Wind properties
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

      // Rodrigues rotation formula
      vec3 rotateAroundAxis(vec3 p, vec3 ax, float a) {
        ax = normalize(ax);
        float c = cos(a);
        float s = sin(a);
        return p * c + cross(ax, p) * s + ax * dot(ax, p) * (1.0 - c);
      }

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

        // SHEARING WITH VERTICAL COMPRESSION (physikalisch korrekt für Baumbewegung):
        // 1. Horizontale Biegung (shearing): oben bewegt sich stärker zur Seite
        // 2. Vertikale Kompression: bei starker horizontaler Auslenkung wird Block nach unten gedrückt

        // Shear direction (normalized wind direction in XZ plane)
        vec3 shearDir = vec3(windDirection.x, 0.0, windDirection.y);
        if (length(shearDir) > 0.01) {
          shearDir = normalize(shearDir);
        }

        // Pivot Y: base of the lever (windLeverDown blocks below current block)
        float pivotY = -windLeverDown;

        // Block height (always 1.0 in block space)
        float blockHeight = 1.0;

        // Normalized height within block (0.0 at bottom, 1.0 at top)
        float h = clamp((position.y - pivotY) / (pivotY + blockHeight + windLeverUp), 0.0, 1.0);

        // Total height from pivot to top of this vertex
        float heightFromPivot = position.y - pivotY;

        // Calculate lever length for this vertex (interpolate between Down and Up)
        float leverAtThisHeight = mix(windLeverDown, windLeverUp, h);

        // Horizontal displacement at this height
        float horizontalDisp = totalWave * leverAtThisHeight * stabilityFactor * 0.05;

        // Apply horizontal shearing
        pos += shearDir * horizontalDisp * h;

        // VERTICAL COMPRESSION (Physik: wenn Baum zur Seite gebogen, wird er kürzer)
        // Für einen flexiblen Stab: wenn horizontal um X verschoben, vertikal um ~X²/(2*L) reduziert
        // Dies approximiert: newHeight = sqrt(L² - X²)
        float horizontalDispTotal = abs(horizontalDisp * h);
        float compressionFactor = (horizontalDispTotal * horizontalDispTotal) / (2.0 * max(heightFromPivot, 0.1));

        // Reduziere Y-Position proportional zur horizontalen Auslenkung
        pos.y -= compressionFactor;

        // VERTICAL LEAFINESS MOVEMENT (zusätzliche Auf/Ab-Bewegung für Blätter)
        // Separate Welle mit etwas anderer Frequenz für organischere Bewegung
        float verticalLeafWave = sin(time * windSwayFactor * 2.1 + worldPos.x * 0.015 + worldPos.z * 0.015) * leafiness;
        verticalLeafWave += cos(time * windSwayFactor * 1.3 + worldPos.z * 0.02) * leafiness * 0.5;

        // Nur obere Teile bewegen sich vertikal (h-Faktor), Amplitude 0.02
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
        // Sample texture from atlas
        vec4 texColor = texture2D(textureSampler, vUV);

        // Alpha test: discard transparent pixels (alpha < 0.5)
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
  }

  /**
   * Create wind material with texture
   */
  createWindMaterial(texture: Texture | undefined, name: string = 'windMaterial'): ShaderMaterial {
    console.log('[WindShader] Creating wind material:', name);
    console.log('[WindShader] Texture provided:', texture ? 'yes' : 'no');

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

    // Add error handling for shader compilation
    material.onError = (effect, errors) => {
      console.error(`[WindShader] Shader compilation error for ${name}:`);
      console.error('Errors:', errors);
      console.error('Effect:', effect);
    };

    // Log successful compilation
    material.onCompiled = (effect) => {
      console.log(`[WindShader] Shader compiled successfully for ${name}`);
    };

    // Set texture if provided
    if (texture) {
      material.setTexture('textureSampler', texture);
      console.log(`[WindShader] Set texture for ${name}:`, texture.name);
    } else {
      console.warn(`[WindShader] No texture provided for ${name} - material will appear gray!`);
    }

    // Set default wind parameters (will be updated from WindManager)
    // Use proper Vector2 object for setVector2()
    material.setVector2('windDirection', new Vector2(1.0, 0.0));
    material.setFloat('windStrength', 0.8);
    material.setFloat('windGustStrength', 0.4);
    material.setFloat('windSwayFactor', 2.0);

    // Set light direction (from above-front)
    material.setVector3('lightDirection', 0.5, 1.0, 0.5);

    // Configure material properties
    // Disable back face culling so transparent blocks are visible from both sides
    material.backFaceCulling = false;

    // Store material reference for later updates
    this.materials.push(material);
    console.log('[WindShader] Material added to array, total materials:', this.materials.length);

    // Update time uniform every frame
    let totalTime = 0;
    let frameCount = 0;
    this.scene.onBeforeRenderObservable.add(() => {
      totalTime += this.scene.getEngine().getDeltaTime() / 1000.0;
      material.setFloat('time', totalTime);

      frameCount++;
      // Log every 120 frames (roughly every 2 seconds at 60 FPS)
      if (frameCount % 120 === 0) {
        console.log('[WindShader] Time update for', name, '- time:', totalTime.toFixed(2));
      }
    });

    console.log('[WindShader] Created wind material:', name);

    // If WindManager is already connected, update this material immediately
    if (this.windManager) {
      const params = this.windManager.getParameters();
      console.log('[WindShader] Immediately updating new material with wind params:', params);
      material.setVector2('windDirection', new Vector2(params.windDirection.x, params.windDirection.z));
      material.setFloat('windStrength', params.windStrength);
      material.setFloat('windGustStrength', params.windGustStrength);
      material.setFloat('windSwayFactor', params.windSwayFactor);
    }

    return material;
  }

  /**
   * Update all wind materials with new wind parameters
   */
  updateWindParameters(
    windDirection: { x: number; z: number },
    windStrength: number,
    windGustStrength: number,
    windSwayFactor: number
  ): void {
    for (const material of this.materials) {
      material.setVector2('windDirection', new Vector2(windDirection.x, windDirection.z));
      material.setFloat('windStrength', windStrength);
      material.setFloat('windGustStrength', windGustStrength);
      material.setFloat('windSwayFactor', windSwayFactor);
    }
  }

  /**
   * Get all wind materials
   */
  getMaterials(): ShaderMaterial[] {
    return this.materials;
  }
}
