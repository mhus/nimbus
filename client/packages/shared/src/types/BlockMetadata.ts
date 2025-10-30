/**
 * BlockMetadata - Additional metadata for block instances
 *
 * Contains instance-specific data that is not part of the block type definition.
 * This includes organizational data like group membership and display names.
 *
 * Note: Block modifiers have been moved to Block.modifiers for better structure.
 */

export interface BlockMetadata {
  /**
   * Group ID for organization/categorization
   */
  groupId?: number;

}
