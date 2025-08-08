package de.mhus.nimbus.server.shared.client;

import de.mhus.nimbus.server.shared.dto.CreateWorldDto;
import de.mhus.nimbus.server.shared.dto.UpdateWorldDto;
import de.mhus.nimbus.server.shared.dto.WorldDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * Client for communicating with the Registry Service.
 * Provides methods to interact with world registry endpoints.
 */
@Component
@Slf4j
public class RegistryServiceClient {

    private final RestTemplate restTemplate;
    private final String registryServiceUrl;

    public RegistryServiceClient(RestTemplate restTemplate,
                               @Value("${nimbus.registry.service.url:http://localhost:8082}") String registryServiceUrl) {
        this.restTemplate = restTemplate;
        this.registryServiceUrl = registryServiceUrl;
    }

    /**
     * Creates a new world.
     * @param createWorldDto the world data
     * @param authToken JWT token for authentication
     * @return the created world
     */
    public Optional<WorldDto> createWorld(CreateWorldDto createWorldDto, String authToken) {
        try {
            log.debug("Creating world: {}", createWorldDto.getName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(authToken);

            HttpEntity<CreateWorldDto> entity = new HttpEntity<>(createWorldDto, headers);

            ResponseEntity<WorldDto> response = restTemplate.exchange(
                registryServiceUrl + "/worlds",
                HttpMethod.POST,
                entity,
                WorldDto.class
            );

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error creating world: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Retrieves a world by its ID.
     * @param worldId the world ID
     * @param authToken JWT token for authentication
     * @return the world if found
     */
    public Optional<WorldDto> getWorldById(String worldId, String authToken) {
        try {
            log.debug("Retrieving world: {}", worldId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<WorldDto> response = restTemplate.exchange(
                registryServiceUrl + "/worlds/" + worldId,
                HttpMethod.GET,
                entity,
                WorldDto.class
            );

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error retrieving world {}: {}", worldId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Lists worlds with optional filters and pagination.
     * @param name optional name filter
     * @param ownerId optional owner ID filter
     * @param enabled optional enabled status filter
     * @param page page number
     * @param size page size
     * @param authToken JWT token for authentication
     * @return page of worlds
     */
    public Optional<Page<WorldDto>> listWorlds(String name, String ownerId, Boolean enabled,
                                             int page, int size, String authToken) {
        try {
            log.debug("Listing worlds with filters");

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(registryServiceUrl + "/worlds")
                .queryParam("page", page)
                .queryParam("size", size);

            if (name != null) {
                builder.queryParam("name", name);
            }
            if (ownerId != null) {
                builder.queryParam("ownerId", ownerId);
            }
            if (enabled != null) {
                builder.queryParam("enabled", enabled);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Page<WorldDto>> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Page<WorldDto>>() {}
            );

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error listing worlds: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Updates an existing world.
     * @param worldId the world ID
     * @param updateWorldDto the updated world data
     * @param authToken JWT token for authentication
     * @return the updated world if successful
     */
    public Optional<WorldDto> updateWorld(String worldId, UpdateWorldDto updateWorldDto, String authToken) {
        try {
            log.debug("Updating world: {}", worldId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(authToken);

            HttpEntity<UpdateWorldDto> entity = new HttpEntity<>(updateWorldDto, headers);

            ResponseEntity<WorldDto> response = restTemplate.exchange(
                registryServiceUrl + "/worlds/" + worldId,
                HttpMethod.PUT,
                entity,
                WorldDto.class
            );

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error updating world {}: {}", worldId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Deletes a world.
     * @param worldId the world ID
     * @param authToken JWT token for authentication
     * @return true if deleted successfully
     */
    public boolean deleteWorld(String worldId, String authToken) {
        try {
            log.debug("Deleting world: {}", worldId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                registryServiceUrl + "/worlds/" + worldId,
                HttpMethod.DELETE,
                entity,
                Void.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error deleting world {}: {}", worldId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Enables a world.
     * @param worldId the world ID
     * @param authToken JWT token for authentication
     * @return the updated world if successful
     */
    public Optional<WorldDto> enableWorld(String worldId, String authToken) {
        try {
            log.debug("Enabling world: {}", worldId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<WorldDto> response = restTemplate.exchange(
                registryServiceUrl + "/worlds/" + worldId + "/enable",
                HttpMethod.POST,
                entity,
                WorldDto.class
            );

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error enabling world {}: {}", worldId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Disables a world.
     * @param worldId the world ID
     * @param authToken JWT token for authentication
     * @return the updated world if successful
     */
    public Optional<WorldDto> disableWorld(String worldId, String authToken) {
        try {
            log.debug("Disabling world: {}", worldId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<WorldDto> response = restTemplate.exchange(
                registryServiceUrl + "/worlds/" + worldId + "/disable",
                HttpMethod.POST,
                entity,
                WorldDto.class
            );

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error disabling world {}: {}", worldId, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
