package de.mhus.nimbus.world.player.api;

import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.shared.world.SAssetService;
import de.mhus.nimbus.world.shared.world.AssetMetadata;
import de.mhus.nimbus.world.shared.world.SAsset;
import de.mhus.nimbus.world.shared.world.SAssetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Assets (read-only).
 * Serves binary assets and metadata from MongoDB.
 *
 * Endpoints:
 * - GET /api/worlds/{worldId}/assets - List assets with filters
 * - GET /api/worlds/{worldId}/assets/** - Serve binary asset
 * - GET /api/worlds/{worldId}/assets/**.info - Serve asset metadata
 */
@RestController
@RequestMapping("/api/worlds/{worldId}/assets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Assets", description = "Asset management (textures, models, audio)")
public class AssetController {

    private final SAssetService assetService;
    private final SAssetRepository assetRepository;

    /**
     * List assets with optional filtering and pagination.
     * GET /api/worlds/{worldId}/assets?query=sword&ext=png&limit=100&offset=0
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List assets", description = "List and search assets with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of assets"),
            @ApiResponse(responseCode = "404", description = "World not found")
    })
    public ResponseEntity<?> listAssets(
            @PathVariable String worldId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String ext,
            @RequestParam(defaultValue = "200") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        // Cap limit at 200
        limit = Math.min(limit, 200);

        // Query assets from database
        List<SAsset> allAssets;

        if (query != null && !query.isBlank()) {
            // Search by path pattern
            allAssets = assetRepository.findAll().stream()
                    .filter(a -> a.getPath().toLowerCase().contains(query.toLowerCase()))
                    .filter(SAsset::isEnabled)
                    .toList();
        } else {
            // Get all enabled assets
            allAssets = assetRepository.findAll().stream()
                    .filter(SAsset::isEnabled)
                    .toList();
        }

        // Filter by extension if provided
        if (ext != null && !ext.isBlank()) {
            String[] extensions = ext.split(",");
            allAssets = allAssets.stream()
                    .filter(a -> {
                        for (String extension : extensions) {
                            if (a.getPath().toLowerCase().endsWith("." + extension.trim().toLowerCase())) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .toList();
        }

        // Apply pagination
        int totalCount = allAssets.size();
        List<AssetDto> assets = allAssets.stream()
                .skip(offset)
                .limit(limit)
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(Map.of(
                "assets", assets,
                "count", totalCount,
                "limit", limit,
                "offset", offset
        ));
    }

    /**
     * Serve asset metadata (.info file).
     * GET /api/worlds/{worldId}/assets/textures/items/sword.png.info
     */
    @GetMapping(value = "/**", produces = MediaType.APPLICATION_JSON_VALUE, params = "info")
    @Operation(summary = "Get asset metadata", description = "Returns asset metadata from publicData")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset metadata"),
            @ApiResponse(responseCode = "404", description = "Asset not found")
    })
    public ResponseEntity<?> getAssetInfo(
            @PathVariable String worldId,
            @RequestParam String info) {

        // Extract path from request and remove leading slash if present
        String assetPath = info;
        if (assetPath != null && assetPath.startsWith("/")) {
            assetPath = assetPath.substring(1);
        }
        final String finalAssetPath = assetPath; // Make final for lambda

        // Find asset
        // Try with worldId as regionId first (for main worlds), then fallback to null regionId
        SAsset asset = assetService.findByPath(WorldId.of(worldId).orElse(null), finalAssetPath)
                .orElse(null);

        if (asset == null) {
            // Return empty description if asset not found (like test_server)
            log.debug("Asset not found for info request: {}", finalAssetPath);
            return ResponseEntity.ok(Map.of("description", ""));
        }

        // Return publicData or empty if not available
        AssetMetadata metadata = asset.getPublicData();
        if (metadata == null) {
            return ResponseEntity.ok(Map.of("description", ""));
        }

        return ResponseEntity.ok(metadata);
    }

    /**
     * Serve binary asset file (alternative with explicit path).
     * This works better with Spring's path matching.
     */
    @GetMapping("/{*assetPath}")
    @Operation(summary = "Get asset binary", description = "Returns asset binary content with proper content type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset binary"),
            @ApiResponse(responseCode = "404", description = "Asset not found")
    })
    public ResponseEntity<?> getAssetByPath(
            @PathVariable String worldId,
            @PathVariable String assetPath) {

        // Remove leading slash if present (Spring path variable includes it)
        if (assetPath != null && assetPath.startsWith("/")) {
            assetPath = assetPath.substring(1);
        }
        final String finalAssetPath = assetPath; // Make final for lambda

        log.debug("Asset request: worldId={}, path={}", worldId, finalAssetPath);

        // Find asset in database
        // Try with worldId as regionId first (for main worlds), then fallback to null regionId
        SAsset asset = assetService.findByPath(WorldId.of(worldId).orElse(null), finalAssetPath)
                .orElse(null);

        if (asset == null) {
            log.debug("Asset not found: {}", finalAssetPath);
            return ResponseEntity.notFound().build();
        }

        if (!asset.isEnabled()) {
            log.debug("Asset disabled: {}", finalAssetPath);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Load content as stream - no memory loading!
        InputStream contentStream = assetService.loadContent(asset);
        if (contentStream == null) {
            log.warn("Asset has no content: {}", finalAssetPath);
            return ResponseEntity.notFound().build();
        }

        // Determine content type from metadata or path
        String contentType = determineContentType(asset);

        // Build response with headers for streaming
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));

        // Set content length if available from asset metadata
        if (asset.getSize() > 0) {
            headers.setContentLength(asset.getSize());
        }

        headers.setCacheControl("public, max-age=86400"); // 24 hours cache

        // Set filename for download (use asset name from DB)
        String filename = asset.getName() != null ? asset.getName() : "asset";
        headers.setContentDispositionFormData("inline", filename);

        log.trace("Streaming asset: path={}, size={}, type={}, filename={}",
                 finalAssetPath, asset.getSize(), contentType, filename);

        // Return InputStreamResource for direct streaming without memory loading
        org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(contentStream);

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    /**
     * Convert SAsset to DTO for listing.
     */
    private AssetDto toDto(SAsset asset) {
        return new AssetDto(
                asset.getPath(),
                asset.getName(),
                asset.getSize(),
                asset.getPublicData() != null ? asset.getPublicData().getMimeType() : null,
                asset.getPublicData() != null ? asset.getPublicData().getCategory() : null,
                asset.getPublicData() != null ? asset.getPublicData().getExtension() : null
        );
    }

    /**
     * Determine content type from asset metadata or file extension.
     */
    private String determineContentType(SAsset asset) {
        // Try metadata first
        if (asset.getPublicData() != null && asset.getPublicData().getMimeType() != null) {
            return asset.getPublicData().getMimeType();
        }

        // Fallback to extension-based detection
        String path = asset.getPath();
        if (path == null) return MediaType.APPLICATION_OCTET_STREAM_VALUE;

        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".png")) return "image/png";
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) return "image/jpeg";
        if (lowerPath.endsWith(".gif")) return "image/gif";
        if (lowerPath.endsWith(".webp")) return "image/webp";
        if (lowerPath.endsWith(".svg")) return "image/svg+xml";
        if (lowerPath.endsWith(".json")) return MediaType.APPLICATION_JSON_VALUE;
        if (lowerPath.endsWith(".obj") || lowerPath.endsWith(".mtl")) return MediaType.TEXT_PLAIN_VALUE;
        if (lowerPath.endsWith(".glb") || lowerPath.endsWith(".gltf")) return "model/gltf-binary";
        if (lowerPath.endsWith(".wav")) return "audio/wav";
        if (lowerPath.endsWith(".mp3")) return "audio/mpeg";
        if (lowerPath.endsWith(".ogg")) return "audio/ogg";

        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /**
     * DTO for asset listing.
     */
    public record AssetDto(
            String path,
            String name,
            long size,
            String mimeType,
            String category,
            String extension
    ) {}
}
