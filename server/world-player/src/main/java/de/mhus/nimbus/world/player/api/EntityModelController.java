package de.mhus.nimbus.world.player.api;

import de.mhus.nimbus.generated.types.EntityModel;
import de.mhus.nimbus.world.shared.world.WEntityModel;
import de.mhus.nimbus.world.shared.world.WEntityModelService;
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
 * REST Controller for EntityModel templates (read-only).
 * Returns only publicData from entities.
 */
@RestController
@RequestMapping("/api/worlds/{worldId}/entitymodel")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "EntityModels", description = "EntityModel templates for 3D models and animations")
public class EntityModelController {

    private final WEntityModelService service;

    @GetMapping("/{modelId}")
    @Operation(summary = "Get EntityModel by ID", description = "Returns EntityModel template for a specific model ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "EntityModel found"),
            @ApiResponse(responseCode = "404", description = "EntityModel not found")
    })
    public ResponseEntity<?> getEntityModel(
            @PathVariable String worldId,
            @PathVariable String modelId) {
        return service.findByModelId(modelId)
                .map(WEntityModel::getPublicData)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all EntityModels", description = "Returns all enabled EntityModel templates, optionally filtered")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of EntityModels")
    })
    public ResponseEntity<?> getAllEntityModels(
            @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String worldId) {

        List<EntityModel> entityModels;

        if (regionId != null && !regionId.isBlank()) {
            entityModels = service.findByRegionId(regionId).stream()
                    .filter(WEntityModel::isEnabled)
                    .map(WEntityModel::getPublicData)
                    .toList();
        } else if (worldId != null && !worldId.isBlank()) {
            entityModels = service.findByWorldId(worldId).stream()
                    .filter(WEntityModel::isEnabled)
                    .map(WEntityModel::getPublicData)
                    .toList();
        } else {
            entityModels = service.findAllEnabled().stream()
                    .map(WEntityModel::getPublicData)
                    .toList();
        }

        return ResponseEntity.ok(Map.of(
                "entityModels", entityModels,
                "count", entityModels.size()
        ));
    }
}
