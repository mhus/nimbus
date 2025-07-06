package de.mhus.nimbus.voxelworld.repository;

import de.mhus.nimbus.voxelworld.entity.Asset;
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
 * Repository interface for Asset entity operations.
 * Provides CRUD operations and custom queries for asset management.
 */
@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    /**
     * Find asset by unique name
     */
    Optional<Asset> findByName(String name);

    /**
     * Find assets by type
     */
    List<Asset> findByAssetType(Asset.AssetType assetType);

    /**
     * Find assets by category
     */
    List<Asset> findByCategory(String category);

    /**
     * Find active assets by type
     */
    List<Asset> findByAssetTypeAndActiveTrue(Asset.AssetType assetType);

    /**
     * Find active assets by category
     */
    List<Asset> findByCategoryAndActiveTrue(String category);

    /**
     * Find all active assets
     */
    List<Asset> findByActiveTrue();

    /**
     * Find assets by name pattern (case-insensitive)
     */
    @Query("SELECT a FROM Asset a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
    List<Asset> findByNameContainingIgnoreCase(@Param("namePattern") String namePattern);

    /**
     * Find assets modified after specific date
     */
    List<Asset> findByLastModifiedAfter(LocalDateTime after);

    /**
     * Find assets by multiple criteria with pagination
     */
    @Query("SELECT a FROM Asset a WHERE " +
           "(:assetType IS NULL OR a.assetType = :assetType) AND " +
           "(:category IS NULL OR a.category = :category) AND " +
           "(:active IS NULL OR a.active = :active) AND " +
           "(:namePattern IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :namePattern, '%')))")
    Page<Asset> findByCriteria(@Param("assetType") Asset.AssetType assetType,
                              @Param("category") String category,
                              @Param("active") Boolean active,
                              @Param("namePattern") String namePattern,
                              Pageable pageable);

    /**
     * Count assets by type
     */
    long countByAssetType(Asset.AssetType assetType);

    /**
     * Count active assets
     */
    long countByActiveTrue();

    /**
     * Check if asset name exists
     */
    boolean existsByName(String name);

    /**
     * Find assets with size greater than specified bytes
     */
    List<Asset> findBySizeBytesGreaterThan(Long sizeBytes);

    /**
     * Get asset names only for dropdown/selection purposes
     */
    @Query("SELECT a.name FROM Asset a WHERE a.active = true ORDER BY a.name")
    List<String> findActiveAssetNames();
}
