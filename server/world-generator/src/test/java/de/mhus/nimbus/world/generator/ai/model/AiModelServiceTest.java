package de.mhus.nimbus.world.generator.ai.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AiModelService.
 */
@ExtendWith(MockitoExtension.class)
class AiModelServiceTest {

    @Mock(lenient = true)
    private AiChat mockChat;

    @Mock(lenient = true)
    private LangchainModel openaiProvider;

    @Mock(lenient = true)
    private LangchainModel geminiProvider;

    private AiModelService service;

    @BeforeEach
    void setUp() {
        when(openaiProvider.getName()).thenReturn("openai");
        when(openaiProvider.isAvailable()).thenReturn(true);

        when(geminiProvider.getName()).thenReturn("gemini");
        when(geminiProvider.isAvailable()).thenReturn(true);

        when(mockChat.getName()).thenReturn("openai:gpt-4");
        when(mockChat.isAvailable()).thenReturn(true);

        List<LangchainModel> providers = List.of(openaiProvider, geminiProvider);
        String mappingsConfig = "chat=openai:gpt-4,generate=gemini:gemini-pro";

        service = new AiModelService(providers, mappingsConfig);
    }

    @Test
    void testGetAvailableProviders() {
        List<String> providers = service.getAvailableProviders();

        assertThat(providers).hasSize(2);
        assertThat(providers).contains("openai", "gemini");
    }

    @Test
    void testGetProvider() {
        Optional<LangchainModel> provider = service.getProvider("openai");

        assertThat(provider).isPresent();
        assertThat(provider.get()).isEqualTo(openaiProvider);
    }

    @Test
    void testGetProvider_notFound() {
        Optional<LangchainModel> provider = service.getProvider("unknown");

        assertThat(provider).isEmpty();
    }

    @Test
    void testIsProviderAvailable() {
        assertThat(service.isProviderAvailable("openai")).isTrue();
        assertThat(service.isProviderAvailable("gemini")).isTrue();
        assertThat(service.isProviderAvailable("unknown")).isFalse();
    }

    @Test
    void testIsProviderAvailable_notAvailable() {
        when(geminiProvider.isAvailable()).thenReturn(false);

        assertThat(service.isProviderAvailable("gemini")).isFalse();
    }

    @Test
    void testCreateChat_directModelName() {
        when(openaiProvider.createAiChat(eq("gpt-4"), any(AiChatOptions.class)))
                .thenReturn(Optional.of(mockChat));

        Optional<AiChat> chat = service.createChat("openai:gpt-4");

        assertThat(chat).isPresent();
        assertThat(chat.get().getName()).isEqualTo("openai:gpt-4");
        verify(openaiProvider).createAiChat(eq("gpt-4"), any(AiChatOptions.class));
    }

    @Test
    void testCreateChat_withOptions() {
        AiChatOptions options = AiChatOptions.builder()
                .temperature(0.9)
                .maxTokens(2000)
                .build();

        when(geminiProvider.createAiChat(eq("gemini-pro"), any(AiChatOptions.class)))
                .thenReturn(Optional.of(mockChat));

        Optional<AiChat> chat = service.createChat("gemini:gemini-pro", options);

        assertThat(chat).isPresent();
        verify(geminiProvider).createAiChat(eq("gemini-pro"), eq(options));
    }

    @Test
    void testCreateChat_mappedModelName() {
        when(openaiProvider.createAiChat(eq("gpt-4"), any(AiChatOptions.class)))
                .thenReturn(Optional.of(mockChat));

        Optional<AiChat> chat = service.createChat("default:chat");

        assertThat(chat).isPresent();
        verify(openaiProvider).createAiChat(eq("gpt-4"), any(AiChatOptions.class));
    }

    @Test
    void testCreateChat_unknownProvider() {
        Optional<AiChat> chat = service.createChat("unknown:model");

        assertThat(chat).isEmpty();
    }

    @Test
    void testCreateChat_invalidFormat() {
        Optional<AiChat> chat = service.createChat("invalid-format");

        assertThat(chat).isEmpty();
    }

    @Test
    void testCreateChat_emptyModelName() {
        Optional<AiChat> chat = service.createChat("");

        assertThat(chat).isEmpty();
    }

    @Test
    void testCreateChat_providerNotAvailable() {
        when(geminiProvider.isAvailable()).thenReturn(false);

        Optional<AiChat> chat = service.createChat("gemini:gemini-pro");

        assertThat(chat).isEmpty();
    }

    @Test
    void testRegisterMapping() {
        service.registerMapping("custom", "openai:gpt-3.5-turbo");

        when(openaiProvider.createAiChat(eq("gpt-3.5-turbo"), any(AiChatOptions.class)))
                .thenReturn(Optional.of(mockChat));

        Optional<AiChat> chat = service.createChat("default:custom");

        assertThat(chat).isPresent();
        verify(openaiProvider).createAiChat(eq("gpt-3.5-turbo"), any(AiChatOptions.class));
    }

    @Test
    void testGetMappings() {
        var mappings = service.getMappings();

        assertThat(mappings).hasSize(2);
        assertThat(mappings).containsEntry("default:chat", "openai:gpt-4");
        assertThat(mappings).containsEntry("default:generate", "gemini:gemini-pro");
    }

    @Test
    void testInitializationWithoutMappings() {
        List<LangchainModel> providers = List.of(openaiProvider);
        AiModelService serviceWithoutMappings = new AiModelService(providers, "");

        assertThat(serviceWithoutMappings.getMappings()).isEmpty();
        assertThat(serviceWithoutMappings.getAvailableProviders()).contains("openai");
    }
}
