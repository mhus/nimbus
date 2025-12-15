package de.mhus.nimbus.universe.api;

import de.mhus.nimbus.shared.user.UniverseRoles;
import de.mhus.nimbus.universe.security.Role;
import de.mhus.nimbus.universe.world.UWorld;
import de.mhus.nimbus.universe.world.UWorldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(UWorldController.BASE_PATH)
@Role({UniverseRoles.ADMIN})
@Tag(name = "UWorld", description = "Verwaltung von Welten (UWorld)")
public class UWorldController {

    public static final String BASE_PATH = "/universe/user/world";

    private final UWorldService service;

    public UWorldController(UWorldService service) {
        this.service = service;
    }

    // DTOs
    public record UWorldRequest(String name, String description,
                                String regionId, String planetId, String solarSystemId, String galaxyId,
                                String worldId, String coordinates) { }

    public record UWorldResponse(String id, String name, String description, Date createdAt,
                                 String regionId, String planetId, String solarSystemId, String galaxyId,
                                 String worldId, String coordinates) { }

    private UWorldResponse toResponse(UWorld w) {
        return new UWorldResponse(w.getId(), w.getName(), w.getDescription(), w.getCreatedAt(),
                w.getRegionId(), w.getPlanetId(), w.getSolarSystemId(), w.getGalaxyId(),
                w.getWorldId(), w.getCoordinates());
    }

    @GetMapping
    @Operation(summary = "UWorlds auflisten")
    @ApiResponse(responseCode = "200", description = "Liste zurückgegeben")
    public ResponseEntity<List<UWorldResponse>> list() {
        List<UWorldResponse> out = service.listAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{id}")
    @Operation(summary = "UWorld abrufen")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Gefunden"),
            @ApiResponse(responseCode = "404", description = "Nicht gefunden")})
    public ResponseEntity<UWorldResponse> get(@PathVariable String id) {
        return service.getById(id)
                .map(w -> ResponseEntity.ok(toResponse(w)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "UWorld erstellen")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Erstellt"),
            @ApiResponse(responseCode = "400", description = "Validierungsfehler")})
    public ResponseEntity<UWorldResponse> create(@RequestBody UWorldRequest req) {
        UWorld w = service.create(req.name(), req.description(), req.regionId(), req.planetId(), req.solarSystemId(), req.galaxyId(), req.worldId(), req.coordinates());
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + w.getId())).body(toResponse(w));
    }

    @PutMapping("/{id}")
    @Operation(summary = "UWorld aktualisieren")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Aktualisiert"),
            @ApiResponse(responseCode = "404", description = "Nicht gefunden")})
    public ResponseEntity<UWorldResponse> update(@PathVariable String id, @RequestBody UWorldRequest req) {
        if (service.getById(id).isEmpty()) return ResponseEntity.notFound().build();
        UWorld updated = service.update(id, req.name(), req.description(), req.regionId(), req.planetId(), req.solarSystemId(), req.galaxyId(), req.worldId(), req.coordinates());
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "UWorld löschen")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Gelöscht"),
            @ApiResponse(responseCode = "404", description = "Nicht gefunden")})
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (service.getById(id).isEmpty()) return ResponseEntity.notFound().build();
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
