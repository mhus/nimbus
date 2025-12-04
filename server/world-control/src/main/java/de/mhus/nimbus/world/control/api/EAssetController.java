package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.shared.asset.SAssetService;
import de.mhus.nimbus.shared.persistence.SAsset;
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
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Asset-CRUD für den World-Editor Kontext.
 * Basis-Pfad: /editor/user/asset/{regionId}/{worldId}/{*path}
 * Catch-All Syntax angepasst auf Spring PathPatternParser ({*path}). Content-Endpunkt jetzt
 * /editor/user/asset/{regionId}/{worldId}/content/{*path} statt Mid-Pattern.
 */
@RestController
@RequestMapping(EAssetController.BASE_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "EditorAssets", description = "CRUD für World-Editor Assets")
public class EAssetController {

    public static final String BASE_PATH = "/editor/user/asset";

    private final SAssetService assetService;

    // DTOs
    public record AssetDto(String id, String path, String name, long size, boolean enabled,
                           String regionId, String worldId, String storageId,
                           String createdBy, Instant createdAt) {}

    @GetMapping("/{regionId}/{worldId}/{*path}")
    @Operation(summary = "Asset Metadaten abrufen")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404")})
    public ResponseEntity<?> get(@PathVariable String regionId,
                                 @PathVariable String worldId,
                                 @PathVariable String path
                                 ) {
        path = normalizePath(path);
        if (blank(regionId) || blank(worldId) || blank(path)) return bad("blank parameter");
        Optional<SAsset> opt = assetService.findByPath(regionId, worldId, path);
        if (opt.isEmpty()) return notFound("asset not found");
        SAsset a = opt.get();
        AssetDto dto = toDto(a);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{regionId}/{worldId}/content/{*path}")
    @Operation(summary = "Asset Rohinhalt abrufen")
    public ResponseEntity<?> content(@PathVariable String regionId,
                                     @PathVariable String worldId,
                                     @PathVariable String path) {
        path = normalizePath(path);
        Optional<SAsset> opt = assetService.findByPath(regionId, worldId, path);
        if (opt.isEmpty()) return notFound("asset not found");
        var stream = assetService.loadContent(opt.get());
        if (stream == null) return ResponseEntity.ok().header(HttpHeaders.CONTENT_LENGTH, "0").body(new byte[0]);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(stream);
    }

    @PostMapping("/{regionId}/{worldId}/{*path}")
    @Operation(summary = "Asset erstellen")
    @ApiResponses({@ApiResponse(responseCode = "201"), @ApiResponse(responseCode = "409")})
    public ResponseEntity<?> create(@PathVariable String regionId,
                                    @PathVariable String worldId,
                                    @PathVariable String path,
                                    @RequestHeader(value = "Content-Type", required = false) String contentType,
                                    InputStream stream
                                    ) {
        path = normalizePath(path);
        try {
            if (exists(regionId, worldId, path)) return conflict("asset exists");
            SAsset saved = assetService.saveAsset(regionId, worldId, path, stream,"editor");
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
        } catch (IllegalArgumentException e) {
            return bad(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{regionId}/{worldId}/{*path}")
    @Operation(summary = "Asset ersetzen oder erstellen")
    public ResponseEntity<?> put(@PathVariable String regionId,
                                 @PathVariable String worldId,
                                 @PathVariable String path,
                                 @RequestHeader(value = "Content-Type", required = false) String contentType,
                                 InputStream stream
                                 ) {
        path = normalizePath(path);
        Optional<SAsset> existing = assetService.findByPath(regionId, worldId, path);
        if (existing.isEmpty()) {
            SAsset saved = assetService.saveAsset(regionId, worldId, path, stream, "editor");
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
        }
        SAsset current = existing.get();
        SAsset updated = assetService.updateContent(current.getId(), stream);
        if (updated == null) return notFound("asset disappeared");
        return ResponseEntity.ok(toDto(updated));
    }

    @PatchMapping("/{regionId}/{worldId}/{*path}")
    @Operation(summary = "Asset aktivieren/deaktivieren")
    public ResponseEntity<?> patchEnabled(@PathVariable String regionId,
                                          @PathVariable String worldId,
                                          @PathVariable String path,
                                          @RequestParam Boolean enabled) {
        path = normalizePath(path);
        Optional<SAsset> existing = assetService.findByPath(regionId, worldId, path);
        if (existing.isEmpty()) return notFound("asset not found");
        SAsset asset = existing.get();
        boolean current = asset.isEnabled();
        if (enabled != null && enabled != current) {
            if (!enabled) assetService.disable(asset.getId());
            else asset.setEnabled(true);
        }
        return ResponseEntity.ok(toDto(asset));
    }

    @DeleteMapping("/{regionId}/{worldId}/{*path}")
    @Operation(summary = "Asset löschen")
    public ResponseEntity<?> delete(@PathVariable String regionId,
                                    @PathVariable String worldId,
                                    @PathVariable String path) {
        path = normalizePath(path);
        Optional<SAsset> existing = assetService.findByPath(regionId, worldId, path);
        if (existing.isEmpty()) return notFound("asset not found");
        assetService.delete(existing.get().getId());
        return ResponseEntity.noContent().build();
    }

    // Hilfsmethoden
    private boolean exists(String regionId, String worldId, String path) {
        return assetService.findByPath(regionId, worldId, path).isPresent();
    }
    private AssetDto toDto(SAsset a) {
        return new AssetDto(a.getId(), a.getPath(), a.getName(), a.getSize(), a.isEnabled(),
                a.getRegionId(), a.getWorldId(), a.getStorageId(), a.getCreatedBy(), a.getCreatedAt());
    }

    private String normalizePath(String path) {
        if (path == null) return null;
        return path.replaceAll("/{2,}", "/")
                   .replaceAll("^/+", "")
                .replaceAll("/+\\$", "") // remove trailing slashes
                .replaceAll("/+\\$", ""); // idempotent second pass
    }
    private boolean blank(String s) { return s == null || s.isBlank(); }
    private ResponseEntity<Map<String,String>> bad(String msg) { return ResponseEntity.badRequest().body(Map.of("error", msg)); }
    private ResponseEntity<Map<String,String>> notFound(String msg) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg)); }
    private ResponseEntity<Map<String,String>> conflict(String msg) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg)); }
}
