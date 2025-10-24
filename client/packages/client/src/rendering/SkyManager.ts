/**
 * Sky Manager
 * Manages skybox rendering using Babylon.js CubeTexture
 *
 * Usage:
 * const skyManager = new SkyManager(scene);
 * skyManager.createSkybox('textures/skybox'); // Loads skybox_px.jpg, skybox_nx.jpg, etc.
 */

import { Scene, MeshBuilder, StandardMaterial, Color3, CubeTexture, Texture } from '@babylonjs/core';

export class SkyManager {
  private scene: Scene;
  private skybox: any = null;

  constructor(scene: Scene) {
    this.scene = scene;
  }

  /**
   * Create a skybox using a cube texture
   * @param textureUrl Base URL for skybox textures (without _px.jpg suffix)
   * @param size Size of the skybox cube (default: 1000)
   */
  createSkybox(textureUrl: string, size: number = 1000.0): void {
    // Remove existing skybox if present
    if (this.skybox) {
      this.skybox.dispose();
      this.skybox = null;
    }

    // Create skybox mesh
    const skybox = MeshBuilder.CreateBox('skyBox', { size }, this.scene);

    // Create skybox material
    const skyboxMaterial = new StandardMaterial('skyBox', this.scene);
    skyboxMaterial.backFaceCulling = false;

    // Load cube texture (expects _px.jpg, _nx.jpg, _py.jpg, _ny.jpg, _pz.jpg, _nz.jpg)
    skyboxMaterial.reflectionTexture = new CubeTexture(textureUrl, this.scene);
    skyboxMaterial.reflectionTexture.coordinatesMode = Texture.SKYBOX_MODE;

    // Remove diffuse and specular lighting
    skyboxMaterial.diffuseColor = new Color3(0, 0, 0);
    skyboxMaterial.specularColor = new Color3(0, 0, 0);

    // Apply material to skybox
    skybox.material = skyboxMaterial;

    // Store reference
    this.skybox = skybox;

    console.log(`[SkyManager] Created skybox with texture: ${textureUrl}`);
  }

  /**
   * Dispose the skybox and free resources
   */
  dispose(): void {
    if (this.skybox) {
      this.skybox.dispose();
      this.skybox = null;
      console.log('[SkyManager] Skybox disposed');
    }
  }

  /**
   * Get the current skybox mesh
   */
  getSkybox(): any {
    return this.skybox;
  }
}
