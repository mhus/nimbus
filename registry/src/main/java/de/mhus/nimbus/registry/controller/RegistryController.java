package de.mhus.nimbus.registry.controller;

import de.mhus.nimbus.registry.service.RegistryService;
import de.mhus.nimbus.server.shared.dto.CreateWorldDto;
import de.mhus.nimbus.server.shared.dto.UpdateWorldDto;
import de.mhus.nimbus.server.shared.dto.WorldDto;
import de.mhus.nimbus.server.shared.util.AuthorizationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for world registry operations.
 * Provides endpoints for managing worlds and their metadata.
 */
@RestController
@RequestMapping("/worlds")
@RequiredArgsConstructor
@Slf4j
public class RegistryController {

    private final RegistryService registryService;

    /**
     * Creates a new world.
     * Requires CREATOR role.
     */
    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<WorldDto> createWorld(@RequestBody CreateWorldDto createWorldDto,
                                              HttpServletRequest request) {
        log.info("Creating world with name: {}", createWorldDto.getName());

        String userId = AuthorizationUtils.getUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WorldDto createdWorld = registryService.createWorld(createWorldDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdWorld);
    }

    /**
     * Retrieves a world by its ID.
     * Requires USER role.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WorldDto> getWorld(@PathVariable String id) {
        log.debug("Retrieving world with ID: {}", id);

        return registryService.getWorldById(id)
                .map(world -> ResponseEntity.ok(world))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lists worlds with optional filters and pagination.
     * Requires USER role.
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<WorldDto>> listWorlds(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Listing worlds with filters - name: {}, ownerId: {}, enabled: {}, page: {}, size: {}",
                 name, ownerId, enabled, page, size);

        Page<WorldDto> worlds = registryService.listWorlds(name, ownerId, enabled, page, size);
        return ResponseEntity.ok(worlds);
    }

    /**
     * Updates an existing world.
     * Requires ADMIN role or ownership of the world.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<WorldDto> updateWorld(@PathVariable String id,
                                              @RequestBody UpdateWorldDto updateWorldDto,
                                              HttpServletRequest request) {
        log.info("Updating world with ID: {}", id);

        String userId = AuthorizationUtils.getUserId(request);
        List<String> userRoles = AuthorizationUtils.getUserRoles(request);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return registryService.updateWorld(id, updateWorldDto, userId, userRoles)
                .map(world -> ResponseEntity.ok(world))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes a world.
     * Requires ADMIN role or ownership of the world.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Void> deleteWorld(@PathVariable String id,
                                          HttpServletRequest request) {
        log.info("Deleting world with ID: {}", id);

        String userId = AuthorizationUtils.getUserId(request);
        List<String> userRoles = AuthorizationUtils.getUserRoles(request);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean deleted = registryService.deleteWorld(id, userId, userRoles);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Enables a world.
     * Requires ADMIN role or ownership of the world.
     */
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<WorldDto> enableWorld(@PathVariable String id,
                                              HttpServletRequest request) {
        log.info("Enabling world with ID: {}", id);

        String userId = AuthorizationUtils.getUserId(request);
        List<String> userRoles = AuthorizationUtils.getUserRoles(request);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return registryService.enableWorld(id, userId, userRoles)
                .map(world -> ResponseEntity.ok(world))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Disables a world.
     * Requires ADMIN role or ownership of the world.
     */
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<WorldDto> disableWorld(@PathVariable String id,
                                               HttpServletRequest request) {
        log.info("Disabling world with ID: {}", id);

        String userId = AuthorizationUtils.getUserId(request);
        List<String> userRoles = AuthorizationUtils.getUserRoles(request);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return registryService.disableWorld(id, userId, userRoles)
                .map(world -> ResponseEntity.ok(world))
                .orElse(ResponseEntity.notFound().build());
    }
}
