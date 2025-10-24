/**
 * Player Controller - Handles player movement, collision, gravity
 */

import { Vector3, Scene, FreeCamera, KeyboardEventTypes } from '@babylonjs/core';
import type { ChunkManager } from '../world/ChunkManager';
import type { ClientRegistry } from '../registry/ClientRegistry';

export enum MovementMode {
  WALK = 'walk',
  FLIGHT = 'flight',
}

/**
 * Controls player movement with collision detection and gravity
 */
export class PlayerController {
  private scene: Scene;
  private camera: FreeCamera;
  private chunkManager: ChunkManager;
  private registry: ClientRegistry;

  private mode: MovementMode = MovementMode.FLIGHT;
  private velocity: Vector3 = Vector3.Zero();

  // Movement settings
  private walkSpeed = 4.3; // Blocks per second
  private flySpeed = 10.5; // Blocks per second
  private jumpVelocity = 10.0; // Increased for 2-block jump
  private gravity = -26.0; // Blocks per second^2
  private maxStepHeight = 1.0; // Auto-step up to 1 block

  // Player dimensions
  private playerHeight = 1.8;
  private playerWidth = 0.6;
  private eyeHeight = 1.62; // Distance from feet to camera

  // State
  private isOnGround = false;
  private moveForward = false;
  private moveBackward = false;
  private moveLeft = false;
  private moveRight = false;
  private moveUp = false;
  private moveDown = false;
  private wantJump = false;
  private turnLeft = false;
  private turnRight = false;
  private pitchUp = false;
  private pitchDown = false;
  private collisionEnabled = true;

  // Camera rotation settings
  private turnSpeed = 1.5; // Radians per second
  private pitchSpeed = 1.2; // Radians per second for vertical camera rotation

  // Orbit camera settings (Q/E + Shift+Space/§ in flight mode)
  private orbitDistance = 4.0; // Distance to focus point in blocks
  private orbitRotateLeft = false;
  private orbitRotateRight = false;
  private orbitRotateUp = false;
  private orbitRotateDown = false;
  private orbitSpeed = 1.0; // Radians per second for orbit rotation

  constructor(scene: Scene, camera: FreeCamera, chunkManager: ChunkManager, registry: ClientRegistry) {
    this.scene = scene;
    this.camera = camera;
    this.chunkManager = chunkManager;
    this.registry = registry;

    this.setupControls();
    this.startUpdateLoop();

    console.log('[PlayerController] Initialized in FLIGHT mode');
  }

  /**
   * Setup keyboard controls
   */
  private setupControls(): void {
    this.scene.onKeyboardObservable.add((kbInfo) => {
      const key = kbInfo.event.key; // Keep case-sensitive!
      const isDown = kbInfo.type === KeyboardEventTypes.KEYDOWN;
      const shiftPressed = kbInfo.event.shiftKey;

      switch (key) {
        case 'w':
        case 'W':
          this.moveForward = isDown;
          break;
        case 's':
        case 'S':
          this.moveBackward = isDown;
          break;
        case 'a':
        case 'A':
          this.moveLeft = isDown;
          break;
        case 'd':
        case 'D':
          this.moveRight = isDown;
          break;
        case ' ':
          if (this.mode === MovementMode.WALK) {
            this.wantJump = isDown;
          } else if (this.mode === MovementMode.FLIGHT && shiftPressed) {
            // Shift+Space in flight mode: orbit rotate up
            this.orbitRotateUp = isDown;
          } else {
            // In flight mode: Space = move up
            this.moveUp = isDown;
          }
          break;
        case '§':
          if (this.mode === MovementMode.FLIGHT && shiftPressed) {
            // Shift+§ in flight mode: orbit rotate down
            this.orbitRotateDown = isDown;
          } else if (this.mode === MovementMode.FLIGHT) {
            // In flight mode: § = move down
            this.moveDown = isDown;
          }
          break;
        case 'f':
        case 'F':
          if (isDown) {
            this.toggleMode();
          }
          break;

        // Lowercase q = turn left (normal)
        case 'q':
          this.turnLeft = isDown;
          break;

        // Uppercase Q = orbit rotate left (flight mode only)
        case 'Q':
          if (this.mode === MovementMode.FLIGHT) {
            this.orbitRotateLeft = isDown;
          }
          break;

        // Lowercase e = turn right (normal)
        case 'e':
          this.turnRight = isDown;
          break;

        // Uppercase E = orbit rotate right (flight mode only)
        case 'E':
          if (this.mode === MovementMode.FLIGHT) {
            this.orbitRotateRight = isDown;
          }
          break;
      }
    });
  }

