package de.mhus.nimbus.world.control.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.shared.world.SAssetService;
import de.mhus.nimbus.world.shared.world.AssetMetadata;
import de.mhus.nimbus.world.shared.world.SAsset;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Asset metadata operations at /api/worlds/{worldId}/assetinfo
 * Handles .info suffix for asset metadata (GET and PUT only).
 * For binary asset content, use WorldAssetController.
 */
@RestController
@RequestMapping("/api/worlds/{worldId}/assetinfo")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WorldAssetInfo", description = "Asset metadata management")
public class WorldAssetInfoController extends BaseEditorController {

    private final SAssetService assetService;
    private final ObjectMapper objectMapper;

    /**
     * Get asset metadata.
     * GET /api/worlds/{worldId}/assetinfo/{*path}
     * Example: GET /api/worlds/main/assetinfo/textures/block/stone.png
     * Returns the asset's publicData (AssetMetadata)
     */
    @GetMapping("/{*path}")
    @Operation(summary = "Get asset metadata")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metadata found or empty"),
            @ApiResponse(responseCode = "404", description = "Asset not found")
    })
    public ResponseEntity<?> getAssetInfo(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Asset path") @PathVariable String path) {

        // Remove leading slash if present
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        log.debug("GET asset info: worldId={}, path={}", worldId, path);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(path)) {
            return bad("asset path required");
        }

        // Find asset (worldId=worldId)
        Optional<SAsset> opt = assetService.findByPath(WorldId.of(worldId).orElse(null), path);
        if (opt.isEmpty()) {
            // Return empty description if not found (like test_server)
            log.debug("Asset not found for info request: {}", path);
            return ResponseEntity.ok(Map.of("description", ""));
        }

        SAsset asset = opt.get();

        // Return metadata (publicData)
        if (asset.getPublicData() == null) {
            return ResponseEntity.ok(Map.of("description", ""));
        }

        return ResponseEntity.ok(asset.getPublicData());
    }

    /**
     * Update asset metadata.
     * PUT /api/worlds/{worldId}/assetinfo/{*path}
     * Example: PUT /api/worlds/main/assetinfo/textures/block/stone.png
     * Body: AssetMetadata JSON { description: "..." }
     */
    @PutMapping("/{*path}")
    @Operation(summary = "Update asset metadata")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metadata updated"),
            @ApiResponse(responseCode = "404", description = "Asset not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<?> updateAssetInfo(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Asset path") @PathVariable String path,
            @RequestBody(required = true) String jsonContent) {

        // Remove leading slash if present
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        log.debug("UPDATE asset metadata: worldId={}, path={}", worldId, path);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(path)) {
            return bad("asset path required");
        }

        try {
            Optional<SAsset> existing = assetService.findByPath(WorldId.of(worldId).orElse(null), path);
            if (existing.isEmpty()) {
                return notFound("asset not found");
            }

            // Parse metadata from request body (JSON)
            AssetMetadata metadata = objectMapper.readValue(jsonContent, AssetMetadata.class);

            Optional<SAsset> updated = assetService.updateMetadata(existing.get(), metadata);
            if (updated.isPresent()) {
                log.info("Updated asset metadata: path={}", path);
                return ResponseEntity.ok(updated.get().getPublicData());
            } else {
                return notFound("asset disappeared during update");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating metadata: {}", e.getMessage());
            return bad(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating metadata", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get license information for an asset.
     * GET /api/worlds/{worldId}/assetinfo/{*path}/license
     * Returns: { source, author, license }
     */
    @GetMapping("/{*path}/license")
    @Operation(summary = "Get asset license information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "License info found or empty"),
            @ApiResponse(responseCode = "404", description = "Asset not found")
    })
    public ResponseEntity<?> getLicenseInfo(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Asset path") @PathVariable String path) {

        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        log.debug("GET asset license: worldId={}, path={}", worldId, path);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(path)) {
            return bad("asset path required");
        }

        Optional<SAsset> opt = assetService.findByPath(WorldId.of(worldId).orElse(null), path);
        if (opt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "source", "",
                    "author", "",
                    "license", ""
            ));
        }

        SAsset asset = opt.get();
        AssetMetadata metadata = asset.getPublicData();

        if (metadata == null) {
            return ResponseEntity.ok(Map.of(
                    "source", "",
                    "author", "",
                    "license", ""
            ));
        }

        return ResponseEntity.ok(Map.of(
                "source", metadata.getSource() != null ? metadata.getSource() : "",
                "author", metadata.getAuthor() != null ? metadata.getAuthor() : "",
                "license", metadata.getLicense() != null ? metadata.getLicense() : ""
        ));
    }

    /**
     * Set license information for an asset.
     * PUT /api/worlds/{worldId}/assetinfo/{*path}/license
     * Body: { source, author, license }
     * Automatically sets licenseFixed=true
     */
    @PutMapping("/{*path}/license")
    @Operation(summary = "Set asset license information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "License info updated"),
            @ApiResponse(responseCode = "404", description = "Asset not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<?> setLicenseInfo(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Asset path") @PathVariable String path,
            @RequestBody Map<String, String> licenseData) {

        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        log.debug("SET asset license: worldId={}, path={}", worldId, path);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(path)) {
            return bad("asset path required");
        }

        try {
            Optional<SAsset> existing = assetService.findByPath(WorldId.of(worldId).orElse(null), path);
            if (existing.isEmpty()) {
                return notFound("asset not found");
            }

            SAsset asset = existing.get();
            AssetMetadata metadata = asset.getPublicData();
            if (metadata == null) {
                metadata = new AssetMetadata();
            }

            // Set license fields
            metadata.setSource(licenseData.get("source"));
            metadata.setAuthor(licenseData.get("author"));
            metadata.setLicense(licenseData.get("license"));
            // Automatically set licenseFixed to true
            metadata.setLicenseFixed(true);

            Optional<SAsset> updated = assetService.updateMetadata(asset, metadata);
            if (updated.isPresent()) {
                log.info("Updated asset license (licenseFixed=true): path={}", path);
                return ResponseEntity.ok(Map.of(
                        "source", metadata.getSource() != null ? metadata.getSource() : "",
                        "author", metadata.getAuthor() != null ? metadata.getAuthor() : "",
                        "license", metadata.getLicense() != null ? metadata.getLicense() : "",
                        "licenseFixed", true
                ));
            } else {
                return notFound("asset disappeared during update");
            }
        } catch (Exception e) {
            log.error("Error updating license info", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error"));
        }
    }
}

