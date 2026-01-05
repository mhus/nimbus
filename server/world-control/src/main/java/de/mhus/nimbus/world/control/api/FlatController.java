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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
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
}
