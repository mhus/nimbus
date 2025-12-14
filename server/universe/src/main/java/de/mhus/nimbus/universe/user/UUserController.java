package de.mhus.nimbus.universe.user;

import de.mhus.nimbus.shared.user.UniverseRoles;
import de.mhus.nimbus.universe.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(UUserController.BASE_PATH)
@Role({UniverseRoles.ADMIN})
@Tag(name = "UUsers", description = "Verwaltung von Universe-Usern (nur ADMIN)")
public class UUserController {

    // Achtung: geforderter Pfad mit "universum"
    public static final String BASE_PATH = "/universum/user/user";

    private final UUserService service;

    public UUserController(UUserService service) {
        this.service = service;
    }

    // DTOs
    public record UUserRequest(String username, String email, String roles, String password) {}
    public record UUserResponse(String id, String username, String email, String roles) {}

    private UUserResponse toResponse(UUser u) {
        return new UUserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRolesAsString());
    }

    @Operation(summary = "User auflisten")
    @ApiResponse(responseCode = "200", description = "Liste geliefert")
    @GetMapping
    public ResponseEntity<List<UUserResponse>> list(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "role", required = false) String role
    ) {
        List<UUserResponse> list = service.listAll().stream()
                .filter(u -> username == null || username.equals(u.getUsername()))
                .filter(u -> email == null || email.equals(u.getEmail()))
                .filter(u -> role == null || (u.getRolesAsString() != null &&
                        List.of(u.getRolesAsString().split(",")).stream().map(String::trim).collect(Collectors.toSet()).contains(role)))
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "User per ID abrufen")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Gefunden"), @ApiResponse(responseCode = "404", description = "Nicht gefunden")})
    @GetMapping("/{id}")
    public ResponseEntity<UUserResponse> get(@PathVariable String id) {
        Optional<UUser> opt = service.getById(id);
        return opt.map(u -> ResponseEntity.ok(toResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "User anlegen")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Erstellt"), @ApiResponse(responseCode = "400", description = "Validierungsfehler")})
    @PostMapping
    public ResponseEntity<?> create(@RequestBody UUserRequest req) {
        try {
            UUser u = service.createUser(req.username(), req.email());
            if (req.roles() != null && !req.roles().isBlank()) {
                u.setRolesStringList(req.roles());
                u = service.update(u.getId(), u.getUsername(), u.getEmail(), u.getRolesAsString());
            }
            // optional initial password
            if (req.password() != null && !req.password().isBlank()) {
                service.setPassword(u.getId(), req.password());
            }
            return ResponseEntity.created(URI.create(BASE_PATH + "/" + u.getId())).body(toResponse(u));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "User ändern")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Aktualisiert"), @ApiResponse(responseCode = "404", description = "Nicht gefunden"), @ApiResponse(responseCode = "400", description = "Validierungsfehler")})
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody UUserRequest req) {
        if (service.getById(id).isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        try {
            UUser updated = service.update(id, req.username(), req.email(), req.roles());
            // optional password change via general update
            if (req.password() != null && !req.password().isBlank()) {
                service.setPassword(id, req.password());
            }
            return ResponseEntity.ok(toResponse(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // Password-Update als separater Endpunkt
    public record UUserPasswordRequest(String password) {}

    @Operation(summary = "Passwort setzen/ändern")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Passwort aktualisiert"),
            @ApiResponse(responseCode = "404", description = "Nicht gefunden"),
            @ApiResponse(responseCode = "400", description = "Validierungsfehler")
    })
    @PutMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(@PathVariable String id, @RequestBody UUserPasswordRequest req) {
        if (service.getById(id).isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        try {
            service.setPassword(id, req.password());
            // Antwort ohne Benutzer-Details; alternativ könnte man den User zurückgeben
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "User löschen")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Gelöscht"), @ApiResponse(responseCode = "404", description = "Nicht gefunden")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (service.getById(id).isEmpty()) return ResponseEntity.notFound().build();
        service.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
