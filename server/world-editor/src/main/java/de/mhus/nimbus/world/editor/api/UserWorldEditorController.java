package de.mhus.nimbus.world.editor.api;

import de.mhus.nimbus.shared.world.WorldKind;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/world/user/world")
@RequiredArgsConstructor
@Validated
public class UserWorldEditorController {

    private final WWorldService worldService;

    public record CreateChildWorldRequest(String worldId, Boolean enabled, Boolean publicFlag) {}
    public record UpdateChildWorldRequest(Boolean enabled, Boolean publicFlag, List<String> editor, List<String> player) {}

    private String currentUserId(HttpServletRequest req) {
        Object attr = req.getAttribute("currentUserId");
        return attr instanceof String s ? s : null;
    }

    @GetMapping
    public ResponseEntity<?> get(@RequestParam String worldId, HttpServletRequest req) {
        String userId = currentUserId(req);
        if (userId == null) return unauthorized();
        Optional<WWorld> opt = worldService.getByWorldId(worldId);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","world not found"));
        WWorld w = opt.get();
        // Zugriff: Owner darf immer; publicFlag erlaubt Zugriff; andere nicht
        if (!w.getOwner().contains(userId) && !w.isPublicFlag()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","access denied"));
        }
        return ResponseEntity.ok(worldToMap(w));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateChildWorldRequest req, HttpServletRequest http) {
        String userId = currentUserId(http);
        if (userId == null) return unauthorized();
        if (req.worldId() == null || req.worldId().isBlank()) return bad("worldId blank");
        WorldKind kind;
        try { kind = WorldKind.of(req.worldId()); } catch (IllegalArgumentException e) { return bad(e.getMessage()); }
        if (kind.isMain()) return bad("world must not be main (needs zone or branch)");
        // Haupt-Welt laden
        Optional<WWorld> mainOpt = worldService.getByWorldId(kind.worldId());
        if (mainOpt.isEmpty()) return bad("main world not found: " + kind.worldId());
        WWorld main = mainOpt.get();
        if (!main.getOwner().contains(userId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","not an owner of main world"));
        try {
            WWorld created = worldService.createWorld(req.worldId(), main.getInfo(), main.getWorldId(), kind.isBranch() ? kind.branch() : null, req.enabled());
            // publicFlag separat updaten falls gesetzt
            if (req.publicFlag() != null || req.enabled() != null) {
                worldService.updateWorld(created.getWorldId(), w -> {
                    if (req.publicFlag() != null) w.setPublicFlag(req.publicFlag());
                    if (req.enabled() != null) w.setEnabled(req.enabled());
                });
                created = worldService.getByWorldId(created.getWorldId()).orElse(created);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(worldToMap(created));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestParam String worldId, @RequestBody UpdateChildWorldRequest req, HttpServletRequest http) {
        String userId = currentUserId(http);
        if (userId == null) return unauthorized();
        WorldKind kind;
        try { kind = WorldKind.of(worldId); } catch (IllegalArgumentException e) { return bad(e.getMessage()); }
        if (kind.isMain()) return bad("cannot update main world here");
        Optional<WWorld> opt = worldService.getByWorldId(worldId);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","world not found"));
        WWorld existing = opt.get();
        // parent prüfen (sollte worldId des main sein)
        if (existing.getParent() == null || !existing.getParent().equals(kind.worldId())) return bad("world has invalid parent");
        Optional<WWorld> mainOpt = worldService.getByWorldId(kind.worldId());
        if (mainOpt.isEmpty()) return bad("main world not found: " + kind.worldId());
        if (!mainOpt.get().getOwner().contains(userId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","not owner of main world"));
        worldService.updateWorld(worldId, w -> {
            if (req.enabled() != null) w.setEnabled(req.enabled());
            if (req.publicFlag() != null) w.setPublicFlag(req.publicFlag());
            if (req.editor() != null) w.setEditor(req.editor());
            if (req.player() != null) w.setPlayer(req.player());
        });
        WWorld updated = worldService.getByWorldId(worldId).orElse(existing);
        return ResponseEntity.ok(worldToMap(updated));
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@RequestParam String worldId, HttpServletRequest http) {
        String userId = currentUserId(http);
        if (userId == null) return unauthorized();
        WorldKind kind;
        try { kind = WorldKind.of(worldId); } catch (IllegalArgumentException e) { return bad(e.getMessage()); }
        if (kind.isMain()) return bad("cannot delete main world here");
        Optional<WWorld> opt = worldService.getByWorldId(worldId);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","world not found"));
        Optional<WWorld> mainOpt = worldService.getByWorldId(kind.worldId());
        if (mainOpt.isEmpty()) return bad("main world not found: " + kind.worldId());
        if (!mainOpt.get().getOwner().contains(userId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","not owner of main world"));
        boolean deleted = worldService.deleteWorld(worldId);
        if (deleted) return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error","delete failed"));
    }

    private Map<String,Object> worldToMap(WWorld w) {
        return Map.of(
                "worldId", w.getWorldId(),
                "enabled", w.isEnabled(),
                "parent", w.getParent(),
                "branch", w.getBranch(),
                "publicFlag", w.isPublicFlag(),
                "owner", w.getOwner(),
                "editor", w.getEditor(),
                "player", w.getPlayer()
        );
    }

    private ResponseEntity<?> bad(String msg) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg)); }
    private ResponseEntity<?> unauthorized() { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","unauthorized")); }
}