  /**
   * Toggle between walk and flight mode
   */
  private toggleMode(): void {
    if (this.mode === MovementMode.WALK) {
      this.mode = MovementMode.FLIGHT;
      this.velocity.y = 0; // Cancel gravity
      console.log('[PlayerController] Switched to FLIGHT mode');
    } else {
      this.mode = MovementMode.WALK;
      console.log('[PlayerController] Switched to WALK mode');
    }
  }

  /**
   * Start update loop
   */
  private startUpdateLoop(): void {
    let lastTime = performance.now();

    this.scene.onBeforeRenderObservable.add(() => {
      const currentTime = performance.now();
      const deltaTime = (currentTime - lastTime) / 1000; // Convert to seconds
      lastTime = currentTime;

      this.update(deltaTime);
    });
  }

  /**
   * Update player physics and movement
   */
  private update(deltaTime: number): void {
    // Cap delta time to prevent large jumps
    deltaTime = Math.min(deltaTime, 0.1);

    // Handle camera rotation
    this.updateCameraRotation(deltaTime);

    if (this.mode === MovementMode.WALK) {
      this.updateWalkMode(deltaTime);
    } else {
      this.updateFlightMode(deltaTime);
    }
  }

  /**
   * Update camera rotation from keyboard input
   */
  private updateCameraRotation(deltaTime: number): void {
    // Horizontal rotation (yaw) - Q and E keys
    if (this.turnLeft) {
      this.camera.rotation.y -= this.turnSpeed * deltaTime;
    }
    if (this.turnRight) {
      this.camera.rotation.y += this.turnSpeed * deltaTime;
    }

    // Note: Vertical camera rotation (pitch) is handled by mouse in flight mode
    // In walk mode, pitch would be handled here if enabled via keyboard
  }

  /**
   * Update walk mode (with gravity and collision)
   */
  private updateWalkMode(deltaTime: number): void {
    // Apply gravity
    this.velocity.y += this.gravity * deltaTime;

    // Get movement input
    const moveDirection = this.getMovementDirection();

    // Apply horizontal movement
    const speed = this.walkSpeed;
    const moveVelocity = moveDirection.scale(speed);

    // Combine velocities
    const targetPosition = this.camera.position.clone();
    targetPosition.x += moveVelocity.x * deltaTime;
    targetPosition.y += this.velocity.y * deltaTime;
    targetPosition.z += moveVelocity.z * deltaTime;

    // Check collision and adjust position
    const finalPosition = this.handleCollision(this.camera.position, targetPosition);

    // Update camera position
    this.camera.position.copyFrom(finalPosition);

    // Check if on ground
    this.isOnGround = this.checkGround();

    // Handle jumping
    if (this.wantJump && this.isOnGround) {
      this.velocity.y = this.jumpVelocity;
      this.isOnGround = false;
      this.wantJump = false;
    }

    // Stop falling if on ground
    if (this.isOnGround && this.velocity.y < 0) {
      this.velocity.y = 0;
    }
  }

