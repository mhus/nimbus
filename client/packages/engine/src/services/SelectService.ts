/**
 * SelectService - Manages block selection in front of the player
 *
 * Provides raycasting functionality to find blocks in the player's line of sight.
 * Supports different selection modes: INTERACTIVE, BLOCK, AIR, ALL, NONE.
 * Includes auto-select mode with visual highlighting.
 */

import { Vector3, Mesh, MeshBuilder, StandardMaterial, Color3, Scene } from '@babylonjs/core';
import { AdvancedDynamicTexture, TextBlock, Control } from '@babylonjs/gui';
import { getLogger, ExceptionHandler } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { ChunkService } from './ChunkService';
import type { PlayerService } from './PlayerService';
import type { Block } from '@nimbus/shared';
import type { ClientBlock } from '../types/ClientBlock';
import { mergeBlockModifier } from '../utils/BlockModifierMerge';

const logger = getLogger('SelectService');

/**
 * Minimum distance from camera to start selecting blocks (in block units)
 * Prevents selecting blocks too close to the camera/player
 */
const MIN_SELECT_DISTANCE = 2.0;

/**
 * Selection modes
 */
export enum SelectMode {
  /** No selection */
  NONE = 'NONE',

  /** Only interactive blocks (block.metadata.interactive OR currentModifier.physics.interactive) */
  INTERACTIVE = 'INTERACTIVE',

  /** Any solid block */
  BLOCK = 'BLOCK',

  /** Only AIR blocks (empty spaces) */
  AIR = 'AIR',

  /** Any block or AIR if no block found */
  ALL = 'ALL',
}


/**
 * SelectService - Manages block selection
 *
 * Features:
 * - Raycasting from player position in view direction
 * - Multiple selection modes
 * - Distance-based selection (radius)
 * - AIR block creation for empty spaces
 * - Auto-select mode with visual highlighting
 */
export class SelectService {
  private appContext: AppContext;
  private chunkService: ChunkService;
  private playerService: PlayerService;
  private scene?: Scene;

  // Auto-select mode
  private _autoSelectMode: SelectMode = SelectMode.NONE;
  private autoSelectRadius: number = 5.0;
  private currentSelectedBlock: ClientBlock | null = null;

  // Highlight rendering
  private highlightMesh?: Mesh;
  private highlightMaterial?: StandardMaterial;

  // Label rendering
  private guiTexture?: AdvancedDynamicTexture;
  private labelTextBlock?: TextBlock;

  // Edit block selection (green highlight)
  private selectedEditBlock: Vector3 | null = null;
  private editHighlightMesh?: Mesh;
  private editHighlightMaterial?: StandardMaterial;

  // Cached player eye height for raycast origin (updated via event)
  private playerEyeHeight: number = 1.6; // Default value

  constructor(
    appContext: AppContext,
    chunkService: ChunkService,
    playerService: PlayerService,
    scene?: Scene
  ) {
    this.appContext = appContext;
    this.chunkService = chunkService;
    this.playerService = playerService;
    this.scene = scene;

    // Initialize player eye height from PlayerInfo
    if (appContext.playerInfo) {
      this.playerEyeHeight = appContext.playerInfo.eyeHeight;
    }

    // Subscribe to PlayerInfo updates
    playerService.on('playerInfo:updated', (info: import('@nimbus/shared').PlayerInfo) => {
      // Update cached eye height for raycast
      this.playerEyeHeight = info.eyeHeight;
      logger.debug('SelectService: eyeHeight updated', {
        eyeHeight: this.playerEyeHeight,
      });
    });

    // Initialize highlighting if scene is available
    if (scene) {
      this.initializeHighlight();
    }

    logger.info('SelectService initialized', {
      eyeHeight: this.playerEyeHeight,
    });
  }

  /**
   * Get selected block based on mode, position, rotation, and radius
   *
   * @param mode Selection mode
   * @param position Player position (world coordinates)
   * @param rotation Camera rotation (pitch, yaw, roll in radians)
   * @param radius Maximum search distance
   * @returns Selected ClientBlock or null
   */
  getSelectedBlock(
    mode: SelectMode,
    position: Vector3,
    rotation: Vector3,
    radius: number
  ): ClientBlock | null {
    try {
      // NONE mode returns immediately
      if (mode === SelectMode.NONE) {
        return null;
      }

      // Calculate ray direction from rotation
      const direction = this.calculateRayDirection(rotation);

      // Perform raycasting
      return this.raycast(mode, position, direction, radius);
    } catch (error) {
      ExceptionHandler.handle(error, 'SelectService.getSelectedBlock', {
        mode,
        position,
        rotation,
        radius,
      });
      return null;
    }
  }

