package de.mhus.nimbus.world.terrain.controller;

import de.mhus.nimbus.shared.dto.terrain.AssetDto;
import de.mhus.nimbus.shared.dto.terrain.request.AssetBatchRequest;
import de.mhus.nimbus.world.terrain.service.WorldTerrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetController {
    
    private final WorldTerrainService worldTerrainService;
    
    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<AssetDto> createAsset(@RequestBody AssetDto assetDto) {
        AssetDto created = worldTerrainService.createAsset(assetDto);
        return ResponseEntity.ok(created);
    }
    
    @GetMapping("/{world}/{name}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AssetDto> getAsset(
            @PathVariable String world,
            @PathVariable String name) {
        return worldTerrainService.getAsset(world, name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{world}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<AssetDto>> getAssets(
            @PathVariable String world,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AssetDto> assets = worldTerrainService.getAssets(world, page, size);
        return ResponseEntity.ok(assets);
    }
    
    @PostMapping("/batch")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AssetDto>> getAssetsBatch(@RequestBody AssetBatchRequest request) {
        List<AssetDto> assets = worldTerrainService.getAssetsBatch(request);
        return ResponseEntity.ok(assets);
    }
    
    @PutMapping("/{world}/{name}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<AssetDto> updateAsset(
            @PathVariable String world,
            @PathVariable String name,
            @RequestBody AssetDto assetDto) {
        return worldTerrainService.updateAsset(world, name, assetDto)
                .map(ResponseEntity::ok)
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
    public ResponseEntity<Void> compressAssets(@RequestBody String world) {
        worldTerrainService.compressAssets(world.replaceAll("\"", ""));
        return ResponseEntity.ok().build();
    }
}
