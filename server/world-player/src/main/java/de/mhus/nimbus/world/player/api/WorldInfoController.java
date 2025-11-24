package de.mhus.nimbus.world.player.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/world/{worldId}")
@Tag(name = "WorldInfo", description = "Infos zu einer World im world-provider")
public class WorldInfoController {

    @GetMapping("/info")
    @Operation(summary = "World Info abrufen", description = "Liefert Basisinformationen und aktuellen Timestamp")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "worldId missing")
    })
    public ResponseEntity<Map<String,Object>> info(@PathVariable String worldId) {
        if (worldId == null || worldId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error","worldId missing"));
        }
        return ResponseEntity.ok(Map.of(
                "service","world-provider",
                "worldId", worldId,
                "timestamp", Instant.now().toString()
        ));
    }
}
