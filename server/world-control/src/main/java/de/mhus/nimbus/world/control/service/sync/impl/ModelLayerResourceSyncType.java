package de.mhus.nimbus.world.control.service.sync.impl;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.control.service.sync.ResourceSyncType;
import de.mhus.nimbus.world.shared.layer.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Import/export implementation for model layers.
 * Exports model layers as folders with _info.yaml and model YAML files.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelLayerResourceSyncType implements ResourceSyncType {

    private final WLayerService layerService;
    private final WLayerRepository layerRepository;
    private final WLayerModelRepository modelRepository;

    @Qualifier("syncYamlMapper")
    private final YAMLMapper yamlMapper;

    @Override
    public String name() {
        return "model";
    }

    @Override
    public ResourceSyncType.ExportResult export(Path dataPath, WorldId worldId, boolean force, boolean removeOvertaken) throws IOException {
        Path modelsDir = dataPath.resolve("models");
        Files.createDirectories(modelsDir);

        // Get layers directly for this worldId (no lookup)
        List<WLayer> layers = layerService.findLayersByWorld(worldId.getId()).stream()
                .filter(l -> l.getLayerType() == LayerType.MODEL && l.isEnabled())
                .toList();

        Set<String> dbLayerNames = new HashSet<>();
        int exported = 0;

        for (WLayer layer : layers) {
            dbLayerNames.add(layer.getName());
            Path layerDir = modelsDir.resolve(layer.getName());
            Files.createDirectories(layerDir);

            Path infoFile = layerDir.resolve("_info.yaml");

            // Check if export needed
            if (!force && Files.exists(infoFile)) {
                Instant fileTime = Files.getLastModifiedTime(infoFile).toInstant();
                if (layer.getUpdatedAt() != null && layer.getUpdatedAt().isBefore(fileTime)) {
                    log.debug("Skipping model layer {} (not modified)", layer.getName());
                    continue;
                }
            }

            // Export layer info
            ModelLayerInfoDTO infoDTO = ModelLayerInfoDTO.builder()
                    .layerType(LayerType.MODEL)
                    .name(layer.getName())
                    .title(layer.getTitle())
                    .order(layer.getOrder())
                    .enabled(layer.isEnabled())
                    .allChunks(layer.isAllChunks())
                    .affectedChunks(layer.getAffectedChunks())
                    .groups(layer.getGroups())
                    .updatedAt(layer.getUpdatedAt())
                    .build();

            yamlMapper.writeValue(infoFile.toFile(), infoDTO);
            exported++;

            // Export models
            List<WLayerModel> models = modelRepository.findByLayerDataIdOrderByOrder(layer.getLayerDataId());
            for (WLayerModel model : models) {
                Path modelFile = layerDir.resolve(model.getName() + ".yaml");

                ModelExportDTO modelDTO = ModelExportDTO.builder()
                        .name(model.getName())
                        .title(model.getTitle())
                        .mountX(model.getMountX())
                        .mountY(model.getMountY())
                        .mountZ(model.getMountZ())
                        .rotation(model.getRotation())
                        .order(model.getOrder())
                        .referenceModelId(model.getReferenceModelId())
                        .groups(model.getGroups())
                        .content(model.getContent())
                        .updatedAt(model.getUpdatedAt())
                        .build();

                yamlMapper.writeValue(modelFile.toFile(), modelDTO);
                exported++;
            }

            log.debug("Exported model layer: {} with {} models", layer.getName(), models.size());
        }

        // Remove layer folders not in DB if requested
        int deleted = 0;
        if (removeOvertaken && Files.exists(modelsDir)) {
            try (Stream<Path> layerDirs = Files.list(modelsDir)) {
                for (Path layerDir : layerDirs.filter(Files::isDirectory).toList()) {
                    String layerName = layerDir.getFileName().toString();
                    if (!dbLayerNames.contains(layerName)) {
                        // Delete entire layer folder recursively
                        deleteRecursively(layerDir);
                        log.info("Deleted layer folder not in database: {}", layerName);
                        deleted++;
                    }
                }
            }
        }

        return ResourceSyncType.ExportResult.of(exported, deleted);
    }

    /**
     * Delete directory recursively.
     */
    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                for (Path entry : entries.toList()) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.delete(path);
    }

    @Override
    public ResourceSyncType.ImportResult importData(Path dataPath, WorldId worldId, boolean force, boolean removeOvertaken) throws IOException {
        Path modelsDir = dataPath.resolve("models");
        if (!Files.exists(modelsDir)) {
            log.info("No models directory found");
            return ResourceSyncType.ImportResult.of(0, 0);
        }

        // Collect filesystem layers and models
        Map<String, Set<String>> filesystemModels = new HashMap<>(); // layerName -> modelNames
        int imported = 0;

        try (Stream<Path> layerDirs = Files.list(modelsDir)) {
            for (Path layerDir : layerDirs.filter(Files::isDirectory).toList()) {
                String layerName = layerDir.getFileName().toString();
                Set<String> modelNames = new HashSet<>();
                filesystemModels.put(layerName, modelNames);

                Path infoFile = layerDir.resolve("_info.yaml");
                if (!Files.exists(infoFile)) {
                    log.warn("No _info.yaml found in: {}", layerDir);
                    continue;
                }

                try {
                    ModelLayerInfoDTO infoDTO = yamlMapper.readValue(infoFile.toFile(), ModelLayerInfoDTO.class);

                    // Check if layer exists (directly for this worldId)
                    Optional<WLayer> existingLayer = layerService.findLayer(worldId.getId(), infoDTO.getName());

                    WLayer layer;
                    if (existingLayer.isPresent()) {
                        layer = existingLayer.get();
                        // Update layer
                        layer.setTitle(infoDTO.getTitle());
                        layer.setOrder(infoDTO.getOrder());
                        layer.setEnabled(infoDTO.isEnabled());
                        layer.setAllChunks(infoDTO.isAllChunks());
                        layer.setAffectedChunks(infoDTO.getAffectedChunks());
                        layer.setGroups(infoDTO.getGroups());
                        layer.setUpdatedAt(infoDTO.getUpdatedAt());
                        layer.touchUpdate();
                        layer = layerRepository.save(layer);
                    } else {
                        // Create new layer
                        layer = layerService.createLayer(
                                worldId.getId(),
                                infoDTO.getName(),
                                LayerType.MODEL,
                                infoDTO.getOrder(),
                                infoDTO.isAllChunks(),
                                infoDTO.getAffectedChunks(),
                                false
                        );
                        layer.setTitle(infoDTO.getTitle());
                        layer.setGroups(infoDTO.getGroups());
                        layer.setUpdatedAt(infoDTO.getUpdatedAt());
                        layer = layerRepository.save(layer);
                    }

                    imported++;

                    // Import models
                    try (Stream<Path> modelFiles = Files.list(layerDir)) {
                        for (Path modelFile : modelFiles.filter(f -> f.toString().endsWith(".yaml") && !f.getFileName().toString().equals("_info.yaml")).toList()) {
                            try {
                                ModelExportDTO modelDTO = yamlMapper.readValue(modelFile.toFile(), ModelExportDTO.class);
                                modelNames.add(modelDTO.getName());

                                // Check if model exists (use repository directly)
                                List<WLayerModel> existingModels = modelRepository.findByWorldIdAndName(
                                        worldId.getId(),
                                        modelDTO.getName()
                                );

                                // Make layer final for lambda
                                final String layerDataId = layer.getLayerDataId();
                                final String finalWorldId = worldId.getId();

                                WLayerModel model;
                                if (!existingModels.isEmpty()) {
                                    // Find model with matching layerDataId
                                    model = existingModels.stream()
                                            .filter(m -> m.getLayerDataId().equals(layerDataId))
                                            .findFirst()
                                            .orElseGet(() -> {
                                                // Model exists but for different layer, create new
                                                WLayerModel newModel = new WLayerModel();
                                                newModel.setWorldId(finalWorldId);
                                                newModel.setLayerDataId(layerDataId);
                                                newModel.setName(modelDTO.getName());
                                                newModel.touchCreate();
                                                return newModel;
                                            });
                                } else {
                                    model = new WLayerModel();
                                    model.setWorldId(worldId.getId());
                                    model.setLayerDataId(layerDataId);
                                    model.setName(modelDTO.getName());
                                    model.touchCreate();
                                }

                                // Update fields
                                model.setTitle(modelDTO.getTitle());
                                model.setMountX(modelDTO.getMountX());
                                model.setMountY(modelDTO.getMountY());
                                model.setMountZ(modelDTO.getMountZ());
                                model.setRotation(modelDTO.getRotation());
                                model.setOrder(modelDTO.getOrder());
                                model.setReferenceModelId(modelDTO.getReferenceModelId());
                                model.setGroups(modelDTO.getGroups());
                                model.setContent(modelDTO.getContent());
                                model.setUpdatedAt(modelDTO.getUpdatedAt());
                                model.touchUpdate();

                                modelRepository.save(model);
                                imported++;

                            } catch (Exception e) {
                                log.warn("Failed to import model from file: " + modelFile, e);
                            }
                        }
                    }

                    log.debug("Imported model layer: {}", infoDTO.getName());

                } catch (Exception e) {
                    log.warn("Failed to import model layer from: " + layerDir, e);
                }
            }
        }

        // Remove overtaken if requested
        int deleted = 0;
        if (removeOvertaken) {
            List<WLayer> dbLayers = layerService.findLayersByWorld(worldId.getId()).stream()
                    .filter(l -> l.getLayerType() == LayerType.MODEL)
                    .toList();

            for (WLayer layer : dbLayers) {
                if (!filesystemModels.containsKey(layer.getName())) {
                    // Layer not in filesystem, delete
                    layerService.deleteLayer(worldId.getId(), layer.getName());
                    log.info("Deleted model layer not in filesystem: {}", layer.getName());
                    deleted++;
                } else {
                    // Check models within layer
                    Set<String> fsModels = filesystemModels.get(layer.getName());
                    List<WLayerModel> dbModels = modelRepository.findByLayerDataIdOrderByOrder(layer.getLayerDataId());

                    for (WLayerModel model : dbModels) {
                        if (!fsModels.contains(model.getName())) {
                            modelRepository.delete(model);
                            log.info("Deleted model not in filesystem: {}/{}", layer.getName(), model.getName());
                            deleted++;
                        }
                    }
                }
            }
        }

        return ResourceSyncType.ImportResult.of(imported, deleted);
    }

    /**
     * DTO for model layer info export/import.
     */
    @Data
    @Builder
    public static class ModelLayerInfoDTO {
        private LayerType layerType;
        private String name;
        private String title;
        private int order;
        private boolean enabled;
        private boolean allChunks;
        private List<String> affectedChunks;
        private Map<String, Integer> groups;
        private Instant updatedAt;
    }

    /**
     * DTO for model export/import.
     */
    @Data
    @Builder
    public static class ModelExportDTO {
        private String name;
        private String title;
        private int mountX;
        private int mountY;
        private int mountZ;
        private int rotation;
        private int order;
        private String referenceModelId;
        private Map<String, Integer> groups;
        private List<LayerBlock> content;
        private Instant updatedAt;
    }
}
