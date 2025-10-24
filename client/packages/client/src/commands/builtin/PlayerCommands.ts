/**
 * Player Commands
 * Commands for player interaction (position, teleport, etc.)
 */
import type { Command } from '../Command';
import type { CmdExecutionContext } from '../CmdExecutionContext';
import type { VoxelClient } from '../../VoxelClient';
import { MovementMode } from '../../player/PlayerController';

/**
 * Position Command - Show current player position
 */
export class PositionCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'position';
  }

  getDescription(): string {
    return 'Shows current player position';
  }

  getHelp(): string {
    return `Usage: position

Displays the current position of the player in the world.`;
  }

  execute(context: CmdExecutionContext): void {
    const camera = (this.client as any).camera;

    if (!camera) {
      context.writeError('Camera not available');
      return;
    }

    const pos = camera.position;
    const blockPos = {
      x: Math.floor(pos.x),
      y: Math.floor(pos.y),
      z: Math.floor(pos.z),
    };
    const chunkPos = {
      x: Math.floor(blockPos.x / 32),
      z: Math.floor(blockPos.z / 32),
    };

    context.writeLine('Player Position:');
    context.writeLine(`  World:  (${pos.x.toFixed(2)}, ${pos.y.toFixed(2)}, ${pos.z.toFixed(2)})`);
    context.writeLine(`  Block:  (${blockPos.x}, ${blockPos.y}, ${blockPos.z})`);
    context.writeLine(`  Chunk:  (${chunkPos.x}, ${chunkPos.z})`);
  }
}

/**
 * Teleport Command - Teleport player to coordinates
 */
export class TeleportCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'teleport';
  }

  getDescription(): string {
    return 'Teleport to coordinates';
  }

  getHelp(): string {
    return `Usage: teleport <x> <y> <z>

Teleports the player to the specified coordinates.

Examples:
  teleport 0 80 0      - Teleport to spawn
  teleport 100 70 -50  - Teleport to coordinates`;
  }

  execute(context: CmdExecutionContext): void {
    const camera = (this.client as any).camera;

    if (!camera) {
      context.writeError('Camera not available');
      return;
    }

    const x = parseFloat(context.getParameter(0) || '');
    const y = parseFloat(context.getParameter(1) || '');
    const z = parseFloat(context.getParameter(2) || '');

    if (isNaN(x) || isNaN(y) || isNaN(z)) {
      context.writeError('Invalid coordinates. Usage: teleport <x> <y> <z>');
      return;
    }

    camera.position.set(x, y, z);
    context.writeLine(`Teleported to (${x}, ${y}, ${z})`);
  }
}

/**
 * Start Command - Teleport to spawn
 */
export class StartCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'start';
  }

  getDescription(): string {
    return 'Teleport to spawn point';
  }

  getHelp(): string {
    return `Usage: start

Teleports the player to the spawn point (0, 80, 0).`;
  }

  execute(context: CmdExecutionContext): void {
    const camera = (this.client as any).camera;

    if (!camera) {
      context.writeError('Camera not available');
      return;
    }

    camera.position.set(0, 80, 0);
    context.writeLine('Teleported to spawn');
  }
}

/**
 * Flight Command - Enable flight mode
 */
export class FlightCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'flight';
  }

  getDescription(): string {
    return 'Enable flight mode';
  }

  getHelp(): string {
    return `Usage: flight

Switches the player to flight mode, allowing free movement in all directions.`;
  }

  execute(context: CmdExecutionContext): void {
    const playerController = (this.client as any).playerController;

    if (!playerController) {
      context.writeError('Player controller not available');
      return;
    }

    playerController.setMode(MovementMode.FLIGHT);
    context.writeLine('Flight mode enabled');
  }
}

/**
 * Walk Command - Enable walk mode
 */
export class WalkCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'walk';
  }

  getDescription(): string {
    return 'Enable walk mode';
  }

  getHelp(): string {
    return `Usage: walk

Switches the player to walk mode with gravity and collision detection.`;
  }

  execute(context: CmdExecutionContext): void {
    const playerController = (this.client as any).playerController;

    if (!playerController) {
      context.writeError('Player controller not available');
      return;
    }

    playerController.setMode(MovementMode.WALK);
    context.writeLine('Walk mode enabled');
  }
}

/**
 * OrbitDistance Command - Set orbit camera distance
 */
export class OrbitDistanceCommand implements Command {
  private client: VoxelClient;

  constructor(client: VoxelClient) {
    this.client = client;
  }

  getName(): string {
    return 'orbitdist';
  }

  getDescription(): string {
    return 'Set orbit camera distance';
  }

  getHelp(): string {
    return `Usage: orbitdist [distance]

Sets the distance to the focus point for orbit camera mode (Shift+Q/E/Z/X in flight mode).
Without parameter, shows current distance.

Examples:
  orbitdist       - Show current orbit distance
  orbitdist 5     - Set orbit distance to 5 blocks
  orbitdist 10.5  - Set orbit distance to 10.5 blocks`;
  }

  execute(context: CmdExecutionContext): void {
    const playerController = (this.client as any).playerController;

    if (!playerController) {
      context.writeError('Player controller not available');
      return;
    }

    const distanceParam = context.getParameter(0);

    if (!distanceParam) {
      // Show current distance
      const currentDistance = playerController.getOrbitDistance();
      context.writeLine(`Orbit distance: ${currentDistance} blocks`);
      return;
    }

    const distance = parseFloat(distanceParam);

    if (isNaN(distance) || distance <= 0) {
      context.writeError('Invalid distance. Must be a positive number.');
      return;
    }

    playerController.setOrbitDistance(distance);
    context.writeLine(`Orbit distance set to ${playerController.getOrbitDistance()} blocks`);
  }
}
