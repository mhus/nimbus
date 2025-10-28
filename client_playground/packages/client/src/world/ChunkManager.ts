/**
 * Chunk Manager (migrated from tmp/voxelsrv/src/lib/gameplay/world.ts)
 */

import type { WebSocketClient } from '../network/WebSocketClient';
import type { Scene } from '@babylonjs/core';
import { ChunkRenderer } from '../rendering/ChunkRenderer';
import type { TextureAtlas } from '../rendering/TextureAtlas';
import type { ClientRegistry } from '../registry/ClientRegistry';
import type { ChunkData } from '@nimbus-client/core';
import type { MaterialManager } from '../rendering/MaterialManager';
import { SpriteManagerRegistry } from '../rendering/SpriteManagerRegistry';
import type { WindManager } from '../wind/WindManager';

/**
 * Manages chunk loading and rendering
 */
export class ChunkManager {
  private socket: WebSocketClient;
  private scene: Scene;
  private chunks: Map<string, ChunkData> = new Map();
  private chunkMeshes: Map<string, any> = new Map();
  private renderer: ChunkRenderer;
  private spriteManagerRegistry: SpriteManagerRegistry;
  private chunkSize = 32;
  private wireframeEnabled = false;

  // Dynamic loading settings - browser-specific values
  private renderDistance = this.getBrowserSpecificRenderDistance();
  private unloadDistance = this.getBrowserSpecificUnloadDistance();
  private lastPlayerChunk: { x: number, z: number } = { x: 0, z: 0 };
  private updateInterval = 1000; // Check for new chunks every 1 second
  private lastUpdateTime = 0;

  // Debounce system for re-rendering chunks (prevents multiple render calls)
  private pendingRenderTimeouts: Map<string, NodeJS.Timeout> = new Map();
  private renderDebounceMs = 100; // Wait 100ms before re-rendering

  /**
   * Detect if browser is Safari
   */
  private isSafari(): boolean {
    const ua = navigator.userAgent.toLowerCase();
    return ua.includes('safari') && !ua.includes('chrome') && !ua.includes('chromium');
  }

  /**
   * Get browser-specific render distance
   * Safari: 3, Chrome: 1
   */
  private getBrowserSpecificRenderDistance(): number {
    return this.isSafari() ? 3 : 1; //TODO 2
  }

  /**
   * Get browser-specific unload distance
   * Safari: 4, Chrome: 2
   */
  private getBrowserSpecificUnloadDistance(): number {
    return this.isSafari() ? 4 : 3;
  }

  constructor(socket: WebSocketClient, scene: Scene, atlas: TextureAtlas, registry: ClientRegistry, materialManager: MaterialManager) {
    this.socket = socket;
    this.scene = scene;

    // Create SpriteManagerRegistry for SPRITE blocks
    this.spriteManagerRegistry = new SpriteManagerRegistry(scene, atlas);

    this.renderer = new ChunkRenderer(scene, atlas, registry, materialManager, this.spriteManagerRegistry);

    // Log browser-specific settings
    console.log(`[ChunkManager] Browser detected: ${this.isSafari() ? 'Safari' : 'Chrome/Other'}`);
    console.log(`[ChunkManager] Render distance: ${this.renderDistance}, Unload distance: ${this.unloadDistance}`);

    // Listen for chunk data from server
    this.socket.on('chunk_data', (data) => {
      this.onChunkData(data);
    });

    this.socket.on('WorldChunkLoad', (data) => {
      this.onChunkLoad(data);
    });

    // Start update loop for dynamic chunk loading
    this.startUpdateLoop();
  }

  /**
   * Request chunks around position
   */
  requestChunksAround(x: number, z: number, radius: number = 3): void {
    const chunkX = Math.floor(x / this.chunkSize);
    const chunkZ = Math.floor(z / this.chunkSize);

    console.log(`[ChunkManager] Requesting chunks around ${chunkX},${chunkZ} (radius ${radius})`);

    for (let cx = chunkX - radius; cx <= chunkX + radius; cx++) {
      for (let cz = chunkZ - radius; cz <= chunkZ + radius; cz++) {
        this.requestChunk(cx, cz);
      }
    }
  }

  /**
   * Request single chunk
   */
  private requestChunk(chunkX: number, chunkZ: number): void {
    const key = this.getChunkKey(chunkX, chunkZ);

    // Don't request if already loaded
    if (this.chunks.has(key)) {
      return;
    }

    console.log(`[ChunkManager] Requesting chunk ${chunkX},${chunkZ}`);
    this.socket.send('request_chunk', { chunkX, chunkZ });
  }

