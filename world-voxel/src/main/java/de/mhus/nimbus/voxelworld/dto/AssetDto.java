package de.mhus.nimbus.voxelworld.dto;

import de.mhus.nimbus.voxelworld.entity.Asset;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Asset entity.
 * Used for API communication without exposing internal entity structure.
 */
@Data
@NoArgsConstructor
public class AssetDto {

    private Long id;
    private String name;
    private Asset.AssetType assetType;
    private String category;
    private String description;
    private String format;
    private Long sizeBytes;
    private String mimeType;
    private String filePath;
    private Integer version;
    private Boolean active;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private String createdBy;
    private String lastModifiedBy;

    /**
     * Convert Asset entity to DTO (without binary data)
     */
    public static AssetDto fromEntity(Asset asset) {
        if (asset == null) {
            return null;
        }

        AssetDto dto = new AssetDto();
        dto.setId(asset.getId());
        dto.setName(asset.getName());
        dto.setAssetType(asset.getAssetType());
        dto.setCategory(asset.getCategory());
        dto.setDescription(asset.getDescription());
        dto.setFormat(asset.getFormat());
        dto.setSizeBytes(asset.getSizeBytes());
        dto.setMimeType(asset.getMimeType());
        dto.setFilePath(asset.getFilePath());
        dto.setVersion(asset.getVersion());
        dto.setActive(asset.getActive());
        dto.setMetadata(asset.getMetadata());
        dto.setCreatedAt(asset.getCreatedAt());
        dto.setLastModified(asset.getLastModified());
        dto.setCreatedBy(asset.getCreatedBy());
        dto.setLastModifiedBy(asset.getLastModifiedBy());

        return dto;
    }

    /**
     * Convert DTO to Asset entity (without binary data)
     */
    public Asset toEntity() {
        Asset asset = new Asset();
        asset.setId(this.getId());
        asset.setName(this.getName());
        asset.setAssetType(this.getAssetType());
        asset.setCategory(this.getCategory());
        asset.setDescription(this.getDescription());
        asset.setFormat(this.getFormat());
        asset.setSizeBytes(this.getSizeBytes());
        asset.setMimeType(this.getMimeType());
        asset.setFilePath(this.getFilePath());
        asset.setVersion(this.getVersion());
        asset.setActive(this.getActive());
        asset.setMetadata(this.getMetadata());
        asset.setCreatedAt(this.getCreatedAt());
        asset.setLastModified(this.getLastModified());
        asset.setCreatedBy(this.getCreatedBy());
        asset.setLastModifiedBy(this.getLastModifiedBy());

        return asset;
    }
}
