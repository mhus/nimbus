package de.mhus.nimbus.world.generator.ai.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing AI models and their providers.
 * Maintains a lazy-loaded list of LangchainModel implementations
 * and provides model name mapping through configuration.
 */
@Service
@Slf4j
public class AiModelService {

    private final List<LangchainModel> modelProviders;
    private final Map<String, String> modelMappings = new ConcurrentHashMap<>();
    private final Map<String, LangchainModel> providerCache = new ConcurrentHashMap<>();

    public AiModelService(List<LangchainModel> modelProviders,
                          @Value("${ai.model.mappings:}") String mappingsConfig) {
        this.modelProviders = modelProviders;

        log.info("Initializing AiModelService with {} providers", modelProviders.size());

        // Initialize provider cache
        for (LangchainModel provider : modelProviders) {
            providerCache.put(provider.getName(), provider);
            log.info("Registered AI provider: {} (available: {})",
                    provider.getName(), provider.isAvailable());
        }

        // Parse model mappings from configuration
        parseMappings(mappingsConfig);
    }

    /**
     * Create an AI chat instance by full model name.
     * Format: provider:model or default:name (which resolves via mapping)
     *
     * @param fullModelName Full model name (e.g., "openai:gpt-4", "default:chat")
     * @param options Chat configuration options
     * @return AI chat instance if available
     */
    public Optional<AiChat> createChat(String fullModelName, AiChatOptions options) {
        if (fullModelName == null || fullModelName.isBlank()) {
            log.warn("Empty model name provided");
            return Optional.empty();
        }

        // Resolve default: prefix
        String resolvedName = resolveModelName(fullModelName);

        // Parse provider:model
        String[] parts = resolvedName.split(":", 2);
        if (parts.length != 2) {
            log.warn("Invalid model name format: {}. Expected 'provider:model'", resolvedName);
            return Optional.empty();
        }

        String providerName = parts[0];
        String modelName = parts[1];

        // Get provider
        LangchainModel provider = providerCache.get(providerName);
        if (provider == null) {
            log.warn("Unknown AI provider: {}", providerName);
            return Optional.empty();
        }

        if (!provider.isAvailable()) {
            log.warn("AI provider not available: {}", providerName);
            return Optional.empty();
        }

        // Create chat
        try {
            Optional<AiChat> chat = provider.createAiChat(modelName, options);
            if (chat.isPresent()) {
                log.info("Created AI chat: {}", chat.get().getName());
            } else {
                log.warn("Provider {} could not create model: {}", providerName, modelName);
            }
            return chat;
        } catch (Exception e) {
            log.error("Failed to create AI chat: {}:{}", providerName, modelName, e);
            return Optional.empty();
        }
    }

    /**
     * Create an AI chat instance with default options.
     *
     * @param fullModelName Full model name
     * @return AI chat instance if available
     */
    public Optional<AiChat> createChat(String fullModelName) {
        return createChat(fullModelName, AiChatOptions.defaults());
    }

    /**
     * Register a model mapping.
     * Maps "default:name" to a specific "provider:model".
     *
     * @param alias Alias name (without "default:" prefix)
     * @param targetModel Target model name (e.g., "openai:gpt-4")
     */
    public void registerMapping(String alias, String targetModel) {
        String key = "default:" + alias;
        modelMappings.put(key, targetModel);
        log.info("Registered model mapping: {} -> {}", key, targetModel);
    }

    /**
     * Get all available provider names.
     *
     * @return List of provider names
     */
    public List<String> getAvailableProviders() {
        return modelProviders.stream()
                .filter(LangchainModel::isAvailable)
                .map(LangchainModel::getName)
                .toList();
    }

    /**
     * Get a specific provider by name.
     *
     * @param providerName Provider name
     * @return Provider if available
     */
    public Optional<LangchainModel> getProvider(String providerName) {
        return Optional.ofNullable(providerCache.get(providerName));
    }

    /**
     * Check if a specific provider is available.
     *
     * @param providerName Provider name
     * @return true if provider exists and is available
     */
    public boolean isProviderAvailable(String providerName) {
        LangchainModel provider = providerCache.get(providerName);
        return provider != null && provider.isAvailable();
    }

    /**
     * Get all registered model mappings.
     *
     * @return Map of alias to target model
     */
    public Map<String, String> getMappings() {
        return new HashMap<>(modelMappings);
    }

    private String resolveModelName(String fullModelName) {
        // Check if it's a default: mapping
        if (fullModelName.startsWith("default:")) {
            String resolved = modelMappings.get(fullModelName);
            if (resolved != null) {
                log.debug("Resolved model mapping: {} -> {}", fullModelName, resolved);
                return resolved;
            }
            log.warn("No mapping found for: {}", fullModelName);
        }
        return fullModelName;
    }

    private void parseMappings(String mappingsConfig) {
        if (mappingsConfig == null || mappingsConfig.isBlank()) {
            log.debug("No model mappings configured");
            return;
        }

        // Format: "chat=openai:gpt-4,generate=gemini:gemini-pro"
        String[] mappings = mappingsConfig.split(",");
        for (String mapping : mappings) {
            String[] parts = mapping.split("=", 2);
            if (parts.length == 2) {
                registerMapping(parts[0].trim(), parts[1].trim());
            }
        }
    }
}
