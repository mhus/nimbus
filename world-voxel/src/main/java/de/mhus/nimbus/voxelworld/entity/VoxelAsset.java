package de.mhus.nimbus.voxelworld.entity;

import de.mhus.nimbus.shared.asset.Asset;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * JPA Entity for storing voxel assets in the voxel world.
 * Wraps the shared Asset class and persists it as JSON.
 */
@Entity
@Table(name = "voxel_assets",
       indexes = {
           @Index(name = "idx_voxel_asset_asset_id", columnList = "assetId"),
           @Index(name = "idx_voxel_asset_name", columnList = "name"),
           @Index(name = "idx_voxel_asset_namespace", columnList = "namespace"),
           @Index(name = "idx_voxel_asset_created", columnList = "createdAt"),
           @Index(name = "idx_voxel_asset_modified", columnList = "lastModified")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_voxel_asset_id", columnNames = {"assetId"})
       })
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"assetJson"})
@Slf4j
public class VoxelAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique asset identifier from the shared Asset class
     */
    @Column(nullable = false, length = 255, unique = true)
    @EqualsAndHashCode.Include
    private String assetId;

    /**
     * Cached asset name for quick queries
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Cached namespace for organization
     */
    @Column(length = 100)
    private String namespace;

    /**
     * JSON representation of the Asset object
     */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String assetJson;

    /**
     * Version of the voxel asset for tracking changes
     */
    @Column(nullable = false)
    private Integer version = 1;

    /**
     * Indicates if this voxel asset is currently active/published
     */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Creation timestamp
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last modification timestamp
     */
    @Column(nullable = false)
    private LocalDateTime lastModified;

    /**
     * User who created this voxel asset
     */
    @Column(length = 255)
    private String createdBy;

    /**
     * User who last modified this voxel asset
     */
    @Column(length = 255)
    private String lastModifiedBy;

    /**
     * Get the Asset object from JSON
     */
    @Transient
    public Asset getAsset() {
        if (assetJson == null || assetJson.trim().isEmpty()) {
            return null;
        }

        return Asset.fromJson(assetJson);
    }

    /**
     * Set the Asset object and serialize to JSON
     */
    public void setAsset(Asset asset) {
        if (asset == null) {
            this.assetJson = null;
            this.assetId = null;
            this.name = null;
            this.namespace = null;
            return;
        }

        // Cache commonly queried fields
        this.assetId = asset.getId();
        this.name = asset.getName();
        this.namespace = asset.getNamespace();

        // Serialize asset to JSON (binary data is handled by the Asset class itself)
        this.assetJson = asset.toJson();

        if (this.assetJson == null) {
            LOGGER.error("Failed to serialize Asset to JSON for asset ID: {}", asset.getId());
        }
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        lastModified = now;
        if (version == null) {
            version = 1;
        }
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }

    /**
     * Create VoxelAsset from Asset
     */
    public static VoxelAsset fromAsset(Asset asset) {
        if (asset == null) {
            return null;
        }

        VoxelAsset voxelAsset = new VoxelAsset();
        voxelAsset.setAsset(asset);
        return voxelAsset;
    }

    /**
     * Update this VoxelAsset with new Asset data
     */
    public void updateFromAsset(Asset asset) {
        if (asset == null) {
            return;
        }

        setAsset(asset);
        setVersion(getVersion() + 1);
        setLastModified(LocalDateTime.now());
    }
}
