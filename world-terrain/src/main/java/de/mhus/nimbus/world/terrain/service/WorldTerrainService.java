package de.mhus.nimbus.world.terrain.service;

import de.mhus.nimbus.shared.dto.world.*;
import de.mhus.nimbus.world.terrain.entity.*;
import de.mhus.nimbus.world.terrain.repository.*;
import de.mhus.nimbus.world.terrain.exception.WorldNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
                .sizeX(worldDto.getSizeX())
                .sizeY(worldDto.getSizeY())
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
                    entity.setSizeX(worldDto.getSizeX());
                    entity.setSizeY(worldDto.getSizeY());
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

    // Material Management - now using JSON serialization
    public MaterialDto createMaterial(MaterialDto materialDto) {
        MaterialEntity entity = MaterialEntity.builder()
                .name(materialDto.getName())
                .data(serializeToJson(materialDto))
                .build();

        MaterialEntity saved = materialRepository.save(entity);
        return deserializeFromJson(saved.getData(), MaterialDto.class);
    }

    public Optional<MaterialDto> getMaterial(Integer id) {
        return materialRepository.findById(id)
                .map(entity -> deserializeFromJson(entity.getData(), MaterialDto.class));
    }

    public Page<MaterialDto> getMaterials(Pageable pageable) {
        Page<MaterialEntity> entities = materialRepository.findAll(pageable);
        List<MaterialDto> dtos = entities.getContent().stream()
                .map(entity -> deserializeFromJson(entity.getData(), MaterialDto.class))
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, entities.getTotalElements());
    }

    public Optional<MaterialDto> updateMaterial(Integer id, MaterialDto materialDto) {
        return materialRepository.findById(id)
                .map(entity -> {
                    materialDto.setId(id); // Ensure ID consistency
                    entity.setData(serializeToJson(materialDto));
                    entity.setName(materialDto.getName()); // Keep name for indexing
                    return deserializeFromJson(materialRepository.save(entity).getData(), MaterialDto.class);
                });
    }

    public boolean deleteMaterial(Integer id) {
        if (materialRepository.existsById(id)) {
            materialRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Map Management - now using JSON serialization for clusters
    public void createMap(MapCreateRequest request) {
        validateWorldExists(request.getWorld());

        for (TerrainClusterDto cluster : request.getClusters()) {
            MapEntity entity = mapRepository.findByWorldAndLevelAndClusterXAndClusterY(
                    request.getWorld(), cluster.getLevel(), cluster.getX(), cluster.getY())
                    .orElse(MapEntity.builder()
                            .world(request.getWorld())
                            .level(cluster.getLevel())
                            .clusterX(cluster.getX())
                            .clusterY(cluster.getY())
                            .build());

            // Merge existing fields with new fields
            List<TerrainFieldDto> existingFields = parseExistingFields(entity.getData());
            List<TerrainFieldDto> mergedFields = mergeFields(existingFields, cluster.getFields());

            TerrainClusterDto mergedCluster = TerrainClusterDto.builder()
                    .x(cluster.getX())
                    .y(cluster.getY())
                    .level(cluster.getLevel())
                    .fields(mergedFields)
                    .build();

            entity.setData(serializeToJson(mergedCluster));
            entity.setCompressed(null); // Reset compression when data changes
            mapRepository.save(entity);
        }
    }

    public Optional<TerrainClusterDto> getMap(String world, Integer level, Integer clusterX, Integer clusterY) {
        validateWorldExists(world);

        return mapRepository.findByWorldAndLevelAndClusterXAndClusterY(world, level, clusterX, clusterY)
                .map(entity -> deserializeFromJson(entity.getData(), TerrainClusterDto.class));
    }

    public List<TerrainClusterDto> getMapBatch(MapBatchRequest request) {
        validateWorldExists(request.getWorld());

        List<Object[]> coordinates = request.getClusters().stream()
                .map(coord -> new Object[]{coord.getX(), coord.getY()})
                .collect(Collectors.toList());

        return mapRepository.findByWorldAndLevelAndClusterCoordinates(
                request.getWorld(), request.getLevel(), coordinates)
                .stream()
                .map(entity -> deserializeFromJson(entity.getData(), TerrainClusterDto.class))
                .collect(Collectors.toList());
    }

    public void updateMap(MapCreateRequest request) {
        createMap(request); // Same logic as create - merge fields
    }

    public void deleteMapLevel(String world, Integer level) {
        validateWorldExists(world);
        mapRepository.deleteByWorldAndLevel(world, level);
    }

    // Sprite Management - now using JSON serialization
    public List<String> createSprites(SpriteCreateRequest request) {
        validateWorldExists(request.getWorld());

        List<String> createdIds = new ArrayList<>();

        for (SpriteDto spriteDto : request.getSprites()) {
            String id = "S" + UUID.randomUUID().toString();
            spriteDto.setId(id);

            // Calculate cluster positions based on sprite position and size
            int[] clusterPositions = calculateClusterPositions(spriteDto);

            SpriteEntity entity = SpriteEntity.builder()
                    .id(id)
                    .world(request.getWorld())
                    .level(request.getLevel())
                    .enabled(true)
                    .clusterX0(clusterPositions[0])
                    .clusterY0(clusterPositions[1])
                    .clusterX1(clusterPositions.length > 2 ? clusterPositions[2] : null)
                    .clusterY1(clusterPositions.length > 3 ? clusterPositions[3] : null)
                    .clusterX2(clusterPositions.length > 4 ? clusterPositions[4] : null)
                    .clusterY2(clusterPositions.length > 5 ? clusterPositions[5] : null)
                    .clusterX3(clusterPositions.length > 6 ? clusterPositions[6] : null)
                    .clusterY3(clusterPositions.length > 7 ? clusterPositions[7] : null)
                    .data(serializeToJson(spriteDto))
                    .build();

            spriteRepository.save(entity);
            createdIds.add(id);
        }

        return createdIds;
    }

    public Optional<SpriteDto> getSprite(String id) {
        return spriteRepository.findById(id)
                .map(entity -> deserializeFromJson(entity.getData(), SpriteDto.class));
    }

    public List<SpriteDto> getSpritesInCluster(String world, Integer level, Integer clusterX, Integer clusterY) {
        validateWorldExists(world);

        return spriteRepository.findSpritesInCluster(world, level, true, clusterX, clusterY)
                .stream()
                .map(entity -> deserializeFromJson(entity.getData(), SpriteDto.class))
                .collect(Collectors.toList());
    }

    public Optional<SpriteDto> updateSprite(String id, SpriteDto spriteDto) {
        return spriteRepository.findById(id)
                .map(entity -> {
                    spriteDto.setId(id); // Ensure ID consistency
                    entity.setData(serializeToJson(spriteDto));
                    entity.setCompressed(null); // Reset compression when data changes
                    return deserializeFromJson(spriteRepository.save(entity).getData(), SpriteDto.class);
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
                    SpriteDto spriteDto = deserializeFromJson(entity.getData(), SpriteDto.class);
                    spriteDto.setX(request.getX());
                    spriteDto.setY(request.getY());
                    if (request.getZ() != null) {
                        spriteDto.setZ(request.getZ());
                    }

                    // Recalculate cluster positions
                    int[] clusterPositions = calculateClusterPositions(spriteDto);
                    entity.setClusterX0(clusterPositions[0]);
                    entity.setClusterY0(clusterPositions[1]);
                    entity.setClusterX1(clusterPositions.length > 2 ? clusterPositions[2] : null);
                    entity.setClusterY1(clusterPositions.length > 3 ? clusterPositions[3] : null);
                    entity.setClusterX2(clusterPositions.length > 4 ? clusterPositions[4] : null);
                    entity.setClusterY2(clusterPositions.length > 5 ? clusterPositions[5] : null);
                    entity.setClusterX3(clusterPositions.length > 6 ? clusterPositions[6] : null);
                    entity.setClusterY3(clusterPositions.length > 7 ? clusterPositions[7] : null);

                    entity.setData(serializeToJson(spriteDto));
                    entity.setCompressed(null);
                    return deserializeFromJson(spriteRepository.save(entity).getData(), SpriteDto.class);
                });
    }

    public Optional<SpriteDto> enableSprite(String id) {
        return spriteRepository.findById(id)
                .map(entity -> {
                    entity.setEnabled(true);
                    return deserializeFromJson(spriteRepository.save(entity).getData(), SpriteDto.class);
                });
    }

    public Optional<SpriteDto> disableSprite(String id) {
        return spriteRepository.findById(id)
                .map(entity -> {
                    entity.setEnabled(false);
                    return deserializeFromJson(spriteRepository.save(entity).getData(), SpriteDto.class);
                });
    }

    // Asset Management - now using JSON serialization for properties
    public AssetDto createAsset(String world, AssetCreateRequest request) {
        validateWorldExists(world);

        AssetEntity entity = AssetEntity.builder()
                .world(world)
                .name(request.getName())
                .type(request.getType())
                .data(request.getData())
                .properties(serializeToJson(request.getProperties()))
                .build();

        AssetEntity saved = assetRepository.save(entity);
        return mapToAssetDto(saved);
    }

    public Optional<AssetDto> getAsset(String world, String name) {
        validateWorldExists(world);

        return assetRepository.findByWorldAndName(world, name)
                .map(this::mapToAssetDto);
    }

    public List<AssetDto> getAssets(String world) {
        validateWorldExists(world);

        return assetRepository.findByWorld(world).stream()
                .map(this::mapToAssetDto)
                .collect(Collectors.toList());
    }

    public Optional<AssetDto> updateAsset(String world, String name, AssetCreateRequest request) {
        validateWorldExists(world);

        return assetRepository.findByWorldAndName(world, name)
                .map(entity -> {
                    entity.setType(request.getType());
                    entity.setData(request.getData());
                    entity.setProperties(serializeToJson(request.getProperties()));
                    entity.setCompressed(null); // Reset compression when data changes
                    return mapToAssetDto(assetRepository.save(entity));
                });
    }

    public boolean deleteAsset(String world, String name) {
        validateWorldExists(world);

        if (assetRepository.existsByWorldAndName(world, name)) {
            assetRepository.deleteByWorldAndName(world, name);
            return true;
        }
        return false;
    }

    public List<AssetDto> getAssetsBatch(String world, List<String> names) {
        validateWorldExists(world);

        return names.stream()
                .map(name -> assetRepository.findByWorldAndName(world, name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::mapToAssetDto)
                .collect(Collectors.toList());
    }

    public void compressAssets(String world, List<String> names) {
        validateWorldExists(world);

        names.forEach(name -> {
            assetRepository.findByWorldAndName(world, name)
                    .ifPresent(entity -> {
                        // TODO: Implement compression logic
                        entity.setCompressedAt(LocalDateTime.now());
                        assetRepository.save(entity);
                    });
        });
    }

    // Group Management - now using JSON serialization
    public GroupDto createGroup(String world, GroupCreateRequest request) {
        validateWorldExists(world);

        Long groupId = generateGroupId(); // GroupCreateRequest hat keine ID, generiere immer eine neue

        // Convert Map<String,String> to Map<String,Object> for TerrainGroupDto
        Map<String, Object> objectProperties = new HashMap<>();
        if (request.getProperties() != null) {
            objectProperties.putAll(request.getProperties());
        }

        TerrainGroupDto groupDto = TerrainGroupDto.builder()
                .id(groupId)
                .name(request.getName())
                .type(request.getType())
                .properties(objectProperties)
                .build();

        TerrainGroupEntity entity = TerrainGroupEntity.builder()
                .world(world)
                .groupId(groupId)
                .name(request.getName())
                .type(request.getType())
                .data(serializeToJson(groupDto))
                .build();

        TerrainGroupEntity saved = terrainGroupRepository.save(entity);
        return mapToGroupDto(deserializeFromJson(saved.getData(), TerrainGroupDto.class));
    }

    public Optional<GroupDto> getGroup(String world, Long groupId) {
        validateWorldExists(world);

        return terrainGroupRepository.findByWorldAndGroupId(world, groupId)
                .map(entity -> mapToGroupDto(deserializeFromJson(entity.getData(), TerrainGroupDto.class)));
    }

    public List<GroupDto> getGroups(String world) {
        validateWorldExists(world);

        return terrainGroupRepository.findByWorld(world).stream()
                .map(entity -> mapToGroupDto(deserializeFromJson(entity.getData(), TerrainGroupDto.class)))
                .collect(Collectors.toList());
    }

    public Optional<GroupDto> updateGroup(String world, Long groupId, GroupCreateRequest request) {
        validateWorldExists(world);

        return terrainGroupRepository.findByWorldAndGroupId(world, groupId)
                .map(entity -> {
                    // Convert Map<String,String> to Map<String,Object> for TerrainGroupDto
                    Map<String, Object> objectProperties = new HashMap<>();
                    if (request.getProperties() != null) {
                        objectProperties.putAll(request.getProperties());
                    }

                    TerrainGroupDto groupDto = TerrainGroupDto.builder()
                            .id(groupId)
                            .name(request.getName())
                            .type(request.getType())
                            .properties(objectProperties)
                            .build();

                    entity.setName(request.getName());
                    entity.setType(request.getType());
                    entity.setData(serializeToJson(groupDto));

                    TerrainGroupEntity saved = terrainGroupRepository.save(entity);
                    return mapToGroupDto(deserializeFromJson(saved.getData(), TerrainGroupDto.class));
                });
    }

    public boolean deleteGroup(String world, Long groupId) {
        validateWorldExists(world);

        if (terrainGroupRepository.existsByWorldAndGroupId(world, groupId)) {
            terrainGroupRepository.deleteByWorldAndGroupId(world, groupId);
            return true;
        }
        return false;
    }

    // Utility methods
    private void validateWorldExists(String worldId) {
        if (!worldRepository.existsById(worldId)) {
            throw new WorldNotFoundException("World not found: " + worldId);
        }
    }

    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON", e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    private <T> T deserializeFromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Failed to deserialize JSON to object", e);
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }

    private List<TerrainFieldDto> parseExistingFields(String data) {
        if (data == null || data.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            TerrainClusterDto cluster = objectMapper.readValue(data, TerrainClusterDto.class);
            return cluster.getFields() != null ? cluster.getFields() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Failed to parse existing fields, starting with empty list", e);
            return new ArrayList<>();
        }
    }

    private List<TerrainFieldDto> mergeFields(List<TerrainFieldDto> existing, List<TerrainFieldDto> newFields) {
        Map<String, TerrainFieldDto> fieldMap = new HashMap<>();

        // Add existing fields
        for (TerrainFieldDto field : existing) {
            String key = field.getX() + "," + field.getY() + "," + field.getZ();
            fieldMap.put(key, field);
        }

        // Merge or add new fields
        for (TerrainFieldDto newField : newFields) {
            String key = newField.getX() + "," + newField.getY() + "," + newField.getZ();
            TerrainFieldDto existingField = fieldMap.get(key);

            if (existingField != null) {
                // Merge fields
                fieldMap.put(key, mergeField(existingField, newField));
            } else {
                // Add new field
                fieldMap.put(key, newField);
            }
        }

        return new ArrayList<>(fieldMap.values());
    }

    private TerrainFieldDto mergeField(TerrainFieldDto existing, TerrainFieldDto newField) {
        return TerrainFieldDto.builder()
                .x(newField.getX())
                .y(newField.getY())
                .z(newField.getZ())
                .groups(newField.getGroups() != null ? newField.getGroups() : existing.getGroups())
                .materials(newField.getMaterials() != null ? newField.getMaterials() : existing.getMaterials())
                .opacity(newField.getOpacity() != null ? newField.getOpacity() : existing.getOpacity())
                .sizeZ(newField.getSizeZ() != null ? newField.getSizeZ() : existing.getSizeZ())
                .parameters(mergeParameters(existing.getParameters(), newField.getParameters()))
                .build();
    }

    private Map<String, String> mergeParameters(Map<String, String> existing, Map<String, String> newParams) {
        Map<String, String> merged = new HashMap<>();
        if (existing != null) {
            merged.putAll(existing);
        }
        if (newParams != null) {
            merged.putAll(newParams);
        }
        return merged;
    }

    private int[] calculateClusterPositions(SpriteDto sprite) {
        final int CLUSTER_SIZE = 32;

        int clusterX0 = sprite.getX() / CLUSTER_SIZE;
        int clusterY0 = sprite.getY() / CLUSTER_SIZE;

        List<Integer> positions = new ArrayList<>();
        positions.add(clusterX0);
        positions.add(clusterY0);

        // Check if sprite spans multiple clusters
        int endX = sprite.getX() + sprite.getSizeX() - 1;
        int endY = sprite.getY() + sprite.getSizeY() - 1;

        int clusterX1 = endX / CLUSTER_SIZE;
        int clusterY1 = endY / CLUSTER_SIZE;

        if (clusterX1 != clusterX0 || clusterY1 != clusterY0) {
            // Sprite spans multiple clusters
            if (clusterX1 != clusterX0 && clusterY1 == clusterY0) {
                // Spans horizontally
                positions.add(clusterX1);
                positions.add(clusterY0);
            } else if (clusterX1 == clusterX0 && clusterY1 != clusterY0) {
                // Spans vertically
                positions.add(clusterX0);
                positions.add(clusterY1);
            } else {
                // Spans both directions (up to 4 clusters)
                positions.add(clusterX1);
                positions.add(clusterY0);
                positions.add(clusterX0);
                positions.add(clusterY1);
                positions.add(clusterX1);
                positions.add(clusterY1);
            }
        }

        return positions.stream().mapToInt(Integer::intValue).toArray();
    }

    private Long generateGroupId() {
        // Generate a unique group ID
        return System.currentTimeMillis();
    }

    // Mapping methods
    private WorldDto mapToWorldDto(WorldEntity entity) {
        return WorldDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .sizeX(entity.getSizeX())
                .sizeY(entity.getSizeY())
                .properties(parseJsonToMap(entity.getProperties()))
                .createdAt(entity.getCreatedAt() != null ? Date.from(entity.getCreatedAt()) : null)
                .updatedAt(entity.getUpdatedAt() != null ? Date.from(entity.getUpdatedAt()) : null)
                .build();
    }

    private AssetDto mapToAssetDto(AssetEntity entity) {
        return AssetDto.builder()
                .world(entity.getWorld())
                .name(entity.getName())
                .type(entity.getType())
                .data(entity.getData())
                .properties(parseJsonToMap(entity.getProperties()))
                .createdAt(entity.getCreatedAt() != null ? Date.from(entity.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()) : null)
                .updatedAt(entity.getUpdatedAt() != null ? Date.from(entity.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()) : null)
                .compressedAt(entity.getCompressedAt() != null ? Date.from(entity.getCompressedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()) : null)
                .build();
    }

    private GroupDto mapToGroupDto(TerrainGroupDto terrainGroupDto) {
        // Convert Map<String,Object> to Map<String,String> for GroupDto
        Map<String, String> stringProperties = new HashMap<>();
        if (terrainGroupDto.getProperties() != null) {
            terrainGroupDto.getProperties().forEach((key, value) ->
                stringProperties.put(key, value != null ? value.toString() : null)
            );
        }

        return GroupDto.builder()
                .id(terrainGroupDto.getId())
                .name(terrainGroupDto.getName())
                .type(terrainGroupDto.getType())
                .properties(stringProperties)
                .build();
    }

    private String mapToJson(Map<String, Object> map) {
        if (map == null) return "{}";
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to convert map to JSON", e);
            return "{}";
        }
    }

    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to parse JSON to map", e);
            return new HashMap<>();
        }
    }
}
