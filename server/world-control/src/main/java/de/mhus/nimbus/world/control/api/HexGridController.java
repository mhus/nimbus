package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.generated.types.HexGrid;
import de.mhus.nimbus.generated.types.HexVector2;
import de.mhus.nimbus.world.shared.rest.BaseEditorController;
import de.mhus.nimbus.world.shared.world.WHexGridEntity;
import de.mhus.nimbus.world.shared.world.WHexGridService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing WHexGrid entities.
 * Provides CRUD operations for hexagonal grid areas within worlds.
 */
@RestController
@RequestMapping("/control/worlds/{worldId}/hexgrid")
@RequiredArgsConstructor
public class HexGridController extends BaseEditorController {

    private final WHexGridService hexGridService;

    // DTOs

    /**
     * Request DTO for creating or updating hex grids.
     */
    public record HexGridRequest(
            HexGrid publicData,
            Map<String, String> generatorParameters,
            Boolean enabled
    ) {}

    /**
     * Response DTO for hex grid data.
     */
    public record HexGridResponse(
            String id,
            String worldId,
            String position,
            HexGrid publicData,
            Map<String, String> generatorParameters,
            Instant createdAt,
            Instant updatedAt,
            boolean enabled
    ) {}

    private HexGridResponse toResponse(WHexGridEntity hexGrid) {
        return new HexGridResponse(
                hexGrid.getId(),
                hexGrid.getWorldId(),
                hexGrid.getPosition(),
                hexGrid.getPublicData(),
                hexGrid.getGeneratorParameters(),
                hexGrid.getCreatedAt(),
                hexGrid.getUpdatedAt(),
                hexGrid.isEnabled()
        );
    }

    /**
     * List all hex grids in a world
     * GET /control/worlds/{worldId}/hexgrid
     */
    @GetMapping
    public ResponseEntity<?> list(@PathVariable String worldId) {
        var error = validateId(worldId, "worldId");
        if (error != null) return error;

        try {
            List<HexGridResponse> result = hexGridService.findByWorldId(worldId).stream()
                    .map(this::toResponse)
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return bad(e.getMessage());
        }
    }

    /**
     * List only enabled hex grids in a world
     * GET /control/worlds/{worldId}/hexgrid/enabled
     */
    @GetMapping("/enabled")
    public ResponseEntity<?> listEnabled(@PathVariable String worldId) {
        var error = validateId(worldId, "worldId");
        if (error != null) return error;

        try {
            List<HexGridResponse> result = hexGridService.findAllEnabled(worldId).stream()
                    .map(this::toResponse)
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return bad(e.getMessage());
        }
    }

