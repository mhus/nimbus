package de.mhus.nimbus.world.control.service.sync.impl;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.control.service.sync.ResourceSyncType;
import de.mhus.nimbus.world.shared.world.AssetMetadata;
import de.mhus.nimbus.world.shared.world.SAsset;
import de.mhus.nimbus.world.shared.world.SAssetService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Import/export implementation for assets.
 * Exports assets as binary files + .info.yaml metadata in assets/ folder.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssetResourceSyncType implements ResourceSyncType {

    private final SAssetService assetService;

    @Qualifier("syncYamlMapper")
    private final YAMLMapper yamlMapper;

    @Override
    public String name() {
        return "asset";
    }

    @Override
    public ResourceSyncType.ExportResult export(Path dataPath, WorldId worldId, boolean force, boolean removeOvertaken) throws IOException {
        Path assetsDir = dataPath.resolve("assets");
        Files.createDirectories(assetsDir);

        List<SAsset> assets = assetService.findByWorldId(worldId);
        Set<String> dbAssetPaths = new HashSet<>();
        int exported = 0;

        for (SAsset asset : assets) {
            if (!asset.isEnabled()) {
                continue; // Skip disabled assets
            }

            dbAssetPaths.add(asset.getPath());
            Path targetBinary = assetsDir.resolve(asset.getPath());
            Path targetInfo = assetsDir.resolve(asset.getPath() + ".info.yaml");

            // Check if export needed
            if (!force && Files.exists(targetInfo)) {
                Instant fileTime = Files.getLastModifiedTime(targetInfo).toInstant();
                if (asset.getCreatedAt() != null && asset.getCreatedAt().isBefore(fileTime)) {
                    log.debug("Skipping asset {} (not modified)", asset.getPath());
                    continue;
                }
            }

            // Create parent directories
            Files.createDirectories(targetBinary.getParent());

            // Export binary data
            try (InputStream stream = assetService.loadContent(asset)) {
                Files.copy(stream, targetBinary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // Export metadata
            AssetInfoExportDTO dto = AssetInfoExportDTO.builder()
                    .path(asset.getPath())
                    .createdAt(asset.getCreatedAt())
                    .createdBy(asset.getCreatedBy())
                    .metadata(asset.getPublicData())
                    .build();

            yamlMapper.writeValue(targetInfo.toFile(), dto);
            log.debug("Exported asset: {}", asset.getPath());
            exported++;
        }

        // Remove files not in DB if requested
        int deleted = 0;
        if (removeOvertaken && Files.exists(assetsDir)) {
            try (Stream<Path> paths = Files.walk(assetsDir)) {
                List<Path> infoFiles = paths.filter(p -> p.toString().endsWith(".info.yaml")).toList();

                for (Path infoFile : infoFiles) {
                    try {
                        AssetInfoExportDTO dto = yamlMapper.readValue(infoFile.toFile(), AssetInfoExportDTO.class);
                        if (!dbAssetPaths.contains(dto.getPath())) {
                            // Delete both info and binary file
                            Files.delete(infoFile);
                            String relativePath = assetsDir.relativize(infoFile).toString();
                            relativePath = relativePath.substring(0, relativePath.length() - ".info.yaml".length());
                            Path binaryFile = assetsDir.resolve(relativePath);
                            if (Files.exists(binaryFile)) {
                                Files.delete(binaryFile);
                            }
                            log.info("Deleted files not in database: {}", dto.getPath());
                            deleted++;
                        }
                    } catch (IOException e) {
                        log.warn("Failed to check file for deletion: " + infoFile, e);
                    }
                }
            }
        }

        return ResourceSyncType.ExportResult.of(exported, deleted);
    }

    @Override
    public ResourceSyncType.ImportResult importData(Path dataPath, WorldId worldId, boolean force, boolean removeOvertaken) throws IOException {
        Path assetsDir = dataPath.resolve("assets");
        if (!Files.exists(assetsDir)) {
            log.info("No assets directory found");
            return ResourceSyncType.ImportResult.of(0, 0);
        }

        // Collect filesystem assets
        Set<String> filesystemAssets = new HashSet<>();
        int imported = 0;

        // Find all .info.yaml files recursively
        try (Stream<Path> paths = Files.walk(assetsDir)) {
            List<Path> infoFiles = paths
                    .filter(p -> p.toString().endsWith(".info.yaml"))
                    .toList();

            for (Path infoFile : infoFiles) {
                try {
                    AssetInfoExportDTO dto = yamlMapper.readValue(infoFile.toFile(), AssetInfoExportDTO.class);
                    filesystemAssets.add(dto.getPath());

                    // Get binary file path (remove .info.yaml)
                    String relativePath = assetsDir.relativize(infoFile).toString();
                    relativePath = relativePath.substring(0, relativePath.length() - ".info.yaml".length());
                    Path binaryFile = assetsDir.resolve(relativePath);

                    if (!Files.exists(binaryFile)) {
                        log.warn("Binary file not found for asset: {}", relativePath);
                        continue;
                    }

                    // Check if exists in DB
                    var existing = assetService.findByPath(worldId, dto.getPath());

                    if (existing.isPresent()) {
                        SAsset asset = existing.get();
                        // Only update if file is newer (unless force=true)
                        if (!force) {
                            Instant fileTime = Files.getLastModifiedTime(infoFile).toInstant();
                            if (asset.getCreatedAt() != null && fileTime.isBefore(asset.getCreatedAt())) {
                                log.debug("Skipping asset {} (DB is newer)", dto.getPath());
                                continue;
                            }
                        }

                        // Update content
                        try (InputStream stream = Files.newInputStream(binaryFile)) {
                            assetService.updateContent(asset, stream);
                        }
                        // Update metadata separately
                        assetService.updateMetadata(asset, dto.getMetadata());
                    } else {
                        // Create new asset
                        try (InputStream stream = Files.newInputStream(binaryFile)) {
                            assetService.saveAsset(worldId, dto.getPath(), stream,
                                    dto.getCreatedBy(), dto.getMetadata());
                        }
                    }

                    log.debug("Imported asset: {}", dto.getPath());
                    imported++;

                } catch (Exception e) {
                    log.warn("Failed to import asset from file: " + infoFile, e);
                }
            }
        }

        // Remove overtaken if requested
        int deleted = 0;
        if (removeOvertaken) {
            List<SAsset> dbAssets = assetService.findByWorldId(worldId);

            for (SAsset asset : dbAssets) {
                if (!filesystemAssets.contains(asset.getPath())) {
                    assetService.delete(asset);
                    log.info("Deleted asset not in filesystem: {}", asset.getPath());
                    deleted++;
                }
            }
        }

        return ResourceSyncType.ImportResult.of(imported, deleted);
    }

    /**
     * DTO for asset metadata export/import.
     */
    @Data
    @Builder
    public static class AssetInfoExportDTO {
        private String path;
        private Instant createdAt;
        private String createdBy;
        private AssetMetadata metadata;
    }
}
