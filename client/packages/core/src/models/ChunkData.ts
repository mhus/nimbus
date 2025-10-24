/**
 * Chunk Data Structure
 *
 * Defines the structure for chunk data transmission and storage.
 * Chunks are 32x32xH blocks with optional metadata.
 */

import type { BlockModifier, BlockType } from '../registry/BlockType';
import type { BlockMetadata } from './BlockMetadata';

/**
 * Block Instance - A concrete block in the world with an attached BlockType
 * This is a runtime-only structure, not serialized
 */
export interface BlockInstance {
  /** Block ID (references BlockType in registry) */
  blockId: number;

  /** Attached BlockType object (resolved from registry) */
  blockType: BlockType;

  /** Optional modifier that overrides BlockType properties */
  modifier?: BlockModifier;

  /** Position in chunk (local coordinates 0-31) */
  position: {
    x: number;
    y: number;
    z: number;
  };
}

/**
 * Chunk data structure for network transmission and storage
 */
export interface ChunkData {
  /** Chunk X coordinate */
  chunkX: number;

  /** Chunk Z coordinate */
  chunkZ: number;

  /** Block IDs (Uint16Array or number array) */
  data: Uint16Array | number[];

  /** Chunk height (Y dimension), defaults to 256 */
  height?: number;

  /** Optional metadata array (packed 16-bit metadata per block) */
  metadata?: Uint16Array | number[];

  /**
   * Optional edge offset array for vertex deformation
   * Each block has 8 corners (vertices) with 3 offset values (x, y, z) each = 24 values per block
   * Values are signed bytes (-127 to 128) stored as Int8Array
   * Negative values move vertices inward, positive values move them outward
   * If not set, blocks render as perfect cubes
   */
  edgeOffset?: Int8Array | number[];

  /**
   * Optional modifiers for individual blocks
   * Key: block index (calculated via getBlockIndex)
   * Value: BlockModifier object with property overrides
   * Only blocks with modifiers are stored in this map (sparse storage)
   */
  modifiers?: Map<number, BlockModifier> | Record<number, BlockModifier>;

  /**
   * Optional metadata for individual blocks
   * Key: block index (calculated via getBlockIndex)
   * Value: BlockMetadata object with persistent per-instance data (e.g., displayName)
   * Only blocks with metadata are stored in this map (sparse storage)
   */
  blockMetadata?: Map<number, BlockMetadata> | Record<number, BlockMetadata>;

  /** Whether data is compressed */
  compressed?: boolean;
}

/**
 * Create empty chunk data
 */
export function createEmptyChunkData(
  chunkX: number,
  chunkZ: number,
  chunkSize: number = 32,
  height: number = 256
): ChunkData {
  const totalBlocks = chunkSize * chunkSize * height;

  return {
    chunkX,
    chunkZ,
    data: new Uint16Array(totalBlocks),
    height,
    metadata: new Uint16Array(totalBlocks), // All zeros = default metadata
  };
}

/**
 * Get block index from coordinates
 * Formula: x + y * chunkSize + z * chunkSize * height
 */
export function getBlockIndex(
  x: number,
  y: number,
  z: number,
  chunkSize: number = 32,
  height: number = 256
): number {
  return x + y * chunkSize + z * chunkSize * height;
}

/**
 * Get coordinates from block index
 */
export function getBlockCoordinates(
  index: number,
  chunkSize: number = 32,
  height: number = 256
): { x: number; y: number; z: number } {
  const x = index % chunkSize;
  const y = Math.floor((index % (chunkSize * height)) / chunkSize);
  const z = Math.floor(index / (chunkSize * height));

  return { x, y, z };
}

/**
 * Set block in chunk data
 */
export function setBlock(
  chunk: ChunkData,
  x: number,
  y: number,
  z: number,
  blockId: number,
  metadata: number = 0,
  chunkSize: number = 32
): void {
  const height = chunk.height || 256;
  const index = getBlockIndex(x, y, z, chunkSize, height);

  // Ensure arrays exist
  if (!chunk.data) {
    chunk.data = new Uint16Array(chunkSize * chunkSize * height);
  }
  if (!chunk.metadata && metadata !== 0) {
    chunk.metadata = new Uint16Array(chunkSize * chunkSize * height);
  }

  // Set block ID
  if (Array.isArray(chunk.data)) {
    chunk.data[index] = blockId;
  } else {
    chunk.data[index] = blockId;
  }

  // Set metadata if provided
  if (metadata !== 0 && chunk.metadata) {
    if (Array.isArray(chunk.metadata)) {
      chunk.metadata[index] = metadata;
    } else {
      chunk.metadata[index] = metadata;
    }
  }
}

/**
 * Get block from chunk data
 */
