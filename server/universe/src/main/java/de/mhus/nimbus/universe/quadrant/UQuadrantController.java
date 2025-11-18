package de.mhus.nimbus.universe.quadrant;

import de.mhus.nimbus.universe.security.CurrentUser;
import de.mhus.nimbus.universe.security.RequestUserHolder;
import de.mhus.nimbus.universe.security.Role;
import de.mhus.nimbus.shared.user.UniverseRoles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(UQuadrantController.BASE_PATH)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "UQuadrants", description = "Manage quadrants")
public class UQuadrantController {

    public static final String BASE_PATH = "/universe/quadrant";

    private final UQuadrantService service;
    private final RequestUserHolder userHolder;

    public UQuadrantController(UQuadrantService service, RequestUserHolder userHolder) {
        this.service = service;
        this.userHolder = userHolder;
    }

    // DTOs
    public record UQuadrantRequest(String name, String apiUrl, String publicSignKey, String maintainers) {}
    public record UQuadrantResponse(String id, String name, String apiUrl, String publicSignKey, List<String> maintainers) {}

    private UQuadrantResponse toResponse(UQuadrant q) {
        // Maintainer Set -> List
        return new UQuadrantResponse(q.getId(), q.getName(), q.getApiUrl(), q.getPublicSignKey(), q.getMaintainerSet().stream().toList());
    }

    private CurrentUser current() { return userHolder.get(); }
    private boolean hasRole(UniverseRoles role) { return current()!=null && current().user()!=null && current().user().hasRole(role); }

    private boolean canMaintain(UQuadrant q) {
        if (hasRole(UniverseRoles.ADMIN)) return true;
        if (hasRole(UniverseRoles.MAINTAINER)) {
            return q != null && q.hasMaintainer(current().userId());
        }
        return false;
    }
    private boolean canCreateAsMaintainer(String maintainersRaw) {
        if (hasRole(UniverseRoles.ADMIN)) return true;
        if (hasRole(UniverseRoles.MAINTAINER)) {
            if (maintainersRaw == null || maintainersRaw.isBlank()) return false; // muss sich selbst eintragen
            var set = java.util.Arrays.stream(maintainersRaw.split(",")).map(String::trim).filter(s->!s.isEmpty()).collect(Collectors.toSet());
            return set.contains(current().userId());
        }
        return false;
    }

    @Operation(summary = "List quadrants")
    @ApiResponse(responseCode = "200", description = "List returned")
    @GetMapping
    @Role({UniverseRoles.USER, UniverseRoles.ADMIN, UniverseRoles.MAINTAINER})
    public ResponseEntity<List<UQuadrantResponse>> list() {
        List<UQuadrantResponse> list = service.listAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Get quadrant")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Found"), @ApiResponse(responseCode = "404", description = "Not found")})
    @GetMapping("/{id}")
    @Role({UniverseRoles.USER, UniverseRoles.ADMIN, UniverseRoles.MAINTAINER})
    public ResponseEntity<UQuadrantResponse> get(@PathVariable String id) {
        return service.getById(id)
                .map(q -> ResponseEntity.ok(toResponse(q)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create quadrant")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Created"), @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "400", description = "Validation error")})
    @PostMapping
    @Role({UniverseRoles.ADMIN, UniverseRoles.MAINTAINER})
    public ResponseEntity<UQuadrantResponse> create(@RequestBody UQuadrantRequest req) {
        if (current()==null) return ResponseEntity.status(401).build();
        if (!canCreateAsMaintainer(req.maintainers())) return ResponseEntity.status(403).build();
        UQuadrant q = service.create(req.name(), req.apiUrl(), req.publicSignKey(), req.maintainers());
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + q.getId())).body(toResponse(q));
    }

    @Operation(summary = "Update quadrant")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Updated"), @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")})
    @PutMapping("/{id}")
    @Role({UniverseRoles.ADMIN, UniverseRoles.MAINTAINER})
    public ResponseEntity<UQuadrantResponse> update(@PathVariable String id, @RequestBody UQuadrantRequest req) {
        UQuadrant existing = service.getById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (!canMaintain(existing)) return ResponseEntity.status(403).build();
        UQuadrant updated = service.update(id, req.name(), req.apiUrl(), req.publicSignKey(), req.maintainers());
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(summary = "Delete quadrant")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Deleted"), @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")})
    @DeleteMapping("/{id}")
    @Role({UniverseRoles.ADMIN, UniverseRoles.MAINTAINER})
    public ResponseEntity<Void> delete(@PathVariable String id) {
        UQuadrant existing = service.getById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (!canMaintain(existing)) return ResponseEntity.status(403).build();
        service.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Add maintainer")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Maintainer added"), @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")})
    @PostMapping("/{id}/maintainers/{userId}")
    @Role({UniverseRoles.ADMIN, UniverseRoles.MAINTAINER})
    public ResponseEntity<UQuadrantResponse> addMaintainer(@PathVariable String id, @PathVariable String userId) {
        UQuadrant existing = service.getById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (!canMaintain(existing)) return ResponseEntity.status(403).build();
        UQuadrant updated = service.addMaintainer(id, userId);
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(summary = "Remove maintainer")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Maintainer removed"), @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")})
    @DeleteMapping("/{id}/maintainers/{userId}")
    @Role({UniverseRoles.ADMIN, UniverseRoles.MAINTAINER})
    public ResponseEntity<UQuadrantResponse> removeMaintainer(@PathVariable String id, @PathVariable String userId) {
        UQuadrant existing = service.getById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (!canMaintain(existing)) return ResponseEntity.status(403).build();
        UQuadrant updated = service.removeMaintainer(id, userId);
        return ResponseEntity.ok(toResponse(updated));
    }
}
