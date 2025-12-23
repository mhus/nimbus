package de.mhus.nimbus.world.control.service.sync.impl;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.mhus.nimbus.generated.types.Backdrop;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.control.service.sync.ResourceSyncType;
import de.mhus.nimbus.world.shared.world.WBackdrop;
import de.mhus.nimbus.world.shared.world.WBackdropService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Import/export implementation for backdrops.
 * Exports backdrops as YAML files in backdrops/ folder.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackdropResourceSyncType implements ResourceSyncType {

    private final WBackdropService backdropService;

    @Qualifier("syncYamlMapper")
    private final YAMLMapper yamlMapper;

    @Override
    public String name() {
        return "backdrop";
    }

    @Override
    public ResourceSyncType.ExportResult export(Path dataPath, WorldId worldId, boolean force, boolean removeOvertaken) throws IOException {
        Path backdropsDir = dataPath.resolve("backdrops");
        Files.createDirectories(backdropsDir);

        // Get backdrops directly for this worldId (no lookup)
        List<WBackdrop> backdrops = backdropService.findByWorldId(worldId);
        Set<String> dbBackdropIds = new HashSet<>();
        int exported = 0;

        for (WBackdrop backdrop : backdrops) {
            if (!backdrop.isEnabled()) {
                continue; // Skip disabled backdrops
            }

            dbBackdropIds.add(backdrop.getBackdropId());
            Path targetFile = backdropsDir.resolve(backdrop.getBackdropId() + ".yaml");

            // Check if export needed
            if (!force && Files.exists(targetFile)) {
                Instant fileTime = Files.getLastModifiedTime(targetFile).toInstant();
                if (backdrop.getUpdatedAt() != null && backdrop.getUpdatedAt().isBefore(fileTime)) {
                    log.debug("Skipping backdrop {} (not modified)", backdrop.getBackdropId());
                    continue;
                }
            }

            // Export complete entity as DTO
            BackdropExportDTO dto = BackdropExportDTO.builder()
                    .backdropId(backdrop.getBackdropId())
                    .updatedAt(backdrop.getUpdatedAt())
                    .enabled(backdrop.isEnabled())
                    .publicData(backdrop.getPublicData())
                    .build();

            // Write YAML
            yamlMapper.writeValue(targetFile.toFile(), dto);
            log.debug("Exported backdrop: {}", backdrop.getBackdropId());
            exported++;
        }

        // Remove files not in DB if requested
        int deleted = 0;
        if (removeOvertaken && Files.exists(backdropsDir)) {
            try (Stream<Path> files = Files.list(backdropsDir)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".yaml")).toList()) {
                    String filename = file.getFileName().toString();
                    String backdropId = filename.substring(0, filename.length() - 5); // Remove .yaml

                    if (!dbBackdropIds.contains(backdropId)) {
                        Files.delete(file);
                        log.info("Deleted file not in database: {}", file);
                        deleted++;
                    }
                }
            }
        }

        return ResourceSyncType.ExportResult.of(exported, deleted);
    }

    @Override
    public ResourceSyncType.ImportResult importData(Path dataPath, WorldId worldId, boolean force, boolean removeOvertaken) throws IOException {
        Path backdropsDir = dataPath.resolve("backdrops");
        if (!Files.exists(backdropsDir)) {
            log.info("No backdrops directory found");
            return ResourceSyncType.ImportResult.of(0, 0);
        }

        // Collect filesystem backdrops
        Set<String> filesystemBackdrops = new HashSet<>();
        int imported = 0;

        try (Stream<Path> files = Files.list(backdropsDir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".yaml")).toList()) {
                try {
                    BackdropExportDTO dto = yamlMapper.readValue(file.toFile(), BackdropExportDTO.class);
                    filesystemBackdrops.add(dto.getBackdropId());

                    // Check if exists (directly for this worldId, no lookup)
                    var existing = backdropService.findByBackdropId(worldId, dto.getBackdropId());

                    if (existing.isPresent()) {
                        WBackdrop backdrop = existing.get();
                        // Only update if file is newer (unless force=true)
                        if (!force && dto.getUpdatedAt() != null && backdrop.getUpdatedAt() != null &&
                                dto.getUpdatedAt().isBefore(backdrop.getUpdatedAt())) {
                            log.debug("Skipping backdrop {} (DB is newer)", dto.getBackdropId());
                            continue;
                        }
                    }

                    // Save using service (creates or updates)
                    backdropService.save(worldId, dto.getBackdropId(), dto.getPublicData());
                    log.debug("Imported backdrop: {}", dto.getBackdropId());
                    imported++;

                } catch (Exception e) {
                    log.warn("Failed to import backdrop from file: " + file, e);
                }
            }
        }

        // Remove overtaken if requested
        int deleted = 0;
        if (removeOvertaken) {
            List<WBackdrop> dbBackdrops = backdropService.findByWorldId(worldId);

            for (WBackdrop backdrop : dbBackdrops) {
                if (!filesystemBackdrops.contains(backdrop.getBackdropId())) {
                    backdropService.delete(worldId, backdrop.getBackdropId());
                    log.info("Deleted backdrop not in filesystem: {}", backdrop.getBackdropId());
                    deleted++;
                }
            }
        }

        return ResourceSyncType.ImportResult.of(imported, deleted);
    }

    /**
     * DTO for backdrop export/import.
     */
    @Data
    @Builder
    public static class BackdropExportDTO {
        private String backdropId;
        private Instant updatedAt;
        private boolean enabled;
        private Backdrop publicData;
    }
}