  /**
   * Force reload a chunk from server (even if already loaded)
   * Used by BlockEditor to revert changes
   */
  reloadChunk(chunkX: number, chunkZ: number): void {
    console.log(`[ChunkManager] Force reloading chunk ${chunkX},${chunkZ}`);

    // Remove from local cache so it will be re-requested
    const key = this.getChunkKey(chunkX, chunkZ);
    this.chunks.delete(key);

    // Request from server
    console.log(`[ChunkManager] Requesting fresh chunk data from server: ${chunkX},${chunkZ}`);
    this.socket.send('request_chunk', { chunkX, chunkZ });
  }

  /**
   * Handle chunk data from server (simple JSON format)
   */
  private onChunkData(data: any): void {
    const { chunkX, chunkZ, data: chunkData, height, metadata, edgeOffset, modifiers } = data;

    if (chunkX === undefined || chunkZ === undefined || !chunkData) {
      console.warn('[ChunkManager] Invalid chunk data received');
      return;
    }

    console.log(`[ChunkManager] Received chunk ${chunkX},${chunkZ} (${chunkData.length} blocks)`);

    // DEBUG: Check for modifiers
    if (modifiers && modifiers.length > 0) {
      console.log(`[ChunkManager] 🟢 Received ${modifiers.length} modifiers in chunk ${chunkX},${chunkZ}`);
      modifiers.forEach((entry: any) => {
        console.log(`[ChunkManager] 🟢 Modifier at index ${entry.index}:`, entry.modifier);
      });
    }

    // DEBUG: Check for block 94
    const block94Count = chunkData.filter((id: number) => id === 94).length;
    if (block94Count > 0) {
      console.log(`[ChunkManager] 🔵 BLOCK 94 - Received chunk ${chunkX},${chunkZ} with ${block94Count} blocks of type 94`);

      // Find positions
      for (let i = 0; i < chunkData.length; i++) {
        if (chunkData[i] === 94) {
          // Correct index formula: index = x + y * 32 + z * 32 * 256
          const localX = i % 32;
          const localY = Math.floor((i / 32) % 256);
          const localZ = Math.floor(i / (32 * 256));
          const worldX = chunkX * 32 + localX;
          const worldZ = chunkZ * 32 + localZ;
          console.log(`[ChunkManager] 🔵 BLOCK 94 - Found at world pos (${worldX}, ${localY}, ${worldZ}), index ${i}`);

          // Check if there's a modifier for this block 94
          if (modifiers) {
            const modEntry = modifiers.find((e: any) => e.index === i);
            if (modEntry) {
              console.log(`[ChunkManager] 🔵 BLOCK 94 - Has modifier:`, modEntry.modifier);
            } else {
              console.log(`[ChunkManager] 🔵 BLOCK 94 - No modifier found`);
            }
          }
        }
      }
    }

    // Deserialize modifiers (convert array back to Map)
    let modifiersMap: Map<number, any> | undefined = undefined;
    if (modifiers && Array.isArray(modifiers) && modifiers.length > 0) {
      modifiersMap = new Map(
        modifiers.map((entry: any) => [entry.index, entry.modifier])
      );
      console.log(`[ChunkManager] Deserialized ${modifiersMap.size} modifiers for chunk ${chunkX},${chunkZ}`);
    }

    const chunk: ChunkData = {
      chunkX,
      chunkZ,
      data: Array.isArray(chunkData) ? new Uint16Array(chunkData) : chunkData,
      height: height || 256,
      metadata: metadata ? (Array.isArray(metadata) ? new Uint16Array(metadata) : metadata) : undefined,
      edgeOffset: edgeOffset ? (Array.isArray(edgeOffset) ? new Int8Array(edgeOffset) : edgeOffset) : undefined,
      modifiers: modifiersMap,
    };

    const key = this.getChunkKey(chunkX, chunkZ);
    this.chunks.set(key, chunk);

    // DEBUG: Verify block 94 is in the stored chunk
    if (block94Count > 0) {
      const verifyCount = Array.from(chunk.data).filter(id => id === 94).length;
      console.log(`[ChunkManager] 🔵 BLOCK 94 - Stored chunk has ${verifyCount} blocks of type 94`);
    }

    // Render chunk
    this.renderChunk(chunk);
  }

  /**
   * Handle chunk load (protobuf format from old server)
   */
  private onChunkLoad(data: any): void {
    const { x, z, data: chunkData, height, compressed } = data;

    console.log(`[ChunkManager] Loading chunk ${x},${z} (compressed: ${compressed})`);

    // TODO: Handle decompression if needed
    const chunk: ChunkData = {
      chunkX: x,
      chunkZ: z,
      data: chunkData,
      height: height || 256,
    };

    const key = this.getChunkKey(x, z);
    this.chunks.set(key, chunk);

    // Render chunk
    this.renderChunk(chunk);
  }

