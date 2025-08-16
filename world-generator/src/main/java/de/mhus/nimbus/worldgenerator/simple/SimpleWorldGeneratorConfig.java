package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Konfiguration für den World Generator Simple Service.
 * Registriert alle Phasen-Prozessoren und konfiguriert Asset-Pfade.
 */
@Configuration
@Slf4j
public class SimpleWorldGeneratorConfig {

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * Registriert alle Simple World Generator Prozessoren.
     */
    @Bean
    public Map<String, PhaseProcessor> simpleProcessors(
            SimpleTerrainProcessor terrainProcessor,
            SimpleAssetProcessor assetProcessor,
            SimpleBiomeProcessor biomeProcessor,
            SimpleStructureProcessor structureProcessor,
            SimpleWorldProcessor worldProcessor) {

        Map<String, PhaseProcessor> processors = new HashMap<>();
        processors.put("simpleTerrainProcessor", terrainProcessor);
        processors.put("simpleAssetProcessor", assetProcessor);
        processors.put("simpleBiomeProcessor", biomeProcessor);
        processors.put("simpleStructureProcessor", structureProcessor);
        processors.put("simpleWorldProcessor", worldProcessor);

        log.info("Registered {} Simple World Generator processors", processors.size());
        return processors;
    }

    /**
     * Konfiguriert den Asset-Pfad für Simple World Generator Assets.
     */
    @Bean
    public String simpleAssetPath() {
        return "classpath:simple/assets/";
    }
}
