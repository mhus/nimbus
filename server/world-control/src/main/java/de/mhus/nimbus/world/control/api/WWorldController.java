package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.generated.types.WorldInfo;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import de.mhus.nimbus.shared.types.WorldId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for managing WWorld entities.
 * Provides CRUD operations for worlds within regions.
 */
@RestController
@RequestMapping("/api/regions/{regionId}/worlds")
@RequiredArgsConstructor
public class WWorldController extends BaseEditorController {

    private final WWorldService worldService;

    // DTOs
    public record WorldRequest(
            String worldId,
            String name,
            String description,
            WorldInfo publicData,
            Boolean enabled,
            String parent,
            String branch,
            Integer groundLevel,
            Integer waterLevel,
            String groundBlockType,
            String waterBlockType
    ) {}

    public record WorldResponse(
            String id,
            String worldId,
            String regionId,
            String name,
            String description,
            WorldInfo publicData,
            Instant createdAt,
            Instant updatedAt,
            boolean enabled,
            String parent,
            String branch,
            int groundLevel,
            Integer waterLevel,
            String groundBlockType,
            String waterBlockType,
            List<String> owner,
            List<String> editor,
            List<String> player,
            boolean publicFlag
    ) {}

    private WorldResponse toResponse(WWorld world) {
        return new WorldResponse(
                world.getId(),
                world.getWorldId(),
                world.getRegionId(),
                world.getName(),
                world.getDescription(),
                world.getPublicData(),
                world.getCreatedAt(),
                world.getUpdatedAt(),
                world.isEnabled(),
                world.getParent(),
                world.getBranch(),
                world.getGroundLevel(),
                world.getWaterLevel(),
                world.getGroundBlockType(),
                world.getWaterBlockType(),
                world.getOwner(),
                world.getEditor(),
                world.getPlayer(),
                world.isPublicFlag()
        );
    }

    /**
     * List all worlds in a region
     * GET /api/regions/{regionId}/worlds
     */
    @GetMapping
    public ResponseEntity<?> list(@PathVariable String regionId) {
        var error = validateId(regionId, "regionId");
        if (error != null) return error;

        try {
            List<WorldResponse> result = worldService.findByRegionId(regionId).stream()
                    .map(this::toResponse)
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return bad(e.getMessage());
        }
    }

    /**
     * Get world by worldId
     * GET /api/regions/{regionId}/worlds/{worldId}
     */
    @GetMapping("/{worldId}")
    public ResponseEntity<?> get(
            @PathVariable String regionId,
            @PathVariable String worldId) {

        var error = validateId(regionId, "regionId");
        if (error != null) return error;

        var error2 = validateId(worldId, "worldId");
        if (error2 != null) return error2;

        return worldService.getByWorldId(worldId)
                .<ResponseEntity<?>>map(w -> {
                    if (!regionId.equals(w.getRegionId())) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "World not found in this region"));
                    }
                    return ResponseEntity.ok(toResponse(w));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "World not found: " + worldId)));
    }

    /**
     * Create new world
     * POST /api/regions/{regionId}/worlds
     */
    @PostMapping
    public ResponseEntity<?> create(
            @PathVariable String regionId,
            @RequestBody WorldRequest request) {

        var error = validateId(regionId, "regionId");
        if (error != null) return error;

        if (blank(request.worldId())) {
            return bad("worldId is required");
        }

        if (blank(request.name())) {
            return bad("name is required");
        }

        try {
            WorldInfo info = request.publicData() != null ? request.publicData() : new WorldInfo();
            WorldId worldIdObj = WorldId.of(request.worldId()).orElseThrow(() ->
                new IllegalArgumentException("Invalid worldId: " + request.worldId()));

            WWorld created = worldService.createWorld(
                    worldIdObj,
                    info,
                    request.parent(),
                    request.branch(),
                    request.enabled()
            );

            // Set additional fields via update
            worldService.updateWorld(worldIdObj, w -> {
                w.setRegionId(regionId);
                w.setName(request.name());
                w.setDescription(request.description());
                if (request.groundLevel() != null) w.setGroundLevel(request.groundLevel());
                if (request.waterLevel() != null) w.setWaterLevel(request.waterLevel());
                if (request.groundBlockType() != null) w.setGroundBlockType(request.groundBlockType());
                if (request.waterBlockType() != null) w.setWaterBlockType(request.waterBlockType());
            });

            WWorld updated = worldService.getByWorldId(worldIdObj).orElseThrow();
            return ResponseEntity.created(URI.create("/api/regions/" + regionId + "/worlds/" + updated.getWorldId()))
                    .body(toResponse(updated));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return bad(e.getMessage());
        }
    }

    /**
     * Update world
     * PUT /api/regions/{regionId}/worlds/{worldId}
     */
    @PutMapping("/{worldId}")
    public ResponseEntity<?> update(
            @PathVariable String regionId,
            @PathVariable String worldId,
            @RequestBody WorldRequest request) {

        var error = validateId(regionId, "regionId");
        if (error != null) return error;

        var error2 = validateId(worldId, "worldId");
        if (error2 != null) return error2;

        WWorld existing = worldService.getByWorldId(worldId).orElse(null);
        if (existing == null) {
            return notFound("World not found: " + worldId);
        }

        if (!regionId.equals(existing.getRegionId())) {
            return notFound("World not found in this region");
        }

        try {
            if (request.name() != null) existing.setName(request.name());
            if (request.description() != null) existing.setDescription(request.description());
            if (request.publicData() != null) existing.setPublicData(request.publicData());
            if (request.enabled() != null) existing.setEnabled(request.enabled());
            if (request.parent() != null) existing.setParent(request.parent());
            if (request.branch() != null) existing.setBranch(request.branch());
            if (request.groundLevel() != null) existing.setGroundLevel(request.groundLevel());
            if (request.waterLevel() != null) existing.setWaterLevel(request.waterLevel());
            if (request.groundBlockType() != null) existing.setGroundBlockType(request.groundBlockType());
            if (request.waterBlockType() != null) existing.setWaterBlockType(request.waterBlockType());

            WWorld updated = worldService.save(existing);
            return ResponseEntity.ok(toResponse(updated));
        } catch (Exception e) {
            return bad(e.getMessage());
        }
    }

    /**
     * Delete world
     * DELETE /api/regions/{regionId}/worlds/{worldId}
     */
    @DeleteMapping("/{worldId}")
    public ResponseEntity<?> delete(
            @PathVariable String regionId,
            @PathVariable String worldId) {

        var error = validateId(regionId, "regionId");
        if (error != null) return error;

        var error2 = validateId(worldId, "worldId");
        if (error2 != null) return error2;

        Optional<WWorld> existing = worldService.getByWorldId(worldId);
        if (existing.isEmpty()) {
            return notFound("World not found: " + worldId);
        }

        if (!regionId.equals(existing.get().getRegionId())) {
            return notFound("World not found in this region");
        }

        try {
            WorldId worldIdObj = WorldId.of(worldId).orElseThrow(() ->
                new IllegalArgumentException("Invalid worldId: " + worldId));

            worldService.deleteWorld(worldIdObj);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception e) {
            return bad(e.getMessage());
        }
    }
}
