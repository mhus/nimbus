package de.mhus.nimbus.voxelworld.repository;

import de.mhus.nimbus.voxelworld.entity.WorldVoxel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WorldVoxel entities
 */
@Repository
public interface WorldVoxelRepository extends JpaRepository<WorldVoxel, Long> {

    /**
     * Find a voxel by world ID and coordinates
     */
    Optional<WorldVoxel> findByWorldIdAndXAndYAndZ(String worldId, int x, int y, int z);

    /**
     * Find all voxels in a specific world
     */
    List<WorldVoxel> findByWorldId(String worldId);

    /**
     * Find all voxels in a specific chunk
     */
    List<WorldVoxel> findByWorldIdAndChunkXAndChunkYAndChunkZ(String worldId, int chunkX, int chunkY, int chunkZ);

    /**
     * Find all voxels in a world within a coordinate range
     */
    @Query("SELECT wv FROM WorldVoxel wv WHERE wv.worldId = :worldId " +
           "AND wv.x BETWEEN :minX AND :maxX " +
           "AND wv.y BETWEEN :minY AND :maxY " +
           "AND wv.z BETWEEN :minZ AND :maxZ")
    List<WorldVoxel> findByWorldIdAndCoordinateRange(
            @Param("worldId") String worldId,
            @Param("minX") int minX, @Param("maxX") int maxX,
            @Param("minY") int minY, @Param("maxY") int maxY,
            @Param("minZ") int minZ, @Param("maxZ") int maxZ);

    /**
     * Find all voxels modified after a specific timestamp
     */
    List<WorldVoxel> findByWorldIdAndLastModifiedAfter(String worldId, LocalDateTime timestamp);

    /**
     * Delete a voxel by world ID and coordinates
     */
    void deleteByWorldIdAndXAndYAndZ(String worldId, int x, int y, int z);

    /**
     * Delete all voxels in a specific chunk
     */
    void deleteByWorldIdAndChunkXAndChunkYAndChunkZ(String worldId, int chunkX, int chunkY, int chunkZ);

    /**
     * Count voxels in a world
     */
    long countByWorldId(String worldId);

    /**
     * Check if a voxel exists at the given coordinates
     */
    boolean existsByWorldIdAndXAndYAndZ(String worldId, int x, int y, int z);
}
