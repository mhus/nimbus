package de.mhus.nimbus.region.user;

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

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller zur Bereitstellung von Region-Assets (CRUD über SAssetService).
 * Pfade nutzen eine Catch-All PathVariable am Ende im Spring PathPattern-Format: {@code {*path}}.
 * Content-Endpunkt wurde auf Prefix-Variante umgestellt: /{regionId}/asset/content/{*path},
 * da ein Catch-All nicht mehr vor weiteren Segments stehen darf (Spring 6 PathPatternParser).
 */
@RestController
@RequestMapping(RAssetController.BASE_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RegionAssets", description = "CRUD für Region Assets")
public class RAssetController {

    public static final String BASE_PATH = "/region/user/asset";

    private final SAssetService assetService;

    // DTOs
    public record AssetDto(String id, String path, String name, long size, boolean enabled,
                           String regionId, String worldId, boolean inline, String storageId,
                           String createdBy, Instant createdAt) {}
    public record AssetWithContentDto(AssetDto meta, String base64Content) {}
    public record AssetContentRequest(String base64Content, String worldId, String createdBy) {}

    @GetMapping(value = "/{regionId}/assets")
    @Operation(summary = "Liste Assets einer Region", description = "Optional Filter auf worldId")
    public ResponseEntity<?> list(@PathVariable String regionId, @RequestParam(required = false) String worldId) {
        if (regionId == null || regionId.isBlank()) return badRequest("regionId blank");
        // Listing effizient nur über Repository? Hier rudimentär: nicht implementiert -> 501
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("error","listing not implemented yet"));
    }

    @GetMapping(value = "/{regionId}/asset/{*path}")
    @Operation(summary = "Asset Metadaten abrufen")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404")})
    public ResponseEntity<?> get(@PathVariable String regionId,
                                 @PathVariable String path,
                                 @RequestParam(required = false) String worldId,
                                 @RequestParam(defaultValue = "false") boolean withContent) {
        path = normalizePath(path);
        if (regionId == null || regionId.isBlank()) return badRequest("regionId blank");
        if (path == null || path.isBlank()) return badRequest("path blank");
        Optional<SAsset> opt = assetService.findByPath(regionId, worldId, path);
        if (opt.isEmpty()) return notFound("asset not found");
        SAsset a = opt.get();
        AssetDto dto = toDto(a);
        if (withContent) {
            byte[] data = assetService.loadContent(a);
            String b64 = data == null ? null : Base64.getEncoder().encodeToString(data);
            return ResponseEntity.ok(new AssetWithContentDto(dto, b64));
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping(value = "/{regionId}/asset/content/{*path}")
    @Operation(summary = "Rohinhalt abrufen", description = "Liefert den Binärinhalt des Assets")
    public ResponseEntity<?> getContent(@PathVariable String regionId,
                                        @PathVariable String path,
                                        @RequestParam(required = false) String worldId) {
        path = normalizePath(path);
        Optional<SAsset> opt = assetService.findByPath(regionId, worldId, path);
        if (opt.isEmpty()) return notFound("asset not found");
        byte[] data = assetService.loadContent(opt.get());
        if (data == null) return ResponseEntity.ok().header(HttpHeaders.CONTENT_LENGTH, "0").body(new byte[0]);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(data);
    }

    @PostMapping(value = "/{regionId}/asset/{*path}")
    @Operation(summary = "Asset erstellen", description = "Erstellt ein neues Asset. Raw Bytes oder JSON mit base64Content.")
    public ResponseEntity<?> create(@PathVariable String regionId,
                                    @PathVariable String path,
                                    @RequestParam(required = false) String worldId,
                                    @RequestHeader(value = "Content-Type", required = false) String contentType,
                                    @RequestBody(required = false) byte[] rawBody,
                                    @RequestBody(required = false) AssetContentRequest jsonBody,
                                    @RequestHeader(value = "X-User", required = false) String createdBy) {
        path = normalizePath(path);
        try {
            if (exists(regionId, worldId, path)) return conflict("asset already exists");
            byte[] data = resolveBody(contentType, rawBody, jsonBody);
            SAsset saved = assetService.saveAsset(regionId, worldId, path, data, createdBy == null ? "unknown" : createdBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping(value = "/{regionId}/asset/{*path}")
    @Operation(summary = "Asset ersetzen", description = "Ersetzt den Inhalt (oder erstellt falls nicht vorhanden)")
    public ResponseEntity<?> put(@PathVariable String regionId,
                                 @PathVariable String path,
                                 @RequestParam(required = false) String worldId,
                                 @RequestHeader(value = "Content-Type", required = false) String contentType,
                                 @RequestBody(required = false) byte[] rawBody,
                                 @RequestBody(required = false) AssetContentRequest jsonBody,
                                 @RequestHeader(value = "X-User", required = false) String user) {
        path = normalizePath(path);
        byte[] data = resolveBody(contentType, rawBody, jsonBody);
        Optional<SAsset> existing = assetService.findByPath(regionId, worldId, path);
        if (existing.isEmpty()) {
            SAsset saved = assetService.saveAsset(regionId, worldId, path, data, user == null ? "unknown" : user);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
        }
        // update
        // Wir benötigen die ID für updateContent => vorhandenes Asset
        SAsset current = existing.get();
        Optional<SAsset> updated = assetService.updateContent(current.getId(), data);
        return updated.<ResponseEntity<?>>map(a -> ResponseEntity.ok(toDto(a)))
                .orElseGet(() -> notFound("asset disappeared"));
    }

    @PatchMapping(value = "/{regionId}/asset/{*path}")
    @Operation(summary = "Asset aktivieren/deaktivieren")
    public ResponseEntity<?> patchEnabled(@PathVariable String regionId,
                                          @PathVariable String path,
                                          @RequestParam(required = false) String worldId,
                                          @RequestParam Boolean enabled) {
        path = normalizePath(path);
        Optional<SAsset> existing = assetService.findByPath(regionId, worldId, path);
        if (existing.isEmpty()) return notFound("asset not found");
        SAsset asset = existing.get();
        boolean current = asset.isEnabled();
        if (enabled != null && enabled != current) {
            if (!enabled) assetService.disable(asset.getId());
            else { // re-enable
                asset.setEnabled(true); // direkt setzen
            }
        }
        return ResponseEntity.ok(toDto(asset));
    }

    @DeleteMapping(value = "/{regionId}/asset/{*path}")
    @Operation(summary = "Asset löschen")
    public ResponseEntity<?> delete(@PathVariable String regionId,
                                    @PathVariable String path,
                                    @RequestParam(required = false) String worldId) {
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
                a.getRegionId(), a.getWorldId(), a.isInline(), a.getStorageId(), a.getCreatedBy(), a.getCreatedAt());
    }

    private byte[] resolveBody(String contentType, byte[] rawBytes, AssetContentRequest json) {
        if (contentType != null && contentType.startsWith(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
            return rawBytes == null ? new byte[0] : rawBytes;
        }
        if (json != null && json.base64Content() != null) {
            return Base64.getDecoder().decode(json.base64Content());
        }
        // Fallback: falls rawBytes vorhanden aber contentType nicht octet-stream
        return rawBytes == null ? new byte[0] : rawBytes;
    }

    private String normalizePath(String path) {
        if (path == null) return null;
        return path.replaceAll("/{2,}", "/")
                   .replaceAll("^/+", "")
                   .replaceAll("/+\\$", "") // remove trailing slashes
                   .replaceAll("/+\\$", ""); // idempotent second pass
    }

    private ResponseEntity<Map<String,String>> badRequest(String msg) { return ResponseEntity.badRequest().body(Map.of("error", msg)); }
    private ResponseEntity<Map<String,String>> notFound(String msg) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg)); }
    private ResponseEntity<Map<String,String>> conflict(String msg) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg)); }
}
