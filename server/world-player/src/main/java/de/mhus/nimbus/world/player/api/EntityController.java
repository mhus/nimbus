package de.mhus.nimbus.world.player.api;

import de.mhus.nimbus.generated.types.Entity;
import de.mhus.nimbus.shared.types.PlayerId;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.player.service.PlayerService;
import de.mhus.nimbus.world.shared.world.WEntity;
import de.mhus.nimbus.world.shared.world.WEntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Entity instances in the world (read-only).
 * Returns only publicData from entities.
 */
@RestController
@RequestMapping("/player/world/entities")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Entities", description = "Entity instances placed in worlds (NPCs, players, etc.)")
public class EntityController {

    private final WEntityService service;
    private final PlayerService playerService;

    @GetMapping("/{worldId}/{entityId}")
    @Operation(summary = "Get Entity by world and entity ID", description = "Returns Entity instance for a specific entity in a world")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entity found"),
            @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public ResponseEntity<?> getEntity(
            @PathVariable String worldId,
            @PathVariable String entityId) {

        if (Strings.isBlank(entityId)) {
            return ResponseEntity.badRequest().body("entityId is required");
        }
        if (entityId.startsWith("@")) {
            var playerId = PlayerId.of(entityId);
            if (playerId.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid player ID");
            }
            var playerEntity = playerService.getPlayerAsEntity(playerId.get(), worldId);
            return playerEntity
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }

        return WorldId.of(worldId)
                .map(wid -> service.findByWorldIdAndEntityId(wid, entityId)
                        .map(WEntity::getPublicData)
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build()))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

}
