package de.mhus.nimbus.world.terrain.controller;

import de.mhus.nimbus.shared.dto.world.MaterialDto;
import de.mhus.nimbus.world.terrain.service.WorldTerrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/materials")
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

        if (size > 100) size = 100; // Maximum size limit

        Pageable pageable = PageRequest.of(page, size);
        Page<MaterialDto> materials = worldTerrainService.getMaterials(pageable);
        return ResponseEntity.ok(materials);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<MaterialDto> updateMaterial(@PathVariable Integer id, @RequestBody MaterialDto materialDto) {
        return worldTerrainService.updateMaterial(id, materialDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Integer id) {
        boolean deleted = worldTerrainService.deleteMaterial(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
