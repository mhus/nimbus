package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.generated.configs.Settings;
import de.mhus.nimbus.world.shared.sector.RUser;
import de.mhus.nimbus.world.shared.sector.RUserService;
import de.mhus.nimbus.shared.user.SectorRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing RUser entities.
 * Provides CRUD operations, role management, and settings management.
 */
@RestController
@RequestMapping("/control/users")
@RequiredArgsConstructor
public class RUserController extends BaseEditorController {

    private final RUserService userService;

    // DTOs
    public record UserRequest(String username, String email, String sectorRolesRaw) {}
    public record UserResponse(
            String id,
            String username,
            String email,
            boolean enabled,
            List<String> sectorRoles,
            Map<String, Settings> userSettings
    ) {}
    public record SettingsRequest(String clientType, Settings settings) {}

    private UserResponse toResponse(RUser user) {
        List<String> sectorRoles = user.getSectorRoles().stream()
                .map(Enum::name)
                .toList();
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                sectorRoles,
                user.getUserSettings()
        );
    }

    /**
     * List all users
     * GET /control/user
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        List<UserResponse> result = userService.listAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Get user by username
     * GET /control/user/{username}
     */
    @GetMapping("/{username}")
    public ResponseEntity<?> get(@PathVariable String username) {
        var error = validateId(username, "username");
        if (error != null) return error;

        return userService.getByUsername(username)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(toResponse(u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found: " + username)));
    }

    /**
     * Update user
     * PUT /control/user/{username}
     */
    @PutMapping("/{username}")
    public ResponseEntity<?> update(
            @PathVariable String username,
            @RequestBody UserRequest request) {

        var error = validateId(username, "username");
        if (error != null) return error;

        if (userService.getByUsername(username).isEmpty()) {
            return notFound("User not found: " + username);
        }

        try {
            RUser updated = userService.update(username, request.email(), request.sectorRolesRaw());
            return ResponseEntity.ok(toResponse(updated));
        } catch (IllegalArgumentException e) {
            return bad(e.getMessage());
        }
    }

    /**
     * Delete user (disable)
     * DELETE /control/user/{username}
     */
    @DeleteMapping("/{username}")
    public ResponseEntity<?> delete(@PathVariable String username) {
        var error = validateId(username, "username");
        if (error != null) return error;

        return userService.getByUsername(username)
                .map(user -> {
                    user.disable();
                    // We need to save the user, but RUserService doesn't have a simple save method
                    // So we'll use update with existing values
                    userService.update(username, user.getEmail(), user.getSectorRolesRaw());
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Get user settings
     * GET /control/user/{username}/settings
     */
    @GetMapping("/{username}/settings")
    public ResponseEntity<?> getSettings(@PathVariable String username) {
        var error = validateId(username, "username");
        if (error != null) return error;

        try {
            Map<String, Settings> settings = userService.getUserSettings(username);
            return ResponseEntity.ok(settings);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    /**
     * Get settings for specific client type
     * GET /control/user/{username}/settings/{clientType}
     */
    @GetMapping("/{username}/settings/{clientType}")
    public ResponseEntity<?> getSettingsForClientType(
            @PathVariable String username,
            @PathVariable String clientType) {

        var error = validateId(username, "username");
        if (error != null) return error;

        var error2 = validateId(clientType, "clientType");
        if (error2 != null) return error2;

        try {
            Settings settings = userService.getSettingsForClientType(username, clientType);
            if (settings == null) {
                return notFound("Settings not found for clientType: " + clientType);
            }
            return ResponseEntity.ok(settings);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    /**
     * Update settings for specific client type
     * PUT /control/user/{username}/settings/{clientType}
     */
    @PutMapping("/{username}/settings/{clientType}")
    public ResponseEntity<?> updateSettingsForClientType(
            @PathVariable String username,
            @PathVariable String clientType,
            @RequestBody Settings settings) {

        var error = validateId(username, "username");
        if (error != null) return error;

        var error2 = validateId(clientType, "clientType");
        if (error2 != null) return error2;

        try {
            userService.setSettingsForClientType(username, clientType, settings);
            return ResponseEntity.ok(Map.of("message", "Settings updated successfully"));
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    /**
     * Delete settings for specific client type
     * DELETE /control/user/{username}/settings/{clientType}
     */
    @DeleteMapping("/{username}/settings/{clientType}")
    public ResponseEntity<?> deleteSettingsForClientType(
            @PathVariable String username,
            @PathVariable String clientType) {

        var error = validateId(username, "username");
        if (error != null) return error;

        var error2 = validateId(clientType, "clientType");
        if (error2 != null) return error2;

        try {
            userService.setSettingsForClientType(username, clientType, null);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    /**
     * Update all user settings
     * PUT /control/user/{username}/settings
     */
    @PutMapping("/{username}/settings")
    public ResponseEntity<?> updateAllSettings(
            @PathVariable String username,
            @RequestBody Map<String, Settings> settings) {

        var error = validateId(username, "username");
        if (error != null) return error;

        try {
            userService.setUserSettings(username, settings);
            return ResponseEntity.ok(Map.of("message", "All settings updated successfully"));
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        }
    }

    /**
     * Add sector role
     * POST /control/user/{username}/roles/{role}
     */
    @PostMapping("/{username}/roles/{role}")
    public ResponseEntity<?> addSectorRole(
            @PathVariable String username,
            @PathVariable String role) {

        var error = validateId(username, "username");
        if (error != null) return error;

        try {
            SectorRoles sectorRole = SectorRoles.valueOf(role);
            RUser updated = userService.addSectorRoles(username, sectorRole);
            return ResponseEntity.ok(toResponse(updated));
        } catch (IllegalArgumentException e) {
            return bad("Invalid role or user not found: " + e.getMessage());
        }
    }

    /**
     * Remove sector role
     * DELETE /control/user/{username}/roles/{role}
     */
    @DeleteMapping("/{username}/roles/{role}")
    public ResponseEntity<?> removeSectorRole(
            @PathVariable String username,
            @PathVariable String role) {

        var error = validateId(username, "username");
        if (error != null) return error;

        try {
            SectorRoles sectorRole = SectorRoles.valueOf(role);
            RUser updated = userService.removeSectorRole(username, sectorRole);
            return ResponseEntity.ok(toResponse(updated));
        } catch (IllegalArgumentException e) {
            return bad("Invalid role or user not found: " + e.getMessage());
        }
    }
}