  /**
   * Update flight mode (no gravity, free movement in look direction)
   */
  private updateFlightMode(deltaTime: number): void {
    // Check if orbit rotation is active (Q/E + Shift+Space/§)
    const orbitActive = this.orbitRotateLeft || this.orbitRotateRight ||
                        this.orbitRotateUp || this.orbitRotateDown;

    if (orbitActive) {
      // Orbit mode: rotate camera around a focus point
      this.updateOrbitRotation(deltaTime);
    } else {
      // Normal flight mode
      // Get movement input with full 3D direction (includes vertical component)
      const moveDirection = this.getFlightMovementDirection();

      // Add additional vertical movement for space/shift
      if (this.moveUp) {
        moveDirection.y += 1;
      }
      if (this.moveDown) {
        moveDirection.y -= 1;
      }

      // Normalize if moving
      if (moveDirection.length() > 0) {
        moveDirection.normalize();
      }

      // Apply movement
      const speed = this.flySpeed;
      const targetPosition = this.camera.position.clone();
      targetPosition.addInPlace(moveDirection.scale(speed * deltaTime));

      // Check collision if enabled (in flight mode, collision can be disabled)
      const finalPosition = this.collisionEnabled
        ? this.handleCollision(this.camera.position, targetPosition)
        : targetPosition;

      // Update camera position
      this.camera.position.copyFrom(finalPosition);
    }
  }

  /**
   * Update orbit rotation (Q/E + Shift+Space/§ in flight mode)
   * Rotates camera around a focus point in front of the camera
   */
  private updateOrbitRotation(deltaTime: number): void {
    // Calculate focus point (orbitDistance blocks in front of camera)
    const forward = this.camera.getDirection(Vector3.Forward());
    const focusPoint = this.camera.position.clone().add(forward.scale(this.orbitDistance));

    // Calculate rotation angles
    let yawDelta = 0; // Horizontal rotation (Q/E)
    let pitchDelta = 0; // Vertical rotation (Shift+Space/§)

    if (this.orbitRotateLeft) {
      yawDelta = -this.orbitSpeed * deltaTime;
    }
    if (this.orbitRotateRight) {
      yawDelta = this.orbitSpeed * deltaTime;
    }
    if (this.orbitRotateUp) {
      pitchDelta = -this.orbitSpeed * deltaTime;
    }
    if (this.orbitRotateDown) {
      pitchDelta = this.orbitSpeed * deltaTime;
    }

    // Get current offset from focus point
    const offset = this.camera.position.subtract(focusPoint);

    // Apply horizontal rotation (yaw) around Y axis
    if (yawDelta !== 0) {
      const cosYaw = Math.cos(yawDelta);
      const sinYaw = Math.sin(yawDelta);
      const newX = offset.x * cosYaw - offset.z * sinYaw;
      const newZ = offset.x * sinYaw + offset.z * cosYaw;
      offset.x = newX;
      offset.z = newZ;
    }

    // Apply vertical rotation (pitch)
    if (pitchDelta !== 0) {
      // Calculate current pitch angle
      const horizontalDist = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
      const currentPitch = Math.atan2(offset.y, horizontalDist);
      const newPitch = currentPitch + pitchDelta;

      // Clamp pitch to prevent flipping (roughly -85° to +85°)
      const maxPitch = Math.PI / 2 - 0.1;
      const clampedPitch = Math.max(-maxPitch, Math.min(maxPitch, newPitch));

      // Calculate new offset with new pitch
      const distance = offset.length();
      offset.y = Math.sin(clampedPitch) * distance;
      const newHorizontalDist = Math.cos(clampedPitch) * distance;

      // Scale horizontal components to match new distance
      const scale = newHorizontalDist / (horizontalDist || 1);
      offset.x *= scale;
      offset.z *= scale;
    }

    // Update camera position
    this.camera.position.copyFrom(focusPoint.add(offset));

    // Make camera look at focus point
    this.camera.setTarget(focusPoint);
  }

