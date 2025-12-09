package de.mhus.nimbus.region.world;

import de.mhus.nimbus.shared.types.WorldId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping(RUserWorldController.BASE_PATH)
@Validated
@Tag(name = "UserWorld", description = "User-Zugriff auf Main Worlds (Forward zu World & Universe Server)")
public class RUserWorldController {

    public static final String BASE_PATH = "/region/user/world";

    private final RWorldService rWorldService;
    private final RWorldClientService worldClient;
    private final RUniverseClientService universeClient;

    public RUserWorldController(RWorldService rWorldService,
                                RWorldClientService worldClient,
                                RUniverseClientService universeClient) {
        this.rWorldService = rWorldService;
        this.worldClient = worldClient;
        this.universeClient = universeClient;
    }

    // DTOs
    public record CreateMainWorldRequest(String worldId, String name, String description, String regionId, String coordinates) {}
    public record WorldInfoResponse(String worldId, Boolean enabled, String parent, String branch) {}

    @GetMapping
    @Operation(summary = "Main World abrufen", description = "Lädt Informationen einer Main World über Forward zum World-Server")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Gefunden"),
        @ApiResponse(responseCode = "400", description = "Validierungsfehler"),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden (Konfiguration oder Remote)"),
        @ApiResponse(responseCode = "500", description = "Unerwarteter Fehler")
    })
    public ResponseEntity<?> getMainWorld(@RequestParam String worldIdStr) {
        if (worldIdStr == null || worldIdStr.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","worldId blank"));
        WorldId worldId;
        try { worldId = WorldId.of(worldIdStr).get(); } catch (NoSuchElementException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        if (!worldId.isMain()) return ResponseEntity.badRequest().body(Map.of("error","not a main world"));
        // lokale RWorld suchen für apiUrl
        Optional<RWorld> opt = rWorldService.getByWorldId(worldIdStr);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","world config not found"));
        Optional<RWorldClientService.WorldInfoDto> remote = worldClient.fetchWorld(opt.get());
        return remote.<ResponseEntity<?>>map(r -> ResponseEntity.ok(new WorldInfoResponse(r.worldId(), r.enabled(), r.parent(), r.branch())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","world not found on server")));
    }

    @PostMapping
    @Operation(summary = "Main World erstellen", description = "Erstellt eine Main World lokal und remote (World-Server, optional Universe). Nur main worldIds erlaubt.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Erstellt"),
        @ApiResponse(responseCode = "400", description = "Validierungsfehler"),
        @ApiResponse(responseCode = "502", description = "Fehler beim Forward (Universe oder World Server)")
    })
    public ResponseEntity<?> createMainWorld(@RequestBody CreateMainWorldRequest req) {
        if (req.worldId() == null || req.worldId().isBlank()) return ResponseEntity.badRequest().body(Map.of("error","worldId blank"));
        if (req.name() == null || req.name().isBlank()) return ResponseEntity.badRequest().body(Map.of("error","name blank"));
        WorldId worldId;
        try { worldId = WorldId.of(req.worldId()).get(); } catch (NoSuchElementException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        if (!worldId.isMain()) return ResponseEntity.badRequest().body(Map.of("error","only main worlds allowed"));
        // Region-Welt Config anlegen falls nicht vorhanden
        Optional<RWorld> existing = rWorldService.getByWorldId(req.worldId());
        RWorld config = existing.orElseGet(() -> rWorldService.create(req.worldId(), req.name()));
        // Universe World erstellen (falls regionId angegeben)
        if (req.regionId() != null && !req.regionId().isBlank()) {
            try {
                universeClient.create(req.regionId(), req.worldId(), req.name(), req.description(), req.coordinates());
            } catch (RuntimeException ex) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error","universe create failed","details", ex.getMessage()));
            }
        }
        // World Server Main World erstellen
        try {
            RWorldClientService.WorldInfoDto info = worldClient.createMainWorld(config, req.name(), req.description());
            return ResponseEntity.created(URI.create(BASE_PATH + "?worldId=" + info.worldId()))
                    .body(new WorldInfoResponse(info.worldId(), info.enabled(), info.parent(), info.branch()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error","world server create failed","details", ex.getMessage()));
        }
    }
}
