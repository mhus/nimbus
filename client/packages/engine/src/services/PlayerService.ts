/**
 * PlayerService - Manages player state and movement
 *
 * Handles player position, movement, and camera synchronization.
 * Initial implementation provides position/logic only (no rendering).
 */

import { Vector3 } from '@babylonjs/core';
import { getLogger } from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { CameraService } from './CameraService';
import type { PhysicsService, MovementMode } from './PhysicsService';
import type { PlayerEntity } from '../types/PlayerEntity';
import type { ModifierStack, Modifier } from './ModifierService';

const logger = getLogger('PlayerService');

/**
 * Event listener type
 */
type EventListener = (...args: any[]) => void;

/**
 * PlayerService - Manages player state
 *
 * Features:
 * - Player position tracking
 * - Camera synchronization
 * - Movement logic
 * - Event emission for position updates
 *
 * Note: Player rendering (third-person view) is not implemented yet
 */
export class PlayerService {
  private appContext: AppContext;
  private cameraService: CameraService;
  private physicsService?: PhysicsService;
  private entityRenderService?: any; // EntityRenderService

  // Player as physics entity with player info
  private playerEntity: PlayerEntity;

  // Last known position (for change detection)
  private lastPosition: { x: number; y: number; z: number };

  // Event system
  private eventListeners: Map<string, EventListener[]> = new Map();

  // View mode (ego vs third-person)
  private viewModeStack?: ModifierStack<boolean>; // true = ego, false = third-person
  private playerViewModifier?: Modifier<boolean>;
  private underwaterViewModifier?: Modifier<boolean>;

  // Third-person model rendering
  private thirdPersonMesh?: any; // AbstractMesh from Babylon.js
  private thirdPersonAnimations?: any[]; // AnimationGroup[]

  // Character rotation (independent from camera in third-person)
  private characterYaw: number = 0; // Degrees
  private targetCharacterYaw: number = 0; // Target yaw for smooth rotation

  constructor(appContext: AppContext, cameraService: CameraService) {
    this.appContext = appContext;
    this.cameraService = cameraService;

    // Get PlayerInfo from AppContext (must be initialized before PlayerService)
    if (!appContext.playerInfo) {
      throw new Error('PlayerInfo must be initialized in AppContext before creating PlayerService');
    }

    // Initialize dimensions if not present
    if (!appContext.playerInfo.dimensions) {
      appContext.playerInfo.dimensions = {
        walk: { height: 2.0, width: 0.6, footprint: 0.3 },
        sprint: { height: 2.0, width: 0.6, footprint: 0.3 },
        crouch: { height: 1.0, width: 0.6, footprint: 0.3 },
        swim: { height: 1.8, width: 0.6, footprint: 0.3 },
        climb: { height: 1.8, width: 0.6, footprint: 0.3 },
        fly: { height: 1.8, width: 0.6, footprint: 0.3 },
        teleport: { height: 1.8, width: 0.6, footprint: 0.3 },
      };
    }

    // Create player entity (starts in Walk mode)
    this.playerEntity = {
      entityId: 'player',
      position: new Vector3(0, 64, 0),
      velocity: Vector3.Zero(),
      rotation: Vector3.Zero(), // Rotation in radians (x: pitch, y: yaw, z: roll)
      movementMode: 'walk' as MovementMode,
      wishMove: Vector3.Zero(), // Movement intention
      grounded: false, // Is on ground
      onSlope: false, // Is on slope
      inWater: false, // Is in water
      canAutoJump: false, // Can trigger auto-jump
      jumpRequested: false, // Jump requested this frame
      lastBlockPos: new Vector3(0, 64, 0), // Last block position for cache invalidation
      playerInfo: appContext.playerInfo,
      // Initialize cached effective values from PlayerInfo
      effectiveWalkSpeed: appContext.playerInfo.effectiveWalkSpeed,
      effectiveRunSpeed: appContext.playerInfo.effectiveRunSpeed,
      effectiveUnderwaterSpeed: appContext.playerInfo.effectiveUnderwaterSpeed,
      effectiveCrawlSpeed: appContext.playerInfo.effectiveCrawlSpeed,
      effectiveRidingSpeed: appContext.playerInfo.effectiveRidingSpeed,
      effectiveJumpSpeed: appContext.playerInfo.effectiveJumpSpeed,
      effectiveTurnSpeed: appContext.playerInfo.effectiveTurnSpeed,
      effectiveUnderwaterTurnSpeed: appContext.playerInfo.effectiveUnderwaterTurnSpeed,
    };

    // Initialize last position for change detection
    this.lastPosition = {
      x: this.playerEntity.position.x,
      y: this.playerEntity.position.y,
      z: this.playerEntity.position.z,
    };

    // Initialize view mode stack (ego vs third-person)
    // Note: ModifierService might not be available yet during construction
    // Will be initialized lazily on first use
    this.initializeViewModeStack();

    // Initialize player position and sync camera
    this.syncCameraToPlayer();

    // Load third-person model if configured (async, doesn't block initialization)
    if (appContext.playerInfo.thirdPersonModelId) {
      this.loadThirdPersonModel(appContext.playerInfo.thirdPersonModelId).catch(error => {
        logger.error('Failed to load third-person model during init', {}, error as Error);
      });
    }

    logger.info('PlayerService initialized', {
      position: this.playerEntity.position,
      movementMode: this.playerEntity.movementMode,
      displayName: this.playerEntity.playerInfo.displayName,
    });
  }

