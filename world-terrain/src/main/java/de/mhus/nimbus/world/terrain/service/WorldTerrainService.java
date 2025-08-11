package de.mhus.nimbus.world.terrain.service;

import de.mhus.nimbus.shared.dto.terrain.*;
import de.mhus.nimbus.shared.dto.terrain.request.*;
import de.mhus.nimbus.world.terrain.entity.*;
import de.mhus.nimbus.world.terrain.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorldTerrainService {

    private final MaterialRepository materialRepository;
    private final MapClusterRepository mapClusterRepository;
    private final SpriteRepository spriteRepository;
    private final AssetRepository assetRepository;
    private final TerrainGroupRepository terrainGroupRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int CLUSTER_SIZE = 32;

    // Material operations
    public MaterialDto createMaterial(MaterialDto materialDto) {
        Material material = Material.builder()
                .name(materialDto.getName())
                .blocking(materialDto.getBlocking())
                .friction(materialDto.getFriction())
                .color(materialDto.getColor())
                .texture(materialDto.getTexture())
                .soundWalk(materialDto.getSoundWalk())
                .properties(convertPropertiesToJson(materialDto.getProperties()))
                .build();

        Material saved = materialRepository.save(material);
        publishMaterialEvent("material.created", saved);
        return convertMaterialToDto(saved);
    }

    public Optional<MaterialDto> getMaterial(Integer id) {
        return materialRepository.findById(id)
                .map(this::convertMaterialToDto);
    }

    public Page<MaterialDto> getMaterials(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return materialRepository.findAll(pageable)
                .map(this::convertMaterialToDto);
    }

    @Transactional
    public Optional<MaterialDto> updateMaterial(Integer id, MaterialDto materialDto) {
        return materialRepository.findById(id)
                .map(material -> {
                    material.setName(materialDto.getName());
                    material.setBlocking(materialDto.getBlocking());
                    material.setFriction(materialDto.getFriction());
                    material.setColor(materialDto.getColor());
                    material.setTexture(materialDto.getTexture());
                    material.setSoundWalk(materialDto.getSoundWalk());
                    material.setProperties(convertPropertiesToJson(materialDto.getProperties()));

                    Material saved = materialRepository.save(material);
                    publishMaterialEvent("material.updated", saved);
                    return convertMaterialToDto(saved);
                });
    }

    @Transactional
    public boolean deleteMaterial(Integer id) {
        if (materialRepository.existsById(id)) {
            materialRepository.deleteById(id);
            publishMaterialEvent("material.deleted", Map.of("id", id));
            return true;
        }
        return false;
    }

    // Map operations
    @Transactional
    public void createMap(MapCreateRequest request) {
        for (ClusterDto cluster : request.getClusters()) {
            saveCluster(request.getWorld(), cluster);
        }
        publishMapEvent("map.created", Map.of("world", request.getWorld(), "clusters", request.getClusters().size()));
    }

    public Optional<ClusterDto> getMap(String world, Integer level, Integer clusterX, Integer clusterY) {
        return mapClusterRepository.findByWorldAndLevelAndClusterXAndClusterY(world, level, clusterX, clusterY)
                .map(this::convertMapClusterToDto);
    }

    public List<ClusterDto> getMapBatch(MapBatchRequest request) {
        return request.getClusters().stream()
                .map(coord -> getMap(request.getWorld(), request.getLevel(), coord.getX(), coord.getY()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateMap(MapCreateRequest request) {
        for (ClusterDto cluster : request.getClusters()) {
            saveCluster(request.getWorld(), cluster);
        }
        publishMapEvent("map.updated", Map.of("world", request.getWorld(), "clusters", request.getClusters().size()));
    }

    @Transactional
    public void deleteMapLevel(String world, Integer level) {
        mapClusterRepository.deleteByWorldAndLevel(world, level);
        publishMapEvent("map.level.deleted", Map.of("world", world, "level", level));
    }

    private void saveCluster(String world, ClusterDto cluster) {
        try {
            Optional<MapCluster> existingCluster = mapClusterRepository
                    .findByWorldAndLevelAndClusterXAndClusterY(world, cluster.getLevel(), cluster.getX(), cluster.getY());

            List<FieldDto> finalFields;
            if (existingCluster.isPresent()) {
                // Merge existing fields with new fields
                List<FieldDto> existingFields = parseExistingFields(existingCluster.get().getData());
                finalFields = mergeFields(existingFields, cluster.getFields());
            } else {
                // No existing cluster, use new fields as-is
                finalFields = cluster.getFields();
            }

            String clusterData = objectMapper.writeValueAsString(finalFields);

            MapCluster mapCluster;
            if (existingCluster.isPresent()) {
                mapCluster = existingCluster.get();
                mapCluster.setData(clusterData);
                mapCluster.setCompressed(null); // Mark for recompression
            } else {
                mapCluster = MapCluster.builder()
                        .world(world)
                        .level(cluster.getLevel())
                        .clusterX(cluster.getX())
                        .clusterY(cluster.getY())
                        .data(clusterData)
                        .build();
            }

            mapClusterRepository.save(mapCluster);
        } catch (Exception e) {
            log.error("Error saving cluster data", e);
            throw new RuntimeException("Failed to save cluster data", e);
        }
    }

    private List<FieldDto> parseExistingFields(String data) {
        try {
            return objectMapper.readValue(data, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, FieldDto.class));
        } catch (Exception e) {
            log.warn("Failed to parse existing cluster data, returning empty list", e);
            return new ArrayList<>();
        }
    }

    private List<FieldDto> mergeFields(List<FieldDto> existingFields, List<FieldDto> newFields) {
        Map<String, FieldDto> fieldMap = new HashMap<>();

        // Add existing fields to map with position as key
        for (FieldDto field : existingFields) {
            String key = field.getX() + "," + field.getY() + "," + field.getZ();
            fieldMap.put(key, field);
        }

        // Merge or add new fields
        for (FieldDto newField : newFields) {
            String key = newField.getX() + "," + newField.getY() + "," + newField.getZ();
            FieldDto existingField = fieldMap.get(key);

            if (existingField != null) {
                // Merge fields at same position
                fieldMap.put(key, mergeField(existingField, newField));
            } else {
                // Add new field
                fieldMap.put(key, newField);
            }
        }

        return new ArrayList<>(fieldMap.values());
    }

    private FieldDto mergeField(FieldDto existing, FieldDto newField) {
        return FieldDto.builder()
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
            merged.putAll(newParams); // New parameters override existing ones
        }
        return merged.isEmpty() ? null : merged;
    }

    // Sprite operations
    @Transactional
    public List<String> createSprites(SpriteCreateRequest request) {
        List<String> spriteIds = new ArrayList<>();

        for (SpriteCreateRequest.SpriteData spriteData : request.getSprites()) {
            String spriteId = (spriteData.getDynamic() ? "D" : "S") + UUID.randomUUID().toString();

            if (!spriteData.getDynamic()) {
                // Save static sprite to database
                Sprite sprite = createSpriteEntity(spriteId, request.getWorld(), request.getLevel(), spriteData);
                spriteRepository.save(sprite);
            } else {
                // TODO: Save dynamic sprite to Redis
                log.warn("Dynamic sprites not yet implemented");
            }

            spriteIds.add(spriteId);
        }

        publishSpriteEvent("sprites.created", Map.of("world", request.getWorld(), "count", spriteIds.size()));
        return spriteIds;
    }

    public Optional<SpriteDto> getSprite(String id) {
        if (id.startsWith("S")) {
            return spriteRepository.findById(id)
                    .map(this::convertSpriteToDto);
        } else if (id.startsWith("D")) {
            // TODO: Get dynamic sprite from Redis
            log.warn("Dynamic sprites not yet implemented");
            return Optional.empty();
        }
        return Optional.empty();
    }

    public List<SpriteDto> getSpritesInCluster(String world, Integer level, Integer clusterX, Integer clusterY) {
        return spriteRepository.findByCluster(world, level, clusterX, clusterY)
                .stream()
                .map(this::convertSpriteToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<SpriteDto> updateSprite(String id, SpriteDto spriteDto) {
        if (id.startsWith("S")) {
            return spriteRepository.findById(id)
                    .map(sprite -> {
                        updateSpriteFromDto(sprite, spriteDto);
                        Sprite saved = spriteRepository.save(sprite);
                        publishSpriteEvent("sprite.updated", Map.of("id", id));
                        return convertSpriteToDto(saved);
                    });
        } else if (id.startsWith("D")) {
            // TODO: Update dynamic sprite in Redis
            log.warn("Dynamic sprites not yet implemented");
        }
        return Optional.empty();
    }

    @Transactional
    public boolean deleteSprite(String id) {
        if (id.startsWith("S")) {
            if (spriteRepository.existsById(id)) {
                spriteRepository.deleteById(id);
                publishSpriteEvent("sprite.deleted", Map.of("id", id));
                return true;
            }
        } else if (id.startsWith("D")) {
            // TODO: Delete dynamic sprite from Redis
            log.warn("Dynamic sprites not yet implemented");
        }
        return false;
    }

    @Transactional
    public Optional<SpriteDto> updateSpriteCoordinates(String id, SpriteCoordinateUpdateRequest request) {
        if (id.startsWith("S")) {
            return spriteRepository.findById(id)
                    .map(sprite -> {
                        try {
                            SpriteDto spriteData = objectMapper.readValue(sprite.getData(), SpriteDto.class);
                            spriteData.setX(request.getX());
                            spriteData.setY(request.getY());
                            if (request.getZ() != null) {
                                spriteData.setZ(request.getZ());
                            }

                            // Recalculate cluster positions
                            int newClusterX = request.getX() / CLUSTER_SIZE;
                            int newClusterY = request.getY() / CLUSTER_SIZE;

                            sprite.setClusterX0(newClusterX);
                            sprite.setClusterY0(newClusterY);
                            sprite.setData(objectMapper.writeValueAsString(spriteData));
                            sprite.setCompressed(null);

                            Sprite saved = spriteRepository.save(sprite);
                            publishSpriteEvent("sprite.coordinates.updated", Map.of("id", id, "x", request.getX(), "y", request.getY()));
                            return convertSpriteToDto(saved);
                        } catch (Exception e) {
                            log.error("Error updating sprite coordinates", e);
                            throw new RuntimeException("Failed to update sprite coordinates", e);
                        }
                    });
        } else if (id.startsWith("D")) {
            // TODO: Update dynamic sprite coordinates in Redis
            log.warn("Dynamic sprites coordinate update not yet implemented");
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<SpriteDto> enableSprite(String id) {
        if (id.startsWith("S")) {
            return spriteRepository.findById(id)
                    .map(sprite -> {
                        sprite.setEnabled(true);
                        Sprite saved = spriteRepository.save(sprite);
                        publishSpriteEvent("sprite.enabled", Map.of("id", id));
                        return convertSpriteToDto(saved);
                    });
        } else if (id.startsWith("D")) {
            // TODO: Enable dynamic sprite in Redis
            log.warn("Dynamic sprites enable not yet implemented");
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<SpriteDto> disableSprite(String id) {
        if (id.startsWith("S")) {
            return spriteRepository.findById(id)
                    .map(sprite -> {
                        sprite.setEnabled(false);
                        Sprite saved = spriteRepository.save(sprite);
                        publishSpriteEvent("sprite.disabled", Map.of("id", id));
                        return convertSpriteToDto(saved);
                    });
        } else if (id.startsWith("D")) {
            // TODO: Disable dynamic sprite in Redis
            log.warn("Dynamic sprites disable not yet implemented");
        }
        return Optional.empty();
    }

    // Asset operations
    @Transactional
    public AssetDto createAsset(AssetDto assetDto) {
        Asset asset = Asset.builder()
                .world(assetDto.getWorld())
                .name(assetDto.getName())
                .type(assetDto.getType())
                .data(assetDto.getData())
                .properties(convertPropertiesToJson(assetDto.getProperties()))
                .build();

        Asset saved = assetRepository.save(asset);
        publishAssetEvent("asset.created", saved);
        return convertAssetToDto(saved);
    }

    public Optional<AssetDto> getAsset(String world, String name) {
        return assetRepository.findByWorldAndName(world, name)
                .map(this::convertAssetToDto);
    }

    public Page<AssetDto> getAssets(String world, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return assetRepository.findByWorld(world, pageable)
                .map(this::convertAssetToDto);
    }

    @Transactional
    public Optional<AssetDto> updateAsset(String world, String name, AssetDto assetDto) {
        return assetRepository.findByWorldAndName(world, name)
                .map(asset -> {
                    asset.setType(assetDto.getType());
                    asset.setData(assetDto.getData());
                    asset.setProperties(convertPropertiesToJson(assetDto.getProperties()));
                    asset.setCompressed(null); // Mark for recompression

                    Asset saved = assetRepository.save(asset);
                    publishAssetEvent("asset.updated", saved);
                    return convertAssetToDto(saved);
                });
    }

    @Transactional
    public boolean deleteAsset(String world, String name) {
        Optional<Asset> asset = assetRepository.findByWorldAndName(world, name);
        if (asset.isPresent()) {
            assetRepository.deleteByWorldAndName(world, name);
            publishAssetEvent("asset.deleted", Map.of("world", world, "name", name));
            return true;
        }
        return false;
    }

    public List<AssetDto> getAssetsBatch(AssetBatchRequest request) {
        return request.getAssets().stream()
                .map(assetName -> getAsset(request.getWorld(), assetName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public void compressAssets(String world) {
        // TODO: Implement asset compression logic
        List<Asset> assets = assetRepository.findByWorld(world, PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        for (Asset asset : assets) {
            if (asset.getCompressed() == null && asset.getData() != null) {
                try {
                    // Simple compression placeholder - in real implementation, use proper compression
                    asset.setCompressed(asset.getData()); // Placeholder
                    asset.setCompressedAt(java.time.LocalDateTime.now());
                    assetRepository.save(asset);
                } catch (Exception e) {
                    log.error("Error compressing asset: " + asset.getName(), e);
                }
            }
        }

        publishAssetEvent("assets.compressed", Map.of("world", world, "count", assets.size()));
    }

    // Group operations
    @Transactional
    public GroupDto createGroup(String world, GroupDto groupDto) {
        TerrainGroup group = TerrainGroup.builder()
                .world(world)
                .name(groupDto.getName())
                .type(groupDto.getType())
                .data(convertPropertiesToJson(groupDto.getProperties()))
                .build();

        TerrainGroup saved = terrainGroupRepository.save(group);
        publishGroupEvent("group.created", saved);
        return convertGroupToDto(saved);
    }

    public Optional<GroupDto> getGroup(String world, Long id) {
        return terrainGroupRepository.findByWorldAndId(world, id)
                .map(this::convertGroupToDto);
    }

    public List<GroupDto> getGroups(String world) {
        return terrainGroupRepository.findByWorld(world)
                .stream()
                .map(this::convertGroupToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<GroupDto> updateGroup(String world, Long id, GroupDto groupDto) {
        return terrainGroupRepository.findByWorldAndId(world, id)
                .map(group -> {
                    group.setName(groupDto.getName());
                    group.setType(groupDto.getType());
                    group.setData(convertPropertiesToJson(groupDto.getProperties()));

                    TerrainGroup saved = terrainGroupRepository.save(group);
                    publishGroupEvent("group.updated", saved);
                    return convertGroupToDto(saved);
                });
    }

    @Transactional
    public boolean deleteGroup(String world, Long id) {
        Optional<TerrainGroup> group = terrainGroupRepository.findByWorldAndId(world, id);
        if (group.isPresent()) {
            terrainGroupRepository.delete(group.get());
            publishGroupEvent("group.deleted", Map.of("world", world, "id", id));
            return true;
        }
        return false;
    }

    private MaterialDto convertMaterialToDto(Material material) {
        return MaterialDto.builder()
                .id(material.getId())
                .name(material.getName())
                .blocking(material.getBlocking())
                .friction(material.getFriction())
                .color(material.getColor())
                .texture(material.getTexture())
                .soundWalk(material.getSoundWalk())
                .properties(convertJsonToProperties(material.getProperties()))
                .build();
    }

    private ClusterDto convertMapClusterToDto(MapCluster mapCluster) {
        try {
            List<FieldDto> fields = objectMapper.readValue(mapCluster.getData(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, FieldDto.class));

            return ClusterDto.builder()
                    .level(mapCluster.getLevel())
                    .x(mapCluster.getClusterX())
                    .y(mapCluster.getClusterY())
                    .fields(fields)
                    .build();
        } catch (Exception e) {
            log.error("Error converting map cluster to DTO", e);
            throw new RuntimeException("Failed to convert map cluster data", e);
        }
    }

    private SpriteDto convertSpriteToDto(Sprite sprite) {
        try {
            SpriteDto spriteData = objectMapper.readValue(sprite.getData(), SpriteDto.class);
            spriteData.setId(sprite.getId());
            spriteData.setEnabled(sprite.getEnabled());
            return spriteData;
        } catch (Exception e) {
            log.error("Error converting sprite to DTO", e);
            throw new RuntimeException("Failed to convert sprite data", e);
        }
    }

    private AssetDto convertAssetToDto(Asset asset) {
        return AssetDto.builder()
                .world(asset.getWorld())
                .name(asset.getName())
                .type(asset.getType())
                .data(asset.getData())
                .properties(convertJsonToProperties(asset.getProperties()))
                .build();
    }

    private GroupDto convertGroupToDto(TerrainGroup group) {
        return GroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .type(group.getType())
                .properties(convertJsonToProperties(group.getData()))
                .build();
    }

    private Sprite createSpriteEntity(String id, String world, Integer level, SpriteCreateRequest.SpriteData spriteData) {
        try {
            // Calculate cluster positions
            int clusterX = spriteData.getX() / CLUSTER_SIZE;
            int clusterY = spriteData.getY() / CLUSTER_SIZE;

            String data = objectMapper.writeValueAsString(spriteData);

            return Sprite.builder()
                    .id(id)
                    .world(world)
                    .level(level)
                    .enabled(true)
                    .clusterX0(clusterX)
                    .clusterY0(clusterY)
                    .data(data)
                    .build();
        } catch (Exception e) {
            log.error("Error creating sprite entity", e);
            throw new RuntimeException("Failed to create sprite entity", e);
        }
    }

    private void updateSpriteFromDto(Sprite sprite, SpriteDto spriteDto) {
        try {
            sprite.setData(objectMapper.writeValueAsString(spriteDto));
            sprite.setEnabled(spriteDto.getEnabled());
            sprite.setCompressed(null); // Mark for recompression
        } catch (Exception e) {
            log.error("Error updating sprite from DTO", e);
            throw new RuntimeException("Failed to update sprite", e);
        }
    }

    private String convertPropertiesToJson(Map<String, String> properties) {
        if (properties == null) return null;
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (Exception e) {
            log.error("Error converting properties to JSON", e);
            return null;
        }
    }

    private Map<String, String> convertJsonToProperties(String json) {
        if (json == null) return new HashMap<>();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
        } catch (Exception e) {
            log.error("Error converting JSON to properties", e);
            return new HashMap<>();
        }
    }

    // Kafka event publishing
    private void publishMaterialEvent(String event, Object data) {
        try {
            kafkaTemplate.send("terrain.material.events", event, data);
        } catch (Exception e) {
            log.error("Failed to publish material event: " + event, e);
        }
    }

    private void publishMapEvent(String event, Object data) {
        try {
            kafkaTemplate.send("terrain.map.events", event, data);
        } catch (Exception e) {
            log.error("Failed to publish map event: " + event, e);
        }
    }

    private void publishSpriteEvent(String event, Object data) {
        try {
            kafkaTemplate.send("terrain.sprite.events", event, data);
        } catch (Exception e) {
            log.error("Failed to publish sprite event: " + event, e);
        }
    }

    private void publishAssetEvent(String event, Object data) {
        try {
            kafkaTemplate.send("terrain.asset.events", event, data);
        } catch (Exception e) {
            log.error("Failed to publish asset event: " + event, e);
        }
    }

    private void publishGroupEvent(String event, Object data) {
        try {
            kafkaTemplate.send("terrain.group.events", event, data);
        } catch (Exception e) {
            log.error("Failed to publish group event: " + event, e);
        }
    }
}
