package de.mhus.nimbus.region.world;

import de.mhus.nimbus.shared.world.WorldKind;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(RUserWorldController.BASE_PATH)
@Validated
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
    public ResponseEntity<?> getMainWorld(@RequestParam String worldId) {
        if (worldId == null || worldId.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","worldId blank"));
        WorldKind kind;
        try { kind = WorldKind.of(worldId); } catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        if (!kind.isMain()) return ResponseEntity.badRequest().body(Map.of("error","not a main world"));
        // lokale RWorld suchen für apiUrl
        Optional<RWorld> opt = rWorldService.getByWorldId(worldId);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","world config not found"));
        Optional<RWorldClientService.WorldInfoDto> remote = worldClient.fetchWorld(opt.get());
        return remote.<ResponseEntity<?>>map(r -> ResponseEntity.ok(new WorldInfoResponse(r.worldId(), r.enabled(), r.parent(), r.branch())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","world not found on server")));
    }

    @PostMapping
    public ResponseEntity<?> createMainWorld(@RequestBody CreateMainWorldRequest req) {
        if (req.worldId() == null || req.worldId().isBlank()) return ResponseEntity.badRequest().body(Map.of("error","worldId blank"));
        if (req.name() == null || req.name().isBlank()) return ResponseEntity.badRequest().body(Map.of("error","name blank"));
        WorldKind kind;
        try { kind = WorldKind.of(req.worldId()); } catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        if (!kind.isMain()) return ResponseEntity.badRequest().body(Map.of("error","only main worlds allowed"));
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

