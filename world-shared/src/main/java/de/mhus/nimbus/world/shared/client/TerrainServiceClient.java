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

    // Sprite Management
    public SpriteDto createSprite(SpriteCreateRequest request) {
        String url = worldTerrainUrl + "/api/sprites";
        ResponseEntity<SpriteDto> response = restTemplate.postForEntity(url, request, SpriteDto.class);
        return response.getBody();
    }

    public Optional<SpriteDto> getSprite(String id) {
        try {
            String url = worldTerrainUrl + "/api/sprites/" + id;
            ResponseEntity<SpriteDto> response = restTemplate.getForEntity(url, SpriteDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to get sprite with id {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    public List<SpriteDto> getSprites(String world, Integer level, Integer x, Integer y) {
        String url = UriComponentsBuilder.fromUriString(worldTerrainUrl + "/api/sprites/" + world + "/" + level + "/" + x + "/" + y)
                .toUriString();
        ResponseEntity<SpriteDto[]> response = restTemplate.getForEntity(url, SpriteDto[].class);
        SpriteDto[] body = response.getBody();
        return body != null ? List.of(body) : List.of();
    }

    public Optional<SpriteDto> updateSprite(String id, SpriteDto spriteDto) {
        try {
            String url = worldTerrainUrl + "/api/sprites/" + id;
            ResponseEntity<SpriteDto> response = restTemplate.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(spriteDto), SpriteDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to update sprite with id {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean deleteSprite(String id) {
        try {
            String url = worldTerrainUrl + "/api/sprites/" + id;
            restTemplate.delete(url);
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete sprite with id {}: {}", id, e.getMessage());
            return false;
        }
    }

    public Optional<SpriteDto> updateSpriteCoordinates(String id, SpriteCoordinatesDto coordinates) {
        try {
            String url = worldTerrainUrl + "/api/sprites/" + id + "/coordinates";
            ResponseEntity<SpriteDto> response = restTemplate.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(coordinates), SpriteDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to update sprite coordinates for id {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<SpriteDto> enableSprite(String id) {
        try {
            String url = worldTerrainUrl + "/api/sprites/" + id + "/enable";
            ResponseEntity<SpriteDto> response = restTemplate.postForEntity(url, null, SpriteDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to enable sprite with id {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<SpriteDto> disableSprite(String id) {
        try {
            String url = worldTerrainUrl + "/api/sprites/" + id + "/disable";
            ResponseEntity<SpriteDto> response = restTemplate.postForEntity(url, null, SpriteDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to disable sprite with id {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    // Asset Management
    public AssetDto createAsset(AssetCreateRequest request) {
        String url = worldTerrainUrl + "/api/assets";
        ResponseEntity<AssetDto> response = restTemplate.postForEntity(url, request, AssetDto.class);
        return response.getBody();
    }

    public Optional<AssetDto> getAsset(String world, String name) {
        try {
            String url = worldTerrainUrl + "/api/assets/" + world + "/" + name;
            ResponseEntity<AssetDto> response = restTemplate.getForEntity(url, AssetDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to get asset {} in world {}: {}", name, world, e.getMessage());
            return Optional.empty();
        }
    }

    public Page<AssetDto> getAssets(String world, int page, int size) {
        String url = UriComponentsBuilder.fromUriString(worldTerrainUrl + "/api/assets/" + world)
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();

        ResponseEntity<PageImpl<AssetDto>> response = restTemplate.exchange(
            url, HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {});

        return response.getBody();
    }

    public Optional<AssetDto> updateAsset(String world, String name, AssetDto assetDto) {
        try {
            String url = worldTerrainUrl + "/api/assets/" + world + "/" + name;
            ResponseEntity<AssetDto> response = restTemplate.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(assetDto), AssetDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to update asset {} in world {}: {}", name, world, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean deleteAsset(String world, String name) {
        try {
            String url = worldTerrainUrl + "/api/assets/" + world + "/" + name;
            restTemplate.delete(url);
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete asset {} in world {}: {}", name, world, e.getMessage());
            return false;
        }
    }

    public void compressAssets(String world) {
        String url = worldTerrainUrl + "/api/assets/compress";
        AssetCompressRequest request = new AssetCompressRequest(world);
        restTemplate.postForEntity(url, request, Void.class);
    }

    public List<AssetDto> getAssetsBatch(AssetBatchRequest request) {
        String url = worldTerrainUrl + "/api/assets/batch";
        ResponseEntity<AssetDto[]> response = restTemplate.postForEntity(url, request, AssetDto[].class);
        AssetDto[] body = response.getBody();
        return body != null ? List.of(body) : List.of();
    }

    // Group Management
    public GroupDto createGroup(GroupCreateRequest request) {
        String url = worldTerrainUrl + "/api/groups";
        ResponseEntity<GroupDto> response = restTemplate.postForEntity(url, request, GroupDto.class);
        return response.getBody();
    }

    public Optional<GroupDto> getGroup(String world, Long id) {
        try {
            String url = worldTerrainUrl + "/api/groups/" + world + "/" + id;
            ResponseEntity<GroupDto> response = restTemplate.getForEntity(url, GroupDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to get group {} in world {}: {}", id, world, e.getMessage());
            return Optional.empty();
        }
    }

    public Page<GroupDto> getGroups(String world, String type, int page, int size) {
        String url = UriComponentsBuilder.fromUriString(worldTerrainUrl + "/api/groups/" + world)
                .queryParamIfPresent("type", Optional.ofNullable(type))
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();

        ResponseEntity<PageImpl<GroupDto>> response = restTemplate.exchange(
            url, HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {});

        return response.getBody();
    }

    public Optional<GroupDto> updateGroup(String world, Long id, GroupDto groupDto) {
        try {
            String url = worldTerrainUrl + "/api/groups/" + world + "/" + id;
            ResponseEntity<GroupDto> response = restTemplate.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(groupDto), GroupDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to update group {} in world {}: {}", id, world, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean deleteGroup(String world, Long id) {
        try {
            String url = worldTerrainUrl + "/api/groups/" + world + "/" + id;
            restTemplate.delete(url);
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete group {} in world {}: {}", id, world, e.getMessage());
            return false;
        }
    }
}
