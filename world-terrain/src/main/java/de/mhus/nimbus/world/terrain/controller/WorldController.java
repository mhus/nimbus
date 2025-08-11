package de.mhus.nimbus.world.terrain.controller;

import de.mhus.nimbus.shared.dto.world.WorldDto;
import de.mhus.nimbus.world.terrain.service.WorldTerrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/worlds")
@RequiredArgsConstructor
public class WorldController {

    private final WorldTerrainService worldTerrainService;

    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<WorldDto> createWorld(@RequestBody WorldDto worldDto) {
        WorldDto created = worldTerrainService.createWorld(worldDto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WorldDto> getWorld(@PathVariable String id) {
        return worldTerrainService.getWorld(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WorldDto>> getAllWorlds() {
        List<WorldDto> worlds = worldTerrainService.getAllWorlds();
        return ResponseEntity.ok(worlds);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<WorldDto> updateWorld(@PathVariable String id, @RequestBody WorldDto worldDto) {
        return worldTerrainService.updateWorld(id, worldDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> deleteWorld(@PathVariable String id) {
        boolean deleted = worldTerrainService.deleteWorld(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
