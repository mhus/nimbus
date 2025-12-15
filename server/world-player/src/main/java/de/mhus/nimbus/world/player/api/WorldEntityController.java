package de.mhus.nimbus.world.player.api;

import de.mhus.nimbus.generated.types.Entity;
import de.mhus.nimbus.generated.types.PlayerInfo;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.player.service.PlayerService;
import de.mhus.nimbus.world.player.session.PlayerSession;
import de.mhus.nimbus.world.player.ws.SessionManager;
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
 * Matches client URL: GET /player/worlds/{worldId}/entity/{entityId}
 *
 * Searches in:
 * 1. Active player sessions (for player entities like "@ecb:blade")
 * 2. Database (for persistent NPC entities)
 */
@RestController
@RequestMapping("/player/worlds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "World Entities", description = "Entity instances (alternative URL pattern)")
public class WorldEntityController {

    private final WEntityService service;
    private final SessionManager sessionManager;
    private final PlayerService playerService;

    /**
     * Get entity by ID.
     * URL: GET /player/worlds/{worldId}/entity/{entityId}
     *
     * Searches for:
     * 1. Player entities in active sessions (entityId starts with "@")
     * 2. Persistent NPC entities in database
     */
    @GetMapping("/{worldId}/entity/{entityId}")
    @Operation(summary = "Get Entity by ID", description = "Returns Entity instance for a specific entity (player or NPC)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entity found"),
            @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public ResponseEntity<Entity> getEntity(
            @PathVariable String worldId,
            @PathVariable String entityId) {

        log.debug("GET /player/worlds/{}/entity/{}", worldId, entityId);

        // Check if this is a player entity (starts with "@")
        if (entityId.startsWith("@")) {
            // Search in active sessions
            var entityX = playerService.getPlayerAsEntity(de.mhus.nimbus.shared.types.PlayerId.of(entityId).get(), worldId);
            if (entityX.isPresent()) {
                log.debug("Found player entity in active sessions: {}", entityId);
                return ResponseEntity.ok(entityX.get());
            }
            log.debug("Player entity not found in active sessions: {}", entityId);
            return ResponseEntity.notFound().build();
        }

        // Not a player entity or not found in sessions - search database
        return WorldId.of(worldId)
                .map(wid -> service.findByWorldIdAndEntityId(wid, entityId)
                        .map(WEntity::getPublicData)
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> {
                            log.debug("Entity not found: worldId={}, entityId={}", worldId, entityId);
                            return ResponseEntity.notFound().build();
                        }))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

}
