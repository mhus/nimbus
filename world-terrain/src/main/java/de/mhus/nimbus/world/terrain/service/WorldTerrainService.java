package de.mhus.nimbus.world.terrain.service;

import de.mhus.nimbus.shared.dto.world.*;
import de.mhus.nimbus.world.terrain.entity.*;
import de.mhus.nimbus.world.terrain.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorldTerrainService {

    private final WorldRepository worldRepository;
    private final MaterialRepository materialRepository;
    private final MapRepository mapRepository;
    private final SpriteRepository spriteRepository;
    private final TerrainGroupRepository terrainGroupRepository;
    private final AssetRepository assetRepository;
    private final ObjectMapper objectMapper;

    // World Management
    public WorldDto createWorld(WorldDto worldDto) {
        WorldEntity entity = WorldEntity.builder()
                .id(worldDto.getId() != null ? worldDto.getId() : UUID.randomUUID().toString())
                .name(worldDto.getName())
                .description(worldDto.getDescription())
                .properties(mapToJson(worldDto.getProperties()))
                .build();

        WorldEntity saved = worldRepository.save(entity);
        return mapToWorldDto(saved);
    }

    public Optional<WorldDto> getWorld(String id) {
        return worldRepository.findById(id)
                .map(this::mapToWorldDto);
    }

    public List<WorldDto> getAllWorlds() {
        return worldRepository.findAll().stream()
                .map(this::mapToWorldDto)
                .collect(Collectors.toList());
    }

    public Optional<WorldDto> updateWorld(String id, WorldDto worldDto) {
        return worldRepository.findById(id)
                .map(entity -> {
                    entity.setName(worldDto.getName());
                    entity.setDescription(worldDto.getDescription());
                    entity.setProperties(mapToJson(worldDto.getProperties()));
                    return mapToWorldDto(worldRepository.save(entity));
                });
    }

    public boolean deleteWorld(String id) {
        if (worldRepository.existsById(id)) {
            worldRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Material Management
    public MaterialDto createMaterial(MaterialDto materialDto) {
        MaterialEntity entity = MaterialEntity.builder()
                .name(materialDto.getName())
                .blocking(materialDto.getBlocking())
                .friction(materialDto.getFriction())
                .color(materialDto.getColor())
                .texture(materialDto.getTexture())
                .soundWalk(materialDto.getSoundWalk())
                .properties(mapToJson(materialDto.getProperties()))
                .build();

        MaterialEntity saved = materialRepository.save(entity);
        return mapToMaterialDto(saved);
    }

    public Optional<MaterialDto> getMaterial(Integer id) {
        return materialRepository.findById(id)
                .map(this::mapToMaterialDto);
    }

    public Page<MaterialDto> getMaterials(String name, Pageable pageable) {
        Page<MaterialEntity> entities;
        if (name != null && !name.trim().isEmpty()) {
            entities = materialRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            entities = materialRepository.findAll(pageable);
        }
        return entities.map(this::mapToMaterialDto);
    }

    public Optional<MaterialDto> updateMaterial(Integer id, MaterialDto materialDto) {
        return materialRepository.findById(id)
                .map(entity -> {
                    entity.setName(materialDto.getName());
                    entity.setBlocking(materialDto.getBlocking());
                    entity.setFriction(materialDto.getFriction());
                    entity.setColor(materialDto.getColor());
                    entity.setTexture(materialDto.getTexture());
                    entity.setSoundWalk(materialDto.getSoundWalk());
                    entity.setProperties(mapToJson(materialDto.getProperties()));
                    return mapToMaterialDto(materialRepository.save(entity));
                });
    }

    public boolean deleteMaterial(Integer id) {
        if (materialRepository.existsById(id)) {
            materialRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Map Management
    public void createOrUpdateMap(MapCreateRequest request) {
        for (TerrainClusterDto cluster : request.getClusters()) {
            MapEntity entity = mapRepository.findByWorldAndLevelAndClusterXAndClusterY(
                    request.getWorld(), cluster.getLevel(), cluster.getX(), cluster.getY())
                    .orElse(MapEntity.builder()
                            .world(request.getWorld())
                            .level(cluster.getLevel())
                            .clusterX(cluster.getX())
                            .clusterY(cluster.getY())
                            .build());

            entity.setData(mapToJson(cluster.getFields()));
            entity.setCompressed(null); // Reset compression
            mapRepository.save(entity);
        }
    }

    public Optional<TerrainClusterDto> getMapCluster(String world, Integer level, Integer clusterX, Integer clusterY) {
        return mapRepository.findByWorldAndLevelAndClusterXAndClusterY(world, level, clusterX, clusterY)
                .map(entity -> TerrainClusterDto.builder()
                        .level(entity.getLevel())
                        .x(entity.getClusterX())
                        .y(entity.getClusterY())
                        .fields(parseJsonList(entity.getData(), TerrainFieldDto.class))
                        .build());
    }

    public List<TerrainClusterDto> getMapClusters(MapBatchRequest request) {
        List<TerrainClusterDto> result = new ArrayList<>();

        for (ClusterCoordinateDto coord : request.getClusters()) {
            getMapCluster(request.getWorld(), request.getLevel(), coord.getX(), coord.getY())
                    .ifPresent(result::add);
        }

        return result;
    }

    public void deleteMapFields(MapDeleteRequest request) {
        for (TerrainClusterDeleteDto clusterDelete : request.getClusters()) {
            Optional<MapEntity> entityOpt = mapRepository.findByWorldAndLevelAndClusterXAndClusterY(
                    request.getWorld(), request.getLevel(), clusterDelete.getX(), clusterDelete.getY());

            if (entityOpt.isPresent()) {
                MapEntity entity = entityOpt.get();
                List<TerrainFieldDto> existingFields = parseJsonList(entity.getData(), TerrainFieldDto.class);

                // Remove specified fields
                Set<String> fieldsToRemove = clusterDelete.getFields().stream()
                        .map(field -> field.getX() + "," + field.getY())
                        .collect(Collectors.toSet());

                List<TerrainFieldDto> filteredFields = existingFields.stream()
                        .filter(field -> !fieldsToRemove.contains(field.getX() + "," + field.getY()))
                        .collect(Collectors.toList());

                if (filteredFields.isEmpty()) {
                    mapRepository.delete(entity);
                } else {
                    entity.setData(mapToJson(filteredFields));
                    entity.setCompressed(null);
                    mapRepository.save(entity);
                }
            }
        }
    }

    public void deleteLevel(String world, Integer level) {
        mapRepository.deleteByWorldAndLevel(world, level);
    }

    // Asset Management
    public AssetDto createAsset(AssetDto assetDto) {
        AssetEntity entity = AssetEntity.builder()
                .world(assetDto.getWorld())
                .name(assetDto.getName())
                .type(assetDto.getType())
                .data(assetDto.getData())
                .properties(mapToJson(assetDto.getProperties()))
                .build();

        AssetEntity saved = assetRepository.save(entity);
        return mapToAssetDto(saved);
    }

    public Optional<AssetDto> getAsset(String world, String name) {
        return assetRepository.findByWorldAndName(world, name)
                .map(this::mapToAssetDto);
    }

    public Page<AssetDto> getAssets(String world, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return assetRepository.findByWorld(world, pageable)
                .map(this::mapToAssetDto);
    }

    public List<AssetDto> getAssetsBatch(AssetBatchRequest request) {
        List<AssetEntity> entities = assetRepository.findByWorldAndNameIn(
                request.getWorld(), request.getAssets());
        return entities.stream()
                .map(this::mapToAssetDto)
                .collect(Collectors.toList());
    }

    public Optional<AssetDto> updateAsset(String world, String name, AssetDto assetDto) {
        return assetRepository.findByWorldAndName(world, name)
                .map(entity -> {
                    entity.setType(assetDto.getType());
                    entity.setData(assetDto.getData());
                    entity.setProperties(mapToJson(assetDto.getProperties()));
                    entity.setCompressed(null); // Reset compression when updating
                    return mapToAssetDto(assetRepository.save(entity));
                });
    }

    public boolean deleteAsset(String world, String name) {
        if (assetRepository.existsByWorldAndName(world, name)) {
            assetRepository.deleteByWorldAndName(world, name);
            return true;
        }
        return false;
    }

    public void compressAssets(String world) {
        // This would be implemented as a background job in a real system
        // For now, it's a placeholder that could trigger asset compression
        log.info("Asset compression triggered for world: {}", world);
        // Implementation would involve finding uncompressed assets and compressing them
    }

    // Terrain Group Management
    public TerrainGroupDto createGroup(String world, TerrainGroupDto groupDto) {
        TerrainGroupEntity entity = TerrainGroupEntity.builder()
                .world(world)
                .name(groupDto.getName())
                .type(groupDto.getType())
                .data(mapToJson(groupDto.getProperties()))
                .build();

        TerrainGroupEntity saved = terrainGroupRepository.save(entity);
        return mapToTerrainGroupDto(saved);
    }

    public Optional<TerrainGroupDto> getGroup(String world, Long id) {
        return terrainGroupRepository.findByWorldAndName(world, String.valueOf(id))
                .or(() -> terrainGroupRepository.findById(id)
                        .filter(entity -> entity.getWorld().equals(world)))
                .map(this::mapToTerrainGroupDto);
    }

    public List<TerrainGroupDto> getGroups(String world) {
        return terrainGroupRepository.findByWorld(world).stream()
                .map(this::mapToTerrainGroupDto)
                .collect(Collectors.toList());
    }

    public Optional<TerrainGroupDto> updateGroup(String world, Long id, TerrainGroupDto groupDto) {
        return terrainGroupRepository.findById(id)
                .filter(entity -> entity.getWorld().equals(world))
                .map(entity -> {
                    entity.setName(groupDto.getName());
                    entity.setType(groupDto.getType());
                    entity.setData(mapToJson(groupDto.getProperties()));
                    return mapToTerrainGroupDto(terrainGroupRepository.save(entity));
                });
    }

    public boolean deleteGroup(String world, Long id) {
        Optional<TerrainGroupEntity> entityOpt = terrainGroupRepository.findById(id)
                .filter(entity -> entity.getWorld().equals(world));

        if (entityOpt.isPresent()) {
            terrainGroupRepository.delete(entityOpt.get());
            return true;
        }
        return false;
    }

    // Sprite Management
    public List<String> createSprites(SpriteCreateRequest request) {
        List<String> spriteIds = new ArrayList<>();

        for (SpriteDto spriteDto : request.getSprites()) {
            String spriteId = "S" + UUID.randomUUID().toString();

            // Calculate cluster positions for sprite placement
            int clusterX0 = spriteDto.getX() / 32; // CLUSTER_SIZE = 32
            int clusterY0 = spriteDto.getY() / 32;

            // Check if sprite spans multiple clusters
            Integer clusterX1 = null, clusterY1 = null, clusterX2 = null, clusterY2 = null, clusterX3 = null, clusterY3 = null;

            int maxX = spriteDto.getX() + (spriteDto.getSizeX() != null ? spriteDto.getSizeX() : 1);
            int maxY = spriteDto.getY() + (spriteDto.getSizeY() != null ? spriteDto.getSizeY() : 1);

            int endClusterX = (maxX - 1) / 32;
            int endClusterY = (maxY - 1) / 32;

            if (endClusterX > clusterX0) {
                clusterX1 = endClusterX;
                clusterY1 = clusterY0;
            }
            if (endClusterY > clusterY0) {
                clusterX2 = clusterX0;
                clusterY2 = endClusterY;
            }
            if (endClusterX > clusterX0 && endClusterY > clusterY0) {
                clusterX3 = endClusterX;
                clusterY3 = endClusterY;
            }

            SpriteEntity entity = SpriteEntity.builder()
                    .id(spriteId)
                    .world(request.getWorld())
                    .level(request.getLevel())
                    .enabled(true)
                    .clusterX0(clusterX0)
                    .clusterY0(clusterY0)
                    .clusterX1(clusterX1)
                    .clusterY1(clusterY1)
                    .clusterX2(clusterX2)
                    .clusterY2(clusterY2)
                    .clusterX3(clusterX3)
                    .clusterY3(clusterY3)
                    .data(mapToJson(spriteDto))
                    .build();

            spriteRepository.save(entity);
            spriteIds.add(spriteId);
        }

        return spriteIds;
    }

    public Optional<SpriteDto> getSprite(String id) {
        return spriteRepository.findById(id)
                .map(this::mapToSpriteDto);
    }

    public List<SpriteDto> getSpritesInCluster(String world, Integer level, Integer x, Integer y) {
        return spriteRepository.findByWorldAndLevelAndCluster(world, level, x, y)
                .stream()
                .map(this::mapToSpriteDto)
                .collect(Collectors.toList());
    }

    public Optional<SpriteDto> updateSprite(String id, SpriteDto spriteDto) {
        return spriteRepository.findById(id)
                .map(entity -> {
                    // Update sprite data
                    entity.setData(mapToJson(spriteDto));
                    entity.setCompressed(null); // Reset compression

                    // Recalculate cluster positions if coordinates changed
                    SpriteDto currentData = parseJson(entity.getData(), SpriteDto.class);
                    if (currentData != null &&
                        (!Objects.equals(currentData.getX(), spriteDto.getX()) ||
                         !Objects.equals(currentData.getY(), spriteDto.getY()) ||
                         !Objects.equals(currentData.getSizeX(), spriteDto.getSizeX()) ||
                         !Objects.equals(currentData.getSizeY(), spriteDto.getSizeY()))) {

                        updateSpriteClusterPositions(entity, spriteDto);
                    }

                    return mapToSpriteDto(spriteRepository.save(entity));
                });
    }

    public boolean deleteSprite(String id) {
        if (spriteRepository.existsById(id)) {
            spriteRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<SpriteDto> updateSpriteCoordinates(String id, SpriteCoordinateUpdateRequest request) {
        return spriteRepository.findById(id)
                .map(entity -> {
                    SpriteDto spriteData = parseJson(entity.getData(), SpriteDto.class);
                    if (spriteData != null) {
                        spriteData.setX(request.getX());
                        spriteData.setY(request.getY());
                        spriteData.setZ(request.getZ());
                        spriteData.setSizeX(request.getSizeX());
                        spriteData.setSizeY(request.getSizeY());
                        spriteData.setSizeZ(request.getSizeZ());

                        entity.setData(mapToJson(spriteData));
                        entity.setCompressed(null);

                        updateSpriteClusterPositions(entity, spriteData);

                        return mapToSpriteDto(spriteRepository.save(entity));
                    }
                    return null;
                })
                .filter(Objects::nonNull);
    }

    public Optional<SpriteDto> enableSprite(String id) {
        return spriteRepository.findById(id)
                .map(entity -> {
                    entity.setEnabled(true);
                    return mapToSpriteDto(spriteRepository.save(entity));
                });
    }

    public Optional<SpriteDto> disableSprite(String id) {
        return spriteRepository.findById(id)
                .map(entity -> {
                    entity.setEnabled(false);
                    return mapToSpriteDto(spriteRepository.save(entity));
                });
    }

    private void updateSpriteClusterPositions(SpriteEntity entity, SpriteDto spriteDto) {
        int clusterX0 = spriteDto.getX() / 32;
        int clusterY0 = spriteDto.getY() / 32;

        int maxX = spriteDto.getX() + (spriteDto.getSizeX() != null ? spriteDto.getSizeX() : 1);
        int maxY = spriteDto.getY() + (spriteDto.getSizeY() != null ? spriteDto.getSizeY() : 1);

        int endClusterX = (maxX - 1) / 32;
        int endClusterY = (maxY - 1) / 32;

        entity.setClusterX0(clusterX0);
        entity.setClusterY0(clusterY0);

        entity.setClusterX1(endClusterX > clusterX0 ? endClusterX : null);
        entity.setClusterY1(endClusterX > clusterX0 ? clusterY0 : null);

        entity.setClusterX2(endClusterY > clusterY0 ? clusterX0 : null);
        entity.setClusterY2(endClusterY > clusterY0 ? endClusterY : null);

        entity.setClusterX3((endClusterX > clusterX0 && endClusterY > clusterY0) ? endClusterX : null);
        entity.setClusterY3((endClusterX > clusterX0 && endClusterY > clusterY0) ? endClusterY : null);
    }

    // Utility methods
    private WorldDto mapToWorldDto(WorldEntity entity) {
        return WorldDto.builder()
                .id(entity.getId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .name(entity.getName())
                .description(entity.getDescription())
                .properties(parseJsonMap(entity.getProperties()))
                .build();
    }

    private MaterialDto mapToMaterialDto(MaterialEntity entity) {
        return MaterialDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .blocking(entity.getBlocking())
                .friction(entity.getFriction())
                .color(entity.getColor())
                .texture(entity.getTexture())
                .soundWalk(entity.getSoundWalk())
                .properties(parseJsonMapString(entity.getProperties()))
                .build();
    }

    private AssetDto mapToAssetDto(AssetEntity entity) {
        return AssetDto.builder()
                .world(entity.getWorld())
                .name(entity.getName())
                .type(entity.getType())
                .data(entity.getData())
                .properties(parseJsonMap(entity.getProperties()))
                .build();
    }

    private SpriteDto mapToSpriteDto(SpriteEntity entity) {
        SpriteDto spriteData = parseJson(entity.getData(), SpriteDto.class);
        if (spriteData != null) {
            spriteData.setId(entity.getId());
            return spriteData;
        }
        return SpriteDto.builder()
                .id(entity.getId())
                .build();
    }

    private TerrainGroupDto mapToTerrainGroupDto(TerrainGroupEntity entity) {
        return TerrainGroupDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .properties(parseJsonMap(entity.getData()))
                .build();
    }

    private String mapToJson(Object object) {
        if (object == null) return null;
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Error converting object to JSON", e);
            return null;
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.trim().isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Error parsing JSON to Map<String, Object>", e);
            return new HashMap<>();
        }
    }

    private Map<String, String> parseJsonMapString(String json) {
        if (json == null || json.trim().isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Error parsing JSON to Map<String, String>", e);
            return new HashMap<>();
        }
    }

    private <T> List<T> parseJsonList(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            log.error("Error parsing JSON to List<{}>", clazz.getSimpleName(), e);
            return new ArrayList<>();
        }
    }

    private <T> T parseJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Error parsing JSON to {}", clazz.getSimpleName(), e);
            return null;
        }
    }
}
