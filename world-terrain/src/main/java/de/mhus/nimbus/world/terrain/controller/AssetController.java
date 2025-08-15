package de.mhus.nimbus.world.terrain.controller;

import de.mhus.nimbus.shared.dto.world.AssetDto;
import de.mhus.nimbus.shared.dto.world.AssetBatchRequest;
import de.mhus.nimbus.shared.dto.world.AssetCreateRequest;
import de.mhus.nimbus.shared.dto.world.AssetCompressRequest;
import de.mhus.nimbus.world.terrain.service.WorldTerrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final WorldTerrainService worldTerrainService;

    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<AssetDto> createAsset(@RequestBody AssetCreateRequest request) {
        AssetDto created = worldTerrainService.createAsset(request.getWorld(), request);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{world}/{name}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AssetDto> getAsset(
            @PathVariable String world,
            @PathVariable String name) {
        return worldTerrainService.getAsset(world, name)
                .map(asset -> ResponseEntity.ok(asset))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AssetDto>> getAssets(@RequestParam String world) {
        List<AssetDto> assets = worldTerrainService.getAssets(world);
        return ResponseEntity.ok(assets);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AssetDto>> getAssetsBatch(@RequestBody AssetBatchRequest request) {
        List<AssetDto> assets = worldTerrainService.getAssetsBatch(request.getWorld(), request.getAssets());
        return ResponseEntity.ok(assets);
    }
    
    @PutMapping("/{world}/{name}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<AssetDto> updateAsset(
            @PathVariable String world,
            @PathVariable String name,
            @RequestBody AssetCreateRequest request) {
        return worldTerrainService.updateAsset(world, name, request)
                .map(asset -> ResponseEntity.ok(asset))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{world}/{name}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> deleteAsset(
            @PathVariable String world,
            @PathVariable String name) {
        if (worldTerrainService.deleteAsset(world, name)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/compress")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> compressAssets(@RequestBody AssetCompressRequest request) {
        worldTerrainService.compressAssets(request.getWorld(), List.of()); // Empty list since request has no names
        return ResponseEntity.ok().build();
    }
}
