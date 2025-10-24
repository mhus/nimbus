/**
 * Block Metadata System
 *
 * Stores additional per-block data like rotation, state, damage, etc.
 * Metadata is stored separately from block IDs for memory efficiency.
 */

/**
 * Block rotation/facing directions
 */
export enum BlockFacing {
  NORTH = 0,   // -Z
  EAST = 1,    // +X
  SOUTH = 2,   // +Z
  WEST = 3,    // -X
  UP = 4,      // +Y
  DOWN = 5,    // -Y
}

/**
 * Rotation axis for blocks
 */
export enum RotationAxis {
  NONE = 0,
  X = 1,
  Y = 2,
  Z = 3,
}

/**
 * Block state flags (can be combined with bitwise OR)
 */
export enum BlockState {
  NONE = 0,
  OPEN = 1 << 0,        // Door/gate is open
  POWERED = 1 << 1,     // Block is powered by redstone
  LIT = 1 << 2,         // Furnace/torch is lit
  TRIGGERED = 1 << 3,   // Dispenser/dropper is triggered
  EXTENDED = 1 << 4,    // Piston is extended
  WATERLOGGED = 1 << 5, // Block contains water
  SNOWY = 1 << 6,       // Block has snow on top
  PERSISTENT = 1 << 7,  // Leaves won't decay
}

/**
 * Core block metadata structure
 *
 * Stores per-instance block data that should be saved with the world.
 * Properties here are persisted to disk, while BlockModifier properties are temporary/visual.
 */
export interface BlockMetadata {
  /** Override display name for this specific block instance */
  displayName?: string;

  /** Group ID to associate this block with a group */
  groupId?: string;
}

/**
 * Extended metadata for special blocks (stored separately)
 */
export interface ExtendedBlockMetadata extends BlockMetadata {
  /** Block entity data (inventory, text, etc.) */
  blockEntity?: Record<string, any>;

  /** Custom properties */
  custom?: Record<string, any>;
}

/**
 * Pack metadata into 16-bit integer
 * DEPRECATED: Returns 0 as BlockMetadata is now empty
 */
export function packMetadata(metadata: BlockMetadata): number {
  return 0;
}

/**
 * Unpack metadata from 16-bit integer
 * DEPRECATED: Returns empty object as BlockMetadata is now empty
 */
export function unpackMetadata(packed: number): BlockMetadata {
  return {};
}

/**
 * Create default metadata (no rotation, no state)
 * DEPRECATED: Returns empty object as BlockMetadata is now empty
 */
export function createDefaultMetadata(): BlockMetadata {
  return {};
}

/**
 * Create metadata with facing direction
 * DEPRECATED: Returns empty object as BlockMetadata is now empty
 */
export function createMetadataWithFacing(facing: BlockFacing): BlockMetadata {
  return {};
}

/**
 * Create metadata with rotation
 * DEPRECATED: Returns empty object as BlockMetadata is now empty
 */
export function createMetadataWithRotation(
  axis: RotationAxis,
  angleDegrees: number
): BlockMetadata {
  return {};
}

/**
 * Get rotation angle in degrees from metadata
 * DEPRECATED: Returns 0 as BlockMetadata is now empty
 */
export function getRotationAngle(metadata: BlockMetadata): number {
  return 0;
}

/**
 * Get rotation angle in radians from metadata
 * DEPRECATED: Returns 0 as BlockMetadata is now empty
 */
export function getRotationAngleRadians(metadata: BlockMetadata): number {
  return 0;
}

/**
 * Check if block state flag is set
 * DEPRECATED: Returns false as BlockMetadata is now empty
 */
export function hasState(metadata: BlockMetadata, state: BlockState): boolean {
  return false;
}

/**
 * Set block state flag
 * DEPRECATED: Returns empty object as BlockMetadata is now empty
 */
export function setState(metadata: BlockMetadata, state: BlockState): BlockMetadata {
  return {};
}

/**
 * Clear block state flag
 * DEPRECATED: Returns empty object as BlockMetadata is now empty
 */
export function clearState(metadata: BlockMetadata, state: BlockState): BlockMetadata {
  return {};
}

/**
 * Toggle block state flag
 * DEPRECATED: Returns empty object as BlockMetadata is now empty
 */
export function toggleState(metadata: BlockMetadata, state: BlockState): BlockMetadata {
  return {};
}

/**
 * Rotate block metadata 90° clockwise around Y axis
 * DEPRECATED: Returns empty object as BlockMetadata is now empty
 */
export function rotateMetadataY(metadata: BlockMetadata): BlockMetadata {
  return {};
}

