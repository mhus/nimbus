package de.mhus.nimbus.identity.controller;

import de.mhus.nimbus.identity.service.IdentityService;
import de.mhus.nimbus.server.shared.dto.*;
import de.mhus.nimbus.server.shared.util.AuthorizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * REST controller for Identity Service endpoints.
 * Provides HTTP endpoints for user management and authentication.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class IdentityController {

    private final IdentityService identityService;

    /**
     * Helper method to create a UserDto from request attributes for authorization checks.
     */
    private UserDto createCurrentUserFromRequest(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) request.getAttribute("userRoles");

        if (userId == null) {
            return null;
        }

        return UserDto.builder()
                .id(userId)
                .roles(roles)
                .build();
    }

    /**
     * Creates a new user.
     * POST /users
     */
    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserDto createUserDto) {
        try {
            UserDto createdUser = identityService.createUser(createUserDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            log.error("Error creating user: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Retrieves user information by ID.
     * GET /users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable String id, HttpServletRequest request) {
        try {
            // Check authorization using AuthorizationUtils
            UserDto currentUser = createCurrentUserFromRequest(request);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // User can access their own data, admin can access any user's data
            if (!AuthorizationUtils.canAccessUserData(currentUser, id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            UserDto user = identityService.getUser(id);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            log.error("Error retrieving user: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Updates user information.
     * PUT /users/{id}
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable String id,
                                            @RequestBody UserDto userDto,
                                            HttpServletRequest request) {
        try {
            // Check authorization using AuthorizationUtils
            UserDto currentUser = createCurrentUserFromRequest(request);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // User can update their own data, admin can update any user's data
            if (!AuthorizationUtils.canAccessUserData(currentUser, id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            UserDto updatedUser = identityService.updateUser(id, userDto);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            log.error("Error updating user: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Deletes a user.
     * DELETE /users/{id}
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id, HttpServletRequest request) {
        try {
            // Check authorization using AuthorizationUtils: only admin can delete users
            UserDto currentUser = createCurrentUserFromRequest(request);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            if (!AuthorizationUtils.hasRole(currentUser, AuthorizationUtils.Roles.ADMIN)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            identityService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting user: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Retrieves all users.
     * GET /users
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers(HttpServletRequest request) {
        // Check authorization using AuthorizationUtils: only admin can list all users
        UserDto currentUser = createCurrentUserFromRequest(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!AuthorizationUtils.hasRole(currentUser, AuthorizationUtils.Roles.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<UserDto> users = identityService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Performs user login.
     * POST /login
     */
    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@RequestBody LoginDto loginDto) {
        try {
            TokenDto token = identityService.login(loginDto);
            return ResponseEntity.ok(token);
        } catch (IllegalArgumentException e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Renews an existing JWT token.
     * POST /token/renew
     */
    @PostMapping("/token/renew")
    public ResponseEntity<TokenDto> renewToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().build();
            }

            String token = authHeader.substring(7);
            TokenDto newToken = identityService.renewToken(token);
            return ResponseEntity.ok(newToken);
        } catch (IllegalArgumentException e) {
            log.error("Token renewal failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Changes user password.
     * POST /users/{id}/change-password
     */
    @PostMapping("/users/{id}/change-password")
    public ResponseEntity<Void> changePassword(@PathVariable String id,
                                             @RequestBody ChangePasswordDto changePasswordDto,
                                             HttpServletRequest request) {
        try {
            // Check authorization using AuthorizationUtils
            UserDto currentUser = createCurrentUserFromRequest(request);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // User can change their own password, admin can change any user's password
            if (!AuthorizationUtils.canAccessUserData(currentUser, id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            identityService.changePassword(id, changePasswordDto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Password change failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Health check endpoint.
     * GET /health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Identity Service is running");
    }
}
