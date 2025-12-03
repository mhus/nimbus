package de.mhus.nimbus.world.player.api;

import de.mhus.nimbus.generated.types.Entity;
import de.mhus.nimbus.world.shared.world.WEntity;
import de.mhus.nimbus.world.shared.world.WEntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for entity lookup using alternative URL pattern.
 * Matches client URL: GET /api/worlds/{worldId}/entity/{entityId}
 */
@RestController
@RequestMapping("/api/worlds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "World Entities", description = "Entity instances (alternative URL pattern)")
public class WorldEntityController {

    private final WEntityService service;

    /**
     * Get entity by ID.
     * URL: GET /api/worlds/{worldId}/entity/{entityId}
     */
    @GetMapping("/{worldId}/entity/{entityId}")
    @Operation(summary = "Get Entity by ID", description = "Returns Entity instance for a specific entity")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entity found"),
            @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public ResponseEntity<Entity> getEntity(
            @PathVariable String worldId,
            @PathVariable String entityId) {

        log.debug("GET /api/worlds/{}/entity/{}", worldId, entityId);

        return service.findByWorldIdAndEntityId(worldId, entityId)
                .map(WEntity::getPublicData)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.debug("Entity not found: worldId={}, entityId={}", worldId, entityId);
                    return ResponseEntity.notFound().build();
                });
    }
}
