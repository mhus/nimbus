package de.mhus.nimbus.world.terrain.controller;

import de.mhus.nimbus.shared.dto.world.TerrainGroupDto;
import de.mhus.nimbus.world.terrain.service.WorldTerrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final WorldTerrainService worldTerrainService;

    @PostMapping("/{world}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<TerrainGroupDto> createGroup(
            @PathVariable String world,
            @RequestBody TerrainGroupDto groupDto) {
        TerrainGroupDto created = worldTerrainService.createGroup(world, groupDto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{world}/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TerrainGroupDto> getGroup(
            @PathVariable String world,
            @PathVariable Long id) {
        return worldTerrainService.getGroup(world, id)
                .map(group -> ResponseEntity.ok(group))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{world}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TerrainGroupDto>> getGroups(@PathVariable String world) {
        List<TerrainGroupDto> groups = worldTerrainService.getGroups(world);
        return ResponseEntity.ok(groups);
    }

    @PutMapping("/{world}/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<TerrainGroupDto> updateGroup(
            @PathVariable String world,
            @PathVariable Long id,
            @RequestBody TerrainGroupDto groupDto) {
        return worldTerrainService.updateGroup(world, id, groupDto)
                .map(group -> ResponseEntity.ok(group))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{world}/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable String world,
            @PathVariable Long id) {
        if (worldTerrainService.deleteGroup(world, id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
