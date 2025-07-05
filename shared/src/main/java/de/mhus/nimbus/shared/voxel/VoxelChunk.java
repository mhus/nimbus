package de.mhus.nimbus.shared.voxel;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A chunk of voxels representing a 3D region in the world.
 * Chunks are used to efficiently manage and process voxels in discrete regions.
 *
 * This implementation supports:
 * - Thread-safe voxel access and modification
 * - Efficient storage and retrieval
 * - Chunk-level operations like generation and serialization
 * - Neighbor chunk awareness
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"voxels", "lock"})
public class VoxelChunk implements Serializable, Iterable<Voxel> {

    @Serial
    private static final long serialVersionUID = 1L;

    // Standard chunk dimensions (can be configured)
    public static final int CHUNK_SIZE_X = 16;
    public static final int CHUNK_SIZE_Y = 16;
    public static final int CHUNK_SIZE_Z = 16;
    public static final int CHUNK_VOLUME = CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z;

    // Chunk coordinates in world space
    @EqualsAndHashCode.Include
    private int chunkX;

    @EqualsAndHashCode.Include
    private int chunkY;

    @EqualsAndHashCode.Include
    private int chunkZ;

    // World coordinates of the chunk's origin (bottom-left-front corner)
    private int worldX;
    private int worldY;
    private int worldZ;

    // Voxel storage - using ConcurrentHashMap for thread safety
    private ConcurrentHashMap<String, Voxel> voxels = new ConcurrentHashMap<>();

    // Chunk state
    private boolean generated = false;
    private boolean modified = false;
    private boolean loaded = true;
    private long lastAccess = System.currentTimeMillis();
    private long creationTime = System.currentTimeMillis();

