package de.mhus.nimbus.tools.demoimport.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.asset.SAssetService;
import de.mhus.nimbus.shared.persistence.AssetMetadata;
import de.mhus.nimbus.shared.persistence.SAsset;
import de.mhus.nimbus.tools.demoimport.ImportStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports Assets from test_server files.
 * Reads from: {source-path}/assets/
 *
 * Structure:
 * - Binary files (*.png, *.wav, *.obj, etc.)
 * - Metadata files (*.png.info, *.wav.info, etc.) with description, dimensions, color
 *
 * Example: textures/items/sword.png + textures/items/sword.png.info
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssetImporter {

    private final SAssetService service;
    private final ObjectMapper objectMapper;

    @Value("${import.source-path:../../client/packages/test_server/files}")
    private String sourcePath;

    @Value("${import.asset-batch-size:50}")
    private int batchSize;

    public ImportStats importAll() throws Exception {
        log.info("Starting Asset import from: {}/assets/", sourcePath);

        ImportStats stats = new ImportStats();
        Path assetsDir = Path.of(sourcePath, "assets");

        if (!Files.exists(assetsDir)) {
            log.warn("Assets directory not found: {}", assetsDir);
            return stats;
        }

        // Recursively import all assets
        importDirectory(assetsDir, "", stats);

        log.info("Asset import completed: {} imported, {} skipped, {} failed",
                stats.getSuccessCount(), stats.getSkippedCount(), stats.getFailureCount());

        return stats;
    }

    private void importDirectory(Path dir, String relativePath, ImportStats stats) throws IOException {
        File[] files = dir.toFile().listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Recurse into subdirectory
                String subPath = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
                importDirectory(file.toPath(), subPath, stats);
            } else if (file.isFile() && !file.getName().endsWith(".info")) {
                // Import asset file (skip .info files, they're metadata)
                String assetPath = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
                importAsset(file, assetPath, stats);
            }
        }
    }

    private void importAsset(File assetFile, String assetPath, ImportStats stats) {
        try {
            // Read binary content
            byte[] content = Files.readAllBytes(assetFile.toPath());

            // Try to read metadata from .info file
            AssetMetadata metadata = loadMetadata(assetFile);

            // Determine MIME type and category
            String extension = getExtension(assetFile.getName());
            String mimeType = getMimeType(extension);
            String category = getCategory(assetPath);

            // Enhance metadata with derived properties
            if (metadata == null) {
                metadata = new AssetMetadata();
            }
            if (metadata.getMimeType() == null) {
                metadata.setMimeType(mimeType);
            }
            if (metadata.getCategory() == null) {
                metadata.setCategory(category);
            }
            if (metadata.getExtension() == null) {
                metadata.setExtension(extension);
            }

            // Save asset with metadata
            service.saveAsset("main", "main", assetPath, content, "demo-import", metadata);
            stats.incrementSuccess();

            if (stats.getSuccessCount() % batchSize == 0) {
                log.info("Progress: {} assets imported", stats.getSuccessCount());
            }

            log.trace("Imported asset: {} ({} bytes)", assetPath, content.length);

        } catch (Exception e) {
            log.error("Failed to import asset: {}", assetPath, e);
            stats.incrementFailure();
            System.exit(1);
        }
    }

    /**
     * Load metadata from .info file (e.g., sword.png.info).
     */
    private AssetMetadata loadMetadata(File assetFile) {
        File infoFile = new File(assetFile.getParentFile(), assetFile.getName() + ".info");
        if (!infoFile.exists()) {
            return null;
        }

        try {
            JsonNode json = objectMapper.readTree(infoFile);

            AssetMetadata metadata = new AssetMetadata();
            if (json.has("description")) {
                metadata.setDescription(json.get("description").asText());
            }
            if (json.has("width")) {
                metadata.setWidth(json.get("width").asInt());
            }
            if (json.has("height")) {
                metadata.setHeight(json.get("height").asInt());
            }
            if (json.has("color")) {
                metadata.setColor(json.get("color").asText());
            }

            log.trace("Loaded metadata from: {}", infoFile.getName());
            return metadata;

        } catch (Exception e) {
            log.warn("Failed to load metadata from: {}", infoFile.getName(), e);
            return null;
        }
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : "";
    }

    private String getMimeType(String ext) {
        return switch (ext.toLowerCase()) {
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".svg" -> "image/svg+xml";
            case ".json" -> "application/json";
            case ".obj" -> "model/obj";
            case ".mtl" -> "model/mtl";
            case ".glb", ".gltf" -> "model/gltf-binary";
            case ".wav" -> "audio/wav";
            case ".mp3" -> "audio/mpeg";
            case ".ogg" -> "audio/ogg";
            default -> "application/octet-stream";
        };
    }

    private String getCategory(String path) {
        if (path == null || !path.contains("/")) return "other";
        return path.substring(0, path.indexOf('/'));
    }
}