  /**
   * Calculate ray direction from camera rotation
   *
   * @param rotation Camera rotation (pitch, yaw, roll)
   * @returns Normalized direction vector
   */
  private calculateRayDirection(rotation: Vector3): Vector3 {
    // rotation.x = pitch (up/down)
    // rotation.y = yaw (left/right)
    // rotation.z = roll (usually 0)

    const pitch = rotation.x;
    const yaw = rotation.y;

    // Convert rotation to direction vector
    const direction = new Vector3(
      Math.sin(yaw) * Math.cos(pitch),  // x
      -Math.sin(pitch),                  // y (negative because Babylon.js Y is up)
      Math.cos(yaw) * Math.cos(pitch)   // z
    );

    return direction.normalize();
  }

  /**
   * Perform raycasting using DDA algorithm (Digital Differential Analyzer)
   *
   * @param mode Selection mode
   * @param origin Ray origin (player position)
   * @param direction Ray direction (normalized)
   * @param maxDistance Maximum ray distance
   * @returns ClientBlock or null
   */
  private raycast(
    mode: SelectMode,
    origin: Vector3,
    direction: Vector3,
    maxDistance: number
  ): ClientBlock | null {
    // Step size for ray marching (smaller = more accurate, slower)
    const stepSize = 0.1;

    // Calculate step increments
    const stepX = direction.x * stepSize;
    const stepY = direction.y * stepSize;
    const stepZ = direction.z * stepSize;

    // Start ray at minimum distance to avoid selecting blocks too close to camera
    const startDistance = MIN_SELECT_DISTANCE;

    // Current position along the ray (start at minimum distance)
    let currentX = origin.x + direction.x * startDistance;
    let currentY = origin.y + direction.y * startDistance;
    let currentZ = origin.z + direction.z * startDistance;

    // Track distance traveled (starting from MIN_SELECT_DISTANCE)
    let distance = startDistance;

    // Variables to track last AIR position (for ALL mode fallback)
    let lastAirBlock: { x: number; y: number; z: number } | null = null;

    // March along the ray
    while (distance <= maxDistance) {
      // Get current block coordinates (floor to get block position)
      const blockX = Math.floor(currentX);
      const blockY = Math.floor(currentY);
      const blockZ = Math.floor(currentZ);

      // Query block at this position
      const clientBlock = this.chunkService.getBlockAt(blockX, blockY, blockZ);

      if (clientBlock && clientBlock.block) {
        // Found a solid block
        const block = clientBlock.block;

        // Check if block matches the selection mode
        if (this.matchesMode(mode, clientBlock, false)) {
          return clientBlock;
        }
      } else {
        // Empty space (AIR)
        lastAirBlock = { x: blockX, y: blockY, z: blockZ };

        // For AIR mode, return immediately
        if (mode === SelectMode.AIR) {
          return this.createAirClientBlock(blockX, blockY, blockZ);
        }
      }

      // Step forward along the ray
      currentX += stepX;
      currentY += stepY;
      currentZ += stepZ;
      distance += stepSize;
    }

    // No block found within radius
    // For ALL mode, return last AIR position if we found one
    if (mode === SelectMode.ALL && lastAirBlock) {
      return this.createAirClientBlock(
        lastAirBlock.x,
        lastAirBlock.y,
        lastAirBlock.z
      );
    }

    return null;
  }

  /**
   * Check if a block matches the selection mode
   *
   * @param mode Selection mode
   * @param clientBlock ClientBlock to check (contains merged modifier)
   * @param isAir Whether this is an AIR block
   * @returns True if block matches mode
   */
  private matchesMode(mode: SelectMode, clientBlock: ClientBlock, isAir: boolean): boolean {
    switch (mode) {
      case SelectMode.NONE:
        return false;

      case SelectMode.INTERACTIVE:
        // Check currentModifier.physics.interactive (from BlockType or Block modifier)
        if (isAir) return false;
        return clientBlock.currentModifier?.physics?.interactive === true;

      case SelectMode.BLOCK:
        // Any solid block
        return !isAir;

      case SelectMode.AIR:
        // Only AIR blocks
        return isAir;

      case SelectMode.ALL:
        // Any block or AIR
        return true;

      default:
        return false;
    }
  }

