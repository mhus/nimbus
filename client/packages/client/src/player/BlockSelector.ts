/**
 * Block Selector
 * Detects and highlights blocks in front of camera using raycasting
 */
import { Scene, FreeCamera, Vector3, Ray, MeshBuilder, StandardMaterial, Color3, Mesh } from '@babylonjs/core';
import type { ChunkManager } from '../world/ChunkManager';
import type { ClientRegistry } from '../registry/ClientRegistry';

export interface SelectedBlock {
  blockX: number;
  blockY: number;
  blockZ: number;
  blockId: number;
  distance: number;
}

/**
 * Block Selector - Finds and highlights blocks in front of camera
 */
export class BlockSelector {
  private scene: Scene;
  private camera: FreeCamera;
  private chunkManager: ChunkManager;
  private registry: ClientRegistry;

  private enabled = false;
  private maxDistance = 5;
  private selectedBlock: SelectedBlock | null = null;
  private lastLoggedBlock: string | null = null; // For logging only when selection changes
  private highlightMesh?: Mesh;
  private editHighlightMesh?: Mesh; // Red highlight for edit mode
  private editModeBlock: SelectedBlock | null = null; // Block being edited
  private selectForNewMode = false; // Select mode for placing new blocks
  private selectForNewHighlightMesh?: Mesh; // Green highlight for select-for-new mode

  constructor(scene: Scene, camera: FreeCamera, chunkManager: ChunkManager, registry: ClientRegistry) {
    this.scene = scene;
    this.camera = camera;
    this.chunkManager = chunkManager;
    this.registry = registry;

    this.createHighlightMeshes();

    console.log('[BlockSelector] Initialized');
  }

  /**
   * Create wireframe boxes for highlighting selected blocks
   */
  private createHighlightMeshes(): void {
    // Create normal selection highlight (white)
    this.highlightMesh = MeshBuilder.CreateBox('blockHighlight', {
      size: 1.01, // Slightly larger than block to prevent z-fighting
    }, this.scene);

    const material = new StandardMaterial('blockHighlightMaterial', this.scene);
    material.emissiveColor = new Color3(1, 1, 1); // White
    material.wireframe = true;
    material.disableLighting = true;

    this.highlightMesh.material = material;
    this.highlightMesh.isPickable = false;
    this.highlightMesh.isVisible = false;

    // Create edit mode highlight (red)
    this.editHighlightMesh = MeshBuilder.CreateBox('editBlockHighlight', {
      size: 1.02, // Slightly larger than normal highlight
    }, this.scene);

    const editMaterial = new StandardMaterial('editBlockHighlightMaterial', this.scene);
    editMaterial.emissiveColor = new Color3(1, 0, 0); // Red
    editMaterial.wireframe = true;
    editMaterial.disableLighting = true;

    this.editHighlightMesh.material = editMaterial;
    this.editHighlightMesh.isPickable = false;
    this.editHighlightMesh.isVisible = false;

    // Create select-for-new highlight (green)
    this.selectForNewHighlightMesh = MeshBuilder.CreateBox('selectForNewHighlight', {
      size: 1.01,
    }, this.scene);

    const selectForNewMaterial = new StandardMaterial('selectForNewHighlightMaterial', this.scene);
    selectForNewMaterial.emissiveColor = new Color3(0, 1, 0); // Green
    selectForNewMaterial.wireframe = true;
    selectForNewMaterial.disableLighting = true;

    this.selectForNewHighlightMesh.material = selectForNewMaterial;
    this.selectForNewHighlightMesh.isPickable = false;
    this.selectForNewHighlightMesh.isVisible = false;
  }

  /**
   * Enable block selection
   */
  enable(): void {
    if (this.enabled) return;
    this.enabled = true;
    console.log('[BlockSelector] Enabled');
  }

  /**
   * Disable block selection
   */
  disable(): void {
    if (!this.enabled) return;
    this.enabled = false;
    this.selectedBlock = null;
    if (this.highlightMesh) {
      this.highlightMesh.isVisible = false;
    }
    console.log('[BlockSelector] Disabled');
  }

  /**
   * Toggle block selection
   */
  toggle(): void {
    if (this.enabled) {
      this.disable();
    } else {
      this.enable();
    }
  }

  /**
   * Check if block selection is enabled
   */
  isEnabled(): boolean {
    return this.enabled;
  }

