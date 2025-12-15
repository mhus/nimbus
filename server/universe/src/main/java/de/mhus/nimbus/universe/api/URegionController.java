package de.mhus.nimbus.universe.api;

import de.mhus.nimbus.shared.dto.universe.URegionRequest;
import de.mhus.nimbus.shared.dto.universe.URegionResponse;
import de.mhus.nimbus.universe.region.URegion;
import de.mhus.nimbus.universe.region.URegionService;
import de.mhus.nimbus.universe.security.CurrentUser;
import de.mhus.nimbus.universe.security.RequestUserHolder;
import de.mhus.nimbus.universe.security.Role;
import de.mhus.nimbus.shared.user.UniverseRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(URegionController.BASE_PATH)
//@SecurityRequirement(name = "bearerAuth")
@Tag(name = "URegion", description = "Manage regions")
@RequiredArgsConstructor
public class URegionController {

    public static final String BASE_PATH = "/universe/region";

    private final URegionService service;
    private final RequestUserHolder userHolder;

    private URegionResponse toResponse(URegion q) {
        // Maintainer Set -> List
        return new URegionResponse(q.getId(), q.getName(), q.getApiUrl(), q.getMaintainerSet().stream().toList());
    }

    private CurrentUser current() { return userHolder.get(); }
    private boolean hasRole(UniverseRoles role) { return current()!=null && current().user()!=null && current().user().hasRole(role); }

    private boolean canMaintain(URegion q) {
        if (hasRole(UniverseRoles.ADMIN)) return true;
        if (hasRole(UniverseRoles.SUPPORT)) {
            return q != null && q.hasMaintainer(current().userId());
        }
        return false;
    }
    private boolean canCreateAsMaintainer(String maintainersRaw) {
        if (hasRole(UniverseRoles.ADMIN)) return true;
        if (hasRole(UniverseRoles.SUPPORT)) {
            if (maintainersRaw == null || maintainersRaw.isBlank()) return false; // muss sich selbst eintragen
            var set = java.util.Arrays.stream(maintainersRaw.split(",")).map(String::trim).filter(s->!s.isEmpty()).collect(Collectors.toSet());
            return set.contains(current().userId());
        }
        return false;
    }

    @Operation(summary = "List Regions")
    @ApiResponse(responseCode = "200", description = "List returned")
    @GetMapping
    @Role({UniverseRoles.USER, UniverseRoles.ADMIN, UniverseRoles.SUPPORT})
    public ResponseEntity<List<URegionResponse>> list() {
        List<URegionResponse> list = service.listAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Get Region")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Found"), @ApiResponse(responseCode = "404", description = "Not found")})
    @GetMapping("/{id}")
    @Role({UniverseRoles.USER, UniverseRoles.ADMIN, UniverseRoles.SUPPORT})
    public ResponseEntity<URegionResponse> get(@PathVariable String id) {
        return service.getById(id)
                .map(q -> ResponseEntity.ok(toResponse(q)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create Region")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Created"), @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "400", description = "Validation error")})
    @PostMapping
//    @Role({UniverseRoles.ADMIN, UniverseRoles.MAINTAINER})
    public ResponseEntity<URegionResponse> create(@RequestBody URegionRequest req) {
//        if (current()==null) return ResponseEntity.status(401).build();
//        if (!canCreateAsMaintainer(req.maintainers())) return ResponseEntity.status(403).build();
        URegion q = service.create(req.name(), req.apiUrl(), req.maintainers(), req.publicSignKey());
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + q.getId())).body(toResponse(q));
    }

    @Operation(summary = "Update Region")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Updated"), @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")})
    @PutMapping("/{id}")
    @Role({UniverseRoles.ADMIN, UniverseRoles.SUPPORT})
    public ResponseEntity<URegionResponse> update(@PathVariable String id, @RequestBody URegionRequest req) {
        URegion existing = service.getById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (!canMaintain(existing)) return ResponseEntity.status(403).build();
        URegion updated = service.update(id, req.name(), req.apiUrl(), req.maintainers());
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(summary = "Delete Region")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Deleted"), @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")})
    @DeleteMapping("/{id}")
    @Role({UniverseRoles.ADMIN, UniverseRoles.SUPPORT})
    public ResponseEntity<Void> delete(@PathVariable String id) {
        URegion existing = service.getById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (!canMaintain(existing)) return ResponseEntity.status(403).build();
        service.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Add maintainer")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Maintainer added"), @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")})
    @PostMapping("/{id}/maintainers/{userId}")
    @Role({UniverseRoles.ADMIN, UniverseRoles.SUPPORT})
    public ResponseEntity<URegionResponse> addMaintainer(@PathVariable String id, @PathVariable String userId) {
        URegion existing = service.getById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (!canMaintain(existing)) return ResponseEntity.status(403).build();
        URegion updated = service.addMaintainer(id, userId);
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(summary = "Remove maintainer")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Maintainer removed"), @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")})
    @DeleteMapping("/{id}/maintainers/{userId}")
    @Role({UniverseRoles.ADMIN, UniverseRoles.SUPPORT})
    public ResponseEntity<URegionResponse> removeMaintainer(@PathVariable String id, @PathVariable String userId) {
        URegion existing = service.getById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (!canMaintain(existing)) return ResponseEntity.status(403).build();
        URegion updated = service.removeMaintainer(id, userId);
        return ResponseEntity.ok(toResponse(updated));
    }
}
