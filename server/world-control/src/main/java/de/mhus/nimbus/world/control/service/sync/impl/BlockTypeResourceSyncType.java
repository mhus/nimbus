package de.mhus.nimbus.world.control.service.sync.impl;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.mhus.nimbus.generated.types.BlockType;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.control.service.sync.ResourceSyncType;
import de.mhus.nimbus.world.shared.world.WBlockType;
import de.mhus.nimbus.world.shared.world.WBlockTypeService;
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
 * Import/export implementation for block types.
 * Exports block types as YAML files in blocktypes/ folder.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockTypeResourceSyncType implements ResourceSyncType {

    private final WBlockTypeService blockTypeService;

    @Qualifier("syncYamlMapper")
    private final YAMLMapper yamlMapper;

    @Override
    public String name() {
        return "blocktype";
    }

    @Override
    public ResourceSyncType.ExportResult export(Path dataPath, WorldId worldId, boolean force, boolean removeOvertaken) throws IOException {
        Path blocktypesDir = dataPath.resolve("blocktypes");
        Files.createDirectories(blocktypesDir);

        // Get block types directly for this worldId (no lookup)
        List<WBlockType> blockTypes = blockTypeService.findByWorldId(worldId);
        Set<String> dbBlockIds = new HashSet<>();
        int exported = 0;

        for (WBlockType blockType : blockTypes) {
            if (!blockType.isEnabled()) {
                continue; // Skip disabled block types
            }

            dbBlockIds.add(blockType.getBlockId());

            // Sanitize filename (replace : with _)
            String filename = blockType.getBlockId().replace(":", "_") + ".yaml";
            Path targetFile = blocktypesDir.resolve(filename);

            // Check if export needed
            if (!force && Files.exists(targetFile)) {
                Instant fileTime = Files.getLastModifiedTime(targetFile).toInstant();
                if (blockType.getUpdatedAt() != null && blockType.getUpdatedAt().isBefore(fileTime)) {
                    log.debug("Skipping blocktype {} (not modified)", blockType.getBlockId());
                    continue;
                }
            }

            // Create export DTO
            BlockTypeExportDTO dto = BlockTypeExportDTO.builder()
                    .blockId(blockType.getBlockId())
                    .updatedAt(blockType.getUpdatedAt())
                    .enabled(blockType.isEnabled())
                    .publicData(blockType.getPublicData())
                    .build();

            // Write YAML
            yamlMapper.writeValue(targetFile.toFile(), dto);
            log.debug("Exported blocktype: {}", blockType.getBlockId());
            exported++;
        }

        // Remove files not in DB if requested
        int deleted = 0;
        if (removeOvertaken && Files.exists(blocktypesDir)) {
            try (Stream<Path> files = Files.list(blocktypesDir)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".yaml")).toList()) {
                    try {
                        BlockTypeExportDTO dto = yamlMapper.readValue(file.toFile(), BlockTypeExportDTO.class);
                        if (!dbBlockIds.contains(dto.getBlockId())) {
                            Files.delete(file);
                            log.info("Deleted file not in database: {}", file);
                            deleted++;
                        }
                    } catch (IOException e) {
                        log.warn("Failed to check file for deletion: " + file, e);
                    }
                }
            }
        }

        return ResourceSyncType.ExportResult.of(exported, deleted);
    }

    @Override
    public ResourceSyncType.ImportResult importData(Path dataPath, WorldId worldId, boolean force, boolean removeOvertaken) throws IOException {
        Path blocktypesDir = dataPath.resolve("blocktypes");
        if (!Files.exists(blocktypesDir)) {
            log.info("No blocktypes directory found");
            return ResourceSyncType.ImportResult.of(0, 0);
        }

        // Collect filesystem block types
        Set<String> filesystemBlockTypes = new HashSet<>();
        int imported = 0;

        try (Stream<Path> files = Files.list(blocktypesDir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".yaml")).toList()) {
                try {
                    BlockTypeExportDTO dto = yamlMapper.readValue(file.toFile(), BlockTypeExportDTO.class);
                    filesystemBlockTypes.add(dto.getBlockId());

                    // Check if exists (directly for this worldId, no lookup)
                    var existing = blockTypeService.findByBlockId(worldId, dto.getBlockId());

                    if (existing.isPresent()) {
                        WBlockType blockType = existing.get();
                        // Only update if file is newer (unless force=true)
                        if (!force && dto.getUpdatedAt() != null && blockType.getUpdatedAt() != null &&
                                dto.getUpdatedAt().isBefore(blockType.getUpdatedAt())) {
                            log.debug("Skipping blocktype {} (DB is newer)", dto.getBlockId());
                            continue;
                        }
                    }

                    // Save using service (creates or updates)
                    blockTypeService.save(worldId, dto.getBlockId(), dto.getPublicData());
                    log.debug("Imported blocktype: {}", dto.getBlockId());
                    imported++;

                } catch (Exception e) {
                    log.warn("Failed to import blocktype from file: " + file, e);
                }
            }
        }

        // Remove overtaken if requested
        int deleted = 0;
        if (removeOvertaken) {
            List<WBlockType> dbBlockTypes = blockTypeService.findByWorldId(worldId);

            for (WBlockType blockType : dbBlockTypes) {
                if (!filesystemBlockTypes.contains(blockType.getBlockId())) {
                    blockTypeService.delete(worldId, blockType.getBlockId());
                    log.info("Deleted blocktype not in filesystem: {}", blockType.getBlockId());
                    deleted++;
                }
            }
        }

        return ResourceSyncType.ImportResult.of(imported, deleted);
    }

    /**
     * DTO for blocktype export/import.
     */
    @Data
    @Builder
    public static class BlockTypeExportDTO {
        private String blockId;
        private Instant updatedAt;
        private boolean enabled;
        private BlockType publicData;
    }
}
