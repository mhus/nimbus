package de.mhus.nimbus.universe.region;

import de.mhus.nimbus.shared.dto.universe.RegionWorldRequest;
import de.mhus.nimbus.shared.dto.universe.RegionWorldResponse;
import de.mhus.nimbus.universe.world.UWorld;
import de.mhus.nimbus.universe.world.UWorldRepository;
import de.mhus.nimbus.universe.world.UWorldService;
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
 * REST-Controller für automatische Weltverwaltung einer Region.
 * Pfad: /universe/region/{regionId}/world/{worldId}
 *
 * Zugriff erfolgt ausschließlich mit einem Region-Bearer-Token (siehe RegionJwtAuthenticationFilter).
 */
@RestController
@RequestMapping(URegionWorldController.BASE_PATH)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "RegionWorld", description = "Region verwaltet Welten über Region-Token")
public class URegionWorldController {

    public static final String BASE_PATH = "/universe/region/{regionId}/world";

    private final UWorldService worldService;
    private final UWorldRepository worldRepository;

    public URegionWorldController(UWorldService worldService, UWorldRepository worldRepository) {
        this.worldService = worldService;
        this.worldRepository = worldRepository;
    }

    private RegionWorldResponse toResponse(UWorld w) {
        return new RegionWorldResponse(w.getId(), w.getName(), w.getDescription(), w.getCreatedAt(),
                w.getRegionId(), w.getPlanetId(), w.getSolarSystemId(), w.getGalaxyId(),
                w.getWorldId(), w.getCoordinates());
    }

    @GetMapping("/{worldId}")
    @Operation(summary = "Welt abrufen (Region)", description = "Liefert die Welt anhand von regionId und worldId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gefunden"),
            @ApiResponse(responseCode = "404", description = "Nicht gefunden")
    })
    public ResponseEntity<RegionWorldResponse> get(@PathVariable String regionId,
                                                   @PathVariable String worldId) {
        Optional<UWorld> w = worldRepository.findByRegionIdAndWorldId(regionId, worldId);
        return w.map(value -> ResponseEntity.ok(toResponse(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{worldId}")
    @Operation(summary = "Welt erstellen (Region)", description = "Erstellt eine Welt für die Region; worldId wird aus dem Pfad übernommen")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Erstellt"),
            @ApiResponse(responseCode = "400", description = "Validierungsfehler"),
            @ApiResponse(responseCode = "409", description = "Bereits vorhanden")
    })
    public ResponseEntity<RegionWorldResponse> create(@PathVariable String regionId,
                                                      @PathVariable String worldId,
                                                      @RequestBody RegionWorldRequest req) {
        Optional<UWorld> existing = worldRepository.findByRegionIdAndWorldId(regionId, worldId);
        if (existing.isPresent()) return ResponseEntity.status(409).build();
        UWorld w = worldService.create(
                req.name(), req.description(),
                regionId, req.planetId(), req.solarSystemId(), req.galaxyId(),
                worldId, req.coordinates());
        return ResponseEntity.created(URI.create("/universe/region/" + regionId + "/world/" + worldId))
                .body(toResponse(w));
    }

    @PutMapping("/{worldId}")
    @Operation(summary = "Welt aktualisieren (Region)", description = "Aktualisiert Felder einer bestehenden Welt (Identifikation über regionId + worldId)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aktualisiert"),
            @ApiResponse(responseCode = "404", description = "Nicht gefunden")
    })
    public ResponseEntity<RegionWorldResponse> update(@PathVariable String regionId,
                                                      @PathVariable String worldId,
                                                      @RequestBody RegionWorldRequest req) {
        UWorld w = worldRepository.findByRegionIdAndWorldId(regionId, worldId).orElse(null);
        if (w == null) return ResponseEntity.notFound().build();
        UWorld updated = worldService.update(w.getId(),
                req.name(), req.description(),
                regionId, req.planetId(), req.solarSystemId(), req.galaxyId(),
                worldId, req.coordinates());
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{worldId}")
    @Operation(summary = "Welt löschen (Region)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Gelöscht"),
            @ApiResponse(responseCode = "404", description = "Nicht gefunden")
    })
    public ResponseEntity<Void> delete(@PathVariable String regionId, @PathVariable String worldId) {
        UWorld w = worldRepository.findByRegionIdAndWorldId(regionId, worldId).orElse(null);
        if (w == null) return ResponseEntity.notFound().build();
        worldService.delete(w.getId());
        return ResponseEntity.noContent().build();
    }
}