export function getBlock(
  chunk: ChunkData,
  x: number,
  y: number,
  z: number,
  chunkSize: number = 32
): { blockId: number; metadata: number } {
  const height = chunk.height || 256;
  const index = getBlockIndex(x, y, z, chunkSize, height);

  const blockId = chunk.data[index] || 0;
  const metadata = chunk.metadata?.[index] || 0;

  return { blockId, metadata };
}

/**
 * Set edge offsets for a block
 * @param chunk Chunk data
 * @param x Block X coordinate
 * @param y Block Y coordinate
 * @param z Block Z coordinate
 * @param offsets Array of 24 signed bytes: 8 vertices * 3 values (x,y,z) each
 * @param chunkSize Chunk size (default 32)
 */
export function setBlockEdgeOffsets(
  chunk: ChunkData,
  x: number,
  y: number,
  z: number,
  offsets: number[],
  chunkSize: number = 32
): void {
  if (offsets.length !== 24) {
    throw new Error('Edge offsets must have exactly 24 values (8 vertices * 3 coordinates)');
  }

  const height = chunk.height || 256;
  const blockIndex = getBlockIndex(x, y, z, chunkSize, height);
  const totalBlocks = chunkSize * chunkSize * height;

  // Initialize edgeOffset array if it doesn't exist
  if (!chunk.edgeOffset) {
    chunk.edgeOffset = new Int8Array(totalBlocks * 24);
  }

  // Set the 24 offset values for this block
  const offsetIndex = blockIndex * 24;
  for (let i = 0; i < 24; i++) {
    chunk.edgeOffset[offsetIndex + i] = Math.max(-127, Math.min(128, offsets[i]));
  }
}

/**
 * Get edge offsets for a block
 * @returns Array of 24 signed bytes, or null if no offsets are set
 */
export function getBlockEdgeOffsets(
  chunk: ChunkData,
  x: number,
  y: number,
  z: number,
  chunkSize: number = 32
): number[] | null {
  if (!chunk.edgeOffset) {
    return null;
  }

  const height = chunk.height || 256;
  const blockIndex = getBlockIndex(x, y, z, chunkSize, height);
  const offsetIndex = blockIndex * 24;

  const offsets: number[] = [];
  for (let i = 0; i < 24; i++) {
    offsets.push(chunk.edgeOffset[offsetIndex + i]);
  }

  // Check if all offsets are zero (no deformation)
  if (offsets.every(o => o === 0)) {
    return null;
  }

  return offsets;
}

/**
 * Set modifier for a block
 */
export function setBlockModifier(
  chunk: ChunkData,
  x: number,
  y: number,
  z: number,
  modifier: BlockModifier,
  chunkSize: number = 32
): void {
  const height = chunk.height || 256;
  const index = getBlockIndex(x, y, z, chunkSize, height);

  // Initialize modifiers map if it doesn't exist
  if (!chunk.modifiers) {
    chunk.modifiers = new Map();
  }

  // Convert Record to Map if necessary
  if (!(chunk.modifiers instanceof Map)) {
    chunk.modifiers = new Map(Object.entries(chunk.modifiers).map(([k, v]) => [parseInt(k), v]));
  }

  chunk.modifiers.set(index, modifier);
}

/**
 * Get modifier for a block
 */
export function getBlockModifier(
  chunk: ChunkData,
  x: number,
  y: number,
  z: number,
  chunkSize: number = 32
): BlockModifier | undefined {
  if (!chunk.modifiers) {
    return undefined;
  }

  const height = chunk.height || 256;
  const index = getBlockIndex(x, y, z, chunkSize, height);

  // Handle both Map and Record formats
  if (chunk.modifiers instanceof Map) {
    return chunk.modifiers.get(index);
  } else {
    return chunk.modifiers[index];
  }
}

/**
 * Remove modifier for a block
 */
export function removeBlockModifier(
  chunk: ChunkData,
  x: number,
  y: number,
  z: number,
  chunkSize: number = 32
): void {
  if (!chunk.modifiers) {
    return;
  }

  const height = chunk.height || 256;
  const index = getBlockIndex(x, y, z, chunkSize, height);

  if (chunk.modifiers instanceof Map) {
    chunk.modifiers.delete(index);
  } else {
    delete chunk.modifiers[index];
  }
}

/**
 * Set metadata for a block
 */
export function setBlockMetadataObj(
  chunk: ChunkData,
  x: number,
  y: number,
  z: number,
  metadata: BlockMetadata,
  chunkSize: number = 32
): void {
  const height = chunk.height || 256;
  const index = getBlockIndex(x, y, z, chunkSize, height);

  // Initialize blockMetadata map if it doesn't exist
  if (!chunk.blockMetadata) {
    chunk.blockMetadata = new Map();
  }

  // Convert Record to Map if necessary
  if (!(chunk.blockMetadata instanceof Map)) {
    chunk.blockMetadata = new Map(Object.entries(chunk.blockMetadata).map(([k, v]) => [parseInt(k), v]));
  }

  chunk.blockMetadata.set(index, metadata);
}

