package de.mhus.nimbus.world.generator.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AiWorldGeneratorService.
 */
@ExtendWith(MockitoExtension.class)
class AiWorldGeneratorServiceTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    private AiWorldGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new AiWorldGeneratorService(chatLanguageModel);
    }

    @Test
    void testIsAvailable_withModel() {
        assertThat(service.isAvailable()).isTrue();
    }

    @Test
    void testIsAvailable_withoutModel() {
        AiWorldGeneratorService serviceWithoutModel = new AiWorldGeneratorService(null);
        assertThat(serviceWithoutModel.isAvailable()).isFalse();
    }

    @Test
    void testGenerateWorldDescription() {
        String expectedDescription = "A vast fantasy realm filled with magic and mystery.";
        AiMessage aiMessage = AiMessage.from(expectedDescription);
        Response<AiMessage> response = Response.from(aiMessage);

        when(chatLanguageModel.generate(anyList())).thenReturn(response);

        String result = service.generateWorldDescription("fantasy", "large", "magical");

        assertThat(result).isEqualTo(expectedDescription);
        verify(chatLanguageModel, times(1)).generate(anyList());
    }

    @Test
    void testGenerateWorldDescription_withoutModel() {
        AiWorldGeneratorService serviceWithoutModel = new AiWorldGeneratorService(null);

        String result = serviceWithoutModel.generateWorldDescription("fantasy", "large", "magical");

        assertThat(result).isEqualTo("A procedurally generated world.");
    }

    @Test
    void testSuggestBiomes() {
        String expectedBiomes = "mountains, forests, valleys, rivers";
        AiMessage aiMessage = AiMessage.from(expectedBiomes);
        Response<AiMessage> response = Response.from(aiMessage);

        when(chatLanguageModel.generate(anyList())).thenReturn(response);

        String result = service.suggestBiomes("mountainous", "temperate");

        assertThat(result).isEqualTo(expectedBiomes);
        verify(chatLanguageModel, times(1)).generate(anyList());
    }

    @Test
    void testSuggestBiomes_withoutModel() {
        AiWorldGeneratorService serviceWithoutModel = new AiWorldGeneratorService(null);

        String result = serviceWithoutModel.suggestBiomes("mountainous", "temperate");

        assertThat(result).isEqualTo("grassland, forest, plains");
    }

    @Test
    void testGenerateTerrainParameters() {
        String paramString = "heightVariation:0.8,hilliness:0.7,roughness:0.6";
        AiMessage aiMessage = AiMessage.from(paramString);
        Response<AiMessage> response = Response.from(aiMessage);

        when(chatLanguageModel.generate(anyList())).thenReturn(response);

        Map<String, String> result = service.generateTerrainParameters("very hilly with rough terrain");

        assertThat(result).containsEntry("heightVariation", "0.8");
        assertThat(result).containsEntry("hilliness", "0.7");
        assertThat(result).containsEntry("roughness", "0.6");
        verify(chatLanguageModel, times(1)).generate(anyList());
    }

    @Test
    void testGenerateTerrainParameters_withoutModel() {
        AiWorldGeneratorService serviceWithoutModel = new AiWorldGeneratorService(null);

        Map<String, String> result = serviceWithoutModel.generateTerrainParameters("very hilly");

        assertThat(result).containsKeys("heightVariation", "hilliness", "roughness");
        assertThat(result.get("heightVariation")).isEqualTo("0.5");
    }

    @Test
    void testSuggestBlockType() {
        String expectedBlockType = "grass";
        AiMessage aiMessage = AiMessage.from(expectedBlockType);
        Response<AiMessage> response = Response.from(aiMessage);

        when(chatLanguageModel.generate(anyList())).thenReturn(response);

        String result = service.suggestBlockType("plains", 63, "surface level");

        assertThat(result).isEqualTo(expectedBlockType);
        verify(chatLanguageModel, times(1)).generate(anyList());
    }

    @Test
    void testSuggestBlockType_withoutModel() {
        AiWorldGeneratorService serviceWithoutModel = new AiWorldGeneratorService(null);

        String result = serviceWithoutModel.suggestBlockType("plains", 63, "surface level");

        assertThat(result).isEqualTo("grass");
    }

    @Test
    void testSuggestBlockType_lowHeight() {
        AiWorldGeneratorService serviceWithoutModel = new AiWorldGeneratorService(null);

        String result = serviceWithoutModel.suggestBlockType("cave", 3, "underground");

        assertThat(result).isEqualTo("stone");
    }
}