  /**
   * Schedule chunk render with debounce (prevents multiple simultaneous renders)
   */
  private scheduleChunkRender(chunk: ChunkData): void {
    const key = this.getChunkKey(chunk.chunkX, chunk.chunkZ);

    // Clear existing timeout for this chunk
    const existingTimeout = this.pendingRenderTimeouts.get(key);
    if (existingTimeout) {
      clearTimeout(existingTimeout);
      console.log(`[ChunkManager] Cancelled pending render for chunk ${chunk.chunkX},${chunk.chunkZ}`);
    }

    // Schedule new render after debounce delay
    const timeout = setTimeout(() => {
      this.pendingRenderTimeouts.delete(key);
      this.renderChunk(chunk);
    }, this.renderDebounceMs);

    this.pendingRenderTimeouts.set(key, timeout);
    console.log(`[ChunkManager] Scheduled render for chunk ${chunk.chunkX},${chunk.chunkZ} in ${this.renderDebounceMs}ms`);
  }

  /**
   * Render chunk immediately (internal method)
   */
  private async renderChunk(chunk: ChunkData): Promise<void> {
    console.log(`[ChunkManager] Starting to render chunk ${chunk.chunkX},${chunk.chunkZ}`);

    const key = this.getChunkKey(chunk.chunkX, chunk.chunkZ);

    // Keep reference to old mesh but don't dispose yet
    const oldMesh = this.chunkMeshes.get(key);

    // Dispose old sprites BEFORE creating new mesh (so new sprites can register)
    if (oldMesh) {
      this.disposeChunkSprites(oldMesh);
    }

    try {
      // Create new mesh (async - loads textures into atlas)
      console.log(`[ChunkManager] Calling createChunkMesh for ${chunk.chunkX},${chunk.chunkZ}`);
      const mesh = await this.renderer.createChunkMesh(chunk);

      // NOW dispose old mesh after new one is ready
      if (oldMesh) {
        // Dispose mesh with doNotRecurse=false to dispose ALL children and descendants
        // This ensures all child meshes (solid, transparent, transparent_wind, water, lava) are properly removed
        // Use disposeMaterialAndTextures=false to keep shared materials managed by MaterialManager
        oldMesh.dispose(false, false); // (doNotRecurse=false, disposeMaterialAndTextures=false)
        console.log(`[ChunkManager] Disposed old mesh for chunk ${chunk.chunkX},${chunk.chunkZ} with all children`);
      }

      this.chunkMeshes.set(key, mesh);

      console.log(`[ChunkManager] Rendered chunk ${chunk.chunkX},${chunk.chunkZ}`);
    } catch (error) {
      console.error(`[ChunkManager] Error rendering chunk ${chunk.chunkX},${chunk.chunkZ}:`, error);
    }
  }

  /**
   * Unload chunk
   */
  unloadChunk(chunkX: number, chunkZ: number): void {
    const key = this.getChunkKey(chunkX, chunkZ);

    const mesh = this.chunkMeshes.get(key);
    if (mesh) {
      // Dispose sprites first
      this.disposeChunkSprites(mesh);

      // Dispose mesh with all children, but keep shared materials
      mesh.dispose(false, false); // (doNotRecurse=false, disposeMaterialAndTextures=false)
      this.chunkMeshes.delete(key);
    }

    this.chunks.delete(key);
    console.log(`[ChunkManager] Unloaded chunk ${chunkX},${chunkZ}`);
  }

  /**
   * Get chunk key for map
   */
  private getChunkKey(chunkX: number, chunkZ: number): string {
    return `${chunkX},${chunkZ}`;
  }

  /**
   * Get chunk data by chunk coordinates
   * @param chunkX Chunk X coordinate
   * @param chunkZ Chunk Z coordinate
   * @returns ChunkData or undefined if chunk is not loaded
   */
  getChunk(chunkX: number, chunkZ: number): ChunkData | undefined {
    const key = this.getChunkKey(chunkX, chunkZ);
    return this.chunks.get(key);
  }

  /**
   * Get loaded chunks count
   */
  getLoadedChunksCount(): number {
    return this.chunks.size;
  }

  /**
   * Start update loop for dynamic chunk loading
   */
  private startUpdateLoop(): void {
    this.scene.onBeforeRenderObservable.add(() => {
      const currentTime = performance.now();
      if (currentTime - this.lastUpdateTime > this.updateInterval) {
        this.lastUpdateTime = currentTime;
        this.updateChunks();
      }
    });
  }

  /**
   * Update chunks based on camera position
   */
  updateChunksAroundPosition(worldX: number, worldZ: number): void {
    const chunkX = Math.floor(worldX / this.chunkSize);
    const chunkZ = Math.floor(worldZ / this.chunkSize);

    // Check if player moved to a new chunk
    if (chunkX !== this.lastPlayerChunk.x || chunkZ !== this.lastPlayerChunk.z) {
      console.log(`[ChunkManager] Player moved to chunk (${chunkX}, ${chunkZ})`);
      this.lastPlayerChunk = { x: chunkX, z: chunkZ };

      // Request new chunks
      this.requestChunksAround(worldX, worldZ, this.renderDistance);

      // Unload far chunks
      this.unloadDistantChunks(chunkX, chunkZ);
    }
  }

