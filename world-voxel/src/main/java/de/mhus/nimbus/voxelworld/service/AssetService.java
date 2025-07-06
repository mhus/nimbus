package de.mhus.nimbus.voxelworld.service;

import de.mhus.nimbus.voxelworld.entity.Asset;
import de.mhus.nimbus.voxelworld.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing assets in the voxel world.
 * Provides business logic for CRUD operations, validation, and asset management.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AssetService {

    private final AssetRepository assetRepository;

    /**
     * Create a new asset
     *
     * @param asset the asset to create
     * @return the created asset
     * @throws IllegalArgumentException if asset name already exists
     */
    public Asset createAsset(Asset asset) {
        LOGGER.debug("Creating new asset with name: {}", asset.getName());

        if (assetRepository.existsByName(asset.getName())) {
            throw new IllegalArgumentException("Asset with name '" + asset.getName() + "' already exists");
        }

        // Validate asset data
        validateAsset(asset);

        // Set creation metadata
        asset.setCreatedAt(LocalDateTime.now());
        asset.setLastModified(LocalDateTime.now());

        Asset savedAsset = assetRepository.save(asset);
        LOGGER.info("Created asset: {} (ID: {})", savedAsset.getName(), savedAsset.getId());

        return savedAsset;
    }

    /**
     * Update an existing asset
     *
     * @param id the asset ID
     * @param updatedAsset the updated asset data
     * @return the updated asset
     * @throws IllegalArgumentException if asset not found or name conflict
     */
    public Asset updateAsset(Long id, Asset updatedAsset) {
        LOGGER.debug("Updating asset with ID: {}", id);

        Asset existingAsset = assetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + id));

        // Check name uniqueness if name is being changed
        if (!existingAsset.getName().equals(updatedAsset.getName()) &&
            assetRepository.existsByName(updatedAsset.getName())) {
            throw new IllegalArgumentException("Asset with name '" + updatedAsset.getName() + "' already exists");
        }

        // Validate updated data
        validateAsset(updatedAsset);

        // Update fields
        existingAsset.setName(updatedAsset.getName());
        existingAsset.setAssetType(updatedAsset.getAssetType());
        existingAsset.setCategory(updatedAsset.getCategory());
        existingAsset.setDescription(updatedAsset.getDescription());
        existingAsset.setFormat(updatedAsset.getFormat());
        existingAsset.setSizeBytes(updatedAsset.getSizeBytes());
        existingAsset.setMimeType(updatedAsset.getMimeType());
        existingAsset.setData(updatedAsset.getData());
        existingAsset.setFilePath(updatedAsset.getFilePath());
        existingAsset.setActive(updatedAsset.getActive());
        existingAsset.setMetadata(updatedAsset.getMetadata());
        existingAsset.setLastModifiedBy(updatedAsset.getLastModifiedBy());

        // Increment version
        existingAsset.setVersion(existingAsset.getVersion() + 1);

        Asset savedAsset = assetRepository.save(existingAsset);
        LOGGER.info("Updated asset: {} (ID: {}, Version: {})", savedAsset.getName(), savedAsset.getId(), savedAsset.getVersion());

        return savedAsset;
    }

    /**
     * Find asset by ID
     *
     * @param id the asset ID
     * @return the asset if found
     */
    @Transactional(readOnly = true)
    public Optional<Asset> findById(Long id) {
        LOGGER.debug("Finding asset by ID: {}", id);
        return assetRepository.findById(id);
    }

    /**
     * Find asset by name
     *
     * @param name the asset name
     * @return the asset if found
     */
    @Transactional(readOnly = true)
    public Optional<Asset> findByName(String name) {
        LOGGER.debug("Finding asset by name: {}", name);
        return assetRepository.findByName(name);
    }

    /**
     * Get all assets
     *
     * @return list of all assets
     */
    @Transactional(readOnly = true)
    public List<Asset> findAll() {
        LOGGER.debug("Finding all assets");
        return assetRepository.findAll();
    }

    /**
     * Get all active assets
     *
     * @return list of active assets
     */
    @Transactional(readOnly = true)
    public List<Asset> findAllActive() {
        LOGGER.debug("Finding all active assets");
        return assetRepository.findByActiveTrue();
    }

    /**
     * Find assets by type
     *
     * @param assetType the asset type
     * @return list of assets of the specified type
     */
    @Transactional(readOnly = true)
    public List<Asset> findByType(Asset.AssetType assetType) {
        LOGGER.debug("Finding assets by type: {}", assetType);
        return assetRepository.findByAssetType(assetType);
    }

    /**
     * Find active assets by type
     *
     * @param assetType the asset type
     * @return list of active assets of the specified type
     */
    @Transactional(readOnly = true)
    public List<Asset> findActiveByType(Asset.AssetType assetType) {
        LOGGER.debug("Finding active assets by type: {}", assetType);
        return assetRepository.findByAssetTypeAndActiveTrue(assetType);
    }

    /**
     * Find assets by category
     *
     * @param category the category
     * @return list of assets in the specified category
     */
    @Transactional(readOnly = true)
    public List<Asset> findByCategory(String category) {
        LOGGER.debug("Finding assets by category: {}", category);
        return assetRepository.findByCategory(category);
    }

    /**
     * Search assets by name pattern
     *
     * @param namePattern the name pattern to search for
     * @return list of matching assets
     */
    @Transactional(readOnly = true)
    public List<Asset> searchByName(String namePattern) {
        LOGGER.debug("Searching assets by name pattern: {}", namePattern);
        return assetRepository.findByNameContainingIgnoreCase(namePattern);
    }

    /**
     * Find assets with pagination and filtering
     *
     * @param assetType optional asset type filter
     * @param category optional category filter
     * @param active optional active status filter
     * @param namePattern optional name pattern filter
     * @param pageable pagination parameters
     * @return page of filtered assets
     */
    @Transactional(readOnly = true)
    public Page<Asset> findByCriteria(Asset.AssetType assetType, String category, Boolean active,
                                     String namePattern, Pageable pageable) {
        LOGGER.debug("Finding assets by criteria - type: {}, category: {}, active: {}, pattern: {}",
                  assetType, category, active, namePattern);
        return assetRepository.findByCriteria(assetType, category, active, namePattern, pageable);
    }

    /**
     * Deactivate an asset (soft delete)
     *
     * @param id the asset ID
     * @return the deactivated asset
     * @throws IllegalArgumentException if asset not found
     */
    public Asset deactivateAsset(Long id) {
        LOGGER.debug("Deactivating asset with ID: {}", id);

        Asset asset = assetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + id));

        asset.setActive(false);
        Asset savedAsset = assetRepository.save(asset);

        LOGGER.info("Deactivated asset: {} (ID: {})", savedAsset.getName(), savedAsset.getId());
        return savedAsset;
    }

    /**
     * Activate an asset
     *
     * @param id the asset ID
     * @return the activated asset
     * @throws IllegalArgumentException if asset not found
     */
    public Asset activateAsset(Long id) {
        LOGGER.debug("Activating asset with ID: {}", id);

        Asset asset = assetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + id));

        asset.setActive(true);
        Asset savedAsset = assetRepository.save(asset);

        LOGGER.info("Activated asset: {} (ID: {})", savedAsset.getName(), savedAsset.getId());
        return savedAsset;
    }

    /**
     * Delete an asset permanently
     *
     * @param id the asset ID
     * @throws IllegalArgumentException if asset not found
     */
    public void deleteAsset(Long id) {
        LOGGER.debug("Deleting asset with ID: {}", id);

        if (!assetRepository.existsById(id)) {
            throw new IllegalArgumentException("Asset not found with ID: " + id);
        }

        assetRepository.deleteById(id);
        LOGGER.info("Deleted asset with ID: {}", id);
    }

    /**
     * Get asset statistics
     *
     * @return asset statistics
     */
    @Transactional(readOnly = true)
    public AssetStatistics getStatistics() {
        LOGGER.debug("Calculating asset statistics");

        long totalAssets = assetRepository.count();
        long activeAssets = assetRepository.countByActiveTrue();

        AssetStatistics stats = new AssetStatistics();
        stats.setTotalAssets(totalAssets);
        stats.setActiveAssets(activeAssets);
        stats.setInactiveAssets(totalAssets - activeAssets);

        // Count by type
        for (Asset.AssetType type : Asset.AssetType.values()) {
            long count = assetRepository.countByAssetType(type);
            stats.getAssetsByType().put(type, count);
        }

        return stats;
    }

    /**
     * Get list of active asset names for dropdowns/selections
     *
     * @return list of active asset names
     */
    @Transactional(readOnly = true)
    public List<String> getActiveAssetNames() {
        LOGGER.debug("Getting active asset names");
        return assetRepository.findActiveAssetNames();
    }

    /**
     * Validate asset data
     *
     * @param asset the asset to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateAsset(Asset asset) {
        if (asset.getName() == null || asset.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Asset name is required");
        }

        if (asset.getAssetType() == null) {
            throw new IllegalArgumentException("Asset type is required");
        }

        if (asset.getName().length() > 255) {
            throw new IllegalArgumentException("Asset name must not exceed 255 characters");
        }

        // Validate that either data or filePath is provided, but not both
        boolean hasData = asset.getData() != null && asset.getData().length > 0;
        boolean hasFilePath = asset.getFilePath() != null && !asset.getFilePath().trim().isEmpty();

        if (!hasData && !hasFilePath) {
            throw new IllegalArgumentException("Asset must have either data or file path");
        }

        if (hasData && hasFilePath) {
            throw new IllegalArgumentException("Asset cannot have both data and file path");
        }

        // Update size if data is provided
        if (hasData && asset.getSizeBytes() == null) {
            asset.setSizeBytes((long) asset.getData().length);
        }
    }

    /**
     * Statistics class for asset information
     */
    public static class AssetStatistics {
        private long totalAssets;
        private long activeAssets;
        private long inactiveAssets;
        private java.util.Map<Asset.AssetType, Long> assetsByType = new java.util.HashMap<>();

        // Getters and setters
        public long getTotalAssets() { return totalAssets; }
        public void setTotalAssets(long totalAssets) { this.totalAssets = totalAssets; }

        public long getActiveAssets() { return activeAssets; }
        public void setActiveAssets(long activeAssets) { this.activeAssets = activeAssets; }

        public long getInactiveAssets() { return inactiveAssets; }
        public void setInactiveAssets(long inactiveAssets) { this.inactiveAssets = inactiveAssets; }

        public java.util.Map<Asset.AssetType, Long> getAssetsByType() { return assetsByType; }
        public void setAssetsByType(java.util.Map<Asset.AssetType, Long> assetsByType) { this.assetsByType = assetsByType; }
    }
}
