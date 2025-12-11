package de.mhus.nimbus.world.player.api;

import de.mhus.nimbus.generated.types.Backdrop;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.shared.world.WBackdrop;
import de.mhus.nimbus.world.shared.world.WBackdropService;
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
 * REST Controller for Backdrop configurations (read-only).
 * Returns only publicData from entities.
 */
@RestController
@RequestMapping("/api/worlds/{worldId}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Backdrops", description = "Backdrop configurations for visual effects at chunk boundaries")
public class BackdropController {

    private final WBackdropService service;

    @GetMapping("/backdrop/{backdropId}")
    @Operation(summary = "Get Backdrop by ID", description = "Returns Backdrop configuration for a specific backdrop ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Backdrop found"),
            @ApiResponse(responseCode = "400", description = "Invalid worldId"),
            @ApiResponse(responseCode = "404", description = "Backdrop not found")
    })
    public ResponseEntity<?> getBackdrop(
            @PathVariable String worldId,
            @PathVariable String backdropId) {
        return WorldId.of(worldId)
                .map(wid -> service.findByBackdropId(wid, backdropId)
                        .map(WBackdrop::getPublicData)
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build()))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping("/backdrops")
    @Operation(summary = "Get all Backdrops", description = "Returns all enabled Backdrop configurations")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of Backdrops"),
            @ApiResponse(responseCode = "400", description = "Invalid worldId")
    })
    public ResponseEntity<?> getAllBackdrops(
            @PathVariable String worldId) {

        return WorldId.of(worldId)
                .map(wid -> {
                    List<Backdrop> backdrops = service.findAllEnabled(wid).stream()
                            .map(WBackdrop::getPublicData)
                            .toList();

                    return ResponseEntity.ok(Map.of(
                            "backdrops", backdrops,
                            "count", backdrops.size()
                    ));
                })
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }
}
