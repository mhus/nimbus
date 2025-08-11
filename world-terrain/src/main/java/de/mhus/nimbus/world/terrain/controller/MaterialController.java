package de.mhus.nimbus.world.terrain.controller;

import de.mhus.nimbus.shared.dto.terrain.MaterialDto;
import de.mhus.nimbus.world.terrain.service.WorldTerrainService;
import de.mhus.nimbus.world.shared.filter.WorldAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final WorldTerrainService worldTerrainService;

    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<MaterialDto> createMaterial(@RequestBody MaterialDto materialDto) {
        MaterialDto created = worldTerrainService.createMaterial(materialDto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MaterialDto> getMaterial(@PathVariable Integer id) {
        return worldTerrainService.getMaterial(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<MaterialDto>> getMaterials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<MaterialDto> materials = worldTerrainService.getMaterials(page, size);
        return ResponseEntity.ok(materials);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<MaterialDto> updateMaterial(
            @PathVariable Integer id,
            @RequestBody MaterialDto materialDto) {
        return worldTerrainService.updateMaterial(id, materialDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Integer id) {
        if (worldTerrainService.deleteMaterial(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
