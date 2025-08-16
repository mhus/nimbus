package de.mhus.nimbus.worldgenerator.config;

import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class PhaseProcessorConfig {

    @Bean
    public Map<String, PhaseProcessor> phaseProcessors(List<PhaseProcessor> processors) {
        return processors.stream()
                .collect(Collectors.toMap(
                    PhaseProcessor::getProcessorName,
                    Function.identity()
                ));
    }
}
