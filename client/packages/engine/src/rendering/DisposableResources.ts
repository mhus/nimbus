/**
 * DisposableResources - Manages disposable resources for chunk lifecycle
 *
 * Tracks all disposable resources (meshes, sprites, etc.) created during chunk rendering.
 * When the chunk is unloaded, all resources are automatically disposed.
 *
 * This provides clean lifecycle management and prevents memory leaks.
 */

import type { Mesh, Sprite } from '@babylonjs/core';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('DisposableResources');

/**
 * DisposableResources - Tracks and disposes rendering resources
 *
 * Features:
 * - Tracks meshes, sprites, and other disposable objects
 * - Automatic disposal when chunk is unloaded
 * - Extensible for future resource types
 */
export class DisposableResources {
  private meshes: Mesh[] = [];
  private sprites: Sprite[] = [];

  /**
   * Add a mesh to be disposed later
   *
   * @param mesh Mesh to track
   */
  addMesh(mesh: Mesh): void {
    this.meshes.push(mesh);
  }

  /**
   * Add a sprite to be disposed later
   *
   * @param sprite Sprite to track
   */
  addSprite(sprite: Sprite): void {
    this.sprites.push(sprite);
  }

  /**
   * Get statistics about tracked resources
   */
  getStats(): { meshes: number; sprites: number } {
    return {
      meshes: this.meshes.length,
      sprites: this.sprites.length,
    };
  }

  /**
   * Dispose all tracked resources
   *
   * Disposes all meshes and sprites, then clears the tracking arrays.
   */
  dispose(): void {
    // Dispose meshes
    for (const mesh of this.meshes) {
      try {
        mesh.dispose();
      } catch (error) {
        logger.warn('Failed to dispose mesh', { name: mesh.name, error });
      }
    }

    // Dispose sprites
    for (const sprite of this.sprites) {
      try {
        sprite.dispose();
      } catch (error) {
        logger.warn('Failed to dispose sprite', { error });
      }
    }

    const stats = this.getStats();
    logger.debug('Resources disposed', stats);

    // Clear arrays
    this.meshes = [];
    this.sprites = [];
  }
}