  /**
   * Set physics service (called after PhysicsService is created)
   */
  setPhysicsService(physicsService: PhysicsService): void {
    this.physicsService = physicsService;
    this.physicsService.registerEntity(this.playerEntity);
    logger.debug('PhysicsService set and player registered');
  }

  /**
   * Set entity render service (called after EntityRenderService is created)
   */
  setEntityRenderService(entityRenderService: any): void {
    this.entityRenderService = entityRenderService;
    logger.debug('EntityRenderService set');
  }

  /**
   * Get player position
   */
  getPosition(): Vector3 {
    return this.playerEntity.position.clone();
  }

  /**
   * Set player position
   *
   * @param x World X coordinate
   * @param y World Y coordinate
   * @param z World Z coordinate
   */
  setPosition(x: number, y: number, z: number): void {
    this.playerEntity.position.set(x, y, z);
    this.syncCameraToPlayer();
    this.emit('position:changed', this.playerEntity.position.clone());
  }

  /**
   * Move player forward/backward relative to camera direction
   *
   * @param distance Distance to move (positive = forward, negative = backward)
   */
  moveForward(distance: number): void {
    if (!this.physicsService) return;

    const cameraRotation = this.cameraService.getRotation();
    this.physicsService.moveForward(
      this.playerEntity,
      distance,
      cameraRotation.y, // yaw
      cameraRotation.x  // pitch
    );

    this.syncCameraToPlayer();
    this.emit('position:changed', this.playerEntity.position.clone());
  }

  /**
   * Move player left/right relative to camera direction
   *
   * @param distance Distance to move (positive = right, negative = left)
   */
  moveRight(distance: number): void {
    if (!this.physicsService) return;

    const cameraRotation = this.cameraService.getRotation();
    this.physicsService.moveRight(
      this.playerEntity,
      distance,
      cameraRotation.y // yaw
    );

    this.syncCameraToPlayer();
    this.emit('position:changed', this.playerEntity.position.clone());
  }

  /**
   * Move player up/down (Fly mode only)
   *
   * @param distance Distance to move (positive = up, negative = down)
   */
  moveUp(distance: number): void {
    if (!this.physicsService) return;

    this.physicsService.moveUp(this.playerEntity, distance);

    this.syncCameraToPlayer();
    this.emit('position:changed', this.playerEntity.position.clone());
  }

  /**
   * Jump (Walk mode only, if on ground)
   */
  jump(): void {
    if (!this.physicsService) return;

    this.physicsService.jump(this.playerEntity);
  }

  /**
   * Rotate camera (controls player look direction)
   *
   * @param deltaPitch Pitch delta in radians
   * @param deltaYaw Yaw delta in radians
   */
  rotate(deltaPitch: number, deltaYaw: number): void {
    this.cameraService.rotate(deltaPitch, deltaYaw);
  }

