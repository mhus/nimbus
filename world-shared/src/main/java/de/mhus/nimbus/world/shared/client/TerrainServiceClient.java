package de.mhus.nimbus.world.shared.client;

import de.mhus.nimbus.shared.dto.world.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TerrainServiceClient {

    private final RestTemplate restTemplate;

    @Value("${nimbus.world-terrain.url:http://localhost:8083}")
    private String worldTerrainUrl;

    // World Management
    public WorldDto createWorld(WorldDto worldDto) {
        String url = worldTerrainUrl + "/api/worlds";
        ResponseEntity<WorldDto> response = restTemplate.postForEntity(url, worldDto, WorldDto.class);
        return response.getBody();
    }

    public Optional<WorldDto> getWorld(String id) {
        try {
            String url = worldTerrainUrl + "/api/worlds/" + id;
            ResponseEntity<WorldDto> response = restTemplate.getForEntity(url, WorldDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to get world with id {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    public List<WorldDto> getAllWorlds() {
        String url = worldTerrainUrl + "/api/worlds";
        ResponseEntity<WorldDto[]> response = restTemplate.getForEntity(url, WorldDto[].class);
        WorldDto[] body = response.getBody();
        return body != null ? List.of(body) : List.of();
    }

    public Optional<WorldDto> updateWorld(String id, WorldDto worldDto) {
        try {
            String url = worldTerrainUrl + "/api/worlds/" + id;
            ResponseEntity<WorldDto> response = restTemplate.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(worldDto), WorldDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to update world with id {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean deleteWorld(String id) {
        try {
            String url = worldTerrainUrl + "/api/worlds/" + id;
            restTemplate.delete(url);
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete world with id {}: {}", id, e.getMessage());
            return false;
        }
    }

    // Material Management
    public MaterialDto createMaterial(MaterialDto materialDto) {
        String url = worldTerrainUrl + "/api/materials";
        ResponseEntity<MaterialDto> response = restTemplate.postForEntity(url, materialDto, MaterialDto.class);
        return response.getBody();
    }

    public Optional<MaterialDto> getMaterial(Integer id) {
        try {
            String url = worldTerrainUrl + "/api/materials/" + id;
            ResponseEntity<MaterialDto> response = restTemplate.getForEntity(url, MaterialDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to get material with id {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    public Page<MaterialDto> getMaterials(String name, int page, int size) {
        String url = UriComponentsBuilder.fromUriString(worldTerrainUrl + "/api/materials")
                .queryParamIfPresent("name", Optional.ofNullable(name))
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();

        ResponseEntity<PageImpl<MaterialDto>> response = restTemplate.exchange(
            url, HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {});

        return response.getBody();
    }

    public Optional<MaterialDto> updateMaterial(Integer id, MaterialDto materialDto) {
        try {
            String url = worldTerrainUrl + "/api/materials/" + id;
            ResponseEntity<MaterialDto> response = restTemplate.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(materialDto), MaterialDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to update material with id {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean deleteMaterial(Integer id) {
        try {
            String url = worldTerrainUrl + "/api/materials/" + id;
            restTemplate.delete(url);
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete material with id {}: {}", id, e.getMessage());
            return false;
        }
    }

    // Map Management
    public void createOrUpdateMap(MapCreateRequest request) {
        String url = worldTerrainUrl + "/api/maps";
        restTemplate.postForEntity(url, request, Void.class);
    }

    public Optional<TerrainClusterDto> getMapCluster(String world, Integer level, Integer x, Integer y) {
        try {
            String url = UriComponentsBuilder.fromUriString(worldTerrainUrl + "/api/maps/" + x + "/" + y)
                    .queryParam("world", world)
                    .queryParam("level", level)
                    .toUriString();

            ResponseEntity<TerrainClusterDto> response = restTemplate.getForEntity(url, TerrainClusterDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to get map cluster at {},{} for world {} level {}: {}",
                    x, y, world, level, e.getMessage());
            return Optional.empty();
        }
    }

    public List<TerrainClusterDto> getMapClusters(MapBatchRequest request) {
        String url = worldTerrainUrl + "/api/maps/batch";
        ResponseEntity<TerrainClusterDto[]> response = restTemplate.postForEntity(url, request, TerrainClusterDto[].class);
        TerrainClusterDto[] body = response.getBody();
        return body != null ? List.of(body) : List.of();
    }

    public void deleteMapFields(MapDeleteRequest request) {
        String url = worldTerrainUrl + "/api/maps";
        restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(request), Void.class);
    }

    public void deleteLevel(String world, Integer level) {
        String url = UriComponentsBuilder.fromUriString(worldTerrainUrl + "/api/maps/level")
                .queryParam("world", world)
                .queryParam("level", level)
                .toUriString();

        restTemplate.delete(url);
    }
}
