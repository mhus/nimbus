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

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Entity instances in the world (read-only).
 * Returns only publicData from entities.
 */
@RestController
@RequestMapping("/api/world/entities")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Entities", description = "Entity instances placed in worlds (NPCs, players, etc.)")
public class EntityController {

    private final WEntityService service;

    @GetMapping("/{worldId}/{entityId}")
    @Operation(summary = "Get Entity by world and entity ID", description = "Returns Entity instance for a specific entity in a world")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entity found"),
            @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public ResponseEntity<?> getEntity(
            @PathVariable String worldId,
            @PathVariable String entityId) {
        return service.findByWorldIdAndEntityId(worldId, entityId)
                .map(WEntity::getPublicData)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all Entities", description = "Returns all enabled Entity instances, filtered by world or chunk")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of Entities"),
            @ApiResponse(responseCode = "400", description = "worldId required")
    })
    public ResponseEntity<?> getAllEntities(
            @RequestParam(required = false) String worldId,
            @RequestParam(required = false) String chunk,
            @RequestParam(required = false) String modelId) {

        // worldId is typically required for meaningful queries
        if (worldId == null || worldId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "worldId required"));
        }

        List<Entity> entities;

        if (chunk != null && !chunk.isBlank()) {
            // Filter by world and chunk (most common for streaming)
            entities = service.findByWorldIdAndChunk(worldId, chunk).stream()
                    .filter(WEntity::isEnabled)
                    .map(WEntity::getPublicData)
                    .toList();
        } else if (modelId != null && !modelId.isBlank()) {
            // Filter by modelId (all entities of a specific type)
            entities = service.findByModelId(modelId).stream()
                    .filter(WEntity::isEnabled)
                    .filter(e -> worldId.equals(e.getWorldId()))
                    .map(WEntity::getPublicData)
                    .toList();
        } else {
            // All entities in world
            entities = service.findByWorldId(worldId).stream()
                    .filter(WEntity::isEnabled)
                    .map(WEntity::getPublicData)
                    .toList();
        }

        return ResponseEntity.ok(Map.of(
                "entities", entities,
                "count", entities.size(),
                "worldId", worldId
        ));
    }
}
