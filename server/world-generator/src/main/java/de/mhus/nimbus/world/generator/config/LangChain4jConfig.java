package de.mhus.nimbus.world.generator.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for LangChain4j integration.
 * Provides AI model beans for world generation.
 */
@Configuration
@Slf4j
public class LangChain4jConfig {

    @Value("${langchain4j.openai.api-key:}")
    private String openAiApiKey;

    @Value("${langchain4j.openai.model-name:gpt-3.5-turbo}")
    private String modelName;

    @Value("${langchain4j.openai.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${langchain4j.openai.temperature:0.7}")
    private double temperature;

    @Value("${langchain4j.openai.max-tokens:1000}")
    private int maxTokens;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            log.warn("OpenAI API key not configured. LangChain4j features will be limited.");
            return null;
        }

        log.info("Initializing OpenAI ChatLanguageModel: model={}, timeout={}s, temperature={}",
                modelName, timeoutSeconds, temperature);

        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .temperature(temperature)
                .maxTokens(maxTokens)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