  /**
   * Get movement direction from input (walk mode - horizontal only)
   */
  private getMovementDirection(): Vector3 {
    const direction = Vector3.Zero();

    // Get camera forward and right vectors (only horizontal)
    const forward = this.camera.getDirection(Vector3.Forward());
    forward.y = 0;
    forward.normalize();

    const right = this.camera.getDirection(Vector3.Right());
    right.y = 0;
    right.normalize();

    // Apply input
    if (this.moveForward) {
      direction.addInPlace(forward);
    }
    if (this.moveBackward) {
      direction.subtractInPlace(forward);
    }
    if (this.moveLeft) {
      direction.subtractInPlace(right);
    }
    if (this.moveRight) {
      direction.addInPlace(right);
    }

    return direction;
  }

  /**
   * Get movement direction for flight mode (includes vertical component)
   */
  private getFlightMovementDirection(): Vector3 {
    const direction = Vector3.Zero();

    // Get camera forward and right vectors (with vertical component!)
    const forward = this.camera.getDirection(Vector3.Forward());
    const right = this.camera.getDirection(Vector3.Right());

    // Apply input
    if (this.moveForward) {
      direction.addInPlace(forward);
    }
    if (this.moveBackward) {
      direction.subtractInPlace(forward);
    }
    if (this.moveLeft) {
      direction.subtractInPlace(right);
    }
    if (this.moveRight) {
      direction.addInPlace(right);
    }

    return direction;
  }

  /**
   * Handle collision detection
   */
  private handleCollision(fromPos: Vector3, toPos: Vector3): Vector3 {
    const finalPos = toPos.clone();

    // Check collision in each axis separately
    // This allows sliding along walls

    // X axis
    const testX = new Vector3(toPos.x, fromPos.y, fromPos.z);
    if (this.checkCollisionAtPosition(testX)) {
      // Try auto-step in walk mode
      if (this.mode === MovementMode.WALK && this.tryAutoStep(fromPos, toPos, finalPos)) {
        return finalPos; // Auto-step successful
      }
      finalPos.x = fromPos.x;
    }

    // Y axis
    const testY = new Vector3(finalPos.x, toPos.y, fromPos.z);
    if (this.checkCollisionAtPosition(testY)) {
      finalPos.y = fromPos.y;

      // If hitting ceiling, cancel upward velocity
      if (toPos.y > fromPos.y) {
        this.velocity.y = Math.min(0, this.velocity.y);
      }

      // If hitting ground, we're on ground
      if (toPos.y < fromPos.y) {
        this.isOnGround = true;
        this.velocity.y = 0;
      }
    }

    // Z axis
    const testZ = new Vector3(finalPos.x, finalPos.y, toPos.z);
    if (this.checkCollisionAtPosition(testZ)) {
      // Try auto-step in walk mode
      if (this.mode === MovementMode.WALK && this.tryAutoStep(fromPos, toPos, finalPos)) {
        return finalPos; // Auto-step successful
      }
      finalPos.z = fromPos.z;
    }

    return finalPos;
  }

  /**
   * Try to automatically step up when hitting a wall (max 1 block)
   */
  private tryAutoStep(fromPos: Vector3, toPos: Vector3, finalPos: Vector3): boolean {
    // Only auto-step when on ground
    if (!this.isOnGround) {
      return false;
    }

    // Try stepping up in small increments
    const stepIncrement = 0.1;
    for (let stepHeight = stepIncrement; stepHeight <= this.maxStepHeight; stepHeight += stepIncrement) {
      const steppedPos = toPos.clone();
      steppedPos.y = fromPos.y + stepHeight;

      // Check if we can fit at this height
      if (!this.checkCollisionAtPosition(steppedPos)) {
        // We can step up to this height
        finalPos.copyFrom(steppedPos);
        return true;
      }
    }

    return false;
  }

