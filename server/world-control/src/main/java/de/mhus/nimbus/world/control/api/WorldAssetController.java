package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.shared.world.SAssetService;
import de.mhus.nimbus.world.shared.world.SAsset;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for Asset list operations at /control/worlds/{worldId}/assets
 * This provides the test_server compatible asset list endpoint.
 * For full CRUD operations, use EAssetController at /editor/user/asset
 */
@RestController
@RequestMapping("/control/worlds/{worldId}/assets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WorldAssets", description = "Asset listing for world editor")
public class WorldAssetController extends BaseEditorController {

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
     * GET /control/worlds/{worldId}/assets?query=...&ext=png,jpg&offset=0&limit=50
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
        WorldId wid = toWorldId(worldId);
        if (wid == null) return ResponseEntity.badRequest().body(Map.of("error", "invalid worldId"));


        List<SAsset> all = assetService.findByWorldId(wid).stream()
                .collect(Collectors.toList());

        // Apply search filter if provided
        if (Strings.isNotBlank(query)) {
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
     * Get/serve asset file content.
     * GET /control/worlds/{worldId}/assets/{*path}
     * Example: GET /control/worlds/main/assets/textures/block/stone.png
     *
     * For metadata (.info), use WorldAssetInfoController
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

        log.debug("GET asset file: worldId={}, path={}", worldId, path);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(path)) {
            return bad("asset path required");
        }

        // Find asset (regionId=worldId, worldId=worldId)
        Optional<SAsset> opt = assetService.findByPath(toWorldId(worldId), path);
        if (opt.isEmpty()) {
            log.warn("Asset not found: worldId={}, path={}", worldId, path);
            return notFound("asset not found");
        }

        SAsset asset = opt.get();

        // Serve binary content
        if (!asset.isEnabled()) {
            log.warn("Asset disabled: path={}", path);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "asset disabled"));
        }

        // Load content as stream - no memory loading!
        InputStream contentStream = assetService.loadContent(asset);
        if (contentStream == null) {
            log.warn("Asset has no content: {}", path);
            return ResponseEntity.notFound().build();
        }

        // Determine content type
        String mimeType = determineMimeType(path);

        log.debug("Streaming asset: path={}, size={}, mimeType={}", path, asset.getSize(), mimeType);

        // Return InputStreamResource for streaming
        org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(contentStream);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .contentLength(asset.getSize())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }

    /**
     * Create new asset.
     * POST /control/worlds/{worldId}/assets/{*path}
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
            InputStream contentStream) {

        // Remove leading slash if present
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        log.debug("CREATE asset: worldId={}, path={}", worldId, path);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(path)) {
            return bad("asset path required");
        }

        // Check if asset already exists
        if (assetService.findByPath(toWorldId(worldId), path).isPresent()) {
            return conflict("asset already exists");
        }

        try {
            SAsset saved = assetService.saveAsset(toWorldId(worldId), path, contentStream, "editor");
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
     * Update asset content.
     * PUT /control/worlds/{worldId}/assets/textures/block/stone.png - Update/create binary content
     *
     * For metadata updates (.info), use WorldAssetInfoController
     */
    @PutMapping("/{*path}")
    @Operation(summary = "Update asset content")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset updated"),
            @ApiResponse(responseCode = "201", description = "Asset created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<?> updateAsset(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Asset path") @PathVariable String path,
            InputStream contentStream) {

        // Remove leading slash if present
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        log.debug("UPDATE asset content: worldId={}, path={}", worldId, path);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(path)) {
            return bad("asset path required");
        }

        try {
            Optional<SAsset> existing = assetService.findByPath(toWorldId(worldId), path);

            // Update/create binary content
            if (existing.isPresent()) {
                // Update existing
                SAsset updated = assetService.updateContent(existing.get(), contentStream);
                if (updated != null) {
                    log.info("Updated asset: path={}, size={}", path, updated.getSize());
                    return ResponseEntity.ok(toListDto(updated));
                } else {
                    return notFound("asset disappeared during update");
                }
            } else {
                // Create new
                SAsset saved = assetService.saveAsset(toWorldId(worldId), path, contentStream, "editor");
                log.info("Created asset via PUT: path={}, size={}", path, saved.getSize());
                return ResponseEntity.status(HttpStatus.CREATED).body(toListDto(saved));
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
     * DELETE /control/worlds/{worldId}/assets/{*path}
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

        Optional<SAsset> existing = assetService.findByPath(toWorldId(worldId), path);
        if (existing.isEmpty()) {
            log.warn("Asset not found for deletion: path={}", path);
            return notFound("asset not found");
        }

        assetService.delete(existing.get());
        log.info("Deleted asset: path={}", path);
        return ResponseEntity.noContent().build();
    }


    /**
     * Duplicate Asset with a new path.
     * PATCH /control/worlds/{worldId}/assets/duplicate/{*path}
     * Body: { "newPath": "..." }
     */
    @PatchMapping("/duplicate")
    @Operation(summary = "Asset duplizieren",
            description = "Erstellt eine Kopie eines Assets mit einem neuen Pfad")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asset erfolgreich dupliziert"),
            @ApiResponse(responseCode = "400", description = "Ungültige Parameter"),
            @ApiResponse(responseCode = "404", description = "Quell-Asset nicht gefunden"),
            @ApiResponse(responseCode = "409", description = "Ziel-Asset existiert bereits")
    })
    public ResponseEntity<?> duplicate(
                                       @Parameter(description = "World identifier") @PathVariable String worldId,
                                       @PathVariable String sourcePath,
                                       @RequestBody Map<String, String> body) {
        String newPath = normalizePath(body.get("newPath"));

        log.debug("DUPLICATE asset: worldId={}, sourcePath={}, newPath={}",
                worldId, sourcePath, newPath);

        if (blank(sourcePath)) return bad("sourcePath required");
        if (blank(newPath)) return bad("newPath required");
        if (sourcePath.equals(newPath)) return bad("sourcePath and newPath must be different");

        WorldId wid = WorldId.of(worldId).orElse(null);
        if (wid == null) return bad("invalid worldId");

        // Check if source asset exists
        Optional<SAsset> sourceOpt = assetService.findByPath(wid, sourcePath);
        if (sourceOpt.isEmpty()) {
            log.warn("Source asset not found for duplication: path={}", sourcePath);
            return notFound("source asset not found");
        }

        // Check if new path already exists
        if (assetService.findByPath(wid, newPath).isPresent()) {
            return conflict("asset already exists at new path: " + newPath);
        }

        try {
            SAsset source = sourceOpt.get();

            // Duplicate the asset
            SAsset duplicate = assetService.duplicateAsset(source, newPath, "editor");

            log.info("Duplicated asset: sourcePath={}, newPath={}, size={}",
                    sourcePath, newPath, duplicate.getSize());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "path", duplicate.getPath(),
                    "message", "Asset duplicated successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to duplicate asset", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to duplicate asset: " + e.getMessage()));
        }
    }


    // Helper methods

    private String normalizePath(String path) {
        if (path == null) return null;
        return path.replaceAll("/{2,}", "/")
                .replaceAll("^/+", "")
                .replaceAll("/+\\$", "") // remove trailing slashes
                .replaceAll("/+\\$", ""); // idempotent second pass
    }

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

    private WorldId toWorldId(String worldIdStr) {
        return WorldId.of(worldIdStr).orElse(null);
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
            default -> "application/octet_stream";
        };
    }
}