    // Thread safety
    private transient ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructor with chunk coordinates
     *
     * @param chunkX Chunk X coordinate
     * @param chunkY Chunk Y coordinate
     * @param chunkZ Chunk Z coordinate
     */
    public VoxelChunk(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.worldX = chunkX * CHUNK_SIZE_X;
        this.worldY = chunkY * CHUNK_SIZE_Y;
        this.worldZ = chunkZ * CHUNK_SIZE_Z;
        this.voxels = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Gets a voxel at the specified local chunk coordinates
     *
     * @param localX Local X coordinate within chunk (0-15)
     * @param localY Local Y coordinate within chunk (0-15)
     * @param localZ Local Z coordinate within chunk (0-15)
     * @return The voxel at the position, or null if empty
     */
    public Voxel getVoxel(int localX, int localY, int localZ) {
        if (!isValidLocalCoordinate(localX, localY, localZ)) {
            return null;
        }

        lock.readLock().lock();
        try {
            lastAccess = System.currentTimeMillis();
            return voxels.get(createKey(localX, localY, localZ));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets a voxel at the specified world coordinates
     *
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @return The voxel at the position, or null if not in this chunk or empty
     */
    public Voxel getVoxelAtWorldPos(int worldX, int worldY, int worldZ) {
        int localX = worldX - this.worldX;
        int localY = worldY - this.worldY;
        int localZ = worldZ - this.worldZ;

        return getVoxel(localX, localY, localZ);
    }

    /**
     * Sets a voxel at the specified local chunk coordinates
     *
     * @param localX Local X coordinate within chunk (0-15)
     * @param localY Local Y coordinate within chunk (0-15)
     * @param localZ Local Z coordinate within chunk (0-15)
     * @param voxel  The voxel to set, or null to remove
     */
    public void setVoxel(int localX, int localY, int localZ, Voxel voxel) {
        if (!isValidLocalCoordinate(localX, localY, localZ)) {
            return;
        }

        lock.writeLock().lock();
        try {
            String key = createKey(localX, localY, localZ);

            if (voxel == null) {
                voxels.remove(key);
            } else {
                // Update voxel position to match chunk coordinates
                voxel.setX(worldX + localX);
                voxel.setY(worldY + localY);
                voxel.setZ(worldZ + localZ);
                voxel.touch();
                voxels.put(key, voxel);
            }

            this.modified = true;
            this.lastAccess = System.currentTimeMillis();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sets a voxel at the specified world coordinates
     *
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @param voxel  The voxel to set, or null to remove
     */
    public void setVoxelAtWorldPos(int worldX, int worldY, int worldZ, Voxel voxel) {
        int localX = worldX - this.worldX;
        int localY = worldY - this.worldY;
        int localZ = worldZ - this.worldZ;

        setVoxel(localX, localY, localZ, voxel);
    }

    /**
     * Checks if a position contains a non-air voxel
     *
     * @param localX Local X coordinate
     * @param localY Local Y coordinate
     * @param localZ Local Z coordinate
     * @return true if there's a solid voxel at the position
     */
    public boolean hasSolidVoxel(int localX, int localY, int localZ) {
        Voxel voxel = getVoxel(localX, localY, localZ);
        return voxel != null && voxel.isSolid();
    }

    /**
     * Gets all voxels in this chunk
     *
     * @return Collection of all voxels
     */
    public Collection<Voxel> getAllVoxels() {
        lock.readLock().lock();
        try {
            return voxels.values();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the number of voxels in this chunk
     *
     * @return Number of voxels
     */
    public int getVoxelCount() {
        lock.readLock().lock();
        try {
            return voxels.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Checks if this chunk is empty (contains no voxels)
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return getVoxelCount() == 0;
    }

    /**
     * Clears all voxels from this chunk
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            voxels.clear();
            this.modified = true;
            this.lastAccess = System.currentTimeMillis();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Fills the entire chunk with the specified voxel type
     *
     * @param voxelType The type of voxel to fill with
     */
    public void fill(VoxelType voxelType) {
        lock.writeLock().lock();
        try {
            voxels.clear();

            for (int x = 0; x < CHUNK_SIZE_X; x++) {
                for (int y = 0; y < CHUNK_SIZE_Y; y++) {
                    for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                        if (voxelType != VoxelType.AIR) {
                            Voxel voxel = new Voxel(worldX + x, worldY + y, worldZ + z, voxelType);
                            voxels.put(createKey(x, y, z), voxel);
                        }
                    }
                }
            }

            this.modified = true;
            this.generated = true;
            this.lastAccess = System.currentTimeMillis();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the chunk coordinates as a Vector3i
     *
     * @return Chunk coordinates
     */
    public Vector3i getChunkCoordinates() {
        return new Vector3i(chunkX, chunkY, chunkZ);
    }

    /**
     * Gets the world coordinates of the chunk origin
     *
     * @return World coordinates of chunk origin
     */
    public Vector3i getWorldCoordinates() {
        return new Vector3i(worldX, worldY, worldZ);
    }

    /**
     * Converts world coordinates to chunk coordinates
     *
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @return Chunk coordinates
     */
    public static Vector3i worldToChunkCoordinates(int worldX, int worldY, int worldZ) {
        return new Vector3i(
            Math.floorDiv(worldX, CHUNK_SIZE_X),
            Math.floorDiv(worldY, CHUNK_SIZE_Y),
            Math.floorDiv(worldZ, CHUNK_SIZE_Z)
        );
    }

    /**
     * Converts world coordinates to local chunk coordinates
     *
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @return Local coordinates within chunk
     */
    public static Vector3i worldToLocalCoordinates(int worldX, int worldY, int worldZ) {
        return new Vector3i(
            Math.floorMod(worldX, CHUNK_SIZE_X),
            Math.floorMod(worldY, CHUNK_SIZE_Y),
            Math.floorMod(worldZ, CHUNK_SIZE_Z)
        );
    }

    /**
     * Validates local coordinates
     */
    private boolean isValidLocalCoordinate(int x, int y, int z) {
        return x >= 0 && x < CHUNK_SIZE_X &&
               y >= 0 && y < CHUNK_SIZE_Y &&
               z >= 0 && z < CHUNK_SIZE_Z;
    }

    /**
     * Creates a unique key for voxel storage
     */
    private String createKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    /**
     * Marks this chunk as needing regeneration
     */
    public void markForRegeneration() {
        this.generated = false;
        this.modified = true;
    }

    /**
     * Marks this chunk as saved (clears modified flag)
     */
    public void markAsSaved() {
        this.modified = false;
    }

    /**
     * Iterator implementation for iterating over all voxels
     */
    @Override
    public Iterator<Voxel> iterator() {
        return getAllVoxels().iterator();
    }

    /**
     * Updates the last access time
     */
    public void touch() {
        this.lastAccess = System.currentTimeMillis();
    }

    /**
     * Gets the age of this chunk in milliseconds
     *
     * @return Age in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - creationTime;
    }

    /**
     * Gets the time since last access in milliseconds
     *
     * @return Time since last access in milliseconds
     */
    public long getTimeSinceLastAccess() {
        return System.currentTimeMillis() - lastAccess;
    }

    /**
     * Reinitializes transient fields after deserialization
     */
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.lock = new ReentrantReadWriteLock();
    }
}
