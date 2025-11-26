import { getLogger } from '@nimbus/shared';
import {
  Scene,
  TransformNode,
  Mesh,
  PlaneBuilder,
  StandardMaterial,
  Color3,
  RawTexture,
  Constants,
  Texture,
} from '@babylonjs/core';
import type { AppContext } from '../AppContext';
import type { CameraService } from './CameraService';
import type { NetworkService } from './NetworkService';
import { RENDERING_GROUPS } from '../config/renderingGroups';

const logger = getLogger('SunService');

/**
 * SunService - Manages sun visualization using a simple billboard
 *
 * Creates a single billboard plane that always faces the camera.
 * Supports custom texture from WorldInfo or fallback circular disc.
 */
export class SunService {
  private scene: Scene;
  private appContext: AppContext;
  private cameraService: CameraService;
  private networkService?: NetworkService;

  // Sun components
  private sunRoot?: TransformNode;
  private sunMesh?: Mesh;
  private sunMaterial?: StandardMaterial;
  private sunTexture?: Texture | RawTexture;

  // Sun position parameters
  private currentAngleY: number = 90; // Default: East
  private currentElevation: number = 45; // Default: 45° above horizon
  private orbitRadius: number = 400; // Distance from camera

  // Sun appearance
  private sunColor: Color3 = new Color3(1, 1, 0.9); // Warm white/yellow
  private sunSize: number = 80; // Billboard size
  private enabled: boolean = true;

  constructor(scene: Scene, appContext: AppContext) {
    this.scene = scene;
    this.appContext = appContext;
    this.cameraService = appContext.services.camera!;
    this.networkService = appContext.services.network;

    this.initialize();
  }

  private async initialize(): Promise<void> {
    // Create sun root node attached to camera environment root
    const cameraRoot = this.cameraService.getCameraEnvironmentRoot();
    if (!cameraRoot) {
      logger.error('Camera environment root not available');
      return;
    }

    this.sunRoot = new TransformNode('sunRoot', this.scene);
    this.sunRoot.parent = cameraRoot;

    // Load texture (from WorldInfo or fallback to procedural)
    await this.loadSunTexture();

    // Create material
    this.sunMaterial = new StandardMaterial('sunMaterial', this.scene);
    this.sunMaterial.diffuseTexture = this.sunTexture!;
    this.sunMaterial.emissiveTexture = this.sunTexture!;
    this.sunMaterial.emissiveColor = this.sunColor;
    this.sunMaterial.disableLighting = true;
    this.sunMaterial.opacityTexture = this.sunTexture!;
    this.sunMaterial.useAlphaFromDiffuseTexture = false;
    this.sunMaterial.backFaceCulling = false;

    // Create sun billboard mesh
    this.sunMesh = PlaneBuilder.CreatePlane('sun', { size: this.sunSize }, this.scene);
    this.sunMesh.parent = this.sunRoot;
    this.sunMesh.material = this.sunMaterial;
    this.sunMesh.billboardMode = Mesh.BILLBOARDMODE_ALL;
    this.sunMesh.renderingGroupId = RENDERING_GROUPS.ENVIRONMENT;

    // Set initial position
    this.updateSunPosition();

    logger.info('SunService initialized', {
      angleY: this.currentAngleY,
      elevation: this.currentElevation,
      hasCustomTexture: this.appContext.worldInfo?.settings.sunTexture !== undefined,
    });
  }

  /**
   * Load sun texture from WorldInfo or use default
   */
  private async loadSunTexture(): Promise<void> {
    const texturePath = this.appContext.worldInfo?.settings.sunTexture || 'textures/sun/sun1.png';

    if (this.networkService) {
      try {
        // Load texture from asset server
        const textureUrl = this.networkService.getAssetUrl(texturePath);
        this.sunTexture = new Texture(
          textureUrl,
          this.scene,
          false, // noMipmap
          true, // invertY
          Constants.TEXTURE_TRILINEAR_SAMPLINGMODE,
          () => {
            logger.info('Sun texture loaded', { path: texturePath });
          },
          (message) => {
            logger.error('Failed to load sun texture, using fallback', { path: texturePath, error: message });
            this.sunTexture = this.createFallbackTexture();
            if (this.sunMaterial) {
              this.sunMaterial.diffuseTexture = this.sunTexture;
              this.sunMaterial.emissiveTexture = this.sunTexture;
              this.sunMaterial.opacityTexture = this.sunTexture;
            }
          }
        );
        this.sunTexture.hasAlpha = true;
      } catch (error) {
        logger.error('Error loading sun texture, using fallback', { error });
        this.sunTexture = this.createFallbackTexture();
      }
    } else {
      // Use fallback circular disc if network service not available
      this.sunTexture = this.createFallbackTexture();
      logger.info('Using fallback circular sun texture (no NetworkService)');
    }
  }

  /**
   * Create simple circular disc texture as fallback
   */
  private createFallbackTexture(): RawTexture {
    const size = 256;
    const center = size / 2;
    const radius = size / 2 - 10;
    const textureData = new Uint8Array(size * size * 4);

    for (let y = 0; y < size; y++) {
      for (let x = 0; x < size; x++) {
        const dx = x - center;
        const dy = y - center;
        const dist = Math.sqrt(dx * dx + dy * dy);

        let alpha = 0;

        if (dist < radius) {
          // Smooth edge falloff
          const edgeDist = radius - dist;
          if (edgeDist < 10) {
            alpha = edgeDist / 10; // Soft edge
          } else {
            alpha = 1.0; // Full opacity
          }
        }

        const idx = (y * size + x) * 4;
        textureData[idx] = 255; // R
        textureData[idx + 1] = 255; // G
        textureData[idx + 2] = 255; // B
        textureData[idx + 3] = Math.floor(alpha * 255); // A
      }
    }

    return RawTexture.CreateRGBATexture(
      textureData,
      size,
      size,
      this.scene,
      false,
      false,
      Constants.TEXTURE_BILINEAR_SAMPLINGMODE
    );
  }