  /**
   * Create an AIR ClientBlock at the given position
   *
   * @param x World X coordinate
   * @param y World Y coordinate
   * @param z World Z coordinate
   * @returns New AIR ClientBlock with BlockTypeId 0
   */
  private createAirClientBlock(x: number, y: number, z: number): ClientBlock {
    // Get AIR blockType from registry (id 0)
    const blockTypeService = this.appContext.services.blockType;
    let airBlockType = blockTypeService?.getBlockType(0);

    // Fallback: Create minimal AIR BlockType if not found
    if (!airBlockType) {
      airBlockType = {
        id: 0,
        initialStatus: 0,
        modifiers: {
          0: {
            visibility: {
              shape: 0, // INVISIBLE
            },
            physics: {
              solid: false,
            },
          },
        },
      };
    }

    // Get chunk coordinates
    const chunkSize = this.appContext.worldInfo?.chunkSize || 16;
    const cx = Math.floor(x / chunkSize);
    const cz = Math.floor(z / chunkSize);

    // Create Block instance
    const block: Block = {
      position: { x, y, z },
      blockTypeId: 0, // AIR block type
    };

    // Get merged modifier using utility function
    const currentModifier = mergeBlockModifier(this.appContext, block, airBlockType);

    // Create ClientBlock
    const clientBlock: ClientBlock = {
      block,
      chunk: { cx, cz },
      blockType: airBlockType,
      currentModifier,
      clientBlockType: airBlockType as any, // Cast to ClientBlockType
      isVisible: true,
      isDirty: false,
      lastUpdate: Date.now(),
    };

    return clientBlock;
  }

  /**
   * Get selected block using current player position and rotation
   *
   * FIX: Raycast now starts from eye level (player.position + eyeHeight)
   * instead of from feet (player.position). This gives correct block selection
   * matching where the player is actually looking.
   *
   * @param mode Selection mode
   * @param radius Maximum search distance
   * @returns ClientBlock or null
   */
  getSelectedBlockFromPlayer(
    mode: SelectMode,
    radius: number = 5.0
  ): ClientBlock | null {
    try {
      // Get player feet position
      const feetPosition = this.playerService.getPosition();

      // FIX: Start raycast from eye level, not feet!
      const eyePosition = feetPosition.clone();
      eyePosition.y += this.playerEyeHeight; // Add cached eye height

      // Get camera rotation from CameraService via reflection
      // (PlayerService doesn't expose rotation directly)
      const cameraService = (this.playerService as any).cameraService;
      if (!cameraService) {
        logger.warn('CameraService not available');
        return null;
      }

      const rotation = cameraService.getRotation();

      // Use eye position for raycast (not feet!)
      return this.getSelectedBlock(mode, eyePosition, rotation, radius);
    } catch (error) {
      ExceptionHandler.handle(error, 'SelectService.getSelectedBlockFromPlayer', {
        mode,
        radius,
      });
      return null;
    }
  }

