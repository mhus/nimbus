package de.mhus.nimbus.world.generator.ai.model.openai;

import de.mhus.nimbus.world.generator.ai.model.AiChat;
import de.mhus.nimbus.world.generator.ai.model.AiChatOptions;
import de.mhus.nimbus.world.generator.ai.model.LangchainModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * OpenAI implementation of LangchainModel.
 * Supports GPT models (gpt-3.5-turbo, gpt-4, gpt-4-turbo, etc.)
 */
@Component
@Slf4j
public class OpenAiLangchainModel implements LangchainModel {

    private static final String PROVIDER_NAME = "openai";

    @Value("${langchain4j.openai.api-key:}")
    private String apiKey;

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public Optional<AiChat> createAiChat(String modelName, AiChatOptions options) {
        if (!isAvailable()) {
            log.warn("OpenAI API key not configured");
            return Optional.empty();
        }

        try {
            ChatLanguageModel chatModel = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .temperature(options.getTemperature())
                    .maxTokens(options.getMaxTokens())
                    .timeout(Duration.ofSeconds(options.getTimeoutSeconds()))
                    .logRequests(options.getLogRequests())
                    .logResponses(options.getLogRequests())
                    .build();

            String fullName = PROVIDER_NAME + ":" + modelName;
            AiChat chat = new OpenAiChat(fullName, chatModel, options);

            log.info("Created OpenAI chat: model={}", modelName);
            return Optional.of(chat);

        } catch (Exception e) {
            log.error("Failed to create OpenAI chat: model={}", modelName, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }
}
