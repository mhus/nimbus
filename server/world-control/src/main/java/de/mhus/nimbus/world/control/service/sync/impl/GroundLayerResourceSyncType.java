package de.mhus.nimbus.world.control.service.sync.impl;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.mhus.nimbus.generated.types.HeightData;
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
 * Import/export implementation for ground layers.
 * Exports ground layers as folders with _info.yaml and chunked terrain data.
 * Chunks are organized in subfolders (cx/100)_(cz/100) to avoid filesystem limits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroundLayerResourceSyncType implements ResourceSyncType {

    private final WLayerService layerService;
    private final WLayerRepository layerRepository;
    private final WLayerTerrainRepository terrainRepository;

    @Qualifier("syncYamlMapper")
    private final YAMLMapper yamlMapper;

    @Override
    public String name() {
        return "ground";
    }

    @Override
    public ResourceSyncType.ExportResult export(Path dataPath, WorldId worldId, boolean force, boolean removeOvertaken) throws IOException {
        Path groundDir = dataPath.resolve("ground");
        Files.createDirectories(groundDir);

        // Get layers directly for this worldId (no lookup)
        List<WLayer> layers = layerService.findLayersByWorld(worldId.getId()).stream()
                .filter(l -> l.getLayerType() == LayerType.GROUND && l.isEnabled())
                .toList();

        Set<String> dbLayerNames = new HashSet<>();
        int exported = 0;

        for (WLayer layer : layers) {
            dbLayerNames.add(layer.getName());
            Path layerDir = groundDir.resolve(layer.getName());
            Files.createDirectories(layerDir);

            Path infoFile = layerDir.resolve("_info.yaml");

            // Check if export needed
            if (!force && Files.exists(infoFile)) {
                Instant fileTime = Files.getLastModifiedTime(infoFile).toInstant();
                if (layer.getUpdatedAt() != null && layer.getUpdatedAt().isBefore(fileTime)) {
                    log.debug("Skipping ground layer {} (not modified)", layer.getName());
                    continue;
                }
            }

            // Export layer info
            GroundLayerInfoDTO infoDTO = GroundLayerInfoDTO.builder()
                    .layerType(LayerType.GROUND)
                    .name(layer.getName())
                    .title(layer.getTitle())
                    .order(layer.getOrder())
                    .enabled(layer.isEnabled())
                    .allChunks(layer.isAllChunks())
                    .baseGround(layer.isBaseGround())
                    .groups(layer.getGroups())
                    .updatedAt(layer.getUpdatedAt())
                    .build();

            yamlMapper.writeValue(infoFile.toFile(), infoDTO);
            exported++;

            // Export terrain chunks
            List<WLayerTerrain> terrains = terrainRepository.findByLayerDataId(layer.getLayerDataId());
            for (WLayerTerrain terrain : terrains) {
                try {
                    // Parse chunk coordinates from chunkKey (format: "cx:cz")
                    String[] parts = terrain.getChunkKey().split(":");
                    if (parts.length != 2) {
                        log.warn("Invalid chunkKey format: {}", terrain.getChunkKey());
                        continue;
                    }
                    int cx = Integer.parseInt(parts[0]);
                    int cz = Integer.parseInt(parts[1]);

                    // Calculate subfolder (cx/100)_(cz/100)
                    int folderX = cx / 100;
                    int folderZ = cz / 100;
                    String subfolderName = folderX + "_" + folderZ;
                    Path subfolder = layerDir.resolve(subfolderName);
                    Files.createDirectories(subfolder);

                    // Export chunk
                    Path chunkFile = subfolder.resolve("chunk_" + cx + "_" + cz + ".yaml");

                    // Load chunk data
                    Optional<LayerChunkData> chunkDataOpt = layerService.loadTerrainChunk(
                            layer.getLayerDataId(),
                            terrain.getChunkKey()
                    );

                    if (chunkDataOpt.isEmpty()) {
                        log.warn("Could not load terrain chunk data: {}", terrain.getChunkKey());
                        continue;
                    }

                    LayerChunkData chunkData = chunkDataOpt.get();

                    // Create export DTO
                    ChunkExportDTO chunkDTO = ChunkExportDTO.builder()
                            .cx(cx)
                            .cz(cz)
                            .updatedAt(terrain.getUpdatedAt())
                            .blocks(chunkData.getBlocks())
                            .heightData(chunkData.getHeightData())
                            .build();

                    yamlMapper.writeValue(chunkFile.toFile(), chunkDTO);
                    exported++;

                } catch (Exception e) {
                    log.warn("Failed to export terrain chunk: " + terrain.getChunkKey(), e);
                }
            }

            log.debug("Exported ground layer: {} with {} chunks", layer.getName(), terrains.size());
        }

        // Remove layer folders not in DB if requested
        int deleted = 0;
        if (removeOvertaken && Files.exists(groundDir)) {
            try (Stream<Path> layerDirs = Files.list(groundDir)) {
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
        Path groundDir = dataPath.resolve("ground");
        if (!Files.exists(groundDir)) {
            log.info("No ground directory found");
            return ResourceSyncType.ImportResult.of(0, 0);
        }

        // Collect filesystem layers and chunks
        Map<String, Set<String>> filesystemChunks = new HashMap<>(); // layerName -> chunkKeys
        int imported = 0;

        try (Stream<Path> layerDirs = Files.list(groundDir)) {
            for (Path layerDir : layerDirs.filter(Files::isDirectory).toList()) {
                String layerName = layerDir.getFileName().toString();
                Set<String> chunkKeys = new HashSet<>();
                filesystemChunks.put(layerName, chunkKeys);

                Path infoFile = layerDir.resolve("_info.yaml");
                if (!Files.exists(infoFile)) {
                    log.warn("No _info.yaml found in: {}", layerDir);
                    continue;
                }

                try {
                    GroundLayerInfoDTO infoDTO = yamlMapper.readValue(infoFile.toFile(), GroundLayerInfoDTO.class);

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
                        layer.setBaseGround(infoDTO.isBaseGround());
                        layer.setGroups(infoDTO.getGroups());
                        layer.setUpdatedAt(infoDTO.getUpdatedAt());
                        layer.touchUpdate();
                        layer = layerRepository.save(layer);
                    } else {
                        // Create new layer
                        layer = layerService.createLayer(
                                worldId.getId(),
                                infoDTO.getName(),
                                LayerType.GROUND,
                                infoDTO.getOrder(),
                                infoDTO.isAllChunks(),
                                List.of(),
                                infoDTO.isBaseGround()
                        );
                        layer.setTitle(infoDTO.getTitle());
                        layer.setGroups(infoDTO.getGroups());
                        layer.setUpdatedAt(infoDTO.getUpdatedAt());
                        layer = layerRepository.save(layer);
                    }

                    imported++;

                    // Import chunks (walk recursively to handle subfolders)
                    try (Stream<Path> paths = Files.walk(layerDir)) {
                        List<Path> chunkFiles = paths
                                .filter(p -> p.toString().endsWith(".yaml") && !p.getFileName().toString().equals("_info.yaml"))
                                .toList();

                        for (Path chunkFile : chunkFiles) {
                            try {
                                ChunkExportDTO chunkDTO = yamlMapper.readValue(chunkFile.toFile(), ChunkExportDTO.class);

                                // Create chunk key
                                String chunkKey = chunkDTO.getCx() + ":" + chunkDTO.getCz();
                                chunkKeys.add(chunkKey);

                                // Create LayerChunkData
                                LayerChunkData chunkData = LayerChunkData.builder()
                                        .cx(chunkDTO.getCx())
                                        .cz(chunkDTO.getCz())
                                        .blocks(chunkDTO.getBlocks())
                                        .heightData(chunkDTO.getHeightData())
                                        .build();

                                // Save chunk
                                layerService.saveTerrainChunk(
                                        worldId.getId(),
                                        layer.getLayerDataId(),
                                        chunkKey,
                                        chunkData
                                );

                                imported++;

                            } catch (Exception e) {
                                log.warn("Failed to import chunk from file: " + chunkFile, e);
                            }
                        }
                    }

                    log.debug("Imported ground layer: {}", infoDTO.getName());

                } catch (Exception e) {
                    log.warn("Failed to import ground layer from: " + layerDir, e);
                }
            }
        }

        // Remove overtaken if requested
        int deleted = 0;
        if (removeOvertaken) {
            List<WLayer> dbLayers = layerService.findLayersByWorld(worldId.getId()).stream()
                    .filter(l -> l.getLayerType() == LayerType.GROUND)
                    .toList();

            for (WLayer layer : dbLayers) {
                if (!filesystemChunks.containsKey(layer.getName())) {
                    // Layer not in filesystem, delete
                    layerService.deleteLayer(worldId.getId(), layer.getName());
                    log.info("Deleted ground layer not in filesystem: {}", layer.getName());
                    deleted++;
                } else {
                    // Check chunks within layer
                    Set<String> fsChunks = filesystemChunks.get(layer.getName());
                    List<WLayerTerrain> dbChunks = terrainRepository.findByLayerDataId(layer.getLayerDataId());

                    for (WLayerTerrain chunk : dbChunks) {
                        if (!fsChunks.contains(chunk.getChunkKey())) {
                            terrainRepository.delete(chunk);
                            log.info("Deleted chunk not in filesystem: {}/{}", layer.getName(), chunk.getChunkKey());
                            deleted++;
                        }
                    }
                }
            }
        }

        return ResourceSyncType.ImportResult.of(imported, deleted);
    }

    /**
     * DTO for ground layer info export/import.
     */
    @Data
    @Builder
    public static class GroundLayerInfoDTO {
        private LayerType layerType;
        private String name;
        private String title;
        private int order;
        private boolean enabled;
        private boolean allChunks;
        private boolean baseGround;
        private Map<String, Integer> groups;
        private Instant updatedAt;
    }

    /**
     * DTO for chunk export/import.
     */
    @Data
    @Builder
    public static class ChunkExportDTO {
        private int cx;
        private int cz;
        private Instant updatedAt;
        private List<LayerBlock> blocks;
        private List<HeightData> heightData;
    }
}