    /**
     * Get hex grid by position (q:r)
     * GET /control/worlds/{worldId}/hexgrid/{q}/{r}
     */
    @GetMapping("/{q}/{r}")
    public ResponseEntity<?> get(
            @PathVariable String worldId,
            @PathVariable int q,
            @PathVariable int r) {

        var error = validateId(worldId, "worldId");
        if (error != null) return error;

        HexVector2 position = HexVector2.builder().q(q).r(r).build();

        return hexGridService.findByWorldIdAndPosition(worldId, position)
                .<ResponseEntity<?>>map(hexGrid -> ResponseEntity.ok(toResponse(hexGrid)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Hex grid not found at position: " + q + ":" + r)));
    }

    /**
     * Create new hex grid
     * POST /control/worlds/{worldId}/hexgrid
     */
    @PostMapping
    public ResponseEntity<?> create(
            @PathVariable String worldId,
            @RequestBody HexGridRequest request) {

        var error = validateId(worldId, "worldId");
        if (error != null) return error;

        if (request.publicData() == null) {
            return bad("publicData is required");
        }

        if (request.publicData().getPosition() == null) {
            return bad("publicData.position is required");
        }

        try {
            WHexGridEntity created = hexGridService.create(
                    worldId,
                    request.publicData(),
                    request.generatorParameters()
            );

            // Apply enabled flag if specified
            if (request.enabled() != null && !request.enabled()) {
                hexGridService.disable(worldId, request.publicData().getPosition());
                created = hexGridService.findByWorldIdAndPosition(worldId, request.publicData().getPosition())
                        .orElseThrow();
            }

            HexVector2 pos = request.publicData().getPosition();
            return ResponseEntity.created(
                    URI.create("/control/worlds/" + worldId + "/hexgrid/" + pos.getQ() + "/" + pos.getR()))
                    .body(toResponse(created));

        } catch (IllegalStateException e) {
            return conflict(e.getMessage());
        } catch (IllegalArgumentException e) {
            return bad(e.getMessage());
        } catch (Exception e) {
            return bad("Failed to create hex grid: " + e.getMessage());
        }
    }

    /**
     * Update hex grid
     * PUT /control/worlds/{worldId}/hexgrid/{q}/{r}
     */
    @PutMapping("/{q}/{r}")
    public ResponseEntity<?> update(
            @PathVariable String worldId,
            @PathVariable int q,
            @PathVariable int r,
            @RequestBody HexGridRequest request) {

        var error = validateId(worldId, "worldId");
        if (error != null) return error;

        HexVector2 position = HexVector2.builder().q(q).r(r).build();

        try {
            var updated = hexGridService.update(worldId, position, hexGrid -> {
                if (request.publicData() != null) {
                    hexGrid.setPublicData(request.publicData());
                }
                if (request.generatorParameters() != null) {
                    hexGrid.setGeneratorParameters(request.generatorParameters());
                }
                if (request.enabled() != null) {
                    hexGrid.setEnabled(request.enabled());
                }
            });

            return updated
                    .<ResponseEntity<?>>map(h -> ResponseEntity.ok(toResponse(h)))
                    .orElseGet(() -> notFound("Hex grid not found at position: " + q + ":" + r));

        } catch (IllegalArgumentException e) {
            return bad(e.getMessage());
        } catch (Exception e) {
            return bad("Failed to update hex grid: " + e.getMessage());
        }
    }

    /**
     * Patch hex grid (partial update)
     * PATCH /control/worlds/{worldId}/hexgrid/{q}/{r}
     */
    @PatchMapping("/{q}/{r}")
    public ResponseEntity<?> patch(
            @PathVariable String worldId,
            @PathVariable int q,
            @PathVariable int r,
            @RequestBody Map<String, Object> updates) {

        var error = validateId(worldId, "worldId");
        if (error != null) return error;

        HexVector2 position = HexVector2.builder().q(q).r(r).build();

        try {
            var updated = hexGridService.update(worldId, position, hexGrid -> {
                // Only update fields that are present in the request
                if (updates.containsKey("generatorParameters")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> params = (Map<String, String>) updates.get("generatorParameters");
                    hexGrid.setGeneratorParameters(params);
                }
                if (updates.containsKey("enabled")) {
                    hexGrid.setEnabled((Boolean) updates.get("enabled"));
                }
                // Note: publicData updates should use PUT for safety
            });

            return updated
                    .<ResponseEntity<?>>map(h -> ResponseEntity.ok(toResponse(h)))
                    .orElseGet(() -> notFound("Hex grid not found at position: " + q + ":" + r));

        } catch (ClassCastException e) {
            return bad("Invalid data type in update: " + e.getMessage());
        } catch (Exception e) {
            return bad("Failed to patch hex grid: " + e.getMessage());
        }
    }

    /**
     * Delete hex grid
     * DELETE /control/worlds/{worldId}/hexgrid/{q}/{r}
     */
    @DeleteMapping("/{q}/{r}")
    public ResponseEntity<?> delete(
            @PathVariable String worldId,
            @PathVariable int q,
            @PathVariable int r) {

        var error = validateId(worldId, "worldId");
        if (error != null) return error;

        HexVector2 position = HexVector2.builder().q(q).r(r).build();

        try {
            boolean deleted = hexGridService.delete(worldId, position);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return notFound("Hex grid not found at position: " + q + ":" + r);
            }
        } catch (Exception e) {
            return bad("Failed to delete hex grid: " + e.getMessage());
        }
    }

    /**
     * Disable hex grid (soft delete)
     * POST /control/worlds/{worldId}/hexgrid/{q}/{r}/disable
     */
    @PostMapping("/{q}/{r}/disable")
    public ResponseEntity<?> disable(
            @PathVariable String worldId,
            @PathVariable int q,
            @PathVariable int r) {

        var error = validateId(worldId, "worldId");
        if (error != null) return error;

        HexVector2 position = HexVector2.builder().q(q).r(r).build();

        try {
            boolean disabled = hexGridService.disable(worldId, position);
            if (disabled) {
                return ResponseEntity.ok(Map.of("message", "Hex grid disabled", "position", q + ":" + r));
            } else {
                return notFound("Hex grid not found at position: " + q + ":" + r);
            }
        } catch (Exception e) {
            return bad("Failed to disable hex grid: " + e.getMessage());
        }
    }

    /**
     * Enable hex grid
     * POST /control/worlds/{worldId}/hexgrid/{q}/{r}/enable
     */
    @PostMapping("/{q}/{r}/enable")
    public ResponseEntity<?> enable(
            @PathVariable String worldId,
            @PathVariable int q,
            @PathVariable int r) {

        var error = validateId(worldId, "worldId");
        if (error != null) return error;

        HexVector2 position = HexVector2.builder().q(q).r(r).build();

        try {
            boolean enabled = hexGridService.enable(worldId, position);
            if (enabled) {
                return ResponseEntity.ok(Map.of("message", "Hex grid enabled", "position", q + ":" + r));
            } else {
                return notFound("Hex grid not found at position: " + q + ":" + r);
            }
        } catch (Exception e) {
            return bad("Failed to enable hex grid: " + e.getMessage());
        }
    }
}
