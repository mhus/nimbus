/**
 * BlockType - Definition/Template for a block type
 *
 * This is the registry definition that defines what a block type is.
 * BlockType instances in the world only store the BlockType ID.
 */

/**
 * Block status values
 * 0 = default status
 * 1-9 = standard states (open, closed, locked, destroyed, etc.)
 * 10-17 = seasonal states
 * 100+ = custom world-specific states
 */
export enum BlockStatus {
  DEFAULT = 0,
  OPEN = 1,
  CLOSED = 2,
  LOCKED = 3,
  DESTROYED = 5,

  // Seasonal states
  WINTER = 10,
  WINTER_SPRING = 11,
  SPRING = 12,
  SPRING_SUMMER = 13,
  SUMMER = 14,
  SUMMER_AUTUMN = 15,
  AUTUMN = 16,
  AUTUMN_WINTER = 17,

  // Custom states start at 100
  CUSTOM_START = 100,
}

/**
 * BlockType definition
 */
export interface BlockType {
  /**
   * Unique block type ID
   */
  id: number;

  /**
   * Initial status for new block instances
   * @default 0 (BlockStatus.DEFAULT)
   */
  initialStatus?: number;

  /**
   * Block modifier defining visual and behavioral properties
   */
  status: number; // References BlockModifier for this status
}