  /**
   * Update chunks (called periodically)
   */
  private updateChunks(): void {
    // Get camera position from scene
    const camera = this.scene.activeCamera;
    if (!camera) return;

    const position = camera.position;
    this.updateChunksAroundPosition(position.x, position.z);
  }

  /**
   * Unload chunks that are too far from player
   */
  private unloadDistantChunks(playerChunkX: number, playerChunkZ: number): void {
    const chunksToUnload: string[] = [];

    for (const [key, chunk] of this.chunks.entries()) {
      const dx = Math.abs(chunk.chunkX - playerChunkX);
      const dz = Math.abs(chunk.chunkZ - playerChunkZ);
      const distance = Math.max(dx, dz);

      if (distance > this.unloadDistance) {
        chunksToUnload.push(key);
      }
    }

    for (const key of chunksToUnload) {
      const parts = key.split(',');
      const chunkX = parseInt(parts[0]);
      const chunkZ = parseInt(parts[1]);
      this.unloadChunk(chunkX, chunkZ);
    }

    if (chunksToUnload.length > 0) {
      console.log(`[ChunkManager] Unloaded ${chunksToUnload.length} distant chunks`);
    }
  }

  /**
   * Set render distance (how many chunks to load around player)
   */
  setRenderDistance(distance: number): void {
    this.renderDistance = distance;
    console.log(`[ChunkManager] Render distance set to ${distance} chunks`);
  }

  /**
   * Set unload distance (chunks further than this will be unloaded)
   */
  setUnloadDistance(distance: number): void {
    this.unloadDistance = distance;
    console.log(`[ChunkManager] Unload distance set to ${distance} chunks`);
  }

  /**
   * Get render distance
   */
  getRenderDistance(): number {
    return this.renderDistance;
  }

  /**
   * Get current player chunk
   */
  getPlayerChunk(): { x: number, z: number } {
    return { ...this.lastPlayerChunk };
  }

  /**
   * Re-render current chunk (where player is standing)
   */
  rerenderCurrentChunk(): void {
    const camera = this.scene.activeCamera;
    if (!camera) {
      console.warn('[ChunkManager] Cannot re-render: No active camera');
      return;
    }

    const position = camera.position;
    const chunkX = Math.floor(position.x / this.chunkSize);
    const chunkZ = Math.floor(position.z / this.chunkSize);

    this.rerenderChunk(chunkX, chunkZ);
  }

  /**
   * Re-render specific chunk by coordinates
   */
  rerenderChunk(chunkX: number, chunkZ: number): void {
    const key = this.getChunkKey(chunkX, chunkZ);
    const chunk = this.chunks.get(key);

    if (!chunk) {
      console.warn(`[ChunkManager] Cannot re-render chunk ${chunkX},${chunkZ}: Not loaded`);
      return;
    }

    console.log(`[ChunkManager] Re-rendering chunk ${chunkX},${chunkZ}`);
    this.scheduleChunkRender(chunk);
  }

  /**
   * Re-render all visible chunks (within render distance)
   */
  rerenderVisibleChunks(): number {
    const camera = this.scene.activeCamera;
    if (!camera) {
      console.warn('[ChunkManager] Cannot re-render: No active camera');
      return 0;
    }

    const position = camera.position;
    const playerChunkX = Math.floor(position.x / this.chunkSize);
    const playerChunkZ = Math.floor(position.z / this.chunkSize);

    let rerenderCount = 0;

    // Re-render all chunks within render distance
    for (const [key, chunk] of this.chunks.entries()) {
      const dx = Math.abs(chunk.chunkX - playerChunkX);
      const dz = Math.abs(chunk.chunkZ - playerChunkZ);
      const distance = Math.max(dx, dz);

      if (distance <= this.renderDistance) {
        this.scheduleChunkRender(chunk);
        rerenderCount++;
      }
    }

    console.log(`[ChunkManager] Re-rendering ${rerenderCount} visible chunks`);
    return rerenderCount;
  }

  /**
   * Enable wireframe mode for all chunks
   */
  enableWireframe(): void {
    this.wireframeEnabled = true;
    this.updateWireframeMode();
    console.log('[ChunkManager] Wireframe mode enabled');
  }

  /**
   * Disable wireframe mode for all chunks
   */
  disableWireframe(): void {
    this.wireframeEnabled = false;
    this.updateWireframeMode();
    console.log('[ChunkManager] Wireframe mode disabled');
  }

  /**
   * Check if wireframe mode is enabled
   */
  isWireframeEnabled(): boolean {
    return this.wireframeEnabled;
  }

