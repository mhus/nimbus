package de.mhus.nimbus.voxelworld.service;

import de.mhus.nimbus.shared.voxel.Voxel;
import de.mhus.nimbus.shared.voxel.VoxelChunk;
import de.mhus.nimbus.voxelworld.entity.WorldVoxel;
import de.mhus.nimbus.voxelworld.repository.WorldVoxelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing voxels in the world database
 */
@Service
@Transactional
public class VoxelWorldService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoxelWorldService.class);

    private final WorldVoxelRepository worldVoxelRepository;

    @Autowired
    public VoxelWorldService(WorldVoxelRepository worldVoxelRepository) {
        this.worldVoxelRepository = worldVoxelRepository;
    }

    /**
     * Saves a voxel to the database
     *
     * @param worldId The world identifier
     * @param voxel   The voxel to save
     * @return The saved WorldVoxel entity
     */
    public WorldVoxel saveVoxel(String worldId, Voxel voxel) {
        LOGGER.debug("Saving voxel at position ({}, {}, {}) in world {}",
                    voxel.getX(), voxel.getY(), voxel.getZ(), worldId);

        // Check if voxel already exists at this position
        Optional<WorldVoxel> existing = worldVoxelRepository.findByWorldIdAndXAndYAndZ(
                worldId, voxel.getX(), voxel.getY(), voxel.getZ());

        WorldVoxel worldVoxel;
        if (existing.isPresent()) {
            // Update existing voxel
            worldVoxel = existing.get();
            worldVoxel.updateVoxel(voxel);
            LOGGER.debug("Updated existing voxel at position ({}, {}, {}) in world {}",
                        voxel.getX(), voxel.getY(), voxel.getZ(), worldId);
        } else {
            // Create new voxel
            worldVoxel = new WorldVoxel(worldId, voxel);
            LOGGER.debug("Created new voxel at position ({}, {}, {}) in world {}",
                        voxel.getX(), voxel.getY(), voxel.getZ(), worldId);
        }

        return worldVoxelRepository.save(worldVoxel);
    }

    /**
     * Gets a voxel from the database
     *
     * @param worldId The world identifier
     * @param x       X coordinate
     * @param y       Y coordinate
     * @param z       Z coordinate
     * @return The voxel if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<Voxel> getVoxel(String worldId, int x, int y, int z) {
        LOGGER.debug("Getting voxel at position ({}, {}, {}) in world {}", x, y, z, worldId);

        return worldVoxelRepository.findByWorldIdAndXAndYAndZ(worldId, x, y, z)
                .map(WorldVoxel::getVoxel);
    }

    /**
     * Deletes a voxel from the database
     *
     * @param worldId The world identifier
     * @param x       X coordinate
     * @param y       Y coordinate
     * @param z       Z coordinate
     * @return true if a voxel was deleted, false if none existed
     */
    public boolean deleteVoxel(String worldId, int x, int y, int z) {
        LOGGER.debug("Deleting voxel at position ({}, {}, {}) in world {}", x, y, z, worldId);

        if (worldVoxelRepository.existsByWorldIdAndXAndYAndZ(worldId, x, y, z)) {
            worldVoxelRepository.deleteByWorldIdAndXAndYAndZ(worldId, x, y, z);
            LOGGER.info("Deleted voxel at position ({}, {}, {}) in world {}", x, y, z, worldId);
            return true;
        }

        LOGGER.debug("No voxel found to delete at position ({}, {}, {}) in world {}", x, y, z, worldId);
        return false;
    }

    /**
     * Loads a chunk of voxels from the database
     *
     * @param worldId The world identifier
     * @param chunkX  Chunk X coordinate
     * @param chunkY  Chunk Y coordinate
     * @param chunkZ  Chunk Z coordinate
     * @return A VoxelChunk containing all voxels in the chunk
     */
    @Transactional(readOnly = true)
    public VoxelChunk loadChunk(String worldId, int chunkX, int chunkY, int chunkZ) {
        LOGGER.debug("Loading chunk ({}, {}, {}) in world {}", chunkX, chunkY, chunkZ, worldId);

        VoxelChunk chunk = new VoxelChunk(chunkX, chunkY, chunkZ);

        List<WorldVoxel> voxels = worldVoxelRepository.findByWorldIdAndChunkXAndChunkYAndChunkZ(
                worldId, chunkX, chunkY, chunkZ);

        for (WorldVoxel worldVoxel : voxels) {
            Voxel voxel = worldVoxel.getVoxel();
            if (voxel != null) {
                // Convert world coordinates to local chunk coordinates
                int localX = voxel.getX() - (chunkX * VoxelChunk.CHUNK_SIZE_X);
                int localY = voxel.getY() - (chunkY * VoxelChunk.CHUNK_SIZE_Y);
                int localZ = voxel.getZ() - (chunkZ * VoxelChunk.CHUNK_SIZE_Z);

                chunk.setVoxel(localX, localY, localZ, voxel);
            }
        }

        LOGGER.debug("Loaded {} voxels for chunk ({}, {}, {}) in world {}",
                    voxels.size(), chunkX, chunkY, chunkZ, worldId);

        return chunk;
    }

    /**
     * Loads a complete chunk with all voxels from the database including detailed metadata
     * This method provides comprehensive chunk loading with performance optimizations
     *
     * @param worldId The world identifier
     * @param chunkX  Chunk X coordinate
     * @param chunkY  Chunk Y coordinate
     * @param chunkZ  Chunk Z coordinate
     * @param includeEmpty Whether to include empty positions (air voxels) in the result
     * @return A VoxelChunk containing all voxels in the chunk with metadata
     */
    @Transactional(readOnly = true)
    public VoxelChunk loadFullChunk(String worldId, int chunkX, int chunkY, int chunkZ, boolean includeEmpty) {
        LOGGER.info("Loading full chunk ({}, {}, {}) in world {} (includeEmpty: {})",
                   chunkX, chunkY, chunkZ, worldId, includeEmpty);

        long startTime = System.currentTimeMillis();

        // Create new chunk
        VoxelChunk chunk = new VoxelChunk(chunkX, chunkY, chunkZ);
        chunk.setGenerated(true);
        chunk.setLoaded(true);

        // Load all voxels for this chunk from database
        List<WorldVoxel> worldVoxels = worldVoxelRepository.findByWorldIdAndChunkXAndChunkYAndChunkZ(
                worldId, chunkX, chunkY, chunkZ);

        LOGGER.debug("Found {} voxels in database for chunk ({}, {}, {})",
                    worldVoxels.size(), chunkX, chunkY, chunkZ);

        // Convert and add voxels to chunk
        int voxelCount = 0;
        for (WorldVoxel worldVoxel : worldVoxels) {
            try {
                Voxel voxel = worldVoxel.getVoxel();
                if (voxel != null) {
                    // Calculate local coordinates within chunk
                    int localX = voxel.getX() - (chunkX * VoxelChunk.CHUNK_SIZE_X);
                    int localY = voxel.getY() - (chunkY * VoxelChunk.CHUNK_SIZE_Y);
                    int localZ = voxel.getZ() - (chunkZ * VoxelChunk.CHUNK_SIZE_Z);

                    // Validate coordinates are within chunk bounds
                    if (localX >= 0 && localX < VoxelChunk.CHUNK_SIZE_X &&
                        localY >= 0 && localY < VoxelChunk.CHUNK_SIZE_Y &&
                        localZ >= 0 && localZ < VoxelChunk.CHUNK_SIZE_Z) {

                        // Only add non-air voxels unless includeEmpty is true
                        if (includeEmpty || !voxel.isAir()) {
                            chunk.setVoxel(localX, localY, localZ, voxel);
                            voxelCount++;
                        }
                    } else {
                        LOGGER.warn("Voxel at world position ({}, {}, {}) has invalid local coordinates ({}, {}, {}) for chunk ({}, {}, {})",
                                   voxel.getX(), voxel.getY(), voxel.getZ(), localX, localY, localZ, chunkX, chunkY, chunkZ);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to process voxel from WorldVoxel ID {}: {}", worldVoxel.getId(), e.getMessage(), e);
            }
        }

        // Mark chunk as modified if it contains voxels
        if (voxelCount > 0) {
            chunk.setModified(false); // Just loaded, not modified
        }

        long loadTime = System.currentTimeMillis() - startTime;

        LOGGER.info("Successfully loaded full chunk ({}, {}, {}) in world {} with {} voxels in {}ms",
                   chunkX, chunkY, chunkZ, worldId, voxelCount, loadTime);

        return chunk;
    }

    /**
     * Loads a complete chunk with all voxels (convenience method with includeEmpty=false)
     *
     * @param worldId The world identifier
     * @param chunkX  Chunk X coordinate
     * @param chunkY  Chunk Y coordinate
     * @param chunkZ  Chunk Z coordinate
     * @return A VoxelChunk containing all non-air voxels in the chunk
     */
    @Transactional(readOnly = true)
    public VoxelChunk loadFullChunk(String worldId, int chunkX, int chunkY, int chunkZ) {
        return loadFullChunk(worldId, chunkX, chunkY, chunkZ, false);
    }

    /**
     * Saves a complete chunk to the database
     *
     * @param worldId The world identifier
     * @param chunk   The chunk to save
     * @return Number of voxels saved
     */
    public int saveChunk(String worldId, VoxelChunk chunk) {
        LOGGER.debug("Saving chunk ({}, {}, {}) in world {}",
                    chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(), worldId);

        int savedCount = 0;

        for (Voxel voxel : chunk) {
            if (voxel != null && !voxel.isAir()) {
                saveVoxel(worldId, voxel);
                savedCount++;
            }
        }

        LOGGER.info("Saved {} voxels for chunk ({}, {}, {}) in world {}",
                   savedCount, chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(), worldId);

        return savedCount;
    }

    /**
     * Gets voxels in a coordinate range
     *
     * @param worldId The world identifier
     * @param minX    Minimum X coordinate
     * @param maxX    Maximum X coordinate
     * @param minY    Minimum Y coordinate
     * @param maxY    Maximum Y coordinate
     * @param minZ    Minimum Z coordinate
     * @param maxZ    Maximum Z coordinate
     * @return List of voxels in the range
     */
    @Transactional(readOnly = true)
    public List<Voxel> getVoxelsInRange(String worldId, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        LOGGER.debug("Getting voxels in range ({},{},{}) to ({},{},{}) in world {}",
                    minX, minY, minZ, maxX, maxY, maxZ, worldId);

        return worldVoxelRepository.findByWorldIdAndCoordinateRange(worldId, minX, maxX, minY, maxY, minZ, maxZ)
                .stream()
                .map(WorldVoxel::getVoxel)
                .collect(Collectors.toList());
    }

    /**
     * Gets all voxels modified after a specific timestamp
     *
     * @param worldId   The world identifier
     * @param timestamp The timestamp to check against
     * @return List of modified voxels
     */
    @Transactional(readOnly = true)
    public List<Voxel> getModifiedVoxels(String worldId, LocalDateTime timestamp) {
        LOGGER.debug("Getting voxels modified after {} in world {}", timestamp, worldId);

        return worldVoxelRepository.findByWorldIdAndLastModifiedAfter(worldId, timestamp)
                .stream()
                .map(WorldVoxel::getVoxel)
                .collect(Collectors.toList());
    }

    /**
     * Clears a chunk (deletes all voxels in it)
     *
     * @param worldId The world identifier
     * @param chunkX  Chunk X coordinate
     * @param chunkY  Chunk Y coordinate
     * @param chunkZ  Chunk Z coordinate
     * @return Number of voxels deleted
     */
    public long clearChunk(String worldId, int chunkX, int chunkY, int chunkZ) {
        LOGGER.debug("Clearing chunk ({}, {}, {}) in world {}", chunkX, chunkY, chunkZ, worldId);

        long count = worldVoxelRepository.findByWorldIdAndChunkXAndChunkYAndChunkZ(
                worldId, chunkX, chunkY, chunkZ).size();

        worldVoxelRepository.deleteByWorldIdAndChunkXAndChunkYAndChunkZ(worldId, chunkX, chunkY, chunkZ);

        LOGGER.info("Cleared {} voxels from chunk ({}, {}, {}) in world {}",
                   count, chunkX, chunkY, chunkZ, worldId);

        return count;
    }

    /**
     * Gets the total number of voxels in a world
     *
     * @param worldId The world identifier
     * @return Number of voxels
     */
    @Transactional(readOnly = true)
    public long getVoxelCount(String worldId) {
        return worldVoxelRepository.countByWorldId(worldId);
    }

    /**
     * Checks if a voxel exists at the given coordinates
     *
     * @param worldId The world identifier
     * @param x       X coordinate
     * @param y       Y coordinate
     * @param z       Z coordinate
     * @return true if voxel exists
     */
    @Transactional(readOnly = true)
    public boolean voxelExists(String worldId, int x, int y, int z) {
        return worldVoxelRepository.existsByWorldIdAndXAndYAndZ(worldId, x, y, z);
    }

    /**
     * Batch save multiple voxels
     *
     * @param worldId The world identifier
     * @param voxels  List of voxels to save
     * @return Number of voxels saved
     */
    public int saveVoxels(String worldId, List<Voxel> voxels) {
        LOGGER.debug("Batch saving {} voxels in world {}", voxels.size(), worldId);

        List<WorldVoxel> worldVoxels = voxels.stream()
                .filter(voxel -> voxel != null && !voxel.isAir())
                .map(voxel -> new WorldVoxel(worldId, voxel))
                .collect(Collectors.toList());

        worldVoxelRepository.saveAll(worldVoxels);

        LOGGER.info("Batch saved {} voxels in world {}", worldVoxels.size(), worldId);

        return worldVoxels.size();
    }
}
