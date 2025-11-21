package de.mhus.nimbus.universe.favorit;

import de.mhus.nimbus.universe.security.RequestUserHolder;
import de.mhus.nimbus.universe.security.CurrentUser;
import de.mhus.nimbus.universe.security.Role;
import de.mhus.nimbus.shared.user.UniverseRoles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(UFavoritController.BASE_PATH)
@Validated
@Tag(name = "Favorites", description = "Manage user favorites")
@SecurityRequirement(name = "bearerAuth")
@Role({UniverseRoles.USER, UniverseRoles.ADMIN})
public class UFavoritController {

    public static final String BASE_PATH = "/universe/user/favorties";

    private final UFavoritService service;
    private final RequestUserHolder userHolder;

    public UFavoritController(UFavoritService service, RequestUserHolder userHolder) {
        this.service = service;
        this.userHolder = userHolder;
    }

    // DTOs
    public record FavoritRequest(
            @NotBlank String regionId,
            String solarSystemId,
            String worldId,
            String entryPointId,
            @NotBlank String title,
            boolean favorit
    ) {}

    public record FavoritResponse(
            String id,
            String regionId,
            String solarSystemId,
            String worldId,
            String entryPointId,
            String title,
            boolean favorit,
            java.time.Instant createdAt,
            java.time.Instant lastAccessAt
    ) {}

    private CurrentUser requireUser() {
        CurrentUser cu = userHolder.get();
        if (cu == null) throw new UnauthorizedException();
        return cu;
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    private static class UnauthorizedException extends RuntimeException {}

    @Operation(summary = "List favorite entries", description = "Returns only entries marked as favorit")
    @ApiResponse(responseCode = "200", description = "List returned")
    @GetMapping
    public ResponseEntity<List<FavoritResponse>> listFavorites() {
        String userId = requireUser().userId();
        List<FavoritResponse> list = service.listFavorites(userId).stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "List all entries", description = "Returns all favorites including non-favorit entries")
    @ApiResponse(responseCode = "200", description = "List returned")
    @GetMapping("/all")
    public ResponseEntity<List<FavoritResponse>> listAll() {
        String userId = requireUser().userId();
        List<FavoritResponse> list = service.listAllByUser(userId).stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Get single favorite", description = "Returns favorite by id")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Found"), @ApiResponse(responseCode = "404", description = "Not found")})
    @GetMapping("/{id}")
    public ResponseEntity<FavoritResponse> get(@PathVariable String id) {
        String userId = requireUser().userId();
        return service.getById(id)
                .filter(f -> f.getUserId().equals(userId))
                .map(f -> ResponseEntity.ok(toResponse(f)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create favorite", description = "Creates new favorite entry")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Created"), @ApiResponse(responseCode = "400", description = "Validation error"), @ApiResponse(responseCode = "401", description = "Unauthorized")})
    @PostMapping
    public ResponseEntity<FavoritResponse> create(@Valid @RequestBody FavoritRequest req) {
        String userId = requireUser().userId();
        UFavorit f = service.create(userId, req.regionId(), req.solarSystemId(), req.worldId(), req.entryPointId(), req.title(), req.favorit());
        FavoritResponse resp = toResponse(f);
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + f.getId())).body(resp);
    }

    @Operation(summary = "Update favorite", description = "Updates fields of existing favorite")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Updated"), @ApiResponse(responseCode = "404", description = "Not found")})
    @PutMapping("/{id}")
    public ResponseEntity<FavoritResponse> update(@PathVariable String id, @Valid @RequestBody FavoritRequest req) {
        String userId = requireUser().userId();
        return service.getById(id)
                .filter(f -> f.getUserId().equals(userId))
                .map(existing -> {
                    UFavorit updated = service.update(id, req.regionId(), req.solarSystemId(), req.worldId(), req.entryPointId(), req.title(), req.favorit());
                    return ResponseEntity.ok(toResponse(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete favorite", description = "Deletes favorite by id")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Deleted"), @ApiResponse(responseCode = "404", description = "Not found")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        String userId = requireUser().userId();
        return service.getById(id)
                .filter(f -> f.getUserId().equals(userId))
                .map(f -> {
                    service.delete(id);
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Toggle favorit flag", description = "Sets favorit true/false")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Toggled"), @ApiResponse(responseCode = "404", description = "Not found")})
    @PostMapping("/{id}/toggle")
    public ResponseEntity<FavoritResponse> toggle(@PathVariable String id, @RequestParam boolean favorit) {
        String userId = requireUser().userId();
        return service.getById(id)
                .filter(f -> f.getUserId().equals(userId))
                .map(f -> ResponseEntity.ok(toResponse(service.toggleFavorite(id, favorit))))
                .orElse(ResponseEntity.notFound().build());
    }

    private FavoritResponse toResponse(UFavorit f) {
        return new FavoritResponse(
                f.getId(),
                f.getRegionId(),
                f.getSolarSystemId(),
                f.getWorldId(),
                f.getEntryPointId(),
                f.getTitle(),
                f.isFavorit(),
                f.getCreatedAt(),
                f.getLastAccessAt()
        );
    }
}