  /**
   * Update wireframe mode for all loaded chunks
   */
  private updateWireframeMode(): void {
    for (const mesh of this.chunkMeshes.values()) {
      if (mesh && mesh.material) {
        mesh.material.wireframe = this.wireframeEnabled;
      }
      // Handle child meshes (transparent, fluid)
      if (mesh.getChildren) {
        const children = mesh.getChildren();
        for (const child of children) {
          if (child.material) {
            child.material.wireframe = this.wireframeEnabled;
          }
        }
      }
    }
  }

  /**
   * Get block ID at world position
   * @param worldX World X position
   * @param worldY World Y position
   * @param worldZ World Z position
   * @returns Block ID or undefined if chunk not loaded
   */
  getBlockAt(worldX: number, worldY: number, worldZ: number): number | undefined {
    const chunkX = Math.floor(worldX / this.chunkSize);
    const chunkZ = Math.floor(worldZ / this.chunkSize);
    const key = this.getChunkKey(chunkX, chunkZ);

    const chunk = this.chunks.get(key);
    if (!chunk) {
      return undefined;
    }

    let localX = worldX - chunkX * this.chunkSize;
    let localZ = worldZ - chunkZ * this.chunkSize;

    if (localX < 0) localX += this.chunkSize;
    if (localX >= this.chunkSize) localX -= this.chunkSize;
    if (localZ < 0) localZ += this.chunkSize;
    if (localZ >= this.chunkSize) localZ -= this.chunkSize;

    const localY = worldY;

    if (localX < 0 || localX >= this.chunkSize ||
        localZ < 0 || localZ >= this.chunkSize ||
        localY < 0 || localY >= chunk.height) {
      return undefined;
    }

    const index = localX + localZ * this.chunkSize + localY * this.chunkSize * this.chunkSize;
    return chunk.data[index];
  }

  /**
   * Update a single block in a chunk and re-render the chunk
   * @param worldX World X position of the block
   * @param worldY World Y position of the block
   * @param worldZ World Z position of the block
   * @param newBlockId New block ID to set
   */
  updateBlockAt(worldX: number, worldY: number, worldZ: number, newBlockId: number): void {
    console.log(`[ChunkManager] updateBlockAt called with world position: (${worldX}, ${worldY}, ${worldZ}), newBlockId: ${newBlockId}`);

    // Calculate chunk coordinates (floor division for negative numbers)
    const chunkX = Math.floor(worldX / this.chunkSize);
    const chunkZ = Math.floor(worldZ / this.chunkSize);
    const key = this.getChunkKey(chunkX, chunkZ);

    console.log(`[ChunkManager] Calculated chunk position: (${chunkX}, ${chunkZ}), key: ${key}`);
    console.log(`[ChunkManager] Available chunks:`, Array.from(this.chunks.keys()));

    // Get the chunk
    const chunk = this.chunks.get(key);
    if (!chunk) {
      console.warn(`[ChunkManager] Cannot update block at (${worldX}, ${worldY}, ${worldZ}): Chunk not loaded (key: ${key})`);
      return;
    }

    // Calculate local block position within chunk
    // For negative world coordinates, we need to handle wrapping correctly
    // Example: worldX = -4, chunkX = -1 → localX = -4 - (-1 * 32) = -4 + 32 = 28
    // Example: worldX = 5, chunkX = 0 → localX = 5 - (0 * 32) = 5
    let localX = worldX - chunkX * this.chunkSize;
    let localZ = worldZ - chunkZ * this.chunkSize;

    // Ensure local coordinates are within [0, chunkSize)
    if (localX < 0) localX += this.chunkSize;
    if (localX >= this.chunkSize) localX -= this.chunkSize;
    if (localZ < 0) localZ += this.chunkSize;
    if (localZ >= this.chunkSize) localZ -= this.chunkSize;

    const localY = worldY;

    console.log(`[ChunkManager] Local block position: (${localX}, ${localY}, ${localZ})`);

    // Validate local coordinates
    if (localX < 0 || localX >= this.chunkSize ||
        localZ < 0 || localZ >= this.chunkSize ||
        localY < 0 || localY >= chunk.height) {
      console.error(`[ChunkManager] Invalid local coordinates: (${localX}, ${localY}, ${localZ}), chunk size: ${this.chunkSize}, height: ${chunk.height}`);
      return;
    }

    // Calculate index in chunk data array
    // IMPORTANT: Must match ChunkRenderer formula: x + y * chunkSize + z * chunkSize * height
    const index = localX + localY * this.chunkSize + localZ * this.chunkSize * chunk.height;

    console.log(`[ChunkManager] Block index in chunk data: ${index} (chunk data length: ${chunk.data.length})`);

    // Update block data
    const oldBlockId = chunk.data[index];
    chunk.data[index] = newBlockId;

    console.log(`[ChunkManager] Updated block at (${worldX}, ${worldY}, ${worldZ}): ${oldBlockId} -> ${newBlockId}`);
    console.log(`[ChunkManager] Scheduling re-render for chunk ${chunkX},${chunkZ}...`);

    // Schedule re-render with debounce
    this.scheduleChunkRender(chunk);
  }