/**
 * Get metadata for a block
 */
export function getBlockMetadataObj(
  chunk: ChunkData,
  x: number,
  y: number,
  z: number,
  chunkSize: number = 32
): BlockMetadata | undefined {
  if (!chunk.blockMetadata) {
    return undefined;
  }

  const height = chunk.height || 256;
  const index = getBlockIndex(x, y, z, chunkSize, height);

  // Handle both Map and Record formats
  if (chunk.blockMetadata instanceof Map) {
    return chunk.blockMetadata.get(index);
  } else {
    return chunk.blockMetadata[index];
  }
}

/**
 * Remove metadata for a block
 */
export function removeBlockMetadataObj(
  chunk: ChunkData,
  x: number,
  y: number,
  z: number,
  chunkSize: number = 32
): void {
  if (!chunk.blockMetadata) {
    return;
  }

  const height = chunk.height || 256;
  const index = getBlockIndex(x, y, z, chunkSize, height);

  if (chunk.blockMetadata instanceof Map) {
    chunk.blockMetadata.delete(index);
  } else {
    delete chunk.blockMetadata[index];
  }
}

/**
 * Serialize ChunkData for network transmission or file storage
 * Converts Map to Record format for JSON serialization
 */
export function serializeChunkData(chunk: ChunkData): any {
  const serialized: any = {
    chunkX: chunk.chunkX,
    chunkZ: chunk.chunkZ,
    data: Array.isArray(chunk.data) ? chunk.data : Array.from(chunk.data),
    height: chunk.height,
  };

  // Serialize metadata if present
  if (chunk.metadata) {
    serialized.metadata = Array.isArray(chunk.metadata)
      ? chunk.metadata
      : Array.from(chunk.metadata);
  }

  // Serialize edgeOffset if present
  if (chunk.edgeOffset) {
    serialized.edgeOffset = Array.isArray(chunk.edgeOffset)
      ? chunk.edgeOffset
      : Array.from(chunk.edgeOffset);
  }

  // Serialize modifiers if present (convert Map to array of entries)
  if (chunk.modifiers) {
    if (chunk.modifiers instanceof Map) {
      if (chunk.modifiers.size > 0) {
        serialized.modifiers = Array.from(chunk.modifiers.entries()).map(([index, modifier]) => ({
          index,
          modifier
        }));
      }
    } else {
      // Already in Record format
      const entries = Object.entries(chunk.modifiers);
      if (entries.length > 0) {
        serialized.modifiers = entries.map(([index, modifier]) => ({
          index: parseInt(index),
          modifier
        }));
      }
    }
  }

  // Serialize blockMetadata if present (convert Map to array of entries)
  if (chunk.blockMetadata) {
    if (chunk.blockMetadata instanceof Map) {
      if (chunk.blockMetadata.size > 0) {
        serialized.blockMetadata = Array.from(chunk.blockMetadata.entries()).map(([index, metadata]) => ({
          index,
          metadata
        }));
      }
    } else {
      // Already in Record format
      const entries = Object.entries(chunk.blockMetadata);
      if (entries.length > 0) {
        serialized.blockMetadata = entries.map(([index, metadata]) => ({
          index: parseInt(index),
          metadata
        }));
      }
    }
  }

  if (chunk.compressed !== undefined) {
    serialized.compressed = chunk.compressed;
  }

  return serialized;
}

/**
 * Deserialize ChunkData from network transmission or file storage
 * Converts Record format back to Map for efficient access
 */
export function deserializeChunkData(data: any): ChunkData {
  const chunk: ChunkData = {
    chunkX: data.chunkX,
    chunkZ: data.chunkZ,
    data: Array.isArray(data.data) ? new Uint16Array(data.data) : data.data,
    height: data.height,
  };

  // Deserialize metadata
  if (data.metadata) {
    chunk.metadata = Array.isArray(data.metadata)
      ? new Uint16Array(data.metadata)
      : data.metadata;
  }

  // Deserialize edgeOffset
  if (data.edgeOffset) {
    chunk.edgeOffset = Array.isArray(data.edgeOffset)
      ? new Int8Array(data.edgeOffset)
      : data.edgeOffset;
  }

  // Deserialize modifiers (convert array back to Map)
  if (data.modifiers && Array.isArray(data.modifiers)) {
    chunk.modifiers = new Map(
      data.modifiers.map((entry: any) => [entry.index, entry.modifier])
    );
  }

  // Deserialize blockMetadata (convert array back to Map)
  if (data.blockMetadata && Array.isArray(data.blockMetadata)) {
    chunk.blockMetadata = new Map(
      data.blockMetadata.map((entry: any) => [entry.index, entry.metadata])
    );
  }

  if (data.compressed !== undefined) {
    chunk.compressed = data.compressed;
  }

  return chunk;
}
