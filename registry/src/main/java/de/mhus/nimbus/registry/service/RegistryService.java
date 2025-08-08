package de.mhus.nimbus.registry.service;

import de.mhus.nimbus.registry.entity.World;
import de.mhus.nimbus.registry.repository.WorldRepository;
import de.mhus.nimbus.server.shared.dto.CreateWorldDto;
import de.mhus.nimbus.server.shared.dto.UpdateWorldDto;
import de.mhus.nimbus.server.shared.dto.WorldDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing world registry operations.
 * Handles CRUD operations for worlds and provides business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RegistryService {

    private final WorldRepository worldRepository;

    /**
     * Creates a new world.
     * @param createWorldDto the world data
     * @param ownerId the ID of the user creating the world
     * @return the created world
     */
    public WorldDto createWorld(CreateWorldDto createWorldDto, String ownerId) {
        log.info("Creating new world '{}' for user '{}'", createWorldDto.getName(), ownerId);

        World world = World.builder()
                .id(UUID.randomUUID().toString())
                .name(createWorldDto.getName())
                .description(createWorldDto.getDescription())
                .ownerId(ownerId)
                .accessUrl(createWorldDto.getAccessUrl())
                .enabled(true)
                .properties(createWorldDto.getProperties() != null ?
                           new HashMap<>(createWorldDto.getProperties()) : new HashMap<>())
                .build();

        World savedWorld = worldRepository.save(world);
        log.info("Successfully created world with ID '{}'", savedWorld.getId());

        return convertToDto(savedWorld);
    }

    /**
     * Retrieves a world by its ID.
     * @param worldId the world ID
     * @return the world if found
     */
    @Transactional(readOnly = true)
    public Optional<WorldDto> getWorldById(String worldId) {
        log.debug("Retrieving world with ID '{}'", worldId);
        return worldRepository.findById(worldId)
                .map(this::convertToDto);
    }

    /**
     * Lists worlds with optional filters and pagination.
     * @param name optional name filter
     * @param ownerId optional owner ID filter
     * @param enabled optional enabled status filter
     * @param page page number (0-based)
     * @param size page size (max 100)
     * @return page of worlds
     */
    @Transactional(readOnly = true)
    public Page<WorldDto> listWorlds(String name, String ownerId, Boolean enabled,
                                   int page, int size) {
        log.debug("Listing worlds with filters - name: '{}', ownerId: '{}', enabled: '{}'",
                 name, ownerId, enabled);

        // Validate and limit page size
        int validatedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, validatedSize, Sort.by("createdAt").descending());

        Page<World> worlds = worldRepository.findWorldsWithFilters(name, ownerId, enabled, pageable);

        return worlds.map(this::convertToDto);
    }

    /**
     * Updates an existing world.
     * @param worldId the world ID
     * @param updateWorldDto the updated world data
     * @param userId the ID of the user performing the update
     * @param userRoles the roles of the user
     * @return the updated world if found and user has permission
     */
    public Optional<WorldDto> updateWorld(String worldId, UpdateWorldDto updateWorldDto,
                                        String userId, java.util.List<String> userRoles) {
        log.info("Updating world '{}' by user '{}'", worldId, userId);

        return worldRepository.findById(worldId)
                .filter(world -> canModifyWorld(world, userId, userRoles))
                .map(world -> {
                    if (updateWorldDto.getName() != null) {
                        world.setName(updateWorldDto.getName());
                    }
                    if (updateWorldDto.getDescription() != null) {
                        world.setDescription(updateWorldDto.getDescription());
                    }
                    if (updateWorldDto.getAccessUrl() != null) {
                        world.setAccessUrl(updateWorldDto.getAccessUrl());
                    }
                    if (updateWorldDto.getProperties() != null) {
                        world.setProperties(new HashMap<>(updateWorldDto.getProperties()));
                    }

                    World updatedWorld = worldRepository.save(world);
                    log.info("Successfully updated world '{}'", worldId);
                    return convertToDto(updatedWorld);
                });
    }

    /**
     * Deletes a world.
     * @param worldId the world ID
     * @param userId the ID of the user performing the deletion
     * @param userRoles the roles of the user
     * @return true if deleted, false if not found or no permission
     */
    public boolean deleteWorld(String worldId, String userId, java.util.List<String> userRoles) {
        log.info("Deleting world '{}' by user '{}'", worldId, userId);

        return worldRepository.findById(worldId)
                .filter(world -> canModifyWorld(world, userId, userRoles))
                .map(world -> {
                    worldRepository.delete(world);
                    log.info("Successfully deleted world '{}'", worldId);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Enables a world.
     * @param worldId the world ID
     * @param userId the ID of the user performing the action
     * @param userRoles the roles of the user
     * @return the updated world if found and user has permission
     */
    public Optional<WorldDto> enableWorld(String worldId, String userId, java.util.List<String> userRoles) {
        log.info("Enabling world '{}' by user '{}'", worldId, userId);
        return setWorldEnabled(worldId, true, userId, userRoles);
    }

    /**
     * Disables a world.
     * @param worldId the world ID
     * @param userId the ID of the user performing the action
     * @param userRoles the roles of the user
     * @return the updated world if found and user has permission
     */
    public Optional<WorldDto> disableWorld(String worldId, String userId, java.util.List<String> userRoles) {
        log.info("Disabling world '{}' by user '{}'", worldId, userId);
        return setWorldEnabled(worldId, false, userId, userRoles);
    }

    /**
     * Sets the enabled status of a world.
     */
    private Optional<WorldDto> setWorldEnabled(String worldId, boolean enabled,
                                             String userId, java.util.List<String> userRoles) {
        return worldRepository.findById(worldId)
                .filter(world -> canModifyWorld(world, userId, userRoles))
                .map(world -> {
                    world.setEnabled(enabled);
                    World updatedWorld = worldRepository.save(world);
                    log.info("Successfully {} world '{}'", enabled ? "enabled" : "disabled", worldId);
                    return convertToDto(updatedWorld);
                });
    }

    /**
     * Checks if a user can modify a world.
     * Users can modify worlds they own or if they have ADMIN role.
     */
    private boolean canModifyWorld(World world, String userId, java.util.List<String> userRoles) {
        return world.getOwnerId().equals(userId) ||
               (userRoles != null && userRoles.contains("ADMIN"));
    }

    /**
     * Converts a World entity to WorldDto.
     */
    private WorldDto convertToDto(World world) {
        return WorldDto.builder()
                .id(world.getId())
                .name(world.getName())
                .description(world.getDescription())
                .createdAt(world.getCreatedAt() != null ? world.getCreatedAt().toEpochMilli() : System.currentTimeMillis())
                .updatedAt(world.getUpdatedAt() != null ? world.getUpdatedAt().toEpochMilli() : System.currentTimeMillis())
                .ownerId(world.getOwnerId())
                .enabled(world.getEnabled())
                .accessUrl(world.getAccessUrl())
                .properties(world.getProperties())
                .build();
    }
}