  /**
   * Set sun texture from asset path
   * @param texturePath Path to texture (will be loaded via NetworkService.getAssetUrl)
   */
  async setSunTexture(texturePath: string | null): Promise<void> {
    if (!texturePath) {
      // Use fallback
      this.sunTexture?.dispose();
      this.sunTexture = this.createFallbackTexture();
      if (this.sunMaterial) {
        this.sunMaterial.diffuseTexture = this.sunTexture;
        this.sunMaterial.emissiveTexture = this.sunTexture;
        this.sunMaterial.opacityTexture = this.sunTexture;
      }
      logger.info('Sun texture reset to fallback');
      return;
    }

    if (!this.networkService) {
      logger.error('NetworkService not available, cannot load texture');
      return;
    }

    try {
      const textureUrl = this.networkService.getAssetUrl(texturePath);

      // Dispose old texture
      this.sunTexture?.dispose();

      // Load new texture
      this.sunTexture = new Texture(
        textureUrl,
        this.scene,
        false,
        true,
        Constants.TEXTURE_TRILINEAR_SAMPLINGMODE,
        () => {
          logger.info('Sun texture loaded', { path: texturePath });
          if (this.sunMaterial) {
            this.sunMaterial.diffuseTexture = this.sunTexture!;
            this.sunMaterial.emissiveTexture = this.sunTexture!;
            this.sunMaterial.opacityTexture = this.sunTexture!;
          }
        },
        (message) => {
          logger.error('Failed to load sun texture', { path: texturePath, error: message });
          this.sunTexture = this.createFallbackTexture();
          if (this.sunMaterial) {
            this.sunMaterial.diffuseTexture = this.sunTexture;
            this.sunMaterial.emissiveTexture = this.sunTexture;
            this.sunMaterial.opacityTexture = this.sunTexture;
          }
        }
      );
      this.sunTexture.hasAlpha = true;
    } catch (error) {
      logger.error('Error loading sun texture', { error });
    }
  }

  /**
   * Set sun position on circular orbit around camera using Y-axis angle
   * @param angleY Horizontal angle in degrees (0=North, 90=East, 180=South, 270=West)
   */
  setSunPositionOnCircle(angleY: number): void {
    this.currentAngleY = angleY;
    this.updateSunPosition();
  }

  /**
   * Set sun height (elevation) over camera
   * @param elevation Vertical angle in degrees (-90=down, 0=horizon, 90=up)
   */
  setSunHeightOverCamera(elevation: number): void {
    this.currentElevation = elevation;
    this.updateSunPosition();
  }

  /**
   * Update sun root position based on current angleY and elevation
   */
  private updateSunPosition(): void {
    if (!this.sunRoot) return;

    // Convert to radians
    const angleYRad = this.currentAngleY * (Math.PI / 180);
    const elevationRad = this.currentElevation * (Math.PI / 180);

    // Calculate position on sphere (relative to camera)
    const y = this.orbitRadius * Math.sin(elevationRad);
    const horizontalDist = this.orbitRadius * Math.cos(elevationRad);
    const x = horizontalDist * Math.sin(angleYRad);
    const z = horizontalDist * Math.cos(angleYRad);

    this.sunRoot.position.set(x, y, z);

    logger.info('Sun position updated', {
      angleY: this.currentAngleY,
      elevation: this.currentElevation,
      position: { x, y, z },
    });
  }

  /**
   * Set sun color
   * @param r Red component (0-1)
   * @param g Green component (0-1)
   * @param b Blue component (0-1)
   */
  setSunColor(r: number, g: number, b: number): void {
    this.sunColor = new Color3(r, g, b);

    if (this.sunMaterial) {
      this.sunMaterial.emissiveColor = this.sunColor;
    }

    logger.info('Sun color updated', { r, g, b });
  }

  /**
   * Set sun size
   * @param size Billboard plane size
   */
  setSunSize(size: number): void {
    this.sunSize = size;

    if (this.sunMesh) {
      this.sunMesh.scaling.setAll(size / 80); // Scale from default 80
    }

    logger.info('Sun size updated', { size });
  }

  /**
   * Enable/disable sun visibility
   * @param enabled True to show sun, false to hide
   */
  setEnabled(enabled: boolean): void {
    this.enabled = enabled;

    if (this.sunMesh) {
      this.sunMesh.setEnabled(enabled);
    }

    logger.info('Sun visibility changed', { enabled });
  }

  /**
   * Get current sun position
   */
  getSunPosition(): { angleY: number; elevation: number } {
    return {
      angleY: this.currentAngleY,
      elevation: this.currentElevation,
    };
  }

  /**
   * Cleanup and dispose resources
   */
  dispose(): void {
    this.sunMesh?.dispose();
    this.sunMaterial?.dispose();
    this.sunTexture?.dispose();
    this.sunRoot?.dispose();

    logger.info('SunService disposed');
  }
}
