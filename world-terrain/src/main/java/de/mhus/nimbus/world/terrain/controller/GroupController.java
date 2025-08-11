package de.mhus.nimbus.world.terrain.controller;

import de.mhus.nimbus.shared.dto.terrain.GroupDto;
import de.mhus.nimbus.world.terrain.service.WorldTerrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final WorldTerrainService worldTerrainService;

    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<GroupDto> createGroup(
            @RequestParam String world,
            @RequestBody GroupDto groupDto) {
        GroupDto created = worldTerrainService.createGroup(world, groupDto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{world}/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<GroupDto> getGroup(
            @PathVariable String world,
            @PathVariable Long id) {
        return worldTerrainService.getGroup(world, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{world}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<GroupDto>> getGroups(@PathVariable String world) {
        List<GroupDto> groups = worldTerrainService.getGroups(world);
        return ResponseEntity.ok(groups);
    }

    @PutMapping("/{world}/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<GroupDto> updateGroup(
            @PathVariable String world,
            @PathVariable Long id,
            @RequestBody GroupDto groupDto) {
        return worldTerrainService.updateGroup(world, id, groupDto)
                .map(ResponseEntity::ok)
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
