package de.mhus.nimbus.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.mhus.nimbus.common.properties.NimbusProperties;
import de.mhus.nimbus.common.util.RequestIdUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;

/**
 * Bean Configuration für Nimbus Common Module
 * Folgt Spring Boot Naming Conventions
 */
@Configuration
@EnableConfigurationProperties(NimbusProperties.class)
public class NimbusCommonConfiguration {

    /**
     * RestTemplate Bean für HTTP-Aufrufe zwischen Services
     */
    @Bean
    public RestTemplate nimbusRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Clock Bean für konsistente Zeitverarbeitung
     */
    @Bean
    public Clock nimbusClock() {
        return Clock.systemUTC();
    }

    /**
     * Request ID Utils Bean (wird bereits als @Component definiert, aber hier für Klarheit)
     */
    @Bean
    public RequestIdUtils nimbusRequestIdUtils() {
        return new RequestIdUtils();
    }

    /**
     * AvroObjectMapper Bean für JSON-Verarbeitung von Avro-Objekten
     * Konfiguriert für robuste Serialisierung/Deserialisierung
     */
    @Bean
    public ObjectMapper avroObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Konfiguration für robuste Deserialisierung
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        // Konfiguration für Serialisierung
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        return mapper;
    }
}
