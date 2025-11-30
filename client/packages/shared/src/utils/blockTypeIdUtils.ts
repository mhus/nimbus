/**
 * Utility functions for BlockType ID handling
 * 
 * Provides conversion between legacy number IDs and string IDs
 */

/**
 * Normalize a blockTypeId to string format
 * Converts legacy number IDs to strings automatically
 * 
 * @param id - BlockType ID (can be number or string)
 * @returns Normalized string ID
 */
export function normalizeBlockTypeId(id: number | string): string {
  if (typeof id === 'number') {
    return String(id);
  }
  return id;
}

/**
 * Normalize an array of blockTypeIds to string format
 * Converts legacy number IDs to strings automatically
 * 
 * @param ids - Array of BlockType IDs (can be numbers or strings)
 * @returns Array of normalized string IDs
 */
export function normalizeBlockTypeIds(ids: (number | string)[]): string[] {
  return ids.map(normalizeBlockTypeId);
}

/**
 * Check if a blockTypeId represents "air" (empty block)
 * 
 * @param id - BlockType ID
 * @returns true if the ID represents air/empty
 */
export function isAirBlockTypeId(id: number | string): boolean {
  const normalized = normalizeBlockTypeId(id);
  return normalized === '0';
}