  /**
   * Initialize highlight mesh for selected blocks
   */
  private initializeHighlight(): void {
    if (!this.scene) {
      logger.warn('Cannot initialize highlight: scene not available');
      return;
    }

    try {
      // Create wireframe box for highlighting (yellow)
      this.highlightMesh = MeshBuilder.CreateBox(
        'blockHighlight',
        { size: 1.0 },
        this.scene
      );

      // Create highlight material (yellow for selected block)
      this.highlightMaterial = new StandardMaterial('highlightMaterial', this.scene);
      this.highlightMaterial.emissiveColor = new Color3(1, 1, 0); // Yellow
      this.highlightMaterial.wireframe = true;
      this.highlightMaterial.alpha = 0.8;

      this.highlightMesh.material = this.highlightMaterial;
      this.highlightMesh.isPickable = false;
      this.highlightMesh.renderingGroupId = 1; // Render on top
      this.highlightMesh.setEnabled(false); // Hidden by default

      // Create wireframe box for edit block highlighting (green)
      this.editHighlightMesh = MeshBuilder.CreateBox(
        'editBlockHighlight',
        { size: 1.0 },
        this.scene
      );

      // Create edit highlight material (green for edit block)
      this.editHighlightMaterial = new StandardMaterial('editHighlightMaterial', this.scene);
      this.editHighlightMaterial.emissiveColor = new Color3(1, 0, 0); // Red
      this.editHighlightMaterial.wireframe = true;
      this.editHighlightMaterial.alpha = 0.8;

      this.editHighlightMesh.material = this.editHighlightMaterial;
      this.editHighlightMesh.isPickable = false;
      this.editHighlightMesh.renderingGroupId = 1; // Render on top
      this.editHighlightMesh.setEnabled(false); // Hidden by default

      // Initialize BabylonJS GUI for label display
      this.guiTexture = AdvancedDynamicTexture.CreateFullscreenUI('UI', true, this.scene);

      // Create text block for item/block name display
      this.labelTextBlock = new TextBlock('blockLabel');
      this.labelTextBlock.text = '';
      this.labelTextBlock.color = 'white';
      this.labelTextBlock.fontSize = 16;
      this.labelTextBlock.fontWeight = 'bold';
      this.labelTextBlock.outlineWidth = 2;
      this.labelTextBlock.outlineColor = 'black';
      this.labelTextBlock.isVisible = false;
      this.labelTextBlock.verticalAlignment = Control.VERTICAL_ALIGNMENT_TOP;
      this.labelTextBlock.textVerticalAlignment = Control.VERTICAL_ALIGNMENT_TOP;

      this.guiTexture.addControl(this.labelTextBlock);

      logger.debug('Highlight meshes and label GUI initialized');
    } catch (error) {
      ExceptionHandler.handle(error, 'SelectService.initializeHighlight');
    }
  }

  /**
   * Get current auto-select mode
   */
  get autoSelectMode(): SelectMode {
    return this._autoSelectMode;
  }

  /**
   * Get current auto-select mode (method for compatibility)
   */
  getAutoSelectMode(): SelectMode {
    return this._autoSelectMode;
  }

  /**
   * Set auto-select mode
   *
   * @param mode Selection mode for auto-select
   */
  set autoSelectMode(mode: SelectMode) {
    this._autoSelectMode = mode;

    // Hide highlight when mode is NONE
    if (mode === SelectMode.NONE) {
      this.hideHighlight();
      this.currentSelectedBlock = null;
    }

    logger.debug('Auto-select mode set', { mode });
  }

  /**
   * Get auto-select radius
   */
  getAutoSelectRadius(): number {
    return this.autoSelectRadius;
  }

  /**
   * Set auto-select radius
   *
   * @param radius Maximum search distance for auto-select
   */
  setAutoSelectRadius(radius: number): void {
    this.autoSelectRadius = Math.max(1.0, Math.min(radius, 20.0)); // Clamp between 1 and 20
  }

  /**
   * Get currently selected block (from auto-select)
   */
  getCurrentSelectedBlock(): ClientBlock | null {
    return this.currentSelectedBlock;
  }

  /**
   * Update auto-select (called each frame)
   *
   * @param deltaTime Time since last frame in seconds
   */
  update(deltaTime: number): void {
    // Skip if auto-select is disabled
    if (this._autoSelectMode === SelectMode.NONE) {
      return;
    }

    try {
      // Get selected block using auto-select mode
      const selectedBlock = this.getSelectedBlockFromPlayer(
        this._autoSelectMode,
        this.autoSelectRadius
      );

      // Update current selection
      this.currentSelectedBlock = selectedBlock;

      // Update highlight
      if (selectedBlock) {
        this.showHighlight(selectedBlock);
      } else {
        this.hideHighlight();
      }

      // Update edit block highlight
      if (this.selectedEditBlock) {
        this.showEditHighlight(this.selectedEditBlock);
      } else {
        this.hideEditHighlight();
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'SelectService.update');
    }
  }

  /**
   * Show highlight at block position
   *
   * @param clientBlock Block to highlight
   */
  private showHighlight(clientBlock: ClientBlock): void {
    if (!this.highlightMesh || !this.scene) {
      return;
    }

    const pos = clientBlock.block.position;

    // Position highlight at block center
    this.highlightMesh.position.set(
      pos.x + 0.5,
      pos.y + 0.5,
      pos.z + 0.5
    );

    // Scale slightly larger than block for better visibility
    const scale = 1.02;
    this.highlightMesh.scaling.set(scale, scale, scale);

    // Enable highlight
    this.highlightMesh.setEnabled(true);

    // Show label if block has displayName
    this.showLabel(clientBlock);
  }

