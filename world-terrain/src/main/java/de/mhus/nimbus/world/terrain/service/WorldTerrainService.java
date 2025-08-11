package de.mhus.nimbus.world.terrain.service;

import de.mhus.nimbus.shared.dto.world.*;
import de.mhus.nimbus.world.terrain.entity.*;
import de.mhus.nimbus.world.terrain.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
}
