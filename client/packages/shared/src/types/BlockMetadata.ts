/**
 * BlockMetadata - Additional metadata for block instances
 *
 * Metadata merging priority (first match wins):
 * 1. Block-BlockType-status-Metadata (instance status)
 * 2. Block-BlockType-ID status-Metadata (instance status = world status)
 * 3. Block-BlockType-ID status-Metadata (base status)
 * 4. Block-BlockType-ID status-Metadata (base status = world status)
 * 5. Default values (e.g., shape = 0)
 */

export interface BlockMetadata {
  /**
   * Display name for UI
   */
  displayName?: string;

  /**
   * Internal name
   */
  name?: string;

  /**
   * Group ID for organization/categorization
   */
  groupId?: number;
}