  /**
   * Enable "select for new" mode - only select Air blocks
   */
  enableSelectForNew(): void {
    this.selectForNewMode = true;
    this.enable();
    console.log('[BlockSelector] Select-for-new mode enabled');
  }

  /**
   * Disable "select for new" mode - return to normal selection
   */
  disableSelectForNew(): void {
    this.selectForNewMode = false;
    if (this.selectForNewHighlightMesh) {
      this.selectForNewHighlightMesh.isVisible = false;
    }
    console.log('[BlockSelector] Select-for-new mode disabled');
  }

  /**
   * Check if "select for new" mode is active
   */
  isSelectForNewMode(): boolean {
    return this.selectForNewMode;
  }

  /**
   * Get currently selected block
   */
  getSelectedBlock(): SelectedBlock | null {
    return this.selectedBlock;
  }

  /**
   * Set block being edited (shows red highlight)
   */
  setEditModeBlock(block: SelectedBlock | null): void {
    this.editModeBlock = block;

    if (this.editHighlightMesh) {
      if (block) {
        this.editHighlightMesh.position.set(
          block.blockX + 0.5,
          block.blockY + 0.5,
          block.blockZ + 0.5
        );
        this.editHighlightMesh.isVisible = true;
        console.log(`[BlockSelector] Edit mode highlight at (${block.blockX}, ${block.blockY}, ${block.blockZ})`);
      } else {
        this.editHighlightMesh.isVisible = false;
        console.log('[BlockSelector] Edit mode highlight hidden');
      }
    }
  }

  /**
   * Get block being edited
   */
  getEditModeBlock(): SelectedBlock | null {
    return this.editModeBlock;
  }

  /**
   * Update block selection (call every frame)
   */
  update(): void {
    if (!this.enabled) {
      return;
    }

    // Perform raycast from camera
    const ray = this.camera.getForwardRay(this.maxDistance);
    const hit = this.raycastBlocks(ray);

    if (hit) {
      this.selectedBlock = hit;

      // Check if this is the edit block
      const isEditBlock = this.editModeBlock &&
        hit.blockX === this.editModeBlock.blockX &&
        hit.blockY === this.editModeBlock.blockY &&
        hit.blockZ === this.editModeBlock.blockZ;

      // Update highlight based on mode
      if (this.selectForNewMode) {
        // Select-for-new mode: show green (valid) or red (invalid) highlight
        const isAir = hit.blockId === 0;

        if (isAir) {
          // Air block - show green highlight (valid position)
          if (this.selectForNewHighlightMesh) {
            this.selectForNewHighlightMesh.position.set(
              hit.blockX + 0.5,
              hit.blockY + 0.5,
              hit.blockZ + 0.5
            );
            this.selectForNewHighlightMesh.isVisible = true;
          }
          // Hide edit highlight (red)
          if (this.editHighlightMesh && !this.editModeBlock) {
            this.editHighlightMesh.isVisible = false;
          }
        } else {
          // Occupied block - show red highlight (invalid position)
          if (this.editHighlightMesh) {
            this.editHighlightMesh.position.set(
              hit.blockX + 0.5,
              hit.blockY + 0.5,
              hit.blockZ + 0.5
            );
            // Only show red if not currently editing a block
            if (!this.editModeBlock) {
              this.editHighlightMesh.isVisible = true;
            }
          }
          // Hide green highlight
          if (this.selectForNewHighlightMesh) {
            this.selectForNewHighlightMesh.isVisible = false;
          }
        }
        // Hide normal highlight
        if (this.highlightMesh) {
          this.highlightMesh.isVisible = false;
        }
      } else {
        // Normal mode: show white highlight (unless it's the edit block)
        if (this.highlightMesh) {
          if (isEditBlock) {
            this.highlightMesh.isVisible = false;
          } else {
            this.highlightMesh.position.set(
              hit.blockX + 0.5,
              hit.blockY + 0.5,
              hit.blockZ + 0.5
            );
            this.highlightMesh.isVisible = true;
          }
        }
        // Hide select-for-new highlight
        if (this.selectForNewHighlightMesh) {
          this.selectForNewHighlightMesh.isVisible = false;
        }
      }
    } else {
      this.selectedBlock = null;
      if (this.highlightMesh) {
        this.highlightMesh.isVisible = false;
      }
      if (this.selectForNewHighlightMesh) {
        this.selectForNewHighlightMesh.isVisible = false;
      }
    }
  }

