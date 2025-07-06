package de.mhus.nimbus.voxelworld.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * JPA Entity for storing assets in the voxel world.
 * Assets represent reusable components, textures, models, or other resources
 * that can be referenced by voxels or other game elements.
 */
@Entity
@Table(name = "assets",
       indexes = {
           @Index(name = "idx_asset_name", columnList = "name"),
           @Index(name = "idx_asset_type", columnList = "assetType"),
           @Index(name = "idx_asset_category", columnList = "category"),
           @Index(name = "idx_asset_created", columnList = "createdAt"),
           @Index(name = "idx_asset_modified", columnList = "lastModified")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_asset_name", columnNames = {"name"})
       })
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"data"})
public class Asset {

    private static final Logger LOGGER = LoggerFactory.getLogger(Asset.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique name identifier for the asset
     */
    @Column(nullable = false, length = 255, unique = true)
    @EqualsAndHashCode.Include
    private String name;

    /**
     * Type of the asset (e.g., TEXTURE, MODEL, SOUND, SCRIPT)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AssetType assetType;

    /**
     * Category for grouping assets (e.g., BLOCKS, ITEMS, CREATURES)
     */
    @Column(length = 100)
    private String category;

    /**
     * Human-readable description of the asset
     */
    @Column(length = 1000)
    private String description;

    /**
     * File extension or format (e.g., png, obj, wav)
     */
    @Column(length = 20)
    private String format;

    /**
     * Size of the asset data in bytes
     */
    @Column
    private Long sizeBytes;

    /**
     * MIME type of the asset
     */
    @Column(length = 100)
    private String mimeType;

    /**
     * Binary data of the asset (for small assets) or path reference
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] data;

    /**
     * External file path if asset is stored outside database
     */
    @Column(length = 500)
    private String filePath;

    /**
     * Version of the asset for tracking changes
     */
    @Column(nullable = false)
    private Integer version = 1;

    /**
     * Indicates if this asset is currently active/published
     */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Additional metadata as JSON string
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadata;

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
     * User who created this asset
     */
    @Column(length = 255)
    private String createdBy;

    /**
     * User who last modified this asset
     */
    @Column(length = 255)
    private String lastModifiedBy;

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
     * Asset types enum
     */
    public enum AssetType {
        TEXTURE,
        MODEL,
        SOUND,
        SCRIPT,
        ANIMATION,
        SHADER,
        MATERIAL,
        PREFAB,
        CONFIG,
        OTHER
    }
}