  /**
   * Update edge offsets for a single block and re-render the chunk
   * @param worldX World X position of the block
   * @param worldY World Y position of the block
   * @param worldZ World Z position of the block
   * @param offsets Array of 24 signed bytes (8 vertices * 3 coordinates each), or null to clear
   */
  updateBlockEdgeOffsets(worldX: number, worldY: number, worldZ: number, offsets: number[] | null): void {
    console.log(`[ChunkManager] updateBlockEdgeOffsets called for (${worldX}, ${worldY}, ${worldZ})`);

    // Calculate chunk coordinates
    const chunkX = Math.floor(worldX / this.chunkSize);
    const chunkZ = Math.floor(worldZ / this.chunkSize);
    const key = this.getChunkKey(chunkX, chunkZ);

    // Get the chunk
    const chunk = this.chunks.get(key);
    if (!chunk) {
      console.warn(`[ChunkManager] Cannot update edge offsets at (${worldX}, ${worldY}, ${worldZ}): Chunk not loaded`);
      return;
    }

    // Calculate local block position
    let localX = worldX - chunkX * this.chunkSize;
    let localZ = worldZ - chunkZ * this.chunkSize;

    if (localX < 0) localX += this.chunkSize;
    if (localX >= this.chunkSize) localX -= this.chunkSize;
    if (localZ < 0) localZ += this.chunkSize;
    if (localZ >= this.chunkSize) localZ -= this.chunkSize;

    const localY = worldY;

    // Validate coordinates
    if (localX < 0 || localX >= this.chunkSize ||
        localZ < 0 || localZ >= this.chunkSize ||
        localY < 0 || localY >= chunk.height) {
      console.error(`[ChunkManager] Invalid coordinates for edge offset update`);
      return;
    }

    // Calculate block index
    const blockIndex = localX + localY * this.chunkSize + localZ * this.chunkSize * chunk.height;
    const totalBlocks = this.chunkSize * this.chunkSize * chunk.height;

    if (offsets === null || offsets.every(v => v === 0)) {
      // Clear edge offsets - set all 24 values to 0
      if (chunk.edgeOffset) {
        const offsetIndex = blockIndex * 24;
        for (let i = 0; i < 24; i++) {
          chunk.edgeOffset[offsetIndex + i] = 0;
        }
      }
      console.log(`[ChunkManager] Cleared edge offsets for block at (${worldX}, ${worldY}, ${worldZ})`);
    } else {
      // Set edge offsets
      if (offsets.length !== 24) {
        console.error(`[ChunkManager] Invalid edge offsets array length: ${offsets.length} (expected 24)`);
        return;
      }

      // Initialize edgeOffset array if it doesn't exist
      if (!chunk.edgeOffset) {
        chunk.edgeOffset = new Int8Array(totalBlocks * 24);
      }

      // Set the 24 offset values for this block
      const offsetIndex = blockIndex * 24;
      for (let i = 0; i < 24; i++) {
        chunk.edgeOffset[offsetIndex + i] = Math.max(-127, Math.min(128, offsets[i]));
      }

      console.log(`[ChunkManager] Updated edge offsets for block at (${worldX}, ${worldY}, ${worldZ})`);
    }

    // Schedule re-render with debounce
    this.scheduleChunkRender(chunk);
  }

