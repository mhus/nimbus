package de.mhus.nimbus.voxelworld.repository;

import de.mhus.nimbus.voxelworld.entity.VoxelAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for VoxelAsset entity operations.
 * Provides CRUD operations and custom queries for voxel asset management.
 */
@Repository
public interface VoxelAssetRepository extends JpaRepository<VoxelAsset, Long> {

    /**
     * Find voxel asset by unique asset ID
     */
    Optional<VoxelAsset> findByAssetId(String assetId);

    /**
     * Find voxel asset by name
     */
    Optional<VoxelAsset> findByName(String name);

    /**
     * Find voxel assets by namespace
     */
    List<VoxelAsset> findByNamespace(String namespace);

    /**
     * Find voxel assets by namespace and active status
     */
    List<VoxelAsset> findByNamespaceAndActiveTrue(String namespace);

    /**
     * Find all active voxel assets
     */
    List<VoxelAsset> findByActiveTrue();

    /**
     * Find voxel assets by name pattern (case-insensitive)
     */
    @Query("SELECT v FROM VoxelAsset v WHERE LOWER(v.name) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
    List<VoxelAsset> findByNameContainingIgnoreCase(@Param("namePattern") String namePattern);

    /**
     * Find voxel assets modified after specific date
     */
    List<VoxelAsset> findByLastModifiedAfter(LocalDateTime after);

    /**
     * Find voxel assets by multiple criteria with pagination
     */
    @Query("SELECT v FROM VoxelAsset v WHERE " +
           "(:namespace IS NULL OR v.namespace = :namespace) AND " +
           "(:active IS NULL OR v.active = :active) AND " +
           "(:namePattern IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :namePattern, '%')))")
    Page<VoxelAsset> findByCriteria(@Param("namespace") String namespace,
                                   @Param("active") Boolean active,
                                   @Param("namePattern") String namePattern,
                                   Pageable pageable);

    /**
     * Find voxel assets by JSON content using JSON functions (PostgreSQL/MySQL)
     */
    @Query(value = "SELECT * FROM voxel_assets WHERE JSON_EXTRACT(asset_json, '$.type') = :assetType", nativeQuery = true)
    List<VoxelAsset> findByAssetType(@Param("assetType") String assetType);

    /**
     * Find voxel assets by category using JSON functions
     */
    @Query(value = "SELECT * FROM voxel_assets WHERE JSON_EXTRACT(asset_json, '$.category') = :category", nativeQuery = true)
    List<VoxelAsset> findByAssetCategory(@Param("category") String category);

    /**
     * Count active voxel assets
     */
    long countByActiveTrue();

    /**
     * Check if voxel asset ID exists
     */
    boolean existsByAssetId(String assetId);

    /**
     * Find voxel assets with binary data larger than specified size
     */
    @Query("SELECT v FROM VoxelAsset v WHERE LENGTH(v.binaryData) > :sizeBytes")
    List<VoxelAsset> findWithBinaryDataLargerThan(@Param("sizeBytes") long sizeBytes);

    /**
     * Get voxel asset names only for dropdown/selection purposes
     */
    @Query("SELECT v.name FROM VoxelAsset v WHERE v.active = true ORDER BY v.name")
    List<String> findActiveVoxelAssetNames();

    /**
     * Get voxel asset IDs only for lightweight operations
     */
    @Query("SELECT v.assetId FROM VoxelAsset v WHERE v.active = true ORDER BY v.assetId")
    List<String> findActiveVoxelAssetIds();

    /**
     * Find voxel assets by namespace with pagination
     */
    Page<VoxelAsset> findByNamespaceAndActiveTrue(String namespace, Pageable pageable);

    /**
     * Count voxel assets by namespace
     */
    long countByNamespaceAndActiveTrue(String namespace);
}
