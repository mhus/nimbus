package de.mhus.nimbus.world.generator.ai.model.gemini;

import de.mhus.nimbus.world.generator.ai.model.AiChat;
import de.mhus.nimbus.world.generator.ai.model.AiChatOptions;
import de.mhus.nimbus.world.generator.ai.model.LangchainModel;
import de.mhus.nimbus.world.generator.ai.model.SimpleRateLimiter;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Google Gemini implementation of LangchainModel.
 * Supports Gemini models (gemini-pro, gemini-pro-vision, etc.)
 * Includes rate limiting to respect API quotas.
 */
@Component
@Slf4j
public class GeminiLangchainModel implements LangchainModel {

    private static final String PROVIDER_NAME = "gemini";
    private static final int DEFAULT_RATE_LIMIT = 15; // Gemini free tier: 15 RPM

    @Value("${langchain4j.gemini.api-key:}")
    private String apiKey;

    @Value("${langchain4j.gemini.rate-limit:15}")
    private int rateLimit;

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public Optional<AiChat> createAiChat(String modelName, AiChatOptions options) {
        if (!isAvailable()) {
            log.warn("Gemini API key not configured");
            return Optional.empty();
        }

        try {
            ChatLanguageModel chatModel = GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .temperature(options.getTemperature())
                    .maxOutputTokens(options.getMaxTokens())
                    .timeout(Duration.ofSeconds(options.getTimeoutSeconds()))
                    .logRequestsAndResponses(options.getLogRequests())
                    .build();

            String fullName = PROVIDER_NAME + ":" + modelName;
            SimpleRateLimiter rateLimiter = new SimpleRateLimiter(rateLimit);
            AiChat chat = new GeminiChat(fullName, chatModel, options, rateLimiter);

            log.info("Created Gemini chat: model={}, rateLimit={} RPM", modelName, rateLimit);
            return Optional.of(chat);

        } catch (Exception e) {
            log.error("Failed to create Gemini chat: model={}", modelName, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }
}
