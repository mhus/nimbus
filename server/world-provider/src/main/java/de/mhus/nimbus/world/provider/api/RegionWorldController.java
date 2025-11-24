package de.mhus.nimbus.world.provider.api;

import de.mhus.nimbus.generated.types.WorldInfo;
import de.mhus.nimbus.shared.world.WorldKind;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/world/region/world")
@RequiredArgsConstructor
@Validated
public class RegionWorldController {

    private final WWorldService worldService;

    // DTOs
    public record CreateWorldRequest(String worldId, WorldInfo info) {}
    public record WorldResponse(String worldId, boolean enabled, String parent, String branch, WorldInfo info) {}

    @GetMapping
    public ResponseEntity<?> getWorld(@RequestParam String worldId) {
        if (worldId == null || worldId.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","worldId missing"));
        Optional<WWorld> opt = worldService.getByWorldId(worldId);
        return opt.<ResponseEntity<?>>map(w -> ResponseEntity.ok(toResponse(w)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","world not found")));
    }

    @PostMapping
    public ResponseEntity<?> createWorld(@RequestBody CreateWorldRequest req) {
        if (req.worldId() == null || req.worldId().isBlank()) return ResponseEntity.badRequest().body(Map.of("error","worldId blank"));
        WorldKind kind;
        try { kind = WorldKind.of(req.worldId()); } catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        if (!kind.isMain()) {
            return ResponseEntity.badRequest().body(Map.of("error","only main worlds can be created (no zone, no branch)", "worldId", req.worldId()));
        }
        try {
            WWorld created = worldService.createWorld(req.worldId(), req.info());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    private WorldResponse toResponse(WWorld w) {
        return new WorldResponse(w.getWorldId(), w.isEnabled(), w.getParent(), w.getBranch(), w.getInfo());
    }
}

