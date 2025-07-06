package de.mhus.nimbus.shared.asset;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;

/**
 * Shared Asset class representing game assets similar to Minecraft.
 * Can contain binary data for textures, sounds, models, and other game resources.
 * Supports JSON serialization for persistence.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Asset implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the asset
     */
    private String id;

    /**
     * Human-readable name of the asset
     */
    private String name;

    /**
     * Type of the asset
     */
    private AssetType type;

    /**
     * Category for grouping related assets
     */
    private AssetCategory category;

    /**
     * Binary data of the asset (textures, sounds, models, etc.)
     * Excluded from JSON serialization to avoid large payloads
     */
    @JsonIgnore
    private byte[] data;

    /**
     * MIME type of the asset data
     */
    private String mimeType;

    /**
     * File extension/format (png, ogg, json, etc.)
     */
    private String format;

    /**
     * Size of the asset data in bytes
     */
    private long sizeBytes;

    /**
     * Asset metadata as key-value pairs
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Asset dependencies (other assets this asset depends on)
     */
    @Builder.Default
    private Map<String, String> dependencies = new HashMap<>();

    /**
     * Version of the asset
     */
    @Builder.Default
    private String version = "1.0.0";

    /**
     * Namespace for asset organization (similar to Minecraft resource packs)
     */
    @Builder.Default
    private String namespace = "nimbus";

    /**
     * Path within the namespace (e.g., "textures/blocks/stone.png")
     */
    private String path;

    /**
     * Asset types based on Minecraft asset categories
     */
    public enum AssetType {
        // Visual Assets
        TEXTURE,
        MODEL,
        ANIMATION,
        PARTICLE,
        SHADER,
        FONT,
        GUI,

        // Audio Assets
        SOUND,
        MUSIC,

        // Data Assets
        BLOCKSTATE,
        RECIPE,
        LOOT_TABLE,
        ADVANCEMENT,
        STRUCTURE,
        DIMENSION,
        BIOME,

        // Configuration Assets
        LANGUAGE,
        METADATA,
        PACK_MCMETA,

        // Custom Assets
        SCRIPT,
        CONFIG,
        OTHER
    }

    /**
     * Asset categories for organization
     */
    public enum AssetCategory {
        // Block-related assets
        BLOCKS,
        BLOCK_ENTITIES,

        // Item-related assets
        ITEMS,
        TOOLS,
        ARMOR,
        WEAPONS,

        // Entity-related assets
        ENTITIES,
        MOBS,
        PLAYERS,

        // Environment assets
        ENVIRONMENT,
        PARTICLES,
        WEATHER,

        // UI assets
        GUI,
        HUD,
        MENUS,

        // Audio assets
        AMBIENT,
        EFFECTS,
        MUSIC_TRACKS,

        // World generation
        WORLD_GEN,
        STRUCTURES,
        BIOMES,

        // Game mechanics
        RECIPES,
        LOOT,
        ADVANCEMENTS,

        // Customization
        RESOURCE_PACKS,
        DATA_PACKS,
        SHADERS,

        // System
        CORE,
        CONFIGURATION,
        OTHER
    }

    /**
     * Get the full resource path (namespace:path)
     */
    public String getResourcePath() {
        if (namespace == null || path == null) {
            return id;
        }
        return namespace + ":" + path;
    }

    /**
     * Check if asset has binary data
     */
    public boolean hasBinaryData() {
        return data != null && data.length > 0;
    }

    /**
     * Get asset size in KB
     */
    public double getSizeKB() {
        return sizeBytes / 1024.0;
    }

    /**
     * Get asset size in MB
     */
    public double getSizeMB() {
        return sizeBytes / (1024.0 * 1024.0);
    }

    /**
     * Add metadata entry
     */
    public void addMetadata(String key, String value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    /**
     * Get metadata value
     */
    public String getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * Add dependency
     */
    public void addDependency(String dependencyId, String version) {
        if (dependencies == null) {
            dependencies = new HashMap<>();
        }
        dependencies.put(dependencyId, version);
    }

    /**
     * Check if asset depends on another asset
     */
    public boolean dependsOn(String assetId) {
        return dependencies != null && dependencies.containsKey(assetId);
    }

    /**
     * Validate asset data
     */
    public boolean isValid() {
        if (id == null || id.trim().isEmpty()) {
            LOGGER.warn("Asset validation failed: ID is required");
            return false;
        }

        if (type == null) {
            LOGGER.warn("Asset validation failed: Type is required for asset {}", id);
            return false;
        }

        if (name == null || name.trim().isEmpty()) {
            LOGGER.warn("Asset validation failed: Name is required for asset {}", id);
            return false;
        }

        // Validate binary data consistency
        if (hasBinaryData()) {
            if (sizeBytes <= 0) {
                LOGGER.warn("Asset validation failed: Size mismatch for asset {} with binary data", id);
                return false;
            }

            if (data.length != sizeBytes) {
                LOGGER.warn("Asset validation failed: Actual data size {} doesn't match declared size {} for asset {}",
                           data.length, sizeBytes, id);
                return false;
            }
        }

        return true;
    }

    /**
     * Create a texture asset
     */
    public static Asset createTexture(String id, String name, byte[] textureData, String format) {
        return Asset.builder()
                .id(id)
                .name(name)
                .type(AssetType.TEXTURE)
                .category(AssetCategory.BLOCKS) // Default, can be changed
                .data(textureData)
                .format(format)
                .mimeType("image/" + format)
                .sizeBytes(textureData.length)
                .path("textures/" + id + "." + format)
                .build();
    }

    /**
     * Create a sound asset
     */
    public static Asset createSound(String id, String name, byte[] soundData, String format) {
        return Asset.builder()
                .id(id)
                .name(name)
                .type(AssetType.SOUND)
                .category(AssetCategory.EFFECTS)
                .data(soundData)
                .format(format)
                .mimeType("audio/" + format)
                .sizeBytes(soundData.length)
                .path("sounds/" + id + "." + format)
                .build();
    }

    /**
     * Create a model asset
     */
    public static Asset createModel(String id, String name, byte[] modelData, String format) {
        return Asset.builder()
                .id(id)
                .name(name)
                .type(AssetType.MODEL)
                .category(AssetCategory.BLOCKS)
                .data(modelData)
                .format(format)
                .mimeType("application/json") // Most Minecraft models are JSON
                .sizeBytes(modelData.length)
                .path("models/" + id + "." + format)
                .build();
    }

    /**
     * Create a recipe asset
     */
    public static Asset createRecipe(String id, String name, String recipeJson) {
        byte[] data = recipeJson.getBytes();
        return Asset.builder()
                .id(id)
                .name(name)
                .type(AssetType.RECIPE)
                .category(AssetCategory.RECIPES)
                .data(data)
                .format("json")
                .mimeType("application/json")
                .sizeBytes(data.length)
                .path("recipes/" + id + ".json")
                .build();
    }

    /**
     * Convert Asset to JSON string
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize Asset to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create Asset from JSON string
     */
    public static Asset fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, Asset.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to deserialize Asset from JSON: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("Asset{id='%s', name='%s', type=%s, category=%s, size=%d bytes, hasData=%s}",
                           id, name, type, category, sizeBytes, hasBinaryData());
    }
}
