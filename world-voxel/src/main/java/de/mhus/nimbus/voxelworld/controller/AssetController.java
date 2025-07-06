package de.mhus.nimbus.voxelworld.controller;

import de.mhus.nimbus.voxelworld.dto.AssetDto;
import de.mhus.nimbus.voxelworld.entity.Asset;
import de.mhus.nimbus.voxelworld.service.AssetService;
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
 * REST Controller for Asset management operations.
 * Provides endpoints for CRUD operations and asset file handling.
 */
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
@Slf4j
public class AssetController {

    private final AssetService assetService;

    /**
     * Get all assets with optional filtering and pagination
     */
    @GetMapping
    public ResponseEntity<Page<AssetDto>> getAssets(
            @RequestParam(required = false) Asset.AssetType assetType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String namePattern,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Asset> assetPage = assetService.findByCriteria(assetType, category, active, namePattern, pageable);
        Page<AssetDto> dtoPage = assetPage.map(AssetDto::fromEntity);

        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Get asset by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AssetDto> getAsset(@PathVariable Long id) {
        return assetService.findById(id)
                .map(asset -> ResponseEntity.ok(AssetDto.fromEntity(asset)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get asset by name
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<AssetDto> getAssetByName(@PathVariable String name) {
        return assetService.findByName(name)
                .map(asset -> ResponseEntity.ok(AssetDto.fromEntity(asset)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new asset with metadata only
     */
    @PostMapping
    public ResponseEntity<AssetDto> createAsset(@RequestBody AssetDto assetDto) {
        try {
            Asset asset = assetDto.toEntity();
            Asset createdAsset = assetService.createAsset(asset);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AssetDto.fromEntity(createdAsset));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to create asset: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Upload asset with file data
     */
    @PostMapping("/upload")
    public ResponseEntity<AssetDto> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("assetType") Asset.AssetType assetType,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "createdBy", required = false) String createdBy) {

        try {
            Asset asset = new Asset();
            asset.setName(name);
            asset.setAssetType(assetType);
            asset.setCategory(category);
            asset.setDescription(description);
            asset.setCreatedBy(createdBy);
            asset.setData(file.getBytes());
            asset.setSizeBytes(file.getSize());
            asset.setMimeType(file.getContentType());
            asset.setFormat(getFileExtension(file.getOriginalFilename()));

            Asset createdAsset = assetService.createAsset(asset);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AssetDto.fromEntity(createdAsset));
        } catch (IOException e) {
            LOGGER.error("Failed to upload asset file", e);
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to create asset: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update asset metadata
     */
    @PutMapping("/{id}")
    public ResponseEntity<AssetDto> updateAsset(@PathVariable Long id, @RequestBody AssetDto assetDto) {
        try {
            Asset updatedAsset = assetService.updateAsset(id, assetDto.toEntity());
            return ResponseEntity.ok(AssetDto.fromEntity(updatedAsset));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to update asset {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Download asset data
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadAsset(@PathVariable Long id) {
        return assetService.findById(id)
                .map(asset -> {
                    if (asset.getData() == null || asset.getData().length == 0) {
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
     * Activate asset
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<AssetDto> activateAsset(@PathVariable Long id) {
        try {
            Asset asset = assetService.activateAsset(id);
            return ResponseEntity.ok(AssetDto.fromEntity(asset));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate asset
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<AssetDto> deactivateAsset(@PathVariable Long id) {
        try {
            Asset asset = assetService.deactivateAsset(id);
            return ResponseEntity.ok(AssetDto.fromEntity(asset));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete asset
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        try {
            assetService.deleteAsset(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get assets by type
     */
    @GetMapping("/type/{assetType}")
    public ResponseEntity<List<AssetDto>> getAssetsByType(@PathVariable Asset.AssetType assetType) {
        List<Asset> assets = assetService.findActiveByType(assetType);
        List<AssetDto> dtos = assets.stream()
                .map(AssetDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Search assets by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<AssetDto>> searchAssets(@RequestParam String query) {
        List<Asset> assets = assetService.searchByName(query);
        List<AssetDto> dtos = assets.stream()
                .map(AssetDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get asset statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<AssetService.AssetStatistics> getStatistics() {
        AssetService.AssetStatistics stats = assetService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get active asset names for dropdowns
     */
    @GetMapping("/names")
    public ResponseEntity<List<String>> getActiveAssetNames() {
        List<String> names = assetService.getActiveAssetNames();
        return ResponseEntity.ok(names);
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