  /**
   * Hide highlight
   */
  private hideHighlight(): void {
    if (this.highlightMesh) {
      this.highlightMesh.setEnabled(false);
    }
    this.hideLabel();
  }

  /**
   * Show label for selected block
   *
   * Only shows label if block has displayName metadata.
   * Label is positioned above the block in screen space.
   *
   * @param clientBlock Block to show label for
   */
  private showLabel(clientBlock: ClientBlock): void {
    if (!this.labelTextBlock || !this.scene || !this.highlightMesh) {
      return;
    }

    // Get display name from metadata
    const displayName = clientBlock.block.metadata?.displayName;
    if (!displayName) {
      // No displayName - hide label
      this.hideLabel();
      return;
    }

    // Set label text
    this.labelTextBlock.text = displayName;

    // Link label to block position (above the block)
    const blockCenter = this.highlightMesh.position.clone();
    blockCenter.y += 0.7; // Position above block

    // Link label to 3D position (will follow block in screen space)
    this.labelTextBlock.linkWithMesh(this.highlightMesh);
    this.labelTextBlock.linkOffsetY = -60; // Offset above block in pixels

    // Show label
    this.labelTextBlock.isVisible = true;

    logger.debug('Label shown', {
      displayName,
      position: clientBlock.block.position,
    });
  }

  /**
   * Hide label
   */
  private hideLabel(): void {
    if (this.labelTextBlock) {
      this.labelTextBlock.isVisible = false;
      this.labelTextBlock.linkWithMesh(null); // Unlink from mesh
    }
  }

  /**
   * Set highlight color
   *
   * @param color Color as Color3 or hex string
   */
  setHighlightColor(color: Color3 | string): void {
    if (!this.highlightMaterial) {
      return;
    }

    if (typeof color === 'string') {
      // Parse hex color
      this.highlightMaterial.emissiveColor = Color3.FromHexString(color);
    } else {
      this.highlightMaterial.emissiveColor = color;
    }
  }

  /**
   * Set selected edit block (green highlight)
   *
   * @param x World X coordinate (optional, null to clear)
   * @param y World Y coordinate (optional)
   * @param z World Z coordinate (optional)
   */
  setSelectedEditBlock(x?: number, y?: number, z?: number): void {
    try {
      // Clear selection if no parameters provided
      if (x === undefined || y === undefined || z === undefined) {
        this.selectedEditBlock = null;
        this.hideEditHighlight();
        logger.debug('Edit block selection cleared');
        return;
      }

      // Set new selection
      this.selectedEditBlock = new Vector3(x, y, z);
      logger.debug('Edit block selected', { x, y, z });
    } catch (error) {
      ExceptionHandler.handle(error, 'SelectService.setSelectedEditBlock', { x, y, z });
    }
  }

  /**
   * Get selected edit block position
   *
   * @returns Vector3 with block coordinates or null
   */
  getSelectedEditBlock(): { x: number; y: number; z: number } | null {
    if (!this.selectedEditBlock) {
      return null;
    }

    return {
      x: this.selectedEditBlock.x,
      y: this.selectedEditBlock.y,
      z: this.selectedEditBlock.z,
    };
  }

  /**
   * Show edit highlight at block position (green)
   *
   * @param position Block position as Vector3
   */
  private showEditHighlight(position: Vector3): void {
    if (!this.editHighlightMesh || !this.scene) {
      return;
    }

    // Position highlight at block center
    this.editHighlightMesh.position.set(
      position.x + 0.5,
      position.y + 0.5,
      position.z + 0.5
    );

    // Scale slightly larger than block for better visibility
    const scale = 1.02;
    this.editHighlightMesh.scaling.set(scale, scale, scale);

    // Enable highlight
    this.editHighlightMesh.setEnabled(true);
  }

  /**
   * Hide edit highlight
   */
  private hideEditHighlight(): void {
    if (this.editHighlightMesh) {
      this.editHighlightMesh.setEnabled(false);
    }
  }

  /**
   * Dispose service
   */
  dispose(): void {
    // Dispose highlight resources
    this.highlightMesh?.dispose();
    this.highlightMaterial?.dispose();
    this.editHighlightMesh?.dispose();
    this.editHighlightMaterial?.dispose();

    this.currentSelectedBlock = null;
    this.selectedEditBlock = null;

    logger.info('SelectService disposed');
  }
}
