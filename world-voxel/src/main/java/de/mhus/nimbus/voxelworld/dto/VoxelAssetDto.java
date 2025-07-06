package de.mhus.nimbus.voxelworld.dto;

import de.mhus.nimbus.shared.asset.Asset;
import de.mhus.nimbus.voxelworld.entity.VoxelAsset;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for VoxelAsset entity.
 * Now wraps the shared Asset class for API communication.
 */
@Data
@NoArgsConstructor
public class VoxelAssetDto {

    private Long id;
    private String assetId;
    private String name;
    private String namespace;
    private Asset asset;
    private Integer version;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private String createdBy;
    private String lastModifiedBy;

    /**
     * Convert VoxelAsset entity to DTO (including the shared Asset)
     */
    public static VoxelAssetDto fromEntity(VoxelAsset voxelAsset) {
        if (voxelAsset == null) {
            return null;
        }

        VoxelAssetDto dto = new VoxelAssetDto();
        dto.setId(voxelAsset.getId());
        dto.setAssetId(voxelAsset.getAssetId());
        dto.setName(voxelAsset.getName());
        dto.setNamespace(voxelAsset.getNamespace());
        dto.setAsset(voxelAsset.getAsset()); // Get the shared Asset object
        dto.setVersion(voxelAsset.getVersion());
        dto.setActive(voxelAsset.getActive());
        dto.setCreatedAt(voxelAsset.getCreatedAt());
        dto.setLastModified(voxelAsset.getLastModified());
        dto.setCreatedBy(voxelAsset.getCreatedBy());
        dto.setLastModifiedBy(voxelAsset.getLastModifiedBy());

        return dto;
    }

    /**
     * Convert DTO to VoxelAsset entity
     */
    public VoxelAsset toEntity() {
        VoxelAsset voxelAsset = new VoxelAsset();
        voxelAsset.setId(this.getId());
        voxelAsset.setVersion(this.getVersion());
        voxelAsset.setActive(this.getActive());
        voxelAsset.setCreatedAt(this.getCreatedAt());
        voxelAsset.setLastModified(this.getLastModified());
        voxelAsset.setCreatedBy(this.getCreatedBy());
        voxelAsset.setLastModifiedBy(this.getLastModifiedBy());

        // Set the shared Asset object
        if (this.getAsset() != null) {
            voxelAsset.setAsset(this.getAsset());
        }

        return voxelAsset;
    }

    /**
     * Create a lightweight DTO without binary data for list operations
     */
    public static VoxelAssetDto fromEntityLight(VoxelAsset voxelAsset) {
        if (voxelAsset == null) {
            return null;
        }

        VoxelAssetDto dto = new VoxelAssetDto();
        dto.setId(voxelAsset.getId());
        dto.setAssetId(voxelAsset.getAssetId());
        dto.setName(voxelAsset.getName());
        dto.setNamespace(voxelAsset.getNamespace());
        dto.setVersion(voxelAsset.getVersion());
        dto.setActive(voxelAsset.getActive());
        dto.setCreatedAt(voxelAsset.getCreatedAt());
        dto.setLastModified(voxelAsset.getLastModified());
        dto.setCreatedBy(voxelAsset.getCreatedBy());
        dto.setLastModifiedBy(voxelAsset.getLastModifiedBy());

        // Create lightweight Asset without binary data
        Asset originalAsset = voxelAsset.getAsset();
        if (originalAsset != null) {
            Asset lightAsset = Asset.builder()
                    .id(originalAsset.getId())
                    .name(originalAsset.getName())
                    .type(originalAsset.getType())
                    .category(originalAsset.getCategory())
                    .mimeType(originalAsset.getMimeType())
                    .format(originalAsset.getFormat())
                    .sizeBytes(originalAsset.getSizeBytes())
                    .version(originalAsset.getVersion())
                    .namespace(originalAsset.getNamespace())
                    .path(originalAsset.getPath())
                    .metadata(originalAsset.getMetadata())
                    .dependencies(originalAsset.getDependencies())
                    .build();
            dto.setAsset(lightAsset);
        }

        return dto;
    }
}
