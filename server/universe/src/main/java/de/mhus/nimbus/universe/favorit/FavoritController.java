package de.mhus.nimbus.universe.favorit;

import de.mhus.nimbus.universe.security.RequestUserHolder;
import de.mhus.nimbus.universe.security.CurrentUser;
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
@RequestMapping("/api/favorits")
@Validated
@Tag(name = "Favorites", description = "Manage user favorites")
@SecurityRequirement(name = "bearerAuth")
public class FavoritController {

    private final FavoritService service;
    private final RequestUserHolder userHolder;

    public FavoritController(FavoritService service, RequestUserHolder userHolder) {
        this.service = service;
        this.userHolder = userHolder;
    }

    // DTOs
    public record FavoritRequest(
            @NotBlank String quadrantId,
            String solarSystemId,
            String worldId,
            String entryPointId,
            @NotBlank String title,
            boolean favorit
    ) {}

    public record FavoritResponse(
            String id,
            String quadrantId,
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
        Favorit f = service.create(userId, req.quadrantId(), req.solarSystemId(), req.worldId(), req.entryPointId(), req.title(), req.favorit());
        FavoritResponse resp = toResponse(f);
        return ResponseEntity.created(URI.create("/api/favorits/" + f.getId())).body(resp);
    }

    @Operation(summary = "Update favorite", description = "Updates fields of existing favorite")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Updated"), @ApiResponse(responseCode = "404", description = "Not found")})
    @PutMapping("/{id}")
    public ResponseEntity<FavoritResponse> update(@PathVariable String id, @Valid @RequestBody FavoritRequest req) {
        String userId = requireUser().userId();
        return service.getById(id)
                .filter(f -> f.getUserId().equals(userId))
                .map(existing -> {
                    Favorit updated = service.update(id, req.quadrantId(), req.solarSystemId(), req.worldId(), req.entryPointId(), req.title(), req.favorit());
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

    private FavoritResponse toResponse(Favorit f) {
        return new FavoritResponse(
                f.getId(),
                f.getQuadrantId(),
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
