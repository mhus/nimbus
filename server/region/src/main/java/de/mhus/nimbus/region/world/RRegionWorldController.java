package de.mhus.nimbus.region.world;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Date;
import java.util.Optional;

/**
 * Region-seitiger REST-Controller zum Verwalten von Welten.
 * Pfad: /region/{regionId}/world/{worldId}
 *
 * Zugriff ist durch RegionWorldJwtAuthenticationFilter geschützt und erfordert
 * ein Bearer-Token, das mit dem WORLD-Key (owner=worldId) signiert ist.
 */
@RestController
@RequestMapping(RRegionWorldController.BASE_PATH)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "RegionWorld (Region)", description = "Region verwaltet eigene Welten mit WORLD-Token")
public class RRegionWorldController {

    public static final String BASE_PATH = "/region/{regionId}/world";

    private final RWorldService worldService;
    private final RWorldRepository worldRepository;

    public RRegionWorldController(RWorldService worldService, RWorldRepository worldRepository) {
        this.worldService = worldService;
        this.worldRepository = worldRepository;
    }

    // DTOs
    public record RegionWorldRequest(String name, String description, String worldApiUrl) {}
    public record RegionWorldResponse(String id, String worldId, String name, String description, String worldApiUrl,
                                      String regionId, Date createdAt) {}

    private RegionWorldResponse toResponse(RWorld w) {
        return new RegionWorldResponse(w.getId(), w.getWorldId(), w.getName(), w.getDescription(), w.getApiUrl(), w.getRegionId(), w.getCreatedAt());
    }

    @GetMapping("/{worldId}")
    @Operation(summary = "Welt abrufen (Region)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gefunden"),
            @ApiResponse(responseCode = "404", description = "Nicht gefunden")
    })
    public ResponseEntity<RegionWorldResponse> get(@PathVariable String regionId, @PathVariable String worldId) {
        Optional<RWorld> w = worldRepository.findByWorldId(worldId);
        if (w.isEmpty() || (w.get().getRegionId()!=null && !regionId.equals(w.get().getRegionId()))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(w.get()));
    }

    @PostMapping("/{worldId}")
    @Operation(summary = "Welt erstellen (Region)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Erstellt"),
            @ApiResponse(responseCode = "409", description = "Bereits vorhanden"),
            @ApiResponse(responseCode = "400", description = "Validierungsfehler")
    })
    public ResponseEntity<RegionWorldResponse> create(@PathVariable String regionId,
                                                      @PathVariable String worldId,
                                                      @RequestBody RegionWorldRequest req) {
        Optional<RWorld> existing = worldRepository.findByWorldId(worldId);
        if (existing.isPresent()) return ResponseEntity.status(409).build();

        RWorld w = worldService.create(worldId, req.name());
        w.setDescription(req.description());
        w.setApiUrl(req.worldApiUrl());
        w.setRegionId(regionId);
        w = worldRepository.save(w);
        return ResponseEntity.created(URI.create("/region/" + regionId + "/world/" + worldId))
                .body(toResponse(w));
    }

    @PutMapping("/{worldId}")
    @Operation(summary = "Welt aktualisieren (Region)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aktualisiert"),
            @ApiResponse(responseCode = "404", description = "Nicht gefunden")
    })
    public ResponseEntity<RegionWorldResponse> update(@PathVariable String regionId,
                                                      @PathVariable String worldId,
                                                      @RequestBody RegionWorldRequest req) {
        RWorld w = worldRepository.findByWorldId(worldId).orElse(null);
        if (w == null || (w.getRegionId()!=null && !regionId.equals(w.getRegionId()))) return ResponseEntity.notFound().build();
        // Nur einfache Felder ändern, worldId bleibt unverändert
        if (req.name() != null) w.setName(req.name());
        if (req.description() != null) w.setDescription(req.description());
        if (req.worldApiUrl() != null) w.setApiUrl(req.worldApiUrl());
        if (w.getRegionId() == null) w.setRegionId(regionId); // erstmalig setzen
        w = worldRepository.save(w);
        return ResponseEntity.ok(toResponse(w));
    }

    @DeleteMapping("/{worldId}")
    @Operation(summary = "Welt löschen (Region)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Gelöscht"),
            @ApiResponse(responseCode = "404", description = "Nicht gefunden")
    })
    public ResponseEntity<Void> delete(@PathVariable String regionId, @PathVariable String worldId) {
        RWorld w = worldRepository.findByWorldId(worldId).orElse(null);
        if (w == null || (w.getRegionId()!=null && !regionId.equals(w.getRegionId()))) return ResponseEntity.notFound().build();
        worldRepository.deleteById(w.getId());
        return ResponseEntity.noContent().build();
    }
}