  /**
   * Get edge offsets for a block
   * @returns Array of 24 signed bytes, or null if no offsets are set
   */
  getBlockEdgeOffsets(worldX: number, worldY: number, worldZ: number): number[] | null {
    // Calculate chunk coordinates
    const chunkX = Math.floor(worldX / this.chunkSize);
    const chunkZ = Math.floor(worldZ / this.chunkSize);
    const key = this.getChunkKey(chunkX, chunkZ);

    // Get the chunk
    const chunk = this.chunks.get(key);
    if (!chunk || !chunk.edgeOffset) {
      return null;
    }

    // Calculate local block position
    let localX = worldX - chunkX * this.chunkSize;
    let localZ = worldZ - chunkZ * this.chunkSize;

    if (localX < 0) localX += this.chunkSize;
    if (localX >= this.chunkSize) localX -= this.chunkSize;
    if (localZ < 0) localZ += this.chunkSize;
    if (localZ >= this.chunkSize) localZ -= this.chunkSize;

    const localY = worldY;

    // Calculate block index
    const blockIndex = localX + localY * this.chunkSize + localZ * this.chunkSize * chunk.height;
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
   * Update modifier for a single block and re-render the chunk
   * @param worldX World X position of the block
   * @param worldY World Y position of the block
   * @param worldZ World Z position of the block
   * @param modifier BlockModifier object, or null to clear
   */
  updateBlockModifier(worldX: number, worldY: number, worldZ: number, modifier: any | null): void {
    console.log(`[ChunkManager] updateBlockModifier called for (${worldX}, ${worldY}, ${worldZ})`);

    // Calculate chunk coordinates
    const chunkX = Math.floor(worldX / this.chunkSize);
    const chunkZ = Math.floor(worldZ / this.chunkSize);
    const key = this.getChunkKey(chunkX, chunkZ);

    // Get the chunk
    const chunk = this.chunks.get(key);
    if (!chunk) {
      console.warn(`[ChunkManager] Cannot update modifier at (${worldX}, ${worldY}, ${worldZ}): Chunk not loaded`);
      return;
    }

    // Calculate local block position
    let localX = worldX - chunkX * this.chunkSize;
    let localZ = worldZ - chunkZ * this.chunkSize;

    if (localX < 0) localX += this.chunkSize;
    if (localX >= this.chunkSize) localX -= this.chunkSize;
    if (localZ < 0) localZ += this.chunkSize;
    if (localZ >= this.chunkSize) localZ -= this.chunkSize;

    const localY = worldY;

    // Validate coordinates
    if (localX < 0 || localX >= this.chunkSize ||
        localZ < 0 || localZ >= this.chunkSize ||
        localY < 0 || localY >= chunk.height) {
      console.error(`[ChunkManager] Invalid coordinates for modifier update`);
      return;
    }

    // Calculate block index
    const blockIndex = localX + localY * this.chunkSize + localZ * this.chunkSize * chunk.height;

    if (modifier === null || Object.keys(modifier).length === 0) {
      // Clear modifier
      if (chunk.modifiers) {
        if (chunk.modifiers instanceof Map) {
          chunk.modifiers.delete(blockIndex);
        } else {
          delete chunk.modifiers[blockIndex];
        }
      }
      console.log(`[ChunkManager] Cleared modifier for block at (${worldX}, ${worldY}, ${worldZ})`);
    } else {
      // Set modifier
      if (!chunk.modifiers) {
        chunk.modifiers = new Map();
      }

      // Convert Record to Map if necessary
      if (!(chunk.modifiers instanceof Map)) {
        chunk.modifiers = new Map(Object.entries(chunk.modifiers).map(([k, v]) => [parseInt(k), v]));
      }

      chunk.modifiers.set(blockIndex, modifier);
      console.log(`[ChunkManager] Updated modifier for block at (${worldX}, ${worldY}, ${worldZ}):`, modifier);
    }

    // Schedule re-render with debounce (prevents multiple renders for rapid changes)
    this.scheduleChunkRender(chunk);
  }

  /**
   * Get modifier for a block
   * @returns BlockModifier object or null if no modifier is set
   */
  getBlockModifier(worldX: number, worldY: number, worldZ: number): any | null {
    // Calculate chunk coordinates
    const chunkX = Math.floor(worldX / this.chunkSize);
    const chunkZ = Math.floor(worldZ / this.chunkSize);
    const key = this.getChunkKey(chunkX, chunkZ);

    // Get the chunk
    const chunk = this.chunks.get(key);
    if (!chunk || !chunk.modifiers) {
      return null;
    }

    // Calculate local block position
    let localX = worldX - chunkX * this.chunkSize;
    let localZ = worldZ - chunkZ * this.chunkSize;

    if (localX < 0) localX += this.chunkSize;
    if (localX >= this.chunkSize) localX -= this.chunkSize;
    if (localZ < 0) localZ += this.chunkSize;
    if (localZ >= this.chunkSize) localZ -= this.chunkSize;

    const localY = worldY;

    // Calculate block index
    const blockIndex = localX + localY * this.chunkSize + localZ * this.chunkSize * chunk.height;

    // Get modifier
    if (chunk.modifiers instanceof Map) {
      return chunk.modifiers.get(blockIndex) || null;
    } else {
      return chunk.modifiers[blockIndex] || null;
    }
  }

  /**
   * Update metadata for a single block
   * @param worldX World X position of the block
   * @param worldY World Y position of the block
   * @param worldZ World Z position of the block
   * @param metadata BlockMetadata object, or null to clear
   */
  updateBlockMetadata(worldX: number, worldY: number, worldZ: number, metadata: any | null): void {
    console.log(`[ChunkManager] updateBlockMetadata called for (${worldX}, ${worldY}, ${worldZ})`);

    // Calculate chunk coordinates
    const chunkX = Math.floor(worldX / this.chunkSize);
    const chunkZ = Math.floor(worldZ / this.chunkSize);
    const key = this.getChunkKey(chunkX, chunkZ);

    // Get the chunk
    const chunk = this.chunks.get(key);
    if (!chunk) {
      console.warn(`[ChunkManager] Cannot update metadata at (${worldX}, ${worldY}, ${worldZ}): Chunk not loaded`);
      return;
    }

    // Calculate local block position
    let localX = worldX - chunkX * this.chunkSize;
    let localZ = worldZ - chunkZ * this.chunkSize;

    if (localX < 0) localX += this.chunkSize;
    if (localX >= this.chunkSize) localX -= this.chunkSize;
    if (localZ < 0) localZ += this.chunkSize;
    if (localZ >= this.chunkSize) localZ -= this.chunkSize;

    const localY = worldY;

    // Validate coordinates
    if (localX < 0 || localX >= this.chunkSize ||
        localZ < 0 || localZ >= this.chunkSize ||
        localY < 0 || localY >= chunk.height) {
      console.error(`[ChunkManager] Invalid coordinates for metadata update`);
      return;
    }

    // Calculate block index
    const blockIndex = localX + localY * this.chunkSize + localZ * this.chunkSize * chunk.height;

    if (metadata === null || Object.keys(metadata).length === 0) {
      // Clear metadata
      if (chunk.blockMetadata) {
        if (chunk.blockMetadata instanceof Map) {
          chunk.blockMetadata.delete(blockIndex);
        } else {
          delete chunk.blockMetadata[blockIndex];
        }
      }
      console.log(`[ChunkManager] Cleared metadata for block at (${worldX}, ${worldY}, ${worldZ})`);
    } else {
      // Set metadata
      if (!chunk.blockMetadata) {
        chunk.blockMetadata = new Map();
      }

      // Convert Record to Map if necessary
      if (!(chunk.blockMetadata instanceof Map)) {
        chunk.blockMetadata = new Map(Object.entries(chunk.blockMetadata).map(([k, v]) => [parseInt(k), v]));
      }

      chunk.blockMetadata.set(blockIndex, metadata);
      console.log(`[ChunkManager] Updated metadata for block at (${worldX}, ${worldY}, ${worldZ}):`, metadata);
    }

    // Schedule re-render with debounce
    this.scheduleChunkRender(chunk);
  }

  /**
   * Get metadata for a block
   * @returns BlockMetadata object or null if no metadata is set
   */
  getBlockMetadata(worldX: number, worldY: number, worldZ: number): any | null {
    // Calculate chunk coordinates
    const chunkX = Math.floor(worldX / this.chunkSize);
    const chunkZ = Math.floor(worldZ / this.chunkSize);
    const key = this.getChunkKey(chunkX, chunkZ);

    // Get the chunk
    const chunk = this.chunks.get(key);
    if (!chunk || !chunk.blockMetadata) {
      return null;
    }

    // Calculate local block position
    let localX = worldX - chunkX * this.chunkSize;
    let localZ = worldZ - chunkZ * this.chunkSize;

    if (localX < 0) localX += this.chunkSize;
    if (localX >= this.chunkSize) localX -= this.chunkSize;
    if (localZ < 0) localZ += this.chunkSize;
    if (localZ >= this.chunkSize) localZ -= this.chunkSize;

    const localY = worldY;

    // Calculate block index
    const blockIndex = localX + localY * this.chunkSize + localZ * this.chunkSize * chunk.height;

    // Get metadata
    if (chunk.blockMetadata instanceof Map) {
      return chunk.blockMetadata.get(blockIndex) || null;
    } else {
      return chunk.blockMetadata[blockIndex] || null;
    }
  }

  /**
   * Dispose sprites associated with a chunk mesh
   */
  private disposeChunkSprites(mesh: any): void {
    if (mesh && mesh.metadata && mesh.metadata.sprites) {
      const sprites = mesh.metadata.sprites;
      for (const sprite of sprites) {
        sprite.dispose();
      }
      console.log(`[ChunkManager] Disposed ${sprites.length} sprites`);
      mesh.metadata.sprites = [];

      // Clean up disposed sprites from animation system
      this.spriteManagerRegistry.removeDisposedSprites();
    }
  }

  /**
   * Set WindManager for sprite wind animation
   */
  setWindManager(windManager: WindManager): void {
    this.spriteManagerRegistry.setWindManager(windManager);
  }

  /**
   * Dispose all chunks
   */
  dispose(): void {
    for (const mesh of this.chunkMeshes.values()) {
      // Dispose sprites first
      this.disposeChunkSprites(mesh);

      // Dispose mesh with all children, but keep shared materials
      mesh.dispose(false, false); // (doNotRecurse=false, disposeMaterialAndTextures=false)
    }
    this.chunkMeshes.clear();
    this.chunks.clear();
  }
}
