/**
 * WLayer type definition
 * Main registry for all layers
 */

export enum LayerType {
  TERRAIN = 'TERRAIN',
  MODEL = 'MODEL'
}

export interface WLayer {
  id: string;
  worldId: string;
  name: string;
  layerType: LayerType;

  /**
   * Reference to LayerTerrain or LayerModel collection
   */
  layerDataId?: string;

  /**
   * For ModelLayer: mount point X coordinate
   */
  mountX?: number;

  /**
   * For ModelLayer: mount point Y coordinate
   */
  mountY?: number;

  /**
   * For ModelLayer: mount point Z coordinate
   */
  mountZ?: number;

  /**
   * If true, this layer defines ground level (affects terrain generation)
   */
  ground: boolean;

  /**
   * If true, this layer affects all chunks in the world
   * If false, only chunks in affectedChunks list are affected
   */
  allChunks: boolean;

  /**
   * List of chunk keys (format: "cx:cz") affected by this layer
   * Only used if allChunks is false
   */
  affectedChunks?: string[];

  /**
   * Layer overlay order
   * Lower values are rendered first (bottom), higher values on top
   */
  order: number;

  /**
   * Layer enabled flag (soft delete)
   */
  enabled: boolean;

  /**
   * List of group names defined in this layer
   * Blocks can be assigned to groups for organized management
   */
  groups?: Record<number, string>;

  createdAt?: string;
  updatedAt?: string;
}