  /**
   * Check if position collides with solid blocks
   */
  private checkCollisionAtPosition(position: Vector3): boolean {
    // Check multiple points on player bounding box
    const points = [
      // Feet level
      new Vector3(position.x - this.playerWidth/2, position.y - this.eyeHeight, position.z - this.playerWidth/2),
      new Vector3(position.x + this.playerWidth/2, position.y - this.eyeHeight, position.z - this.playerWidth/2),
      new Vector3(position.x - this.playerWidth/2, position.y - this.eyeHeight, position.z + this.playerWidth/2),
      new Vector3(position.x + this.playerWidth/2, position.y - this.eyeHeight, position.z + this.playerWidth/2),

      // Head level
      new Vector3(position.x - this.playerWidth/2, position.y - this.eyeHeight + this.playerHeight, position.z - this.playerWidth/2),
      new Vector3(position.x + this.playerWidth/2, position.y - this.eyeHeight + this.playerHeight, position.z - this.playerWidth/2),
      new Vector3(position.x - this.playerWidth/2, position.y - this.eyeHeight + this.playerHeight, position.z + this.playerWidth/2),
      new Vector3(position.x + this.playerWidth/2, position.y - this.eyeHeight + this.playerHeight, position.z + this.playerWidth/2),

      // Middle level
      new Vector3(position.x, position.y - this.eyeHeight + this.playerHeight/2, position.z),
    ];

    for (const point of points) {
      if (this.isBlockSolid(point)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Check if on ground
   */
  private checkGround(): boolean {
    // Check slightly below feet
    const feetPos = this.camera.position.clone();
    feetPos.y -= this.eyeHeight + 0.1; // Slightly below feet

    return this.isBlockSolid(feetPos);
  }

  /**
   * Check if block at world position is solid
   */
  private isBlockSolid(worldPos: Vector3): boolean {
    // Get block coordinates
    const blockX = Math.floor(worldPos.x);
    const blockY = Math.floor(worldPos.y);
    const blockZ = Math.floor(worldPos.z);

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
      // Chunk not loaded, assume solid
      return true;
    }

    // Check if block coordinates are valid
    if (localX < 0 || localX >= 32 || localZ < 0 || localZ >= 32 || blockY < 0 || blockY >= 256) {
      return false;
    }

    // Get block ID
    const index = localX + blockY * 32 + localZ * 32 * 256;
    const blockId = chunk.data[index];

    // Block ID 0 is air
    if (blockId === 0) {
      return false;
    }

    // Get block definition from registry
    const block = this.registry.getBlockByID(blockId);
    if (!block) {
      // Unknown block, assume solid
      return true;
    }

    // Check if block is a fluid
    // Fluids can be passed through (no collision)
    if (block.options?.fluid) {
      return false;
    }

    // All other blocks are solid
    return true;
  }

  /**
   * Get current movement mode
   */
  getMode(): MovementMode {
    return this.mode;
  }

  /**
   * Set movement mode
   */
  setMode(mode: MovementMode): void {
    if (this.mode !== mode) {
      this.mode = mode;
      if (mode === MovementMode.FLIGHT) {
        this.velocity.y = 0;
      }
      console.log(`[PlayerController] Mode set to ${mode}`);
    }
  }

  /**
   * Enable collision detection
   */
  enableCollision(): void {
    this.collisionEnabled = true;
    console.log('[PlayerController] Collision enabled');
  }

  /**
   * Disable collision detection (only affects flight mode)
   */
  disableCollision(): void {
    this.collisionEnabled = false;
    console.log('[PlayerController] Collision disabled');
  }

  /**
   * Check if collision is enabled
   */
  isCollisionEnabled(): boolean {
    return this.collisionEnabled;
  }

  /**
   * Get orbit distance (distance to focus point for orbit rotation)
   */
  getOrbitDistance(): number {
    return this.orbitDistance;
  }

  /**
   * Set orbit distance (distance to focus point for orbit rotation)
   */
  setOrbitDistance(distance: number): void {
    this.orbitDistance = Math.max(0.5, Math.min(100, distance)); // Clamp between 0.5 and 100 blocks
    console.log(`[PlayerController] Orbit distance set to ${this.orbitDistance} blocks`);
  }
}
