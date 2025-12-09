package de.mhus.nimbus.world.player.session;

import de.mhus.nimbus.world.shared.session.WSession;
import de.mhus.nimbus.world.shared.session.WSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/world/region/{regionId}/session")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RegionSession", description = "Session-Verwaltung für Region-Welten")
public class RegionSessionController {

    private final WSessionService sessionService;

    @PostMapping
    @Operation(summary = "Waiting-Session erstellen", description = "Erzeugt eine neue wartende Sitzung für worldId/userId")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session erstellt"),
        @ApiResponse(responseCode = "400", description = "Validierungsfehler")
    })
    public ResponseEntity<Map<String,Object>> createWaitingSession(@PathVariable String regionId,
                                                                   @RequestParam String worldId,
                                                                   @RequestParam String playerId,
                                                                   @RequestParam(required = false) String characterId) {
        // TODO validate regionId against worldId

        // characterId optional
        WSession session = sessionService.create(worldId, playerId, null);
        return ResponseEntity.ok(Map.of(
                "id", session.getId(),
                "status", session.getStatus().name(),
                "expireAt", session.getExpireAt().toString(),
                "worldId", session.getWorldId()
        ));
    }
}
