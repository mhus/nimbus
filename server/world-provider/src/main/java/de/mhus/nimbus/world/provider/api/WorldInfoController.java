package de.mhus.nimbus.world.provider.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/world/{worldId}")
public class WorldInfoController {

    @GetMapping("/info")
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
