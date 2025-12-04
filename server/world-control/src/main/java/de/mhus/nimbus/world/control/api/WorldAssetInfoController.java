package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.shared.asset.SAssetService;
import de.mhus.nimbus.shared.persistence.SAsset;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for Asset list operations at /api/worlds/{worldId}/assets
 * This provides the test_server compatible asset list endpoint.
 * For full CRUD operations, use EAssetController at /editor/user/asset
 */
@RestController
@RequestMapping("/api/worlds/{worldId}/assetinfo")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WorldAssetInfo", description = "Asset Info for world editor")
public class WorldAssetInfoController extends BaseEditorController {

    private final SAssetService assetService;

    // DTOs
    public record AssetListItemDto(
            String path,
            long size,
            String mimeType,
            Instant lastModified,
            String extension,
            String category
    ) {
    }

    /**
     * List/search assets for a world with pagination and extension filter.
     * GET /api/worlds/{worldId}/assets?query=...&ext=png,jpg&offset=0&limit=50
     */
    @GetMapping
    @Operation(summary = "List assets for world")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "World not found")
    })
    public ResponseEntity<?> list(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Search query") @RequestParam(required = false) String query,
            @Parameter(description = "Extension filter (comma-separated, e.g., 'png,jpg')") @RequestParam(required = false) String ext,
            @Parameter(description = "Pagination offset") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "Pagination limit") @RequestParam(defaultValue = "50") int limit) {

        log.debug("LIST assets: worldId={}, query={}, ext={}, offset={}, limit={}", worldId, query, ext, offset, limit);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validatePagination(offset, limit);
        if (validation != null) return validation;

        // Get all assets for this world (regionId=worldId, worldId=worldId)
        List<SAsset> all = assetService.findByWorldId(worldId).stream()
                .filter(a -> worldId.equals(a.getRegionId()) || a.getRegionId() == null)
                .collect(Collectors.toList());

        // Apply search filter if provided
        if (query != null && !query.isBlank()) {
            all = filterByQuery(all, query);
        }

        // Filter by extensions if provided (e.g., "png,jpg")
        if (ext != null && !ext.isBlank()) {
            String[] extensions = ext.split(",");
            all = all.stream()
                    .filter(asset -> {
                        String assetExt = extractExtension(asset.getPath());
                        for (String reqExt : extensions) {
                            String normalizedExt = reqExt.trim().toLowerCase();
                            if (!normalizedExt.startsWith(".")) {
                                normalizedExt = "." + normalizedExt;
                            }
                            if (assetExt.equalsIgnoreCase(normalizedExt)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
            log.debug("Filtered assets by extensions: {}, count: {}", ext, all.size());
        }

        int totalCount = all.size();

        // Apply pagination
        List<AssetListItemDto> dtos = all.stream()
                .skip(offset)
                .limit(limit)
                .map(this::toListDto)
                .collect(Collectors.toList());

        log.debug("Returning {} assets (total: {})", dtos.size(), totalCount);

        // TypeScript compatible format (match test_server response)
        return ResponseEntity.ok(Map.of(
                "assets", dtos,
                "count", totalCount,
                "limit", limit,
                "offset", offset
        ));
    }

    /**
     * Get/serve asset file content or metadata.
     * GET /api/worlds/{worldId}/assets/{*path}
     * Example: GET /api/worlds/main/assets/textures/block/stone.png
     * Example: GET /api/worlds/main/assets/textures/block/stone.png.info
     */
    @GetMapping("/{*path}")
    @Operation(summary = "Get asset file content")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset found"),
            @ApiResponse(responseCode = "404", description = "Asset not found")
    })
    public ResponseEntity<?> getAssetFile(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Asset path") @PathVariable String path) {

        // Remove leading slash if present
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        // Check if this is an .info request
        boolean isInfoRequest = path != null && path.endsWith(".info");
        if (isInfoRequest) {
            path = path.substring(0, path.length() - 5); // Remove .info suffix
        }

        log.debug("GET asset {}: worldId={}, path={}", isInfoRequest ? "info" : "file", worldId, path);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(path)) {
            return bad("asset path required");
        }

        // Find asset (regionId=worldId, worldId=worldId)
        Optional<SAsset> opt = assetService.findByPath(worldId, worldId, path);
        if (opt.isEmpty()) {
            if (isInfoRequest) {
                // Return empty description if not found (like test_server)
                log.debug("Asset not found for info request: {}", path);
                return ResponseEntity.ok(Map.of("description", ""));
            }
            log.warn("Asset not found: worldId={}, path={}", worldId, path);
            return notFound("asset not found");
        }

        SAsset asset = opt.get();

        // If .info request, return metadata
        if (isInfoRequest) {
            if (asset.getPublicData() == null) {
                return ResponseEntity.ok(Map.of("description", ""));
            }
            return ResponseEntity.ok(asset.getPublicData());
        }

        // Otherwise serve binary content
        if (!asset.isEnabled()) {
            log.warn("Asset disabled: path={}", path);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "asset disabled"));
        }

        byte[] content = assetService.loadContent(asset);
        if (content == null) {
            content = new byte[0];
        }

        // Determine content type
        String mimeType = determineMimeType(path);

        log.debug("Serving asset: path={}, size={}, mimeType={}", path, content.length, mimeType);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(content);
    }

    /**
     * Create new asset.
     * POST /api/worlds/{worldId}/assets/{*path}
     */
    @PostMapping("/{*path}")
    @Operation(summary = "Create new asset")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asset created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Asset already exists")
    })
    public ResponseEntity<?> createAsset(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Asset path") @PathVariable String path,
            @RequestBody byte[] content) {

        // Remove leading slash if present
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        log.debug("CREATE asset: worldId={}, path={}, size={}", worldId, path, content != null ? content.length : 0);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(path)) {
            return bad("asset path required");
        }

        // Check if asset already exists
        if (assetService.findByPath(worldId, worldId, path).isPresent()) {
            return conflict("asset already exists");
        }

        try {
            SAsset saved = assetService.saveAsset(worldId, worldId, path, content, "editor");
            log.info("Created asset: path={}, size={}", path, saved.getSize());
            return ResponseEntity.status(HttpStatus.CREATED).body(toListDto(saved));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating asset: {}", e.getMessage());
            return bad(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating asset", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update asset metadata (.info) or asset content.
     * PUT /api/worlds/{worldId}/assets/textures/block/stone.png.info - Update metadata only
     * PUT /api/worlds/{worldId}/assets/textures/block/stone.png - Update/create binary content
     */
    @PutMapping("/{*path}")
    @Operation(summary = "Update asset or metadata")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset updated"),
            @ApiResponse(responseCode = "201", description = "Asset created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<?> updateAsset(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Asset path") @PathVariable String path,
            @RequestBody(required = true) InputStream stream) {

        // Remove leading slash if present
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        // Check if this is an .info update request
        boolean isInfoUpdate = path != null && path.endsWith(".info");
        if (isInfoUpdate) {
            path = path.substring(0, path.length() - 5); // Remove .info suffix
        }

        log.debug("UPDATE asset {}: worldId={}, path={}", isInfoUpdate ? "metadata" : "content", worldId, path);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(path)) {
            return bad("asset path required");
        }

        try {
            Optional<SAsset> existing = assetService.findByPath(worldId, worldId, path);

            if (isInfoUpdate) {
                // Update metadata only
                if (existing.isEmpty()) {
                    return notFound("asset not found");
                }

                // Parse metadata from request body (JSON)
                String jsonContent = content != null ? new String(content) : "{}";
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                de.mhus.nimbus.shared.persistence.AssetMetadata metadata = mapper.readValue(jsonContent, de.mhus.nimbus.shared.persistence.AssetMetadata.class);

                Optional<SAsset> updated = assetService.updateMetadata(existing.get().getId(), metadata);
                if (updated.isPresent()) {
                    log.info("Updated asset metadata: path={}", path);
                    return ResponseEntity.ok(updated.get().getPublicData());
                } else {
                    return notFound("asset disappeared during update");
                }
            } else {
                // Update/create binary content
                if (existing.isPresent()) {
                    // Update existing
                    Optional<SAsset> updated = assetService.updateContent(existing.get().getId(), content);
                    if (updated.isPresent()) {
                        log.info("Updated asset: path={}, size={}", path, updated.get().getSize());
                        return ResponseEntity.ok(toListDto(updated.get()));
                    } else {
                        return notFound("asset disappeared during update");
                    }
                } else {
                    // Create new
                    SAsset saved = assetService.saveAsset(worldId, worldId, path, content, "editor");
                    log.info("Created asset via PUT: path={}, size={}", path, saved.getSize());
                    return ResponseEntity.status(HttpStatus.CREATED).body(toListDto(saved));
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating asset: {}", e.getMessage());
            return bad(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating asset", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Delete asset.
     * DELETE /api/worlds/{worldId}/assets/{*path}
     */
    @DeleteMapping("/{*path}")
    @Operation(summary = "Delete asset")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Asset deleted"),
            @ApiResponse(responseCode = "404", description = "Asset not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<?> deleteAsset(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Asset path") @PathVariable String path) {

        // Remove leading slash if present
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        log.debug("DELETE asset: worldId={}, path={}", worldId, path);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(path)) {
            return bad("asset path required");
        }

        Optional<SAsset> existing = assetService.findByPath(worldId, worldId, path);
        if (existing.isEmpty()) {
            log.warn("Asset not found for deletion: path={}", path);
            return notFound("asset not found");
        }

        assetService.delete(existing.get().getId());
        log.info("Deleted asset: path={}", path);
        return ResponseEntity.noContent().build();
    }

    // Helper methods

    private AssetListItemDto toListDto(SAsset asset) {
        return new AssetListItemDto(
                asset.getPath(),
                asset.getSize(),
                determineMimeType(asset.getPath()),
                asset.getCreatedAt(),
                extractExtension(asset.getPath()),
                extractCategory(asset.getPath())
        );
    }

    private List<SAsset> filterByQuery(List<SAsset> assets, String query) {
        String lowerQuery = query.toLowerCase();
        return assets.stream()
                .filter(asset -> {
                    String path = asset.getPath();
                    return (path != null && path.toLowerCase().contains(lowerQuery));
                })
                .collect(Collectors.toList());
    }

    private String extractExtension(String path) {
        if (path == null || !path.contains(".")) {
            return "";
        }
        int lastDot = path.lastIndexOf('.');
        return path.substring(lastDot);
    }

    private String extractCategory(String path) {
        if (path == null || !path.contains("/")) {
            return "other";
        }
        int firstSlash = path.indexOf('/');
        return path.substring(0, firstSlash);
    }

    private String determineMimeType(String path) {
        String ext = extractExtension(path).toLowerCase();
        return switch (ext) {
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".svg" -> "image/svg+xml";
            case ".json" -> "application/json";
            case ".glb" -> "model/gltf-binary";
            case ".gltf" -> "model/gltf+json";
            case ".obj" -> "model/obj";
            case ".ogg" -> "audio/ogg";
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            default -> "application/octet-stream";
        };
    }
}
