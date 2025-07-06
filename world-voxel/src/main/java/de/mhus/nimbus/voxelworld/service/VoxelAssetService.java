package de.mhus.nimbus.voxelworld.service;

import de.mhus.nimbus.shared.asset.Asset;
import de.mhus.nimbus.voxelworld.entity.VoxelAsset;
import de.mhus.nimbus.voxelworld.repository.VoxelAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing voxel assets in the voxel world.
 * Works with the shared Asset class persisted as JSON.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VoxelAssetService {

    private final VoxelAssetRepository voxelAssetRepository;

    /**
     * Create a new voxel asset from shared Asset
     *
     * @param asset the shared Asset to persist
     * @return the created voxel asset
     * @throws IllegalArgumentException if asset is invalid or already exists
     */
    public VoxelAsset createVoxelAsset(Asset asset) {
        LOGGER.debug("Creating new voxel asset with ID: {}", asset.getId());

        if (!asset.isValid()) {
            throw new IllegalArgumentException("Asset validation failed for asset: " + asset.getId());
        }

        if (voxelAssetRepository.existsByAssetId(asset.getId())) {
            throw new IllegalArgumentException("VoxelAsset with ID '" + asset.getId() + "' already exists");
        }

        VoxelAsset voxelAsset = VoxelAsset.fromAsset(asset);
        VoxelAsset savedVoxelAsset = voxelAssetRepository.save(voxelAsset);

        LOGGER.info("Created voxel asset: {} (DB ID: {}, Asset ID: {})",
                   savedVoxelAsset.getName(), savedVoxelAsset.getId(), savedVoxelAsset.getAssetId());

        return savedVoxelAsset;
    }

    /**
     * Update an existing voxel asset
     *
     * @param id the voxel asset database ID
     * @param asset the updated Asset data
     * @return the updated voxel asset
     * @throws IllegalArgumentException if voxel asset not found
     */
    public VoxelAsset updateVoxelAsset(Long id, Asset asset) {
        LOGGER.debug("Updating voxel asset with DB ID: {}", id);

        VoxelAsset existingVoxelAsset = voxelAssetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("VoxelAsset not found with ID: " + id));

        if (!asset.isValid()) {
            throw new IllegalArgumentException("Asset validation failed for asset: " + asset.getId());
        }

        // Check if asset ID changed and conflicts
        if (!existingVoxelAsset.getAssetId().equals(asset.getId()) &&
            voxelAssetRepository.existsByAssetId(asset.getId())) {
            throw new IllegalArgumentException("VoxelAsset with ID '" + asset.getId() + "' already exists");
        }

        existingVoxelAsset.updateFromAsset(asset);
        VoxelAsset savedVoxelAsset = voxelAssetRepository.save(existingVoxelAsset);

        LOGGER.info("Updated voxel asset: {} (DB ID: {}, Asset ID: {}, Version: {})",
                   savedVoxelAsset.getName(), savedVoxelAsset.getId(),
                   savedVoxelAsset.getAssetId(), savedVoxelAsset.getVersion());

        return savedVoxelAsset;
    }

    /**
     * Find voxel asset by database ID
     */
    @Transactional(readOnly = true)
    public Optional<VoxelAsset> findById(Long id) {
        LOGGER.debug("Finding voxel asset by DB ID: {}", id);
        return voxelAssetRepository.findById(id);
    }

    /**
     * Find voxel asset by asset ID
     */
    @Transactional(readOnly = true)
    public Optional<VoxelAsset> findByAssetId(String assetId) {
        LOGGER.debug("Finding voxel asset by asset ID: {}", assetId);
        return voxelAssetRepository.findByAssetId(assetId);
    }

    /**
     * Find voxel asset by name
     */
    @Transactional(readOnly = true)
    public Optional<VoxelAsset> findByName(String name) {
        LOGGER.debug("Finding voxel asset by name: {}", name);
        return voxelAssetRepository.findByName(name);
    }

    /**
     * Get all voxel assets
     */
    @Transactional(readOnly = true)
    public List<VoxelAsset> findAll() {
        LOGGER.debug("Finding all voxel assets");
        return voxelAssetRepository.findAll();
    }

    /**
     * Get all active voxel assets
     */
    @Transactional(readOnly = true)
    public List<VoxelAsset> findAllActive() {
        LOGGER.debug("Finding all active voxel assets");
        return voxelAssetRepository.findByActiveTrue();
    }

    /**
     * Find voxel assets by namespace
     */
    @Transactional(readOnly = true)
    public List<VoxelAsset> findByNamespace(String namespace) {
        LOGGER.debug("Finding voxel assets by namespace: {}", namespace);
        return voxelAssetRepository.findByNamespace(namespace);
    }

    /**
     * Find active voxel assets by namespace
     */
    @Transactional(readOnly = true)
    public List<VoxelAsset> findActiveByNamespace(String namespace) {
        LOGGER.debug("Finding active voxel assets by namespace: {}", namespace);
        return voxelAssetRepository.findByNamespaceAndActiveTrue(namespace);
    }

    /**
     * Find voxel assets by asset type (requires JSON querying)
     */
    @Transactional(readOnly = true)
    public List<VoxelAsset> findByAssetType(Asset.AssetType assetType) {
        LOGGER.debug("Finding voxel assets by asset type: {}", assetType);
        return voxelAssetRepository.findByAssetType(assetType.name());
    }

    /**
     * Find voxel assets by asset category (requires JSON querying)
     */
    @Transactional(readOnly = true)
    public List<VoxelAsset> findByAssetCategory(Asset.AssetCategory category) {
        LOGGER.debug("Finding voxel assets by asset category: {}", category);
        return voxelAssetRepository.findByAssetCategory(category.name());
    }

    /**
     * Search voxel assets by name pattern
     */
    @Transactional(readOnly = true)
    public List<VoxelAsset> searchByName(String namePattern) {
        LOGGER.debug("Searching voxel assets by name pattern: {}", namePattern);
        return voxelAssetRepository.findByNameContainingIgnoreCase(namePattern);
    }

    /**
     * Find voxel assets with pagination and filtering
     */
    @Transactional(readOnly = true)
    public Page<VoxelAsset> findByCriteria(String namespace, Boolean active,
                                          String namePattern, Pageable pageable) {
        LOGGER.debug("Finding voxel assets by criteria - namespace: {}, active: {}, pattern: {}",
                  namespace, active, namePattern);
        return voxelAssetRepository.findByCriteria(namespace, active, namePattern, pageable);
    }

    /**
     * Get shared Assets from VoxelAssets (for API responses)
     */
    @Transactional(readOnly = true)
    public List<Asset> getAssetsFromVoxelAssets(List<VoxelAsset> voxelAssets) {
        return voxelAssets.stream()
                .map(VoxelAsset::getAsset)
                .filter(asset -> asset != null)
                .collect(Collectors.toList());
    }

    /**
     * Deactivate a voxel asset (soft delete)
     */
    public VoxelAsset deactivateVoxelAsset(Long id) {
        LOGGER.debug("Deactivating voxel asset with ID: {}", id);

        VoxelAsset voxelAsset = voxelAssetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("VoxelAsset not found with ID: " + id));

        voxelAsset.setActive(false);
        voxelAsset.setLastModified(LocalDateTime.now());
        VoxelAsset savedVoxelAsset = voxelAssetRepository.save(voxelAsset);

        LOGGER.info("Deactivated voxel asset: {} (ID: {})", savedVoxelAsset.getName(), savedVoxelAsset.getId());
        return savedVoxelAsset;
    }

    /**
     * Activate a voxel asset
     */
    public VoxelAsset activateVoxelAsset(Long id) {
        LOGGER.debug("Activating voxel asset with ID: {}", id);

        VoxelAsset voxelAsset = voxelAssetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("VoxelAsset not found with ID: " + id));

        voxelAsset.setActive(true);
        voxelAsset.setLastModified(LocalDateTime.now());
        VoxelAsset savedVoxelAsset = voxelAssetRepository.save(voxelAsset);

        LOGGER.info("Activated voxel asset: {} (ID: {})", savedVoxelAsset.getName(), savedVoxelAsset.getId());
        return savedVoxelAsset;
    }

    /**
     * Delete a voxel asset permanently
     */
    public void deleteVoxelAsset(Long id) {
        LOGGER.debug("Deleting voxel asset with ID: {}", id);

        if (!voxelAssetRepository.existsById(id)) {
            throw new IllegalArgumentException("VoxelAsset not found with ID: " + id);
        }

        voxelAssetRepository.deleteById(id);
        LOGGER.info("Deleted voxel asset with ID: {}", id);
    }

    /**
     * Get voxel asset statistics
     */
    @Transactional(readOnly = true)
    public VoxelAssetStatistics getStatistics() {
        LOGGER.debug("Calculating voxel asset statistics");

        long totalVoxelAssets = voxelAssetRepository.count();
        long activeVoxelAssets = voxelAssetRepository.countByActiveTrue();

        VoxelAssetStatistics stats = new VoxelAssetStatistics();
        stats.setTotalVoxelAssets(totalVoxelAssets);
        stats.setActiveVoxelAssets(activeVoxelAssets);
        stats.setInactiveVoxelAssets(totalVoxelAssets - activeVoxelAssets);

        return stats;
    }

    /**
     * Get list of active voxel asset names
     */
    @Transactional(readOnly = true)
    public List<String> getActiveVoxelAssetNames() {
        LOGGER.debug("Getting active voxel asset names");
        return voxelAssetRepository.findActiveVoxelAssetNames();
    }

    /**
     * Get list of active voxel asset IDs
     */
    @Transactional(readOnly = true)
    public List<String> getActiveVoxelAssetIds() {
        LOGGER.debug("Getting active voxel asset IDs");
        return voxelAssetRepository.findActiveVoxelAssetIds();
    }

    /**
     * Statistics class for voxel asset information
     */
    public static class VoxelAssetStatistics {
        private long totalVoxelAssets;
        private long activeVoxelAssets;
        private long inactiveVoxelAssets;

        // Getters and setters
        public long getTotalVoxelAssets() { return totalVoxelAssets; }
        public void setTotalVoxelAssets(long totalVoxelAssets) { this.totalVoxelAssets = totalVoxelAssets; }

        public long getActiveVoxelAssets() { return activeVoxelAssets; }
        public void setActiveVoxelAssets(long activeVoxelAssets) { this.activeVoxelAssets = activeVoxelAssets; }

        public long getInactiveVoxelAssets() { return inactiveVoxelAssets; }
        public void setInactiveVoxelAssets(long inactiveVoxelAssets) { this.inactiveVoxelAssets = inactiveVoxelAssets; }
    }
}
