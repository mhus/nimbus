package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.generated.types.Backdrop;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.shared.world.WBackdrop;
import de.mhus.nimbus.world.shared.world.WBackdropService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for Backdrop CRUD operations.
 * Base path: /api/worlds/{worldId}/backdrop
 * <p>
 * Backdrops are visual elements rendered at chunk boundaries (fog, sky, etc.).
 */
@RestController
@RequestMapping("/api/worlds/{worldId}/backdrop")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Backdrops", description = "Backdrop configuration management")
public class EBackdropController extends BaseEditorController {

    private final WBackdropService backdropService;

    // DTOs
    public record BackdropDto(
            String backdropId,
            Backdrop publicData,
            String worldId,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CreateBackdropRequest(String backdropId, Backdrop publicData) {
    }

    public record UpdateBackdropRequest(Backdrop publicData, Boolean enabled) {
    }

    /**
     * Get single backdrop by ID.
     * GET /api/worlds/{worldId}/backdrop/{backdropId}
     */
    @GetMapping("/{backdropId}")
    @Operation(summary = "Get backdrop by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Backdrop found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Backdrop not found")
    })
    public ResponseEntity<?> get(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Backdrop identifier") @PathVariable String backdropId) {

        log.debug("GET backdrop: worldId={}, backdropId={}", worldId, backdropId);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(backdropId, "backdropId");
        if (validation != null) return validation;

        Optional<WBackdrop> opt = WorldId.of(worldId)
                .flatMap(wid -> backdropService.findByBackdropId(wid, backdropId));
        if (opt.isEmpty()) {
            log.warn("Backdrop not found: backdropId={}", backdropId);
            return notFound("backdrop not found");
        }

        log.debug("Returning backdrop: backdropId={}", backdropId);
        // Return publicData only (match test_server format)
        return ResponseEntity.ok(opt.get().getPublicData());
    }

    /**
     * List all backdrops for a world with optional search filter and pagination.
     * GET /api/worlds/{worldId}/backdrop?query=...&offset=0&limit=50
     */
    @GetMapping
    @Operation(summary = "List all backdrops")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<?> list(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Search query") @RequestParam(required = false) String query,
            @Parameter(description = "Pagination offset") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "Pagination limit") @RequestParam(defaultValue = "50") int limit) {

        log.debug("LIST backdrops: worldId={}, query={}, offset={}, limit={}", worldId, query, offset, limit);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validatePagination(offset, limit);
        if (validation != null) return validation;

        // Get all backdrops for this world
        Optional<WorldId> widOpt = WorldId.of(worldId);
        if (widOpt.isEmpty()) {
            return bad("invalid worldId");
        }
        List<WBackdrop> all = backdropService.findByWorldId(widOpt.get());

        // Apply search filter if provided
        if (query != null && !query.isBlank()) {
            all = filterByQuery(all, query);
        }

        int totalCount = all.size();

        // Apply pagination
        List<Backdrop> publicDataList = all.stream()
                .skip(offset)
                .limit(limit)
                .map(WBackdrop::getPublicData)
                .collect(Collectors.toList());

        log.debug("Returning {} backdrops (total: {})", publicDataList.size(), totalCount);

        // TypeScript compatible format (match test_server response)
        return ResponseEntity.ok(Map.of(
                "backdrops", publicDataList,
                "count", totalCount,
                "limit", limit,
                "offset", offset
        ));
    }

    /**
     * Create new backdrop.
     * POST /api/worlds/{worldId}/backdrop
     */
    @PostMapping
    @Operation(summary = "Create new backdrop")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Backdrop created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Backdrop already exists")
    })
    public ResponseEntity<?> create(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @RequestBody CreateBackdropRequest request) {

        log.debug("CREATE backdrop: worldId={}, backdropId={}", worldId, request.backdropId());

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(request.backdropId())) {
            return bad("backdropId required");
        }

        if (request.publicData() == null) {
            return bad("publicData required");
        }

        Optional<WorldId> widOpt = WorldId.of(worldId);
        if (widOpt.isEmpty()) {
            return bad("invalid worldId");
        }
        WorldId wid = widOpt.get();

        // Check if backdrop already exists
        if (backdropService.findByBackdropId(wid, request.backdropId()).isPresent()) {
            return conflict("backdrop already exists");
        }

        try {
            WBackdrop saved = backdropService.save(wid, request.backdropId(), request.publicData());
            log.info("Created backdrop: backdropId={}", request.backdropId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating backdrop: {}", e.getMessage());
            return bad(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating backdrop", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update existing backdrop.
     * PUT /api/worlds/{worldId}/backdrop/{backdropId}
     */
    @PutMapping("/{backdropId}")
    @Operation(summary = "Update backdrop")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Backdrop updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Backdrop not found")
    })
    public ResponseEntity<?> update(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Backdrop identifier") @PathVariable String backdropId,
            @RequestBody UpdateBackdropRequest request) {

        log.debug("UPDATE backdrop: worldId={}, backdropId={}", worldId, backdropId);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(backdropId, "backdropId");
        if (validation != null) return validation;

        if (request.publicData() == null && request.enabled() == null) {
            return bad("at least one field required for update");
        }

        Optional<WorldId> widOpt = WorldId.of(worldId);
        if (widOpt.isEmpty()) {
            return bad("invalid worldId");
        }

        Optional<WBackdrop> updated = backdropService.update(widOpt.get(), backdropId, backdrop -> {
            if (request.publicData() != null) {
                backdrop.setPublicData(request.publicData());
            }
            if (request.enabled() != null) {
                backdrop.setEnabled(request.enabled());
            }
        });

        if (updated.isEmpty()) {
            log.warn("Backdrop not found for update: backdropId={}", backdropId);
            return notFound("backdrop not found");
        }

        log.info("Updated backdrop: backdropId={}", backdropId);
        return ResponseEntity.ok(toDto(updated.get()));
    }

    /**
     * Delete backdrop.
     * DELETE /api/worlds/{worldId}/backdrop/{backdropId}
     */
    @DeleteMapping("/{backdropId}")
    @Operation(summary = "Delete backdrop")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Backdrop deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Backdrop not found")
    })
    public ResponseEntity<?> delete(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Backdrop identifier") @PathVariable String backdropId) {

        log.debug("DELETE backdrop: worldId={}, backdropId={}", worldId, backdropId);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(backdropId, "backdropId");
        if (validation != null) return validation;

        Optional<WorldId> widOpt = WorldId.of(worldId);
        if (widOpt.isEmpty()) {
            return bad("invalid worldId");
        }

        boolean deleted = backdropService.delete(widOpt.get(), backdropId);
        if (!deleted) {
            log.warn("Backdrop not found for deletion: backdropId={}", backdropId);
            return notFound("backdrop not found");
        }

        log.info("Deleted backdrop: backdropId={}", backdropId);
        return ResponseEntity.noContent().build();
    }

    // Helper methods

    private BackdropDto toDto(WBackdrop entity) {
        return new BackdropDto(
                entity.getBackdropId(),
                entity.getPublicData(),
                entity.getWorldId(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private List<WBackdrop> filterByQuery(List<WBackdrop> backdrops, String query) {
        String lowerQuery = query.toLowerCase();
        return backdrops.stream()
                .filter(backdrop -> {
                    String backdropId = backdrop.getBackdropId();
                    return (backdropId != null && backdropId.toLowerCase().contains(lowerQuery));
                })
                .collect(Collectors.toList());
    }
}