/**
 * Get facing vector from BlockFacing
 */
export function getFacingVector(facing: BlockFacing): [number, number, number] {
  switch (facing) {
    case BlockFacing.NORTH: return [0, 0, -1];
    case BlockFacing.EAST:  return [1, 0, 0];
    case BlockFacing.SOUTH: return [0, 0, 1];
    case BlockFacing.WEST:  return [-1, 0, 0];
    case BlockFacing.UP:    return [0, 1, 0];
    case BlockFacing.DOWN:  return [0, -1, 0];
    default: return [0, 0, 0];
  }
}

/**
 * Get BlockFacing from player look direction
 */
export function getFacingFromDirection(dx: number, dz: number): BlockFacing {
  // Calculate angle from direction vector
  const angle = Math.atan2(dz, dx) * 180 / Math.PI;

  // Convert to facing (0° = East, 90° = South, 180° = West, 270° = North)
  if (angle >= -45 && angle < 45) return BlockFacing.EAST;
  if (angle >= 45 && angle < 135) return BlockFacing.SOUTH;
  if (angle >= 135 || angle < -135) return BlockFacing.WEST;
  return BlockFacing.NORTH;
}

/**
 * Get BlockFacing from player camera direction (includes vertical)
 */
export function getFacingFromCamera(dx: number, dy: number, dz: number): BlockFacing {
  // Check vertical first
  const horizontalLength = Math.sqrt(dx * dx + dz * dz);
  if (Math.abs(dy) > horizontalLength) {
    return dy > 0 ? BlockFacing.UP : BlockFacing.DOWN;
  }

  // Horizontal facing
  return getFacingFromDirection(dx, dz);
}

/**
 * Get opposite facing direction
 */
export function getOppositeFacing(facing: BlockFacing): BlockFacing {
  switch (facing) {
    case BlockFacing.NORTH: return BlockFacing.SOUTH;
    case BlockFacing.SOUTH: return BlockFacing.NORTH;
    case BlockFacing.EAST: return BlockFacing.WEST;
    case BlockFacing.WEST: return BlockFacing.EAST;
    case BlockFacing.UP: return BlockFacing.DOWN;
    case BlockFacing.DOWN: return BlockFacing.UP;
    default: return facing;
  }
}

/**
 * Create metadata for a block placed by player
 * Block will face towards the player
 */
export function createMetadataFromPlayerPlacement(
  playerX: number,
  playerY: number,
  playerZ: number,
  blockX: number,
  blockY: number,
  blockZ: number,
  includeVertical: boolean = false
): BlockMetadata {
  // Calculate direction from block to player
  const dx = playerX - blockX;
  const dy = playerY - blockY;
  const dz = playerZ - blockZ;

  // Get facing (block faces player)
  const facing = includeVertical
    ? getFacingFromCamera(dx, dy, dz)
    : getFacingFromDirection(dx, dz);

  return createMetadataWithFacing(facing);
}

/**
 * Create metadata for directional block (e.g., furnace, piston)
 * Block will face away from player (in player's look direction)
 */
export function createMetadataFromPlayerDirection(
  lookDirX: number,
  lookDirY: number,
  lookDirZ: number,
  includeVertical: boolean = false
): BlockMetadata {
  const facing = includeVertical
    ? getFacingFromCamera(lookDirX, lookDirY, lookDirZ)
    : getFacingFromDirection(lookDirX, lookDirZ);

  return createMetadataWithFacing(facing);
}

/**
 * Check if two metadata objects are equal
 * DEPRECATED: Always returns true as BlockMetadata is now empty
 */
export function metadataEquals(a: BlockMetadata, b: BlockMetadata): boolean {
  return true;
}

/**
 * Clone metadata
 * DEPRECATED: Returns empty object as BlockMetadata is now empty
 */
export function cloneMetadata(metadata: BlockMetadata): BlockMetadata {
  return {};
}

/**
 * Merge metadata (combine state flags)
 * DEPRECATED: Returns empty object as BlockMetadata is now empty
 */
export function mergeMetadata(base: BlockMetadata, override: Partial<BlockMetadata>): BlockMetadata {
  return {};
}

/**
 * Check if metadata is default (no rotation, no state)
 * DEPRECATED: Always returns true as BlockMetadata is now empty
 */
export function isDefaultMetadata(metadata: BlockMetadata): boolean {
  return true;
}

/**
 * Create metadata with multiple state flags
 * DEPRECATED: Returns empty object as BlockMetadata is now empty
 */
export function createMetadataWithStates(...states: BlockState[]): BlockMetadata {
  return {};
}
