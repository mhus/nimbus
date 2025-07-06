package de.mhus.nimbus.voxelworld.controller;

import de.mhus.nimbus.shared.asset.Asset;
import de.mhus.nimbus.voxelworld.entity.VoxelAsset;
import de.mhus.nimbus.voxelworld.service.VoxelAssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for VoxelAsset management operations.
 * Works directly with the shared Asset class.
 */
@RestController
@RequestMapping("/api/v1/voxel-assets")
@RequiredArgsConstructor
@Slf4j
public class VoxelAssetController {

    private final VoxelAssetService voxelAssetService;

    /**
     * Get all voxel assets with optional filtering and pagination
     */
    @GetMapping
    public ResponseEntity<Page<Asset>> getVoxelAssets(
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String namePattern,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<VoxelAsset> voxelAssetPage = voxelAssetService.findByCriteria(namespace, active, namePattern, pageable);
        Page<Asset> assetPage = voxelAssetPage.map(VoxelAsset::getAsset);

        return ResponseEntity.ok(assetPage);
    }

    /**
     * Get voxel asset by database ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Asset> getVoxelAsset(@PathVariable Long id) {
        return voxelAssetService.findById(id)
                .map(voxelAsset -> ResponseEntity.ok(voxelAsset.getAsset()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get voxel asset by asset ID
     */
    @GetMapping("/asset/{assetId}")
    public ResponseEntity<Asset> getVoxelAssetByAssetId(@PathVariable String assetId) {
        return voxelAssetService.findByAssetId(assetId)
                .map(voxelAsset -> ResponseEntity.ok(voxelAsset.getAsset()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get voxel asset by name
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<Asset> getVoxelAssetByName(@PathVariable String name) {
        return voxelAssetService.findByName(name)
                .map(voxelAsset -> ResponseEntity.ok(voxelAsset.getAsset()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new voxel asset from shared Asset
     */
    @PostMapping
    public ResponseEntity<Asset> createVoxelAsset(@RequestBody Asset asset) {
        try {
            VoxelAsset createdVoxelAsset = voxelAssetService.createVoxelAsset(asset);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(createdVoxelAsset.getAsset());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to create voxel asset: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Upload voxel asset with file data and create shared Asset
     */
    @PostMapping("/upload")
    public ResponseEntity<Asset> uploadVoxelAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("assetId") String assetId,
            @RequestParam("name") String name,
            @RequestParam("type") Asset.AssetType type,
            @RequestParam(value = "category", required = false) Asset.AssetCategory category,
            @RequestParam(value = "namespace", defaultValue = "nimbus") String namespace,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "createdBy", required = false) String createdBy) {

        try {
            String format = getFileExtension(file.getOriginalFilename());
            String path = getAssetPath(type, assetId, format);

            Asset asset = Asset.builder()
                    .id(assetId)
                    .name(name)
                    .type(type)
                    .category(category != null ? category : getDefaultCategory(type))
                    .data(file.getBytes())
                    .format(format)
                    .mimeType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .namespace(namespace)
                    .path(path)
                    .build();

            if (description != null) {
                asset.addMetadata("description", description);
            }
            if (createdBy != null) {
                asset.addMetadata("createdBy", createdBy);
            }

            VoxelAsset createdVoxelAsset = voxelAssetService.createVoxelAsset(asset);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(createdVoxelAsset.getAsset());
        } catch (IOException e) {
            LOGGER.error("Failed to upload voxel asset file", e);
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to create voxel asset: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update voxel asset
     */
    @PutMapping("/{id}")
    public ResponseEntity<Asset> updateVoxelAsset(@PathVariable Long id, @RequestBody Asset asset) {
        try {
            VoxelAsset updatedVoxelAsset = voxelAssetService.updateVoxelAsset(id, asset);
            return ResponseEntity.ok(updatedVoxelAsset.getAsset());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to update voxel asset {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Download voxel asset binary data
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadVoxelAsset(@PathVariable Long id) {
        return voxelAssetService.findById(id)
                .map(voxelAsset -> {
                    Asset asset = voxelAsset.getAsset();
                    if (asset == null || !asset.hasBinaryData()) {
                        return ResponseEntity.notFound().<byte[]>build();
                    }

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType(
                            asset.getMimeType() != null ? asset.getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
                    headers.setContentLength(asset.getData().length);

                    String filename = asset.getName();
                    if (asset.getFormat() != null) {
                        filename += "." + asset.getFormat();
                    }
                    headers.setContentDispositionFormData("attachment", filename);

                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(asset.getData());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get voxel assets by namespace
     */
    @GetMapping("/namespace/{namespace}")
    public ResponseEntity<List<Asset>> getVoxelAssetsByNamespace(@PathVariable String namespace) {
        List<VoxelAsset> voxelAssets = voxelAssetService.findActiveByNamespace(namespace);
        List<Asset> assets = voxelAssets.stream()
                .map(VoxelAsset::getAsset)
                .collect(Collectors.toList());
        return ResponseEntity.ok(assets);
    }

    /**
     * Get voxel assets by asset type
     */
    @GetMapping("/type/{assetType}")
    public ResponseEntity<List<Asset>> getVoxelAssetsByType(@PathVariable Asset.AssetType assetType) {
        List<VoxelAsset> voxelAssets = voxelAssetService.findByAssetType(assetType);
        List<Asset> assets = voxelAssets.stream()
                .map(VoxelAsset::getAsset)
                .collect(Collectors.toList());
        return ResponseEntity.ok(assets);
    }

    /**
     * Get voxel assets by asset category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Asset>> getVoxelAssetsByCategory(@PathVariable Asset.AssetCategory category) {
        List<VoxelAsset> voxelAssets = voxelAssetService.findByAssetCategory(category);
        List<Asset> assets = voxelAssets.stream()
                .map(VoxelAsset::getAsset)
                .collect(Collectors.toList());
        return ResponseEntity.ok(assets);
    }

    /**
     * Activate voxel asset
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Asset> activateVoxelAsset(@PathVariable Long id) {
        try {
            VoxelAsset voxelAsset = voxelAssetService.activateVoxelAsset(id);
            return ResponseEntity.ok(voxelAsset.getAsset());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate voxel asset
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Asset> deactivateVoxelAsset(@PathVariable Long id) {
        try {
            VoxelAsset voxelAsset = voxelAssetService.deactivateVoxelAsset(id);
            return ResponseEntity.ok(voxelAsset.getAsset());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete voxel asset
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVoxelAsset(@PathVariable Long id) {
        try {
            voxelAssetService.deleteVoxelAsset(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Search voxel assets by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<Asset>> searchVoxelAssets(@RequestParam String query) {
        List<VoxelAsset> voxelAssets = voxelAssetService.searchByName(query);
        List<Asset> assets = voxelAssets.stream()
                .map(VoxelAsset::getAsset)
                .collect(Collectors.toList());
        return ResponseEntity.ok(assets);
    }

    /**
     * Get voxel asset statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<VoxelAssetService.VoxelAssetStatistics> getStatistics() {
        VoxelAssetService.VoxelAssetStatistics stats = voxelAssetService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get active voxel asset names for dropdowns
     */
    @GetMapping("/names")
    public ResponseEntity<List<String>> getActiveVoxelAssetNames() {
        List<String> names = voxelAssetService.getActiveVoxelAssetNames();
        return ResponseEntity.ok(names);
    }

    /**
     * Get default category for asset type
     */
    private Asset.AssetCategory getDefaultCategory(Asset.AssetType type) {
        return switch (type) {
            case TEXTURE -> Asset.AssetCategory.BLOCKS;
            case MODEL -> Asset.AssetCategory.BLOCKS;
            case SOUND -> Asset.AssetCategory.EFFECTS;
            case MUSIC -> Asset.AssetCategory.MUSIC_TRACKS;
            case RECIPE -> Asset.AssetCategory.RECIPES;
            case BLOCKSTATE -> Asset.AssetCategory.BLOCKS;
            case LOOT_TABLE -> Asset.AssetCategory.LOOT;
            case ADVANCEMENT -> Asset.AssetCategory.ADVANCEMENTS;
            case STRUCTURE -> Asset.AssetCategory.STRUCTURES;
            case BIOME -> Asset.AssetCategory.BIOMES;
            case GUI -> Asset.AssetCategory.GUI;
            case SHADER -> Asset.AssetCategory.SHADERS;
            default -> Asset.AssetCategory.OTHER;
        };
    }

    /**
     * Generate asset path based on type and ID
     */
    private String getAssetPath(Asset.AssetType type, String assetId, String format) {
        String basePath = switch (type) {
            case TEXTURE -> "textures";
            case MODEL -> "models";
            case SOUND -> "sounds";
            case MUSIC -> "music";
            case RECIPE -> "recipes";
            case BLOCKSTATE -> "blockstates";
            case LOOT_TABLE -> "loot_tables";
            case ADVANCEMENT -> "advancements";
            case STRUCTURE -> "structures";
            case BIOME -> "worldgen/biome";
            case GUI -> "textures/gui";
            case SHADER -> "shaders";
            default -> "data";
        };

        return format != null ? basePath + "/" + assetId + "." + format : basePath + "/" + assetId;
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return null;
    }
}
