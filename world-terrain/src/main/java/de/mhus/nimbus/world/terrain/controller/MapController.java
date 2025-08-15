package de.mhus.nimbus.world.terrain.controller;

import de.mhus.nimbus.shared.dto.world.*;
import de.mhus.nimbus.world.terrain.service.WorldTerrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
public class MapController {

    private final WorldTerrainService worldTerrainService;

    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> createMap(@RequestBody MapCreateRequest request) {
        worldTerrainService.createMap(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{x}/{y}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TerrainClusterDto> getMapCluster(
            @PathVariable Integer x,
            @PathVariable Integer y,
            @RequestParam String world,
            @RequestParam Integer level) {

        return worldTerrainService.getMap(world, level, x, y)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TerrainClusterDto>> getMapClusters(@RequestBody MapBatchRequest request) {
        List<TerrainClusterDto> clusters = worldTerrainService.getMapBatch(request);
        return ResponseEntity.ok(clusters);
    }

    @PutMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> updateMap(@RequestBody MapCreateRequest request) {
        worldTerrainService.updateMap(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> deleteMapFields(@RequestBody MapDeleteRequest request) {
        // TODO: Implement deleteMapFields method in service
        return ResponseEntity.status(501).build(); // 501 Not Implemented
    }

    @DeleteMapping("/level")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> deleteLevel(@RequestParam String world, @RequestParam Integer level) {
        worldTerrainService.deleteMapLevel(world, level);
        return ResponseEntity.ok().build();
    }
}
