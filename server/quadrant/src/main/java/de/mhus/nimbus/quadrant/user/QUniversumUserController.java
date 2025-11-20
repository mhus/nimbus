package de.mhus.nimbus.quadrant.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(QUniversumUserController.BASE_PATH)
@Tag(name = "QUsers", description = "Universum access to quadrant users")
public class QUniversumUserController {

    public static final String BASE_PATH = "/quadrant/universum/user";

    private final QUserService service;

    public QUniversumUserController(QUserService service) {
        this.service = service;
    }

    // DTOs
    public record QUserRequest(String username, String email, String roles) {}
    public record QUserResponse(String id, String username, String email, String roles) {}

    private QUserResponse toResponse(QUser u) {
        return new QUserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRolesRaw());
    }

    @Operation(summary = "List users")
    @ApiResponse(responseCode = "200", description = "List returned")
    @GetMapping
    public ResponseEntity<List<QUserResponse>> list() {
        List<QUserResponse> list = service.listAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Get user by id")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Found"), @ApiResponse(responseCode = "404", description = "Not found")})
    @GetMapping("/{id}")
    public ResponseEntity<QUserResponse> get(@PathVariable String id) {
        Optional<QUser> opt = service.getById(id);
        return opt.map(u -> ResponseEntity.ok(toResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create user")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Created"), @ApiResponse(responseCode = "400", description = "Validation error")})
    @PostMapping
    public ResponseEntity<?> create(@RequestBody QUserRequest req) {
        try {
            QUser u = service.createUser(req.username(), req.email());
            // roles from request (optional)
            if (req.roles() != null && !req.roles().isBlank()) {
                u.setRolesRaw(req.roles());
                u = service.update(u.getId(), u.getUsername(), u.getEmail(), u.getRolesRaw());
            }
            return ResponseEntity.created(URI.create(BASE_PATH + "/" + u.getId())).body(toResponse(u));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Update user")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Updated"), @ApiResponse(responseCode = "404", description = "Not found"), @ApiResponse(responseCode = "400", description = "Validation error")})
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody QUserRequest req) {
        // ensure exists
        if (service.getById(id).isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        try {
            QUser updated = service.update(id, req.username(), req.email(), req.roles());
            return ResponseEntity.ok(toResponse(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete user")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Deleted"), @ApiResponse(responseCode = "404", description = "Not found")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (service.getById(id).isEmpty()) return ResponseEntity.notFound().build();
        service.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