  /**
   * Raycast through blocks to find first solid block or Air block (depending on mode)
   */
  private raycastBlocks(ray: Ray): SelectedBlock | null {
    const origin = ray.origin;
    const direction = ray.direction;
    const maxDistance = ray.length;

    // Special handling for "select for new" mode: find free Air block at distance 3, 2, or 1
    if (this.selectForNewMode) {
      let targetBlockX: number = 0;
      let targetBlockY: number = 0;
      let targetBlockZ: number = 0;
      let targetBlockId: number = 0;
      let foundAir = false;

      // Search from far to near: 3 -> 2 -> 1
      for (let dist = 3.0; dist >= 1.0; dist -= 1.0) {
        const testPoint = origin.add(direction.scale(dist));
        const testX = Math.floor(testPoint.x);
        const testY = Math.floor(testPoint.y);
        const testZ = Math.floor(testPoint.z);
        const testId = this.getBlockAt(testX, testY, testZ);

        // Use this position (Air or occupied)
        targetBlockX = testX;
        targetBlockY = testY;
        targetBlockZ = testZ;
        targetBlockId = testId;

        // If Air found, use this position
        if (testId === 0) {
          foundAir = true;
          break;
        }
      }

      const blockKey = `${targetBlockX},${targetBlockY},${targetBlockZ}`;

      // Only log if selection changed
      if (this.lastLoggedBlock !== blockKey) {
        const isAir = targetBlockId === 0;
        console.log(`[BlockSelector] Select-for-new at (${targetBlockX}, ${targetBlockY}, ${targetBlockZ}) - ${isAir ? 'Air (valid)' : 'Occupied (invalid)'}`);
        this.lastLoggedBlock = blockKey;
      }

      return {
        blockX: targetBlockX,
        blockY: targetBlockY,
        blockZ: targetBlockZ,
        blockId: targetBlockId,
        distance: foundAir ? Math.sqrt(
          Math.pow(targetBlockX - origin.x, 2) +
          Math.pow(targetBlockY - origin.y, 2) +
          Math.pow(targetBlockZ - origin.z, 2)
        ) : 1.0,
      };
    }

    // Normal mode: find first solid block
    const stepSize = 0.1;
    let distance = 0;

    while (distance < maxDistance) {
      const point = origin.add(direction.scale(distance));

      // Get block at this point
      const blockX = Math.floor(point.x);
      const blockY = Math.floor(point.y);
      const blockZ = Math.floor(point.z);

      // Check if block is solid
      const blockId = this.getBlockAt(blockX, blockY, blockZ);

      if (blockId !== 0) {
        // Found a solid block
        const blockKey = `${blockX},${blockY},${blockZ}`;

        // Only log if selection changed
        if (this.lastLoggedBlock !== blockKey) {
          const blockType = this.registry.getBlockByID(blockId);
          const blockName = blockType ? blockType.name : 'Unknown';
          console.log(`[BlockSelector] Selected block at (${blockX}, ${blockY}, ${blockZ}) - ID: ${blockId}, Name: ${blockName}`);
          this.lastLoggedBlock = blockKey;
        }

        return {
          blockX,
          blockY,
          blockZ,
          blockId,
          distance,
        };
      }

      distance += stepSize;
    }

    return null;
  }

  /**
   * Get block ID at world coordinates
   */
  private getBlockAt(blockX: number, blockY: number, blockZ: number): number {
    // Get chunk coordinates
    const chunkX = Math.floor(blockX / 32);
    const chunkZ = Math.floor(blockZ / 32);

    // Get local block coordinates within chunk
    const localX = blockX - chunkX * 32;
    const localZ = blockZ - chunkZ * 32;

    // Get chunk data
    const chunkKey = `${chunkX},${chunkZ}`;
    const chunk = (this.chunkManager as any).chunks.get(chunkKey);

    if (!chunk) {
      return 0; // Chunk not loaded
    }

    // Check if block coordinates are valid
    if (localX < 0 || localX >= 32 || localZ < 0 || localZ >= 32 || blockY < 0 || blockY >= 256) {
      return 0;
    }

    // Get block ID
    const index = localX + blockY * 32 + localZ * 32 * 256;
    return chunk.data[index] || 0;
  }

  /**
   * Dispose selector
   */
  dispose(): void {
    if (this.highlightMesh) {
      this.highlightMesh.dispose();
    }
    if (this.editHighlightMesh) {
      this.editHighlightMesh.dispose();
    }
    if (this.selectForNewHighlightMesh) {
      this.selectForNewHighlightMesh.dispose();
    }
  }
}
