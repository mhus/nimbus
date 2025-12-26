package de.mhus.nimbus.world.generator.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for AI-powered world generation using LangChain4j.
 * Provides intelligent terrain generation, biome suggestions, and world description generation.
 */
//@Service
@Slf4j
@RequiredArgsConstructor
public class AiWorldGeneratorService {

    private final ChatLanguageModel chatLanguageModel;

    /**
     * Generate world description based on parameters.
     *
     * @param worldType Type of world (e.g., "fantasy", "sci-fi", "medieval")
     * @param size World size description
     * @param theme Additional theme information
     * @return AI-generated world description
     */
    public String generateWorldDescription(String worldType, String size, String theme) {
        if (chatLanguageModel == null) {
            log.warn("ChatLanguageModel not available, returning default description");
            return "A procedurally generated world.";
        }

        try {
            PromptTemplate template = PromptTemplate.from(
                    "Generate a creative and immersive description for a {{worldType}} world. " +
                    "The world is {{size}} in size and has a {{theme}} theme. " +
                    "Keep the description between 2-3 sentences and make it engaging for players."
            );

            Map<String, Object> variables = new HashMap<>();
            variables.put("worldType", worldType);
            variables.put("size", size);
            variables.put("theme", theme);

            Prompt prompt = template.apply(variables);
            Response<AiMessage> response = chatLanguageModel.generate(java.util.List.of(prompt.toUserMessage()));

            String description = response.content().text();
            log.info("Generated world description: {}", description);
            return description;

        } catch (Exception e) {
            log.error("Failed to generate world description", e);
            return "A procedurally generated world.";
        }
    }

    /**
     * Suggest biome types based on world characteristics.
     *
     * @param terrainType Terrain type (e.g., "hilly", "flat", "mountainous")
     * @param climate Climate description
     * @return AI-suggested biome types
     */
    public String suggestBiomes(String terrainType, String climate) {
        if (chatLanguageModel == null) {
            log.warn("ChatLanguageModel not available, returning default biomes");
            return "grassland, forest, plains";
        }

        try {
            PromptTemplate template = PromptTemplate.from(
                    "Suggest 3-5 appropriate biome types for a world with {{terrainType}} terrain " +
                    "and {{climate}} climate. Return only the biome names separated by commas, " +
                    "without any additional explanation."
            );

            Map<String, Object> variables = new HashMap<>();
            variables.put("terrainType", terrainType);
            variables.put("climate", climate);

            Prompt prompt = template.apply(variables);
            Response<AiMessage> response = chatLanguageModel.generate(java.util.List.of(prompt.toUserMessage()));

            String biomes = response.content().text().trim();
            log.info("Suggested biomes: {}", biomes);
            return biomes;

        } catch (Exception e) {
            log.error("Failed to suggest biomes", e);
            return "grassland, forest, plains";
        }
    }

    /**
     * Generate terrain generation parameters based on natural language description.
     *
     * @param description Natural language description of desired terrain
     * @return Map of generation parameters
     */
    public Map<String, String> generateTerrainParameters(String description) {
        if (chatLanguageModel == null) {
            log.warn("ChatLanguageModel not available, returning default parameters");
            return getDefaultTerrainParameters();
        }

        try {
            PromptTemplate template = PromptTemplate.from(
                    "Based on this terrain description: '{{description}}', " +
                    "generate appropriate terrain generation parameters. " +
                    "Return the result as key-value pairs in the format: " +
                    "heightVariation:value,hilliness:value,roughness:value " +
                    "where values are between 0 and 1. Only return the parameters, no explanation."
            );

            Map<String, Object> variables = new HashMap<>();
            variables.put("description", description);

            Prompt prompt = template.apply(variables);
            Response<AiMessage> response = chatLanguageModel.generate(java.util.List.of(prompt.toUserMessage()));

            String paramString = response.content().text().trim();
            Map<String, String> parameters = parseParameterString(paramString);
            log.info("Generated terrain parameters: {}", parameters);
            return parameters;

        } catch (Exception e) {
            log.error("Failed to generate terrain parameters", e);
            return getDefaultTerrainParameters();
        }
    }

    /**
     * Generate creative block type suggestions for a specific location.
     *
     * @param biome Current biome
     * @param height Height level (y-coordinate)
     * @param context Additional context
     * @return Suggested block type
     */
    public String suggestBlockType(String biome, int height, String context) {
        if (chatLanguageModel == null) {
            return getDefaultBlockType(height);
        }

        try {
            PromptTemplate template = PromptTemplate.from(
                    "For a {{biome}} biome at height {{height}}, suggest an appropriate block type. " +
                    "Context: {{context}}. " +
                    "Return only the block type name (e.g., 'grass', 'stone', 'sand'), no explanation."
            );

            Map<String, Object> variables = new HashMap<>();
            variables.put("biome", biome);
            variables.put("height", String.valueOf(height));
            variables.put("context", context);

            Prompt prompt = template.apply(variables);
            Response<AiMessage> response = chatLanguageModel.generate(java.util.List.of(prompt.toUserMessage()));

            String blockType = response.content().text().trim().toLowerCase();
            log.debug("Suggested block type for biome={}, height={}: {}", biome, height, blockType);
            return blockType;

        } catch (Exception e) {
            log.error("Failed to suggest block type", e);
            return getDefaultBlockType(height);
        }
    }

    /**
     * Check if AI service is available.
     *
     * @return true if ChatLanguageModel is configured
     */
    public boolean isAvailable() {
        return chatLanguageModel != null;
    }

    private Map<String, String> getDefaultTerrainParameters() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("heightVariation", "0.5");
        defaults.put("hilliness", "0.5");
        defaults.put("roughness", "0.3");
        return defaults;
    }

    private String getDefaultBlockType(int height) {
        if (height < 5) return "stone";
        if (height < 60) return "dirt";
        if (height <= 63) return "grass";
        return "air";
    }

    private Map<String, String> parseParameterString(String paramString) {
        Map<String, String> result = new HashMap<>();
        try {
            String[] pairs = paramString.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    result.put(kv[0].trim(), kv[1].trim());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse parameter string: {}", paramString, e);
        }
        return result.isEmpty() ? getDefaultTerrainParameters() : result;
    }
}
