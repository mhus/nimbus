package de.mhus.nimbus.world.control.service.sync.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.mhus.nimbus.shared.service.SchemaMigrationService;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.control.service.sync.ResourceSyncType;
import de.mhus.nimbus.world.shared.world.WBlockType;
import de.mhus.nimbus.world.shared.world.WBlockTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Import/export implementation for block types.
 * Exports MongoDB documents directly as YAML files (preserves all fields including _schema).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockTypeResourceSyncType implements ResourceSyncType {

    private static final String COLLECTION_NAME = "w_blocktypes";

    private final WBlockTypeService blockTypeService;
    private final MongoTemplate mongoTemplate;
    private final SchemaMigrationService migrationService;
    private final ObjectMapper objectMapper;

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

        // Get blocktypes directly from MongoDB as Documents
        Query query = new Query(Criteria.where("worldId").is(worldId.getId()));
        List<Document> documents = mongoTemplate.find(query, Document.class, COLLECTION_NAME);

        Set<String> dbBlockIds = new HashSet<>();
        int exported = 0;

        for (Document doc : documents) {
            try {
                String blockId = doc.getString("blockId");
                if (blockId == null) {
                    log.warn("BlockType without blockId, skipping");
                    continue;
                }

                dbBlockIds.add(blockId);

                // Sanitize filename (replace : with _)
                String filename = blockId.replace(":", "_") + ".yaml";
                Path targetFile = blocktypesDir.resolve(filename);

                // Write MongoDB Document directly as YAML
                yamlMapper.writeValue(targetFile.toFile(), doc);
                log.debug("Exported blocktype: {}", blockId);
                exported++;

            } catch (Exception e) {
                log.warn("Failed to export blocktype document", e);
            }
        }

        // Remove files not in DB if requested
        int deleted = 0;
        if (removeOvertaken && Files.exists(blocktypesDir)) {
            try (Stream<Path> files = Files.list(blocktypesDir)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".yaml")).toList()) {
                    try {
                        Document doc = yamlMapper.readValue(file.toFile(), Document.class);
                        String blockId = doc.getString("blockId");

                        if (!dbBlockIds.contains(blockId)) {
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

        // Collect filesystem block IDs
        Set<String> filesystemBlockIds = new HashSet<>();
        int imported = 0;

        try (Stream<Path> files = Files.list(blocktypesDir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".yaml")).toList()) {
                try {
                    // Read YAML and convert to JSON for migration
                    Document doc = yamlMapper.readValue(file.toFile(), Document.class);
                    String blockId = doc.getString("blockId");
                    filesystemBlockIds.add(blockId);

                    String json = objectMapper.writeValueAsString(doc);

                    // Migrate if needed
                    String entityType = doc.getString("_class");
                    if (entityType == null) {
                        entityType = "de.mhus.nimbus.world.shared.world.WBlockType";
                    }

                    String migratedJson = migrationService.migrateToLatest(json, entityType);
                    Document migratedDoc = Document.parse(migratedJson);

                    // Check if should import
                    if (!force) {
                        Document existing = mongoTemplate.findById(migratedDoc.get("_id"), Document.class, COLLECTION_NAME);
                        if (existing != null) {
                            Object fileUpdatedAt = migratedDoc.get("updatedAt");
                            Object dbUpdatedAt = existing.get("updatedAt");
                            if (fileUpdatedAt != null && dbUpdatedAt != null) {
                                if (dbUpdatedAt.toString().compareTo(fileUpdatedAt.toString()) > 0) {
                                    log.debug("Skipping blocktype {} (DB is newer)", blockId);
                                    continue;
                                }
                            }
                        }
                    }

                    // Save to MongoDB
                    mongoTemplate.save(migratedDoc, COLLECTION_NAME);
                    log.debug("Imported blocktype: {}", blockId);
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
                if (!filesystemBlockIds.contains(blockType.getBlockId())) {
                    blockTypeService.delete(worldId, blockType.getBlockId());
                    log.info("Deleted blocktype not in filesystem: {}", blockType.getBlockId());
                    deleted++;
                }
            }
        }

        return ResourceSyncType.ImportResult.of(imported, deleted);
    }
}
