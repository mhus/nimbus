package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.world.shared.generator.WFlat;
import de.mhus.nimbus.world.shared.generator.WFlatService;
import de.mhus.nimbus.world.shared.rest.BaseEditorController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for WFlat operations.
 * Base path: /control/flats
 * <p>
 * Provides access to flat terrain data for editing.
 */
@RestController
@RequestMapping("/control/flats")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Flats", description = "Flat terrain data management")
public class FlatController extends BaseEditorController {

    private final WFlatService flatService;

    // DTOs
    public record FlatListDto(
            String id,
            String worldId,
            String layerDataId,
            String flatId,
            String title,
            String description,
            int sizeX,
            int sizeZ,
            int mountX,
            int mountZ,
            int oceanLevel,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record FlatDetailDto(
            String id,
            String worldId,
            String layerDataId,
            String flatId,
            String title,
            String description,
            int sizeX,
            int sizeZ,
            int mountX,
            int mountZ,
            int oceanLevel,
            String oceanBlockId,
            boolean unknownProtected,
            byte[] levels,
            byte[] columns,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record UpdateFlatMetadataRequest(
            String title,
            String description
    ) {}


    /**
     * List all flats for a world.
     * GET /control/flats?worldId={worldId}
     */
    @GetMapping
    @Operation(summary = "List flats for a world")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Missing worldId parameter")
    })
    public ResponseEntity<List<FlatListDto>> listFlats(
            @Parameter(description = "World ID", required = true)
            @RequestParam String worldId) {

        log.debug("Listing flats for worldId: {}", worldId);

        List<WFlat> flats = flatService.findByWorldId(worldId);

        List<FlatListDto> dtos = flats.stream()
                .map(flat -> new FlatListDto(
                        flat.getId(),
                        flat.getWorldId(),
                        flat.getLayerDataId(),
                        flat.getFlatId(),
                        flat.getTitle(),
                        flat.getDescription(),
                        flat.getSizeX(),
                        flat.getSizeZ(),
                        flat.getMountX(),
                        flat.getMountZ(),
                        flat.getOceanLevel(),
                        flat.getCreatedAt(),
                        flat.getUpdatedAt()
                ))
                .collect(Collectors.toList());

        log.info("Found {} flats for worldId: {}", dtos.size(), worldId);
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get flat details by ID.
     * GET /control/flats/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get flat details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Flat not found")
    })
    public ResponseEntity<FlatDetailDto> getFlat(
            @Parameter(description = "Flat ID", required = true)
            @PathVariable String id) {

        log.debug("Getting flat details: id={}", id);

        Optional<WFlat> flatOpt = flatService.findById(id);
        if (flatOpt.isEmpty()) {
            log.warn("Flat not found: id={}", id);
            return ResponseEntity.notFound().build();
        }

        WFlat flat = flatOpt.get();
        FlatDetailDto dto = new FlatDetailDto(
                flat.getId(),
                flat.getWorldId(),
                flat.getLayerDataId(),
                flat.getFlatId(),
                flat.getTitle(),
                flat.getDescription(),
                flat.getSizeX(),
                flat.getSizeZ(),
                flat.getMountX(),
                flat.getMountZ(),
                flat.getOceanLevel(),
                flat.getOceanBlockId(),
                flat.isUnknownProtected(),
                flat.getLevels(),
                flat.getColumns(),
                flat.getCreatedAt(),
                flat.getUpdatedAt()
        );

        return ResponseEntity.ok(dto);
    }

    /**
     * Update flat metadata (title and description).
     * PATCH /control/flats/{id}/metadata
     */
    @PatchMapping("/{id}/metadata")
    @Operation(summary = "Update flat metadata")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Flat not found")
    })
    public ResponseEntity<FlatDetailDto> updateFlatMetadata(
            @Parameter(description = "Flat ID", required = true)
            @PathVariable String id,
            @RequestBody UpdateFlatMetadataRequest request) {

        log.info("Updating flat metadata: id={}", id);

        // Load flat
        Optional<WFlat> flatOpt = flatService.findById(id);
        if (flatOpt.isEmpty()) {
            log.warn("Flat not found for metadata update: id={}", id);
            return ResponseEntity.notFound().build();
        }

        WFlat flat = flatOpt.get();

        // Update metadata
        flat.setTitle(request.title());
        flat.setDescription(request.description());
        flat.touchUpdate();

        // Save to database
        WFlat updated = flatService.update(flat);

        log.info("Flat metadata updated successfully: id={}", id);

        // Return updated flat details
        FlatDetailDto dto = new FlatDetailDto(
                updated.getId(),
                updated.getWorldId(),
                updated.getLayerDataId(),
                updated.getFlatId(),
                updated.getTitle(),
                updated.getDescription(),
                updated.getSizeX(),
                updated.getSizeZ(),
                updated.getMountX(),
                updated.getMountZ(),
                updated.getOceanLevel(),
                updated.getOceanBlockId(),
                updated.isUnknownProtected(),
                updated.getLevels(),
                updated.getColumns(),
                updated.getCreatedAt(),
                updated.getUpdatedAt()
        );

        return ResponseEntity.ok(dto);
    }

    /**
     * Delete flat by ID.
     * DELETE /control/flats/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete flat")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Flat not found")
    })
    public ResponseEntity<Void> deleteFlat(
            @Parameter(description = "Flat ID", required = true)
            @PathVariable String id) {

        log.info("Deleting flat: id={}", id);

        // Check if exists
        Optional<WFlat> flatOpt = flatService.findById(id);
        if (flatOpt.isEmpty()) {
            log.warn("Flat not found for deletion: id={}", id);
            return ResponseEntity.notFound().build();
        }

        flatService.deleteById(id);
        log.info("Flat deleted successfully: id={}", id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get height map image for flat.
     * GET /control/flats/{id}/height-map
     */
    @GetMapping("/{id}/height-map")
    @Operation(summary = "Get height map image")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Flat not found")
    })
    public ResponseEntity<byte[]> getHeightMap(
            @Parameter(description = "Flat ID", required = true)
            @PathVariable String id) {

        log.debug("Generating height map for flat: id={}", id);

        // Load flat
        Optional<WFlat> flatOpt = flatService.findById(id);
        if (flatOpt.isEmpty()) {
            log.warn("Flat not found for height map: id={}", id);
            return ResponseEntity.notFound().build();
        }

        WFlat flat = flatOpt.get();

        try {
            // Generate height map image
            byte[] imageBytes = generateHeightMapImage(flat);

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                    .body(imageBytes);
        } catch (Exception e) {
            log.error("Failed to generate height map", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get block map image for flat.
     * GET /control/flats/{id}/block-map
     */
    @GetMapping("/{id}/block-map")
    @Operation(summary = "Get block map image")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Flat not found")
    })
    public ResponseEntity<byte[]> getBlockMap(
            @Parameter(description = "Flat ID", required = true)
            @PathVariable String id) {

        log.debug("Generating block map for flat: id={}", id);

        // Load flat
        Optional<WFlat> flatOpt = flatService.findById(id);
        if (flatOpt.isEmpty()) {
            log.warn("Flat not found for block map: id={}", id);
            return ResponseEntity.notFound().build();
        }

        WFlat flat = flatOpt.get();

        try {
            // Generate block map image
            byte[] imageBytes = generateBlockMapImage(flat);

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                    .body(imageBytes);
        } catch (Exception e) {
            log.error("Failed to generate block map", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate height map image from flat data.
     * Blue (low) -> Green (mid) -> Red (high)
     */
    private byte[] generateHeightMapImage(WFlat flat) throws java.io.IOException {
        int width = flat.getSizeX();
        int height = flat.getSizeZ();
        byte[] levels = flat.getLevels();

        // Find min/max for color mapping
        int minLevel = Integer.MAX_VALUE;
        int maxLevel = Integer.MIN_VALUE;
        for (int i = 0; i < levels.length; i++) {
            int level = levels[i] & 0xFF; // Convert to unsigned
            if (level < minLevel) minLevel = level;
            if (level > maxLevel) maxLevel = level;
        }

        int range = maxLevel - minLevel;
        if (range == 0) range = 1;

        // Create image
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);

        // Draw height map
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int index = z * width + x;
                int level = levels[index] & 0xFF;
                float normalized = (float)(level - minLevel) / range;

                // Color gradient: blue (low) -> green (mid) -> red (high)
                int r, g, b;
                if (normalized < 0.5f) {
                    // Blue to green
                    float t = normalized * 2;
                    r = 0;
                    g = (int)(t * 255);
                    b = (int)((1 - t) * 255);
                } else {
                    // Green to red
                    float t = (normalized - 0.5f) * 2;
                    r = (int)(t * 255);
                    g = (int)((1 - t) * 255);
                    b = 0;
                }

                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, z, rgb);
            }
        }

        // Convert to PNG bytes
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Generate block map image from flat data.
     * Each block type ID gets a unique color.
     */
    private byte[] generateBlockMapImage(WFlat flat) throws java.io.IOException {
        int width = flat.getSizeX();
        int height = flat.getSizeZ();
        byte[] columns = flat.getColumns();

        // Create image
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);

        // Draw block map
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int index = z * width + x;
                int blockTypeId = columns[index] & 0xFF; // Convert to unsigned

                int rgb = getBlockColor(blockTypeId);
                image.setRGB(x, z, rgb);
            }
        }

        // Convert to PNG bytes
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Get RGB color for block type ID.
     */
    private int getBlockColor(int id) {
        if (id == 0) return 0x000000; // Black for air

        // Predefined colors for common block types
        int[] colors = {
                0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0x00FFFF, 0xFF00FF, 0xFFA500, 0x800080,
                0xA52A2A, 0xFFC0CB, 0xFFD700, 0xC0C0C0, 0x808080, 0x800000, 0x808000, 0x008000,
                0x008080, 0x000080, 0xFF6347, 0x4682B4, 0xD2691E, 0xCD5C5C, 0xF08080, 0xFA8072,
                0xE9967A, 0xFFA07A, 0xDC143C, 0xFF1493, 0xFF69B4, 0xFFB6C1, 0xFFC0CB, 0xDB7093
        };

        if (id <= colors.length) {
            return colors[id - 1];
        }

        // Generate color based on ID using HSL-like algorithm
        float hue = ((id * 137.5f) % 360) / 360f;
        float saturation = 0.7f + ((id % 30) / 100f);
        float lightness = 0.45f + ((id % 20) / 100f);

        return hslToRgb(hue, saturation, lightness);
    }

    /**
     * Convert HSL to RGB color.
     */
    private int hslToRgb(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = l - c / 2;

        float r, g, b;
        int hi = (int)(h * 6);
        switch (hi) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }

        int ri = (int)((r + m) * 255);
        int gi = (int)((g + m) * 255);
        int bi = (int)((b + m) * 255);

        return (ri << 16) | (gi << 8) | bi;
    }

    /**
     * Export flat data as JSON file for download.
     * GET /control/flats/{id}/export
     * Downloads levels, columns, and materials as JSON file.
     */
    @GetMapping("/{id}/export")
    @Operation(summary = "Export flat data as JSON file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Flat not found")
    })
    public ResponseEntity<byte[]> exportFlat(
            @Parameter(description = "Flat ID", required = true)
            @PathVariable String id) {

        log.info("Exporting flat data: id={}", id);

        try {
            // Load flat
            Optional<WFlat> flatOpt = flatService.findById(id);
            if (flatOpt.isEmpty()) {
                log.warn("Flat not found for export: id={}", id);
                return ResponseEntity.notFound().build();
            }

            WFlat flat = flatOpt.get();

            // Build JSON with levels, columns, and materials
            StringBuilder json = new StringBuilder();
            json.append("{");

            // Levels array
            json.append("\"levels\":[");
            byte[] levels = flat.getLevels();
            for (int i = 0; i < levels.length; i++) {
                if (i > 0) json.append(",");
                json.append(levels[i] & 0xFF); // unsigned
            }
            json.append("],");

            // Columns array
            json.append("\"columns\":[");
            byte[] columns = flat.getColumns();
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) json.append(",");
                json.append(columns[i] & 0xFF); // unsigned
            }
            json.append("],");

            // Materials map
            json.append("\"materials\":{");
            HashMap<Byte, WFlat.MaterialDefinition> materials = flat.getMaterials();
            if (materials != null && !materials.isEmpty()) {
                boolean first = true;
                for (Map.Entry<Byte, WFlat.MaterialDefinition> entry : materials.entrySet()) {
                    if (!first) json.append(",");
                    first = false;

                    json.append("\"").append(entry.getKey() & 0xFF).append("\":{");
                    WFlat.MaterialDefinition mat = entry.getValue();
                    json.append("\"blockDef\":").append(escapeJson(mat.getBlockDef())).append(",");
                    json.append("\"nextBlockDef\":").append(mat.getNextBlockDef() != null ? escapeJson(mat.getNextBlockDef()) : "null").append(",");
                    json.append("\"hasOcean\":").append(mat.isHasOcean());
                    json.append("}");
                }
            }
            json.append("}");

            json.append("}");

            byte[] jsonBytes = json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // Build filename: flat_{worldId}_{flatId}_{title}_{dateTime}.wflat.json
            String normalizedTitle = normalizeForFilename(flat.getTitle());
            String dateTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("flat_%s_%s_%s_%s.wflat.json",
                    flat.getWorldId(),
                    flat.getFlatId(),
                    normalizedTitle,
                    dateTime
            );

            log.info("Flat exported successfully: id={}, size={} bytes, filename={}", id, jsonBytes.length, filename);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(jsonBytes);

        } catch (Exception e) {
            log.error("Failed to export flat", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Import flat data from uploaded JSON file.
     * POST /control/flats/{id}/import
     * Updates levels, columns, and materials from uploaded file.
     */
    @PostMapping("/{id}/import")
    @Operation(summary = "Import flat data from JSON file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Flat not found"),
            @ApiResponse(responseCode = "400", description = "Invalid file or data")
    })
    public ResponseEntity<FlatDetailDto> importFlat(
            @Parameter(description = "Flat ID", required = true)
            @PathVariable String id,
            @Parameter(description = "JSON file to import", required = true)
            @RequestParam("file") MultipartFile file) {

        log.info("Importing flat data: id={}, filename={}", id, file.getOriginalFilename());

        try {
            // Validate file
            if (file.isEmpty()) {
                log.warn("Empty file uploaded");
                return ResponseEntity.badRequest().build();
            }

            // Load flat
            Optional<WFlat> flatOpt = flatService.findById(id);
            if (flatOpt.isEmpty()) {
                log.warn("Flat not found for import: id={}", id);
                return ResponseEntity.notFound().build();
            }

            WFlat flat = flatOpt.get();

            // Read and parse JSON
            String jsonContent = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);

            // Parse JSON manually (simple parsing for our structure)
            Map<String, Object> data = parseSimpleJson(jsonContent);

            // Extract levels
            @SuppressWarnings("unchecked")
            List<Integer> levelsList = (List<Integer>) data.get("levels");
            if (levelsList == null) {
                log.warn("Missing levels in import data");
                return ResponseEntity.badRequest().build();
            }

            // Extract columns
            @SuppressWarnings("unchecked")
            List<Integer> columnsList = (List<Integer>) data.get("columns");
            if (columnsList == null) {
                log.warn("Missing columns in import data");
                return ResponseEntity.badRequest().build();
            }

            // Validate size
            int expectedSize = flat.getSizeX() * flat.getSizeZ();
            if (levelsList.size() != expectedSize || columnsList.size() != expectedSize) {
                log.warn("Invalid import data: size mismatch. Expected: {}, got levels: {}, columns: {}",
                        expectedSize, levelsList.size(), columnsList.size());
                return ResponseEntity.badRequest().build();
            }

            // Convert to byte arrays
            byte[] newLevels = new byte[levelsList.size()];
            for (int i = 0; i < levelsList.size(); i++) {
                newLevels[i] = levelsList.get(i).byteValue();
            }

            byte[] newColumns = new byte[columnsList.size()];
            for (int i = 0; i < columnsList.size(); i++) {
                newColumns[i] = columnsList.get(i).byteValue();
            }

            // Extract materials
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> materialsMap = (Map<String, Map<String, Object>>) data.get("materials");
            HashMap<Byte, WFlat.MaterialDefinition> newMaterials = new HashMap<>();
            if (materialsMap != null) {
                for (Map.Entry<String, Map<String, Object>> entry : materialsMap.entrySet()) {
                    byte key = Byte.parseByte(entry.getKey());
                    Map<String, Object> matData = entry.getValue();

                    WFlat.MaterialDefinition matDef = WFlat.MaterialDefinition.builder()
                            .blockDef((String) matData.get("blockDef"))
                            .nextBlockDef((String) matData.get("nextBlockDef"))
                            .hasOcean((Boolean) matData.get("hasOcean"))
                            .build();

                    newMaterials.put(key, matDef);
                }
            }

            // Update flat data
            flat.setLevels(newLevels);
            flat.setColumns(newColumns);
            flat.setMaterials(newMaterials);
            flat.touchUpdate();

            // Save to database
            WFlat updated = flatService.update(flat);

            log.info("Flat imported successfully: id={}", id);

            // Return updated flat details
            FlatDetailDto dto = new FlatDetailDto(
                    updated.getId(),
                    updated.getWorldId(),
                    updated.getLayerDataId(),
                    updated.getFlatId(),
                    updated.getTitle(),
                    updated.getDescription(),
                    updated.getSizeX(),
                    updated.getSizeZ(),
                    updated.getMountX(),
                    updated.getMountZ(),
                    updated.getOceanLevel(),
                    updated.getOceanBlockId(),
                    updated.isUnknownProtected(),
                    updated.getLevels(),
                    updated.getColumns(),
                    updated.getCreatedAt(),
                    updated.getUpdatedAt()
            );

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            log.error("Failed to import flat", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Escape JSON string
     */
    private String escapeJson(String str) {
        if (str == null) return "null";
        return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * Normalize string for use in filename
     * Removes/replaces special characters, spaces, etc.
     * Limits length to 50 characters.
     */
    private String normalizeForFilename(String str) {
        if (str == null || str.isBlank()) {
            return "untitled";
        }

        // Replace spaces with underscores, remove special characters
        String normalized = str.trim()
                .replaceAll("[\\s]+", "_")  // Replace whitespace with underscore
                .replaceAll("[^a-zA-Z0-9_-]", "")  // Remove special characters except underscore and dash
                .replaceAll("_{2,}", "_")  // Replace multiple underscores with single
                .toLowerCase();

        // Limit length to 50 characters
        if (normalized.length() > 50) {
            normalized = normalized.substring(0, 50);
            // Remove trailing underscore if any
            if (normalized.endsWith("_")) {
                normalized = normalized.substring(0, 49);
            }
        }

        return normalized;
    }

    /**
     * Simple JSON parser for our specific structure
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSimpleJson(String json) {
        // Use Jackson ObjectMapper for proper JSON parsing
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}