  /**
   * Update player (physics is handled by PhysicsService)
   *
   * @param deltaTime Time since last frame in seconds
   */
  update(deltaTime: number): void {
    // Physics is now handled by PhysicsService
    // Just sync camera after physics update
    this.syncCameraToPlayer();

    // Update third-person model position/rotation if active
    if (!this.isEgoView() && this.thirdPersonMesh) {
      this.updateThirdPersonModel();
    }

    // Emit position change if moved (for chunk loading, etc.)
    const currentPos = this.playerEntity.position;
    if (
      currentPos.x !== this.lastPosition.x ||
      currentPos.y !== this.lastPosition.y ||
      currentPos.z !== this.lastPosition.z
    ) {
      this.lastPosition.x = currentPos.x;
      this.lastPosition.y = currentPos.y;
      this.lastPosition.z = currentPos.z;

      this.emit('position:changed', currentPos.clone());
    }
  }

  /**
   * Sync camera position to player position
   */
  private syncCameraToPlayer(): void {
    const isEgo = this.isEgoView();

    if (isEgo) {
      // Ego-view: Camera at player eye level
      const eyeHeight = this.playerEntity.playerInfo.eyeHeight;
      this.cameraService.setPosition(
        this.playerEntity.position.x,
        this.playerEntity.position.y + eyeHeight,
        this.playerEntity.position.z
      );
    } else {
      // Third-person: Camera orbits around player (independent rotation)
      this.cameraService.setThirdPersonPosition(
        this.playerEntity.position,
        5.0 // Distance from player
      );
    }
  }

  /**
   * Update third-person model position and rotation (via EntityRenderService)
   */
  private updateThirdPersonModel(): void {
    const playerAvatarEntityId = (this as any).playerAvatarEntityId;
    if (!playerAvatarEntityId || !this.entityRenderService) {
      return; // Not loaded yet
    }

    // Get camera yaw for character rotation
    const cameraYaw = this.cameraService.getCameraYaw();

    // Update entity transform via EntityRenderService
    this.entityRenderService.updateEntityTransform(
      playerAvatarEntityId,
      this.playerEntity.position,
      { y: cameraYaw, p: 0 }
    );

    // TODO: Update animations based on movement mode/velocity
  }

  /**
   * Get PlayerEntity (for advanced access)
   */
  getPlayerEntity(): PlayerEntity {
    return this.playerEntity;
  }

  /**
   * Update player info dynamically (for power-ups, equipment, status effects)
   *
   * Updates PlayerInfo and emits 'playerInfo:updated' event so all services
   * can react to the changes (PhysicsService, CameraService, SelectService, etc.)
   *
   * @param updates Partial PlayerInfo with values to update
   */
  updatePlayerInfo(updates: Partial<import('@nimbus/shared').PlayerInfo>): void {
    // Update PlayerInfo
    Object.assign(this.playerEntity.playerInfo, updates);

    // Update cached effective values on entity
    this.playerEntity.effectiveWalkSpeed = this.playerEntity.playerInfo.effectiveWalkSpeed;
    this.playerEntity.effectiveRunSpeed = this.playerEntity.playerInfo.effectiveRunSpeed;
    this.playerEntity.effectiveUnderwaterSpeed = this.playerEntity.playerInfo.effectiveUnderwaterSpeed;
    this.playerEntity.effectiveCrawlSpeed = this.playerEntity.playerInfo.effectiveCrawlSpeed;
    this.playerEntity.effectiveRidingSpeed = this.playerEntity.playerInfo.effectiveRidingSpeed;
    this.playerEntity.effectiveJumpSpeed = this.playerEntity.playerInfo.effectiveJumpSpeed;
    this.playerEntity.effectiveTurnSpeed = this.playerEntity.playerInfo.effectiveTurnSpeed;
    this.playerEntity.effectiveUnderwaterTurnSpeed = this.playerEntity.playerInfo.effectiveUnderwaterTurnSpeed;

    logger.debug('PlayerInfo updated', { updates });

    // Emit event so all services can react
    this.emit('playerInfo:updated', this.playerEntity.playerInfo);

    // Sync camera in case eyeHeight changed (direct update for immediate feedback)
    this.syncCameraToPlayer();
  }

  /**
   * Get current move speed
   */
  getMoveSpeed(): number {
    if (!this.physicsService) return 5.0;
    return this.physicsService.getMoveSpeed(this.playerEntity);
  }

