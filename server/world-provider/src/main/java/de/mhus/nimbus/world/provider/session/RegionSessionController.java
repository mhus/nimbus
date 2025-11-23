package de.mhus.nimbus.world.provider.session;

import de.mhus.nimbus.world.shared.session.WSession;
import de.mhus.nimbus.world.shared.session.WSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/world/region/{regionId}/session")
@RequiredArgsConstructor
@Slf4j
public class RegionSessionController {

    private final WSessionService sessionService;

    @PostMapping
    public ResponseEntity<Map<String,Object>> createWaitingSession(@PathVariable String regionId,
                                                                   @RequestParam String worldId,
                                                                   @RequestParam String userId,
                                                                   @RequestParam(required = false) String characterId) {
        // characterId optional
        WSession session = sessionService.create(worldId, regionId, userId, characterId, null);
        return ResponseEntity.ok(Map.of(
                "id", session.getId(),
                "status", session.getStatus().name(),
                "expireAt", session.getExpireAt().toString(),
                "worldId", session.getWorldId(),
                "regionId", session.getRegionId()
        ));
    }
}

