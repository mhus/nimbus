package de.mhus.nimbus.world.terrain.controller;

import de.mhus.nimbus.shared.dto.world.SpriteDto;
import de.mhus.nimbus.shared.dto.world.SpriteCreateRequest;
import de.mhus.nimbus.shared.dto.world.SpriteCoordinateUpdateRequest;
import de.mhus.nimbus.world.terrain.service.WorldTerrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprites")
@RequiredArgsConstructor
public class SpriteController {

    private final WorldTerrainService worldTerrainService;

    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<List<String>> createSprites(@RequestBody SpriteCreateRequest request) {
        List<String> spriteIds = worldTerrainService.createSprites(request);
        return ResponseEntity.ok(spriteIds);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SpriteDto> getSprite(@PathVariable String id) {
        return worldTerrainService.getSprite(id)
                .map(sprite -> ResponseEntity.ok(sprite))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cluster")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SpriteDto>> getSpritesInCluster(
            @RequestParam String world,
            @RequestParam Integer level,
            @RequestParam Integer x,
            @RequestParam Integer y) {
        List<SpriteDto> sprites = worldTerrainService.getSpritesInCluster(world, level, x, y);
        return ResponseEntity.ok(sprites);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<SpriteDto> updateSprite(
            @PathVariable String id,
            @RequestBody SpriteDto spriteDto) {
        return worldTerrainService.updateSprite(id, spriteDto)
                .map(sprite -> ResponseEntity.ok(sprite))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> deleteSprite(@PathVariable String id) {
        if (worldTerrainService.deleteSprite(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/coordinates")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<SpriteDto> updateSpriteCoordinates(
            @PathVariable String id,
            @RequestBody SpriteCoordinateUpdateRequest request) {
        return worldTerrainService.updateSpriteCoordinates(id, request)
                .map(sprite -> ResponseEntity.ok(sprite))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<SpriteDto> enableSprite(@PathVariable String id) {
        return worldTerrainService.enableSprite(id)
                .map(sprite -> ResponseEntity.ok(sprite))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<SpriteDto> disableSprite(@PathVariable String id) {
        return worldTerrainService.disableSprite(id)
                .map(sprite -> ResponseEntity.ok(sprite))
                .orElse(ResponseEntity.notFound().build());
    }
}