  /**
   * Check if player is on ground
   */
  isPlayerOnGround(): boolean {
    return this.playerEntity.grounded;
  }

  /**
   * Get current movement mode
   */
  getMovementMode(): MovementMode {
    return this.playerEntity.movementMode;
  }

  /**
   * Set movement mode
   */
  setMovementMode(mode: MovementMode): void {
    if (!this.physicsService) return;
    this.physicsService.setMovementMode(this.playerEntity, mode);
  }

  /**
   * Toggle between Walk and Fly modes
   */
  toggleMovementMode(): void {
    if (!this.physicsService) return;
    this.physicsService.toggleMovementMode(this.playerEntity);
  }

  /**
   * Add event listener
   *
   * @param event Event name
   * @param listener Event listener function
   */
  on(event: string, listener: EventListener): void {
    const listeners = this.eventListeners.get(event) || [];
    listeners.push(listener);
    this.eventListeners.set(event, listeners);
  }

  /**
   * Remove event listener
   *
   * @param event Event name
   * @param listener Event listener function
   */
  off(event: string, listener: EventListener): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      const index = listeners.indexOf(listener);
      if (index !== -1) {
        listeners.splice(index, 1);
      }
    }
  }

  /**
   * Emit event
   *
   * @param event Event name
   * @param args Event arguments
   */
  private emit(event: string, ...args: any[]): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      listeners.forEach((listener) => {
        try {
          listener(...args);
        } catch (error) {
          logger.error('Error in event listener', { event }, error as Error);
        }
      });
    }
  }

  /**
   * Initialize view mode stack (lazy initialization)
   */
  private initializeViewModeStack(): void {
    if (this.viewModeStack) {
      return; // Already initialized
    }

    const modifierService = this.appContext.services.modifier;
    if (!modifierService) {
      return; // Not available yet, will retry later
    }

    // Get or create stack (safe to call multiple times)
    this.viewModeStack = modifierService.getOrCreateModifierStack<boolean>(
      'playerViewMode',
      true, // Default: ego-view (first-person)
      (isEgo) => {
        // Callback when view mode changes
        this.onViewModeChanged(isEgo);
      }
    );

    // Add player's default preference if not already added
    if (!this.playerViewModifier) {
      this.playerViewModifier = this.viewModeStack.addModifier(true, 100); // Priority 100 (low)
    }

    // Create underwater modifier if not already added
    if (!this.underwaterViewModifier) {
      this.underwaterViewModifier = this.viewModeStack.addModifier(true, 10); // Priority 10 (high)
      this.underwaterViewModifier.setEnabled(false); // Disabled initially
    }

    logger.debug('View mode stack initialized', { defaultEgo: true });
  }

  /**
   * Toggle view mode (ego vs third-person)
   * Called by F5 key handler
   */
  toggleViewMode(): void {
    // Ensure view mode stack is initialized
    this.initializeViewModeStack();

    if (!this.viewModeStack || !this.playerViewModifier) {
      logger.warn('View mode stack not initialized');
      return;
    }

    // Toggle player's preference (priority 100)
    const currentEgo = this.playerViewModifier.getValue();
    this.playerViewModifier.setValue(!currentEgo);

    logger.info('Player toggled view mode', {
      from: currentEgo ? 'ego' : 'third-person',
      to: !currentEgo ? 'ego' : 'third-person',
    });
  }

  /**
   * Force ego-view (used for underwater auto-switch)
   *
   * @param underwater True if underwater, false if surfaced
   */
  setUnderwaterViewMode(underwater: boolean): void {
    // Ensure view mode stack is initialized
    this.initializeViewModeStack();

    if (!this.underwaterViewModifier) {
      return;
    }

    // Simply enable/disable the underwater modifier instead of creating/destroying it
    this.underwaterViewModifier.setEnabled(underwater);

    logger.debug('Underwater ego-view modifier', { enabled: underwater });
  }

  /**
   * Get current view mode
   */
  isEgoView(): boolean {
    // Ensure view mode stack is initialized
    this.initializeViewModeStack();

    return this.viewModeStack?.currentValue ?? true;
  }

  /**
   * Called when view mode changes (from modifier stack)
   */
  private onViewModeChanged(isEgo: boolean): void {
    logger.info('View mode changed', { isEgo });

    if (isEgo) {
      // Switch to ego-view (first-person)
      this.hideThirdPersonModel();
    } else {
      // Switch to third-person
      this.showThirdPersonModel();
    }

    // Update camera position
    this.syncCameraToPlayer();
  }

  /**
   * Load and show third-person model
   */
  private async showThirdPersonModel(): Promise<void> {
    const modelId = this.playerEntity.playerInfo.thirdPersonModelId;
    if (!modelId) {
      logger.warn('No third-person model ID configured in PlayerInfo');
      return;
    }

    const playerAvatarEntityId = (this as any).playerAvatarEntityId;

    // If already loaded, just show it via EntityRenderService
    if (playerAvatarEntityId && this.entityRenderService) {
      // Show entity via visibility event
      this.entityRenderService.onEntityVisibility(playerAvatarEntityId, true);
      logger.info('Third-person model shown via EntityRenderService');
      return;
    }

    // Not loaded yet - load it (lazy loading)
    await this.loadThirdPersonModel(modelId);
  }

  /**
   * Hide third-person model
   */
  private hideThirdPersonModel(): void {
    const playerAvatarEntityId = (this as any).playerAvatarEntityId;
    if (!playerAvatarEntityId || !this.entityRenderService) {
      return;
    }

    // Hide entity via visibility event
    this.entityRenderService.onEntityVisibility(playerAvatarEntityId, false);
    logger.debug('Third-person model hidden via EntityRenderService');
  }

  /**
   * Load third-person model from entity model ID
   * Delegates to EntityRenderService for rendering
   */
  private async loadThirdPersonModel(modelId: string): Promise<void> {
    try {
      logger.info('Loading player avatar via EntityRenderService', { modelId });

      // Get entity model
      const entityService = this.appContext.services.entity;
      if (!entityService) {
        logger.error('EntityService not available');
        return;
      }

      const entityModel = await entityService.getEntityModel(modelId);
      if (!entityModel) {
        logger.error('Entity model not found', { modelId });
        return;
      }

      // Check EntityRenderService
      if (!this.entityRenderService) {
        logger.error('EntityRenderService not available');
        return;
      }

      // Create Entity for player avatar
      const { createClientEntity } = await import('@nimbus/shared');
      const playerAvatarEntity: any = {
        id: '@player_avatar', // Special ID for player avatar
        name: this.playerEntity.playerInfo.displayName,
        model: modelId,
        modelModifier: {},
        movementType: 'dynamic' as const,
        solid: false,
        interactive: false,
      };

      // Create ClientEntity
      const clientEntity = createClientEntity(
        playerAvatarEntity,
        entityModel,
        this.playerEntity.position,
        { y: 0, p: 0 },
        0 // IDLE pose
      );

      logger.info('Created ClientEntity for player avatar', {
        entityId: clientEntity.id,
        position: clientEntity.currentPosition,
      });

      // Register in EntityService cache
      (entityService as any).entityCache.set(clientEntity.id, clientEntity);

      // Create pathway to trigger EntityRenderService rendering
      const pathway = {
        entityId: clientEntity.id,
        startAt: Date.now(),
        waypoints: [{
          timestamp: Date.now() + 100,
          target: { ...this.playerEntity.position },
          rotation: { y: 0, p: 0 },
          pose: 0 // IDLE
        }],
      };

      // Trigger EntityRenderService to render the model
      await this.entityRenderService.onEntityPathway(pathway);

      logger.info('Player avatar rendered via EntityRenderService');

      // Store entity ID and ClientEntity for later updates
      (this as any).playerAvatarEntityId = '@player_avatar';
      (this as any).playerAvatarClientEntity = clientEntity;

      logger.info('Player avatar loaded successfully');
    } catch (error) {
      logger.error('Failed to load player avatar via EntityRenderService', { modelId }, error as Error);
    }
  }

  /**
   * Dispose player service
   */
  dispose(): void {
    this.eventListeners.clear();

    // Dispose third-person model
    if (this.thirdPersonMesh) {
      this.thirdPersonMesh.dispose();
    }

    // Close modifiers
    this.playerViewModifier?.close();
    this.underwaterViewModifier?.close();

    logger.info('PlayerService disposed');
  }
}
